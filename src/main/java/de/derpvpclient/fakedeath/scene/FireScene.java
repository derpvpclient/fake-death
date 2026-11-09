package de.derpvpclient.fakedeath.scene;

import de.derpvpclient.fakedeath.CameraDirector;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A spark lands at the player's feet and they stamp it out. Then two, stamped out too. Then the whole ground around
 * them goes up in flames, they catch fire and look around in panic. Saved: a bucket! Only it is the lava bucket. It
 * gets poured out anyway, and the player sinks into the lava.
 */
public class FireScene extends Scene {
	private static final int FIRST = 5;
	private static final int[] STOMPS = {24, 52, 64};
	private static final int TWO = 40;
	private static final int RING = 84;
	private static final int RING_WIDE = 90;
	private static final int CATCH = 94;
	private static final int[] BURNS = {104, 116};
	private static final int BUCKET = 122;
	private static final int POUR = 140;
	private static final int POOL = 146;
	private static final int SINK = 150;
	private static final int SINK_END = 166;
	private static final int[] LAVA_HITS = {154, 164};
	private static final int DEATH = 176;

	private Vec3 dir;
	private Vec3 side;
	private BlockPos front;
	private BlockPos left;
	private BlockPos right;
	private final List<BlockPos> ring = new ArrayList<>();
	private Display.ItemDisplay bucket;
	private Vec3 look;
	private int now;

	@Override
	public int length() {
		return DEATH + 20;
	}

	@Override
	public int photoTick() {
		return POUR + 2;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.lava", player.getDisplayName());
	}

	@Override
	public float lying() {
		// In F5 the body sinks into the lava face down.
		return now < SINK ? 0.0F : Math.min(1.0F, (now - SINK) / (float) (SINK_END - SINK));
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		now = tick;
		if (tick == 0) {
			dir = horizontalLook(player);
			side = new Vec3(-dir.z, 0.0, dir.x);
			front = spot(level, BlockPos.containing(player.position().add(dir.scale(1.3))), null);
			left = spot(level, BlockPos.containing(player.position().add(side.scale(1.3)).add(dir.scale(0.6))), null);
			right = spot(level, BlockPos.containing(player.position().subtract(side.scale(1.3)).add(dir.scale(0.6))), null);
		}
		Vec3 eyes = player.getEyePosition();

		if (tick == FIRST) {
			flame(level, front, true);
		}
		if (tick == TWO) {
			flame(level, left, true);
			flame(level, right, true);
		}
		BlockPos[] stomped = {front, left, right};
		for (int i = 0; i < STOMPS.length; i++) {
			if (tick == STOMPS[i]) {
				stomp(level, player, stomped[i]);
			}
		}

		if (tick == RING) {
			// Whoosh: the whole ground around the player at once, a moment later the next ring.
			ring(level, player, 1);
			sound(level, player.position(), SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.5F, 0.7F);
			sound(level, player.position(), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.5F, 0.6F);
		}
		if (tick == RING_WIDE) {
			ring(level, player, 2);
			sound(level, player.position(), SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.2F, 0.9F);
		}
		if (tick >= RING && tick < POOL && tick % 15 == 0) {
			sound(level, player.position(), SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.5F, 0.8F + random.nextFloat() * 0.3F);
		}
		if (tick >= RING && tick < POOL && tick % 2 == 0) {
			particles(level, ParticleTypes.FLAME, player.position().add(0.0, 0.3, 0.0), 4, 1.6, 0.03);
			particles(level, ParticleTypes.LARGE_SMOKE, player.position().add(0.0, 1.8, 0.0), 1, 1.2, 0.03);
		}

		if (tick == CATCH) {
			setOnFire(player);
			hurt(level, player, SoundEvents.PLAYER_HURT_ON_FIRE, 0.12F);
		}
		for (int burn : BURNS) {
			if (tick == burn) {
				hurt(level, player, SoundEvents.PLAYER_HURT_ON_FIRE, 0.12F);
			}
		}

		bucket(level, player, eyes, tick);

		if (tick == POOL) {
			// The flames around the player turn into a lava pool.
			for (BlockPos pos : ring) {
				if (level.getBlockState(pos).is(Blocks.FIRE)) {
					replaceBlock(level, pos, Blocks.LAVA.defaultBlockState());
				}
			}
			sound(level, player.position(), SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 1.5F, 0.8F);
			hurt(level, player, SoundEvents.PLAYER_HURT_ON_FIRE, 0.15F);
		}
		for (int hit : LAVA_HITS) {
			if (tick == hit) {
				hurt(level, player, SoundEvents.PLAYER_HURT_ON_FIRE, 0.15F);
				particles(level, ParticleTypes.LAVA, eyes, 6, 0.6, 0.1);
			}
		}
		if (tick >= POOL && tick % 6 == 0) {
			sound(level, player.position(), SoundEvents.LAVA_POP, SoundSource.BLOCKS, 1.0F, 0.8F + random.nextFloat() * 0.4F);
		}
		if (tick == DEATH) {
			kill(level, player, SoundEvents.PLAYER_HURT_ON_FIRE);
		}

		camera(player, eyes, tick);
	}

	/** Where the camera looks and goes: down at each flame, in panic around, at the bucket, and down into the lava. */
	private void camera(LocalPlayer player, Vec3 eyes, int tick) {
		Vec3 offset = Vec3.ZERO;
		float speed = 0.2F;
		if (tick < TWO) {
			look = tick < STOMPS[0] + 6 ? at(front, eyes.add(dir.scale(1.5)).add(0.0, -1.5, 0.0)) : eyes.add(dir.scale(4.0)).add(0.0, -0.6, 0.0);
		} else if (tick < RING) {
			look = tick < STOMPS[1] + 4 ? at(left, eyes.add(side.scale(1.5)).add(0.0, -1.5, 0.0)) : tick < STOMPS[2] + 6 ? at(right, eyes.subtract(side.scale(1.5)).add(0.0, -1.5, 0.0)) : eyes.add(dir.scale(4.0)).add(0.0, -0.4, 0.0);
			speed = 0.3F;
		} else if (tick < BUCKET) {
			// Panic: left, right, left, faster and faster.
			int swing = (tick - RING) / 7;
			look = eyes.add(dir.yRot((swing % 2 == 0 ? 1 : -1) * 1.1F).scale(4.0)).add(0.0, -0.8 + (swing % 3) * 0.3, 0.0);
			speed = 0.4F;
			if (tick < RING + 6) {
				// The whoosh pushes the head back a little.
				offset = dir.scale(-0.25 * (1.0 - (tick - RING) / 6.0));
			}
		} else if (tick < SINK) {
			look = eyes.add(dir.scale(4.0)).add(0.0, -1.2, 0.0);
			speed = 0.3F;
		} else {
			// Sinking forward into the lava, until the camera is in it.
			double f = Math.min(1.0, (tick - SINK) / (double) (SINK_END - SINK));
			f = f * f * (3.0 - 2.0 * f);
			offset = dir.scale(0.8 * f).add(0.0, -1.2 * f, 0.0);
			look = eyes.add(dir.scale(4.0)).add(0.0, -1.0 - f, 0.0);
		}
		// Stomping: the view drops with the foot and comes back up.
		for (int stomp : STOMPS) {
			int since = tick - stomp;
			if (since >= -3 && since < 6) {
				double dip = since < 0 ? 0.08 * (since + 3) : 0.3 * (1.0 - since / 6.0);
				offset = offset.add(0.0, since < 0 ? dip : -dip, 0.0);
			}
		}
		moveCamera(offset);
		turnTowards(player, look, speed);
	}

	/** A bucket comes up into view, it is the lava bucket, and it gets poured out over the player anyway. */
	private void bucket(ClientLevel level, LocalPlayer player, Vec3 eyes, int tick) {
		if (tick < BUCKET || tick >= SINK) {
			if (bucket != null && tick >= SINK) {
				despawn(level, bucket);
				bucket = null;
			}
			return;
		}
		Vec3 camera = eyes.add(CameraDirector.positionOffsetNow());
		Vec3 view = look.subtract(camera).normalize();
		Vec3 right = view.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
		Vec3 up = right.cross(view);
		float rise = Math.min(1.0F, (tick - BUCKET) / 8.0F);
		float tilt = tick < POUR ? 0.0F : Math.min(1.0F, (tick - POUR) / 6.0F);
		// Held in front, then lifted and turned over above the head.
		Vec3 pos = camera.add(view.scale(0.7)).add(right.scale(0.22 - 0.22 * tilt)).add(up.scale(-0.55 + rise * 0.3 + tilt * 0.45));
		if (bucket == null) {
			bucket = itemDisplay(level, new ItemStack(Items.LAVA_BUCKET), pos);
			((de.derpvpclient.fakedeath.mixin.DisplayAccessor) bucket).fakedeath$setBillboard(Display.BillboardConstraints.CENTER);
			sound(level, camera, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
		}
		place(bucket, pos, new Vector3f(0.4F), new Quaternionf().rotateZ(-tilt * 2.4F));
		if (tick == POUR + 4) {
			sound(level, camera, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.PLAYERS, 1.5F, 1.0F);
			((de.derpvpclient.fakedeath.mixin.ItemDisplayAccessor) bucket).fakedeath$setItemStack(new ItemStack(Items.BUCKET));
		}
		if (tick >= POUR + 3) {
			// The lava pours down over the head.
			particles(level, ParticleTypes.FALLING_LAVA, pos, 6, 0.25, 0.0);
			particles(level, ParticleTypes.LAVA, camera.add(view.scale(0.6)), 2, 0.4, 0.1);
		}
	}

	/** The middle of a spot, or somewhere else if there was no room for a flame. */
	private static Vec3 at(BlockPos pos, Vec3 orElse) {
		return pos != null ? Vec3.atCenterOf(pos) : orElse;
	}

	/** Stamps the flame out: the foot comes down, a hiss and a puff of smoke. */
	private void stomp(ClientLevel level, LocalPlayer player, BlockPos pos) {
		if (pos == null) {
			return;
		}
		Vec3 at = Vec3.atBottomCenterOf(pos);
		if (level.getBlockState(pos).is(Blocks.FIRE)) {
			replaceBlock(level, pos, Blocks.AIR.defaultBlockState());
		}
		sound(level, at, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.2F);
		sound(level, at, SoundEvents.PLAYER_SMALL_FALL, SoundSource.PLAYERS, 1.0F, 0.8F);
		particles(level, ParticleTypes.LARGE_SMOKE, at.add(0.0, 0.3, 0.0), 6, 0.3, 0.03);
		Looks.swing(player);
	}

	/** A flame on a spot, with a little spark and a crackle when it comes out of nowhere. */
	private void flame(ClientLevel level, BlockPos pos, boolean spark) {
		if (pos == null) {
			return;
		}
		BlockState state = level.getBlockState(pos);
		if (state.isAir() || state.is(Blocks.FIRE)) {
			replaceBlock(level, pos, Blocks.FIRE.defaultBlockState());
		}
		if (spark) {
			Vec3 at = Vec3.atBottomCenterOf(pos);
			particles(level, ParticleTypes.FLAME, at.add(0.0, 0.4, 0.0), 8, 0.2, 0.05);
			sound(level, at, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6F, 1.6F);
			sound(level, at, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
	}

	/** Flames on every free spot of a ring around the player, the player's own block stays free. */
	private void ring(ClientLevel level, LocalPlayer player, int radius) {
		BlockPos feet = player.blockPosition();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
					continue;
				}
				BlockPos pos = spot(level, feet.offset(dx, 0, dz), null);
				if (pos != null && level.getBlockState(pos).isAir()) {
					flame(level, pos, false);
					ring.add(pos);
					particles(level, ParticleTypes.FLAME, Vec3.atBottomCenterOf(pos).add(0.0, 0.5, 0.0), 4, 0.3, 0.08);
				}
			}
		}
	}

	/** A free spot with firm ground at or a step up or down from a block, or the fallback if there is none. */
	private static BlockPos spot(ClientLevel level, BlockPos around, BlockPos fallback) {
		for (int dy : new int[] {0, 1, -1}) {
			BlockPos pos = around.above(dy);
			BlockState state = level.getBlockState(pos);
			if ((state.isAir() || state.is(Blocks.FIRE)) && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
				&& !onTree(level, pos)) {
				return pos;
			}
		}
		return fallback;
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		if (bucket != null) {
			despawn(level, bucket);
			bucket = null;
		}
		super.end(level, player);
	}
}
