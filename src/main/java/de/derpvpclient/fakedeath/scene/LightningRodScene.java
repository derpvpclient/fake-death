package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

/**
 * Clouds gather over the lightning rod. The first bolt hits the rod, as it should. The second one too. The third one
 * has seen enough of the rod and takes the player standing next to it.
 */
public class LightningRodScene extends Scene {
	private static final int BOLT_1 = 38;
	private static final int BOLT_2 = 70;
	private static final int CHARGE = 84;
	private static final int BOLT_3 = 110;
	private final BlockPos found;
	private Vec3 rod;
	private int now;

	public LightningRodScene(BlockPos rod) {
		this.found = rod;
	}

	@Override
	public int length() {
		return BOLT_3 + 20;
	}

	@Override
	public int photoTick() {
		return BOLT_2 + 4;
	}

	@Override
	public int screenTint() {
		// Darker and darker under the clouds, white for a moment with each bolt.
		if (now >= BOLT_1 && now < BOLT_1 + 3 || now >= BOLT_2 && now < BOLT_2 + 3 || now >= BOLT_3 && now < BOLT_3 + 5) {
			return now >= BOLT_3 ? 0xD0FFFFFF : 0x70FFFFFF;
		}
		return (Math.min(130, now * 4) << 24) | 0x1A1E2A;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.blitzableiter", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		now = tick;
		Vec3 feet = player.position();

		if (tick == 0) {
			BlockPos block = found;
			if (block == null) {
				// Asked for by name with no rod around: one is put up in front, only on this client.
				block = BlockPos.containing(groundSpot(level, player, 3.0, 0.0));
				placeBlock(level, block, Nearby.block("lightning_rod").defaultBlockState());
			}
			rod = Vec3.atCenterOf(block).add(0.0, 0.5, 0.0);
			sound(level, feet.add(0.0, 20.0, 0.0), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.0F, 0.55F);
		}

		// The clouds: a gray lid that closes over the rod, and then the rain.
		if (tick < BOLT_3) {
			int clouds = Math.min(14, 2 + tick / 4);
			for (int i = 0; i < clouds; i++) {
				level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, rod.x + (random.nextDouble() - 0.5) * 26.0, rod.y + 6.0 + random.nextDouble() * 1.5, rod.z + (random.nextDouble() - 0.5) * 26.0,
					(random.nextDouble() - 0.5) * 0.04, -0.04, (random.nextDouble() - 0.5) * 0.04);
			}
		}
		if (tick > 16) {
			for (int i = 0; i < 12; i++) {
				level.addParticle(ParticleTypes.FALLING_WATER, feet.x + (random.nextDouble() - 0.5) * 14.0, feet.y + 6.0 + random.nextDouble() * 3.0, feet.z + (random.nextDouble() - 0.5) * 14.0, 0.0, -1.0, 0.0);
			}
		}
		if (tick == 20) {
			level.setSkyFlashTime(2);
			sound(level, feet.add(0.0, 20.0, 0.0), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 4.0F, 0.7F);
		}

		if (tick < BOLT_1 - 10) {
			turnTowards(player, rod.add(0.0, 6.0, 0.0), 0.1F);
		} else if (tick < CHARGE) {
			turnTowards(player, rod, 0.2F);
		} else if (tick < BOLT_3) {
			// The hair stands up. A look to the sky, and that was the mistake.
			turnTowards(player, player.getEyePosition().add(horizontalLook(player).scale(0.5)).add(0.0, 4.0, 0.0), 0.12F);
		}

		if (tick == BOLT_1 || tick == BOLT_2) {
			strike(level, rod);
			particles(level, ParticleTypes.ELECTRIC_SPARK, rod, 40, 0.5, 1.2);
			sound(level, rod, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 2.0F, 1.0F);
		}
		if (tick > BOLT_1 && tick < BOLT_1 + 14 || tick > BOLT_2 && tick < BOLT_2 + 14) {
			particles(level, ParticleTypes.ELECTRIC_SPARK, rod, 5, 0.4, 0.6);
		}

		if (tick >= CHARGE && tick < BOLT_3) {
			// The air around the player crackles, and a few sparks already jump over from the rod.
			particles(level, ParticleTypes.ELECTRIC_SPARK, feet.add(0.0, 1.0, 0.0), 3 + (tick - CHARGE) / 4, 0.6, 0.3);
			if (tick % 5 == 0) {
				line(level, ParticleTypes.ELECTRIC_SPARK, rod, feet.add(0.0, 1.0 + random.nextDouble(), 0.0), 0.35);
				sound(level, feet, SoundEvents.BEACON_POWER_SELECT, SoundSource.WEATHER, 0.5F, 1.4F + (tick - CHARGE) * 0.02F);
			}
		}

		if (tick == BOLT_3) {
			strike(level, feet);
			setOnFire(player);
			particles(level, ParticleTypes.ELECTRIC_SPARK, feet.add(0.0, 1.0, 0.0), 50, 0.6, 1.5);
			particles(level, ParticleTypes.LARGE_SMOKE, feet.add(0.0, 1.0, 0.0), 12, 0.4, 0.05);
			kill(level, player, SoundEvents.PLAYER_HURT_ON_FIRE);
		}
	}

	private void strike(ClientLevel level, Vec3 pos) {
		LightningBolt bolt = spawn(level, EntityTypes.LIGHTNING_BOLT, pos);
		bolt.setVisualOnly(true);
		// The bolt plays its own thunder on the client.
		bolt.setSilent(false);
		level.setSkyFlashTime(2);
	}
}
