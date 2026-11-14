package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

/**
 * A thunderstorm builds up: rumbling far away, a bolt in the distance, the air starts to crackle,
 * then a real looking bolt hits the player and they burn. Only visible on this client.
 */
public class LightningScene extends Scene {
	private static final int DISTANT_BOLT = 50;
	private static final int CHARGE = 72;
	private static final int STRIKE = 90;
	private static final int SECOND = STRIKE + 38;
	private static final int DEATH = SECOND + 2;

	private Vec3 distantBolt;
	private int now;

	@Override
	public int screenTint() {
		// Darker and darker under the storm, white for a moment at the strike.
		if ((now >= STRIKE && now < STRIKE + 4) || (now >= SECOND && now < SECOND + 3)) {
			return 0xC0FFFFFF;
		}
		return (Math.min(120, now * 2) << 24) | 0x101828;
	}

	@Override
	public int length() {
		return DEATH;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.lightningBolt", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		Vec3 far = player.position().add(horizontalLook(player).scale(-40.0)).add(0, 20, 0);
		now = tick;
		if (tick < DEATH) {
			for (int i = 0; i < 12; i++) {
				level.addParticle(ParticleTypes.FALLING_WATER, player.getX() + (random.nextDouble() - 0.5) * 14, player.getY() + 6 + random.nextDouble() * 3, player.getZ() + (random.nextDouble() - 0.5) * 14, 0, -1.0, 0);
			}
		}
		if (tick == 12 || tick == 48) {
			// Bolts far away, closer each time.
			strike(level, groundSpot(level, player, tick == 12 ? 40.0 : 25.0, tick == 12 ? -50.0 : 60.0));
			level.setSkyFlashTime(2);
		}

		if (tick == 0) {
			sound(level, far, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.0F, 0.55F);
		}

		if (tick == 28) {
			level.setSkyFlashTime(2);
			sound(level, far, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 4.0F, 0.65F);
		}

		if (tick == DISTANT_BOLT) {
			distantBolt = groundSpot(level, player, 14.0, 30.0);
			strike(level, distantBolt);
		}

		if (distantBolt != null && tick > DISTANT_BOLT && tick < CHARGE) {
			turnTowards(player, distantBolt.add(0, 4, 0), 0.12F);
		}

		// The air around the player starts to crackle, the next one is coming here.
		if (tick >= CHARGE && tick < STRIKE) {
			particles(level, ParticleTypes.ELECTRIC_SPARK, player.position().add(0, 1.0, 0), 4, 0.6, 0.3);

			if (tick % 6 == 0) {
				sound(level, player.position(), SoundEvents.BEACON_POWER_SELECT, SoundSource.WEATHER, 0.4F, 1.8F);
			}
		}

		if (tick >= CHARGE + 8 && tick < STRIKE) {
			// The hair stands on end: the view jerks up into the black clouds, sparks crackle around the head.
			turnTowards(player, player.getEyePosition().add(horizontalLook(player).scale(2.0)).add(0.0, 8.0, 0.0), 0.3F);
			particles(level, ParticleTypes.ELECTRIC_SPARK, player.getEyePosition().add(0.0, 0.3, 0.0), 6, 0.35, 0.4);
			cameraShake(0.03F);
		}
		if (tick == STRIKE - 3) {
			sound(level, player.position(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.WEATHER, 1.5F, 1.2F);
		}
		if (tick == STRIKE) {
			strike(level, player.position());
			sound(level, player.position(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 3.0F, 0.8F);
			particles(level, ParticleTypes.EXPLOSION, player.position().add(0.0, 0.5, 0.0), 3, 0.5, 0.0);
			particles(level, ParticleTypes.ELECTRIC_SPARK, player.position().add(0.0, 1.0, 0.0), 40, 1.2, 0.8);
			setOnFire(player);
			hurt(level, player, SoundEvents.PLAYER_HURT_ON_FIRE, 0.5F);
		}
		if (tick >= STRIKE && tick < STRIKE + 12) {
			// Knocked to the ground.
			cameraShake(0.3F * (STRIKE + 12 - tick) / 12.0F);
			float p = (tick - STRIKE) / 12.0F;
			moveCamera(new Vec3(0.0, -1.3 * p * p, 0.0));
		} else if (tick >= STRIKE + 12) {
			moveCamera(new Vec3(0.0, -1.3, 0.0));
			turnTowards(player, player.getEyePosition().add(0.0, 6.0, 0.0).add(horizontalLook(player).scale(3.0)), 0.06F);
		}
		if (tick == SECOND - 12) {
			sound(level, player.position(), SoundEvents.BEACON_POWER_SELECT, SoundSource.WEATHER, 0.8F, 1.9F);
		}
		if (tick == SECOND) {
			// A second one, right where the player lies.
			strike(level, player.position());
			sound(level, player.position(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 3.0F, 0.6F);
			kill(level, player, SoundEvents.PLAYER_HURT_ON_FIRE);
		}

		if (tick > STRIKE) {
			particles(level, ParticleTypes.FLAME, player.position().add(0, 0.5, 0), 3, 0.4, 0.05);
			particles(level, ParticleTypes.LARGE_SMOKE, player.position().add(0, 1.2, 0), 1, 0.4, 0.05);

			if (tick % 5 == 0) {
				igniteNear(level, player, 2);
			}

			if ((tick - STRIKE) % 13 == 6 && tick < SECOND - 6) {
				hurt(level, player, SoundEvents.PLAYER_HURT_ON_FIRE, 0.12F);
			}
		}
	}

	@Override
	public float lying() {
		// In F5 the body lies flat on the ground after the strike.
		return now >= STRIKE + 6 ? 1.0F : 0.0F;
	}

	private void strike(ClientLevel level, Vec3 pos) {
		LightningBolt bolt = spawn(level, EntityType.LIGHTNING_BOLT, pos);
		bolt.setVisualOnly(true);
		// The bolt plays its own thunder on the client.
		bolt.setSilent(false);
	}
}
