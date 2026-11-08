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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * A cat strolls over and rubs against the player's legs, purring. Then ten more come, all circling, all purring louder
 * and louder, until the whole view shakes and the player is purred to death.
 */
public class CatScene extends Scene {
	private static final int CROWD = 75;
	private static final int ARRIVE = 40;
	private static final int CATS = 10;
	private static final int[] HURTS = {140, 160, 180};
	private static final int DEATH = 200;

	private final List<LivingEntity> cats = new ArrayList<>();
	private final List<Vec3> starts = new ArrayList<>();

	@Override
	public int length() {
		return DEATH + 25;
	}

	@Override
	public int photoTick() {
		return CROWD + ARRIVE + 10;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.katzen", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			add(level, groundSpot(level, player, 6.0, 20.0));
		}
		if (tick == CROWD) {
			for (int i = 0; i < CATS; i++) {
				add(level, groundSpot(level, player, 8.0 + random.nextDouble() * 4.0, i * 360.0 / CATS - 180.0));
			}
		}
		if (cats.isEmpty()) {
			return;
		}

		for (int i = 0; i < cats.size(); i++) {
			LivingEntity cat = cats.get(i);
			int since = i == 0 ? tick : tick - CROWD;
			// Round and round the legs, every cat on its own ring and in its own direction.
			double radius = i == 0 ? 0.75 : 0.9 + (i % 3) * 0.45;
			double speed = (i % 2 == 0 ? 1 : -1) * (0.06 + (i % 4) * 0.01);
			double angle = i * 1.3 + tick * speed;
			Vec3 ring = player.position().add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
			ring = new Vec3(ring.x, floor(level, ring.x, ring.z, player.getY()), ring.z);
			int walkIn = i == 0 ? 35 : ARRIVE;
			Vec3 at = since < walkIn ? starts.get(i).lerp(ring, since / (double) walkIn) : ring;
			Vec3 before = cat.position();
			walk(cat, at);
			Vec3 moved = at.subtract(before);
			face(cat, moved.horizontalDistanceSqr() > 1.0E-4 ? cat.getEyePosition().add(moved.normalize()) : player.getEyePosition());
		}

		// The purring gets louder and faster, and the view starts to buzz with it.
		float crowd = tick < CROWD ? 0.0F : Math.min(1.0F, (tick - CROWD) / (float) (DEATH - CROWD));
		int every = tick < CROWD ? 24 : Math.max(3, 14 - (int) (crowd * 12));
		if (tick % every == 0) {
			Vec3 from = cats.get(random.nextInt(cats.size())).position();
			sound(level, from, Looks.CAT_PURR, SoundSource.NEUTRAL, 0.8F + crowd * 1.5F, 0.9F + random.nextFloat() * 0.3F);
		}
		if (tick % 10 == 0) {
			// Little hearts over the cats, never right in front of the eyes.
			for (int h = 0; h < 1 + (int) (crowd * 3); h++) {
				LivingEntity cat = cats.get(random.nextInt(cats.size()));
				if (cat.distanceToSqr(player) > 2.0) {
					particles(level, ParticleTypes.HEART, cat.position().add(0.0, 1.0, 0.0), 1, 0.1, 0.0);
				}
			}
		}
		if (tick > CROWD + ARRIVE) {
			cameraShake(0.05F + crowd * 0.5F);
		}
		turnTowards(player, tick < CROWD ? cats.get(0).position() : player.position().add(horizontalLook(player).yRot(Mth.sin(tick * 0.04F) * 1.2F).scale(2.0)), 0.12F);

		for (int h : HURTS) {
			if (tick == h) {
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.2F);
			}
		}
		if (tick == DEATH) {
			sound(level, player.position(), Looks.CAT_PURREOW, SoundSource.NEUTRAL, 2.0F, 0.8F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void add(ClientLevel level, Vec3 spot) {
		LivingEntity cat = spawn(level, EntityTypes.CAT, spot);
		cat.noPhysics = true;
		Looks.catColor(cat, cats.size() * 3 + 1);
		cats.add(cat);
		starts.add(spot);
	}
}
