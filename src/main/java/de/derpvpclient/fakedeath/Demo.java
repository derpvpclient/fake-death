package de.derpvpclient.fakedeath;

import de.derpvpclient.fakedeath.scene.Scene;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

/**
 * A one-time run through every scene, for testing and for the gallery. It only starts when the file
 * {@code config/fakedeath-demo.txt} exists: once in a world, every scene plays in turn, gets a picture at its
 * best moment and one of the death screen, and the death screen is clicked away by itself. At the end the
 * results land in {@code config/fakedeath-demo-result.txt} and the start file is deleted, so it runs only once.
 */
final class Demo {
	private static final String MARKER = "fakedeath-demo.txt";
	private static final String RESULT = "fakedeath-demo-result.txt";
	private static final int START_DELAY = 200;
	private static final int DEATH_PHOTO = 20;
	private static final int DEATH_SCREEN = 50;
	private static final int PAUSE = 40;
	private static final int GIVE_UP = 60 * 20;

	private static Boolean wanted;
	private static List<String> names = Situations.NAMES;
	private static boolean running;
	private static int index;
	private static int wait = START_DELAY;
	private static Scene current;
	private static boolean photo;
	private static boolean deathPhoto;
	private static int waitedForGround;
	private static final List<String> results = new ArrayList<>();

	private Demo() {
	}

	static boolean isRunning() {
		return running;
	}

	static void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
		if (wanted == null) {
			wanted = Files.exists(marker());
			names = chosenScenes();
		}
		if (!wanted) {
			return;
		}
		running = true;

		Scene scene = FakeDeath.currentScene();
		String name = index < names.size() ? names.get(index) : null;

		if (current != null) {
			if (scene != current) {
				// The scene is over, after the death screen or because it failed.
				String error = FakeDeath.lastError;
				results.add(name + ": " + (error != null ? "FEHLER " + error : deathPhoto ? "ok" : "ohne Todesbildschirm"));
				FakeDeathClient.LOGGER.info("[Fake Death demo] {} done: {}", name, results.getLast());
				current = null;
				index++;
				wait = PAUSE;
				return;
			}

			if (!photo && !FakeDeath.isActive() && FakeDeath.currentSceneTick() >= scene.photoTick()) {
				photo = true;
				photo(minecraft, name);
			}

			if (FakeDeath.isActive()) {
				if (!deathPhoto && FakeDeath.deathTicks() >= DEATH_PHOTO) {
					deathPhoto = true;
					photo(minecraft, name + "-death");
				}
				if (FakeDeath.deathTicks() >= DEATH_SCREEN) {
					FakeDeath.reveal(minecraft);
				}
			}
			return;
		}

		if (scene != null) {
			// Something else is playing, wait for it.
			return;
		}
		if (wait > 0) {
			wait--;
			return;
		}
		if (name == null) {
			finish();
			return;
		}
		if ((!player.onGround() || minecraft.gui.screen() != null) && ++waitedForGround < GIVE_UP) {
			return;
		}
		waitedForGround = 0;

		Scene next;
		try {
			next = Situations.create(name, level, player);
		} catch (RuntimeException e) {
			FakeDeathClient.LOGGER.error("[Fake Death demo] {} could not start", name, e);
			next = null;
			results.add(name + ": FEHLER beim Start " + e);
		}

		if (next == null) {
			if (results.isEmpty() || !results.getLast().startsWith(name + ":")) {
				results.add(name + ": übersprungen (passt hier nicht)");
			}
			index++;
			return;
		}

		FakeDeathClient.LOGGER.info("[Fake Death demo] {} ({}/{})", name, index + 1, names.size());
		current = next;
		photo = false;
		deathPhoto = false;
		FakeDeath.startNow(minecraft, next);
	}

	private static void photo(Minecraft minecraft, String name) {
		Screenshot.grab(minecraft.gameDirectory, "fakedeath-" + name + ".png", minecraft.gameRenderer.mainRenderTarget(), 1,
			message -> FakeDeathClient.LOGGER.info("[Fake Death demo] {}", message.getString()));
	}

	private static void finish() {
		running = false;
		wanted = false;
		FakeDeathClient.LOGGER.info("[Fake Death demo] finished: {}", results);

		try {
			Files.write(FabricLoader.getInstance().getConfigDir().resolve(RESULT), results, StandardCharsets.UTF_8);
			Files.deleteIfExists(marker());
		} catch (IOException e) {
			FakeDeathClient.LOGGER.warn("[Fake Death demo] could not write the result", e);
		}

		FakeDeath.rollRandomTimer();
	}

	/** The scenes listed in the start file, comma separated, or all of them. */
	private static List<String> chosenScenes() {
		try {
			List<String> chosen = java.util.Arrays.stream(Files.readString(marker()).split(","))
				.map(String::trim).filter(Situations.NAMES::contains).toList();
			return chosen.isEmpty() ? Situations.NAMES : chosen;
		} catch (IOException e) {
			return Situations.NAMES;
		}
	}

	private static Path marker() {
		return FabricLoader.getInstance().getConfigDir().resolve(MARKER);
	}
}
