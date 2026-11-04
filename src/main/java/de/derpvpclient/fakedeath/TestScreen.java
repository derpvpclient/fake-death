package de.derpvpclient.fakedeath;

import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Every scene as a button, for testing at a glance. A click closes the screen and plays the scene as soon as the
 * player stands safely, just like {@code /fakedeath test <scene>}. Opened with {@code /fakedeath test} and from the settings.
 */
public class TestScreen extends Screen {
	private static final int COLUMNS = 3;
	private static final int BUTTON_WIDTH = 120;
	private static boolean openNextTick;

	private final Screen parent;
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
	private ScrollableLayout scroll;

	public TestScreen(Screen parent) {
		super(Component.translatable("fakedeath.test.title"));
		this.parent = parent;
	}

	/** From a command the chat closes after it runs, so the screen opens one tick later. */
	public static void openNextTick() {
		openNextTick = true;
	}

	static void tick(Minecraft minecraft) {
		if (openNextTick && minecraft.screen == null) {
			openNextTick = false;
			minecraft.setScreen(new TestScreen(null));
		}
	}

	@Override
	protected void init() {
		layout.addTitleHeader(title, font);

		GridLayout grid = new GridLayout().columnSpacing(4).rowSpacing(4);
		GridLayout.RowHelper rows = grid.createRowHelper(COLUMNS);

		// Sorted by the name the player reads, in their language.
		List<String> scenes = Situations.NAMES.stream()
			.sorted(Comparator.comparing(id -> Component.translatable("fakedeath.scene." + id).getString()))
			.toList();

		for (String id : scenes) {
			rows.addChild(sceneButton(id));
		}

		scroll = new ScrollableLayout(minecraft, grid, layout.getContentHeight());
		layout.addToContents(scroll);

		LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(8));
		footer.addChild(Button.builder(Component.translatable("fakedeath.test.fitting"), button -> start(null))
			.width(BUTTON_WIDTH).tooltip(Tooltip.create(Component.translatable("fakedeath.test.fitting.tooltip"))).build());
		footer.addChild(Button.builder(Component.translatable("fakedeath.test.cancel"), button -> {
			FakeDeath.cancelPending();
			onClose();
		}).width(BUTTON_WIDTH).build());
		footer.addChild(Button.builder(Component.translatable("gui.done"), button -> onClose()).width(BUTTON_WIDTH).build());

		layout.visitWidgets(this::addRenderableWidget);
		repositionElements();
	}

	private Button sceneButton(String id) {
		Component name = Component.translatable("fakedeath.scene." + id);
		boolean flashingOff = Situations.FLASHING.contains(id) && !FakeDeathConfig.flashing;
		boolean switchedOff = !FakeDeathConfig.isEnabled(id);

		Button button = Button.builder(switchedOff ? name.copy().withStyle(ChatFormatting.GRAY) : name, b -> start(id))
			.width(BUTTON_WIDTH)
			.tooltip(Tooltip.create(tooltip(id, flashingOff, switchedOff)))
			.build();
		// Flashing scenes stay locked until the setting allows them.
		button.active = !flashingOff;
		return button;
	}

	private static Component tooltip(String id, boolean flashingOff, boolean switchedOff) {
		String command = "/fakedeath test " + Situations.GERMAN_NAMES.get(Situations.NAMES.indexOf(id));
		if (flashingOff) {
			return Component.translatable("fakedeath.test.locked");
		}
		if (switchedOff) {
			return Component.translatable("fakedeath.test.switched_off", command);
		}
		return Component.literal(command);
	}

	private void start(String id) {
		minecraft.setScreen(null);
		FakeDeath.schedule(0, id);
		Component scene = id != null ? Component.translatable("fakedeath.scene." + id) : Component.translatable("fakedeath.test.fitting");
		minecraft.gui.getChat().addMessage(Component.literal("[Fake Death] ").withStyle(ChatFormatting.DARK_RED)
			.append(Component.translatable("fakedeath.command.test", scene).withStyle(ChatFormatting.GRAY)));
	}

	@Override
	protected void repositionElements() {
		if (scroll != null) {
			scroll.setMaxHeight(layout.getContentHeight());
		}
		layout.arrangeElements();
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}
}
