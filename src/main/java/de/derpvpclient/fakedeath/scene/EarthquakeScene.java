package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** The ground rumbles harder and harder, a crack runs across the ground towards the player, opens up under them, and down they go. */
public class EarthquakeScene extends Scene {
	private static final int CRACK = 50;
	private static final int CRACK_LENGTH = 7;
	private static final int CRACK_EVERY = 8;
	private static final int OPEN = CRACK + CRACK_LENGTH * CRACK_EVERY;
	private static final int DEPTH = 10;
	private static final int DEATH = OPEN + 26;
	private Vec3 dir;

	@Override
	public int length() {
		return DEATH + 20;
	}

	@Override
	public int photoTick() {
		return OPEN - 10;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.erdbeben", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			dir = horizontalLook(player);
		}
		if (tick < OPEN) {
			cameraShake(0.05F + tick / (float) OPEN * 0.3F);
			if (tick % 12 == 0) {
				sound(level, player.position(), Looks.CAVE, SoundSource.AMBIENT, 1.5F, 0.4F);
				sound(level, player.position(), SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 0.4F);
			}
			BlockState ground = level.getBlockState(player.blockPosition().below());
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, ground.isAir() ? Blocks.DIRT.defaultBlockState() : ground), player.position(), 3, 3.0, 0.1);
		}
		// The crack runs from ahead straight at the player's feet.
		int since = tick - CRACK;
		if (since >= 0 && since % CRACK_EVERY == 0 && since / CRACK_EVERY < CRACK_LENGTH) {
			int step = CRACK_LENGTH - since / CRACK_EVERY;
			BlockPos column = BlockPos.containing(player.position().add(dir.scale(step))).below();
			Vec3 side = dir.yRot((float) (Math.PI / 2));
			// Three blocks wide in the middle, the crack tears open with chunks flying out.
			int width = step > 1 && step < CRACK_LENGTH - 1 ? 1 : 0;
			for (int s = -width; s <= width; s++) {
				BlockPos part = BlockPos.containing(player.position().add(dir.scale(step)).add(side.scale(s))).below();
				BlockState torn = level.getBlockState(part);
				for (int i = 0; i < 6; i++) {
					hideBlock(level, part.below(i));
				}
				if (!torn.isAir()) {
					for (int i = 0; i < 12; i++) {
						level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, torn), part.getX() + random.nextDouble(), part.getY() + 1.0, part.getZ() + random.nextDouble(), (random.nextDouble() - 0.5) * 0.4, 0.4 + random.nextDouble() * 0.4, (random.nextDouble() - 0.5) * 0.4);
					}
				}
			}
			sound(level, Vec3.atCenterOf(column), SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.5F, 0.6F);
			particles(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, Vec3.atCenterOf(column).add(0.0, 1.0, 0.0), 2, 0.3, 0.02);
		}
		if (tick < OPEN) {
			turnTowards(player, player.position().add(dir.scale(3.5)).add(0.0, -0.5, 0.0), 0.08F);
		}
		if (tick == OPEN) {
			cameraShake(0.0F);
			openShaft(level, player, DEPTH);
			sound(level, player.position(), SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 2.0F, 0.5F);
		}
		if (tick > OPEN && tick <= DEATH) {
			float p = (tick - OPEN) / (float) (DEATH - OPEN);
			moveCamera(new Vec3(0.0, -p * p * (DEPTH - 1), 0.0));
			turnTowards(player, player.getEyePosition().add(0.0, 8.0, 0.0).add(dir), 0.2F);
		}
		if (tick == DEATH) {
			sound(level, player.position(), SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 1.5F, 1.0F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}
}
