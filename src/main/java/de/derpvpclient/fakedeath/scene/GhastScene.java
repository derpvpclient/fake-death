package de.derpvpclient.fakedeath.scene;

import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * In the Nether a ghast cries somewhere, floats into view and shoots: the first fireball misses,
 * the second explodes right next to the player, the third one hits.
 */
public class GhastScene extends Scene {
	private static final int APPEAR = 25;
	private static final int[] SHOTS = {60, 115, 170};
	private static final int WARN = 16;
	private static final int FLIGHT = 22;
	private static final int DEATH = SHOTS[SHOTS.length - 1] + FLIGHT + 8;
	private static final float BALL = 0.9F;
	private static final double[] ANGLES = {0.0, 30.0, -30.0, 60.0, -60.0, 90.0, -90.0, 180.0};

	private Ghast ghast;
	private Vec3 home;
	private Display.BlockDisplay ball;
	private Vec3 ballFrom;
	private Vec3 ballTo;
	private int shot = -1;
	private int shotIndex;

	@Override
	public int length() {
		return DEATH;
	}

	@Override
	public int photoTick() {
		return SHOTS[1] + 12;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.fireball", player.getDisplayName(), EntityTypes.GHAST.getDescription());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			sound(level, groundSpot(level, player, 20.0, 0.0), SoundEvents.GHAST_AMBIENT, SoundSource.HOSTILE, 3.0F, 1.0F);
		}

		if (tick == APPEAR) {
			home = freeSpot(level, player);
			ghast = spawn(level, EntityTypes.GHAST, home);
			ghast.noPhysics = true;
			sound(level, home, SoundEvents.GHAST_SCREAM, SoundSource.HOSTILE, 3.0F, 1.0F);
		}

		if (ghast == null) {
			return;
		}

		Vec3 pos = home.add(0.0, Mth.sin(tick * 0.08F) * 0.4, 0.0);
		ghast.setPos(pos.x, pos.y, pos.z);
		face(ghast, player.getEyePosition());

		if (tick % 45 == 30) {
			sound(level, pos, SoundEvents.GHAST_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.9F + random.nextFloat() * 0.2F);
		}

		for (int i = 0; i < SHOTS.length; i++) {
			if (tick == SHOTS[i] - WARN) {
				ghast.setCharging(true);
				sound(level, pos, SoundEvents.GHAST_WARN, SoundSource.HOSTILE, 3.0F, 1.0F);
			}

			if (tick == SHOTS[i]) {
				shoot(level, player, pos, i, tick);
			}
		}

		if (ball != null) {
			fly(level, player, tick);
		} else {
			turnTowards(player, ghast.position().add(0.0, 2.0, 0.0), 0.12F);
		}
	}

	private void shoot(ClientLevel level, LocalPlayer player, Vec3 pos, int index, int tick) {
		ghast.setCharging(false);
		sound(level, pos, SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 3.0F, 1.0F);

		Vec3 toPlayer = player.position().subtract(pos);
		Vec3 side = new Vec3(-toPlayer.z, 0.0, toPlayer.x).normalize();
		ballFrom = pos.add(0.0, 2.0, 0.0).add(toPlayer.normalize().scale(2.5));
		// Miss, almost, hit.
		ballTo = switch (index) {
			case 0 -> player.position().add(side.scale(4.5));
			case 1 -> player.position().add(side.scale(-1.8));
			default -> player.getEyePosition().add(0.0, -0.3, 0.0);
		};
		ball = blockDisplay(level, Blocks.MAGMA_BLOCK.defaultBlockState(), ballFrom, 2.0F);
		shot = tick;
		shotIndex = index;
	}

	private void fly(ClientLevel level, LocalPlayer player, int tick) {
		float t = (tick - shot) / (float) FLIGHT;
		Vec3 pos = ballFrom.lerp(ballTo, Math.min(1.0F, t));
		ball.setPos(pos.x, pos.y, pos.z);

		Quaternionf spin = new Quaternionf().rotateXYZ(tick * 0.3F, tick * 0.2F, 0.0F);
		transform(ball, new Transformation(new Vector3f(-BALL / 2.0F).rotate(spin), spin, new Vector3f(BALL), new Quaternionf()));
		particles(level, ParticleTypes.FLAME, pos, 4, 0.3, 0.02);
		particles(level, ParticleTypes.SMOKE, pos, 2, 0.3, 0.02);
		turnTowards(player, pos, 0.3F);

		if (t >= 1.0F) {
			despawn(level, ball);
			ball = null;
			sound(level, ballTo, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.5F, 0.9F);
			particles(level, ParticleTypes.EXPLOSION, ballTo, 4, 0.8, 0.0);
			igniteNear(level, player, 3);

			if (shotIndex == 1) {
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.3F);
			} else if (shotIndex == 2) {
				// The full hit: a big explosion right in the face, and the player burns.
				level.addParticle(ParticleTypes.EXPLOSION_EMITTER, ballTo.x, ballTo.y, ballTo.z, 1.0, 0.0, 0.0);
				particles(level, ParticleTypes.FLAME, player.getEyePosition(), 30, 0.6, 0.15);
				sound(level, player.position(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4.0F, 0.7F);
				setOnFire(player);
				kill(level, player, SoundEvents.PLAYER_HURT_ON_FIRE);
				igniteNear(level, player, 2);
				igniteNear(level, player, 2);
			}
		}
	}

	/** Where a ghast fits: open air in front of the player, otherwise somewhere around them. */
	private static Vec3 freeSpot(ClientLevel level, LocalPlayer player) {
		Vec3 first = null;

		for (double angle : ANGLES) {
			Vec3 spot = player.position().add(horizontalLook(player).yRot((float) Math.toRadians(angle)).scale(16.0)).add(0.0, 5.0, 0.0);
			first = first == null ? spot : first;
			BlockPos pos = BlockPos.containing(spot);

			if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above(2)).isAir() && level.getBlockState(pos.above(4)).isAir()) {
				return spot;
			}
		}

		return first;
	}
}
