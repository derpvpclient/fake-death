package de.derpvpclient.fakedeath;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FakeDeathClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Fake Death");

	// Every sub command works in German and English, whatever language the game is set to.

	@Override
	public void onInitializeClient() {
		FakeDeathConfig.load();
		FakeDeath.rollRandomTimer();

		ClientTickEvents.END_CLIENT_TICK.register(FakeDeath::tick);
		ClientTickEvents.END_CLIENT_TICK.register(TestScreen::tick);

		ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, state) -> Situations.onBlockBroken(level, pos, state));

		// Players have no commands at all, everything is in the options. Only the developer gets /fakedeath for testing.
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			if (isDeveloper()) {
				dispatcher.register(command());
			}
		});
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> command() {
		// Argument names show up in the command hints, so they follow the language the game has while joining.
		boolean german = isGerman();
		String scene = german ? "szene" : "scene";

		// Only ever registered for the developer, and only for testing.
		LiteralArgumentBuilder<FabricClientCommandSource> root = literal("fakedeath");
		root
			.then(literal("test")
				// Without a name: the test screen with every scene as a button.
				.executes(context -> {
					TestScreen.openNextTick();
					return 1;
				})
				// Greedy, because a single word may not contain umlauts like in "höhle".
				.then(argument(scene, StringArgumentType.greedyString())
					.suggests((context, builder) -> SharedSuggestionProvider.suggest(sceneNames(), builder))
					.executes(context -> {
						String name = StringArgumentType.getString(context, scene);

						if (Situations.resolve(name) == null) {
							feedback(context, "fakedeath.command.unknown_scene", String.join(", ", sceneNames()));
							return 0;
						}

						if (Situations.FLASHING.contains(Situations.resolve(name)) && !FakeDeathConfig.flashing) {
							feedback(context, "fakedeath.command.flashing_off");
							return 0;
						}

						FakeDeath.schedule(0, name);
						feedback(context, "fakedeath.command.test", Component.translatable("fakedeath.scene." + Situations.resolve(name)));
						return 1;
					})));

		return root;
	}

	/**
	 * The test command and the test screen: only with developer=true in config/fakedeath.properties (false by default,
	 * players never see either), or in a development run like the game tests.
	 */
	public static boolean isDeveloper() {
		return FakeDeathConfig.developer || net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment();
	}

	private static boolean isGerman() {
		return Minecraft.getInstance().getLanguageManager().getSelected().startsWith("de_");
	}

	private static List<String> sceneNames() {
		return isGerman() ? Situations.GERMAN_NAMES : Situations.ENGLISH_NAMES;
	}

	/** Texts come from the lang files: German for a German game, English for every other language. */
	private static void feedback(CommandContext<FabricClientCommandSource> context, String key, Object... args) {
		context.getSource().sendFeedback(Component.literal("[Fake Death] ").withStyle(ChatFormatting.DARK_RED)
			.append(Component.translatable(key, args).withStyle(ChatFormatting.GRAY)));
	}
}
