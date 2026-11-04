package de.derpvpclient.fakedeath;

import de.derpvpclient.fakedeath.scene.AnvilScene;
import de.derpvpclient.fakedeath.scene.BatScene;
import de.derpvpclient.fakedeath.scene.BeeScene;
import de.derpvpclient.fakedeath.scene.CaveInScene;
import de.derpvpclient.fakedeath.scene.CreeperScene;
import de.derpvpclient.fakedeath.scene.DeliveryScene;
import de.derpvpclient.fakedeath.scene.DiscoScene;
import de.derpvpclient.fakedeath.scene.DoppelgangerScene;
import de.derpvpclient.fakedeath.scene.DrownedScene;
import de.derpvpclient.fakedeath.scene.EvokerScene;
import de.derpvpclient.fakedeath.scene.FrogScene;
import de.derpvpclient.fakedeath.scene.GhastScene;
import de.derpvpclient.fakedeath.scene.GhostScene;
import de.derpvpclient.fakedeath.scene.GiantFrogScene;
import de.derpvpclient.fakedeath.scene.GiantItemScene;
import de.derpvpclient.fakedeath.scene.GiantScene;
import de.derpvpclient.fakedeath.scene.GlitchScene;
import de.derpvpclient.fakedeath.scene.GoatScene;
import de.derpvpclient.fakedeath.scene.HazardScene;
import de.derpvpclient.fakedeath.scene.HerobrineScene;
import de.derpvpclient.fakedeath.scene.LightningScene;
import de.derpvpclient.fakedeath.scene.MeteorScene;
import de.derpvpclient.fakedeath.scene.MomScene;
import de.derpvpclient.fakedeath.scene.ParrotScene;
import de.derpvpclient.fakedeath.scene.PhantomScene;
import de.derpvpclient.fakedeath.scene.PlayerKillScene;
import de.derpvpclient.fakedeath.scene.RaidScene;
import de.derpvpclient.fakedeath.scene.Scene;
import de.derpvpclient.fakedeath.scene.SilverfishScene;
import de.derpvpclient.fakedeath.scene.SkeletonScene;
import de.derpvpclient.fakedeath.scene.SlendermanScene;
import de.derpvpclient.fakedeath.scene.StalactiteScene;
import de.derpvpclient.fakedeath.scene.TaxAuditScene;
import de.derpvpclient.fakedeath.scene.TreeScene;
import de.derpvpclient.fakedeath.scene.WolfScene;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Looks at what the player is doing and what is around them, and picks a fake death that fits. */
public final class Situations {
	public static final List<String> NAMES = List.of(
		"baum", "hoehle", "blitz", "lava", "kaktus", "beeren", "schnee", "hunger", "feuer",
		"spieler", "riese", "skelett", "creeper", "finanzamt", "herobrine", "slenderman",
		"tropfstein", "bienen", "phantom", "amboss", "unsichtbar", "geister",
		"doppelgaenger", "ertrunkener", "woelfe", "froesche", "riesenfrosch", "silberfische", "ziege", "magier", "ueberfall", "meteorit", "ghast", "fledermaeuse", "riesenitem", "paketbote", "mama", "papagei", "glitch", "disco");

	/** English scene names, both work in every language. Names that are the same in both are left out. */
	private static final Map<String, String> ENGLISH = Map.ofEntries(
		Map.entry("giant", "riese"), Map.entry("tree", "baum"), Map.entry("cave", "hoehle"), Map.entry("lightning", "blitz"), Map.entry("cactus", "kaktus"),
		Map.entry("berries", "beeren"), Map.entry("snow", "schnee"), Map.entry("fire", "feuer"),
		Map.entry("player", "spieler"), Map.entry("skeleton", "skelett"), Map.entry("taxoffice", "finanzamt"),
		Map.entry("stalactite", "tropfstein"), Map.entry("bees", "bienen"), Map.entry("anvil", "amboss"), Map.entry("invisible", "unsichtbar"), Map.entry("ghosts", "geister"),
		Map.entry("doppelganger", "doppelgaenger"), Map.entry("drowned", "ertrunkener"), Map.entry("wolves", "woelfe"), Map.entry("frogs", "froesche"), Map.entry("giantfrog", "riesenfrosch"),
		Map.entry("silverfish", "silberfische"), Map.entry("goat", "ziege"), Map.entry("evoker", "magier"), Map.entry("raid", "ueberfall"), Map.entry("meteor", "meteorit"),
		Map.entry("bats", "fledermaeuse"), Map.entry("giantitem", "riesenitem"), Map.entry("delivery", "paketbote"), Map.entry("mom", "mama"), Map.entry("parrot", "papagei"));

	public static final List<String> ENGLISH_NAMES = NAMES.stream()
		.map(name -> ENGLISH.entrySet().stream().filter(entry -> entry.getValue().equals(name)).map(Map.Entry::getKey).findFirst().orElse(name))
		.toList();

	/** The ids stay plain for config files, what players type and see is spelled right. */
	private static final Map<String, String> UMLAUTS = Map.of(
		"hoehle", "höhle", "doppelgaenger", "doppelgänger", "fledermaeuse", "fledermäuse", "froesche", "frösche", "ueberfall", "überfall");

	/** Scenes with fast flashing lights, they only play with that setting switched on. */
	public static final java.util.Set<String> FLASHING = java.util.Set.of("glitch", "disco");

	public static final List<String> GERMAN_NAMES = NAMES.stream().map(name -> UMLAUTS.getOrDefault(name, name)).toList();

	/** The German scene id for a German or English name, with or without umlauts, or null if there is no such scene. */
	public static String resolve(String name) {
		String lower = name.trim().toLowerCase(Locale.ROOT);
		String id = ENGLISH.getOrDefault(lower, lower);

		for (Map.Entry<String, String> entry : UMLAUTS.entrySet()) {
			if (entry.getValue().equals(id)) {
				id = entry.getKey();
			}
		}

		return NAMES.contains(id) ? id : null;
	}

	private static final int RECENT_TICKS = 5 * 20;

	private static long lastBreakTime;
	private static BlockPos lastBreakPos;
	private static BlockState lastBreakState;

	private Situations() {
	}

	public static void onBlockBroken(ClientLevel level, BlockPos pos, BlockState state) {
		lastBreakTime = level.getGameTime();
		lastBreakPos = pos.immutable();
		lastBreakState = state;
	}

	public static void reset() {
		lastBreakPos = null;
		lastBreakState = null;
	}

	/** A moment that only fits right now, like a tree being cut down. The random event waits for one of these. */
	public static Scene special(ClientLevel level, LocalPlayer player) {
		if (lastBreakPos == null || level.getGameTime() - lastBreakTime > RECENT_TICKS) {
			return null;
		}

		TreeScene tree = FakeDeathConfig.isEnabled("baum") ? TreeScene.afterBreak(level, lastBreakPos, lastBreakState) : null;

		if (tree != null) {
			return tree;
		}

		if (lastBreakState.is(BlockTags.BASE_STONE_OVERWORLD)) {
			// Digging in stone: the cave comes down, or silverfish come out of it.
			boolean cave = FakeDeathConfig.isEnabled("hoehle") && !level.canSeeSky(player.blockPosition().above());
			boolean silverfish = FakeDeathConfig.isEnabled("silberfische");

			if (cave && (!silverfish || ThreadLocalRandom.current().nextBoolean())) {
				return new CaveInScene();
			}
			if (silverfish) {
				return new SilverfishScene(lastBreakPos);
			}
		}

		return null;
	}

	public static Scene pick(ClientLevel level, LocalPlayer player) {
		Scene special = special(level, player);

		if (special != null) {
			return special;
		}

		BlockPos feet = player.blockPosition();
		BlockPos head = feet.above();
		boolean sky = level.canSeeSky(head);
		boolean dark = sky ? level.isNight() : level.getMaxLocalRawBrightness(head) <= 7;
		Choices options = new Choices();

		if (level.isThundering() && sky) {
			options.add("blitz", 6, LightningScene::new);
		}

		if (options.wants("lava")) {
			BlockPos lava = find(level, feet, 4, 2, state -> state.getFluidState().is(FluidTags.LAVA));
			if (lava != null) {
				options.add("lava", 5, () -> HazardScene.lava(lava));
			}
		}

		if (options.wants("kaktus")) {
			BlockPos cactus = find(level, feet, 1, 1, state -> state.is(Blocks.CACTUS));
			if (cactus != null) {
				options.add("kaktus", 5, () -> HazardScene.cactus(cactus));
			}
		}

		if (options.wants("beeren")) {
			BlockPos bush = find(level, feet, 1, 1, state -> state.is(Blocks.SWEET_BERRY_BUSH));
			if (bush != null) {
				options.add("beeren", 5, () -> HazardScene.berries(bush));
			}
		}

		if (options.wants("schnee")) {
			BlockPos snow = find(level, feet, 1, 1, state -> state.is(Blocks.POWDER_SNOW));
			if (snow != null) {
				options.add("schnee", 5, () -> HazardScene.freeze(snow));
			}
		}

		if (player.getFoodData().getFoodLevel() <= 6) {
			options.add("hunger", 3, HazardScene::starve);
		}

		if (level.dimension() == Level.NETHER) {
			options.add("feuer", 3, HazardScene::fire);
		}

		AbstractClientPlayer killer = nearestPlayer(level, player, 10.0);
		if (killer != null) {
			options.add("spieler", 4, () -> new PlayerKillScene(killer));
		}

		if (dark) {
			options.add("riese", 3, GiantScene::new);
			options.add("skelett", 3, SkeletonScene::new);
			options.add("herobrine", 1, HerobrineScene::new);

			if (sky && options.wants("slenderman") && find(level, head.above(2), 4, 3, state -> state.is(BlockTags.LEAVES)) != null) {
				options.add("slenderman", 3, SlendermanScene::new);
			}

			if (sky && level.isNight()) {
				options.add("phantom", 3, PhantomScene::new);
			}
		}

		// Places that bring their own danger are much more likely to get theirs.
		if (options.wants("tropfstein")) {
			StalactiteScene stalactite = StalactiteScene.above(level, player);
			if (stalactite != null) {
				options.add("tropfstein", 7, () -> stalactite);
			}
		}

		if (options.wants("bienen")) {
			BlockPos hive = find(level, feet, 6, 4, state -> state.is(BlockTags.BEEHIVES));
			if (hive != null) {
				options.add("bienen", 7, () -> new BeeScene(hive));
			}
		}

		if (sky) {
			options.add("amboss", 2, AnvilScene::new);
		}

		// Ghosts like places where souls are: the soul sand valley, soul sand or soul soil, otherwise the night.
		if (options.wants("geister")) {
			boolean souls = level.getBiome(feet).is(Biomes.SOUL_SAND_VALLEY)
				|| find(level, feet, 5, 2, state -> state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL)) != null;

			if (souls) {
				options.add("geister", 6, GhostScene::new);
			} else if (dark) {
				options.add("geister", 2, GhostScene::new);
			}
		}

		if (dark) {
			// Rare, a little more likely deep in a cave.
			options.add("doppelgaenger", sky ? 1 : 2, DoppelgangerScene::new);

			if (sky && options.wants("woelfe") && (isWoods(level, feet) || find(level, head.above(2), 4, 3, state -> state.is(BlockTags.LEAVES)) != null)) {
				options.add("woelfe", 4, WolfScene::new);
			}
		}

		if (options.wants("ertrunkener")) {
			BlockPos sea = find(level, feet, 6, 2, state -> state.getFluidState().is(FluidTags.WATER));
			if (sea != null) {
				options.add("ertrunkener", dark ? 4 : 1, () -> new DrownedScene(sea));
			}
		}

		// Frogs in the swamp, or wherever frogs live nearby.
		if (options.wants("froesche")) {
			boolean swamp = level.getBiome(feet).is(Biomes.SWAMP) || level.getBiome(feet).is(Biomes.MANGROVE_SWAMP);

			if (swamp || !level.getEntitiesOfClass(Frog.class, player.getBoundingBox().inflate(12.0)).isEmpty()) {
				options.add("froesche", swamp ? 7 : 5, FrogScene::new);
			}
		}

		if (sky && options.wants("riesenfrosch") && (level.getBiome(feet).is(Biomes.SWAMP) || level.getBiome(feet).is(Biomes.MANGROVE_SWAMP))) {
			options.add("riesenfrosch", 4, GiantFrogScene::new);
		}

		if (sky && options.wants("ziege") && (level.getBiome(feet).is(BiomeTags.IS_MOUNTAIN)
			|| !level.getEntitiesOfClass(Goat.class, player.getBoundingBox().inflate(16.0)).isEmpty())) {
			options.add("ziege", 5, GoatScene::new);
		}

		if (level.getBiome(feet).is(Biomes.DARK_FOREST) || level.getBiome(feet).is(Biomes.PALE_GARDEN)) {
			options.add("magier", 5, EvokerScene::new);
		}

		if (sky && options.wants("ueberfall") && !level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(32.0)).isEmpty()) {
			options.add("ueberfall", 4, RaidScene::new);
		}

		if (sky && level.isNight() && !level.isRaining()) {
			options.add("meteorit", 2, MeteorScene::new);
		}

		if (level.dimension() == Level.NETHER) {
			options.add("ghast", 5, GhastScene::new);
		}

		if (!sky && dark) {
			options.add("fledermaeuse", 3, BatScene::new);
		}

		// Only with something in hand and room above the head for it to grow.
		if (options.wants("riesenitem") && !GiantItemScene.heldBy(player).isEmpty() && hasRoomAbove(level, head, 3)) {
			options.add("riesenitem", 2, () -> new GiantItemScene(player));
		}

		// The characters stay rare, Mom only comes when it is really late.
		if (sky) {
			options.add("paketbote", 1, DeliveryScene::new);
		}

		if (MomScene.isLate()) {
			options.add("mama", 3, MomScene::new);
		}

		if (options.wants("papagei") && (level.getBiome(feet).is(BiomeTags.IS_JUNGLE)
			|| !level.getEntitiesOfClass(Parrot.class, player.getBoundingBox().inflate(16.0)).isEmpty())) {
			options.add("papagei", 5, ParrotScene::new);
		}

		// Fast flashing lights only with the setting switched on.
		if (FakeDeathConfig.flashing) {
			options.add("glitch", 1, GlitchScene::new);
			boolean music = options.wants("disco") && find(level, feet, 8, 3, state -> state.is(Blocks.JUKEBOX)) != null;
			options.add("disco", music ? 6 : 1, DiscoScene::new);
		}

		options.add("creeper", 3, CreeperScene::new);
		options.add("unsichtbar", 1, () -> new CreeperScene(true));
		options.add("finanzamt", 2, TaxAuditScene::new);

		return options.roll();
	}

	/** The scenes that fit right now, each with a weight. Scenes switched off in the settings never get in. */
	private static final class Choices {
		private final List<Option> options = new ArrayList<>();

		boolean wants(String id) {
			return FakeDeathConfig.isEnabled(id);
		}

		void add(String id, int weight, Supplier<Scene> scene) {
			if (wants(id)) {
				options.add(new Option(weight, scene));
			}
		}

		/** One of the scenes, more likely the heavier ones. Null if every fitting scene is switched off. */
		Scene roll() {
			int total = options.stream().mapToInt(Option::weight).sum();

			if (total == 0) {
				return null;
			}

			int roll = ThreadLocalRandom.current().nextInt(total);

			for (Option option : options) {
				roll -= option.weight();

				if (roll < 0) {
					return option.scene().get();
				}
			}

			return null;
		}
	}

	/** A specific scene for testing. Null when the world has nothing it needs, like a tree. */
	public static Scene create(String name, ClientLevel level, LocalPlayer player) {
		BlockPos feet = player.blockPosition();
		String id = resolve(name);

		if (id == null) {
			return null;
		}

		// Never flashing lights without the setting, not even for a test or the demo run.
		if (FLASHING.contains(id) && !FakeDeathConfig.flashing) {
			return null;
		}

		return switch (id) {
			case "baum" -> {
				Scene special = special(level, player);
				yield special instanceof TreeScene ? special : TreeScene.near(level, player);
			}
			case "hoehle" -> new CaveInScene();
			case "blitz" -> new LightningScene();
			case "lava" -> HazardScene.lava(orFeet(find(level, feet, 4, 2, state -> state.getFluidState().is(FluidTags.LAVA)), feet));
			case "kaktus" -> HazardScene.cactus(orFeet(find(level, feet, 2, 1, state -> state.is(Blocks.CACTUS)), feet));
			case "beeren" -> HazardScene.berries(orFeet(find(level, feet, 2, 1, state -> state.is(Blocks.SWEET_BERRY_BUSH)), feet));
			case "schnee" -> HazardScene.freeze(orFeet(find(level, feet, 2, 1, state -> state.is(Blocks.POWDER_SNOW)), feet));
			case "hunger" -> HazardScene.starve();
			case "feuer" -> HazardScene.fire();
			case "spieler" -> {
				AbstractClientPlayer killer = nearestPlayer(level, player, 32.0);
				yield killer != null ? new PlayerKillScene(killer) : PlayerKillScene.stranger();
			}
			case "riese" -> new GiantScene();
			case "skelett" -> new SkeletonScene();
			case "creeper" -> new CreeperScene();
			case "finanzamt" -> new TaxAuditScene();
			case "herobrine" -> new HerobrineScene();
			case "slenderman" -> new SlendermanScene();
			case "tropfstein" -> StalactiteScene.anywhere(level, player);
			case "bienen" -> new BeeScene(find(level, feet, 8, 4, state -> state.is(BlockTags.BEEHIVES)));
			case "phantom" -> new PhantomScene();
			case "amboss" -> new AnvilScene();
			case "unsichtbar" -> new CreeperScene(true);
			case "geister" -> new GhostScene();
			case "doppelgaenger" -> new DoppelgangerScene();
			case "ertrunkener" -> new DrownedScene(find(level, feet, 8, 3, state -> state.getFluidState().is(FluidTags.WATER)));
			case "woelfe" -> new WolfScene();
			case "froesche" -> new FrogScene();
			case "riesenfrosch" -> new GiantFrogScene();
			case "silberfische" -> new SilverfishScene(lastBreakPos != null ? lastBreakPos : feet.below());
			case "ziege" -> new GoatScene();
			case "magier" -> new EvokerScene();
			case "ueberfall" -> new RaidScene();
			case "meteorit" -> new MeteorScene();
			case "ghast" -> new GhastScene();
			case "fledermaeuse" -> new BatScene();
			case "riesenitem" -> new GiantItemScene(player);
			case "paketbote" -> new DeliveryScene();
			case "mama" -> new MomScene();
			case "papagei" -> new ParrotScene();
			case "glitch" -> new GlitchScene();
			case "disco" -> new DiscoScene();
			default -> null;
		};
	}

	private static boolean hasRoomAbove(ClientLevel level, BlockPos head, int blocks) {
		for (int i = 1; i <= blocks; i++) {
			if (!level.getBlockState(head.above(i)).isAir()) {
				return false;
			}
		}
		return true;
	}

	private static boolean isWoods(ClientLevel level, BlockPos feet) {
		var biome = level.getBiome(feet);
		return biome.is(Biomes.FOREST) || biome.is(Biomes.DARK_FOREST) || biome.is(Biomes.TAIGA)
			|| biome.is(Biomes.SNOWY_TAIGA) || biome.is(Biomes.OLD_GROWTH_PINE_TAIGA);
	}

	private static BlockPos orFeet(BlockPos found, BlockPos feet) {
		return found != null ? found : feet;
	}

	private static BlockPos find(ClientLevel level, BlockPos center, int radius, int vertical, Predicate<BlockState> test) {
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -vertical, -radius), center.offset(radius, vertical, radius))) {
			if (test.test(level.getBlockState(pos))) {
				return pos.immutable();
			}
		}

		return null;
	}

	private static AbstractClientPlayer nearestPlayer(ClientLevel level, LocalPlayer self, double range) {
		AbstractClientPlayer nearest = null;

		for (AbstractClientPlayer other : level.players()) {
			// Negative ids are the scenes' own actors, like Herobrine.
			if (other == self || other.getId() < 0 || other.isSpectator() || !other.isAlive()) {
				continue;
			}

			if (other.distanceTo(self) <= range && (nearest == null || other.distanceTo(self) < nearest.distanceTo(self))) {
				nearest = other;
			}
		}

		return nearest;
	}

	private record Option(int weight, Supplier<Scene> scene) {
	}
}
