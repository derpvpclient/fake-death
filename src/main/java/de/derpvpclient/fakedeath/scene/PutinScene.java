package de.derpvpclient.fakedeath.scene;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.phys.Vec3;

/**
 * Vladimir Putin rides up shirtless on a bear, waving like at a parade. "Privet!" He gets off, cracks his knuckles:
 * judo, black belt. He grabs the player, throws them over his shoulder, and they land flat on their back. "So. Who is
 * lying on the mat now?"
 */
public class PutinScene extends CastScene {
	private static final int ARRIVE = 55;
	private static final int PRIVET = ARRIVE + 5;
	private static final int OFF = PRIVET + 35;
	private static final int JUDO = OFF + 15;
	private static final int GRAB = JUDO + 50;
	private static final int THROW = 18;
	private static final int LAND = GRAB + THROW;
	private static final int MAT = LAND + 15;
	private static final int DEATH = MAT + 30;
	private static final ChatFormatting RU = ChatFormatting.AQUA;

	private RemotePlayer putin;
	private PolarBear bear;
	private Vec3 start;
	private Vec3 stop;
	private boolean down;

	@Override
	public int length() {
		return DEATH + 1;
	}

	@Override
	public int photoTick() {
		return ARRIVE - 15;
	}

	@Override
	public float lying() {
		// After the throw the body lies flat on its back in F5.
		return down ? 1.0F : 0.0F;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.putin", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			Vec3 look = horizontalLook(player);
			start = onGround(level, player, player.position().add(look.scale(15.0)).add(look.yRot(Mth.HALF_PI).scale(3.0)));
			stop = onGround(level, player, player.position().add(look.scale(3.2)));
			bear = spawn(level, EntityType.POLAR_BEAR, start);
			bear.noPhysics = true;
			putin = actor(level, "fakedeath.putin.name", "putin_bare", false, start.add(0.0, 1.2, 0.0));
			pose(putin, Skins.Pose.RAISE);
			sound(level, start, SoundEvents.POLAR_BEAR_AMBIENT, SoundSource.NEUTRAL, 2.0F, 0.8F);
		}
		if (putin == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();

		if (tick <= ARRIVE) {
			// Riding up at a stately pace, waving.
			float p = tick / (float) ARRIVE;
			Vec3 at = onGround(level, player, start.lerp(stop, p));
			walk(bear, at);
			face(bear, eyes);
			putin.setPos(at.x, at.y + 1.15, at.z);
			face(putin, eyes);
			if (tick % 10 == 0) {
				sound(level, at, SoundEvents.POLAR_BEAR_STEP, SoundSource.NEUTRAL, 1.2F, 0.8F);
			}
			if (tick % 16 == 0) {
				pose(putin, tick % 32 == 0 ? Skins.Pose.RAISE : null);
			}
		}
		if (tick == PRIVET) {
			pose(putin, null);
			speak(level, putin, "fakedeath.putin.privet", RU, 30);
			sound(level, bear.position(), SoundEvents.POLAR_BEAR_WARNING, SoundSource.NEUTRAL, 1.5F, 1.0F);
		}
		if (tick > ARRIVE && tick < OFF) {
			putin.setPos(stop.x, stop.y + 1.15, stop.z);
			face(putin, eyes);
		}
		if (tick == OFF) {
			// Down off the bear, right in front of the player.
			Vec3 front = onGround(level, player, player.position().add(horizontalLook(player).scale(1.3)));
			walk(putin, front);
			sound(level, front, SoundEvents.PLAYER_SMALL_FALL, SoundSource.NEUTRAL, 1.0F, 1.0F);
		}
		if (tick >= OFF) {
			face(putin, eyes);
			face(bear, putin.getEyePosition());
		}
		if (tick == JUDO) {
			speak(level, putin, "fakedeath.putin.judo", RU, 45);
		}
		if (tick > JUDO && tick < GRAB && (tick - JUDO) % 12 == 0) {
			// Cracking the knuckles, warming up.
			Looks.swing(putin);
			sound(level, putin.position(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.NEUTRAL, 1.0F, 1.6F);
		}
		if (tick < GRAB) {
			turnTowards(player, putin.getEyePosition(), 0.12F);
		}
		if (tick == GRAB) {
			pose(putin, Skins.Pose.CHEER);
			sound(level, eyes, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.5F, 0.8F);
		}
		if (tick >= GRAB && tick <= LAND) {
			// Up over his shoulder in an arc and down flat on the back, the view turning over with it.
			float p = (tick - GRAB) / (float) THROW;
			Vec3 forward = horizontalLook(player);
			double over = Math.sin(p * Math.PI) * 1.6;
			Vec3 offset = forward.scale(2.4 * p).add(0.0, over - 1.35 * p * p, 0.0);
			moveCamera(offset);
			turnTowards(player, eyes.add(offset).add(forward.scale(p < 0.5 ? 1.0 : -1.0)).add(0.0, p < 0.5 ? 2.0 : 4.0, 0.0), 0.5F);
		}
		if (tick == LAND) {
			down = true;
			pose(putin, null);
			sound(level, player.position(), SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 2.0F, 0.8F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(player.blockPosition().below())), player.position().add(horizontalLook(player).scale(2.4)), 30, 0.6, 0.2);
			cameraShake(0.3F);
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.6F);
		}
		if (tick > LAND) {
			moveCamera(horizontalLook(player).scale(2.4).add(0.0, -1.35, 0.0));
			// Lying there, looking up at him standing over the player.
			turnTowards(player, putin.getEyePosition(), 0.15F);
		}
		if (tick == MAT) {
			speak(level, putin, "fakedeath.putin.mat", RU, 40);
		}
		if (tick == DEATH) {
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		putin = null;
		bear = null;
		super.end(level, player);
	}
}
