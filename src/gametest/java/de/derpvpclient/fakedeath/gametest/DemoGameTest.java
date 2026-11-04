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
		context.runOnClient(client -> client.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0));
		Path marker = FabricLoader.getInstance().getConfigDir().resolve("fakedeath-demo.txt");

		try {
			Files.writeString(marker, System.getProperty("fakedeath.demoScenes", "demo"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getClientWorld().waitForChunksRender();
			context.waitFor(client -> !Files.exists(marker), 20 * 60 * 25);
		}
	}
}
