package de.derpvpclient.fakedeath;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The test screen: every scene as a button, sorted into tabs (nature, animals, monsters, characters) and with a search
 * field. A click closes the screen and plays the scene as soon as the player stands safely, just like
 * {@code /fakedeath test <scene>}. Opened with {@code /fakedeath test} and from the settings.
 */
public class TestScreen extends Screen {
	private static final int BUTTON_WIDTH = 118;
	private static final int FOOTER_WIDTH = 88;
	private static final List<String> CATEGORIES = List.of("all", "world", "animals", "monsters", "characters");
	private static final Map<String, List<String>> SORTED = Map.of(
		"animals", List.of("bienen", "woelfe", "froesche", "riesenfrosch", "silberfische", "ziege", "fledermaeuse", "papagei", "delfin",
			"kamel", "fuchs", "huehner", "bienennest", "axolotl", "panda", "katzen", "allay", "ziegenchor"),
		"monsters", List.of("spieler", "riese", "skelett", "creeper", "unsichtbar", "herobrine", "slenderman", "phantom", "geister",
			"doppelgaenger", "ertrunkener", "magier", "ueberfall", "ghast", "enderman", "saugroboter"),
		"characters", List.of("finanzamt", "paketbote", "mama", "influencer", "rundfunk", "zahnarzt", "bauamt", "umzug", "elektriker",
			"zauberer", "klempner", "hausmeister", "arzt", "polizist", "schornsteinfeger", "trainer", "friseur", "koch", "fahrlehrer",
			"wahrsagerin", "weihnachtsmann", "werbung", "oma", "kellner", "navi", "bombenprank"));
	private static boolean openNextTick;
	// Remembered while the game runs, so the screen opens again on the same tab.
	private static String category = "all";
	private static String search = "";

	private final Screen parent;
	private HeaderAndFooterLayout layout;
	// No scrolling layout before 1.21.6, so the scenes are split into pages that fit the screen.
	private static int page;
	private List<String> shown = List.of();

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

	/** The tab a scene belongs to. Everything not sorted elsewhere is nature and world. */
	static String categoryOf(String id) {
		return SORTED.entrySet().stream().filter(entry -> entry.getValue().contains(id)).map(Map.Entry::getKey).findFirst().orElse("world");
	}

	@Override
	protected void init() {
		// A fresh layout every time, a tab or a search builds the screen anew.
		layout = new HeaderAndFooterLayout(this, 78, 33);
		shown = Situations.NAMES.stream()
			.filter(id -> category.equals("all") || categoryOf(id).equals(category))
			.filter(this::matches)
			.sorted(Comparator.comparing(id -> Component.translatable("fakedeath.scene." + id).getString()))
			.toList();

		// Header: the title, the tabs, and the search field with the number of scenes found.
		LinearLayout header = layout.addToHeader(LinearLayout.vertical().spacing(5));
		header.defaultCellSetting().alignHorizontallyCenter();
		header.addChild(new StringWidget(title, font));

		LinearLayout tabs = header.addChild(LinearLayout.horizontal().spacing(3));
		int tabWidth = Math.min(90, (width - 24) / CATEGORIES.size());
		for (String tab : CATEGORIES) {
			Component label = Component.translatable("fakedeath.test.tab." + tab);
			if (tab.equals(category)) {
				label = label.copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE);
			}
			tabs.addChild(Button.builder(label, button -> {
				category = tab;
				page = 0;
				rebuildWidgets();
			}).width(tabWidth).build());
		}

		LinearLayout searchRow = header.addChild(LinearLayout.horizontal().spacing(8));
		searchRow.defaultCellSetting().alignVerticallyMiddle();
		EditBox box = searchRow.addChild(new EditBox(font, Math.min(200, width - 120), 18, Component.translatable("fakedeath.test.search")));
		box.setHint(Component.translatable("fakedeath.test.search").withStyle(ChatFormatting.GRAY));
		box.setValue(search);
		box.setResponder(text -> {
			if (!text.equals(search)) {
				search = text;
				page = 0;
				rebuildWidgets();
			}
		});
		searchRow.addChild(new StringWidget(Component.translatable("fakedeath.test.count", shown.size(), Situations.NAMES.size()).withStyle(ChatFormatting.GRAY), font));

		// The scenes, as many columns as fit.
		int columns = Math.max(2, Math.min(4, (width - 20) / (BUTTON_WIDTH + 4)));
		GridLayout grid = new GridLayout().columnSpacing(4).rowSpacing(4);
		GridLayout.RowHelper rows = grid.createRowHelper(columns);
		// Under "all" the scenes come in groups, each with its heading: nature and world, animals, monsters, characters.
		// A heading takes a whole row, the empty cells only count for the paging.
		List<String> cells = new java.util.ArrayList<>();
		if (category.equals("all")) {
			for (String group : CATEGORIES.subList(1, CATEGORIES.size())) {
				List<String> ids = shown.stream().filter(id -> categoryOf(id).equals(group)).toList();
				if (ids.isEmpty()) {
					continue;
				}
				while (cells.size() % columns != 0) {
					cells.add("");
				}
				cells.add("#" + group);
				for (int i = 1; i < columns; i++) {
					cells.add("");
				}
				cells.addAll(ids);
			}
		} else {
			cells.addAll(shown);
		}
		int perPage = Math.max(1, (layout.getContentHeight() - 4) / 24) * columns;
		int pages = Math.max(1, (cells.size() + perPage - 1) / perPage);
		page = Math.min(page, pages - 1);
		for (String id : cells.subList(Math.min(cells.size(), page * perPage), Math.min(cells.size(), (page + 1) * perPage))) {
			if (id.startsWith("#")) {
				Component heading = Component.translatable("fakedeath.test.tab." + id.substring(1)).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
				rows.addChild(new StringWidget(heading, font), columns);
			} else if (!id.isEmpty()) {
				rows.addChild(sceneButton(id));
			}
		}
		if (shown.isEmpty()) {
			rows.addChild(new StringWidget(Component.translatable("fakedeath.test.none").withStyle(ChatFormatting.GRAY), font));
		}
		layout.addToContents(grid);

		LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(6));
		if (pages > 1) {
			Button back = footer.addChild(Button.builder(Component.literal("<"), button -> turn(-1)).width(20).build());
			back.active = page > 0;
		}
		footer.addChild(Button.builder(Component.translatable("fakedeath.test.fitting"), button -> start(null))
			.width(FOOTER_WIDTH).tooltip(Tooltip.create(Component.translatable("fakedeath.test.fitting.tooltip"))).build());
		Button random = footer.addChild(Button.builder(Component.translatable("fakedeath.test.random"), button -> startRandom())
			.width(FOOTER_WIDTH).tooltip(Tooltip.create(Component.translatable("fakedeath.test.random.tooltip"))).build());
		random.active = shown.stream().anyMatch(TestScreen::playable);
		footer.addChild(Button.builder(Component.translatable("fakedeath.test.cancel"), button -> {
			FakeDeath.cancelPending();
			onClose();
		}).width(FOOTER_WIDTH).build());
		footer.addChild(Button.builder(Component.translatable("gui.done"), button -> onClose()).width(FOOTER_WIDTH).build());
		if (pages > 1) {
			Button next = footer.addChild(Button.builder(Component.literal(">"), button -> turn(1)).width(20).build());
			next.active = page < pages - 1;
		}

		layout.visitWidgets(this::addRenderableWidget);
		repositionElements();
		setInitialFocus(box);
	}

	/** Matches the search text against the name in the player's language and the command names. */
	private boolean matches(String id) {
		String text = search.trim().toLowerCase(Locale.ROOT);
		if (text.isEmpty()) {
			return true;
		}
		int index = Situations.NAMES.indexOf(id);
		return Component.translatable("fakedeath.scene." + id).getString().toLowerCase(Locale.ROOT).contains(text)
			|| id.contains(text)
			|| Situations.GERMAN_NAMES.get(index).contains(text)
			|| Situations.ENGLISH_NAMES.get(index).contains(text);
	}

	private static boolean playable(String id) {
		return !Situations.FLASHING.contains(id) || FakeDeathConfig.flashing;
	}

	private Button sceneButton(String id) {
		Component name = Component.translatable("fakedeath.scene." + id);
		boolean flashingOff = !playable(id);
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
		Component tab = Component.translatable("fakedeath.test.tab." + categoryOf(id)).withStyle(ChatFormatting.GRAY);
		if (flashingOff) {
			return Component.translatable("fakedeath.test.locked");
		}
		if (switchedOff) {
			return Component.translatable("fakedeath.test.switched_off", command);
		}
		return Component.literal(command).append("\n").append(tab);
	}

	private void startRandom() {
		List<String> choices = shown.stream().filter(TestScreen::playable).toList();
		if (!choices.isEmpty()) {
			start(choices.get(new Random().nextInt(choices.size())));
		}
	}

	private void start(String id) {
		minecraft.setScreen(null);
		FakeDeath.schedule(0, id);
		Component scene = id != null ? Component.translatable("fakedeath.scene." + id) : Component.translatable("fakedeath.test.fitting");
		minecraft.gui.getChat().addMessage(Component.literal("[Fake Death] ").withStyle(ChatFormatting.DARK_RED)
			.append(Component.translatable("fakedeath.command.test", scene).withStyle(ChatFormatting.GRAY)));
	}

	private void turn(int pages) {
		page += pages;
		rebuildWidgets();
	}

	@Override
	protected void repositionElements() {
		if (layout != null) {
			layout.arrangeElements();
		}
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}
}
