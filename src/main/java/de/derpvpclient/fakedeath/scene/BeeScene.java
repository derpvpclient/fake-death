package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.Vec3;

/**
 * Honey drips from the hive next to the player. One bee comes out, hovers right in front of the face and looks. Then
 * it gets angry. The hive starts to hum and shake, and the swarm pours out, bee after bee, a cloud round the head that
 * gets thicker and closer. The player swats around, the view darts from one to the next, they sting one after the
 * other, every bee that stung drops dead, and more keep coming out of the hive until the poison wins.
 */
public class BeeScene extends Scene {
	private static final int SCOUT_OUT = 10;
	private static final int ANGRY = 42;
	private static final int POUR = 54;
	private static final int SWARM = 16;
	private static final int POUR_EVERY = 2;
	/** How long a bee takes from the hive into the cloud. */
	private static final int FLIGHT = 14;
	private static final int FIRST_STING = 80;
	private static final int STING_EVERY = 12;
	/** How long the dive at the face takes, the sting is in the middle of it. */
	private static final int DIVE = 6;
	private static final int DEATH = FIRST_STING + STING_EVERY * 8 + DIVE / 2;

	private final BlockPos hive;
	private final List<Bee> bees = new ArrayList<>();
	private final List<Integer> outAt = new ArrayList<>();
	private final List<Fallen> fallen = new ArrayList<>();
	private Vec3 home;
	private Vec3 door;
	private Bee scout;
	private boolean scoutJoined;
	private Vec3 scoutHover;
	private Bee stinger;
	private int stingStart = -100;
	private int stings;
	private int now;
	private Bee glance;

	public BeeScene(BlockPos hive) {
		this.hive = hive;
	}

	@Override
	public int length() {
		return DEATH + 4;
	}

	@Override
	public int photoTick() {
		return FIRST_STING + STING_EVERY * 3;
	}

	@Override
	public int screenTint() {
		// The poison: sickly green, a little more with every sting.
		return stings == 0 ? 0 : Math.min(120, stings * 14) << 24 | 0x334A08;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.sting", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		now = tick;
		Vec3 head = player.getEyePosition();
		if (tick == 0) {
			home = hive != null ? Vec3.atCenterOf(hive) : groundSpot(level, player, 6.0, 40.0).add(0, 1.5, 0);
			// The bees come out of the side of the hive that faces the player.
			Vec3 toward = new Vec3(head.x - home.x, 0.0, head.z - home.z);
			door = home.add(toward.lengthSqr() < 1.0E-4 ? Vec3.ZERO : toward.normalize().scale(0.6));
			scout = spawn(level, EntityTypes.BEE, door);
		}
		if (scout == null) {
			return;
		}

		if (tick < ANGRY && tick % 10 == 3) {
			// Honey dripping out of the bottom of the hive.
			particles(level, ParticleTypes.DRIPPING_HONEY, home.add(0.0, -0.45, 0.0), 2, 0.3, 0.0);
			sound(level, home, SoundEvents.BEEHIVE_DRIP, SoundSource.BLOCKS, 0.6F, 1.0F);
		}
		if (tick < ANGRY && tick % 25 == 0) {
			sound(level, scout.position(), SoundEvents.BEE_LOOP, SoundSource.NEUTRAL, 0.8F, 1.0F);
		}

		moveScout(tick, head);
		if (tick == ANGRY) {
			// It has seen enough.
			scout.setPersistentAngerEndTime(level.getGameTime() + 2000);
			sound(level, scout.position(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.HOSTILE, 1.4F, 1.2F);
		}
		if (tick >= ANGRY && tick < POUR + 6) {
			// The whole hive hums and shakes.
			cameraShake(0.03F);
			if (tick % 4 == 0) {
				sound(level, home, SoundEvents.BEEHIVE_WORK, SoundSource.BLOCKS, 1.5F, 0.7F + random.nextFloat() * 0.3F);
				if (hive != null) {
					particles(level, new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(hive)), home, 4, 0.4, 0.05);
				}
			}
		}
		if (tick == POUR + 6) {
			cameraShake(0.0F);
		}

		// The swarm pours out bee after bee, and after every sting one more comes to take the dead one's place.
		int poured = tick - POUR;
		if (poured >= 0 && poured % POUR_EVERY == 0 && poured / POUR_EVERY < SWARM) {
			release(level, tick);
		}

		if (tick >= ANGRY && tick % 18 == 0) {
			Vec3 from = head.add((random.nextDouble() - 0.5) * 2.0, 0.0, (random.nextDouble() - 0.5) * 2.0);
			sound(level, from, SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.HOSTILE, 0.8F + Math.min(1.2F, bees.size() * 0.08F), 0.9F + random.nextFloat() * 0.2F);
		}

		moveSwarm(tick, head);
		sting(level, player, tick, head);
		dropDead(level);
		look(player, tick);
	}

	private void moveScout(int tick, Vec3 head) {
		if (tick < SCOUT_OUT || scoutJoined) {
			if (tick < SCOUT_OUT) {
				scout.setPos(door.x, door.y, door.z);
			}
			return;
		}
		// Out of the hive and right up to the face, hovering there and looking, a little nervous once it is angry.
		Vec3 hover = head.add(horizontalTo(head, home).scale(1.7)).add(0.0, -0.1 + 0.08 * Mth.sin(tick * 0.4F), 0.0);
		if (tick >= ANGRY) {
			hover = hover.add((random.nextDouble() - 0.5) * 0.15, (random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.15);
		}
		float t = Math.min(1.0F, (tick - SCOUT_OUT) / 16.0F);
		Vec3 pos = door.lerp(hover, t * t * (3.0F - 2.0F * t));
		scout.setPos(pos.x, pos.y, pos.z);
		face(scout, head);
		if (tick >= POUR) {
			// It joins the swarm, from where it hovers.
			scoutJoined = true;
			scoutHover = pos;
			bees.add(scout);
			outAt.add(tick);
		}
	}

	/** One more angry bee out of the hive. */
	private void release(ClientLevel level, int tick) {
		Bee bee = spawn(level, EntityTypes.BEE, door);
		bee.setPersistentAngerEndTime(level.getGameTime() + 2000);
		bees.add(bee);
		outAt.add(tick);
		particles(level, ParticleTypes.FALLING_HONEY, door, 1, 0.1, 0.0);
	}

	/** Round and round the head, the cloud getting tighter and faster. The one stinging dives right at the face. */
	private void moveSwarm(int tick, Vec3 head) {
		float tight = Math.min(1.0F, Math.max(0, tick - POUR) / 100.0F);
		for (int i = 0; i < bees.size(); i++) {
			Bee bee = bees.get(i);
			int sign = i % 2 == 0 ? 1 : -1;
			float phase = i * 2.39996F + tick * sign * (0.16F + tight * 0.08F);
			double radius = 2.3 - tight * 0.7 + 0.25 * Mth.sin(tick * 0.27F + i);
			Vec3 orbit = head.add(Mth.cos(phase) * radius, -0.5 + 0.55 * Mth.sin(tick * 0.21F + i * 1.7F), Mth.sin(phase) * radius);
			float out = Math.min(1.0F, (tick - outAt.get(i)) / (float) FLIGHT);
			Vec3 pos = out >= 1.0F ? orbit : bee == scout ? scoutHover.lerp(orbit, out)
				: door.lerp(orbit, out * (2.0F - out)).add(0.0, Mth.sin(out * Mth.PI) * 0.6, 0.0);
			if (bee == stinger) {
				double dive = Math.sin(Math.PI * Math.min(1.0, (tick - stingStart) / (double) DIVE));
				Vec3 face = head.add(horizontalTo(head, orbit).scale(0.8)).add(0.0, -0.25, 0.0);
				pos = pos.lerp(face, dive);
			}
			bee.setPos(pos.x, pos.y, pos.z);
			face(bee, head);
		}
	}

	/** Every few moments one of them dives in and stings; a bee that stung falls down dead, and the hive sends a new one. */
	private void sting(ClientLevel level, LocalPlayer player, int tick, Vec3 head) {
		if (tick >= FIRST_STING && (tick - FIRST_STING) % STING_EVERY == 0 && !bees.isEmpty()) {
			stinger = bees.get(random.nextInt(bees.size()));
			stingStart = tick;
			glance = stinger;
		}
		if (stinger != null && tick == stingStart + DIVE / 2) {
			stings++;
			sound(level, head, SoundEvents.BEE_STING, SoundSource.HOSTILE, 1.2F, 1.0F);
			particles(level, ParticleTypes.CRIT, head.add(0.0, -0.3, 0.0), 8, 0.3, 0.3);
			hit(level, player, SoundEvents.PLAYER_HURT, 0.11F, tick >= DEATH);
			if (stings == 1) {
				// Green hearts from the poison.
				addEffect(player, MobEffects.POISON, 400);
			}
		}
		if (stinger != null && tick == stingStart + DIVE) {
			int index = bees.indexOf(stinger);
			if (index >= 0) {
				bees.remove(index);
				outAt.remove(index);
			}
			// The stinger stays in the skin: the bee drops dead.
			stinger.setHealth(0.0F);
			fallen.add(new Fallen(stinger, stinger.position(), tick));
			sound(level, stinger.position(), SoundEvents.BEE_DEATH, SoundSource.NEUTRAL, 1.0F, 1.0F);
			stinger = null;
			if (tick < DEATH - FLIGHT) {
				release(level, tick);
			}
		}
		if (tick >= POUR && tick % 9 == 4 && (stinger == null || tick - stingStart > DIVE)) {
			// Swatting around at them.
			player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
		}
	}

	/** The dead ones fall to the ground and are gone in a puff. */
	private void dropDead(ClientLevel level) {
		for (Iterator<Fallen> it = fallen.iterator(); it.hasNext();) {
			Fallen dead = it.next();
			int since = now - dead.since();
			double floor = floor(level, dead.from().x, dead.from().z, dead.from().y - 1.0);
			double y = Math.max(floor, dead.from().y - 0.02 * since * since);
			dead.bee().setPos(dead.from().x, y, dead.from().z);
			if (since >= 22) {
				particles(level, ParticleTypes.POOF, dead.bee().position(), 3, 0.1, 0.02);
				despawn(level, dead.bee());
				it.remove();
			}
		}
	}

	private void look(LocalPlayer player, int tick) {
		if (tick < SCOUT_OUT + 4) {
			turnTowards(player, home, 0.12F);
		} else if (tick < ANGRY + 6) {
			// Eye to eye with the scout.
			turnTowards(player, scout.getEyePosition(), 0.15F);
		} else if (tick < POUR + 14) {
			turnTowards(player, home, 0.15F);
		} else {
			// Panic: the view darts from one to the next, and always to the one that stings.
			if ((glance == null || !bees.contains(glance) || tick % 8 == 0) && stinger == null && !bees.isEmpty()) {
				glance = bees.get(random.nextInt(bees.size()));
			}
			if (glance != null) {
				turnTowards(player, glance.getEyePosition(), stinger != null ? 0.4F : 0.22F);
			}
		}
	}

	/** The flat direction from one point to another, length 1. */
	private static Vec3 horizontalTo(Vec3 from, Vec3 to) {
		Vec3 way = new Vec3(to.x - from.x, 0.0, to.z - from.z);
		return way.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : way.normalize();
	}

	private record Fallen(Bee bee, Vec3 from, int since) {
	}
}
