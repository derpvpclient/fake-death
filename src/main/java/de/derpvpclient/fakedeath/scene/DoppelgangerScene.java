package de.derpvpclient.fakedeath.scene;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.phys.Vec3;

/**
 * Footsteps behind the player that are not theirs. A message in chat in their own name. Then they
 * stand there: same skin, same armor, same item in hand. And it comes closer.
 */
public class DoppelgangerScene extends Scene {
	private static final int WARNING = 45;
	private static final int APPEAR = 85;
	private static final int TOO_LATE = 150;
	private static final int CLOSE = 165;
	private static final int[] HITS = {180, 196, 212};

	private RemotePlayer copy;

	@Override
	public int length() {
		return HITS[HITS.length - 1] + 8;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.doppelganger", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		// Its own footsteps, always a little behind.
		if (tick < APPEAR && tick % 11 == 0) {
			sound(level, player.position().add(horizontalLook(player).scale(-3.0)), SoundEvents.STONE_STEP, SoundSource.PLAYERS, 0.5F, 0.9F);
		}

		if (tick == WARNING) {
			say(minecraft, player.getDisplayName(), Component.translatable("fakedeath.doppel.dont_turn"));
		}

		if (tick == APPEAR) {
			copy = spawn(level, mirror(level, player), groundSpot(level, player, 6.0, 180.0));
			sound(level, player.position(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.7F);
		}

		if (copy == null) {
			return;
		}

		face(copy, player.getEyePosition());

		if (tick < TOO_LATE) {
			turnTowards(player, copy.getEyePosition(), 0.07F);
		}

		if (tick == TOO_LATE) {
			say(minecraft, player.getDisplayName(), Component.translatable("fakedeath.doppel.too_late"));
			addEffect(player, MobEffects.DARKNESS, 200);
		}

		if (tick == CLOSE) {
			// Never seen moving: suddenly it is right there.
			Vec3 close = groundSpot(level, player, 1.3, 180.0);
			copy.setPos(close.x, close.y, close.z);
			particles(level, ParticleTypes.LARGE_SMOKE, close.add(0, 1, 0), 10, 0.3, 0.05);
			sound(level, close, SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.0F, 1.4F);
		}

		if (tick > CLOSE) {
			turnTowards(player, copy.getEyePosition(), 0.4F);
		}

		for (int hit : HITS) {
			if (tick == hit) {
				copy.swing(InteractionHand.MAIN_HAND);
				hit(level, player, SoundEvents.PLAYER_HURT, 0.3F, hit == HITS[HITS.length - 1]);
			}
		}
	}

	/** A copy of the player: their skin, their armor and what they hold. With its own id, so it never gets mixed up with them. */
	private static RemotePlayer mirror(ClientLevel level, LocalPlayer player) {
		GameProfile profile = new GameProfile(UUID.randomUUID(), player.getGameProfile().name());
		RemotePlayer copy = new RemotePlayer(level, profile) {
			{
				// The second skin layer (hat, jacket, sleeves) and the main hand, exactly as the player has them set.
				getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, player.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));
				getEntityData().set(DATA_PLAYER_MAIN_HAND, player.getEntityData().get(DATA_PLAYER_MAIN_HAND));
			}

			@Override
			public PlayerSkin getSkin() {
				return player.getSkin();
			}
		};

		for (EquipmentSlot slot : EquipmentSlot.values()) {
			copy.setItemSlot(slot, player.getItemBySlot(slot).copy());
		}

		return copy;
	}
}
