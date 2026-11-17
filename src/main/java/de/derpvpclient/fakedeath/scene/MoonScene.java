package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * At night the moon grows bigger and bigger, the ground starts to rumble, and then it comes down. The sky turns red
 * as it burns its way in, glowing pieces break off and crash down all around, and at the end the moon pulls the
 * ground itself up towards it, block by block.
 */
public class MoonScene extends Scene {
	private static final int FALL = 170;
	/** From here on burning pieces break off and come down ahead of it. */
	private static final int PIECES = 50;
	private static final int PIECE_EVERY = 9;
	/** From here on the ground lifts off towards it. */
	private static final int LIFT = 95;
	private static final int MAX_LIFTED = 70;
	private Display.BlockDisplay moon;
	private Vec3 sky;
	private int flash;
	private int now;
	private float bump;
	private final List<Piece> pieces = new ArrayList<>();
	private final List<Lifted> lifted = new ArrayList<>();

	@Override
	public int length() {
		return FALL + 20;
	}

	@Override
	public int photoTick() {
		return FALL - 30;
	}

	@Override
	public int screenTint() {
		return flash > 0 ? (Math.min(255, flash * 30) << 24) | 0xFFFFFF : 0;
	}

	@Override
	public int skyTint() {
		// The air burns: the sky goes red while it comes in.
		float share = Mth.clamp((now / (float) FALL - 0.2F) / 0.6F, 0.0F, 1.0F);
		return share > 0.0F ? (int) (share * 190.0F) << 24 | 0xD23A14 : 0;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.mond", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		now = tick;
		if (tick == 0) {
			sky = player.getEyePosition().add(horizontalLook(player).scale(90.0)).add(0.0, 70.0, 0.0);
			moon = blockDisplay(level, Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState(), sky, 80.0F);
			Looks.farView(moon);
			Looks.glow(moon);
		}
		if (flash > 0) {
			flash--;
		}
		bump *= 0.8F;
		if (tick <= FALL) {
			// Faster and faster towards the player, and bigger.
			float p = tick / (float) FALL;
			float size = 8.0F + p * p * 40.0F;
			// Straight at the eyes, but its near side always stays in front of them: with the camera inside the huge
			// block its faces would vanish and the moon would be gone right before the hit.
			Vec3 eyes = player.getEyePosition();
			Vec3 way = sky.subtract(eyes);
			double start = way.length();
			double distance = start + (size * 0.9 + 1.2 - start) * p * p * p;
			Vec3 at = eyes.add(way.normalize().scale(distance));
			place(moon, at, new Vector3f(size), new Quaternionf().rotateY(tick * 0.004F).rotateX(0.3F));
			turnTowards(player, at, 0.08F);
			cameraShake(p * p * 0.35F + bump);
			if (tick % 30 == 0) {
				sound(level, player.position(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 0.5F + p * 2.0F, 0.3F);
			}
			if (p > 0.25F) {
				// Burning on the way in, a trail of fire and smoke behind it.
				Vec3 back = at.subtract(player.getEyePosition()).normalize().scale(size * 0.6);
				particles(level, ParticleTypes.FLAME, at.add(back), 25, size * 0.4, 0.3);
				particles(level, ParticleTypes.LARGE_SMOKE, at.add(back.scale(1.6)), 15, size * 0.5, 0.2);
			}
			if (p > 0.6F && tick % 3 == 0) {
				particles(level, ParticleTypes.ASH, player.getEyePosition(), 10, 3.0, 0.05);
			}
			if (tick >= PIECES && tick < FALL - 25 && tick % PIECE_EVERY == 0) {
				breakOff(level, player, at, size, tick);
			}
			if (tick >= LIFT && lifted.size() < MAX_LIFTED) {
				for (int i = 0; i < 1 + (tick - LIFT) / 25; i++) {
					liftOne(level, player, tick);
				}
			}
		}
		movePieces(level, player, tick);
		for (Lifted block : lifted) {
			// Slowly at first, then faster, tumbling, up towards the moon.
			int up = tick - block.start();
			Vec3 pos = block.from().add(0.0, 0.004 * up * up + 0.02 * up, 0.0);
			place(block.display(), pos, new Vector3f(1.0F), new Quaternionf().rotateAxis(up * block.spin(), block.axis()));
		}
		if (tick == FALL) {
			flash = 10;
			cameraShake(0.0F);
			sound(level, player.position(), Looks.EXPLODE, SoundSource.WEATHER, 3.0F, 0.4F);
			particles(level, ParticleTypes.EXPLOSION_EMITTER, player.position(), 3, 2.0, 0.0);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	/** A glowing piece breaks off the moon and comes down somewhere in the picture, never right on the player. */
	private void breakOff(ClientLevel level, LocalPlayer player, Vec3 moonAt, float moonSize, int tick) {
		Vec3 look = horizontalLook(player);
		double ahead = 10.0 + random.nextDouble() * 22.0;
		double across = (random.nextDouble() - 0.5) * 1.6 * ahead;
		Vec3 spot = player.position().add(look.scale(ahead)).add(look.yRot(Mth.HALF_PI).scale(across));
		Vec3 ground = new Vec3(spot.x, floor(level, spot.x, spot.z, player.getY()), spot.z);
		Vec3 from = moonAt.add((random.nextDouble() - 0.5) * moonSize * 0.6, -moonSize * 0.3, (random.nextDouble() - 0.5) * moonSize * 0.6);
		float size = 1.2F + random.nextFloat() * 1.6F;
		BlockState rock = random.nextBoolean() ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState();
		Display.BlockDisplay display = blockDisplay(level, rock, from, size * 2.0F + 2.0F);
		Looks.farView(display);
		Looks.glow(display);
		pieces.add(new Piece(display, from, ground, tick, 22 + random.nextInt(10), size));
	}

	private void movePieces(ClientLevel level, LocalPlayer player, int tick) {
		for (Iterator<Piece> it = pieces.iterator(); it.hasNext();) {
			Piece piece = it.next();
			float q = (tick - piece.start()) / (float) piece.duration();
			if (q >= 1.0F) {
				despawn(level, piece.display());
				crash(level, player, piece.to(), piece.size());
				it.remove();
				continue;
			}
			// Speeding up on the way down, with a trail of fire and smoke.
			Vec3 pos = piece.from().lerp(piece.to(), q * (0.4F + 0.6F * q));
			place(piece.display(), pos, new Vector3f(piece.size()), new Quaternionf().rotateXYZ(tick * 0.21F, tick * 0.13F, tick * 0.07F));
			particles(level, ParticleTypes.FLAME, pos, 4, piece.size() * 0.3, 0.02);
			particles(level, ParticleTypes.LARGE_SMOKE, pos, 2, piece.size() * 0.3, 0.01);
		}
	}

	/** Where a piece comes down: a blast, earth flying up, fire left burning, the ground shaking under the player. */
	private void crash(ClientLevel level, LocalPlayer player, Vec3 spot, float size) {
		level.addParticle(ParticleTypes.EXPLOSION_EMITTER, spot.x, spot.y + 0.5, spot.z, 1.0, 0.0, 0.0);
		particles(level, ParticleTypes.LAVA, spot.add(0.0, 0.5, 0.0), 14, 1.5, 0.3);
		particles(level, ParticleTypes.LARGE_SMOKE, spot.add(0.0, 1.0, 0.0), 24, 2.0, 0.08);
		BlockState ground = level.getBlockState(BlockPos.containing(spot).below());
		if (!ground.isAir()) {
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), spot.add(0.0, 0.4, 0.0), 50, 1.8, 0.4);
		}
		sound(level, spot, Looks.EXPLODE, SoundSource.WEATHER, 4.0F, 0.55F + random.nextFloat() * 0.2F);
		for (int i = 0; i < 6; i++) {
			BlockPos pos = BlockPos.containing(spot.x + (random.nextDouble() - 0.5) * size * 2.0, spot.y, spot.z + (random.nextDouble() - 0.5) * size * 2.0);
			if (!level.getBlockState(pos.below()).isAir()) {
				placeBlock(level, pos, Blocks.FIRE.defaultBlockState());
			}
		}
		double distance = spot.distanceTo(player.position());
		bump = Math.max(bump, (float) (0.6 / Math.max(1.0, distance * 0.12)));
	}

	/** A block of the ground around the player tears loose and starts to float up. Never near their feet. */
	private void liftOne(ClientLevel level, LocalPlayer player, int tick) {
		BlockPos feet = player.blockPosition();
		for (int attempt = 0; attempt < 6; attempt++) {
			int dx = random.nextInt(25) - 12;
			int dz = random.nextInt(25) - 12;
			if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2) {
				continue;
			}
			double x = feet.getX() + dx + 0.5;
			double z = feet.getZ() + dz + 0.5;
			BlockPos top = BlockPos.containing(x, floor(level, x, z, player.getY()) - 1.0, z);
			BlockState state = level.getBlockState(top);
			if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity() || !level.getBlockState(top.above()).isAir()) {
				continue;
			}
			Vec3 from = Vec3.atCenterOf(top);
			Display.BlockDisplay display = blockDisplay(level, state, from, 4.0F);
			Looks.farView(display);
			hideBlock(level, top);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, state), from.add(0.0, 0.5, 0.0), 8, 0.4, 0.1);
			if (lifted.size() % 4 == 0) {
				sound(level, from, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 1.0F, 0.5F + random.nextFloat() * 0.3F);
			}
			Vector3f axis = new Vector3f(random.nextFloat() - 0.5F, random.nextFloat() - 0.5F, random.nextFloat() - 0.5F).normalize();
			lifted.add(new Lifted(display, from, tick, axis, 0.03F + random.nextFloat() * 0.06F));
			return;
		}
	}

	private record Piece(Display.BlockDisplay display, Vec3 from, Vec3 to, int start, int duration, float size) {
	}

	private record Lifted(Display.BlockDisplay display, Vec3 from, int start, Vector3f axis, float spin) {
	}
}
