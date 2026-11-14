package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.phys.Vec3;

/**
 * The ground starts to shake, heavy steps from far away. A giant zombie comes running, every step shakes the
 * camera harder, and it does not stop: it runs right over the player and squashes them flat.
 */
public class GiantScene extends Scene {
	private static final int APPEAR = 30;
	private static final double DISTANCE = 22.0;
	private static final int RUN_TICKS = 90;
	private static final int STOMP = APPEAR + RUN_TICKS;
	private static final int STEP_EVERY = 9;
	private static final double OVERSHOOT = 8.0;

	private Giant giant;
	private Vec3 start;
	private Vec3 direction;
	private int tint;

	@Override
	public int length() {
		return STOMP + 12;
	}

	@Override
	public int photoTick() {
		return STOMP - 25;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.giant", player.getDisplayName());
	}

	@Override
	public int screenTint() {
		return tint;
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick < APPEAR && tick % 12 == 0) {
			// Far away, heavy steps.
			Vec3 far = groundSpot(level, player, 40.0, 0.0);
			sound(level, far, SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 4.0F, 0.4F);
			shake(0.04F);
		}

		if (tick == APPEAR) {
			start = groundSpot(level, player, DISTANCE, (random.nextDouble() - 0.5) * 16.0);
			Vec3 away = start.subtract(player.position());
			direction = new Vec3(-away.x, 0.0, -away.z).normalize();
			giant = spawn(level, EntityTypes.GIANT, start);
			giant.noPhysics = true;
			sound(level, start, SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 6.0F, 0.3F);
		}

		if (giant == null) {
			return;
		}

		// Running at the player and right on over them.
		Vec3 at;
		if (tick <= STOMP) {
			float t = (tick - APPEAR) / (float) RUN_TICKS;
			at = start.lerp(new Vec3(player.getX(), start.y, player.getZ()), t);
		} else {
			at = player.position().add(direction.scale(Math.min(OVERSHOOT, (tick - STOMP) * 0.4)));
		}
		Vec3 ground = new Vec3(at.x, floor(level, at.x, at.z, player.getY()), at.z);
		walk(giant, ground);
		face(giant, ground.add(direction.scale(5.0)).add(0.0, 10.0, 0.0));

		double distance = ground.distanceTo(player.position());
		float closeness = (float) Math.max(0.0, 1.0 - distance / DISTANCE);

		if (tick < STOMP && (tick - APPEAR) % STEP_EVERY == 0) {
			// Every step is louder and shakes the camera harder.
			sound(level, ground, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.HOSTILE, 2.0F + closeness * 4.0F, 0.5F);
			sound(level, ground, SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 3.0F + closeness * 3.0F, 0.4F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(player.blockPosition().below())), ground, 20, 2.0, 0.2);
			shake(0.08F + closeness * 0.45F);
		} else if (tick < STOMP) {
			shake(0.02F + closeness * 0.12F);
		}

		if (tick % 40 == 20 && tick < STOMP) {
			sound(level, ground, SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 5.0F, 0.3F);
		}

		// No damage from afar: the only hit is the foot coming down.

		if (tick < STOMP) {
			// Looking up at the giant, higher and higher the closer it gets.
			turnTowards(player, giant.position().add(0.0, 7.0 + closeness * 3.0, 0.0), 0.12F + closeness * 0.25F);
		}

		if (tick == STOMP) {
			// The foot comes down: crushed flat to the ground, everything goes dark.
			sound(level, player.position(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.HOSTILE, 6.0F, 0.4F);
			sound(level, player.position(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 3.0F, 0.5F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(player.blockPosition().below())), player.position(), 60, 1.5, 0.4);
			particles(level, ParticleTypes.POOF, player.position(), 30, 1.5, 0.15);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}

		if (tick >= STOMP) {
			moveCamera(new Vec3(0.0, -1.35, 0.0));
			tint = Math.min(230, (tick - STOMP + 1) * 60) << 24;
		}
	}

	/** Shakes the camera: a small random jolt of its position. */
	private void shake(float strength) {
		moveCamera(new Vec3((random.nextDouble() - 0.5) * strength, (random.nextDouble() - 0.5) * strength, (random.nextDouble() - 0.5) * strength));
	}
}
