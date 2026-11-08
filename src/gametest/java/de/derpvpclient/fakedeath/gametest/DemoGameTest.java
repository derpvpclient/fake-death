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
				context.runOnClient(client -> client.gui.screen().children().stream()
					.filter(child -> child instanceof net.minecraft.client.gui.components.Button button
						&& button.getMessage().getString().contains(net.minecraft.network.chat.Component.translatable("fakedeath.test.tab.characters").getString()))
					.findFirst().ifPresent(child -> ((net.minecraft.client.gui.components.Button) child).onPress(null)));
				context.waitTicks(5);
				context.takeScreenshot("ui-2-characters");
				context.getInput().typeChars("ko");
				context.waitTicks(5);
				context.takeScreenshot("ui-3-search");
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
			context.waitFor(client -> !Files.exists(marker), 20 * 60 * 25);
		}
	}
}
