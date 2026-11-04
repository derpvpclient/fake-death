package de.derpvpclient.fakedeath.scene;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * The game seems to break: the screen flickers in wrong colors, sounds stutter, error messages show up in
 * chat, the world around falls apart block by block, and in the end the player falls out of the world.
 * Flashes fast, so it only plays with flashing effects switched on in the settings.
 */
public class GlitchScene extends Scene {
	private static final int[] COLORS = {0xFF00FF, 0x00FFFF, 0xFF2020, 0x20FF20, 0xFFFFFF, 0x000000};
	private static final int ERROR_1 = 40;
	private static final int ERROR_2 = 110;
	private static final int ERROR_3 = 170;
	private static final int TEAR = 60;
	private static final int FALL = 200;
	private static final int FALL_TICKS = 30;
	private static final int DEATH = FALL + FALL_TICKS;

	private int tint;

	@Override
	public int length() {
		return DEATH + 5;
	}

	@Override
	public int photoTick() {
		return ERROR_2 + 10;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.outOfWorld", player.getDisplayName());
	}

	@Override
	public int screenTint() {
		return tint;
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		// How broken everything is, from a flicker now and then to all the time.
		float chaos = Math.min(1.0F, tick / (float) FALL);
		boolean flicker = tick < FALL && random.nextFloat() < 0.08F + chaos * 0.6F;

		if (tick >= FALL) {
			// Gone through the floor: down into the dark.
			float t = Math.min(1.0F, (tick - FALL) / (float) FALL_TICKS);
			moveCamera(new Vec3(0.0, -8.0 * t * t, 0.0));
			tint = Math.min(255, (int) (t * 255)) << 24;
		} else {
			tint = flicker ? (60 + random.nextInt(120)) << 24 | COLORS[random.nextInt(COLORS.length)] : 0;
		}

		if (flicker) {
			// Stuttering sound and a jumping view.
			sound(level, player.getEyePosition(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.MASTER, 0.8F, 0.5F + random.nextFloat() * 1.5F);
			Vec3 jump = player.getEyePosition().add(horizontalLook(player).scale(3.0)).add((random.nextDouble() - 0.5) * 2.0 * chaos, (random.nextDouble() - 0.5) * chaos, 0.0);
			turnTowards(player, jump, 0.4F);
		}

		if (tick % 20 == 0 && tick > 0 && tick < FALL) {
			sound(level, player.getEyePosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.MASTER, 0.6F, 0.4F + random.nextFloat() * 0.4F);
		}

		if (tick == ERROR_1) {
			error(minecraft, Component.translatable("fakedeath.glitch.chunk", player.getBlockX() >> 4, player.getBlockZ() >> 4));
		}
		if (tick == ERROR_2) {
			error(minecraft, Component.translatable("fakedeath.glitch.null", player.getName()));
		}
		if (tick == ERROR_3) {
			minecraft.gui.getChat().addServerSystemMessage(Component.translatable("fakedeath.glitch.broken").withStyle(ChatFormatting.OBFUSCATED, ChatFormatting.DARK_PURPLE));
		}

		if (tick >= TEAR && tick < FALL && tick % 3 == 0) {
			tear(level, player, chaos);
		}

		if (tick == FALL) {
			sound(level, player.position(), SoundEvents.PORTAL_TRIGGER, SoundSource.MASTER, 1.0F, 2.0F);
			particles(level, ParticleTypes.REVERSE_PORTAL, player.position(), 60, 1.0, 0.2);
		}

		if (tick == DEATH) {
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	/** The world falls apart around the player. Never the blocks they stand on, the real player must not fall. */
	private void tear(ClientLevel level, LocalPlayer player, float chaos) {
		BlockPos feet = player.blockPosition();
		int radius = 2 + (int) (chaos * 6);

		for (int i = 0; i < 1 + (int) (chaos * 4); i++) {
			BlockPos pos = feet.offset(random.nextInt(radius * 2 + 1) - radius, random.nextInt(5) - 2, random.nextInt(radius * 2 + 1) - radius);
			boolean underPlayer = Math.abs(pos.getX() - feet.getX()) <= 1 && Math.abs(pos.getZ() - feet.getZ()) <= 1 && pos.getY() <= feet.getY() + 2;

			if (!underPlayer && !level.getBlockState(pos).isAir()) {
				level.addDestroyBlockEffect(pos, level.getBlockState(pos));
				hideBlock(level, pos);
			}
		}
	}

	private static void error(Minecraft minecraft, Component text) {
		minecraft.gui.getChat().addServerSystemMessage(text.copy().withStyle(ChatFormatting.RED));
	}
}
