package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/** A bright, hot day: the screen turns red, smoke rises from the player, and the hearts melt away. */
public class SunburnScene extends Scene {
	private static final int[] BURNS = {50, 80, 105, 125};
	private static final int DEATH = 140;
	private int heat;
	private net.minecraft.world.entity.Display.BlockDisplay sun;
	private net.minecraft.world.phys.Vec3 sky;

	@Override
	public int length() {
		return DEATH + 20;
	}

	@Override
	public int photoTick() {
		return 110;
	}

	@Override
	public int screenTint() {
		return (heat << 24) | 0xFF8020;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.sonnenbrand", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		heat = Mth.clamp(tick / 2, 0, 80);
		if (tick == 0) {
			// The sun comes down closer and closer, burning bright.
			sky = player.getEyePosition().add(horizontalLook(player).scale(60.0)).add(0.0, 45.0, 0.0);
			sun = blockDisplay(level, net.minecraft.world.level.block.Blocks.OCHRE_FROGLIGHT.defaultBlockState(), sky, 60.0F);
			Looks.farView(sun);
			Looks.glow(sun);
		}
		if (sun != null) {
			float p = Math.min(1.0F, tick / (float) DEATH);
			net.minecraft.world.phys.Vec3 at = sky.lerp(player.getEyePosition().add(horizontalLook(player).scale(25.0)).add(0.0, 18.0, 0.0), p * p);
			place(sun, at, new org.joml.Vector3f(6.0F + p * 14.0F), new org.joml.Quaternionf().rotateY(tick * 0.01F));
			turnTowards(player, at, 0.05F);
		}
		if (tick % 4 == 0) {
			particles(level, ParticleTypes.WHITE_ASH, player.getEyePosition(), 4, 1.5, 0.02);
		}
		if (tick > 40 && tick % 3 == 0) {
			particles(level, ParticleTypes.SMOKE, player.position().add(0.0, 1.0, 0.0), 3, 0.3, 0.02);
		}
		if (tick % 25 == 0) {
			sound(level, player.position(), SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.6F + tick / 150.0F, 1.2F);
		}
		for (int burn : BURNS) {
			if (tick == burn) {
				hurt(level, player, SoundEvents.PLAYER_HURT_ON_FIRE, 0.22F);
			}
		}
		if (tick == DEATH) {
			setOnFire(player);
			kill(level, player, SoundEvents.PLAYER_HURT_ON_FIRE);
		}
	}
}
