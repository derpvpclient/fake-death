package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Napoleon on a hill above the battlefield: cannons thunder far away, a drummer beats the march, two grenadiers of the
 * Guard stand by the map table. Napoleon watches through his telescope, a marshal at his side. "Sire! The enemy cannon
 * is aimed straight at him!" "What do we do, Sire?" Then the battle goes quiet, the colours fade, a sad tune begins.
 * Napoleon slowly lowers his telescope, turns to the player, and says it calmly: "There is nothing we can do." The
 * soldiers lower their heads. Far away the cannon fires, and as the ball comes whistling down, they all salute.
 */
public class NapoleonScene extends CastScene {
	private static final int SPOT = ARRIVE + 6;
	private static final int WARN = ARRIVE + 30;
	private static final int ASK = ARRIVE + 82;
	private static final int MUSIC = ARRIVE + 104;
	private static final int TURN = ARRIVE + 112;
	private static final int NOTHING = ARRIVE + 140;
	/** The soldiers lower their heads, Napoleon folds his hands. */
	private static final int BOW = NOTHING + 40;
	private static final int FIRE = ARRIVE + 205;
	/** Everybody salutes the player as the ball comes down. */
	private static final int SALUTE = FIRE + 22;
	private static final int FLY = 60;
	private static final int HIT = FIRE + FLY;
	/** Ticks between the far cannon shots of the battle. */
	private static final int SHOTS = 34;
	/** Ticks per eighth of the tune. */
	private static final int EIGHTH = 6;
	/** A sad tune of the mod's own in D minor over D minor, B flat, G minor and A: notes and lengths in eighths. */
	private static final int[][] TUNE = {
		{69, 4}, {74, 4}, {77, 6}, {76, 2},
		{74, 4}, {72, 4}, {70, 6}, {69, 2},
		{67, 4}, {70, 4}, {74, 6}, {72, 2},
		{69, 6}, {67, 2}, {65, 4}, {64, 4}};
	private static final int[] ROOTS = {38, 34, 31, 33};
	/** Soft chords under the tune, one to each bar: D minor, B flat, G minor, A. */
	private static final int[][] CHORDS = {{57, 62, 65}, {58, 62, 65}, {55, 58, 62}, {57, 61, 64}};
	private static final ChatFormatting FRANCE = ChatFormatting.BLUE;

	private final List<Integer> tuneSteps = new ArrayList<>();
	private final List<RemotePlayer> guards = new ArrayList<>();
	private final List<Display.BlockDisplay> drum = new ArrayList<>();
	private RemotePlayer napoleon;
	private RemotePlayer general;
	private RemotePlayer drummer;
	private Display.BlockDisplay ball;
	private Vec3 cannon;
	private Vec3 horizon;
	private Vec3 look;
	private Vec3 side;
	private boolean ownMusic = true;
	private int now;

	@Override
	public int length() {
		return HIT + 2;
	}

	@Override
	public int photoTick() {
		return NOTHING + 10;
	}

	@Override
	public int screenTint() {
		// Once the music begins the battle fades into an old, faded painting.
		if (now < MUSIC) {
			return 0;
		}
		int alpha = (int) (Math.min(1.0F, (now - MUSIC) / 40.0F) * 70.0F);
		return alpha << 24 | 0x4A3418;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.napoleon", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		now = tick;
		if (tick == 0) {
			setUp(level, player);
		}
		if (napoleon == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();
		Vec3 field = napoleon.getEyePosition().add(horizon.subtract(napoleon.position()).normalize().scale(6.0));

		if (tick < TURN) {
			// Looking out over the battlefield through the telescope.
			face(napoleon, field.add(0.0, 0.3, 0.0));
		} else if (tick < NOTHING) {
			// Turning slowly to the player.
			float p = (tick - TURN) / (float) (NOTHING - TURN);
			face(napoleon, field.lerp(eyes, p));
		} else if (tick < NOTHING + 24) {
			// A slow shake of the head.
			face(napoleon, eyes.add(side.scale(Math.sin((tick - NOTHING) * 0.4) * 0.6)));
		} else {
			face(napoleon, eyes);
		}
		face(general, tick < WARN ? napoleon.getEyePosition() : tick < ASK + 20 ? eyes : tick < BOW ? napoleon.getEyePosition()
			: tick < FIRE - 10 ? general.position().add(look.scale(-1.5)) : eyes);
		for (RemotePlayer guard : guards) {
			// Standing guard towards the field, then looking at the player, then heads down.
			face(guard, tick < NOTHING ? guard.getEyePosition().add(horizon.subtract(guard.position()).normalize().scale(5.0))
				: tick < BOW || tick >= SALUTE ? eyes : guard.position().add(look.scale(-1.2)));
		}
		face(drummer, tick < NOTHING ? drummer.getEyePosition().add(look.scale(4.0)) : tick < BOW || tick >= SALUTE ? eyes : drummer.position().add(look.scale(-1.2)));
		placeDrum();

		if (tick == SPOT) {
			pose(napoleon, Skins.Pose.RAISE);
		}
		if (tick == WARN) {
			speak(level, general, "fakedeath.napoleon.warn", FRANCE, 45);
			pose(general, Skins.Pose.POINT);
		}
		if (tick == ASK) {
			pose(general, null);
			speak(level, general, "fakedeath.napoleon.ask", FRANCE, 30);
		}
		if (tick == TURN) {
			// The telescope comes down.
			pose(napoleon, null);
		}
		if (tick == NOTHING) {
			speak(level, napoleon, "fakedeath.napoleon.nothing", FRANCE, 60);
		}
		if (tick == BOW) {
			// Hands folded, calm. Nothing to be done.
			hold(napoleon, ItemStack.EMPTY);
			pose(napoleon, Skins.Pose.RAUTE);
		}
		if (tick == FIRE - 10) {
			pose(general, Skins.Pose.SALUTE);
		}
		if (tick == SALUTE) {
			pose(napoleon, Skins.Pose.SALUTE);
			for (RemotePlayer guard : guards) {
				pose(guard, Skins.Pose.SALUTE);
			}
			pose(drummer, Skins.Pose.SALUTE);
		}

		if (tick < MUSIC) {
			battle(level, player, tick, eyes);
		}
		if (tick == MUSIC) {
			// The original song of the meme, if the player brings it in a resource pack of their own.
			ownMusic = !packMusic(level, eyes, "meme.napoleon");
		}
		if (tick >= MUSIC && ownMusic) {
			tune(level, tick - MUSIC, eyes);
		}

		if (tick == FIRE) {
			// Far away, the cannon fires.
			particles(level, ParticleTypes.LARGE_SMOKE, cannon.add(0.0, 1.0, 0.0), 30, 0.8, 0.05);
			particles(level, ParticleTypes.EXPLOSION, cannon.add(0.0, 1.0, 0.0), 3, 0.3, 0.0);
			sound(level, eyes.add(cannon.subtract(eyes).normalize().scale(8.0)), Looks.EXPLODE, SoundSource.HOSTILE, 2.5F, 0.5F);
			ball = blockDisplay(level, Looks.BLACK_CONCRETE, cannon, 32.0F);
		}
		if (ball != null && tick <= HIT) {
			// The ball in a high, slow arc right at the player's head, whistling lower and lower as it comes down.
			float p = (tick - FIRE) / (float) FLY;
			Vec3 at = cannon.add(0.0, 1.2, 0.0).lerp(eyes, p).add(0.0, Math.sin(p * Math.PI) * 9.0, 0.0);
			place(ball, at, new Vector3f(0.55F), new Quaternionf().rotateX(tick * 0.3F));
			level.addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, 0.0, 0.0, 0.0);
			if (p > 0.45F && tick % 3 == 0) {
				float fall = (p - 0.45F) / 0.55F;
				sound(level, eyes, SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.HOSTILE, 0.25F + fall * 0.5F, 1.9F - fall * 1.2F);
			}
		}
		if (tick < FIRE) {
			turnTowards(player, tick < WARN ? napoleon.getEyePosition() : tick < ASK + 10 ? general.getEyePosition() : napoleon.getEyePosition(), 0.08F);
		} else if (ball != null) {
			turnTowards(player, ball.position(), 0.12F);
		}
		if (tick == HIT) {
			sound(level, eyes, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5F, 0.6F);
			sound(level, eyes, Looks.EXPLODE, SoundSource.HOSTILE, 1.5F, 0.8F);
			particles(level, ParticleTypes.EXPLOSION, eyes, 4, 0.5, 0.1);
			cameraShake(0.5F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	/** The battle far away: cannon shots along the horizon with smoke, and the drummer beating the march. */
	private void battle(ClientLevel level, LocalPlayer player, int tick, Vec3 eyes) {
		if (tick % SHOTS == 8) {
			int shot = tick / SHOTS;
			Vec3 spot = onGround(level, player, horizon.add(side.scale(((shot * 7) % 11 - 5) * 2.4)).add(look.scale(-6.0 + (shot % 3) * 2.0)));
			particles(level, ParticleTypes.LARGE_SMOKE, spot.add(0.0, 1.0, 0.0), 14, 1.0, 0.04);
			particles(level, ParticleTypes.EXPLOSION, spot.add(0.0, 0.8, 0.0), 1, 0.2, 0.0);
			sound(level, eyes.add(spot.subtract(eyes).normalize().scale(10.0)), Looks.EXPLODE, SoundSource.HOSTILE, 1.0F, 0.45F + (shot % 3) * 0.05F);
		}
		// The march, once the drummer is in place: a roll and two beats, over and over.
		int beat = (tick - ARRIVE - 14) % 24;
		if (tick >= ARRIVE + 14 && (beat == 0 || beat == 2 || beat == 4 || beat == 12 || beat == 18)) {
			sound(level, drummer.position().add(0.0, 0.8, 0.0), SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.RECORDS, beat == 0 ? 0.7F : 0.5F, 1.0F);
		}
	}

	/** The tune on the harp, a soft flute an octave above it, soft chords and the bass on the first beat of every bar. */
	private void tune(ClientLevel level, int t, Vec3 at) {
		if (t % EIGHTH != 0) {
			return;
		}
		int step = t / EIGHTH;
		int note = tuneSteps.indexOf(step % 64);
		if (note >= 0) {
			float pitch = (float) Math.pow(2.0, (TUNE[note][0] - 66) / 12.0);
			sound(level, at, SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.RECORDS, 0.9F, pitch);
			sound(level, at, SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.RECORDS, 0.35F, pitch);
		}
		if (step % 8 == 0) {
			int bar = (step / 16) % ROOTS.length;
			for (int chordNote : CHORDS[bar]) {
				sound(level, at, SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.RECORDS, step % 16 == 0 ? 0.3F : 0.2F, (float) Math.pow(2.0, (chordNote - 66) / 12.0));
			}
			if (step % 16 == 0) {
				sound(level, at, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.RECORDS, 0.8F, (float) Math.pow(2.0, (ROOTS[bar] - 42) / 12.0));
			}
		}
	}

	/** The drum hangs at the drummer's hip, wherever he walks. */
	private void placeDrum() {
		float yaw = drummer.getYRot() * Mth.DEG_TO_RAD;
		Vec3 facing = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
		Vec3 hip = drummer.position().add(facing.scale(0.32)).add(0.0, 0.62, 0.0);
		Quaternionf turn = new Quaternionf().rotateY(-yaw);
		place(drum.get(0), hip, new Vector3f(0.36F, 0.26F, 0.36F), turn);
		place(drum.get(1), hip.add(0.0, 0.14, 0.0), new Vector3f(0.39F, 0.04F, 0.39F), turn);
		place(drum.get(2), hip.add(0.0, -0.14, 0.0), new Vector3f(0.39F, 0.04F, 0.39F), turn);
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		look = horizontalLook(player);
		side = look.yRot(Mth.HALF_PI);
		float tableYaw = (float) Math.atan2(look.x, look.z);
		napoleon = actor(level, "fakedeath.napoleon.name", "napoleon", false, onGround(level, player, player.position().add(look.scale(3.6)).add(side.scale(-0.6))));
		hold(napoleon, new ItemStack(Items.SPYGLASS));
		general = actor(level, "fakedeath.napoleon.general", "marshal", false, onGround(level, player, player.position().add(look.scale(4.2)).add(side.scale(1.2))));
		arrive(level, player, napoleon, 0);
		arrive(level, player, general, 5);
		// Two grenadiers of the Guard by the map table, a drummer on the other side.
		for (int i = 0; i < 2; i++) {
			RemotePlayer guard = extra(level, "grenadier", onGround(level, player, player.position().add(look.scale(5.1 + i * 0.3)).add(side.scale(-1.6 + i * 1.9))));
			arrive(level, player, guard, 9 + i * 3);
			guards.add(guard);
		}
		drummer = extra(level, "grenadier", onGround(level, player, player.position().add(look.scale(4.8)).add(side.scale(2.7))));
		arrive(level, player, drummer, 14);
		drum.add(blockDisplay(level, Looks.WHITE_CONCRETE, drummer.position(), 4.0F));
		drum.add(blockDisplay(level, Looks.RED_WOOL, drummer.position(), 4.0F));
		drum.add(blockDisplay(level, Looks.RED_WOOL, drummer.position(), 4.0F));
		placeDrum();
		standFlag(level, player, player.position().add(look.scale(5.2)).add(side.scale(-2.4)), "flag_france", 1.4F);
		// The map table: a dark top on two legs, the map of the battle spread out on it.
		Vec3 table = onGround(level, player, player.position().add(look.scale(3.2)).add(side.scale(-2.1)));
		Quaternionf tableTurn = new Quaternionf().rotateY(tableYaw);
		place(blockDisplay(level, Blocks.DARK_OAK_PLANKS.defaultBlockState(), table, 4.0F), table.add(0.0, 0.82, 0.0), new Vector3f(1.2F, 0.08F, 0.8F), tableTurn);
		for (int s : new int[] {-1, 1}) {
			Vec3 leg = table.add(side.scale(s * 0.45)).add(0.0, 0.39, 0.0);
			place(blockDisplay(level, Blocks.DARK_OAK_PLANKS.defaultBlockState(), leg, 4.0F), leg, new Vector3f(0.1F, 0.78F, 0.1F), tableTurn);
		}
		place(itemDisplay(level, new ItemStack(Items.FILLED_MAP), table.add(0.0, 0.87, 0.0)), table.add(0.0, 0.87, 0.0), new Vector3f(0.7F),
			new Quaternionf().rotateY(tableYaw).rotateX(Mth.HALF_PI));
		// The enemy cannon far away on the field, half hidden in smoke.
		horizon = onGround(level, player, player.position().add(look.scale(30.0)).add(side.scale(9.0)));
		cannon = horizon;
		float yaw = (float) Math.atan2(-look.x, -look.z);
		place(blockDisplay(level, Looks.BLACK_CONCRETE, cannon, 32.0F), cannon.add(0.0, 1.0, 0.0), new Vector3f(0.5F, 0.5F, 2.0F), new Quaternionf().rotateY(yaw).rotateX(-0.25F));
		for (int s : new int[] {-1, 1}) {
			Vec3 wheel = cannon.add(look.yRot(Mth.HALF_PI).scale(s * 0.45)).add(0.0, 0.5, 0.0);
			place(blockDisplay(level, Blocks.DARK_OAK_PLANKS.defaultBlockState(), wheel, 8.0F), wheel, new Vector3f(0.15F, 1.0F, 1.0F), new Quaternionf().rotateY(yaw));
		}
		int t = 0;
		for (int[] note : TUNE) {
			tuneSteps.add(t);
			t += note[1];
		}
	}
}
