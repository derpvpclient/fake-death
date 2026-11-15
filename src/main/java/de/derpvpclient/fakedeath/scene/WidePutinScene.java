package de.derpvpclient.fakedeath.scene;

import de.derpvpclient.fakedeath.mixin.DisplayAccessor;
import de.derpvpclient.fakedeath.mixin.TextDisplayAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The famous wide walk: a long red carpet, a guard of honour on both sides, golden doors at the far end. "IT'S HIM."
 * The doors swing open, a synth beat starts, and Vladimir Putin strides down the carpet, very slowly, very
 * importantly, and very, very wide. He does not stop. There is no room left for the player.
 */
public class WidePutinScene extends CastScene {
	private static final int DOORS = ARRIVE;
	private static final int TITLE = ARRIVE + 6;
	private static final int WALK = ARRIVE + 22;
	private static final double CARPET = 16.0;
	private static final double PACE = 0.11;
	private static final float WIDTH = 2.6F;
	/** Ticks per sixteenth of the beat. */
	private static final int STEP = 3;
	/** The bass, eighths, two notes to the bar of four chords: A minor, F, C, G. */
	private static final int[][] BASS = {{33, 45}, {41, 36}, {36, 48}, {31, 43}};
	/** A melody of the mod's own over the chords, notes and lengths in sixteenths. */
	private static final int[][] LEAD = {
		{69, 2}, {72, 2}, {76, 4}, {74, 2}, {72, 2}, {71, 4},
		{69, 2}, {72, 2}, {77, 4}, {76, 2}, {74, 2}, {72, 4},
		{67, 2}, {72, 2}, {76, 4}, {74, 4}, {72, 4},
		{74, 2}, {71, 2}, {67, 4}, {71, 2}, {74, 2}, {71, 4}};

	private final List<RemotePlayer> guards = new ArrayList<>();
	private final List<Integer> leadSteps = new ArrayList<>();
	private RemotePlayer putin;
	private Display.BlockDisplay doorLeft;
	private Display.BlockDisplay doorRight;
	private Display.TextDisplay title;
	private Vec3 door;
	private Vec3 look;
	private Vec3 side;
	private float yaw;
	private int death = Integer.MAX_VALUE;
	private float squeeze;

	@Override
	public int length() {
		return WALK + (int) (CARPET / PACE) + 40;
	}

	@Override
	public int photoTick() {
		return WALK + 70;
	}

	@Override
	public int screenTint() {
		// Pressed against a very wide black suit.
		return squeeze > 0.0F ? ((int) (Math.min(squeeze, 1.0F) * 240.0F) << 24) | 0x16161C : 0;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.wideputin", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (putin == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();

		for (int i = 0; i < guards.size(); i++) {
			RemotePlayer guard = guards.get(i);
			Vec3 across = guard.position().subtract(door).dot(side) > 0.0 ? side.scale(-1.0) : side;
			face(guard, guard.getEyePosition().add(across.scale(3.0)));
			if (tick == ARRIVE - 4 + (i % 3)) {
				pose(guard, Skins.Pose.SALUTE);
			}
		}
		if (tick >= DOORS && tick <= DOORS + 20) {
			// The golden doors swing open.
			float open = Math.min(1.0F, (tick - DOORS) / 20.0F) * 1.4F;
			hinge(doorLeft, 1, open);
			hinge(doorRight, -1, open);
			if (tick == DOORS) {
				sound(level, door, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 2.0F, 0.6F);
			}
		}
		if (tick == TITLE) {
			((TextDisplayAccessor) title).fakedeath$setText(Component.literal("IT'S HIM").withStyle(ChatFormatting.BOLD, ChatFormatting.WHITE));
			((TextDisplayAccessor) title).fakedeath$setBackgroundColor(0xA0000000);
			sound(level, eyes, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.RECORDS, 1.0F, 0.7F);
		}
		if (tick >= TITLE) {
			music(level, tick - TITLE, eyes);
		}

		if (tick >= WALK && tick < death) {
			// Down the carpet, slowly, importantly, straight on through.
			double along = (tick - WALK) * PACE;
			Vec3 at = onGround(level, player, door.add(look.scale(-along)));
			walk(putin, at);
			face(putin, at.add(look.scale(-4.0)).add(0.0, 1.6, 0.0));
			double gap = at.subtract(player.position()).dot(look);
			if (gap < 1.4) {
				// He does not stop.
				squeeze = (float) Math.min(1.0, (1.4 - gap) / 1.0);
				cameraShake(0.05F + squeeze * 0.3F);
				if (tick % 5 == 0) {
					hurt(level, player, SoundEvents.PLAYER_HURT, 0.15F);
				}
				if (squeeze >= 1.0F && death == Integer.MAX_VALUE) {
					death = tick + 6;
				}
			}
			turnTowards(player, putin.getEyePosition().add(0.0, -0.3, 0.0), 0.1F);
		} else if (tick < WALK) {
			turnTowards(player, door.add(0.0, 1.6, 0.0), 0.06F);
		}
		if (tick == death) {
			sound(level, eyes, SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 2.0F, 0.6F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	/** An Italo disco beat of the mod's own: kick, hi-hat, snare, octave bass and a synth melody. */
	private void music(ClientLevel level, int t, Vec3 at) {
		if (t % STEP != 0) {
			return;
		}
		int step = t / STEP;
		int inBar = step % 16;
		int bar = (step / 16) % 4;
		if (inBar % 4 == 0) {
			sound(level, at, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.RECORDS, 0.9F, 0.8F);
		}
		if (inBar % 8 == 4) {
			sound(level, at, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.RECORDS, 0.7F, 1.0F);
		}
		if (inBar % 2 == 1) {
			sound(level, at, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.RECORDS, 0.4F, 1.6F);
		}
		if (inBar % 2 == 0) {
			int note = BASS[bar][(inBar / 2) % 2];
			sound(level, at, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.RECORDS, 0.9F, (float) Math.pow(2.0, (note - 42) / 12.0));
		}
		int lead = leadSteps.indexOf(step % 64);
		if (lead >= 0) {
			float pitch = (float) Math.pow(2.0, (LEAD[lead][0] - 66) / 12.0);
			sound(level, at, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.RECORDS, 0.8F, pitch);
			sound(level, at, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.RECORDS, 0.25F, pitch);
		}
	}

	/** Swings a door leaf around its outer edge. */
	private void hinge(Display.BlockDisplay leaf, int s, float open) {
		Vec3 edge = door.add(side.scale(s * 1.5));
		Vec3 toMiddle = side.scale(-s).yRot(-s * open);
		Vec3 mid = edge.add(toMiddle.scale(0.75)).add(0.0, 1.6, 0.0);
		place(leaf, mid, new Vector3f(1.5F, 3.2F, 0.15F), new Quaternionf().rotateY(yaw - s * open));
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		look = horizontalLook(player);
		side = look.yRot(Mth.HALF_PI);
		yaw = (float) Math.atan2(look.x, look.z);
		Vec3 feet = player.position();
		door = onGround(level, player, feet.add(look.scale(CARPET)));
		Quaternionf turn = new Quaternionf().rotateY(yaw);

		// The red carpet with golden edges, from the doors to the player's feet and past them.
		Vec3 carpetMid = onGround(level, player, feet.add(look.scale(CARPET / 2.0 - 1.0)));
		BlockState gold = Blocks.GOLD_BLOCK.defaultBlockState();
		place(blockDisplay(level, Looks.RED_WOOL, carpetMid, 24.0F), carpetMid.add(0.0, 0.02, 0.0), new Vector3f(2.2F, 0.04F, (float) CARPET + 2.0F), turn);
		for (int s : new int[] {-1, 1}) {
			place(blockDisplay(level, gold, carpetMid, 24.0F), carpetMid.add(side.scale(s * 1.15)).add(0.0, 0.025, 0.0), new Vector3f(0.12F, 0.05F, (float) CARPET + 2.0F), turn);
		}
		// The wall with the golden doors at the far end, white and gold like a palace hall.
		place(blockDisplay(level, Looks.WHITE_CONCRETE, door, 24.0F), door.add(look.scale(0.3)).add(0.0, 2.5, 0.0), new Vector3f(12.0F, 5.0F, 0.4F), turn);
		place(blockDisplay(level, gold, door, 24.0F), door.add(look.scale(0.08)).add(0.0, 3.5, 0.0), new Vector3f(3.6F, 0.3F, 0.1F), turn);
		place(blockDisplay(level, Looks.BLACK_CONCRETE, door, 24.0F), door.add(look.scale(0.1)).add(0.0, 1.6, 0.0), new Vector3f(3.0F, 3.2F, 0.05F), turn);
		doorLeft = blockDisplay(level, gold, door, 24.0F);
		doorRight = blockDisplay(level, gold, door, 24.0F);
		hinge(doorLeft, 1, 0.0F);
		hinge(doorRight, -1, 0.0F);

		// The guard of honour marches in and lines the carpet.
		for (int i = 0; i < 6; i++) {
			int s = i % 2 == 0 ? 1 : -1;
			double along = 4.0 + (i / 2) * 4.0;
			RemotePlayer guard = extra(level, "general", onGround(level, player, feet.add(look.scale(along)).add(side.scale(s * 1.9))));
			arrive(level, player, guard, (i / 2) * 5);
			guards.add(guard);
		}
		standFlag(level, player, door.add(side.scale(3.0)).add(look.scale(-0.4)), "flag_russia", 1.6F);
		standFlag(level, player, door.add(side.scale(-3.0)).add(look.scale(-0.4)), "flag_russia", 1.6F);

		// He waits behind the doors.
		putin = actor(level, "fakedeath.wideputin.name", "putin", false, door.add(look.scale(0.6)));
		Skins.wide(putin.getId(), WIDTH);
		showName(putin, false);
		face(putin, feet.add(0.0, 1.6, 0.0));

		title = spawn(level, EntityType.TEXT_DISPLAY, door.add(0.0, 5.6, 0.0));
		((DisplayAccessor) title).fakedeath$setBillboard(Display.BillboardConstraints.CENTER);
		((TextDisplayAccessor) title).fakedeath$setBackgroundColor(0);
		((TextDisplayAccessor) title).fakedeath$setText(Component.empty());
		Looks.glow(title);
		place(title, door.add(0.0, 5.6, 0.0), 4.0F);

		int t = 0;
		for (int[] note : LEAD) {
			leadSteps.add(t);
			t += note[1];
		}
	}
}
