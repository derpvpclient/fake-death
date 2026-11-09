package de.derpvpclient.fakedeath;

import de.derpvpclient.fakedeath.scene.Scene;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;

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

	public static void rollRandomTimer() {
		armedTicks = -1;

		if (!FakeDeathConfig.randomEvent) {
			randomTicks = -1;
			return;
		}

		int min = FakeDeathConfig.randomMinMinutes * 1200;
		int max = Math.max(min, FakeDeathConfig.randomMaxMinutes * 1200);
		randomTicks = ThreadLocalRandom.current().nextInt(min, max + 1);
	}

	public static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		ClientLevel level = minecraft.level;

		if (player == null || level == null) {
			// The world is gone, and with it everything the scene put into it.
			scene = null;
			active = false;
			outroTick = -1;
			fakeHealth = -1.0F;
			fakeFood = -1;
			pendingTicks = -1;
			Situations.reset();
			CameraDirector.reset();
			restoreCamera(minecraft);
			return;
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
		if (reason == null && minecraft.screen != null) {
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
			minecraft.gui.getChat().addMessage(Component.literal("[Fake Death] ").withStyle(ChatFormatting.DARK_RED)
				.append(Component.translatable("fakedeath.message.waiting", Component.translatable("fakedeath.wait." + reason))
					.withStyle(ChatFormatting.GRAY)));
		} else if (pendingTicks == 0 && safe) {
			String name = pendingScene;
			pendingTicks = -1;
			pendingScene = null;
			Scene next = name != null ? Situations.create(name, level, player) : Situations.pick(level, player);

			if (next != null) {
				begin(minecraft, next);
			} else {
				Component why = name != null ? Component.translatable("fakedeath.message.missing", name) : Component.translatable("fakedeath.message.all_off");
				minecraft.gui.getChat().addMessage(Component.literal("[Fake Death] ").withStyle(ChatFormatting.DARK_RED)
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
				Scene next = Situations.special(level, player);

				boolean timeUp = armedTicks >= ARMED_TICKS;

				if (next == null && timeUp) {
					next = Situations.pick(level, player);
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

	private static void tickScene(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
		// A real death takes over, the fake is over then.
		if (player.isDeadOrDying()) {
			finish(level, player);
			return;
		}

		// Real damage while the player stands frozen in a scene: the fake ends at once, so they can defend themselves.
		if (!active && outroTick < 0 && player.getHealth() < startHealth - 0.01F) {
			FakeDeathClient.LOGGER.info("Real damage during a fake death, the scene ends");
			finish(level, player);
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
				minecraft.setScreen(null);
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
		if (!(minecraft.screen instanceof DeathScreen)) {
			finish(level, player);
		}
	}

	private static void begin(Minecraft minecraft, Scene next) {
		FakeDeathClient.LOGGER.info("Fake death scene starts: {}", next.getClass().getSimpleName());
		scene = next;
		sceneTick = 0;
		outroTick = -1;
		killed = false;
		safeTicks = 0;
		CameraDirector.release(false);

		if (minecraft.player != null) {
			startHealth = minecraft.player.getHealth();
			fakeHealth = startHealth;
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
		Component message = scene.deathMessage(player);

		level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_DEATH, SoundSource.PLAYERS, 1.0F, 1.0F, false);
		minecraft.gui.getChat().addMessage(message);
		minecraft.setScreen(new DeathScreen(message, level.getLevelData().isHardcore(), player));

		active = true;
		killed = false;
		deathTicks = 0;
		fakeHealth = 0.0F;
		player.invulnerableTime = 10;
	}

	private static void finish(ClientLevel level, LocalPlayer player) {
		if (scene != null) {
			scene.end(level, player);
		}

		scene = null;
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
		// Up in a tree no scene looks right: the bushes, people and animals of the scenes would end up in the leaves.
		if (onTree(level, player)) {
			return "tree";
		}

		if (!checkMonsters) {
			return null;
		}

		// Searching for monsters is the expensive part, twice a second is enough for that.
		if (level.getGameTime() % 10 == 0) {
			AABB area = player.getBoundingBox().inflate(MONSTER_RADIUS);
			monsterNearby = !level.getEntities(player, area, entity -> entity instanceof Enemy && entity.isAlive() && entity.getId() >= 0).isEmpty();
		}

		return monsterNearby ? "monster" : null;
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
			scene.onReveal(minecraft, level, player);

			if (scene.outroLength() > 0) {
				outroTick = 0;
			} else {
				finish(level, player);
			}
		}

		minecraft.setScreen(null);
	}
}
