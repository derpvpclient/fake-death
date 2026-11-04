package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.EnderMan;

/** A tall dark figure between the trees, a little closer every time you look. */
public class SlendermanScene extends Scene {
	private static final double[] DISTANCES = {18.0, 12.0, 7.0, 3.5, 1.4};
	private static final double[] ANGLES = {25.0, -20.0, 15.0, -8.0, 0.0};
	private static final int STEP = 34;
	private static final int SCARE = STEP * (DISTANCES.length - 1);

	private EnderMan figure;

	@Override
	public int length() {
		return SCARE + 14;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.slenderman", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			addEffect(player, MobEffects.DARKNESS, 300);
			sound(level, player.position(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.6F);
		}

		if (tick % STEP == 0 && tick <= SCARE) {
			int step = tick / STEP;

			if (figure != null) {
				despawn(level, figure);
			}

			// Never seen moving, it is just somewhere else the next moment.
			figure = spawn(level, EntityType.ENDERMAN, groundSpot(level, player, DISTANCES[step], ANGLES[step]));
			sound(level, player.position(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 0.6F + step * 0.2F, 0.7F + step * 0.05F);

			if (step == 2) {
				addEffect(player, MobEffects.NAUSEA, 200);
			}
		}

		if (figure == null) {
			return;
		}

		face(figure, player.getEyePosition());

		if (tick > STEP * 2 && tick < SCARE) {
			turnTowards(player, figure.getEyePosition(), 0.1F);
		}

		if (tick == SCARE) {
			// Open mouth and shaking, like an angry enderman.
			figure.setTarget(player);
			sound(level, figure.position(), SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 1.5F, 0.6F);
			sound(level, figure.position(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.5F, 0.8F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}

		if (tick >= SCARE) {
			turnTowards(player, figure.getEyePosition(), 0.5F);
		}
	}
}
