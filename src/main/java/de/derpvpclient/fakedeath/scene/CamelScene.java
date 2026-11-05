package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** A camel strolls over in the desert, stops right in front of the player, turns its back and kicks out. Twice. */
public class CamelScene extends Scene {
	private static final int ARRIVE = 60;
	private static final int TURN = 80;
	private static final int[] KICKS = {110, 150};
	private static final int KICK = 6;
	private Camel camel;
	private Vec3 start;
	private Vec3 stand;

	@Override
	public int length() {
		return KICKS[KICKS.length - 1] + 30;
	}

	@Override
	public int photoTick() {
		return KICKS[0] + 2;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.kamel", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			start = groundSpot(level, player, 12.0, 40.0);
			Vec3 flat = player.position().subtract(start);
			flat = new Vec3(flat.x, 0.0, flat.z).normalize();
			stand = player.position().subtract(flat.scale(2.4));
			camel = spawn(level, EntityType.CAMEL, start);
			camel.noPhysics = true;
		}
		if (camel == null) {
			return;
		}

		Vec3 away = stand.subtract(player.position()).normalize();
		Vec3 at = tick < ARRIVE ? start.lerp(stand, tick / (double) ARRIVE) : stand;
		double kickBack = 0.0;
		for (int kick : KICKS) {
			int t = tick - kick;
			if (t >= -10 && t < 0) {
				// Leaning forward, getting ready.
				kickBack = -0.3 * (t + 10) / 10.0;
			} else if (t >= 0 && t < KICK) {
				kickBack = -0.3 + Mth.sin(t / (float) KICK * Mth.PI) * -0.8;
			}
			if (t == 0) {
				camel.setDashing(true);
			}
			if (t == KICK + 2) {
				camel.setDashing(false);
			}
			if (t == -10) {
				sound(level, at, SoundEvents.CAMEL_AMBIENT, SoundSource.NEUTRAL, 1.2F, 0.8F);
			}
			if (t == KICK / 2) {
				sound(level, player.position(), SoundEvents.CAMEL_DASH, SoundSource.NEUTRAL, 1.5F, 0.8F);
				sound(level, player.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.5F, 0.7F);
				particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), player.position(), 20, 0.5, 0.2);
				hit(level, player, SoundEvents.PLAYER_HURT, 0.45F, kick == KICKS[KICKS.length - 1]);
				moveCamera(away.scale(-1.8).add(0.0, 0.5, 0.0));
				cameraShake(0.5F);
				particles(level, ParticleTypes.CLOUD, player.position(), 15, 0.6, 0.15);
			}
			if (t == KICK + 6) {
				moveCamera(Vec3.ZERO);
			}
		}
		walk(camel, new Vec3(at.x, floor(level, at.x, at.z, player.getY()), at.z).subtract(away.scale(kickBack)));
		// Walking towards the player, then the back turned to them.
		face(camel, tick < TURN ? player.getEyePosition() : camel.getEyePosition().add(away.scale(3.0)));
		turnTowards(player, camel.getEyePosition(), 0.12F);
		if (tick < ARRIVE && tick % 10 == 0) {
			sound(level, at, SoundEvents.CAMEL_AMBIENT, SoundSource.NEUTRAL, 0.6F, 1.0F);
		}
	}
}
