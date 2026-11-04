package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** An enderman takes the block under the player. Then the next one, and the next, until there is nothing left but the fall. */
public class EndermanScene extends Scene {
	private static final int APPEAR = 15;
	private static final int[] TAKES = {50, 80, 110};
	private static final int DEPTH = 10;
	private static final int FALL = 150;
	private static final int DEATH = FALL + 22;
	private EnderMan enderman;
	private BlockPos below;
	private int taken;

	@Override
	public int length() {
		return DEATH + 20;
	}

	@Override
	public int photoTick() {
		return TAKES[1] + 5;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.enderman", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			below = player.blockPosition().below();
		}
		if (tick == APPEAR) {
			Vec3 at = groundSpot(level, player, 2.2, 0.0);
			enderman = spawn(level, EntityTypes.ENDERMAN, at);
			enderman.noPhysics = true;
			teleportEffect(level, at);
		}
		if (enderman == null) {
			return;
		}

		face(enderman, player.getEyePosition());
		turnTowards(player, enderman.getEyePosition(), 0.15F);
		if (tick % 25 == 0) {
			sound(level, enderman.position(), SoundEvents.ENDERMAN_AMBIENT, SoundSource.HOSTILE, 1.0F, 1.0F);
		}

		for (int i = 0; i < TAKES.length; i++) {
			if (tick == TAKES[i]) {
				BlockPos pos = below.below(i);
				BlockState state = level.getBlockState(pos);
				enderman.setCarriedBlock(state.isAir() ? Blocks.DIRT.defaultBlockState() : state);
				if (i == 0) {
					// The block under the feet becomes an invisible barrier, so the player really keeps standing.
					replaceBlock(level, pos, Blocks.BARRIER.defaultBlockState());
				} else {
					hideBlock(level, pos);
				}
				taken = i + 1;
				sound(level, Vec3.atCenterOf(pos), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.2F);
				particles(level, ParticleTypes.PORTAL, Vec3.atCenterOf(pos), 30, 0.5, 0.5);
				moveCamera(new Vec3(0.0, -taken, 0.0));
			}
			if (tick == TAKES[i] + 12) {
				enderman.setCarriedBlock(null);
				// Next to the new edge of the hole.
				Vec3 at = enderman.position().add(0.0, -1.0, 0.0);
				enderman.setPos(at.x, at.y, at.z);
				teleportEffect(level, at);
			}
		}

		if (tick == FALL - 10) {
			sound(level, enderman.position(), SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 1.5F, 1.0F);
			for (int i = TAKES.length; i <= DEPTH; i++) {
				hideBlock(level, below.below(i));
			}
		}
		if (tick >= FALL && tick <= DEATH) {
			float p = (tick - FALL) / (float) (DEATH - FALL);
			moveCamera(new Vec3(0.0, -taken - p * p * (DEPTH - taken), 0.0));
		}
		if (tick == DEATH) {
			sound(level, player.position(), SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 1.5F, 1.0F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private static void teleportEffect(ClientLevel level, Vec3 at) {
		sound(level, at, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
		particles(level, ParticleTypes.PORTAL, at.add(0.0, 1.5, 0.0), 40, 0.6, 0.8);
	}
}
