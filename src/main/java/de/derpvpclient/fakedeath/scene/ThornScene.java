package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Berry bushes sprout in a ring around the player and grow closer and closer. Every twitch stings, and there is no way out. */
public class ThornScene extends Scene {
	private static final int[] RINGS = {3, 2, 1};
	private static final int RING_EVERY = 35;
	private static final int[] STINGS = {95, 115, 135};
	private static final int DEATH = 150;
	private final List<BlockPos> grown = new ArrayList<>();

	@Override
	public int length() {
		return DEATH + 20;
	}

	@Override
	public int photoTick() {
		return STINGS[0] + 5;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.dornen", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		BlockPos feet = player.blockPosition();
		for (int r = 0; r < RINGS.length; r++) {
			if (tick == 5 + r * RING_EVERY) {
				ring(level, feet, RINGS[r]);
				sound(level, player.position(), SoundEvents.SWEET_BERRY_BUSH_PLACE, SoundSource.BLOCKS, 1.5F, 0.8F);
			}
		}
		// Every bush keeps growing until it is full of thorns.
		if (tick % 10 == 0) {
			for (BlockPos pos : grown) {
				BlockState state = level.getBlockState(pos);
				if (state.is(Blocks.SWEET_BERRY_BUSH) && state.getValue(SweetBerryBushBlock.AGE) < 3) {
					replaceBlock(level, pos, state.setValue(SweetBerryBushBlock.AGE, state.getValue(SweetBerryBushBlock.AGE) + 1));
				}
			}
		}
		turnTowards(player, player.getEyePosition().add(horizontalLook(player).yRot(tick * 0.015F).scale(3.0)).add(0.0, -1.2, 0.0), 0.05F);
		for (int sting : STINGS) {
			if (tick == sting) {
				particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SWEET_BERRY_BUSH.defaultBlockState()), player.position().add(0.0, 0.5, 0.0), 15, 0.5, 0.1);
				hurt(level, player, SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, 0.3F);
			}
		}
		if (tick == DEATH) {
			kill(level, player, SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH);
		}
	}

	private void ring(ClientLevel level, BlockPos feet, int radius) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
					continue;
				}
				// On the ground next to the player, wherever there is room.
				for (int dy : new int[] {0, 1, -1}) {
					BlockPos pos = feet.offset(dx, dy, dz);
					if (level.getBlockState(pos).isAir() && !level.getBlockState(pos.below()).isAir() && !onTree(level, pos)
						&& placeBlock(level, pos, Blocks.SWEET_BERRY_BUSH.defaultBlockState())) {
						grown.add(pos);
						particles(level, ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(pos), 2, 0.3, 0.0);
						break;
					}
				}
			}
		}
	}
}
