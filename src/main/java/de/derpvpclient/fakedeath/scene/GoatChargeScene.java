package de.derpvpclient.fakedeath.scene;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The goat next to the player looks at them, turns around and walks away. Twenty blocks, counted out loud. Then it
 * turns, lowers its head, and takes all twenty blocks as a run-up.
 */
public class GoatChargeScene extends Scene {
	private static final byte LOWER_HEAD = 58;
	private static final int BLOCKS = 20;
	private static final int LEAVE = 14;
	private static final int PER_BLOCK = 3;
	private static final int THERE = LEAVE + BLOCKS * PER_BLOCK;
	private static final int LOWER = THERE + 10;
	private static final int CHARGE = LOWER + 20;
	private static final int HIT = CHARGE + 20;
	private final Entity found;
	private LivingEntity goat;
	private Vec3 start;
	private Vec3 dir;

	public GoatChargeScene(Entity goat) {
		this.found = goat;
	}

	@Override
	public int length() {
		return HIT + 20;
	}

	@Override
	public int photoTick() {
		return HIT - 3;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.anlauf", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		Vec3 feet = player.position();

		if (tick == 0) {
			if (found != null && found.isAlive()) {
				// The real goat steps aside for the scene, its double takes the run-up.
				start = found.position();
				found.setInvisible(true);
			} else {
				start = groundSpot(level, player, 2.5, 10.0);
			}

			Vec3 away = start.subtract(feet);
			Vec3 flat = new Vec3(away.x, 0.0, away.z);
			dir = flat.lengthSqr() < 0.3 ? horizontalLook(player) : flat.normalize();
			start = feet.add(dir.scale(2.2));
			goat = spawn(level, EntityTypes.GOAT, ground(level, start, player));
			goat.noPhysics = true;
			sound(level, start, SoundEvents.GOAT_AMBIENT, SoundSource.NEUTRAL, 1.5F, 1.0F);
		}

		Vec3 far = feet.add(dir.scale(2.2 + BLOCKS));
		Vec3 at;

		if (tick < LEAVE) {
			at = start;
			face(goat, player.getEyePosition());
		} else if (tick < THERE) {
			// Away, at an easy pace, every fifth block counted.
			float p = (tick - LEAVE) / (float) (THERE - LEAVE);
			at = start.lerp(far, p);
			face(goat, far.add(dir.scale(4.0)).add(0.0, 1.0, 0.0));
			int block = (tick - LEAVE) / PER_BLOCK + 1;
			if ((tick - LEAVE) % PER_BLOCK == 0) {
				sound(level, at, SoundEvents.GOAT_STEP, SoundSource.NEUTRAL, 1.5F, 1.0F);
				if (block % 5 == 0) {
					say(minecraft, EntityTypes.GOAT.getDescription().copy().withStyle(ChatFormatting.GRAY), Component.literal(block + (block == BLOCKS ? "." : " …")));
				}
			}
		} else if (tick < CHARGE) {
			at = far;
			face(goat, player.getEyePosition());
			if (tick > LOWER && tick % 6 == 0) {
				// Scraping the ground with its hooves.
				sound(level, at, SoundEvents.GOAT_STEP, SoundSource.NEUTRAL, 2.0F, 0.7F);
				particles(level, ParticleTypes.POOF, ground(level, at, player), 4, 0.3, 0.05);
			}
		} else {
			// All twenty blocks back, faster with every one of them.
			float p = Math.min(1.0F, (tick - CHARGE) / (float) (HIT - CHARGE));
			at = far.lerp(feet.add(dir.scale(0.8)), p * p);
			face(goat, player.getEyePosition());
			if (tick < HIT && tick % 2 == 0) {
				sound(level, at, SoundEvents.GOAT_STEP, SoundSource.NEUTRAL, 2.0F, 1.0F + p * 0.4F);
				particles(level, ParticleTypes.POOF, ground(level, at, player), 3, 0.2, 0.03);
			}
		}

		walk(goat, ground(level, at, player));
		goat.setXRot(0.0F);

		if (tick == THERE) {
			sound(level, far, SoundEvents.GOAT_SCREAMING_AMBIENT, SoundSource.NEUTRAL, 4.0F, 1.0F);
		}
		if (tick == LOWER) {
			goat.handleEntityEvent(LOWER_HEAD);
			sound(level, far, SoundEvents.GOAT_SCREAMING_PREPARE_RAM, SoundSource.NEUTRAL, 4.0F, 1.0F);
		}

		if (tick < HIT) {
			turnTowards(player, goat.position().add(0.0, 0.9, 0.0), tick >= CHARGE ? 0.35F : 0.15F);
		}

		if (tick == HIT) {
			sound(level, feet, SoundEvents.GOAT_SCREAMING_RAM_IMPACT, SoundSource.NEUTRAL, 3.0F, 0.8F);
			sound(level, feet, Looks.EXPLODE, SoundSource.NEUTRAL, 0.6F, 1.6F);
			particles(level, ParticleTypes.CRIT, player.getEyePosition().add(0.0, -0.4, 0.0), 30, 0.4, 0.6);
			particles(level, ParticleTypes.POOF, feet, 15, 0.4, 0.1);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}

		if (tick >= HIT) {
			// With twenty blocks of run-up the flight goes far: up and backwards, the sky in view.
			float p = Math.min(1.0F, (tick - HIT) / 18.0F);
			moveCamera(dir.scale(-9.0 * p).add(0.0, Math.sin(p * Math.PI * 0.75) * 5.0, 0.0));
			turnTowards(player, player.getEyePosition().add(dir.scale(-1.0)).add(0.0, 6.0, 0.0), 0.3F);
		}
	}

	private static Vec3 ground(ClientLevel level, Vec3 spot, LocalPlayer player) {
		return new Vec3(spot.x, floor(level, spot.x, spot.z, player.getY()), spot.z);
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		if (found != null) {
			found.setInvisible(false);
		}
		goat = null;
		moveCamera(Vec3.ZERO);
		super.end(level, player);
	}
}
