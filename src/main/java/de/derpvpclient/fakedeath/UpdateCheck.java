package de.derpvpclient.fakedeath;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Asks Modrinth whether there is a newer release for this Minecraft version. Mod Menu calls it from its own update
 * thread and shows the result in its mod list, nothing is written into the chat and nothing is downloaded. Any
 * failure simply means "no update".
 */
final class UpdateCheck {
	private static final String MOD_ID = "fakedeath";
	private static final String SLUG = "fake-death";
	/** The same release is on both sites. */
	static final String MODRINTH = "https://modrinth.com/mod/" + SLUG;
	static final String CURSEFORGE = "https://www.curseforge.com/minecraft/mc-mods/" + SLUG;
	private static final int CURSEFORGE_ID = 1795579;
	/** Where the found update can be downloaded: Modrinth, or CurseForge while Modrinth does not list the project. */
	static volatile String link = MODRINTH + "/versions";
	/** Set by newer(): the notice names CurseForge first. */
	static volatile boolean curseForgeFirst;

	private UpdateCheck() {
	}

	/** The newer version number without the Minecraft part, like "1.0.56", or null if there is none. Blocks while asking. */
	static String newer() {
		// Whoever got the file from CurseForge is sent to CurseForge first, everyone else to Modrinth.
		curseForgeFirst = fromCurseForgeApp();
		link = curseForgeFirst ? CURSEFORGE + "/files" : MODRINTH + "/versions";
		FakeDeathClient.LOGGER.info("Update check, {} first", curseForgeFirst ? "CurseForge" : "Modrinth");

		// For the game test: pretends this version is out, so the notice can be seen without asking Modrinth.
		String pretend = System.getProperty("fakedeath.updateTest");
		if (pretend != null && !pretend.isEmpty()) {
			return pretend;
		}

		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			return null;
		}

		String own = version(MOD_ID);
		String game = version("minecraft");
		if (own == null || game == null) {
			return null;
		}

		if (curseForgeFirst) {
			String found = fromCurseForge(own, game);
			if (found != null) {
				return found;
			}
		}

		try {
			URI uri = URI.create("https://api.modrinth.com/v2/project/" + SLUG + "/version?loaders=" + encode("[\"fabric\"]")
				+ "&game_versions=" + encode("[\"" + game + "\"]"));
			HttpRequest request = HttpRequest.newBuilder(uri)
				.header("User-Agent", "derpvpclient/" + SLUG + "/" + own)
				.timeout(Duration.ofSeconds(15))
				.build();
			HttpResponse<String> response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
				.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				String found = read(response.body(), own);
				if (found != null) {
					link = MODRINTH + "/versions";
					return found;
				}
			}
			// Modrinth answers 404 while the project is still waiting for review: then CurseForge, then the public source.
			if (!curseForgeFirst) {
				String found = fromCurseForge(own, game);
				if (found != null) {
					link = CURSEFORGE + "/files";
					return found;
				}
			}
			if (response.statusCode() != 200) {
				// Not listed on Modrinth yet: the download is on CurseForge.
				link = CURSEFORGE + "/files";
			}
			return fromGitHub(own, game);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (Exception e) {
			FakeDeathClient.LOGGER.debug("Update check failed", e);
			return null;
		}
	}

	/**
	 * True for the file downloaded from CurseForge (it carries fakedeath-download.txt), or, for older files, when the game
	 * runs from a CurseForge app instance (its folders live under "curseforge").
	 */
	static boolean fromCurseForgeApp() {
		if (FabricLoader.getInstance().getModContainer(MOD_ID).flatMap(mod -> mod.findPath("fakedeath-download.txt")).isPresent()) {
			return true;
		}
		String dir = FabricLoader.getInstance().getGameDir().toAbsolutePath().toString().toLowerCase(java.util.Locale.ROOT);
		return dir.contains("curseforge");
	}

	/** The newest approved release on CurseForge for this Minecraft version, if it is newer. Read from the file names. */
	static String fromCurseForge(String own, String game) {
		try {
			URI uri = URI.create("https://www.curseforge.com/api/v1/mods/" + CURSEFORGE_ID + "/files?pageIndex=0&pageSize=50&sort=dateCreated&sortDescending=true");
			HttpRequest request = HttpRequest.newBuilder(uri)
				.header("User-Agent", "derpvpclient/" + SLUG + "/" + own)
				.timeout(Duration.ofSeconds(15))
				.build();
			HttpResponse<String> response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
				.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				return null;
			}
			String best = null;
			for (JsonElement element : JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("data")) {
				JsonObject file = element.getAsJsonObject();
				// 4 is approved, 1 a release (not beta or alpha).
				if (file.get("status").getAsInt() != 4 || file.get("releaseType").getAsInt() != 1) {
					continue;
				}
				boolean forThisGame = false;
				for (JsonElement version : file.getAsJsonArray("gameVersions")) {
					forThisGame |= game.equals(version.getAsString());
				}
				String name = file.get("fileName").getAsString();
				if (!forThisGame || !name.startsWith(MOD_ID + "-") || !name.endsWith(".jar")) {
					continue;
				}
				String number = name.substring(MOD_ID.length() + 1, name.length() - ".jar".length()).split("[+-]")[0];
				if (compare(number, best != null ? best : own) > 0) {
					best = plain(number);
				}
			}
			return best;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (Exception e) {
			FakeDeathClient.LOGGER.debug("CurseForge update check failed", e);
			return null;
		}
	}

	/** The version in the public repository's branch for this Minecraft version, if it is newer. */
	static String fromGitHub(String own, String game) throws Exception {
		// The branch comes from the own version: "1.1.0+26.1" was built from mc/26.1, a plain number from main.
		String branch = own.contains("+") ? "mc/" + own.substring(own.indexOf('+') + 1) : "main";
		URI uri = URI.create("https://raw.githubusercontent.com/derpvpclient/" + SLUG + "/" + branch + "/gradle.properties");
		HttpRequest request = HttpRequest.newBuilder(uri)
			.header("User-Agent", "derpvpclient/" + SLUG + "/" + own)
			.timeout(Duration.ofSeconds(15))
			.build();
		HttpResponse<String> response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
			.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			return null;
		}
		for (String line : response.body().split("\\R")) {
			if (line.startsWith("version=")) {
				String number = line.substring("version=".length()).trim();
				return compare(number, own) > 0 ? plain(number) : null;
			}
		}
		return null;
	}

	/** The highest release Modrinth lists, if it is newer than the installed one. */
	static String read(String body, String own) {
		String best = null;

		for (JsonElement element : JsonParser.parseString(body).getAsJsonArray()) {
			JsonObject version = element.getAsJsonObject();
			if (!"release".equals(version.get("version_type").getAsString())) {
				continue;
			}

			String number = version.get("version_number").getAsString();
			if (compare(number, best != null ? best : own) > 0) {
				best = plain(number);
			}
		}

		return best;
	}

	private static String version(String modId) {
		return FabricLoader.getInstance().getModContainer(modId)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse(null);
	}

	private static String encode(String text) {
		return URLEncoder.encode(text, StandardCharsets.UTF_8);
	}

	/** "1.0.54+26.2" is 1.0.54: the part behind the plus only names the Minecraft version. */
	private static String plain(String version) {
		int plus = version.indexOf('+');
		return plus < 0 ? version : version.substring(0, plus);
	}

	/** Compares the numbers between the dots, so 1.0.10 is newer than 1.0.9. */
	static int compare(String a, String b) {
		String[] left = plain(a).split("\\.");
		String[] right = plain(b).split("\\.");

		for (int i = 0; i < Math.max(left.length, right.length); i++) {
			int difference = Integer.compare(number(left, i), number(right, i));
			if (difference != 0) {
				return difference;
			}
		}
		return 0;
	}

	private static int number(String[] parts, int index) {
		if (index >= parts.length) {
			return 0;
		}
		try {
			return Integer.parseInt(parts[index].replaceAll("\\D.*", ""));
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
