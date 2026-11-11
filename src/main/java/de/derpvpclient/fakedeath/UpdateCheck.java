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

	private UpdateCheck() {
	}

	/** The newer version number without the Minecraft part, like "1.0.56", or null if there is none. Blocks while asking. */
	static String newer() {
		if (!FakeDeathConfig.updateCheck) {
			return null;
		}

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

		try {
			URI uri = URI.create("https://api.modrinth.com/v2/project/" + SLUG + "/version?loaders=" + encode("[\"fabric\"]")
				+ "&game_versions=" + encode("[\"" + game + "\"]"));
			HttpRequest request = HttpRequest.newBuilder(uri)
				.header("User-Agent", "derpvpclient/" + SLUG + "/" + own)
				.timeout(Duration.ofSeconds(15))
				.build();
			HttpResponse<String> response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
				.send(request, HttpResponse.BodyHandlers.ofString());
			return response.statusCode() == 200 ? read(response.body(), own) : null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (Exception e) {
			FakeDeathClient.LOGGER.debug("Update check failed", e);
			return null;
		}
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
