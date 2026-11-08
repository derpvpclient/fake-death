package de.derpvpclient.fakedeath.scene;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * A riding lesson. The instructor brings a horse, the player gets on, the instructor sits behind them. "And now left …"
 * The horse goes right. "No, the other left!" It spins round, gallops off, "Brake! BRAKE!", rears up and throws the
 * player off.
 */
public class InstructorScene extends NpcScene {
	private static final int MOUNT = 20;
	private static final int LEFT = 30;
	private static final int WRONG = 85;
	private static final int SPIN = 105;
	private static final int BRAKE = 135;
	private static final int REAR = 165;
	private static final int THROW = 185;
	private static final int LAND = 203;
	/** Camera height above the horse's feet while sitting in the saddle. */
	private static final double SEAT = 2.4;

	private Horse horse;
	private Vec3 at;
	private float heading;
	private float speed;
	private Vec3 flyFrom;
	private Vec3 flyTo;
	private boolean riding;
	/** Where the camera looks, worked out in act() and handed to the character scene as its focus. */
	private Vec3 lookAt;

	@Override
	public boolean seated() {
		return riding;
	}

	public InstructorScene() {
		super("fahrlehrer", ChatFormatting.GOLD);
	}

	@Override
	protected int lineCount() {
		return 2;
	}

	@Override
	protected int actLength() {
		return LAND + 50;
	}

	@Override
	protected int photoOffset() {
		return SPIN + 10;
	}

	@Override
	protected VillagerProfession profession() {
		return VillagerProfession.LEATHERWORKER;
	}

	@Override
	protected ItemStack held() {
		return new ItemStack(Items.LEAD);
	}

	@Override
	protected Vec3 focus(LocalPlayer player) {
		if (at == null) {
			return npc.getEyePosition();
		}
		return lookAt != null ? lookAt : ahead(0.6);
	}

	@Override
	protected void setup(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
		horse = spawn(level, EntityType.HORSE, start);
		horse.noPhysics = true;
		Looks.saddle(horse);
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		super.tick(minecraft, level, player, tick);
		if (horse == null || tick >= actStart()) {
			return;
		}
		// Before the lesson the instructor leads the horse on the lead, it walks beside them and waits.
		Vec3 side = towardPlayer(player).yRot(Mth.HALF_PI).scale(1.6);
		Vec3 beside = npc.position().add(side);
		walk(horse, new Vec3(beside.x, floor(level, beside.x, beside.z, player.getY()), beside.z));
		face(horse, player.getEyePosition().add(side));
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		riding = t >= MOUNT / 2 && t < THROW;
		if (t == 0) {
			at = horse.position();
			Vec3 look = horizontalLook(player);
			heading = (float) Mth.atan2(look.z, look.x);
			speed = 0.0F;
			sound(level, horse.position(), Looks.SADDLE, SoundSource.NEUTRAL, 1.0F, 1.0F);
		}

		if (t <= MOUNT) {
			// The horse steps under the player, and they climb up.
			float p = t / (float) MOUNT;
			if (t == MOUNT / 2) {
				// Sitting right behind the player his name would hang in the middle of the F5 view, also when the crosshair is
				// on him. The chat still shows who talks.
				npc.setCustomNameVisible(false);
				npc.setCustomName(null);
			}
			lookAt = ahead(thirdPerson() ? 8.0 : 0.6);
			at = at.lerp(player.position(), 0.25);
			moveCamera(new Vec3(0.0, (SEAT - player.getEyeHeight()) * p * p, 0.0));
			placeHorse(level, player);
			placeInstructor();
			if (t == MOUNT) {
				sound(level, at, SoundEvents.HORSE_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.0F);
			}
			return;
		}

		if (t == LEFT) {
			speak(minecraft, level, "fakedeath.fahrlehrer.left");
		}
		if (t == WRONG) {
			speak(minecraft, level, "fakedeath.fahrlehrer.wrong");
			sound(level, at, SoundEvents.HORSE_ANGRY, SoundSource.NEUTRAL, 1.0F, 1.1F);
		}
		if (t == BRAKE) {
			speak(minecraft, level, "fakedeath.fahrlehrer.brake");
		}

		if (t < WRONG) {
			// A slow walk, and it turns right instead of left.
			speed = Math.min(0.12F, speed + 0.01F);
			if (t > LEFT + 15) {
				heading += 0.035F;
			}
		} else if (t < SPIN) {
			// Stops, then spins round the other way.
			speed *= 0.8F;
			heading -= 0.16F;
		} else if (t < REAR) {
			// And off, faster and faster.
			speed = Math.min(0.55F, speed + 0.03F);
			heading += Mth.sin(t * 0.2F) * 0.03F;
			if (t % 4 == 0) {
				sound(level, at, SoundEvents.HORSE_GALLOP, SoundSource.NEUTRAL, 1.0F, 1.0F);
				particles(level, ParticleTypes.CLOUD, at, 3, 0.4, 0.02);
			}
		} else {
			speed *= 0.6F;
		}

		if (t < THROW) {
			Vec3 next = at.add(forward().scale(speed));
			at = new Vec3(next.x, floor(level, next.x, next.z, at.y), next.z);
			placeHorse(level, player);
			placeInstructor();
		}

		// Bouncing in the saddle, more the faster it goes.
		double bounce = Math.abs(Mth.sin(t * (speed > 0.3F ? 0.7F : 0.35F))) * speed * 0.6;
		if (t < THROW) {
			double rear = t >= REAR ? Mth.sin(Math.min(1.0F, (t - REAR) / 12.0F) * Mth.HALF_PI) * 1.2 : 0.0;
			Vec3 seat = at.add(0.0, SEAT + bounce + rear, 0.0).subtract(forward().scale(rear * 0.4));
			moveCamera(seat.subtract(player.getEyePosition()));
		}

		if (t == REAR) {
			// Full stop, up on its hind legs.
			Looks.rear(horse);
			sound(level, at, SoundEvents.HORSE_ANGRY, SoundSource.NEUTRAL, 1.5F, 0.8F);
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.3F);
		}
		if (t < THROW) {
			// Straight ahead over the horse's head, up into the sky when it rears. In F5 steeply from above and behind, so
			// the instructor sitting behind the player does not block the view and the camera never dips into the ground.
			lookAt = thirdPerson() ? ahead(8.0) : t > REAR ? at.add(forward().scale(3.0)).add(0.0, SEAT + 2.5, 0.0) : ahead(0.6);
		}

		if (t == THROW) {
			// Thrown off over its head, in a high arc onto the ground.
			flyFrom = at.add(0.0, SEAT + 1.2, 0.0);
			Vec3 land = at.add(forward().scale(4.5));
			flyTo = new Vec3(land.x, floor(level, land.x, land.z, at.y) + 0.3, land.z);
			sound(level, at, SoundEvents.HORSE_ANGRY, SoundSource.NEUTRAL, 1.5F, 1.0F);
		}
		if (t >= THROW && t <= LAND) {
			float p = (t - THROW) / (float) (LAND - THROW);
			Vec3 fly = flyFrom.lerp(flyTo, p).add(0.0, Mth.sin(p * Mth.PI) * 1.8, 0.0);
			moveCamera(fly.subtract(player.getEyePosition()));
			// Head over heels: the view tips down to the ground ahead.
			lookAt = fly.add(forward().scale(2.0)).add(0.0, thirdPerson() ? -2.5 : -2.5 * p, 0.0);
		}
		if (t == LAND) {
			particles(level, ParticleTypes.CRIT, flyTo, 25, 0.6, 0.3);
			particles(level, ParticleTypes.CLOUD, flyTo, 15, 0.8, 0.05);
			kill(level, player, SoundEvents.PLAYER_BIG_FALL);
		}
		if (t > LAND) {
			moveCamera(flyTo.subtract(player.getEyePosition()));
			// Looking back at the horse from the ground, in F5 level with the ground so the camera stays above it.
			lookAt = thirdPerson() ? horse.position().add(0.0, 0.3, 0.0) : horse.getEyePosition();
		}
		if (t == LAND + 25) {
			speak(minecraft, level, "fakedeath.fahrlehrer.fail");
		}
	}

	/** A point far ahead in the riding direction, the given height below the seat. */
	private Vec3 ahead(double drop) {
		return at.add(forward().scale(8.0)).add(0.0, SEAT - drop, 0.0);
	}

	private Vec3 forward() {
		return new Vec3(Mth.cos(heading), 0.0, Mth.sin(heading));
	}

	private void placeHorse(ClientLevel level, LocalPlayer player) {
		walk(horse, at);
		face(horse, horse.getEyePosition().add(forward().scale(4.0)).add(0.0, -0.5, 0.0));
	}

	/** The instructor sits right behind the player, legs in the horse, so he looks like he is sitting. */
	private void placeInstructor() {
		Vec3 seat = at.subtract(forward().scale(1.1)).add(0.0, 0.6, 0.0);
		walk(npc, seat);
		face(npc, seat.add(forward().scale(4.0)).add(0.0, 1.2, 0.0));
	}
}
