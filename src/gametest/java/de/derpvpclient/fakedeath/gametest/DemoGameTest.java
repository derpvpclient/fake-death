package de.derpvpclient.fakedeath.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;

/** Creates a world and waits until the Fake Death demo has played every scene. */
public class DemoGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		// Tests run silently.
		if (!Boolean.getBoolean("fakedeath.demoSound")) {
			context.runOnClient(client -> client.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0));
		} else {
			context.runOnClient(client -> client.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.6));
		}
		if (Boolean.getBoolean("fakedeath.demoUi")) {
			// Only the test screen: opened by the real command, a tab, a search, each one as a picture.
			try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
				context.waitTicks(60);
				context.runOnClient(client -> client.player.connection.sendCommand("fakedeath test"));
				context.waitTicks(10);
				context.takeScreenshot("ui-1-all");
				context.runOnClient(client -> client.gui.screen().children().stream()
					.filter(child -> child instanceof net.minecraft.client.gui.components.Button button
						&& button.getMessage().getString().contains(net.minecraft.network.chat.Component.translatable("fakedeath.test.tab.characters").getString()))
					.findFirst().ifPresent(child -> ((net.minecraft.client.gui.components.Button) child).onPress(null)));
				context.waitTicks(5);
				context.takeScreenshot("ui-2-characters");
				context.getInput().typeChars("ko");
				context.waitTicks(5);
				context.takeScreenshot("ui-3-search");
				// The settings page, at the top and further down among the scenes.
				context.runOnClient(client -> client.gui.setScreen(new de.derpvpclient.fakedeath.FakeDeathOptionsScreen(null)));
				context.waitTicks(10);
				context.takeScreenshot("ui-4-options");
				context.runOnClient(client -> client.gui.screen().mouseScrolled(client.getWindow().getGuiScaledWidth() / 2.0, 120.0, 0.0, -12.0));
				context.waitTicks(5);
				context.takeScreenshot("ui-5-options-scenes");
			}
			return;
		}
		if (!System.getProperty("fakedeath.updateTest", "").isEmpty()) {
			// The update notice: with a pretended newer version Mod Menu marks the mod in its list.
			try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
				context.waitTicks(60);
				context.runOnClient(client -> {
					try {
						client.gui.setScreen((net.minecraft.client.gui.screens.Screen) Class.forName("com.terraformersmc.modmenu.api.ModMenuApi")
							.getMethod("createModsScreen", net.minecraft.client.gui.screens.Screen.class).invoke(null, (Object) null));
					} catch (ReflectiveOperationException e) {
						throw new IllegalStateException(e);
					}
				});
				context.waitTicks(200);
				context.getInput().typeChars("Fake");
				context.waitTicks(10);
				context.takeScreenshot("update-1-modmenu");
			}
			return;
		}
		if (Boolean.getBoolean("fakedeath.demoRandom")) {
			// The random event: comes by itself when due, sets its next time, stays away from monsters and when switched off.
			StringBuilder result = new StringBuilder();
			try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
				context.waitTicks(100);
				context.runOnClient(client -> {
					de.derpvpclient.fakedeath.FakeDeathConfig.randomEvent = true;
					de.derpvpclient.fakedeath.FakeDeath.rollRandomTimer();
				});
				int rolled = context.computeOnClient(client -> de.derpvpclient.fakedeath.FakeDeath.randomTicksLeft());
				int min = de.derpvpclient.fakedeath.FakeDeathConfig.randomMinMinutes * 1200;
				int max = de.derpvpclient.fakedeath.FakeDeathConfig.randomMaxMinutes * 1200;
				result.append("timer after switching on: ").append(rolled).append(rolled >= min - 200 && rolled <= max ? " ok" : " WRONG").append(System.lineSeparator());

				// Which scenes the event picks here, by day on open ground: a hundred rolls.
				java.util.Map<String, Integer> mix = context.computeOnClient(client -> {
					java.util.Map<String, Integer> counts = new java.util.TreeMap<>();
					for (int i = 0; i < 400; i++) {
						de.derpvpclient.fakedeath.scene.Scene scene = de.derpvpclient.fakedeath.Situations.pick(client.level, client.player);
						counts.merge(scene == null ? "none" : scene.getClass().getSimpleName(), 1, Integer::sum);
					}
					return counts;
				});
				result.append("400 picks: ").append(mix).append(System.lineSeparator());

				// A monster close by holds it back.
				singleplayer.getServer().runCommand("difficulty easy");
				singleplayer.getServer().runCommand("execute as @a at @s run summon minecraft:husk ~4 ~ ~ {NoAI:1b,Silent:1b,PersistenceRequired:1b}");
				context.waitTicks(40);
				context.runOnClient(client -> de.derpvpclient.fakedeath.FakeDeath.randomDueIn(20));
				context.waitTicks(400);
				boolean early = context.computeOnClient(client -> de.derpvpclient.fakedeath.FakeDeath.currentScene() != null);
				result.append("monster next to the player: ").append(early ? "STARTED ANYWAY" : "waited").append(System.lineSeparator());

				// Monster gone: it starts by itself, at the latest a minute after it was due.
				singleplayer.getServer().runCommand("kill @e[type=minecraft:husk]");
				singleplayer.getServer().runCommand("kill @e[type=minecraft:item]");
				int waited = 0;
				boolean started = false;
				while (waited < 1500 && !started) {
					context.waitTicks(20);
					waited += 20;
					started = context.computeOnClient(client -> de.derpvpclient.fakedeath.FakeDeath.currentScene() != null);
				}
				String scene = context.computeOnClient(client -> de.derpvpclient.fakedeath.FakeDeath.currentScene() == null
					? "none" : de.derpvpclient.fakedeath.FakeDeath.currentScene().getClass().getSimpleName());
				result.append("after the monster left: ").append(started ? "started " + scene + " after " + waited + " ticks" : "DID NOT START").append(System.lineSeparator());
				int next = context.computeOnClient(client -> de.derpvpclient.fakedeath.FakeDeath.randomTicksLeft());
				result.append("next one planned in: ").append(next).append(next >= min - 200 && next <= max ? " ok" : " WRONG").append(System.lineSeparator());
				context.takeScreenshot("random-1-scene");

				// Through the scene to the death screen and back into the game.
				waited = 0;
				while (waited < 2400 && !context.computeOnClient(client -> de.derpvpclient.fakedeath.FakeDeath.isActive())) {
					context.waitTicks(20);
					waited += 20;
				}
				boolean died = context.computeOnClient(client -> de.derpvpclient.fakedeath.FakeDeath.isActive());
				context.waitTicks(30);
				context.takeScreenshot("random-2-death");
				context.runOnClient(client -> de.derpvpclient.fakedeath.FakeDeath.reveal(client));
				context.waitTicks(60);
				boolean over = context.computeOnClient(client -> de.derpvpclient.fakedeath.FakeDeath.currentScene() == null);
				result.append("death screen: ").append(died ? "reached" : "NOT REACHED").append(", afterwards: ").append(over ? "back in the game" : "STILL RUNNING").append(System.lineSeparator());

				// Switched off: nothing is planned and nothing comes.
				context.runOnClient(client -> {
					de.derpvpclient.fakedeath.FakeDeathConfig.randomEvent = false;
					de.derpvpclient.fakedeath.FakeDeath.rollRandomTimer();
				});
				int off = context.computeOnClient(client -> de.derpvpclient.fakedeath.FakeDeath.randomTicksLeft());
				context.waitTicks(200);
				boolean quiet = context.computeOnClient(client -> de.derpvpclient.fakedeath.FakeDeath.currentScene() == null);
				result.append("switched off: timer ").append(off).append(off == -1 && quiet ? " ok, nothing started" : " WRONG").append(System.lineSeparator());
				context.runOnClient(client -> de.derpvpclient.fakedeath.FakeDeathConfig.randomEvent = true);
			}
			try {
				Files.writeString(FabricLoader.getInstance().getGameDir().resolve("fakedeath-random-result.txt"), result.toString());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			return;
		}
		if (Boolean.getBoolean("fakedeath.demoTree")) {
			// Up in a tree the fake death waits, back on the ground it starts.
			try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
				context.waitTicks(60);
				singleplayer.getServer().runCommand("execute as @a at @s run setblock ~ ~3 ~ minecraft:oak_leaves[persistent=true]");
				singleplayer.getServer().runCommand("execute as @a at @s run tp @s ~ ~4 ~");
				context.waitTicks(40);
				context.runOnClient(client -> client.player.connection.sendCommand("fakedeath test beeren"));
				context.waitTicks(180);
				context.takeScreenshot("tree-1-waiting");
				boolean early = context.computeOnClient(client -> de.derpvpclient.fakedeath.FakeDeath.currentScene() != null);
				singleplayer.getServer().runCommand("execute as @a at @s run setblock ~ ~-1 ~ minecraft:air");
				context.waitTicks(120);
				boolean started = context.computeOnClient(client -> de.derpvpclient.fakedeath.FakeDeath.currentScene() != null);
				context.takeScreenshot("tree-2-ground");
				try {
					Files.writeString(FabricLoader.getInstance().getGameDir().resolve("fakedeath-tree-result.txt"),
						"on the tree: " + (early ? "STARTED" : "waited") + ", on the ground: " + (started ? "started" : "DID NOT START") + System.lineSeparator());
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
			return;
		}
		Path marker = FabricLoader.getInstance().getConfigDir().resolve("fakedeath-demo.txt");

		try {
			Files.writeString(marker, System.getProperty("fakedeath.demoScenes", "demo"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			context.waitTicks(100);
			context.waitFor(client -> !Files.exists(marker), 20 * 60 * 240);
		}
	}
}
