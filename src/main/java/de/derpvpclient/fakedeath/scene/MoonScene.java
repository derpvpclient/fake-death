package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** At night the moon grows bigger and bigger, the ground starts to rumble, and then it comes down. */
public class MoonScene extends Scene {
	private static final int FALL = 170;
	private Display.BlockDisplay moon;
	private Vec3 sky;
	private int flash;

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
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.mond", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			sky = player.getEyePosition().add(horizontalLook(player).scale(90.0)).add(0.0, 70.0, 0.0);
			moon = blockDisplay(level, Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState(), sky, 80.0F);
			Looks.farView(moon);
			Looks.glow(moon);
		}
		if (flash > 0) {
			flash--;
		}
		if (tick <= FALL) {
			// Faster and faster towards the player, and bigger.
			float p = tick / (float) FALL;
			Vec3 at = sky.lerp(player.getEyePosition().add(horizontalLook(player).scale(6.0)).add(0.0, 4.0, 0.0), p * p * p);
			float size = 8.0F + p * p * 40.0F;
			place(moon, at, new Vector3f(size), new Quaternionf().rotateY(tick * 0.004F).rotateX(0.3F));
			turnTowards(player, at, 0.08F);
			cameraShake(p * p * 0.35F);
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
		}
		if (tick == FALL) {
			flash = 10;
			cameraShake(0.0F);
			sound(level, player.position(), Looks.EXPLODE, SoundSource.WEATHER, 3.0F, 0.4F);
			particles(level, ParticleTypes.EXPLOSION_EMITTER, player.position(), 3, 2.0, 0.0);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}
}
