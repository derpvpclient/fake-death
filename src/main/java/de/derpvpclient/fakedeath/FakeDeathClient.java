package de.derpvpclient.fakedeath;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import com.mojang.brigadier.arguments.IntegerArgumentType;
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
	private static final List<String> CANCEL = List.of("abbrechen", "cancel");
	private static final List<String> RANDOM = List.of("zufall", "random");
	private static final List<String> ON = List.of("an", "on");
	private static final List<String> OFF = List.of("aus", "off");

	@Override
	public void onInitializeClient() {
		FakeDeathConfig.load();
		FakeDeath.rollRandomTimer();

		ClientTickEvents.END_CLIENT_TICK.register(FakeDeath::tick);
		ClientTickEvents.END_CLIENT_TICK.register(TestScreen::tick);

		ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, state) -> Situations.onBlockBroken(level, pos, state));

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(command()));
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> command() {
		// Argument names show up in the command hints, so they follow the language the game has while joining.
		boolean german = isGerman();
		String seconds = german ? "sekunden" : "seconds";
		String scene = german ? "szene" : "scene";

		LiteralArgumentBuilder<FabricClientCommandSource> root = literal("fakedeath")
			.executes(context -> {
				FakeDeath.schedule(0, null);
				return 1;
			})
			.then(literal("in")
				.then(argument(seconds, IntegerArgumentType.integer(0, 3600))
					.executes(context -> {
						int delay = IntegerArgumentType.getInteger(context, seconds);
						FakeDeath.schedule(delay * 20, null);
						feedback(context, "fakedeath.command.delay", delay);
						return 1;
					})))
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

		for (String word : CANCEL) {
			root.then(literal(word).executes(context -> {
				feedback(context, FakeDeath.cancelPending() ? "fakedeath.command.cancelled" : "fakedeath.command.nothing_planned");
				return 1;
			}));
		}

		for (String word : RANDOM) {
			LiteralArgumentBuilder<FabricClientCommandSource> random = literal(word)
				.executes(context -> {
					feedback(context, FakeDeathConfig.randomEvent ? "fakedeath.command.random.status_on" : "fakedeath.command.random.status_off",
						FakeDeathConfig.randomMinMinutes, FakeDeathConfig.randomMaxMinutes);
					return 1;
				})
				.then(argument("min", IntegerArgumentType.integer(1, 600))
					.then(argument("max", IntegerArgumentType.integer(1, 600))
						.executes(context -> {
							int min = IntegerArgumentType.getInteger(context, "min");
							int max = Math.max(min, IntegerArgumentType.getInteger(context, "max"));
							FakeDeathConfig.randomMinMinutes = min;
							FakeDeathConfig.randomMaxMinutes = max;
							FakeDeathConfig.save();
							FakeDeath.rollRandomTimer();
							feedback(context, "fakedeath.command.random.range", min, max);
							return 1;
						})));
			ON.forEach(on -> random.then(literal(on).executes(context -> setRandom(context, true))));
			OFF.forEach(off -> random.then(literal(off).executes(context -> setRandom(context, false))));
			root.then(random);
		}

		return root;
	}

	private static boolean isGerman() {
		return Minecraft.getInstance().getLanguageManager().getSelected().startsWith("de_");
	}

	private static List<String> sceneNames() {
		return isGerman() ? Situations.GERMAN_NAMES : Situations.ENGLISH_NAMES;
	}

	private static int setRandom(CommandContext<FabricClientCommandSource> context, boolean enabled) {
		FakeDeathConfig.randomEvent = enabled;
		FakeDeathConfig.save();
		FakeDeath.rollRandomTimer();
		if (enabled) {
			feedback(context, "fakedeath.command.random.on", FakeDeathConfig.randomMinMinutes, FakeDeathConfig.randomMaxMinutes);
		} else {
			feedback(context, "fakedeath.command.random.off");
		}
		return 1;
	}

	/** Texts come from the lang files: German for a German game, English for every other language. */
	private static void feedback(CommandContext<FabricClientCommandSource> context, String key, Object... args) {
		context.getSource().sendFeedback(Component.literal("[Fake Death] ").withStyle(ChatFormatting.DARK_RED)
			.append(Component.translatable(key, args).withStyle(ChatFormatting.GRAY)));
	}
}
