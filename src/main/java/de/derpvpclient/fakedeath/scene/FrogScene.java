package de.derpvpclient.fakedeath.scene;

import de.derpvpclient.fakedeath.mixin.FrogAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.FrogVariant;
import net.minecraft.world.phys.Vec3;

/**
 * A frog plague in the swamp: croaking from everywhere, then frogs rain from the sky, hop over,
 * surround the player and snap at them with their tongues until there is nothing left.
 */
public class FrogScene extends Scene {
	private static final List<ResourceKey<FrogVariant>> COLORS = List.of(FrogVariant.TEMPERATE, FrogVariant.WARM, FrogVariant.COLD);
	private static final int COUNT = 10;
	private static final int RAIN_START = 35;
	private static final int RAIN_EVERY = 5;
	private static final double DROP_HEIGHT = 14.0;
	private static final double RING = 1.7;
	private static final int HOP_EVERY = 16;
	private static final int HOP_TICKS = 8;
	private static final int FIRST_TONGUE = 150;
	private static final int TONGUE_EVERY = 11;
	private static final int TONGUES = 6;
	private static final int DEATH = FIRST_TONGUE + TONGUE_EVERY * TONGUES + 8;
	private static final DustParticleOptions TONGUE = new DustParticleOptions(net.minecraft.world.phys.Vec3.fromRGB24(0xE0607E).toVector3f(), 0.9F);

	private final List<Hopper> frogs = new ArrayList<>();
	private final Hopper[] snapping = new Hopper[2];

	@Override
	public int length() {
		return DEATH;
	}

	@Override
	public int photoTick() {
		return FIRST_TONGUE - 5;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.frogs", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		// The croaking chorus, louder and louder.
		if (tick % 7 == 0 && tick < FIRST_TONGUE) {
			Vec3 from = player.position().add((random.nextDouble() - 0.5) * 16, 0, (random.nextDouble() - 0.5) * 16);
			sound(level, from, SoundEvents.FROG_AMBIENT, SoundSource.NEUTRAL, 1.0F + tick / 80.0F, 0.8F + random.nextFloat() * 0.4F);
		}

		int rained = tick - RAIN_START;
		if (rained >= 0 && rained < COUNT * RAIN_EVERY && rained % RAIN_EVERY == 0) {
			rain(level, player, frogs.size());
		}

		for (Hopper hopper : frogs) {
			move(level, player, hopper, tick);
		}

		if (tick >= RAIN_START && tick < FIRST_TONGUE && !frogs.isEmpty()) {
			// Looking up at the falling frogs first, then down at the ones that landed.
			Hopper last = frogs.getLast();
			turnTowards(player, last.landed ? last.pos.add(0, 0.3, 0) : last.pos, 0.1F);
		}

		int tongue = tick - FIRST_TONGUE;
		if (tongue >= 0 && tongue < TONGUE_EVERY * TONGUES && tongue % TONGUE_EVERY == 0 && !frogs.isEmpty()) {
			// Two frogs snap at once, from opposite sides.
			int first = (tongue / TONGUE_EVERY * 3) % frogs.size();
			snapping[0] = frogs.get(first);
			snapping[1] = frogs.get((first + frogs.size() / 2) % frogs.size());

			for (Hopper hopper : snapping) {
				hopper.frog.setTongueTarget(player);
				pose(hopper, Pose.USING_TONGUE, tick, 7);
				sound(level, hopper.pos, SoundEvents.FROG_TONGUE, SoundSource.NEUTRAL, 1.8F, 0.9F + random.nextFloat() * 0.2F);
			}

			turnTowards(player, snapping[0].pos.add(0, 0.3, 0), 0.4F);
			particles(level, ParticleTypes.CRIT, player.getEyePosition().add(0, -0.3, 0), 10, 0.3, 0.3);
			hit(level, player, SoundEvents.PLAYER_HURT, 0.17F, tongue / TONGUE_EVERY == TONGUES - 1);
		}

		if (tongue >= 0 && tongue < TONGUE_EVERY * TONGUES && tongue % TONGUE_EVERY < 3 && snapping[0] != null) {
			// The tongues reach all the way to the player, so everyone sees where the damage comes from.
			for (Hopper hopper : snapping) {
				Vec3 mouth = hopper.pos.add(0, 0.25, 0);
				line(level, TONGUE, mouth, player.getEyePosition().add(0, -0.35, 0), 0.12);
			}
		}

		if (tick == DEATH - 4) {
			sound(level, player.position(), SoundEvents.FROG_EAT, SoundSource.NEUTRAL, 2.0F, 0.7F);
		}
	}

	private void rain(ClientLevel level, LocalPlayer player, int index) {
		double angle = random.nextDouble() * Mth.TWO_PI;
		double distance = 1.5 + random.nextDouble() * 4.5;
		Vec3 start = player.position().add(Math.cos(angle) * distance, DROP_HEIGHT, Math.sin(angle) * distance);
		Frog frog = spawn(level, EntityType.FROG, start);

		level.registryAccess().lookupOrThrow(Registries.FROG_VARIANT).get(COLORS.get(index % COLORS.size()))
			.ifPresent(color -> ((FrogAccessor) frog).fakedeath$setVariant(color));
		frog.setPose(Pose.LONG_JUMPING);

		Hopper hopper = new Hopper(frog, index, start);
		frogs.add(hopper);
	}

	private void move(ClientLevel level, LocalPlayer player, Hopper hopper, int tick) {
		if (hopper.poseUntil >= 0 && tick >= hopper.poseUntil) {
			hopper.frog.setPose(Pose.STANDING);
			hopper.poseUntil = -1;
		}

		if (!hopper.landed) {
			hopper.fallSpeed += 0.08;
			double ground = floor(level, hopper.pos.x, hopper.pos.z, player.getY());
			double y = hopper.pos.y - hopper.fallSpeed;

			if (y <= ground) {
				y = ground;
				hopper.landed = true;
				hopper.frog.setPose(Pose.STANDING);
				sound(level, hopper.pos, SoundEvents.FROG_STEP, SoundSource.NEUTRAL, 1.5F, 0.8F);
				particles(level, ParticleTypes.SPLASH, hopper.pos, 6, 0.3, 0.1);
			}

			hopper.pos = new Vec3(hopper.pos.x, y, hopper.pos.z);
		} else if (hopper.hopStart >= 0) {
			float t = (tick - hopper.hopStart) / (float) HOP_TICKS;

			if (t >= 1.0F) {
				hopper.pos = hopper.hopTo;
				hopper.hopStart = -1;
				hopper.frog.setPose(Pose.STANDING);
			} else {
				hopper.pos = hopper.hopFrom.lerp(hopper.hopTo, t).add(0, Mth.sin(Mth.PI * t) * 0.8, 0);
			}
		} else {
			Vec3 ring = ringSpot(level, player, hopper.index);

			if ((tick + hopper.index * 3) % HOP_EVERY == 0 && hopper.pos.distanceTo(ring) > 0.3 && tick < FIRST_TONGUE + 20) {
				// One hop towards its place around the player.
				Vec3 way = ring.subtract(hopper.pos);
				Vec3 to = hopper.pos.add(way.normalize().scale(Math.min(way.length(), 1.8)));
				hopper.hopFrom = hopper.pos;
				hopper.hopTo = new Vec3(to.x, floor(level, to.x, to.z, player.getY()), to.z);
				hopper.hopStart = tick;
				hopper.frog.setPose(Pose.LONG_JUMPING);
				sound(level, hopper.pos, SoundEvents.FROG_LONG_JUMP, SoundSource.NEUTRAL, 0.6F, 1.0F);
			} else if (hopper.poseUntil < 0 && tick < FIRST_TONGUE && (tick + hopper.index * 11) % 45 == 0) {
				pose(hopper, Pose.CROAKING, tick, 25);
				sound(level, hopper.pos, SoundEvents.FROG_AMBIENT, SoundSource.NEUTRAL, 1.0F, 0.9F + random.nextFloat() * 0.2F);
			}
		}

		hopper.frog.setPos(hopper.pos.x, hopper.pos.y, hopper.pos.z);
		face(hopper.frog, player.getEyePosition());
	}

	private static void pose(Hopper hopper, Pose pose, int tick, int ticks) {
		hopper.frog.setPose(pose);
		hopper.poseUntil = tick + ticks;
	}

	private static Vec3 ringSpot(ClientLevel level, LocalPlayer player, int index) {
		float angle = index * Mth.TWO_PI / COUNT;
		double x = player.getX() + Mth.cos(angle) * RING;
		double z = player.getZ() + Mth.sin(angle) * RING;
		return new Vec3(x, floor(level, x, z, player.getY()), z);
	}

	private static final class Hopper {
		final Frog frog;
		final int index;
		Vec3 pos;
		double fallSpeed;
		boolean landed;
		Vec3 hopFrom;
		Vec3 hopTo;
		int hopStart = -1;
		int poseUntil = -1;

		Hopper(Frog frog, int index, Vec3 pos) {
			this.frog = frog;
			this.index = index;
			this.pos = pos;
		}
	}
}
