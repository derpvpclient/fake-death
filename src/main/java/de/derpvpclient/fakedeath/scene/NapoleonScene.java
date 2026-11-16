package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.particles.BlockParticleOption;
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
 * Napoleon on a hill above the battlefield: out on the field cannons thunder and the earth flies up, a drummer beats
 * the march, two grenadiers of the Guard in their bearskins stand by the map table. Napoleon in his bicorne watches
 * through his telescope, his marshal at his side. "Sire! The enemy cannon is aimed straight at him!" "What do we do,
 * Sire?" Then the battle goes quiet, the colours fade, a sad tune begins. Napoleon slowly lowers his telescope, turns
 * to the player and says it calmly: "There is nothing we can do." The soldiers lower their heads. The enemy cannon
 * fires, and as the ball comes whistling down, they all salute.
 */
public class NapoleonScene extends CastScene {
	private static final int SPOT = ARRIVE + 6;
	private static final int WARN = ARRIVE + 30;
	/** The view follows the marshal's arm out to the enemy cannon. */
	private static final int SHOW = WARN + 22;
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
	/** Ticks between the shots out on the battlefield. */
	private static final int SHOTS = 17;
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
	private final List<Display.BlockDisplay> ball = new ArrayList<>();
	private RemotePlayer napoleon;
	private RemotePlayer general;
	private RemotePlayer drummer;
	/** The enemy cannon out on the field, the mouth of its barrel, and the middle of the battlefield. */
	private Vec3 cannon;
	private Vec3 muzzle;
	private Vec3 field;
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
		Vec3 aim = cannon.add(0.0, 1.0, 0.0);

		if (tick < TURN) {
			// Looking out at the enemy cannon through the telescope.
			face(napoleon, aim);
		} else if (tick < NOTHING) {
			// Turning slowly to the player.
			float p = (tick - TURN) / (float) (NOTHING - TURN);
			face(napoleon, aim.lerp(eyes, p));
		} else if (tick < NOTHING + 24) {
			// A slow shake of the head.
			face(napoleon, eyes.add(side.scale(Math.sin((tick - NOTHING) * 0.4) * 0.6)));
		} else {
			face(napoleon, eyes);
		}
		// The marshal points out the cannon, looks at the player it is aimed at, asks his emperor, bows his head, salutes.
		face(general, tick < WARN ? napoleon.getEyePosition() : tick < SHOW + 6 ? aim : tick < ASK ? eyes : tick < BOW ? napoleon.getEyePosition()
			: tick < FIRE - 10 ? general.position().add(look.scale(-1.5)) : eyes);
		for (RemotePlayer guard : guards) {
			// Watching the battle, then looking at the player, then heads down.
			face(guard, tick < NOTHING ? aim.add(0.0, 1.0, 0.0) : tick < BOW || tick >= SALUTE ? eyes : guard.position().add(look.scale(-1.2)));
		}
		face(drummer, tick < NOTHING ? field.add(0.0, 2.0, 0.0) : tick < BOW || tick >= SALUTE ? eyes : drummer.position().add(look.scale(-1.2)));
		placeDrum();

		if (tick == SPOT) {
			pose(napoleon, Skins.Pose.RAISE);
		}
		if (tick == WARN) {
			speak(level, general, "fakedeath.napoleon.warn", FRANCE, 45);
			pose(general, Skins.Pose.POINT);
		}
		if (tick == SHOW + 6) {
			pose(general, null);
		}
		if (tick == ASK) {
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
			// The enemy cannon fires: a flash at its mouth, a cloud of smoke, the boom rolling over the hill.
			particles(level, ParticleTypes.EXPLOSION, muzzle, 3, 0.3, 0.0);
			particles(level, ParticleTypes.FLAME, muzzle, 20, 0.3, 0.08);
			particles(level, ParticleTypes.LARGE_SMOKE, muzzle, 40, 0.9, 0.06);
			sound(level, eyes.add(cannon.subtract(eyes).normalize().scale(6.0)), Looks.EXPLODE, SoundSource.HOSTILE, 3.0F, 0.5F);
			// Three cubes turned against each other look like a round iron ball.
			for (int i = 0; i < 3; i++) {
				ball.add(blockDisplay(level, Looks.BLACK_CONCRETE, muzzle, 32.0F));
			}
		}
		Vec3 at = null;
		if (!ball.isEmpty() && tick <= HIT) {
			// The ball in a high, slow arc right at the player's head, whistling lower and lower as it comes down.
			float p = (tick - FIRE) / (float) FLY;
			at = muzzle.lerp(eyes, p).add(0.0, Math.sin(p * Math.PI) * 8.0, 0.0);
			Quaternionf spin = new Quaternionf().rotateX(tick * 0.3F);
			place(ball.get(0), at, new Vector3f(0.5F), spin);
			place(ball.get(1), at, new Vector3f(0.5F), new Quaternionf(spin).rotateY(Mth.PI / 4.0F));
			place(ball.get(2), at, new Vector3f(0.5F), new Quaternionf(spin).rotateZ(Mth.PI / 4.0F).rotateX(Mth.PI / 4.0F));
			level.addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, 0.0, 0.0, 0.0);
			if (p > 0.45F && tick % 3 == 0) {
				float fall = (p - 0.45F) / 0.55F;
				sound(level, eyes, SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.HOSTILE, 0.25F + fall * 0.5F, 1.9F - fall * 1.2F);
			}
		}

		// Where the view goes: out over the battle, to the marshal, along his arm to the cannon, back to the people,
		// to the cannon as it fires and then up with the ball until it comes down.
		if (tick < WARN) {
			turnTowards(player, field.add(0.0, 1.5, 0.0), 0.05F);
		} else if (tick < SHOW) {
			turnTowards(player, general.getEyePosition(), 0.1F);
		} else if (tick < ASK) {
			turnTowards(player, aim, 0.08F);
		} else if (tick < ASK + 24) {
			turnTowards(player, general.getEyePosition(), 0.08F);
		} else if (tick < FIRE - 4) {
			turnTowards(player, napoleon.getEyePosition(), 0.08F);
		} else if (at == null || tick < FIRE + 8) {
			turnTowards(player, aim, 0.3F);
		} else if (tick < HIT - 4) {
			turnTowards(player, at, 0.25F);
		}
		if (tick == HIT) {
			sound(level, eyes, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5F, 0.6F);
			sound(level, eyes, Looks.EXPLODE, SoundSource.HOSTILE, 1.5F, 0.8F);
			particles(level, ParticleTypes.EXPLOSION, eyes, 4, 0.5, 0.1);
			cameraShake(0.5F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	/** The battle out on the field: shells striking all over it with fire, smoke and earth flying, and the march. */
	private void battle(ClientLevel level, LocalPlayer player, int tick, Vec3 eyes) {
		if (tick % SHOTS == 5) {
			int shot = tick / SHOTS;
			// Spread over the field, never twice in the same spot.
			double across = ((shot * 7) % 11 - 5) * 1.6;
			double ahead = ((shot * 5) % 7 - 3) * 2.2;
			Vec3 spot = onGround(level, player, field.add(side.scale(across)).add(look.scale(ahead)));
			particles(level, ParticleTypes.EXPLOSION, spot.add(0.0, 0.6, 0.0), 2, 0.4, 0.0);
			particles(level, ParticleTypes.FLAME, spot.add(0.0, 0.4, 0.0), 8, 0.4, 0.06);
			particles(level, ParticleTypes.LARGE_SMOKE, spot.add(0.0, 1.0, 0.0), 22, 1.0, 0.05);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), spot.add(0.0, 0.5, 0.0), 40, 0.6, 0.4);
			sound(level, eyes.add(spot.subtract(eyes).normalize().scale(8.0)), Looks.EXPLODE, SoundSource.HOSTILE, 1.2F, 0.45F + (shot % 3) * 0.05F);
		}
		if (tick % 4 == 0) {
			// Smoke hanging over the field, and the enemy cannon smoking.
			Vec3 drift = field.add(side.scale((random.nextDouble() - 0.5) * 18.0)).add(look.scale((random.nextDouble() - 0.5) * 10.0));
			level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, drift.x, drift.y + 0.5, drift.z, 0.0, 0.03, 0.0);
			level.addParticle(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 0.0, 0.02, 0.0);
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

	/** A spot on the hill: so far ahead of the player, so far to the side (to the left when positive). */
	private Vec3 spot(ClientLevel level, LocalPlayer player, double ahead, double across) {
		return onGround(level, player, player.position().add(look.scale(ahead)).add(side.scale(across)));
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		look = horizontalLook(player);
		side = look.yRot(Mth.HALF_PI);
		// The battlefield lies ahead to the left, the people stand to the right, so nobody hides the battle.
		field = spot(level, player, 20.0, 6.0);
		cannon = spot(level, player, 17.0, 4.5);
		napoleon = actor(level, "fakedeath.napoleon.name", "napoleon", false, spot(level, player, 3.4, -0.5));
		hold(napoleon, new ItemStack(Items.SPYGLASS));
		wear(napoleon, Looks.prop(Items.IRON_NUGGET, "bicorne"), 0.22);
		general = actor(level, "fakedeath.napoleon.general", "marshal", false, spot(level, player, 4.0, -1.7));
		wear(general, Looks.prop(Items.IRON_NUGGET, "bicorne_marshal"), 0.28);
		arrive(level, player, napoleon, 0);
		arrive(level, player, general, 5);
		// Two grenadiers of the Guard by the map table, a drummer next to them.
		for (int i = 0; i < 2; i++) {
			RemotePlayer guard = extra(level, "grenadier", spot(level, player, 5.4 + i * 0.3, -1.1 - i * 1.5));
			wear(guard, Looks.prop(Items.IRON_NUGGET, "bearskin"), 0.0);
			arrive(level, player, guard, 9 + i * 3);
			guards.add(guard);
		}
		drummer = extra(level, "grenadier", spot(level, player, 4.6, -3.6));
		wear(drummer, Looks.prop(Items.IRON_NUGGET, "bearskin"), 0.0);
		arrive(level, player, drummer, 14);
		drum.add(blockDisplay(level, Looks.WHITE_CONCRETE, drummer.position(), 4.0F));
		drum.add(blockDisplay(level, Looks.RED_WOOL, drummer.position(), 4.0F));
		drum.add(blockDisplay(level, Looks.RED_WOOL, drummer.position(), 4.0F));
		placeDrum();
		standFlag(level, player, spot(level, player, 6.4, -2.0), "flag_france", 1.4F);
		// The map table: a dark top on two legs, the map of the battle spread out on it.
		Vec3 table = spot(level, player, 2.8, -2.8);
		float tableYaw = (float) Math.atan2(look.x, look.z);
		Quaternionf tableTurn = new Quaternionf().rotateY(tableYaw);
		place(blockDisplay(level, Blocks.DARK_OAK_PLANKS.defaultBlockState(), table, 4.0F), table.add(0.0, 0.82, 0.0), new Vector3f(1.2F, 0.08F, 0.8F), tableTurn);
		for (int s : new int[] {-1, 1}) {
			Vec3 leg = table.add(side.scale(s * 0.45)).add(0.0, 0.39, 0.0);
			place(blockDisplay(level, Blocks.DARK_OAK_PLANKS.defaultBlockState(), leg, 4.0F), leg, new Vector3f(0.1F, 0.78F, 0.1F), tableTurn);
		}
		place(itemDisplay(level, new ItemStack(Items.FILLED_MAP), table.add(0.0, 0.87, 0.0)), table.add(0.0, 0.87, 0.0), new Vector3f(0.7F),
			new Quaternionf().rotateY(tableYaw).rotateX(Mth.HALF_PI));
		// The enemy cannon out on the field, its barrel raised and aimed at the player, and two more further off.
		muzzle = cannon(level, cannon, player.position(), 1.0F);
		cannon(level, spot(level, player, 24.0, 11.0), player.position(), 0.8F);
		cannon(level, spot(level, player, 26.0, 1.5), player.position(), 0.8F);
		int t = 0;
		for (int[] note : TUNE) {
			tuneSteps.add(t);
			t += note[1];
		}
	}

	/** A field cannon: a black barrel on a wooden carriage with two big wheels. Gives the mouth of the barrel. */
	private Vec3 cannon(ClientLevel level, Vec3 at, Vec3 target, float size) {
		Vec3 toTarget = target.subtract(at);
		Vec3 ahead = new Vec3(toTarget.x, 0.0, toTarget.z).normalize();
		Vec3 across = ahead.yRot(Mth.HALF_PI);
		float yaw = (float) Math.atan2(ahead.x, ahead.z);
		float raise = 0.3F;
		Quaternionf turn = new Quaternionf().rotateY(yaw);
		Quaternionf barrelTurn = new Quaternionf().rotateY(yaw).rotateX(-raise);
		Vec3 axle = at.add(0.0, 0.65 * size, 0.0);
		place(blockDisplay(level, Looks.BLACK_CONCRETE, at, 32.0F), axle.add(0.0, 0.3 * size, 0.0), new Vector3f(0.6F * size, 0.6F * size, 2.6F * size), barrelTurn);
		// The carriage and its trail resting on the ground behind.
		place(blockDisplay(level, Blocks.DARK_OAK_PLANKS.defaultBlockState(), at, 32.0F), axle.add(ahead.scale(-0.9 * size)).add(0.0, -0.3 * size, 0.0),
			new Vector3f(0.45F * size, 0.25F * size, 1.8F * size), new Quaternionf().rotateY(yaw).rotateX(-0.35F));
		for (int s : new int[] {-1, 1}) {
			place(blockDisplay(level, Blocks.DARK_OAK_PLANKS.defaultBlockState(), at, 32.0F), axle.add(across.scale(s * 0.48 * size)),
				new Vector3f(0.15F * size, 1.3F * size, 1.3F * size), turn);
			place(blockDisplay(level, Blocks.DARK_OAK_PLANKS.defaultBlockState(), at, 32.0F), axle.add(across.scale(s * 0.48 * size)),
				new Vector3f(0.15F * size, 1.3F * size, 1.3F * size), new Quaternionf(turn).rotateX(Mth.PI / 4.0F));
		}
		return axle.add(0.0, 0.3 * size, 0.0).add(ahead.scale(1.3 * size * Math.cos(raise))).add(0.0, 1.3 * size * Math.sin(raise), 0.0);
	}
}
