package de.derpvpclient.fakedeath.scene;

import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The tree the player is cutting creaks, sinks into the cut and topples onto them in one piece. Logs and leaves
 * become client-only block displays that turn around the stump together, the real blocks are back afterwards.
 */
public class TreeScene extends Scene {
	private static final int MAX_LOGS = 16;
	private static final int MAX_LEAVES = 80;
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

	private TreeScene(List<BlockPos> logs, List<BlockPos> leaves, int drop) {
		this.logs = logs;
		this.leaves = leaves;
		this.drop = drop;
	}

	/** The player just broke a log: is there a real tree standing on top of it? */
	public static TreeScene afterBreak(ClientLevel level, BlockPos broken, BlockState state) {
		return state.is(BlockTags.LOGS) ? build(level, broken, broken.above()) : null;
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

		while (level.getBlockState(closest.below()).is(BlockTags.LOGS)) {
			closest = closest.below();
		}

		return build(level, closest.below(), closest);
	}

	private static TreeScene build(ClientLevel level, BlockPos stump, BlockPos firstLog) {
		List<BlockPos> logs = new ArrayList<>();

		for (BlockPos pos = firstLog; logs.size() < MAX_LOGS && level.getBlockState(pos).is(BlockTags.LOGS); pos = pos.above()) {
			logs.add(pos);
		}

		if (logs.isEmpty()) {
			return null;
		}

		// Leaves around the crown tell a tree apart from a log wall in a house.
		BlockPos top = logs.getLast();
		List<BlockPos> leaves = new ArrayList<>();

		for (BlockPos pos : BlockPos.betweenClosed(top.offset(-2, -3, -2), top.offset(2, 2, 2))) {
			if (leaves.size() < MAX_LEAVES && level.getBlockState(pos).is(BlockTags.LEAVES)) {
				leaves.add(pos.immutable());
			}
		}

		// A cut stump leaves a hole, the tree sinks into it before it tips over.
		int drop = level.getBlockState(stump).isAir() ? 1 : 0;
		return leaves.isEmpty() ? null : new TreeScene(logs, leaves, drop);
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

		if (tick >= WOBBLE && tick < IMPACT) {
			turnTowards(player, worldCenter(parts.get(logs.size() - 1), rotation, sink), 0.35F);

			if (tick % 3 == 0 && leafState != null) {
				Part leaf = parts.get(logs.size() + random.nextInt(parts.size() - logs.size()));
				level.addDestroyBlockEffect(BlockPos.containing(worldCenter(leaf, rotation, sink)), leafState);
			}
		}

		if (tick == IMPACT) {
			impact(level, player, rotation, sink);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		Vec3 base = Vec3.atBottomCenterOf(logs.getFirst());
		Vec3 dir = new Vec3(player.getX() - base.x, 0.0, player.getZ() - base.z);
		dir = dir.lengthSqr() < 1.0E-4 ? horizontalLook(player).reverse() : dir.normalize();

		// It tips over the edge of the trunk that faces the player.
		pivot = new Vec3(base.x + dir.x * 0.5, base.y, base.z + dir.z * 0.5);
		axis = new Vector3f((float) dir.z, 0.0F, (float) -dir.x);
		logState = level.getBlockState(logs.getFirst());
		leafState = level.getBlockState(leaves.getFirst());
		float cullSize = (logs.size() + 5) * 2.5F;

		List<BlockPos> blocks = new ArrayList<>(logs);
		blocks.addAll(leaves);

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
