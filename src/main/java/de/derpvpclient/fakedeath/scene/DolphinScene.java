package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.phys.Vec3;

/** At the beach a dolphin plays in the water, twice it jumps a little closer, the third time right into the player's face. */
public class DolphinScene extends Scene {
	private static final int[] JUMPS = {30, 70};
	private static final int LEAP = 110;
	private static final int HIT = LEAP + 16;
	private final BlockPos water;
	private Dolphin dolphin;
	private Vec3 home;

	public DolphinScene(BlockPos water) {
		this.water = water;
	}

	@Override
	public int length() {
		return HIT + 30;
	}

	@Override
	public int photoTick() {
		return LEAP + 10;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.delfin", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			BlockPos seen = visibleWater(level, player);
			home = seen != null ? Vec3.atCenterOf(seen) : groundSpot(level, player, 6.0, 0.0);
			Vec3 flat = home.subtract(player.position());
			flat = new Vec3(flat.x, 0.0, flat.z);
			if (flat.length() < 5.0) {
				home = player.position().add(flat.lengthSqr() < 0.01 ? horizontalLook(player).scale(6.0) : flat.normalize().scale(6.0)).add(0.0, home.y - player.getY(), 0.0);
			}
			dolphin = spawn(level, EntityType.DOLPHIN, home);
			dolphin.setNoGravity(true);
		}
		if (dolphin == null) {
			return;
		}

		Vec3 face = player.getEyePosition();
		Vec3 at;
		if (tick < LEAP) {
			at = home;
			for (int i = 0; i < JUMPS.length; i++) {
				int t = tick - JUMPS[i];
				if (t >= 0 && t < 20) {
					// A playful jump out of the water, each one closer.
					Vec3 to = home.lerp(player.position(), 0.25 * (i + 1));
					at = home.lerp(to, t / 20.0).add(0.0, Mth.sin(t / 20.0F * Mth.PI) * 3.5, 0.0);
					if (t == 0) {
						sound(level, at, SoundEvents.DOLPHIN_JUMP, SoundSource.NEUTRAL, 1.0F, 1.0F);
					}
					if (t == 19) {
						sound(level, to, SoundEvents.DOLPHIN_SPLASH, SoundSource.NEUTRAL, 1.0F, 1.0F);
						particles(level, ParticleTypes.SPLASH, to, 60, 0.9, 0.4);
						particles(level, ParticleTypes.BUBBLE_POP, to, 20, 0.6, 0.2);
					}
				} else if (t >= 20 && (i == JUMPS.length - 1 || tick < JUMPS[i + 1])) {
					at = home.lerp(player.position(), 0.25 * (i + 1));
				}
			}
			if (tick % 8 == 0) {
				particles(level, ParticleTypes.BUBBLE, dolphin.position(), 4, 0.4, 0.1);
			}
		} else {
			Vec3 from = home.lerp(player.position(), 0.5);
			float p = Math.min(1.0F, (tick - LEAP) / (float) (HIT - LEAP));
			at = from.lerp(face.add(0.0, -0.6, 0.0), p).add(0.0, Mth.sin(p * Mth.PI) * 1.5, 0.0);
			if (tick == LEAP) {
				sound(level, from, SoundEvents.DOLPHIN_JUMP, SoundSource.NEUTRAL, 1.5F, 0.9F);
				particles(level, ParticleTypes.SPLASH, from, 30, 0.6, 0.3);
			}
		}
		dolphin.setPos(at.x, at.y, at.z);
		face(dolphin, tick < LEAP ? at.add(player.position().subtract(at).normalize()) : face);
		turnTowards(player, dolphin.position(), 0.2F);

		if (tick % 2 == 0 && dolphin.getY() > home.y + 0.5) {
			// Water dripping off while it is in the air.
			particles(level, ParticleTypes.FALLING_WATER, dolphin.position(), 3, 0.4, 0.0);
		}

		if (tick == HIT) {
			sound(level, player.position(), SoundEvents.DOLPHIN_ATTACK, SoundSource.NEUTRAL, 1.5F, 1.0F);
			particles(level, ParticleTypes.SPLASH, face, 40, 0.5, 0.3);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	/** Water the player can really see from where they stand, not behind a dune: open to the sky, nothing in between. */
	private static BlockPos visibleWater(ClientLevel level, LocalPlayer player) {
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.betweenClosed(player.blockPosition().offset(-12, -4, -12), player.blockPosition().offset(12, 2, 12))) {
			if (!level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER) || !level.getBlockState(pos.above()).isAir()) {
				continue;
			}
			double distance = pos.distSqr(player.blockPosition());
			if (distance < 16 || distance >= bestDistance) {
				continue;
			}
			Vec3 top = Vec3.atCenterOf(pos).add(0.0, 0.6, 0.0);
			var hit = level.clip(new net.minecraft.world.level.ClipContext(player.getEyePosition(), top, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
			if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
				best = pos.immutable();
				bestDistance = distance;
			}
		}
		return best;
	}
}
