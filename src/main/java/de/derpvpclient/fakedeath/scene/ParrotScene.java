package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

/**
 * The parrot prank: a creeper hisses right behind the player, they spin around, and it is only a
 * parrot dancing. Twice. The third time they do not even turn, and that one was a real creeper.
 */
public class ParrotScene extends Scene {
	private static final int[] FAKE_HISSES = {5, 95};
	private static final int LOOK_BACK = 22;
	private static final int DANCE = 45;
	private static final int REAL_HISS = 175;
	private static final int NOTICE = 192;
	private static final int EXPLODE = REAL_HISS + 30;
	private static final int DEATH = EXPLODE + 10;

	private Parrot parrot;
	private Creeper creeper;
	private Vec3 front;
	private int danceUntil = -1;

	@Override
	public int length() {
		return DEATH;
	}

	@Override
	public int photoTick() {
		return NOTICE + 8;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.explosion.player", player.getDisplayName(), EntityType.CREEPER.getDescription());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			front = player.getEyePosition().add(horizontalLook(player).scale(5.0));
			parrot = spawn(level, EntityType.PARROT, groundSpot(level, player, 2.2, 180.0));
			parrot.noPhysics = true;
		}

		if (parrot == null) {
			return;
		}

		face(parrot, player.getEyePosition());

		for (int hiss : FAKE_HISSES) {
			if (tick == hiss) {
				sound(level, parrot.position(), SoundEvents.PARROT_IMITATE_CREEPER, SoundSource.NEUTRAL, 1.2F, 1.0F);
			}

			if (tick > hiss + 4 && tick < hiss + LOOK_BACK + 30) {
				// Spinning around in a panic, and back once it is clear it was only the parrot.
				Vec3 target = tick < hiss + LOOK_BACK + 15 ? parrot.getEyePosition() : front;
				turnTowards(player, target, tick < hiss + LOOK_BACK ? 0.35F : 0.08F);
			}

			if (tick == hiss + DANCE - 20) {
				sound(level, parrot.position(), SoundEvents.PARROT_AMBIENT, SoundSource.NEUTRAL, 1.2F, 1.4F);
				danceUntil = tick + 50;
			}
		}

		// Parrots only dance next to a jukebox, so the scene keeps telling it one is playing.
		boolean dancing = tick < danceUntil;
		parrot.setRecordPlayingNearby(parrot.blockPosition(), dancing);

		if (tick == REAL_HISS - 10) {
			Vec3 spot = groundSpot(level, player, 1.6, 100.0 + random.nextDouble() * 40.0 * (random.nextBoolean() ? 1 : -1));
			creeper = spawn(level, EntityType.CREEPER, spot);
		}

		if (creeper != null) {
			face(creeper, player.getEyePosition());

			if (tick == REAL_HISS) {
				creeper.setSwellDir(1);
				sound(level, creeper.position(), SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1.0F, 0.5F);
			}

			if (tick > REAL_HISS && tick < NOTICE) {
				// Not again, that parrot. A lazy glance at it.
				turnTowards(player, parrot.getEyePosition(), 0.03F);
			}

			if (tick >= NOTICE && tick < EXPLODE) {
				turnTowards(player, creeper.getEyePosition(), 0.4F);
			}

			if (tick == EXPLODE) {
				explode(level, player);
			}
		}

		if (tick == DEATH + 8) {
			sound(level, parrot.position(), SoundEvents.PARROT_IMITATE_CREEPER, SoundSource.NEUTRAL, 1.0F, 1.2F);
			danceUntil = tick + 80;
		}
	}

	private void explode(ClientLevel level, LocalPlayer player) {
		Vec3 blast = creeper.position().add(0.0, 0.8, 0.0);
		despawn(level, creeper);
		creeper = null;

		level.addParticle(ParticleTypes.EXPLOSION_EMITTER, blast.x, blast.y, blast.z, 1.0, 0.0, 0.0);
		particles(level, ParticleTypes.EXPLOSION, blast, 8, 1.5, 0.0);
		particles(level, ParticleTypes.LARGE_SMOKE, blast, 30, 1.2, 0.15);
		sound(level, blast, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F, 0.75F);
		player.animateHurt(random.nextFloat() * 360.0F);
		kill(level, player, SoundEvents.PLAYER_HURT);
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		if (parrot != null) {
			parrot.setRecordPlayingNearby(BlockPos.ZERO, false);
		}
		super.end(level, player);
	}
}
