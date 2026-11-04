package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

/** A creeper sneaks up from behind, hisses, the camera turns around, too late. */
public class CreeperScene extends Scene {
	private static final int SNEAK = 45;
	// A creeper needs 30 ticks from the hiss to the blast.
	private static final int EXPLODE = SNEAK + 30;

	private final boolean invisible;
	private Creeper creeper;
	private Vec3 from;
	private Vec3 to;

	public CreeperScene() {
		this(false);
	}

	/** The invisible one: it hisses, the player turns around, and there is nothing. */
	public CreeperScene(boolean invisible) {
		this.invisible = invisible;
	}

	@Override
	public int length() {
		return EXPLODE + 10;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.explosion.player", player.getDisplayName(), EntityTypes.CREEPER.getDescription());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			// Out of sight behind the player, then it walks up quietly.
			from = groundSpot(level, player, 7.0, 180.0);
			to = groundSpot(level, player, 1.6, 180.0);
			creeper = spawn(level, EntityTypes.CREEPER, from);
			creeper.setInvisible(invisible);
		}

		if (creeper == null) {
			return;
		}

		if (tick <= SNEAK) {
			Vec3 step = from.lerp(to, tick / (double) SNEAK);
			walk(creeper, step);
		}

		if (tick == SNEAK) {
			// Swells on the client by itself, the explosion only ever happens on a server.
			creeper.setSwellDir(1);
			sound(level, creeper.position(), SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1.0F, 0.5F);
		}

		if (tick < EXPLODE) {
			face(creeper, player.getEyePosition());

			if (tick >= SNEAK + 4) {
				turnTowards(player, creeper.getEyePosition(), 0.22F);
			}
		}

		if (tick == EXPLODE) {
			explode(level, player);
		}
	}

	private void explode(ClientLevel level, LocalPlayer player) {
		Vec3 blast = creeper.position().add(0, 0.8, 0);
		despawn(level, creeper);
		creeper = null;

		level.addParticle(ParticleTypes.EXPLOSION_EMITTER, blast.x, blast.y, blast.z, 1.0, 0.0, 0.0);
		particles(level, ParticleTypes.EXPLOSION, blast, 8, 1.5, 0.0);
		particles(level, ParticleTypes.LARGE_SMOKE, blast, 30, 1.2, 0.15);
		sound(level, blast, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F, 0.7F + random.nextFloat() * 0.1F);

		player.animateHurt(random.nextFloat() * 360.0F);
		kill(level, player, SoundEvents.PLAYER_HURT);
	}
}
