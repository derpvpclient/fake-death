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
 * The game seems to break: the black and magenta squares of a missing texture spread over the ground from the
 * player's feet, blocks around flicker into it, sounds stutter, error messages show up in chat, the world falls apart
 * block by block, and in the end the player falls through the broken floor out of the world. No flashing colors.
 */
public class GlitchScene extends Scene {
	private static final int ERROR_1 = 40;
	private static final int ERROR_2 = 110;
	private static final int ERROR_3 = 170;
	private static final int TEAR = 60;
	private static final int FALL = 200;
	private static final int FALL_TICKS = 30;
	private static final int DEATH = FALL + FALL_TICKS;

	private int tint;
	private final java.util.Map<BlockPos, net.minecraft.world.entity.Display.ItemDisplay> missing = new java.util.HashMap<>();
	private final java.util.ArrayDeque<BlockPos> frontier = new java.util.ArrayDeque<>();
	private final java.util.List<Flicker> flickers = new java.util.ArrayList<>();

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
		}
		if (tick == 10) {
			frontier.add(player.blockPosition().below());
		}
		if (tick >= 10 && tick < FALL) {
			spread(level, player, chaos);
		}
		if (tick >= 10 && tick < 80 && !flicker) {
			// The view goes down to the ground, where it starts.
			turnTowards(player, player.position().add(horizontalLook(player).scale(2.5)), 0.06F);
		}
		flicker(level, player, tick, chaos);

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

		if (tick >= TEAR + 60 && tick < FALL && tick % 3 == 0) {
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

			if (!underPlayer && !level.getBlockState(pos).isAir() && missing.containsKey(pos)) {
				despawn(level, missing.remove(pos));
			}
			if (!underPlayer && !level.getBlockState(pos).isAir()) {
				level.addDestroyBlockEffect(pos, level.getBlockState(pos));
				hideBlock(level, pos);
			}
		}
	}

	/** The missing texture creeps over the ground, block by block, outwards from the feet, faster and faster. */
	private void spread(ClientLevel level, LocalPlayer player, float chaos) {
		int steps = 2 + (int) (chaos * 10);
		for (int i = 0; i < steps && !frontier.isEmpty(); i++) {
			BlockPos pos = frontier.poll();
			if (missing.containsKey(pos) || pos.distSqr(player.blockPosition()) > 196) {
				continue;
			}
			missing.put(pos, cover(level, pos));
			if (missing.size() % 4 == 0) {
				sound(level, Vec3.atCenterOf(pos), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.MASTER, 0.4F, 0.5F + random.nextFloat());
			}
			for (net.minecraft.core.Direction side : net.minecraft.core.Direction.Plane.HORIZONTAL) {
				for (int dy = -1; dy <= 1; dy++) {
					BlockPos next = pos.relative(side).above(dy);
					if (!missing.containsKey(next) && !level.getBlockState(next).isAir() && level.getBlockState(next.above()).isAir()) {
						frontier.add(next);
						break;
					}
				}
			}
		}
	}

	/** A block wears the missing texture: a cube of it, a hair bigger, laid over the block. */
	private net.minecraft.world.entity.Display.ItemDisplay cover(ClientLevel level, BlockPos pos) {
		net.minecraft.world.entity.Display.ItemDisplay cube = itemDisplay(level, Looks.prop(net.minecraft.world.item.Items.IRON_NUGGET, "missing_block"), Vec3.atCenterOf(pos));
		// Inside the block it would sit in the dark: the missing texture always shines at full brightness, as in the game.
		((de.derpvpclient.fakedeath.mixin.DisplayAccessor) cube).fakedeath$setBrightnessOverride(new net.minecraft.util.Brightness(15, 15));
		place(cube, Vec3.atCenterOf(pos), 1.004F);
		return cube;
	}

	/** Blocks in the walls and around flicker into the missing texture for a moment and back. */
	private void flicker(ClientLevel level, LocalPlayer player, int tick, float chaos) {
		flickers.removeIf(flicker -> {
			if (tick >= flicker.until()) {
				despawn(level, flicker.cube());
				return true;
			}
			return false;
		});
		if (tick < 30 || tick >= FALL || random.nextFloat() > 0.15F + chaos * 0.5F) {
			return;
		}
		BlockPos feet = player.blockPosition();
		BlockPos pos = feet.offset(random.nextInt(13) - 6, random.nextInt(6) - 1, random.nextInt(13) - 6);
		if (!level.getBlockState(pos).isAir() && !missing.containsKey(pos)) {
			flickers.add(new Flicker(cover(level, pos), tick + 3 + random.nextInt(8)));
		}
	}

	private record Flicker(net.minecraft.world.entity.Display.ItemDisplay cube, int until) {
	}

	private static void error(Minecraft minecraft, Component text) {
		minecraft.gui.getChat().addServerSystemMessage(text.copy().withStyle(ChatFormatting.RED));
	}
}
