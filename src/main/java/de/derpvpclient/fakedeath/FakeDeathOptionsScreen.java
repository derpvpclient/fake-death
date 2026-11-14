package de.derpvpclient.fakedeath;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** The settings, built like Minecraft's own option pages. Opened from Mod Menu and from the options screen. */
public class FakeDeathOptionsScreen extends OptionsSubScreen {
	private final boolean randomBefore = FakeDeathConfig.randomEvent;
	private final int minBefore = FakeDeathConfig.randomMinMinutes;
	private final int maxBefore = FakeDeathConfig.randomMaxMinutes;

	public FakeDeathOptionsScreen(Screen parent) {
		super(parent, Minecraft.getInstance().options, Component.translatable("fakedeath.options.title"));
	}

	@Override
	protected void addOptions() {
		list.addHeader(Component.translatable("fakedeath.options.general"));
		list.addBig(OptionInstance.createBoolean("fakedeath.options.random", tooltip("fakedeath.options.random.tooltip"),
			FakeDeathConfig.randomEvent, value -> FakeDeathConfig.randomEvent = value));
		list.addSmall(
			minutes("fakedeath.options.min", FakeDeathConfig.randomMinMinutes, value -> FakeDeathConfig.randomMinMinutes = value),
			minutes("fakedeath.options.max", FakeDeathConfig.randomMaxMinutes, value -> FakeDeathConfig.randomMaxMinutes = value));
		list.addBig(OptionInstance.createBoolean("fakedeath.options.flashing", tooltip("fakedeath.options.flashing.tooltip"),
			FakeDeathConfig.flashing, value -> FakeDeathConfig.flashing = value));

		// The scenes in the same groups as in the test screen, each sorted by name, with a switch for all of them at once.
		list.addHeader(Component.translatable("fakedeath.options.scenes"));
		list.addSmall(
			Button.builder(Component.translatable("fakedeath.options.all_on"), button -> setAll(true)).build(),
			Button.builder(Component.translatable("fakedeath.options.all_off"), button -> setAll(false)).build());
		for (String group : java.util.List.of("world", "animals", "monsters", "characters")) {
			java.util.List<String> ids = Situations.NAMES.stream()
				.filter(id -> TestScreen.categoryOf(id).equals(group))
				.sorted(java.util.Comparator.comparing(id -> Component.translatable("fakedeath.scene." + id).getString()))
				.toList();
			list.addHeader(Component.translatable("fakedeath.test.tab." + group).append(" (" + ids.size() + ")"));
			list.addSmall(ids.stream()
				.map(id -> OptionInstance.createBoolean("fakedeath.scene." + id, FakeDeathConfig.isEnabled(id), value -> FakeDeathConfig.setEnabled(id, value)))
				.toArray(OptionInstance[]::new));
		}
	}

	@Override
	protected void addFooter() {
		LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(8));
		// The test screen is only for the developer.
		if (FakeDeathClient.isDeveloper()) {
			footer.addChild(Button.builder(Component.translatable("fakedeath.test.open"), button -> minecraft.gui.setScreen(new TestScreen(this))).build());
		}
		footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).build());
	}

	/** Every scene on or off at once, then the page is drawn again with the new switches. */
	private void setAll(boolean enabled) {
		for (String id : Situations.NAMES) {
			FakeDeathConfig.setEnabled(id, enabled);
		}
		FakeDeathConfig.save();
		minecraft.gui.setScreen(new FakeDeathOptionsScreen(lastScreen));
	}

	private static OptionInstance<Integer> minutes(String key, int value, Consumer<Integer> setter) {
		return new OptionInstance<>(key, tooltip(key + ".tooltip"),
			(caption, minutes) -> Options.genericValueLabel(caption, Component.translatable("fakedeath.options.minutes", minutes)),
			new OptionInstance.IntRange(1, 120), value, setter::accept);
	}

	private static <T> OptionInstance.TooltipSupplier<T> tooltip(String key) {
		return OptionInstance.cachedConstantTooltip(Component.translatable(key));
	}

	@Override
	public void removed() {
		FakeDeathConfig.randomMaxMinutes = Math.max(FakeDeathConfig.randomMinMinutes, FakeDeathConfig.randomMaxMinutes);
		FakeDeathConfig.save();

		// Only a changed random event starts its countdown again.
		if (FakeDeathConfig.randomEvent != randomBefore || FakeDeathConfig.randomMinMinutes != minBefore || FakeDeathConfig.randomMaxMinutes != maxBefore) {
			FakeDeath.rollRandomTimer();
		}

		super.removed();
	}
}
