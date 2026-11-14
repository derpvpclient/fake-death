package de.derpvpclient.fakedeath;

import java.util.Comparator;
import java.util.List;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ModOptionsBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The settings as a Fake Death entry in Sodium's video settings: one page for the random event, one per scene group.
 * Only loaded by Sodium; without it the options button stays.
 */
public final class FakeDeathSodiumPage implements ConfigEntryPoint {
	private static final Identifier ICON = Identifier.fromNamespaceAndPath("fakedeath", "textures/gui/config-icon.png");
	private static final int BLOOD_RED = 0xFFD0423A;
	private static final Identifier RANDOM = id("random");

	@Override
	public void registerConfigLate(ConfigBuilder builder) {
		OptionGroupBuilder general = builder.createOptionGroup()
			.setName(Component.translatable("fakedeath.options.general"))
			.addOption(builder.createBooleanOption(RANDOM)
				.setName(Component.translatable("fakedeath.options.random"))
				.setTooltip(Component.translatable("fakedeath.options.random.tooltip"))
				.setDefaultValue(true)
				.setBinding(value -> FakeDeathConfig.randomEvent = value, () -> FakeDeathConfig.randomEvent)
				.setStorageHandler(FakeDeathSodiumPage::saveRandom))
			.addOption(builder.createIntegerOption(id("random_min"))
				.setName(Component.translatable("fakedeath.options.min"))
				.setTooltip(Component.translatable("fakedeath.options.min.tooltip"))
				.setEnabledProvider(state -> state.readBooleanOption(RANDOM), RANDOM)
				.setDefaultValue(10)
				.setRange(1, 120, 1)
				.setValueFormatter(value -> Component.translatable("fakedeath.options.minutes", value))
				.setBinding(value -> FakeDeathConfig.randomMinMinutes = value, () -> FakeDeathConfig.randomMinMinutes)
				.setStorageHandler(FakeDeathSodiumPage::saveRandom))
			.addOption(builder.createIntegerOption(id("random_max"))
				.setName(Component.translatable("fakedeath.options.max"))
				.setTooltip(Component.translatable("fakedeath.options.max.tooltip"))
				.setEnabledProvider(state -> state.readBooleanOption(RANDOM), RANDOM)
				.setDefaultValue(30)
				.setRange(1, 120, 1)
				.setValueFormatter(value -> Component.translatable("fakedeath.options.minutes", value))
				.setBinding(value -> FakeDeathConfig.randomMaxMinutes = value, () -> FakeDeathConfig.randomMaxMinutes)
				.setStorageHandler(FakeDeathSodiumPage::saveRandom))
			.addOption(builder.createBooleanOption(id("flashing"))
				.setName(Component.translatable("fakedeath.options.flashing"))
				.setTooltip(Component.translatable("fakedeath.options.flashing.tooltip"))
				.setDefaultValue(true)
				.setBinding(value -> FakeDeathConfig.flashing = value, () -> FakeDeathConfig.flashing)
				.setStorageHandler(FakeDeathConfig::save));
		// The test screen is only for the developer.
		if (FakeDeathClient.isDeveloper()) {
			general.addOption(builder.createExternalButtonOption(id("test_screen"))
				.setName(Component.translatable("fakedeath.test.open"))
				.setTooltip(Component.translatable("fakedeath.options.test.tooltip"))
				.setScreenConsumer(parent -> Minecraft.getInstance().gui.setScreen(new TestScreen(parent))));
		}

		ModOptionsBuilder mod = builder.registerOwnModOptions()
			.setNonTintedIcon(ICON)
			.setColorTheme(builder.createColorTheme().setBaseThemeRGB(BLOOD_RED))
			.addPage(builder.createOptionPage()
				.setName(Component.translatable("fakedeath.options.general"))
				.addOptionGroup(general));

		// One page per scene group, the same groups as in the test screen, each sorted by name.
		for (String group : List.of("world", "animals", "monsters", "characters")) {
			List<String> ids = Situations.NAMES.stream()
				.filter(scene -> TestScreen.categoryOf(scene).equals(group))
				.sorted(Comparator.comparing(scene -> Component.translatable("fakedeath.scene." + scene).getString()))
				.toList();
			OptionGroupBuilder scenes = builder.createOptionGroup().setName(Component.translatable("fakedeath.options.scenes"));
			for (String scene : ids) {
				scenes.addOption(builder.createBooleanOption(id("scene_" + scene))
					.setName(Component.translatable("fakedeath.scene." + scene))
					.setTooltip(Component.translatable("fakedeath.options.scene.tooltip"))
					.setDefaultValue(true)
					.setBinding(value -> FakeDeathConfig.setEnabled(scene, value), () -> FakeDeathConfig.isEnabled(scene))
					.setStorageHandler(FakeDeathConfig::save));
			}
			mod.addPage(builder.createOptionPage()
				.setName(Component.translatable("fakedeath.test.tab." + group).append(" (" + ids.size() + ")"))
				.addOptionGroup(scenes));
		}
	}

	/** A changed random event starts its countdown again, and the latest time can not lie before the earliest. */
	private static void saveRandom() {
		FakeDeathConfig.randomMaxMinutes = Math.max(FakeDeathConfig.randomMinMinutes, FakeDeathConfig.randomMaxMinutes);
		FakeDeathConfig.save();
		FakeDeath.rollRandomTimer();
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("fakedeath", path);
	}
}
