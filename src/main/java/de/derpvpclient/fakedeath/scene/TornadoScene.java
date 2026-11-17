package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A tornado forms in the distance, comes closer, lifts the player high up, spins them around and drops them. A
 * screaming goat goes round and round in it, and someone famous, a different one every time; once the player is up
 * in the funnel, both fly right alongside them.
 */
public class TornadoScene extends Scene {
	private static final int ARRIVE = 100;
	private static final int LIFT = 45;
	private static final int DROP = 12;
	private static final double HEIGHT = 14.0;
	private static final int DEATH = ARRIVE + LIFT + 20 + DROP;
	private final List<Display.BlockDisplay> debris = new ArrayList<>();
	private Vec3 start;
	private Goat goat;
	private RemotePlayer famous;
	/** Skin and name of the famous people from the other scenes; Herobrine has no name in the language files. */
	private static final String[][] FAMOUS = {
		{"trump", "fakedeath.trumpmauer.trump"}, {"merkel", "fakedeath.merkel.name"}, {"putin", "fakedeath.putin.name"}, {"stalin", "fakedeath.stalin.name"},
		{"ulbricht", "fakedeath.ulbricht.name"}, {"khrushchev", "fakedeath.chruschtschow.name"}, {"gagarin", "fakedeath.gagarin.name"},
		{"napoleon", "fakedeath.napoleon.name"}, {"genghis", "fakedeath.genghis.name"}, {"lenin", "fakedeath.revolution.lenin"}, {"herobrine", null},
		{"kim", "fakedeath.trumprakete.kim"}, {"caesar", "fakedeath.caesar.name"}, {"cleopatra", "fakedeath.cleopatra.name"}, {"wilhelm", "fakedeath.wilhelm.name"}, {"bismarck", "fakedeath.bismarck.name"}, {"barbarossa", "fakedeath.barbarossa.name"}, {"newton", "fakedeath.newton.name"}, {"leonardo", "fakedeath.leonardo.name"}, {"tesla", "fakedeath.tesla.name"}, {"musk", "fakedeath.musk.name"}, {"jobs", "fakedeath.jobs.name"}, {"gates", "fakedeath.gates.name"}, {"zuckerberg", "fakedeath.zuckerberg.name"}, {"schabowski", "fakedeath.mauerfall.schabowski"}};

	@Override
	public int length() {
		return DEATH + 20;
	}

	@Override
	public int photoTick() {
		return ARRIVE - 25;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.tornado", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			start = groundSpot(level, player, 22.0, 20.0);
			for (int i = 0; i < 9; i++) {
				Display.BlockDisplay piece = blockDisplay(level, i % 3 == 0 ? Blocks.DIRT.defaultBlockState() : i % 3 == 1 ? Blocks.OAK_LEAVES.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState(), start, 6.0F);
				Looks.farView(piece);
				debris.add(piece);
			}
			goat = spawn(level, EntityType.GOAT, start.add(0.0, 5.0, 0.0));
			goat.noPhysics = true;
			goat.setScreamingGoat(true);
			String[] who = FAMOUS[random.nextInt(FAMOUS.length)];
			String name = who[1] == null ? "Herobrine" : Component.translatable(who[1]).getString();
			// A player name over the head holds 16 letters: the last name alone for longer ones.
			if (name.length() > 16) {
				name = name.substring(name.lastIndexOf(' ') + 1);
			}
			famous = person(level, name, who[0], false, true, start.add(0.0, 9.0, 0.0));
			famous.noPhysics = true;
		}

		Vec3 center = tick < ARRIVE ? start.lerp(player.position(), tick / (double) ARRIVE) : player.position();
		funnel(level, center, tick);
		passengers(level, player, center, tick);

		if (tick % 20 == 0 && tick < DEATH) {
			sound(level, center, SoundEvents.ELYTRA_FLYING, SoundSource.WEATHER, 1.0F + Math.min(2.0F, tick / 50.0F), 0.6F);
			sound(level, center, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 1.0F, 0.5F);
		}

		if (tick < ARRIVE) {
			turnTowards(player, center.add(0.0, 5.0, 0.0), 0.08F);
			cameraShake(tick / (float) ARRIVE * 0.15F);
		} else if (tick < ARRIVE + LIFT + 20) {
			// Up in the funnel, around and around.
			float p = Math.min(1.0F, (tick - ARRIVE) / (float) LIFT);
			double angle = (tick - ARRIVE) * 0.35;
			moveCamera(new Vec3(Math.cos(angle) * 1.5, HEIGHT * p, Math.sin(angle) * 1.5));
			turnTowards(player, orbit(player.getEyePosition().add(0.0, HEIGHT * p, 0.0), 6.0, angle + 1.6, -2.0), 0.6F);
			if (tick == ARRIVE + LIFT / 2) {
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.3F);
			}
		} else if (tick <= DEATH) {
			float p = (tick - ARRIVE - LIFT - 20) / (float) DROP;
			moveCamera(new Vec3(0.0, HEIGHT * (1.0F - p * p), 0.0));
			turnTowards(player, player.position().add(horizontalLook(player).scale(3.0)), 0.5F);
		}

		if (tick == DEATH) {
			moveCamera(Vec3.ZERO);
			sound(level, player.position(), SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 1.5F, 1.0F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	/**
	 * The goat and the famous one: round and round in the funnel, then, with the player up in it, right next to them in
	 * the picture, and at the end they come down with the player.
	 */
	private void passengers(ClientLevel level, LocalPlayer player, Vec3 center, int tick) {
		if (goat == null || famous == null) {
			return;
		}
		Vec3 goatAt = orbit(center, 2.6, tick * 0.45, 5.5);
		Vec3 famousAt = orbit(center, 3.6, tick * 0.4 + Math.PI, 9.5);
		if (tick >= ARRIVE) {
			// The same height as the camera, a little ahead of where it looks while it circles.
			double angle = (tick - ARRIVE) * 0.35;
			float lift = Math.min(1.0F, (tick - ARRIVE) / (float) LIFT);
			float fall = tick < ARRIVE + LIFT + 20 ? 0.0F : Math.min(1.0F, (tick - ARRIVE - LIFT - 20) / (float) DROP);
			Vec3 eyes = player.getEyePosition().add(0.0, HEIGHT * lift * (1.0F - fall * fall), 0.0);
			float blend = Mth.clamp((tick - ARRIVE) / 15.0F, 0.0F, 1.0F);
			blend = blend * blend * (3.0F - 2.0F * blend);
			goatAt = goatAt.lerp(orbit(eyes, 3.2, angle + 1.25, -1.6), blend);
			famousAt = famousAt.lerp(orbit(eyes, 4.6, angle + 2.0, -1.4), blend);
		}
		if (tick >= DEATH) {
			// Down next to the player.
			goatAt = orbit(player.position(), 2.2, 0.8, 0.0);
			famousAt = orbit(player.position(), 3.0, 2.6, 0.0);
		}
		spin(goat, goatAt, tick < DEATH ? tick * 31.0F : 0.0F);
		spin(famous, famousAt, tick < DEATH ? tick * -27.0F : 120.0F);
		if (tick % 22 == 5 && tick <= DEATH + 4) {
			sound(level, goat.position(), SoundEvents.GOAT_SCREAMING_AMBIENT, SoundSource.NEUTRAL, 2.5F, 0.85F + random.nextFloat() * 0.3F);
		}
	}

	private static void spin(LivingEntity who, Vec3 at, float yaw) {
		who.setPos(at.x, at.y, at.z);
		who.setYRot(yaw);
		who.setYBodyRot(yaw);
		who.setYHeadRot(yaw);
	}

	private void funnel(ClientLevel level, Vec3 center, int tick) {
		// A cone of cloud particles, thin at the ground and wide at the top, turning fast.
		for (int i = 0; i < 45; i++) {
			double h = random.nextDouble() * 20.0;
			double r = 0.4 + h * 0.35;
			double a = tick * 0.5 + random.nextDouble() * Math.PI * 2;
			Vec3 p = orbit(center, r, a, h);
			level.addParticle(h < 2.0 ? ParticleTypes.POOF : i % 3 == 0 ? ParticleTypes.CAMPFIRE_COSY_SMOKE : i % 3 == 1 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CLOUD, p.x, p.y, p.z, -Math.sin(a) * 0.3, 0.05, Math.cos(a) * 0.3);
		}
		for (int i = 0; i < debris.size(); i++) {
			double h = 2.0 + i * 1.8;
			Vec3 p = orbit(center, 1.0 + h * 0.3, tick * 0.4 + i * 1.7, h);
			place(debris.get(i), p, new Vector3f(0.7F), new Quaternionf().rotateXYZ(tick * 0.3F, tick * 0.2F, i));
		}
	}
}
