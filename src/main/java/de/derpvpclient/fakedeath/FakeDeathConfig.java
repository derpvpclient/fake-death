package de.derpvpclient.fakedeath;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import net.fabricmc.loader.api.FabricLoader;

public final class FakeDeathConfig {
	// Version 2 turned the random event on by default, version 3 the flashing effects.
	private static final int VERSION = 3;

	public static boolean randomEvent = true;
	public static int randomMinMinutes = 10;
	public static int randomMaxMinutes = 30;
	/** Scenes with fast flashing lights. On by default, people sensitive to them can switch them off. */
	public static boolean flashing = true;
	/** Developer switch, only in the file: true gives /fakedeath test and the test screen in game. Off for players. */
	public static boolean developer = false;
	/** Scene ids the random event and a plain /fakedeath leave out. Testing a scene by name still works. */
	public static final Set<String> disabledScenes = new TreeSet<>();

	private FakeDeathConfig() {
	}

	public static boolean isEnabled(String scene) {
		return !disabledScenes.contains(scene);
	}

	public static void setEnabled(String scene, boolean enabled) {
		if (enabled) {
			disabledScenes.remove(scene);
		} else {
			disabledScenes.add(scene);
		}
	}

	public static void load() {
		Path file = file();
		Properties properties = new Properties();

		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				properties.load(reader);
			} catch (IOException | IllegalArgumentException e) {
				FakeDeathClient.LOGGER.warn("Could not read {}, using defaults", file, e);
			}
		}

		// Older files only have randomEvent=false (before version 2) or flashing=false (before version 3)
		// because that was the default, not a choice.
		int version = parseInt(properties, "version", 1);
		randomEvent = version < 2 || Boolean.parseBoolean(properties.getProperty("randomEvent", String.valueOf(randomEvent)));
		randomMinMinutes = Math.max(1, parseInt(properties, "randomMinMinutes", randomMinMinutes));
		randomMaxMinutes = Math.max(randomMinMinutes, parseInt(properties, "randomMaxMinutes", randomMaxMinutes));
		flashing = version < 3 || Boolean.parseBoolean(properties.getProperty("flashing", String.valueOf(flashing)));
		developer = Boolean.parseBoolean(properties.getProperty("developer", "false"));

		disabledScenes.clear();
		for (String scene : properties.getProperty("disabledScenes", "").split(",")) {
			if (Situations.NAMES.contains(scene.trim())) {
				disabledScenes.add(scene.trim());
			}
		}

		save();
	}

	public static void save() {
		Path file = file();
		Properties properties = new Properties();
		properties.setProperty("version", String.valueOf(VERSION));
		properties.setProperty("randomEvent", String.valueOf(randomEvent));
		properties.setProperty("randomMinMinutes", String.valueOf(randomMinMinutes));
		properties.setProperty("randomMaxMinutes", String.valueOf(randomMaxMinutes));
		properties.setProperty("flashing", String.valueOf(flashing));
		properties.setProperty("developer", String.valueOf(developer));
		properties.setProperty("disabledScenes", String.join(",", disabledScenes));

		try (Writer writer = Files.newBufferedWriter(file)) {
			properties.store(writer, "Fake Death. randomEvent = Fake-Tod zufällig zwischen randomMinMinutes und randomMaxMinutes. flashing = Szenen mit schnell blinkenden Lichtern erlauben. disabledScenes = abgeschaltete Szenen, mit Komma getrennt. developer = true nur für den Entwickler: /fakedeath test im Spiel");
		} catch (IOException e) {
			FakeDeathClient.LOGGER.warn("Could not write {}", file, e);
		}
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("fakedeath.properties");
	}

	private static int parseInt(Properties properties, String key, int fallback) {
		try {
			return Integer.parseInt(properties.getProperty(key, String.valueOf(fallback)).trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
