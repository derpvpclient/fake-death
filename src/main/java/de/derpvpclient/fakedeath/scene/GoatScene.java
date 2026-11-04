package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.phys.Vec3;

/**
 * In the mountains: a screaming goat stares at the player, lowers its head, charges and rams.
 * It backs off, screams again and comes back for the second hit.
 */
public class GoatScene extends Scene {
	private static final byte LOWER_HEAD = 58;
	private static final byte RAISE_HEAD = 59;
	private static final int APPEAR = 10;
	private static final double DISTANCE = 12.0;
	private static final int[] SCREAMS = {16, 36, 54};
	private static final int LOWER_1 = 66;
	private static final int CHARGE_1 = 100;
	private static final int HIT_1 = 120;
	private static final int BACK = 130;
	private static final int SCREAM_2 = 146;
	private static final int LOWER_2 = 156;
	private static final int CHARGE_2 = 174;
	private static final int HIT_2 = 184;
	private static final int DEATH = HIT_2 + 8;

	private Goat goat;
	private Vec3 direction;
	private Vec3 pos;
	private Vec3 moveFrom;
	private Vec3 moveTo;
	private int moveStart = -1;
	private int moveTicks;
	private boolean charging;

	@Override
	public int length() {
		return DEATH;
	}

	@Override
	public int photoTick() {
		return HIT_1 - 4;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.mob", player.getDisplayName(), EntityTypes.GOAT.getDescription());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			sound(level, groundSpot(level, player, DISTANCE, 0.0), SoundEvents.GOAT_SCREAMING_AMBIENT, SoundSource.NEUTRAL, 2.0F, 1.0F);
		}

		if (tick == APPEAR) {
			Vec3 spot = groundSpot(level, player, DISTANCE, (random.nextDouble() - 0.5) * 80.0);
			Vec3 away = spot.subtract(player.position());
			direction = new Vec3(away.x, 0.0, away.z).normalize();
			pos = new Vec3(spot.x, floor(level, spot.x, spot.z, player.getY()), spot.z);
			goat = spawn(level, EntityTypes.GOAT, pos);
			goat.setScreamingGoat(true);
			goat.noPhysics = true;
		}

		if (goat == null) {
			return;
		}

		for (int scream : SCREAMS) {
			if (tick == scream) {
				sound(level, pos, SoundEvents.GOAT_SCREAMING_AMBIENT, SoundSource.NEUTRAL, 2.0F, 0.9F + random.nextFloat() * 0.2F);
			}
		}

		if (tick == LOWER_1 || tick == LOWER_2) {
			goat.handleEntityEvent(LOWER_HEAD);
			sound(level, pos, SoundEvents.GOAT_SCREAMING_PREPARE_RAM, SoundSource.NEUTRAL, 2.0F, 1.0F);
		}

		if (tick > LOWER_1 && tick < CHARGE_1 && tick % 9 == 0) {
			// Scraping the ground with its hooves.
			sound(level, pos, SoundEvents.GOAT_STEP, SoundSource.NEUTRAL, 1.0F, 0.8F);
			particles(level, ParticleTypes.POOF, pos, 3, 0.3, 0.05);
		}

		if (tick == CHARGE_1 || tick == CHARGE_2) {
			move(player.position().add(direction.scale(0.9)), tick, tick == CHARGE_1 ? HIT_1 - CHARGE_1 : HIT_2 - CHARGE_2, true);
		}

		if (tick == HIT_1 || tick == HIT_2) {
			ram(level, player, tick == HIT_2);
		}

		if (tick == BACK) {
			move(player.position().add(direction.scale(4.5)), tick, 14, false);
		}

		if (tick == SCREAM_2) {
			sound(level, pos, SoundEvents.GOAT_SCREAMING_AMBIENT, SoundSource.NEUTRAL, 2.5F, 1.1F);
		}

		if (moveStart >= 0) {
			float t = Math.min(1.0F, (tick - moveStart) / (float) moveTicks);
			// A charge gets faster and faster, backing off is calm.
			float eased = charging ? t * t : t * t * (3.0F - 2.0F * t);
			Vec3 at = moveFrom.lerp(moveTo, eased);
			pos = new Vec3(at.x, floor(level, at.x, at.z, player.getY()), at.z);

			if (charging && tick % 3 == 0) {
				sound(level, pos, SoundEvents.GOAT_STEP, SoundSource.NEUTRAL, 1.5F, 1.0F);
				particles(level, ParticleTypes.POOF, pos, 2, 0.2, 0.02);
			}

			if (t >= 1.0F) {
				moveStart = -1;
			}
		}

		walk(goat, pos);
		face(goat, player.getEyePosition());

		if (tick > APPEAR && tick != HIT_1 && tick != HIT_2) {
			turnTowards(player, goat.getEyePosition(), charging && moveStart >= 0 ? 0.3F : 0.12F);
		}
	}

	private void move(Vec3 to, int tick, int ticks, boolean charge) {
		moveFrom = pos;
		moveTo = to;
		moveStart = tick;
		moveTicks = ticks;
		charging = charge;
	}

	private void ram(ClientLevel level, LocalPlayer player, boolean last) {
		sound(level, player.position(), SoundEvents.GOAT_SCREAMING_RAM_IMPACT, SoundSource.NEUTRAL, 2.0F, 1.0F);
		particles(level, ParticleTypes.CRIT, player.getEyePosition().add(0.0, -0.4, 0.0), 14, 0.4, 0.3);
		hit(level, player, SoundEvents.PLAYER_HURT, 0.6F, last);
		goat.handleEntityEvent(RAISE_HEAD);
		charging = false;
		// The hit throws the view up into the sky.
		turnTowards(player, player.getEyePosition().add(horizontalLook(player).scale(-0.5)).add(0.0, 3.0, 0.0), 0.6F);
	}
}
