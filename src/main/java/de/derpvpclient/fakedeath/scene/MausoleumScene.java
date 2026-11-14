package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The mausoleum on Red Square. Dim red light, two guards of honour, and in a glass coffin on red velvet lies Lenin,
 * as he has for a hundred years. Total silence. A breath. The guards look round. The glass cracks. Lenin sits up,
 * the coffin bursts, he climbs out and reaches for the player. "Who disturbs my rest?"
 */
public class MausoleumScene extends CastScene {
	private static final int BREATH = 55;
	private static final int CRACK = 85;
	private static final int SIT_UP = 115;
	private static final int SPEAK = SIT_UP + 12;
	private static final int REACH = SPEAK + 40;
	private static final int GRAB = REACH + 18;
	private static final int DEATH = GRAB + 12;
	private static final ChatFormatting RED = ChatFormatting.DARK_RED;

	private final List<Display.BlockDisplay> glass = new ArrayList<>();
	private final List<RemotePlayer> guards = new ArrayList<>();
	private RemotePlayer lenin;
	private BlockPos bed;
	private Vec3 coffin;

	@Override
	public int length() {
		return DEATH + 1;
	}

	@Override
	public int photoTick() {
		return SPEAK + 10;
	}

	@Override
	public int screenTint() {
		// Dim red light, as in the hall of the mausoleum.
		return 0x70300000;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.mausoleum", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (lenin == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();

		for (RemotePlayer guard : guards) {
			face(guard, tick < CRACK ? eyes.add(horizontalLook(player).scale(8.0)) : lenin.getEyePosition());
		}
		if (tick == BREATH) {
			sound(level, coffin, SoundEvents.PLAYER_BREATH, SoundSource.HOSTILE, 1.0F, 0.6F);
		}
		if (tick == CRACK || tick == CRACK + 12) {
			sound(level, coffin, SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 1.5F, 0.6F);
			level.destroyBlockProgress(-7_350_000, bed, tick == CRACK ? 4 : 8);
			for (RemotePlayer guard : guards) {
				pose(guard, null);
			}
		}
		if (tick == SIT_UP) {
			// He gets up, the glass bursts.
			level.destroyBlockProgress(-7_350_000, bed, -1);
			lenin.stopSleeping();
			for (Display.BlockDisplay pane : glass) {
				despawn(level, pane);
			}
			glass.clear();
			sound(level, coffin, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 2.0F, 0.6F);
			sound(level, coffin, SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 1.5F, 0.5F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GLASS.defaultBlockState()), coffin.add(0.0, 0.8, 0.0), 40, 0.8, 0.3);
		}
		if (tick >= SIT_UP) {
			// Up and out of the coffin, step by step towards the player.
			Vec3 target = tick < REACH ? Vec3.atBottomCenterOf(bed).add(0.0, 0.56, 0.0) : onGround(level, player, player.position().add(horizontalLook(player).scale(0.9)));
			if (tick < REACH) {
				lenin.setPos(target.x, target.y, target.z);
			} else {
				stepTowards(level, player, lenin, target, 0.14);
			}
			face(lenin, eyes);
		}
		if (tick == SPEAK) {
			speak(level, lenin, "fakedeath.mausoleum.who", RED, 45);
		}
		if (tick == REACH) {
			pose(lenin, Skins.Pose.POINT);
			sound(level, lenin.position(), SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 1.5F, 0.4F);
		}
		turnTowards(player, tick < SIT_UP ? coffin.add(0.0, 0.6, 0.0) : lenin.getEyePosition(), 0.08F);
		if (tick >= REACH) {
			cameraShake(0.03F + (tick - REACH) * 0.004F);
		}
		if (tick == GRAB) {
			sound(level, eyes, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 1.5F, 0.6F);
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.5F);
		}
		if (tick == DEATH) {
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		Vec3 look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		Direction across = Direction.getApproximateNearest(side.x, 0.0, side.z);
		// A red bed across the view three steps ahead: the velvet he lies on.
		BlockPos head = BlockPos.containing(player.position().add(look.scale(3.2)).add(side.scale(0.5)));
		BlockPos foot = head.relative(across.getOpposite());
		BlockState bedHead = Blocks.BED.red().defaultBlockState().setValue(BedBlock.FACING, across).setValue(BedBlock.PART, BedPart.HEAD);
		BlockState bedFoot = Blocks.BED.red().defaultBlockState().setValue(BedBlock.FACING, across).setValue(BedBlock.PART, BedPart.FOOT);
		if (!placeBlock(level, head, bedHead) || !placeBlock(level, foot, bedFoot)) {
			head = player.blockPosition().relative(Direction.getApproximateNearest(look.x, 0.0, look.z), 3);
		}
		bed = head;
		coffin = Vec3.atBottomCenterOf(head).add(Vec3.atBottomCenterOf(foot)).scale(0.5);
		lenin = actor(level, "fakedeath.mausoleum.lenin", "lenin", false, Vec3.atBottomCenterOf(head));
		lenin.setSleepingPos(head);
		// The glass coffin over him, the pedestal of red stone below.
		float yaw = (float) Math.atan2(look.x, look.z);
		Display.BlockDisplay lid = blockDisplay(level, Blocks.GLASS.defaultBlockState(), coffin, 6.0F);
		place(lid, coffin.add(0.0, 0.85, 0.0), new Vector3f(1.3F, 0.7F, 2.3F), new Quaternionf().rotateY(yaw + Mth.HALF_PI));
		glass.add(lid);
		Display.BlockDisplay pedestal = blockDisplay(level, Blocks.RED_NETHER_BRICKS.defaultBlockState(), coffin, 6.0F);
		place(pedestal, coffin.add(0.0, -0.05, 0.0), new Vector3f(1.6F, 0.1F, 2.6F), new Quaternionf().rotateY(yaw + Mth.HALF_PI));
		// The guards of honour at both ends, saluting.
		for (int s : new int[] {-1, 1}) {
			RemotePlayer guard = extra(level, "general", onGround(level, player, coffin.add(side.scale(s * 2.0)).add(look.scale(0.5))));
			pose(guard, Skins.Pose.SALUTE);
			guards.add(guard);
		}
		standFlag(level, player, coffin.add(look.scale(2.0)), "flag_soviet", 1.6F);
		sound(level, coffin, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.5F);
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		if (bed != null) {
			level.destroyBlockProgress(-7_350_000, bed, -1);
		}
		super.end(level, player);
	}
}
