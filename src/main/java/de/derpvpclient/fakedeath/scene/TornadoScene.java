package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
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

/** A tornado forms in the distance, comes closer, lifts the player high up, spins them around and drops them. */
public class TornadoScene extends Scene {
	private static final int ARRIVE = 100;
	private static final int LIFT = 45;
	private static final int DROP = 12;
	private static final double HEIGHT = 14.0;
	private static final int DEATH = ARRIVE + LIFT + 20 + DROP;
	private final List<Display.BlockDisplay> debris = new ArrayList<>();
	private Vec3 start;

	@Override
	public int length() {
		return DEATH + 20;
	}

	@Override
	public int photoTick() {
		return ARRIVE - 25;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.tornado", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			start = groundSpot(level, player, 22.0, 20.0);
			for (int i = 0; i < 9; i++) {
				Display.BlockDisplay piece = blockDisplay(level, i % 3 == 0 ? Blocks.DIRT.defaultBlockState() : i % 3 == 1 ? Blocks.OAK_LEAVES.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState(), start, 6.0F);
				Looks.farView(piece);
				debris.add(piece);
			}
		}

		Vec3 center = tick < ARRIVE ? start.lerp(player.position(), tick / (double) ARRIVE) : player.position();
		funnel(level, center, tick);

		if (tick % 20 == 0 && tick < DEATH) {
			sound(level, center, SoundEvents.ELYTRA_FLYING, SoundSource.WEATHER, 1.0F + Math.min(2.0F, tick / 50.0F), 0.6F);
			sound(level, center, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 1.0F, 0.5F);
		}

		if (tick < ARRIVE) {
			turnTowards(player, center.add(0.0, 5.0, 0.0), 0.08F);
			cameraShake(tick / (float) ARRIVE * 0.15F);
		} else if (tick < ARRIVE + LIFT + 20) {
			// Up in the funnel, around and around.
			float p = Math.min(1.0F, (tick - ARRIVE) / (float) LIFT);
			double angle = (tick - ARRIVE) * 0.35;
			moveCamera(new Vec3(Math.cos(angle) * 1.5, HEIGHT * p, Math.sin(angle) * 1.5));
			turnTowards(player, orbit(player.getEyePosition().add(0.0, HEIGHT * p, 0.0), 6.0, angle + 1.6, -2.0), 0.6F);
			if (tick == ARRIVE + LIFT / 2) {
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.3F);
			}
		} else if (tick <= DEATH) {
			float p = (tick - ARRIVE - LIFT - 20) / (float) DROP;
			moveCamera(new Vec3(0.0, HEIGHT * (1.0F - p * p), 0.0));
			turnTowards(player, player.position().add(horizontalLook(player).scale(3.0)), 0.5F);
		}

		if (tick == DEATH) {
			moveCamera(Vec3.ZERO);
			sound(level, player.position(), SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 1.5F, 1.0F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void funnel(ClientLevel level, Vec3 center, int tick) {
		// A cone of cloud particles, thin at the ground and wide at the top, turning fast.
		for (int i = 0; i < 45; i++) {
			double h = random.nextDouble() * 20.0;
			double r = 0.4 + h * 0.35;
			double a = tick * 0.5 + random.nextDouble() * Math.PI * 2;
			Vec3 p = orbit(center, r, a, h);
			level.addParticle(h < 2.0 ? ParticleTypes.POOF : i % 3 == 0 ? ParticleTypes.CAMPFIRE_COSY_SMOKE : i % 3 == 1 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CLOUD, p.x, p.y, p.z, -Math.sin(a) * 0.3, 0.05, Math.cos(a) * 0.3);
		}
		for (int i = 0; i < debris.size(); i++) {
			double h = 2.0 + i * 1.8;
			Vec3 p = orbit(center, 1.0 + h * 0.3, tick * 0.4 + i * 1.7, h);
			place(debris.get(i), p, new Vector3f(0.7F), new Quaternionf().rotateXYZ(tick * 0.3F, tick * 0.2F, i));
		}
	}
}
