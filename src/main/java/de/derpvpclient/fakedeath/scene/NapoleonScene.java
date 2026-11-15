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
 * Napoleon on the battlefield, his telescope at the horizon, a general at his side. "Sire! The enemy cannon is aimed
 * straight at him!" "What do we do, Sire?" A sad tune begins. Napoleon slowly lowers his telescope, turns to the
 * player, and says it calmly: "There is nothing we can do." Far away the cannon fires. The ball comes, slowly.
 */
public class NapoleonScene extends CastScene {
	private static final int SPOT = ARRIVE + 6;
	private static final int WARN = ARRIVE + 30;
	private static final int ASK = ARRIVE + 82;
	private static final int MUSIC = ARRIVE + 104;
	private static final int TURN = ARRIVE + 112;
	private static final int NOTHING = ARRIVE + 140;
	private static final int FIRE = ARRIVE + 205;
	private static final int FLY = 54;
	private static final int HIT = FIRE + FLY;
	/** Ticks per eighth of the tune. */
	private static final int EIGHTH = 6;
	/** A sad tune of the mod's own in D minor over D minor, B flat, G minor and A: notes and lengths in eighths. */
	private static final int[][] TUNE = {
		{69, 4}, {74, 4}, {77, 6}, {76, 2},
		{74, 4}, {72, 4}, {70, 6}, {69, 2},
		{67, 4}, {70, 4}, {74, 6}, {72, 2},
		{69, 6}, {67, 2}, {65, 4}, {64, 4}};
	private static final int[] ROOTS = {38, 34, 31, 33};
	private static final ChatFormatting FRANCE = ChatFormatting.BLUE;

	private final List<Integer> tuneSteps = new ArrayList<>();
	private RemotePlayer napoleon;
	private RemotePlayer general;
	private Display.BlockDisplay ball;
	private Vec3 cannon;
	private Vec3 horizon;

	@Override
	public int length() {
		return HIT + 2;
	}

	@Override
	public int photoTick() {
		return NOTHING + 10;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.napoleon", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (napoleon == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();

		if (tick < TURN) {
			// Looking out over the battlefield through the telescope.
			face(napoleon, napoleon.getEyePosition().add(horizon.subtract(napoleon.position()).normalize().scale(6.0)).add(0.0, 0.3, 0.0));
		} else if (tick < NOTHING) {
			// Turning slowly to the player.
			float p = (tick - TURN) / (float) (NOTHING - TURN);
			Vec3 away = napoleon.getEyePosition().add(horizon.subtract(napoleon.position()).normalize().scale(6.0));
			face(napoleon, away.lerp(eyes, p));
		} else if (tick < NOTHING + 24) {
			// A slow shake of the head.
			Vec3 side = horizontalLook(player).yRot(Mth.HALF_PI);
			face(napoleon, eyes.add(side.scale(Math.sin((tick - NOTHING) * 0.4) * 0.6)));
		} else {
			face(napoleon, eyes);
		}
		face(general, tick < WARN ? napoleon.getEyePosition() : tick < ASK + 20 ? eyes : napoleon.getEyePosition());

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
		if (tick >= MUSIC) {
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
			// The ball in a high, slow arc right at the player's head.
			float p = (tick - FIRE) / (float) FLY;
			Vec3 at = cannon.add(0.0, 1.2, 0.0).lerp(eyes, p).add(0.0, Math.sin(p * Math.PI) * 9.0, 0.0);
			place(ball, at, new Vector3f(0.55F), new Quaternionf().rotateX(tick * 0.3F));
			level.addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, 0.0, 0.0, 0.0);
			if (tick % 10 == 0) {
				sound(level, at, SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 1.5F, 0.4F);
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

	/** The tune on the harp, a soft flute an octave above it, the bass on the first beat of every bar. */
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
		if (step % 16 == 0) {
			int root = ROOTS[(step / 16) % ROOTS.length];
			sound(level, at, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.RECORDS, 0.8F, (float) Math.pow(2.0, (root - 42) / 12.0));
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		Vec3 look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		napoleon = actor(level, "fakedeath.napoleon.name", "napoleon", false, onGround(level, player, player.position().add(look.scale(3.6)).add(side.scale(-0.6))));
		hold(napoleon, new ItemStack(Items.SPYGLASS));
		general = actor(level, "fakedeath.napoleon.general", "general", false, onGround(level, player, player.position().add(look.scale(4.2)).add(side.scale(1.2))));
		arrive(level, player, napoleon, 0);
		arrive(level, player, general, 5);
		standFlag(level, player, player.position().add(look.scale(5.2)).add(side.scale(-2.4)), "flag_france", 1.4F);
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
