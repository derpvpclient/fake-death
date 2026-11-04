package de.derpvpclient.fakedeath.scene;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/** Someone stands far away and stares. Gone. Then right behind you. */
public class HerobrineScene extends Scene {
	private static final DustParticleOptions EYE = new DustParticleOptions(net.minecraft.world.phys.Vec3.fromRGB24(0xFFFFFF).toVector3f(), 0.35F);
	private static final UUID STEVE = steveUuid();
	private static final int VANISH = 80;
	private static final int BEHIND = 115;
	private static final int SCARE = 145;

	private RemotePlayer herobrine;

	@Override
	public int length() {
		return SCARE + 8;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.herobrine", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			herobrine = spawn(level, new RemotePlayer(level, new GameProfile(STEVE, "Herobrine")), groundSpot(level, player, 16.0, 8.0));
			sound(level, player.position(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.8F);
		}

		if (tick == VANISH && herobrine != null) {
			despawn(level, herobrine);
			herobrine = null;
		}

		if (tick == BEHIND) {
			herobrine = spawn(level, new RemotePlayer(level, new GameProfile(STEVE, "Herobrine")), groundSpot(level, player, 1.4, 180.0));
			addEffect(player, MobEffects.DARKNESS, 200);
			sound(level, player.position(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.0F, 0.8F);
		}

		if (tick == BEHIND + 12 || tick == BEHIND + 22) {
			sound(level, player.position(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.2F, 0.9F);
		}

		if (herobrine == null) {
			return;
		}

		face(herobrine, player.getEyePosition());
		glowingEyes(level);

		if (tick > BEHIND + 4 && tick < SCARE) {
			turnTowards(player, herobrine.getEyePosition(), 0.18F);
		}

		if (tick == SCARE) {
			sound(level, player.position(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.0F, 0.6F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	/** Two white dots right in front of the face, where the eyes are on the skin. */
	private void glowingEyes(ClientLevel level) {
		float yaw = herobrine.getYHeadRot() * ((float) Math.PI / 180.0F);
		Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
		Vec3 side = new Vec3(forward.z, 0.0, -forward.x);
		Vec3 center = herobrine.position().add(0.0, 1.72, 0.0).add(forward.scale(0.27));

		for (double offset : new double[] {-0.125, 0.125}) {
			Vec3 eye = center.add(side.scale(offset));
			level.addParticle(EYE, eye.x, eye.y, eye.z, 0.0, 0.0, 0.0);
		}
	}

	/** An id whose default skin is Steve, so nobody's real skin gets loaded for him. */
	private static UUID steveUuid() {
		for (long i = 0; i < 1000; i++) {
			UUID uuid = new UUID(0x4865726F62726E65L, i);

			if (DefaultPlayerSkin.get(uuid).texture().getPath().contains("steve")) {
				return uuid;
			}
		}

		return new UUID(0x4865726F62726E65L, 0L);
	}
}
