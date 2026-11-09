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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.phys.Vec3;

/**
 * The player smacks a chicken. It gets back up, turns round and stares. Then it calls, and chickens come running and
 * flapping from everywhere. They peck at the face one after the other, feathers everywhere. Then six of them grab the
 * player, flap up into the sky with them, and let go.
 */
public class ChickenScene extends Scene {
	private static final int SMACK = 25;
	private static final int GLARE = 36;
	private static final int CALL = 52;
	private static final int ARMY = 40;
	private static final int RUN = 66;
	private static final int PECK = CALL + RUN;
	private static final int PECK_EVERY = 8;
	private static final int LIFT = PECK + 44;
	private static final int LIFTED = LIFT + 44;
	private static final int DROP = LIFTED + 6;
	private static final int LAND = DROP + 14;
	private static final int CARRIERS = 6;
	private static final double HEIGHT = 9.0;

	private final List<Chicken> army = new ArrayList<>();
	private final List<Vec3> from = new ArrayList<>();
	private final List<Vec3> to = new ArrayList<>();
	private Chicken victim;
	private Vec3 victimSpot;
	private Vec3 look;
	private int pecker = -1;
	private int peckedAt;

	@Override
	public int length() {
		return LAND + 20;
	}

	@Override
	public int photoTick() {
		return LIFT + 30;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.huehner", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		Vec3 eyes = player.getEyePosition();
		if (tick == 0) {
			victimSpot = groundSpot(level, player, 1.8, 0.0);
			victim = spawn(level, EntityType.CHICKEN, victimSpot);
			look = horizontalLook(player);
		}
		if (victim == null) {
			return;
		}

		victim(level, player, tick);
		if (tick == CALL) {
			call(level, player);
		}
		flock(level, player, tick);
		pecking(level, player, eyes, tick);
		camera(player, eyes, tick);

		if (tick > CALL && tick < LIFT && tick % 4 == 0) {
			sound(level, player.position().add(look.yRot(random.nextFloat() * 6.0F).scale(4.0)), Looks.CHICKEN, SoundSource.NEUTRAL, 1.0F, 0.8F + random.nextFloat() * 0.5F);
		}
		if (tick >= LIFT && tick < DROP && tick % 3 == 0) {
			sound(level, eyes.add(0.0, liftHeight(tick), 0.0), SoundEvents.PARROT_FLY, SoundSource.NEUTRAL, 1.5F, 1.2F + random.nextFloat() * 0.3F);
		}
		if (tick == DROP) {
			sound(level, eyes.add(0.0, HEIGHT, 0.0), Looks.CHICKEN, SoundSource.NEUTRAL, 2.0F, 1.4F);
		}
		if (tick == LAND) {
			sound(level, player.position(), SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 1.5F, 1.0F);
			particles(level, Looks.feathers(), player.position().add(0.0, 0.3, 0.0), 30, 0.8, 0.2);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	/** Smacked, knocked back with a puff of feathers, back on its feet, turned round, staring. */
	private void victim(ClientLevel level, LocalPlayer player, int tick) {
		if (tick == SMACK) {
			Looks.swing(player);
			victim.animateHurt(0.0F);
			sound(level, victim.position(), Looks.CHICKEN_HURT, SoundSource.NEUTRAL, 1.2F, 1.0F);
			particles(level, Looks.feathers(), victim.position().add(0.0, 0.4, 0.0), 14, 0.3, 0.15);
		}
		if (tick > SMACK && tick < GLARE) {
			// Tumbles back, away from the player.
			float p = (tick - SMACK) / (float) (GLARE - SMACK);
			Vec3 back = victimSpot.add(look.scale(0.9 * p)).add(0.0, Mth.sin(p * Mth.PI) * 0.6, 0.0);
			walk(victim, back);
			victim.setYRot(victim.getYRot() + 40.0F);
			victim.setYBodyRot(victim.getYRot());
			return;
		}
		if (tick < CALL || tick > LIFT) {
			face(victim, player.getEyePosition());
		}
		if (tick == GLARE + 6) {
			// A low, angry cluck.
			sound(level, victim.position(), Looks.CHICKEN_HURT, SoundSource.NEUTRAL, 1.5F, 0.5F);
			particles(level, ParticleTypes.ANGRY_VILLAGER, victim.position().add(0.0, 1.0, 0.0), 3, 0.2, 0.0);
		}
		if (tick > CALL - 6 && tick < CALL + 10) {
			// Calls the flock, head up, hopping.
			walk(victim, victimSpot.add(look.scale(0.9)).add(0.0, Math.abs(Mth.sin(tick * 0.8F)) * 0.3, 0.0));
		}
	}

	/** The flock answers from all sides at once. */
	private void call(ClientLevel level, LocalPlayer player) {
		sound(level, victim.position(), Looks.CHICKEN, SoundSource.NEUTRAL, 2.0F, 0.6F);
		for (int i = 0; i < ARMY; i++) {
			double angle = i * Math.PI * 2 / ARMY + random.nextDouble() * 0.2;
			Vec3 start = player.position().add(Math.cos(angle) * (12.0 + random.nextDouble() * 6.0), 0.0, Math.sin(angle) * (12.0 + random.nextDouble() * 6.0));
			Vec3 end = player.position().add(Math.cos(angle) * (0.8 + (i % 3) * 0.55), 0.0, Math.sin(angle) * (0.8 + (i % 3) * 0.55));
			start = new Vec3(start.x, floor(level, start.x, start.z, player.getY()), start.z);
			end = new Vec3(end.x, floor(level, end.x, end.z, player.getY()), end.z);
			Chicken chicken = spawn(level, EntityType.CHICKEN, start);
			chicken.noPhysics = true;
			army.add(chicken);
			from.add(start);
			to.add(end);
		}
	}

	/** Running in with little hops, then the six closest ones carry the player up and fly off in all directions. */
	private void flock(ClientLevel level, LocalPlayer player, int tick) {
		Vec3 eyes = player.getEyePosition();
		for (int i = 0; i < army.size(); i++) {
			Chicken chicken = army.get(i);
			if (i < CARRIERS && tick >= LIFT) {
				// Holding on around the player, flapping hard.
				double angle = i * Math.PI * 2 / CARRIERS + tick * 0.05;
				double height = liftHeight(tick);
				Vec3 hold = eyes.add(Math.cos(angle) * 0.7, height - 0.9 + Mth.sin(tick * 1.3F + i) * 0.08, Math.sin(angle) * 0.7);
				if (tick > DROP) {
					// Let go: off they fly, outwards and up.
					double away = (tick - DROP) * 0.35;
					hold = eyes.add(Math.cos(angle) * (0.7 + away), HEIGHT - 0.9 + away * 0.4, Math.sin(angle) * (0.7 + away));
				}
				chicken.setPos(hold.x, hold.y, hold.z);
				face(chicken, hold.add(Math.cos(angle) * 2.0, 0.0, Math.sin(angle) * 2.0));
				if (tick % 6 == i % 6) {
					particles(level, Looks.feathers(), hold, 1, 0.2, 0.05);
				}
				continue;
			}
			if (i == pecker) {
				continue;
			}
			float p = Math.min(1.0F, (tick - CALL) / (float) (RUN - i % 10));
			Vec3 at = from.get(i).lerp(to.get(i), p);
			// Little hops with flapping wings on the way in.
			double hop = p < 1.0F ? Math.abs(Mth.sin(tick * 0.7F + i)) * 0.35 : 0.0;
			walk(chicken, at.add(0.0, hop, 0.0));
			face(chicken, tick >= LIFT ? eyes.add(0.0, liftHeight(tick), 0.0) : eyes);
		}
	}

	/** One after the other, a chicken jumps up at the face and pecks. */
	private void pecking(ClientLevel level, LocalPlayer player, Vec3 eyes, int tick) {
		if (tick == LIFT) {
			pecker = -1;
		}
		if (tick < PECK || tick >= LIFT || army.isEmpty()) {
			return;
		}
		if ((tick - PECK) % PECK_EVERY == 0) {
			pecker = CARRIERS + random.nextInt(army.size() - CARRIERS);
			peckedAt = tick;
		}
		if (pecker < 0) {
			return;
		}
		Chicken chicken = army.get(pecker);
		int s = tick - peckedAt;
		double up = s < 4 ? s / 4.0 : Math.max(0.0, 1.0 - (s - 4) / 3.0);
		Vec3 toward = to.get(pecker).subtract(player.position());
		Vec3 face = eyes.add(new Vec3(toward.x, 0.0, toward.z).normalize().scale(0.6)).add(0.0, -0.5, 0.0);
		walk(chicken, to.get(pecker).lerp(face, up));
		face(chicken, eyes);
		if (s == 4) {
			particles(level, Looks.feathers(), face, 8, 0.2, 0.12);
			particles(level, ParticleTypes.CRIT, face, 6, 0.2, 0.1);
			hurt(level, player, Looks.CHICKEN_HURT, 0.12F);
		}
	}

	/** Height of the camera while the chickens carry the player: slow up, then held, then dropped. */
	private double liftHeight(int tick) {
		if (tick < LIFT) {
			return 0.0;
		}
		if (tick <= LIFTED) {
			double p = (tick - LIFT) / (double) (LIFTED - LIFT);
			return HEIGHT * p * p * (3.0 - 2.0 * p);
		}
		if (tick <= DROP) {
			return HEIGHT;
		}
		double q = Math.min(1.0, (tick - DROP) / (double) (LAND - DROP));
		return HEIGHT * (1.0 - q * q);
	}

	private void camera(LocalPlayer player, Vec3 eyes, int tick) {
		Vec3 offset = Vec3.ZERO;
		Vec3 target;
		float speed = 0.2F;
		if (tick < CALL) {
			target = victim.position().add(0.0, 0.3, 0.0);
			if (tick >= GLARE && tick < CALL) {
				// Leaning in: the chicken stares back.
				double p = Math.min(1.0, (tick - GLARE) / 8.0);
				offset = look.scale(0.4 * p).add(0.0, -0.3 * p, 0.0);
			}
		} else if (tick < PECK) {
			// Looking round at the flock coming in from everywhere, the ground trembling.
			target = eyes.add(look.yRot((tick - CALL) * 0.05F).scale(6.0)).add(0.0, -1.0, 0.0);
			speed = 0.08F;
			offset = new Vec3((random.nextFloat() - 0.5F) * 0.04, (random.nextFloat() - 0.5F) * 0.04, 0.0);
		} else if (tick < LIFT) {
			target = pecker >= 0 ? army.get(pecker).getEyePosition() : eyes.add(look.scale(2.0)).add(0.0, -1.5, 0.0);
			speed = 0.3F;
		} else {
			// Up in the air, looking down at the flock and the ground, turning slowly.
			double height = liftHeight(tick);
			offset = new Vec3(0.0, height, 0.0);
			target = player.position().add(look.yRot((tick - LIFT) * 0.03F).scale(3.0));
			speed = 0.15F;
		}
		moveCamera(offset);
		turnTowards(player, target, speed);
	}
}
