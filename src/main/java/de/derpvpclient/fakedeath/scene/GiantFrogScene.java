package de.derpvpclient.fakedeath.scene;

import de.derpvpclient.fakedeath.CameraDirector;
import de.derpvpclient.fakedeath.mixin.FrogAccessor;
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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.FrogVariant;
import net.minecraft.world.entity.animal.frog.FrogVariants;
import net.minecraft.world.phys.Vec3;

/**
 * A giant frog: deep croaking from far away, then it comes closer in huge leaps that shake the
 * ground, croaks right in front of the player, snaps with its tongue and swallows them whole.
 * Inside it is dark and wet until the stomach is done.
 */
public class GiantFrogScene extends Scene {
	private static final List<ResourceKey<FrogVariant>> COLORS = List.of(FrogVariants.TEMPERATE, FrogVariants.WARM, FrogVariants.COLD);
	private static final double SIZE = 6.0;
	private static final int APPEAR = 45;
	private static final double START_DISTANCE = 24.0;
	private static final int[] HOPS = {60, 97, 134};
	private static final double[] HOP_DISTANCES = {17.0, 11.0, 5.5};
	private static final int HOP_TICKS = 16;
	private static final int CROAK = 156;
	private static final int TONGUE = 205;
	private static final int STUCK = TONGUE + 4;
	private static final int PULL = TONGUE + 12;
	private static final int PULL_TICKS = 18;
	private static final int GULP = PULL + PULL_TICKS;
	private static final DustParticleOptions TONGUE_COLOR = new DustParticleOptions(0xE0607E, 2.2F);
	private static final int TASTY = GULP + 35;
	private static final int DEATH = TASTY + 65;
	private static final int BURP = DEATH + 15;
	private static final int FADE = 5;
	private static final int STOMACH = 0x300A0E;
	private static final int ACID = 0x2E4A0C;
	private static final int ACID_EVERY = 30;
	private static final int ACID_FIRST = 15;

	private Frog frog;
	private Vec3 direction;
	private Vec3 pos;
	private Vec3 hopFrom;
	private Vec3 hopTo;
	private int hopStart = -1;
	private int hopTicks;
	private double hopHeight;
	private int poseUntil = -1;
	private int tint;

	@Override
	public int length() {
		return DEATH;
	}

	@Override
	public int photoTick() {
		return CROAK + 12;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.giantfrog", player.getDisplayName());
	}

	@Override
	public int screenTint() {
		return tint;
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0 || tick == 24) {
			// Something big croaks far away.
			Vec3 far = groundSpot(level, player, 30.0, 0.0);
			sound(level, far, SoundEvents.FROG_AMBIENT, SoundSource.NEUTRAL, 6.0F, 0.5F);
		}

		if (tick == APPEAR) {
			appear(level, player);
		}

		if (frog != null) {
			act(minecraft, level, player, tick);
		}

		if (tick >= GULP) {
			inside(minecraft, level, player, tick);
		}
	}

	private void appear(ClientLevel level, LocalPlayer player) {
		Vec3 spot = groundSpot(level, player, START_DISTANCE, (random.nextDouble() - 0.5) * 50.0);
		Vec3 away = spot.subtract(player.position());
		direction = new Vec3(away.x, 0.0, away.z).normalize();
		pos = new Vec3(spot.x, floor(level, spot.x, spot.z, player.getY()), spot.z);

		frog = spawn(level, EntityTypes.FROG, pos);
		level.registryAccess().lookupOrThrow(Registries.FROG_VARIANT).get(COLORS.get(random.nextInt(COLORS.size())))
			.ifPresent(color -> ((FrogAccessor) frog).fakedeath$setVariant(color));

		AttributeInstance scale = frog.getAttribute(Attributes.SCALE);
		if (scale != null) {
			scale.setBaseValue(SIZE);
		}
		frog.refreshDimensions();
		// The big body reaches the player in the end, it must not push them around.
		frog.noPhysics = true;
		face(frog, player.getEyePosition());
	}

	private void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (poseUntil >= 0 && tick >= poseUntil) {
			frog.setPose(Pose.STANDING);
			poseUntil = -1;
		}

		for (int i = 0; i < HOPS.length; i++) {
			if (tick == HOPS[i]) {
				Vec3 to = player.position().add(direction.scale(HOP_DISTANCES[i]));
				hop(level, new Vec3(to.x, floor(level, to.x, to.z, player.getY()), to.z), tick, HOP_TICKS, 5.0);
			}
		}

		if (tick == CROAK) {
			say(minecraft, Component.translatable("fakedeath.scene.riesenfrosch"), Component.translatable("fakedeath.giantfrog.croak"));
		}

		if (tick == CROAK || tick == CROAK + 25) {
			pose(Pose.CROAKING, tick, 22);
			sound(level, pos, SoundEvents.FROG_AMBIENT, SoundSource.NEUTRAL, 4.0F, 0.45F);
		}

		if (tick == TONGUE) {
			// Mouth open, the tongue shoots out.
			frog.setTongueTarget(player);
			frog.setPose(Pose.USING_TONGUE);
			poseUntil = -1;
			sound(level, pos, SoundEvents.FROG_TONGUE, SoundSource.NEUTRAL, 4.0F, 0.5F);
		}

		if (tick >= TONGUE && tick < GULP) {
			tongue(level, player, tick);
		}

		if (tick == STUCK) {
			// It sticks.
			sound(level, player.getEyePosition(), SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 2.0F, 0.5F);
			particles(level, ParticleTypes.ITEM_SLIME, player.getEyePosition().add(horizontalLook(player).scale(0.4)), 20, 0.3, 0.2);
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.2F);
		}

		if (tick >= PULL && tick < GULP) {
			// Pulled in on the tongue, faster and faster, right into the mouth. Only the camera moves, the player stays put.
			float t = (tick - PULL + 1) / (float) PULL_TICKS;
			Vec3 eyes = player.getEyePosition();
			moveCamera(mouth(0.6).subtract(eyes).scale(t * t));

			if ((tick - PULL) % 4 == 0) {
				sound(level, eyes, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.NEUTRAL, 1.5F, 0.6F + t * 0.4F);
			}
		}

		if (hopStart >= 0) {
			float t = (tick - hopStart) / (float) hopTicks;

			if (t >= 1.0F) {
				pos = hopTo;
				hopStart = -1;
				frog.setPose(Pose.STANDING);
				land(level, player);
			} else {
				pos = hopFrom.lerp(hopTo, t).add(0.0, Mth.sin(Mth.PI * t) * hopHeight, 0.0);
			}
		}

		frog.setPos(pos.x, pos.y, pos.z);
		face(frog, player.getEyePosition());

		if (tick >= APPEAR && tick < GULP) {
			// Eyes on the frog, from its whole body down to its mouth once the tongue comes.
			Vec3 look = tick < TONGUE ? pos.add(0.0, SIZE * 0.3, 0.0) : mouth(0.0);
			turnTowards(player, look, tick < TONGUE ? 0.12F : 0.35F);
		}
	}

	/** The frog's mouth, {@code inside} blocks back from its lips. */
	private Vec3 mouth(double inside) {
		return pos.add(direction.scale(-(SIZE * 0.26 - inside))).add(0.0, SIZE * 0.2, 0.0);
	}

	/** A thick pink tongue from the mouth to where the camera is. */
	private void tongue(ClientLevel level, LocalPlayer player, int tick) {
		Vec3 camera = player.getEyePosition().add(CameraDirector.positionOffsetNow());
		Vec3 lips = mouth(0.0);
		// The first ticks it shoots out, then it sticks to the face.
		float reach = Math.min(1.0F, (tick - TONGUE + 1) / (float) (STUCK - TONGUE));
		Vec3 tip = lips.lerp(camera.add(horizontalLook(player).scale(0.3)), reach);
		line(level, TONGUE_COLOR, lips, tip, 0.18);
	}

	private void hop(ClientLevel level, Vec3 to, int tick, int ticks, double height) {
		hopFrom = pos;
		hopTo = to;
		hopStart = tick;
		hopTicks = ticks;
		hopHeight = height;
		frog.setPose(Pose.LONG_JUMPING);
		poseUntil = -1;
		sound(level, pos, SoundEvents.FROG_LONG_JUMP, SoundSource.NEUTRAL, 3.0F, 0.5F);
	}

	private void land(ClientLevel level, LocalPlayer player) {
		double closeness = 1.0 - Math.min(1.0, pos.distanceTo(player.position()) / START_DISTANCE);
		float volume = 2.0F + (float) closeness * 4.0F;
		sound(level, pos, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.NEUTRAL, volume, 0.6F);
		sound(level, pos, SoundEvents.FROG_STEP, SoundSource.NEUTRAL, volume, 0.4F);
		particles(level, ParticleTypes.CLOUD, pos.add(0.0, 0.3, 0.0), 30, SIZE * 0.3, 0.15);
		player.animateHurt(0.0F);
	}

	private void inside(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		int since = tick - GULP;
		int alpha = Math.min(240, since * 240 / FADE);
		// Each splash of stomach acid flashes the dark green.
		boolean acid = since % ACID_EVERY >= ACID_FIRST && since % ACID_EVERY < ACID_FIRST + 6 && since >= ACID_FIRST;
		tint = alpha << 24 | (acid ? ACID : STOMACH);

		// Inside the mouth until it is over.
		moveCamera(mouth(1.2).subtract(player.getEyePosition()));

		if (tick == GULP) {
			// The mouth snaps shut.
			frog.setPose(Pose.STANDING);
			sound(level, player.position(), SoundEvents.FROG_EAT, SoundSource.NEUTRAL, 4.0F, 0.5F);
			// Crushed by the jaws: the biggest hit.
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.45F);
		}

		if (tick < DEATH) {
			if (since % 20 == 10) {
				sound(level, player.position(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.NEUTRAL, 1.5F, 0.7F);
			}

			if (since % 13 == 4) {
				sound(level, player.position(), SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 1.2F, 0.4F + random.nextFloat() * 0.2F);
			}

			if (since % ACID_EVERY == ACID_FIRST) {
				// Stomach acid: it hisses and burns.
				sound(level, player.position(), SoundEvents.LAVA_EXTINGUISH, SoundSource.NEUTRAL, 1.0F, 1.4F);
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.1F);
			}
		}

		if (tick == DEATH - 2) {
			// The last splash of acid.
			sound(level, player.position(), SoundEvents.LAVA_EXTINGUISH, SoundSource.NEUTRAL, 1.5F, 1.2F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}

		if (tick == TASTY) {
			say(minecraft, Component.translatable("fakedeath.scene.riesenfrosch"), Component.translatable("fakedeath.giantfrog.tasty"));
		}

		if (tick == BURP) {
			sound(level, player.position(), SoundEvents.PLAYER_BURP, SoundSource.NEUTRAL, 3.0F, 0.5F);
		}
	}

	private void pose(Pose pose, int tick, int ticks) {
		frog.setPose(pose);
		poseUntil = tick + ticks;
	}
}
