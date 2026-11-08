package de.derpvpclient.fakedeath.scene;

import com.mojang.math.Transformation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The tree the player is cutting creaks, sinks into the cut and topples onto them in one piece. Logs and leaves
 * become client-only block displays that turn around the stump together, the real blocks are back afterwards.
 */
public class TreeScene extends Scene {
	private static final int MAX_LOGS = 96;
	private static final int MAX_LEAVES = 450;
	private static final int MAX_EXTRAS = 60;
	private static final int WOBBLE = 32;
	private static final int FALL = 20;
	private static final int IMPACT = WOBBLE + FALL;
	private static final float LYING_ANGLE = 88.0F;

	private final List<BlockPos> logs;
	private final List<BlockPos> leaves;
	private final int drop;
	private final List<Part> parts = new ArrayList<>();
	private Vec3 pivot;
	private Vector3f axis;
	private BlockState logState;
	private BlockState leafState;
	private int now;

	@Override
	public int screenTint() {
		if (now < IMPACT) {
			return 0;
		}
		return (Math.min(150, (now - IMPACT + 1) * 40) << 24) | 0x1C3410;
	}

	private TreeScene(List<BlockPos> logs, List<BlockPos> leaves, int drop) {
		this.logs = logs;
		this.leaves = leaves;
		this.drop = drop;
	}

	/** The player just broke a log: is there a real tree standing on top of it? */
	public static TreeScene afterBreak(ClientLevel level, BlockPos broken, BlockState state) {
		if (!state.is(BlockTags.LOGS)) {
			return null;
		}
		// An acacia goes on at a slant, the next log can sit diagonally above the cut one.
		BlockPos above = logAround(level, broken.above());
		return above != null ? build(level, broken, above) : null;
	}

	/** Any tree right next to the player, for testing. */
	public static TreeScene near(ClientLevel level, LocalPlayer player) {
		BlockPos feet = player.blockPosition();
		BlockPos closest = null;

		for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-4, -1, -4), feet.offset(4, 4, 4))) {
			if (level.getBlockState(pos).is(BlockTags.LOGS) && (closest == null || pos.distSqr(feet) < closest.distSqr(feet))) {
				closest = pos.immutable();
			}
		}

		if (closest == null) {
			return null;
		}

		// Down to the foot of the trunk, also along a crooked acacia trunk.
		for (BlockPos lower = logAround(level, closest.below()); lower != null; lower = logAround(level, closest.below())) {
			closest = lower;
		}

		return build(level, closest.below(), closest);
	}

	/** A log at this spot, or else right next to it on the same level, straight sides first. Null for none. */
	private static BlockPos logAround(ClientLevel level, BlockPos center) {
		if (level.getBlockState(center).is(BlockTags.LOGS)) {
			return center;
		}
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (level.getBlockState(center.relative(side)).is(BlockTags.LOGS)) {
				return center.relative(side);
			}
		}
		for (BlockPos near : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
			if (level.getBlockState(near).is(BlockTags.LOGS)) {
				return near.immutable();
			}
		}
		return null;
	}

	private static TreeScene build(ClientLevel level, BlockPos stump, BlockPos firstLog) {
		if (!level.getBlockState(firstLog).is(BlockTags.LOGS)) {
			return null;
		}

		// The whole wood: the trunk upwards, branches going off at an angle, all four columns of a thick trunk.
		List<BlockPos> logs = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(firstLog);
		seen.add(firstLog);
		while (!queue.isEmpty() && logs.size() < MAX_LOGS) {
			BlockPos pos = queue.poll();
			logs.add(pos);
			for (BlockPos next : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 1, 1))) {
				BlockPos near = next.immutable();
				if (near.getY() >= firstLog.getY() && Math.abs(near.getX() - firstLog.getX()) <= 8 && Math.abs(near.getZ() - firstLog.getZ()) <= 8
					&& seen.add(near) && level.getBlockState(near).is(BlockTags.LOGS)) {
					queue.add(near);
				}
			}
		}
		// Lowest first, so the trunk's foot decides where the tree tips over.
		logs.sort(Comparator.comparingInt(BlockPos::getY));

		// The leaves that belong to this tree, the way the game itself sees it: a leaf counts the steps to the nearest
		// log, and it is ours if our wood is that near. A neighbor's crown stays where it is.
		List<BlockPos> leaves = new ArrayList<>();
		Map<BlockPos, Integer> steps = new HashMap<>();
		ArrayDeque<BlockPos> crown = new ArrayDeque<>();
		for (BlockPos log : logs) {
			steps.put(log, 0);
			crown.add(log);
		}
		while (!crown.isEmpty() && leaves.size() < MAX_LEAVES) {
			BlockPos pos = crown.poll();
			int step = steps.get(pos) + 1;
			for (Direction side : Direction.values()) {
				BlockPos near = pos.relative(side);
				BlockState state = level.getBlockState(near);
				if (steps.containsKey(near) || !state.is(BlockTags.LEAVES)) {
					continue;
				}
				int distance = state.hasProperty(LeavesBlock.DISTANCE) ? state.getValue(LeavesBlock.DISTANCE) : 7;
				boolean placed = state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT);
				if (step <= (placed ? 4 : distance)) {
					steps.put(near, step);
					leaves.add(near);
					crown.add(near);
				}
			}
		}

		// Leaves tell a tree apart from a log wall in a house.
		if (leaves.isEmpty()) {
			return null;
		}

		// What hangs on the tree comes down with it: vines all the way down, cocoa, a bee nest.
		List<BlockPos> extras = new ArrayList<>();
		Set<BlockPos> tree = new HashSet<>(logs);
		tree.addAll(leaves);
		ArrayDeque<BlockPos> hanging = new ArrayDeque<>(tree);
		while (!hanging.isEmpty() && extras.size() < MAX_EXTRAS) {
			BlockPos pos = hanging.poll();
			for (Direction side : Direction.values()) {
				BlockPos near = pos.relative(side);
				BlockState state = level.getBlockState(near);
				if (!tree.contains(near) && (state.is(Blocks.VINE) || state.is(Blocks.COCOA) || state.is(Blocks.BEE_NEST))) {
					tree.add(near);
					extras.add(near);
					hanging.add(near);
				}
			}
		}
		leaves.addAll(extras);

		// A cut stump leaves a hole, the tree sinks into it before it tips over.
		int drop = level.getBlockState(stump).isAir() ? 1 : 0;
		return new TreeScene(logs, leaves, drop);
	}

	@Override
	public int length() {
		return IMPACT + 14;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.tree", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}

		if (parts.isEmpty()) {
			return;
		}

		float angle;
		float sink = 0.0F;

		if (tick < WOBBLE) {
			// Creaking back and forth, a little more each time, while it sinks into the cut.
			angle = Mth.sin(tick * 1.4F) * 2.0F * tick / WOBBLE;
			sink = drop * Mth.clamp((tick - 16) / 6.0F, 0.0F, 1.0F);
		} else if (tick < IMPACT) {
			float progress = (tick - WOBBLE) / (float) FALL;
			angle = LYING_ANGLE * progress * progress;
			sink = drop;
		} else {
			// A small bounce on the ground, then it lies still.
			angle = tick == IMPACT + 1 ? LYING_ANGLE - 3.0F : LYING_ANGLE;
			sink = drop;
		}

		Quaternionf rotation = new Quaternionf().rotateAxis(angle * Mth.DEG_TO_RAD, axis.x, axis.y, axis.z);

		for (Part part : parts) {
			Vector3f translation = rotation.transform(new Vector3f(part.offset())).add(0.0F, -sink, 0.0F);
			transform(part.display(), new Transformation(translation, new Quaternionf(rotation), new Vector3f(1.0F), new Quaternionf()));
		}

		if (tick == 5 || tick == 14 || tick == 22 || tick == 29) {
			sound(level, pivot.add(0, 2, 0), SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 2.0F, 0.45F + random.nextFloat() * 0.1F);
		}

		if (tick == 8) {
			sound(level, pivot.add(0, logs.size(), 0), SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.BLOCKS, 1.5F, 0.7F);
		}

		if (tick >= 4 && tick < WOBBLE) {
			// The view goes up the trunk to the creaking crown, which sheds a few leaves.
			turnTowards(player, worldCenter(parts.get(logs.size() - 1), rotation, sink), 0.07F);
			if (tick % 4 == 0 && leafState != null) {
				Part leaf = parts.get(logs.size() + random.nextInt(parts.size() - logs.size()));
				level.addDestroyBlockEffect(BlockPos.containing(worldCenter(leaf, rotation, sink)), leafState);
			}
		}

		if (tick >= WOBBLE && tick < IMPACT) {
			turnTowards(player, worldCenter(parts.get(logs.size() - 1), rotation, sink), 0.35F);

			if (tick % 3 == 0 && leafState != null) {
				Part leaf = parts.get(logs.size() + random.nextInt(parts.size() - logs.size()));
				level.addDestroyBlockEffect(BlockPos.containing(worldCenter(leaf, rotation, sink)), leafState);
			}
		}

		now = tick;
		if (tick >= IMPACT && tick < IMPACT + 8) {
			// The crown lands on top: the view shakes and goes dark green under the leaves.
			cameraShake(0.35F * (IMPACT + 8 - tick) / 8.0F);
		} else if (tick == IMPACT + 8) {
			cameraShake(0.0F);
		}

		if (tick == IMPACT) {
			impact(level, player, rotation, sink);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		// The middle of the trunk's foot, also for a thick trunk of four columns.
		int bottom = logs.getFirst().getY();
		List<BlockPos> foot = logs.stream().filter(pos -> pos.getY() == bottom).toList();
		double footX = foot.stream().mapToDouble(pos -> pos.getX() + 0.5).average().orElse(0.0);
		double footZ = foot.stream().mapToDouble(pos -> pos.getZ() + 0.5).average().orElse(0.0);
		double half = foot.size() > 1 ? 1.0 : 0.5;
		Vec3 base = new Vec3(footX, bottom, footZ);
		Vec3 dir = new Vec3(player.getX() - base.x, 0.0, player.getZ() - base.z);
		dir = dir.lengthSqr() < 1.0E-4 ? horizontalLook(player).reverse() : dir.normalize();
		// A little to one side, so it swings down across the view instead of straight at the eyes.
		dir = dir.yRot((random.nextBoolean() ? 1 : -1) * 18.0F * Mth.DEG_TO_RAD);

		// It tips over the edge of the trunk that faces the player.
		pivot = new Vec3(base.x + dir.x * half, base.y, base.z + dir.z * half);
		axis = new Vector3f((float) dir.z, 0.0F, (float) -dir.x);
		logState = level.getBlockState(logs.getFirst());
		leafState = level.getBlockState(leaves.getFirst());
		List<BlockPos> blocks = new ArrayList<>(logs);
		blocks.addAll(leaves);
		// Displays are culled by a box around the stump, the falling crown reaches far beyond it.
		float cullSize = (float) blocks.stream().mapToDouble(pos -> Vec3.atCenterOf(pos).distanceTo(pivot)).max().orElse(8.0) * 2.0F + 6.0F;

		for (BlockPos pos : blocks) {
			Vector3f offset = new Vector3f((float) (pos.getX() - pivot.x), (float) (pos.getY() - pivot.y), (float) (pos.getZ() - pivot.z));
			Display.BlockDisplay display = blockDisplay(level, level.getBlockState(pos), pivot, cullSize);
			transform(display, new Transformation(offset, new Quaternionf(), new Vector3f(1.0F), new Quaternionf()));
			parts.add(new Part(display, offset));
		}

		// Swapped in the same tick, so the tree never flickers.
		blocks.forEach(pos -> hideBlock(level, pos));
	}

	private void impact(ClientLevel level, LocalPlayer player, Quaternionf rotation, float sink) {
		Vec3 where = player.position();
		sound(level, where, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 1.6F, 0.6F);
		sound(level, where, SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 1.0F, 0.8F);
		sound(level, where, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.BLOCKS, 2.0F, 0.8F);

		for (int i = 0; i < parts.size(); i++) {
			boolean isLog = i < logs.size();

			if (isLog || i % 3 == 0) {
				level.addDestroyBlockEffect(BlockPos.containing(worldCenter(parts.get(i), rotation, sink)), isLog ? logState : leafState);
			}
		}

		kill(level, player, SoundEvents.PLAYER_HURT);
	}

	/** Where the middle of a part is in the world right now. */
	private Vec3 worldCenter(Part part, Quaternionf rotation, float sink) {
		Vector3f center = rotation.transform(new Vector3f(part.offset()).add(0.5F, 0.5F, 0.5F));
		return pivot.add(center.x, center.y - sink, center.z);
	}

	private record Part(Display.BlockDisplay display, Vector3f offset) {
	}
}
