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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.phys.Vec3;

/**
 * It gets cold, souls rise from the ground and something whispers. One ghost after another appears and
 * circles the player, closer and closer, until they fly right through them.
 */
public class GhostScene extends Scene {
	private static final int[] APPEAR = {40, 70, 100, 130};
	private static final int[] CHARGE = {180, 200, 220, 238};
	private static final int CHARGE_TICKS = 10;
	private static final int RETURN_TICKS = 6;
	private static final int DARK = 150;
	private static final int DEATH = 252;

	private static final int FLASH = 12;
	private static final int ICE = 0xC8E8FF;

	private final List<Vex> ghosts = new ArrayList<>();
	private final Vec3[] chargeFrom = new Vec3[APPEAR.length];
	private int flashTick = -100;
	private int now;

	@Override
	public int length() {
		return DEATH;
	}

	@Override
	public int screenTint() {
		int since = now - flashTick;
		return since < 0 || since >= FLASH ? 0 : (140 - since * 140 / FLASH) << 24 | ICE;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.ghosts", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		Vec3 head = player.getEyePosition();
		now = tick;

		if (tick == 0) {
			sound(level, head, SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(), SoundSource.AMBIENT, 1.5F, 0.8F);
		}

		// The cold creeps in: frost at the screen edge, a little more over time.
		if (tick < DEATH && tick % 10 == 0) {
			chill(player, Math.min(0.9F, tick / 200.0F));
		}

		if (tick % 3 == 0) {
			Vec3 ground = player.position().add((random.nextDouble() - 0.5) * 8, 0.1, (random.nextDouble() - 0.5) * 8);
			level.addParticle(ParticleTypes.SOUL, ground.x, ground.y, ground.z, 0.0, 0.03 + random.nextDouble() * 0.03, 0.0);
		}

		if (tick % 30 == 15) {
			Vec3 whisper = head.add((random.nextDouble() - 0.5) * 6, (random.nextDouble() - 0.5) * 2, (random.nextDouble() - 0.5) * 6);
			sound(level, whisper, SoundEvents.SOUL_ESCAPE.value(), SoundSource.AMBIENT, 1.0F + tick / 120.0F, 0.6F + random.nextFloat() * 0.4F);
		}

		for (int i = 0; i < APPEAR.length; i++) {
			if (tick == APPEAR[i]) {
				Vec3 spot = orbit(head, i, tick);
				ghosts.add(spawn(level, EntityTypes.VEX, spot));
				particles(level, ParticleTypes.SOUL, spot, 12, 0.4, 0.05);
				sound(level, spot, SoundEvents.VEX_AMBIENT, SoundSource.HOSTILE, 1.2F, 0.7F);
			}
		}

		if (tick == DARK) {
			addEffect(player, MobEffects.DARKNESS, 300);
			sound(level, head, SoundEvents.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS.value(), SoundSource.AMBIENT, 2.0F, 0.6F);
		}

		for (int i = 0; i < ghosts.size(); i++) {
			moveGhost(level, player, head, i, tick);
		}

		// The camera follows each ghost as it appears.
		if (!ghosts.isEmpty() && tick < CHARGE[0]) {
			turnTowards(player, ghosts.getLast().position(), 0.08F);
		}
	}

	private void moveGhost(ClientLevel level, LocalPlayer player, Vec3 head, int i, int tick) {
		Vex ghost = ghosts.get(i);
		Vec3 circling = orbit(head, i, tick);
		int charge = CHARGE[i];
		Vec3 pos = circling;

		if (tick >= charge && tick < charge + CHARGE_TICKS) {
			// Straight through the player to the other side.
			if (tick == charge) {
				chargeFrom[i] = ghost.position();
				ghost.setIsCharging(true);
				sound(level, ghost.position(), SoundEvents.VEX_CHARGE, SoundSource.HOSTILE, 1.5F, 0.8F);
			}

			Vec3 through = head.add(head.subtract(chargeFrom[i]));
			pos = chargeFrom[i].lerp(through, (tick - charge) / (double) CHARGE_TICKS);

			// Eyes on the ghost that comes for the player.
			turnTowards(player, pos, 0.35F);

			if (tick == charge + CHARGE_TICKS / 2) {
				// Right through the body: an icy flash, the frost jumps up and the ghost screams.
				particles(level, ParticleTypes.SOUL, head, 25, 0.5, 0.15);
				particles(level, ParticleTypes.SNOWFLAKE, head, 15, 0.5, 0.1);
				sound(level, head, SoundEvents.VEX_HURT, SoundSource.HOSTILE, 1.5F, 0.6F);
				chill(player, 1.0F);
				flashTick = tick;
				hit(level, player, SoundEvents.PLAYER_HURT_FREEZE, 0.3F, i == CHARGE.length - 1);
			}
		} else if (tick >= charge + CHARGE_TICKS && tick < charge + CHARGE_TICKS + RETURN_TICKS) {
			if (tick == charge + CHARGE_TICKS) {
				ghost.setIsCharging(false);
			}

			Vec3 through = head.add(head.subtract(chargeFrom[i]));
			pos = through.lerp(circling, (tick - charge - CHARGE_TICKS) / (double) RETURN_TICKS);
		}

		ghost.setPos(pos.x, pos.y, pos.z);
		face(ghost, head);

		if (tick % 4 == i % 4) {
			level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 0.3, pos.z, 0.0, 0.01, 0.0);
		}
	}

	/** Each ghost circles at its own speed and height, closer to the player the longer it goes. */
	private static Vec3 orbit(Vec3 head, int index, int tick) {
		float closer = Mth.clamp((tick - APPEAR[0]) / 160.0F, 0.0F, 1.0F);
		double radius = Mth.lerp(closer, 5.0F, 2.2F);
		float angle = index * Mth.TWO_PI / APPEAR.length + tick * (index % 2 == 0 ? 0.07F : -0.06F);
		return head.add(Mth.cos(angle) * radius, 0.3 + Mth.sin(tick * 0.12F + index) * 0.6, Mth.sin(angle) * radius);
	}
}
