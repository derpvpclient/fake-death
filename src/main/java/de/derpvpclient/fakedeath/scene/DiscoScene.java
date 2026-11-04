package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

/**
 * Out of nowhere a party: music starts, colored lights flash, parrots dance and a creeper dances right in
 * front of the player. When the beat drops, so does the creeper. Flashes fast, so it only plays with flashing
 * effects switched on in the settings.
 */
public class DiscoScene extends Scene {
	private static final int[] LIGHTS = {0xFF2060, 0x20E0FF, 0xFFE020, 0x40FF40, 0xC040FF};
	private static final int BEAT = 8;
	private static final int CREEPER = 20;
	private static final int PRIME = 150;
	private static final int EXPLODE = PRIME + 30;

	private final List<Parrot> parrots = new ArrayList<>();
	private Creeper creeper;
	private Vec3 floor;
	private int tint;

	@Override
	public int length() {
		return EXPLODE + 10;
	}

	@Override
	public int photoTick() {
		return PRIME - 20;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.explosion.player", player.getDisplayName(), EntityType.CREEPER.getDescription());
	}

	@Override
	public int screenTint() {
		return tint;
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			sound(level, player.position(), SoundEvents.MUSIC_DISC_PIGSTEP.value(), SoundSource.RECORDS, 2.0F, 1.0F);
			floor = groundSpot(level, player, 3.0, 0.0);
		}

		if (tick < EXPLODE) {
			// The lights change with every beat, a flash on the beat itself.
			int light = LIGHTS[(tick / BEAT) % LIGHTS.length];
			tint = (tick % BEAT < 2 ? 110 : 45) << 24 | light;

			if (tick % 2 == 0) {
				Vec3 spot = player.getEyePosition().add((random.nextDouble() - 0.5) * 8.0, random.nextDouble() * 3.0, (random.nextDouble() - 0.5) * 8.0);
				particles(level, new DustParticleOptions(net.minecraft.world.phys.Vec3.fromRGB24(LIGHTS[random.nextInt(LIGHTS.length)]).toVector3f(), 2.0F), spot, 4, 0.3, 0.0);
				level.addParticle(ParticleTypes.NOTE, spot.x, spot.y, spot.z, random.nextDouble(), 0.0, 0.0);
			}
		}

		if (tick == CREEPER) {
			creeper = spawn(level, EntityType.CREEPER, floor);
			creeper.noPhysics = true;

			Vec3 side = horizontalLook(player).yRot(Mth.HALF_PI);
			for (int s : new int[] {-1, 1}) {
				Parrot parrot = spawn(level, EntityType.PARROT, groundSpot(level, player, 3.0, 0.0).add(side.scale(s * 1.6)));
				parrot.noPhysics = true;
				parrots.add(parrot);
			}
		}

		for (Parrot parrot : parrots) {
			parrot.setRecordPlayingNearby(parrot.blockPosition(), true);
			face(parrot, player.getEyePosition());
		}

		if (creeper != null) {
			dance(level, player, tick);
		}

		if (creeper != null && tick == EXPLODE) {
			Vec3 blast = creeper.position().add(0.0, 0.8, 0.0);
			despawn(level, creeper);
			creeper = null;
			minecraft.getSoundManager().stop(SoundEvents.MUSIC_DISC_PIGSTEP.value().getLocation(), SoundSource.RECORDS);
			level.addParticle(ParticleTypes.EXPLOSION_EMITTER, blast.x, blast.y, blast.z, 1.0, 0.0, 0.0);
			particles(level, ParticleTypes.EXPLOSION, blast, 8, 1.5, 0.0);
			sound(level, blast, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F, 0.75F);
			tint = 230 << 24 | 0xFFFFFF;
			kill(level, player, SoundEvents.PLAYER_HURT);
		}

		if (tick > EXPLODE) {
			tint = Math.max(0, 230 - (tick - EXPLODE) * 25) << 24 | 0xFFFFFF;
		}
	}

	/** Hops on every beat and turns around, then stops, looks at the player and hisses. */
	private void dance(ClientLevel level, LocalPlayer player, int tick) {
		Vec3 eyes = player.getEyePosition();

		if (tick < PRIME) {
			double hop = Math.abs(Mth.sin(Mth.PI * (tick % BEAT) / BEAT)) * 0.4;
			creeper.setPos(floor.x, floor.y + hop, floor.z);
			float spin = tick * 18.0F;
			creeper.setYRot(spin);
			creeper.setYBodyRot(spin);
			creeper.setYHeadRot(spin);
			turnTowards(player, creeper.getEyePosition(), 0.15F);
		} else {
			// The music stops for a moment, and the creeper looks the player in the eyes.
			creeper.setPos(floor.x, floor.y, floor.z);
			face(creeper, eyes);
			turnTowards(player, creeper.getEyePosition(), 0.3F);
		}

		if (tick == PRIME) {
			creeper.setSwellDir(1);
			sound(level, creeper.position(), SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1.5F, 0.5F);
		}
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		Minecraft.getInstance().getSoundManager().stop(SoundEvents.MUSIC_DISC_PIGSTEP.value().getLocation(), SoundSource.RECORDS);
		for (Parrot parrot : parrots) {
			parrot.setRecordPlayingNearby(parrot.blockPosition(), false);
		}
		super.end(level, player);
	}
}
