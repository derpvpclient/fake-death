package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The Nether portal next to the player starts to pull. The view is dragged into it bit by bit, everything turns
 * purple, and only half of the player arrives on the other side. The player really stays where they are.
 */
public class NetherPortalScene extends Scene {
	private static final int PULL = 40;
	private static final int THROUGH = 125;
	private final BlockPos found;
	private Vec3 center;
	private float tint;

	public NetherPortalScene(BlockPos portal) {
		this.found = portal;
	}

	@Override
	public int length() {
		return THROUGH + 25;
	}

	@Override
	public int photoTick() {
		return THROUGH - 30;
	}

	@Override
	public int screenTint() {
		return tint > 0.0F ? ((int) (Math.min(tint, 1.0F) * 215.0F) << 24) | 0x5B1FB5 : 0;
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
			center = middle(level, player);
			sound(level, center, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 0.6F, 1.0F);
		}
		if (center == null) {
			return;
		}

		turnTowards(player, center, 0.15F);

		if (tick > THROUGH) {
			return;
		}

		Vec3 eyes = player.getEyePosition();
		Vec3 way = center.subtract(eyes);
		float progress = tick < PULL ? 0.0F : (tick - PULL) / (float) (THROUGH - PULL);

		// Purple sparks are drawn out of the air around the player and into the portal.
		for (int i = 0; i < 2 + (int) (progress * 6.0F); i++) {
			Vec3 from = eyes.add((random.nextDouble() - 0.5) * 3.0, (random.nextDouble() - 0.7) * 2.0, (random.nextDouble() - 0.5) * 3.0);
			Vec3 speed = center.subtract(from).normalize().scale(0.15 + progress * 0.25);
			level.addParticle(ParticleTypes.REVERSE_PORTAL, from.x, from.y, from.z, speed.x, speed.y, speed.z);
		}
		if (tick % 30 == 0) {
			sound(level, center, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.8F + progress, 0.8F + progress * 0.6F);
		}

		if (tick == PULL) {
			addEffect(player, MobEffects.CONFUSION, 300);
		}
		if (tick >= PULL && tick < THROUGH) {
			// The pull comes in waves and gets stronger, like something tugging again and again.
			float pulled = progress * progress * (0.85F + 0.15F * (float) Math.sin(tick * 0.5));
			double shake = 0.04 + progress * 0.06;
			moveCamera(way.scale(Math.min(pulled, 0.95F)).add((random.nextDouble() - 0.5) * shake, (random.nextDouble() - 0.5) * shake, (random.nextDouble() - 0.5) * shake));
			tint = progress * progress * 0.8F;

			if (tick == PULL + 30 || tick == PULL + 58) {
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.3F);
			}
		}
		if (tick == THROUGH) {
			moveCamera(way.scale(0.97));
			tint = 1.0F;
			sound(level, eyes, SoundEvents.PORTAL_TRAVEL, SoundSource.BLOCKS, 0.4F, 1.0F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	/** The middle of the portal at the height of the eyes, taken from the portal block closest to the player. */
	private Vec3 middle(ClientLevel level, LocalPlayer player) {
		BlockPos feet = player.blockPosition();
		BlockPos nearest = null;

		for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-6, -3, -6), feet.offset(6, 3, 6))) {
			if (level.getBlockState(pos).is(Blocks.NETHER_PORTAL) && (nearest == null || pos.distSqr(feet) < nearest.distSqr(feet))) {
				nearest = pos.immutable();
			}
		}
		if (nearest == null) {
			nearest = found;
		}
		if (nearest == null) {
			return null;
		}

		BlockPos bottom = nearest;
		while (level.getBlockState(bottom.below()).is(Blocks.NETHER_PORTAL) && nearest.getY() - bottom.getY() < 24) {
			bottom = bottom.below();
		}

		return new Vec3(nearest.getX() + 0.5, bottom.getY() + player.getEyeHeight(), nearest.getZ() + 0.5);
	}
}
