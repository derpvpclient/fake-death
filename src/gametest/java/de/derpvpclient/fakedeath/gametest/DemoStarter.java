package de.derpvpclient.fakedeath.gametest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** 1.21.1 has no client game test API: this creates a world from the title screen, lets the demo run and closes the game. */
public class DemoStarter implements ClientModInitializer {
	private static boolean started;
	private static int ticks;

	@Override
	public void onInitializeClient() {
		Path marker = FabricLoader.getInstance().getConfigDir().resolve("fakedeath-demo.txt");

		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			minecraft.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
			if (!started && minecraft.screen != null && minecraft.getOverlay() == null && minecraft.level == null && ++ticks > 60) {
				ticks = 0;
				started = true;
				try {
					Files.writeString(marker, System.getProperty("fakedeath.demoScenes", "demo"));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				LevelSettings settings = new LevelSettings("demo-" + System.currentTimeMillis(), GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
				minecraft.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new WorldOptions(12345L, false, false), WorldPresets::createNormalWorldDimensions, minecraft.screen);
			}
			if (started && minecraft.player != null && ++ticks > 200 && !Files.exists(marker)) {
				minecraft.stop();
			}
			if (ticks > 20 * 60 * 25) {
				minecraft.stop();
			}
		});
	}
}
