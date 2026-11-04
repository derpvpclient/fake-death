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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.phys.Vec3;

/**
 * Deep in a cave: squeaking echoes closer and closer, then a whole swarm of bats bursts out of the
 * dark, whirls around the player and scratches until they go down.
 */
public class BatScene extends Scene {
	private static final int SWARM = 40;
	private static final int COUNT = 28;
	private static final int FIRST_BITE = SWARM + COUNT + 15;
	private static final int BITE_EVERY = 12;
	private static final int BITES = 6;
	private static final int DEATH = FIRST_BITE + BITE_EVERY * BITES;

	private final List<Flyer> bats = new ArrayList<>();
	private Vec3 cave;

	@Override
	public int length() {
		return DEATH;
	}

	@Override
	public int photoTick() {
		return FIRST_BITE + 5;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.bats", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			cave = player.getEyePosition().add(horizontalLook(player).yRot((float) Math.toRadians((random.nextDouble() - 0.5) * 90.0)).scale(10.0));
		}

		if (tick < SWARM && tick % Math.max(2, 10 - tick / 5) == 0) {
			// Squeaks from deep in the cave, faster and faster.
			sound(level, cave, SoundEvents.BAT_AMBIENT, SoundSource.AMBIENT, 1.0F + tick / 30.0F, 0.8F + random.nextFloat() * 0.4F);
		}

		if (tick < SWARM) {
			turnTowards(player, cave, 0.06F);
		}

		int since = tick - SWARM;
		if (since >= 0 && since < COUNT) {
			Bat bat = spawn(level, EntityType.BAT, cave);
			bat.setResting(false);
			bat.noPhysics = true;
			bats.add(new Flyer(bat, cave, random.nextFloat() * Mth.TWO_PI, (0.12F + random.nextFloat() * 0.15F) * (random.nextBoolean() ? 1 : -1),
				0.9 + random.nextDouble() * 1.6, (random.nextDouble() - 0.5) * 1.6, random.nextFloat() * Mth.TWO_PI));

			if (since % 4 == 0) {
				sound(level, cave, SoundEvents.BAT_TAKEOFF, SoundSource.AMBIENT, 1.5F, 0.9F + random.nextFloat() * 0.2F);
			}
		}

		for (Flyer flyer : bats) {
			flyer.fly(player, tick);
		}

		if (!bats.isEmpty()) {
			if (tick % 3 == 0) {
				Flyer loud = bats.get(random.nextInt(bats.size()));
				sound(level, loud.pos, SoundEvents.BAT_AMBIENT, SoundSource.AMBIENT, 1.2F, 0.8F + random.nextFloat() * 0.5F);
			}

			if (tick % 8 == 0) {
				// The view jumps from bat to bat.
				turnTowards(player, bats.get(random.nextInt(bats.size())).pos, 0.35F);
			}
		}

		int bite = tick - FIRST_BITE;
		if (bite >= 0 && bite % BITE_EVERY == 0 && tick < DEATH) {
			sound(level, player.getEyePosition(), SoundEvents.BAT_HURT, SoundSource.AMBIENT, 1.0F, 1.2F);
			// One of them goes straight for the face.
			if (!bats.isEmpty()) {
				bats.get(random.nextInt(bats.size())).pos = player.getEyePosition().add(horizontalLook(player).scale(0.35));
			}
			particles(level, ParticleTypes.CRIT, player.getEyePosition().add(horizontalLook(player).scale(0.4)), 8, 0.3, 0.3);
			hit(level, player, SoundEvents.PLAYER_HURT, 0.18F, bite / BITE_EVERY == BITES - 1);
		}
	}

	private static final class Flyer {
		final Bat bat;
		Vec3 pos;
		float angle;
		final float speed;
		final double radius;
		final double height;
		final float phase;

		Flyer(Bat bat, Vec3 pos, float angle, float speed, double radius, double height, float phase) {
			this.bat = bat;
			this.pos = pos;
			this.angle = angle;
			this.speed = speed;
			this.radius = radius;
			this.height = height;
			this.phase = phase;
		}

		void fly(LocalPlayer player, int tick) {
			angle += speed;
			Vec3 orbit = player.getEyePosition().add(Mth.cos(angle) * radius, height + Mth.sin(tick * 0.3F + phase) * 0.4, Mth.sin(angle) * radius);
			Vec3 next = pos.lerp(orbit, 0.2);
			Vec3 delta = next.subtract(pos);
			pos = next;

			bat.setPos(pos.x, pos.y - 0.45, pos.z);
			if (delta.lengthSqr() > 1.0E-4) {
				face(bat, bat.getEyePosition().add(delta));
			}
		}
	}
}
