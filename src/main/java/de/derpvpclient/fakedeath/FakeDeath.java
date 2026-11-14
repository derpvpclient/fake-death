package de.derpvpclient.fakedeath;

import de.derpvpclient.fakedeath.scene.Scene;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The whole fake death runs on the client only: a short scene, the vanilla death screen, a death
 * message in the local chat and the camera roll. Nothing is sent to the server, the player stays alive.
 */
public final class FakeDeath {
	private static final int RETRY_TICKS = 5 * 20;
	private static final int SAFE_TICKS = 2 * 20;
	private static final int ARMED_TICKS = 60 * 20;
	private static final double MONSTER_RADIUS = 16.0;

	private static Scene scene;
	private static int sceneTick;
	private static int outroTick = -1;
	private static boolean killed;
	/** What went wrong in the last scene that failed, for the demo run. Null if nothing did. */
	public static String lastError;
	private static CameraType cameraBefore;
	private static boolean active;
	private static int deathTicks;
	private static float startHealth;
	// The real health and absorption of the last tick: any drop is a real hit, even after regeneration.
	private static float watchedHealth;
	private static float watchedAbsorption;
	// Where the scene started. A portal or a teleport takes the player away from everything it built.
	private static ClientLevel sceneLevel;
	private static Vec3 sceneStart;
	private static final double LEASH = 12.0;
	private static final int FIGHT_TICKS = 10 * 20;
	private static int fightTicks;
	private static float fakeHealth = -1.0F;
	private static int fakeFood = -1;
	private static int safeTicks;
	private static final int WAIT_NOTICE = 5 * 20;
	private static int pendingWaited;
	private static boolean monsterNearby = true;
	private static int pendingTicks = -1;
	// The screen color as it is really shown, eased when flashing effects are off.
	private static int shownTint;
	private static String pendingScene;
	private static int randomTicks = -1;
	private static int armedTicks = -1;

	private FakeDeath() {
	}

	/** True while the fake death screen is open. */
	public static boolean isActive() {
		return active;
	}

	public static int deathTicks() {
		return deathTicks;
	}

	/** What the hearts show: the fake health while a scene runs, otherwise the real one. */
	public static float displayedHealth(float realHealth) {
		return fakeHealth >= 0.0F ? fakeHealth : realHealth;
	}

	/** What the hunger bar shows: a fake food level while a scene sets one, otherwise the real one. */
	public static int displayedFood(int realFood) {
		return fakeFood >= 0 ? fakeFood : realFood;
	}

	public static float displayedSaturation(float realSaturation) {
		return fakeFood >= 0 ? 0.0F : realSaturation;
	}

	/** True while a scene plays: the player stands still until the death screen and the reveal. */
	public static boolean freezesMovement() {
		return scene != null && outroTick < 0;
	}

	/** True while the running scene has taken the item out of this hand, so the first-person view hides it. */
	public static boolean hidesHeldItem(InteractionHand hand) {
		return scene != null && scene.hiddenHand() == hand;
	}

	/** The scene that is playing right now, or null. */
	public static Scene currentScene() {
		return scene;
	}

	public static int currentSceneTick() {
		return sceneTick;
	}

	/** Starts a scene right away, without waiting for a safe moment. Only for the demo run. */
	static void startNow(Minecraft minecraft, Scene next) {
		lastError = null;
		begin(minecraft, next);
	}

	/** The color the running scene lays over the world, 0 for none. Gone with the reveal. */
	public static int screenTint() {
		int wanted = scene != null && outroTick < 0 ? scene.screenTint() : 0;
		return FakeDeathConfig.flashing ? wanted : shownTint;
	}

	/**
	 * With flashing effects switched off no scene may flash: every color over the screen fades in and out slowly instead
	 * of jumping (a lightning flash, a camera flash, an explosion), and the sky does not light up for lightning either.
	 * One place for all scenes, so a new scene can never forget it.
	 */
	private static void calmFlashes(ClientLevel level) {
		int wanted = scene != null && outroTick < 0 ? scene.screenTint() : 0;
		if (FakeDeathConfig.flashing) {
			shownTint = wanted;
			return;
		}
		int result = 0;
		for (int shift = 0; shift < 32; shift += 8) {
			int from = shownTint >>> shift & 0xFF;
			int to = wanted >>> shift & 0xFF;
			// Alpha at most 6 a tick, a full fade takes two seconds. The color follows a little faster.
			int step = shift == 24 ? 6 : 14;
			int next = from + Math.max(-step, Math.min(step, to - from));
			result |= next << shift;
		}
		shownTint = result;
		if (scene != null) {
			level.setSkyFlashTime(0);
		}
	}

	/** Lets the hunger bar show a different level until the fake is over. */
	public static void setFakeFood(int food) {
		fakeFood = Math.max(0, food);
	}

	/** A hit in a scene takes the given share of the health the player had when it started, but never the last half heart. */
	public static void fakeDamage(LocalPlayer player, float share) {
		if (fakeHealth < 0.0F) {
			return;
		}

		fakeHealth = Math.max(1.0F, fakeHealth - startHealth * share);
		// Makes the hearts blink like on real damage. Only the client reads it for that.
		player.invulnerableTime = 10;
	}

	/** The killing blow of a scene: no hearts left, and the death screen opens at the end of this tick. */
	public static void killNow(LocalPlayer player) {
		if (scene == null || active) {
			return;
		}

		fakeHealth = 0.0F;
		player.invulnerableTime = 10;
		killed = true;
	}

	/**
	 * Starts a fake death after the given delay, or as soon after as the player stands safely. Always at least
	 * one tick, so a closing chat screen does not close the death screen again. {@code sceneName} null picks one that fits.
	 */
	public static void schedule(int ticks, String sceneName) {
		pendingTicks = Math.max(1, ticks);
		pendingScene = sceneName;
	}

	public static boolean cancelPending() {
		boolean had = pendingTicks >= 0;
		pendingTicks = -1;
		pendingScene = null;
		return had;
	}

	/** The player hit something real or was hit: the random event stays away from a running fight. */
	public static void noteFight() {
		fightTicks = FIGHT_TICKS;
	}

	/** For the game test: the random event is due in this many ticks. */
	public static void randomDueIn(int ticks) {
		armedTicks = -1;
		randomTicks = ticks;
	}

	/** Ticks until the random event is due, -1 while it is off or already waiting for its moment. */
	public static int randomTicksLeft() {
		return randomTicks;
	}

	/** How long the random timer was set for, so chopping a tree can pull the event forward once half of it has run down. */
	private static int randomSpan = -1;

	/**
	 * The player cut into a tree. Once the random event is at least half due, it comes right now: the tree falling on
	 * them while they chop it is the best moment there is.
	 */
	public static void onTreeCut() {
		if (FakeDeathConfig.randomEvent && scene == null && randomTicks > 0 && randomSpan > 0 && randomTicks <= randomSpan / 2) {
			randomTicks = 0;
		}
	}

	public static void rollRandomTimer() {
		armedTicks = -1;

		if (!FakeDeathConfig.randomEvent) {
			randomTicks = -1;
			return;
		}

		int min = Math.clamp(FakeDeathConfig.randomMinMinutes, 1, FakeDeathConfig.MAX_MINUTES) * 1200;
		int max = Math.max(min, Math.clamp(FakeDeathConfig.randomMaxMinutes, 1, FakeDeathConfig.MAX_MINUTES) * 1200);
		randomTicks = ThreadLocalRandom.current().nextInt(min, max + 1);
		randomSpan = randomTicks;
	}

	public static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		ClientLevel level = minecraft.level;

		if (player == null || level == null) {
			// The world is gone, and with it everything the scene put into it.
			scene = null;
			sceneLevel = null;
			sceneStart = null;
			active = false;
			killed = false;
			sceneTick = 0;
			deathTicks = 0;
			outroTick = -1;
			fakeHealth = -1.0F;
			fakeFood = -1;
			pendingTicks = -1;
			pendingScene = null;
			pendingWaited = 0;
			safeTicks = 0;
			fightTicks = 0;
			shownTint = 0;
			monsterNearby = true;
			Situations.reset();
			CameraDirector.reset();
			restoreCamera(minecraft);
			return;
		}

		if (player.hurtTime > 0) {
			noteFight();
		} else if (fightTicks > 0) {
			fightTicks--;
		}

		CameraDirector.startTick();
		tickInner(minecraft, level, player);
		CameraDirector.endTick(scene != null && outroTick < 0);
		calmFlashes(level);
		Demo.tick(minecraft, level, player);
	}

	private static void tickInner(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
		if (scene != null) {
			tickScene(minecraft, level, player);
			return;
		}

		// Only look around while a fake death is waiting for its moment. The rest of the time the mod does nothing per tick.
		boolean waiting = pendingTicks >= 0 || armedTicks >= 0;
		// Monsters nearby only hold back the random event. Started by a command, the fake also plays at night with
		// mobs around; should one really hit, the scene ends right away anyway.
		String reason = waiting ? unsafeReason(level, player, pendingTicks < 0) : null;
		// No open screen either: closing an inventory or chest would tell the server.
		if (reason == null && minecraft.gui.screen() != null) {
			reason = "menu";
		}
		safeTicks = waiting && reason == null ? safeTicks + 1 : 0;
		boolean safe = safeTicks >= SAFE_TICKS;

		// A planned fake death waits until the player stands safely, however long that takes.
		if (pendingTicks > 0) {
			pendingTicks--;
			pendingWaited = 0;
		} else if (pendingTicks == 0 && !safe && reason != null && ++pendingWaited == WAIT_NOTICE) {
			// Tell the player once what it is waiting for, so nobody thinks the scene is broken.
			FakeDeathClient.LOGGER.info("Fake death {} is waiting: {}", pendingScene != null ? pendingScene : "(fitting scene)", reason);
			minecraft.gui.hud.getChat().addClientSystemMessage(Component.literal("[Fake Death] ").withStyle(ChatFormatting.DARK_RED)
				.append(Component.translatable("fakedeath.message.waiting", Component.translatable("fakedeath.wait." + reason))
					.withStyle(ChatFormatting.GRAY)));
		} else if (pendingTicks == 0 && safe) {
			String name = pendingScene;
			pendingTicks = -1;
			pendingScene = null;
			Scene next = name != null ? choose(() -> Situations.create(name, level, player)) : choose(() -> Situations.pick(level, player));

			if (next != null) {
				begin(minecraft, next);
			} else {
				Component why = name != null ? Component.translatable("fakedeath.message.missing", name) : Component.translatable("fakedeath.message.all_off");
				minecraft.gui.hud.getChat().addClientSystemMessage(Component.literal("[Fake Death] ").withStyle(ChatFormatting.DARK_RED)
					.append(why.copy().withStyle(ChatFormatting.GRAY)));
			}
		}

		if (randomTicks >= 0 && !Demo.isRunning() && !minecraft.isPaused() && --randomTicks < 0) {
			armedTicks = 0;
		}

		// Once armed, the event waits up to a minute for a moment that fits, like cutting a tree. Never while a scene
		// asked for by a command waits or has just started, that one comes first.
		if (armedTicks >= 0 && !minecraft.isPaused() && scene == null && pendingTicks < 0) {
			armedTicks++;

			if (safe) {
				Scene next = choose(() -> Situations.special(level, player));

				boolean timeUp = armedTicks >= ARMED_TICKS;

				if (next == null && timeUp) {
					next = choose(() -> Situations.pick(level, player));
				}

				// Started, or nothing fits because every fitting scene is switched off: either way wait for the next round.
				if (next != null) {
					begin(minecraft, next);
					rollRandomTimer();
				} else if (timeUp) {
					rollRandomTimer();
				}
			}
		}
	}

	/** Picking a scene looks at the world around the player. If that ever fails, there is just no scene this time. */
	private static Scene choose(Supplier<Scene> choice) {
		try {
			return choice.get();
		} catch (RuntimeException e) {
			FakeDeathClient.LOGGER.error("Could not pick a fake death scene", e);
			lastError = "pick: " + e;
			return null;
		}
	}

	/** True if the player lost real health or absorption hearts to a hit since the last tick. */
	private static boolean reallyHurt(LocalPlayer player) {
		float health = player.getHealth();
		float absorption = player.getAbsorptionAmount();
		// Absorption also runs out on its own, that is no hit.
		boolean hurt = health < watchedHealth - 0.01F || absorption < watchedAbsorption - 0.01F && player.hurtTime > 0;
		watchedHealth = health;
		watchedAbsorption = absorption;
		return hurt;
	}

	private static void tickScene(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
		// Through a portal or teleported away: the scene stayed behind, and nothing of it belongs into the new place.
		if (level != sceneLevel || sceneStart != null && player.position().distanceToSqr(sceneStart) > LEASH * LEASH) {
			FakeDeathClient.LOGGER.info("The player left the place of the fake death, the scene ends");
			boolean onDeathScreen = active;
			finish(level, player);
			if (onDeathScreen && minecraft.gui.screen() instanceof DeathScreen) {
				minecraft.gui.setScreen(null);
			}
			return;
		}

		// A real death takes over, the fake is over then.
		if (player.isDeadOrDying()) {
			finish(level, player);
			return;
		}

		boolean hurt = reallyHurt(player);

		// Real damage while the player stands frozen in a scene: the fake ends at once, so they can defend themselves.
		if (!active && outroTick < 0 && hurt) {
			FakeDeathClient.LOGGER.info("Real damage during a fake death, the scene ends");
			finish(level, player);
			return;
		}

		// The same on the fake death screen: a mob hitting the player while they sit there must not get a free kill.
		if (active && hurt) {
			FakeDeathClient.LOGGER.info("Real damage on the fake death screen, back to the game");
			reveal(minecraft);
			return;
		}

		try {
			scene.tick(minecraft, level, player, sceneTick++);
		} catch (RuntimeException e) {
			// A broken scene must never take the game down with it, it just ends.
			FakeDeathClient.LOGGER.error("Scene {} failed at tick {}", scene.getClass().getSimpleName(), sceneTick - 1, e);
			lastError = scene.getClass().getSimpleName() + ": " + e;
			boolean onDeathScreen = active;
			finish(level, player);
			if (onDeathScreen) {
				minecraft.gui.setScreen(null);
			}
			return;
		}

		// After the reveal some scenes keep their actors around a little, like the tax officer saying goodbye.
		if (outroTick >= 0) {
			if (++outroTick >= scene.outroLength()) {
				finish(level, player);
			}
			return;
		}

		if (!active) {
			if (killed || sceneTick >= scene.length()) {
				die(minecraft, level, player);
			}
			return;
		}

		deathTicks++;

		// Any other screen ends it too, for example when the server opens something.
		if (!(minecraft.gui.screen() instanceof DeathScreen)) {
			finish(level, player);
		}
	}

	private static void begin(Minecraft minecraft, Scene next) {
		de.derpvpclient.fakedeath.scene.Scene.resetWatch();
		FakeDeathClient.LOGGER.info("Fake death scene starts: {}", next.getClass().getSimpleName());
		scene = next;
		sceneTick = 0;
		outroTick = -1;
		killed = false;
		safeTicks = 0;
		sceneLevel = minecraft.level;
		sceneStart = null;
		CameraDirector.release(false);

		if (minecraft.player != null) {
			startHealth = minecraft.player.getHealth();
			fakeHealth = startHealth;
			watchedHealth = startHealth;
			watchedAbsorption = minecraft.player.getAbsorptionAmount();
			sceneStart = minecraft.player.position();
		}

		// The scenes are filmed from the player's eyes.
		cameraBefore = minecraft.options.getCameraType();
		if (!cameraBefore.isFirstPerson()) {
			minecraft.options.setCameraType(CameraType.FIRST_PERSON);
		}
	}

	private static void restoreCamera(Minecraft minecraft) {
		if (cameraBefore != null) {
			minecraft.options.setCameraType(cameraBefore);
			cameraBefore = null;
		}
	}

	private static void die(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
		FakeDeathClient.LOGGER.info("Fake death scene {} reached the death screen after {} ticks", scene.getClass().getSimpleName(), sceneTick);
		Component message;
		try {
			message = scene.deathMessage(player);
		} catch (RuntimeException e) {
			FakeDeathClient.LOGGER.error("Scene {} has no death message", scene.getClass().getSimpleName(), e);
			lastError = scene.getClass().getSimpleName() + ": " + e;
			finish(level, player);
			return;
		}

		// Whatever the scene still plays stops with the death, only the killing blow of the last moment may ring out.
		de.derpvpclient.fakedeath.scene.Scene.stopSounds(level.getGameTime() - 6);
		level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_DEATH, SoundSource.PLAYERS, 1.0F, 1.0F, false);
		minecraft.gui.hud.getChat().addServerSystemMessage(message);
		minecraft.gui.setScreen(new DeathScreen(message, level.getLevelData().isHardcore(), player));

		active = true;
		killed = false;
		deathTicks = 0;
		fakeHealth = 0.0F;
		player.invulnerableTime = 10;
	}

	private static void finish(ClientLevel level, LocalPlayer player) {
		if (scene != null) {
			try {
				// Always into the world the scene was built in. After a change of dimension that is no longer the shown one,
				// and its blocks must not be put into the new world.
				scene.end(sceneLevel != null ? sceneLevel : level, player);
			} catch (RuntimeException e) {
				FakeDeathClient.LOGGER.error("Scene {} could not clean up", scene.getClass().getSimpleName(), e);
				lastError = scene.getClass().getSimpleName() + ": " + e;
			}
		}

		de.derpvpclient.fakedeath.scene.Scene.stopSounds(Long.MAX_VALUE);
		scene = null;
		sceneLevel = null;
		sceneStart = null;
		active = false;
		outroTick = -1;
		fakeHealth = -1.0F;
		fakeFood = -1;
		restoreCamera(Minecraft.getInstance());
	}

	/**
	 * Why the player is not safe right now, as the last part of a translation key, or null if they are: solid ground,
	 * nothing hurting them and, if asked, no monster close by, so the fake cannot turn into a real death.
	 */
	private static String unsafeReason(ClientLevel level, LocalPlayer player, boolean checkMonsters) {
		if (player.isInLava()) {
			return "lava";
		}
		if (player.isInWater()) {
			return "water";
		}
		if (player.isOnFire()) {
			return "fire";
		}
		if (player.isPassenger()) {
			return "riding";
		}
		if (player.isFallFlying() || player.getAbilities().flying) {
			return "flying";
		}
		if (player.isSleeping()) {
			return "sleeping";
		}
		if (!player.onGround() || player.fallDistance > 0.0 || player.isDeadOrDying()) {
			return "air";
		}
		// Standing in a cactus or a berry bush hurts all the time.
		if (player.hurtTime > 0) {
			return "hurt";
		}
		// A portal may send the player away at any moment, and the scene would stay behind.
		if (inPortal(level, player)) {
			return "portal";
		}
		// Up in a tree no scene looks right: the bushes, people and animals of the scenes would end up in the leaves.
		if (onTree(level, player)) {
			return "tree";
		}

		if (!checkMonsters) {
			return null;
		}

		// In the middle of a fight nobody wants to stand frozen.
		if (fightTicks > 0) {
			return "fight";
		}

		// Searching for monsters is the expensive part, twice a second is enough for that.
		if (level.getGameTime() % 10 == 0) {
			AABB area = player.getBoundingBox().inflate(MONSTER_RADIUS);
			monsterNearby = !level.getEntities(player, area, entity -> entity instanceof Enemy && entity.isAlive() && entity.getId() >= 0).isEmpty();
		}

		return monsterNearby ? "monster" : null;
	}

	private static boolean inPortal(ClientLevel level, LocalPlayer player) {
		net.minecraft.world.level.block.state.BlockState feet = level.getBlockState(player.blockPosition());
		return feet.is(net.minecraft.world.level.block.Blocks.NETHER_PORTAL) || feet.is(net.minecraft.world.level.block.Blocks.END_PORTAL)
			|| feet.is(net.minecraft.world.level.block.Blocks.END_GATEWAY);
	}

	/** Standing on leaves, or on a log with leaves around it. A floor made of logs, like in a cabin, does not count. */
	private static boolean onTree(ClientLevel level, LocalPlayer player) {
		net.minecraft.core.BlockPos below = player.getOnPos();
		net.minecraft.world.level.block.state.BlockState ground = level.getBlockState(below);
		if (ground.is(net.minecraft.tags.BlockTags.LEAVES)) {
			return true;
		}
		if (!ground.is(net.minecraft.tags.BlockTags.LOGS)) {
			return false;
		}
		for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(below.offset(-2, -1, -2), below.offset(2, 2, 2))) {
			if (level.getBlockState(pos).is(net.minecraft.tags.BlockTags.LEAVES)) {
				return true;
			}
		}
		return false;
	}

	/** Called instead of respawning or leaving to the title screen while the fake is running. */
	public static void reveal(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		LocalPlayer player = minecraft.player;

		active = false;
		fakeHealth = -1.0F;
		fakeFood = -1;
		CameraDirector.release(true);

		if (scene != null && level != null && player != null) {
			int outro = 0;
			try {
				scene.onReveal(minecraft, level, player);
				outro = scene.outroLength();
			} catch (RuntimeException e) {
				FakeDeathClient.LOGGER.error("Scene {} failed at the reveal", scene.getClass().getSimpleName(), e);
				lastError = scene.getClass().getSimpleName() + ": " + e;
			}

			if (outro > 0) {
				outroTick = 0;
			} else {
				finish(level, player);
			}
		}

		minecraft.gui.setScreen(null);
	}
}
