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
		list.addBig(OptionInstance.createBoolean("fakedeath.options.random", tooltip("fakedeath.options.random.tooltip"),
			FakeDeathConfig.randomEvent, value -> FakeDeathConfig.randomEvent = value));
		list.addSmall(
			minutes("fakedeath.options.min", FakeDeathConfig.randomMinMinutes, value -> FakeDeathConfig.randomMinMinutes = value),
			minutes("fakedeath.options.max", FakeDeathConfig.randomMaxMinutes, value -> FakeDeathConfig.randomMaxMinutes = value));
		list.addBig(OptionInstance.createBoolean("fakedeath.options.reveal", tooltip("fakedeath.options.reveal.tooltip"),
			FakeDeathConfig.reveal, value -> FakeDeathConfig.reveal = value));
		list.addBig(OptionInstance.createBoolean("fakedeath.options.flashing", tooltip("fakedeath.options.flashing.tooltip"),
			FakeDeathConfig.flashing, value -> FakeDeathConfig.flashing = value));

		list.addSmall(Situations.NAMES.stream()
			.map(id -> OptionInstance.createBoolean("fakedeath.scene." + id, FakeDeathConfig.isEnabled(id), value -> FakeDeathConfig.setEnabled(id, value)))
			.toArray(OptionInstance[]::new));
	}

	@Override
	protected void addFooter() {
		LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(8));
		footer.addChild(Button.builder(Component.translatable("fakedeath.test.open"), button -> minecraft.setScreen(new TestScreen(this))).build());
		footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).build());
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
