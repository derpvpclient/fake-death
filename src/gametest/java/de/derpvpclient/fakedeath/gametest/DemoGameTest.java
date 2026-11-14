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
		if (Boolean.getBoolean("fakedeath.demoUi")) {
			// Only the test screen: opened by the real command, a tab, a search, each one as a picture.
			try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
				context.waitTicks(60);
				context.runOnClient(client -> client.player.connection.sendCommand("fakedeath test"));
				context.waitTicks(10);
				context.takeScreenshot("ui-1-all");
				context.runOnClient(client -> client.screen.children().stream()
					.filter(child -> child instanceof net.minecraft.client.gui.components.Button button
						&& button.getMessage().getString().contains(net.minecraft.network.chat.Component.translatable("fakedeath.test.tab.characters").getString()))
					.findFirst().ifPresent(child -> ((net.minecraft.client.gui.components.Button) child).onPress()));
				context.waitTicks(5);
				context.takeScreenshot("ui-2-characters");
				context.getInput().typeChars("ko");
				context.waitTicks(5);
				context.takeScreenshot("ui-3-search");
				// The settings page, at the top and further down among the scenes.
				context.runOnClient(client -> client.setScreen(new de.derpvpclient.fakedeath.FakeDeathOptionsScreen(null)));
				context.waitTicks(10);
				context.takeScreenshot("ui-4-options");
				context.runOnClient(client -> client.screen.mouseScrolled(client.getWindow().getGuiScaledWidth() / 2.0, 120.0, 0.0, -12.0));
				context.waitTicks(5);
				context.takeScreenshot("ui-5-options-scenes");
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
			singleplayer.getClientWorld().waitForChunksRender();
			context.waitFor(client -> !Files.exists(marker), 20 * 60 * 240);
		}
	}
}
