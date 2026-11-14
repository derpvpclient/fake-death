package de.derpvpclient.fakedeath.scene;

import com.mojang.math.Transformation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The Nether portal next to the player hums louder and louder, purple sparks whirling. Then the whole purple sheet
 * comes loose from its obsidian frame, leans out, turns towards the player and flies at them, bigger and bigger,
 * and swallows them. For a moment everything is purple, then red: the Nether, a ghast screams.
 */
public class NetherPortalScene extends Scene {
	private static final int LOOSEN = 40;
	private static final int FLY = 70;
	private static final int FLIGHT = 26;
	private static final int SWALLOW = FLY + FLIGHT;
	private static final int NETHER = SWALLOW + 12;
	private static final int DEATH = NETHER + 22;
	private static final int MAX_BLOCKS = 80;

	private final BlockPos found;
	private final List<Part> parts = new ArrayList<>();
	private Vec3 center;
	private Vec3 normal;
	private float tint;
	private int tintColor = 0x5B1FB5;

	public NetherPortalScene(BlockPos portal) {
		this.found = portal;
	}

	@Override
	public int length() {
		return DEATH + 1;
	}

	@Override
	public int photoTick() {
		return FLY + FLIGHT / 2;
	}

	@Override
	public int screenTint() {
		return tint > 0.0F ? ((int) (Math.min(tint, 1.0F) * 225.0F) << 24) | tintColor : 0;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.netherportal", player.getDisplayName());
	}

	@Override
	public void onReveal(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
		tint = 0.0F;
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (parts.isEmpty()) {
			return;
		}

		Vec3 eyes = player.getEyePosition();
		Vec3 toPlayer = new Vec3(eyes.x - center.x, 0.0, eyes.z - center.z).normalize();

		// Humming louder, sparks whirling around it.
		if (tick < SWALLOW && tick % 20 == 0) {
			sound(level, center, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 1.0F + tick / 60.0F, 0.7F + tick / 120.0F);
		}

		Vec3 at = center;
		float turn = 0.0F;
		float lean = 0.0F;
		float scale = 1.0F;
		float wobble = tick < FLY ? Mth.sin(tick * 0.9F) * 0.02F * tick / FLY : 0.0F;
		float face = (float) Math.atan2(normal.x * toPlayer.z - normal.z * toPlayer.x, normal.x * toPlayer.x + normal.z * toPlayer.z);
		// A sheet looks the same from both sides: turn the short way round, never edge-on through 90 degrees.
		if (face > Mth.HALF_PI) {
			face -= Mth.PI;
		} else if (face < -Mth.HALF_PI) {
			face += Mth.PI;
		}
		if (tick >= LOOSEN && tick < FLY) {
			// It comes loose: leaning out of the frame, turning to face the player.
			float p = (tick - LOOSEN) / (float) (FLY - LOOSEN);
			at = center.add(toPlayer.scale(0.8 * p * p));
			lean = 0.2F * p;
			turn = face * p * p;
			if (tick == LOOSEN) {
				sound(level, center, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.2F, 0.5F);
				sound(level, center, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 1.0F, 1.4F);
			}
		} else if (tick >= FLY) {
			// Flying at the eyes, faster and faster, growing.
			float p = Math.min(1.0F, (tick - FLY) / (float) FLIGHT);
			Vec3 start = center.add(toPlayer.scale(0.8));
			at = start.lerp(eyes, p * p);
			lean = 0.2F * (1.0F - p);
			turn = face + Mth.sin(p * Mth.PI) * 0.3F;
			scale = 1.0F + p * 0.6F;
			if (tick == FLY) {
				sound(level, center, SoundEvents.PORTAL_TRAVEL, SoundSource.BLOCKS, 0.5F, 1.6F);
			}
		}

		if (tick <= SWALLOW) {
			Quaternionf rotation = new Quaternionf().rotateY(turn).rotateX(lean).rotateZ(wobble);
			for (Part part : parts) {
				Vector3f offset = new Vector3f(part.offset()).mul(scale).rotate(rotation);
				part.display().setPos(at.x, at.y, at.z);
				transform(part.display(), new Transformation(offset, new Quaternionf(rotation), new Vector3f(scale), new Quaternionf()));
			}
			for (int i = 0; i < 3 + tick / 15; i++) {
				Vec3 from = at.add((random.nextDouble() - 0.5) * 4.0, (random.nextDouble() - 0.5) * 4.0, (random.nextDouble() - 0.5) * 4.0);
				Vec3 speed = at.subtract(from).scale(0.08);
				level.addParticle(ParticleTypes.REVERSE_PORTAL, from.x, from.y, from.z, speed.x, speed.y, speed.z);
			}
		}

		if (tick < FLY) {
			turnTowards(player, center, 0.15F);
		}
		if (tick >= FLY && tick < SWALLOW) {
			tint = Math.max(0.0F, (tick - SWALLOW + 10) / 10.0F) * 0.9F;
		}
		if (tick == SWALLOW) {
			for (Part part : parts) {
				despawn(level, part.display());
			}
			parts.clear();
			tint = 1.0F;
			sound(level, eyes, SoundEvents.PORTAL_TRAVEL, SoundSource.BLOCKS, 0.6F, 1.0F);
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.35F);
		}
		if (tick >= NETHER) {
			// Through: the red of the Nether, a ghast screaming somewhere.
			tintColor = 0x7A1408;
			tint = 0.85F + Mth.sin(tick * 0.4F) * 0.08F;
			if (tick == NETHER) {
				sound(level, eyes, SoundEvents.GHAST_SCREAM, SoundSource.HOSTILE, 1.5F, 0.8F);
				sound(level, eyes, SoundEvents.AMBIENT_NETHER_WASTES_MOOD.value(), SoundSource.AMBIENT, 1.5F, 0.8F);
			}
		}
		if (tick == DEATH) {
			sound(level, eyes, SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 1.5F, 0.7F);
			kill(level, player, SoundEvents.PLAYER_HURT_ON_FIRE);
		}
	}

	/** The whole purple sheet of the portal, block by block, becomes a moving copy of itself; the real one is hidden. */
	private void setUp(ClientLevel level, LocalPlayer player) {
		BlockPos start = nearest(level, player);
		if (start == null) {
			return;
		}
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		List<BlockPos> blocks = new ArrayList<>();
		queue.add(start);
		seen.add(start);
		while (!queue.isEmpty() && blocks.size() < MAX_BLOCKS) {
			BlockPos pos = queue.poll();
			blocks.add(pos);
			for (Direction side : Direction.values()) {
				BlockPos next = pos.relative(side);
				if (seen.add(next) && level.getBlockState(next).is(Blocks.NETHER_PORTAL)) {
					queue.add(next);
				}
			}
		}
		double cx = blocks.stream().mapToDouble(pos -> pos.getX() + 0.5).average().orElse(0.0);
		double cy = blocks.stream().mapToDouble(pos -> pos.getY() + 0.5).average().orElse(0.0);
		double cz = blocks.stream().mapToDouble(pos -> pos.getZ() + 0.5).average().orElse(0.0);
		center = new Vec3(cx, cy, cz);
		BlockState state = level.getBlockState(start);
		Direction.Axis axis = state.hasProperty(NetherPortalBlock.AXIS) ? state.getValue(NetherPortalBlock.AXIS) : Direction.Axis.X;
		normal = axis == Direction.Axis.X ? new Vec3(0.0, 0.0, 1.0) : new Vec3(1.0, 0.0, 0.0);
		for (BlockPos pos : blocks) {
			Vector3f offset = new Vector3f((float) (pos.getX() - cx), (float) (pos.getY() - cy), (float) (pos.getZ() - cz));
			Display.BlockDisplay display = blockDisplay(level, level.getBlockState(pos), center, 12.0F);
			parts.add(new Part(display, offset));
		}
		blocks.forEach(pos -> hideBlock(level, pos));
		sound(level, center, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 0.6F, 1.0F);
	}

	private BlockPos nearest(ClientLevel level, LocalPlayer player) {
		BlockPos feet = player.blockPosition();
		BlockPos nearest = null;
		for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-6, -3, -6), feet.offset(6, 3, 6))) {
			if (level.getBlockState(pos).is(Blocks.NETHER_PORTAL) && (nearest == null || pos.distSqr(feet) < nearest.distSqr(feet))) {
				nearest = pos.immutable();
			}
		}
		return nearest != null ? nearest : found != null && level.getBlockState(found).is(Blocks.NETHER_PORTAL) ? found : null;
	}

	private record Part(Display.BlockDisplay display, Vector3f offset) {
	}
}
