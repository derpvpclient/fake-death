package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The geode starts to ring, softly at first, then louder and louder. Crystals sprout in a ring around the player,
 * grow from buds to clusters, and then the amethyst closes in: feet, head, and the last block on top.
 */
public class AmethystScene extends Scene {
	private static final Block[] STAGES = {Blocks.SMALL_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.LARGE_AMETHYST_BUD, Blocks.AMETHYST_CLUSTER};
	private static final int[][] RING = {{2, 0}, {2, 1}, {1, 2}, {0, 2}, {-1, 2}, {-2, 1}, {-2, 0}, {-2, -1}, {-1, -2}, {0, -2}, {1, -2}, {2, -1}};
	private static final int[][] CLOSE = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}, {1, 1}, {-1, 1}, {-1, -1}, {1, -1}};
	private static final int SPROUT = 18;
	private static final int GROW = 11;
	private static final int FEET = SPROUT + 4 * GROW + 6;
	private static final int HEAD = FEET + 16;
	private static final int LID = HEAD + 16;
	private static final int CRUSH = LID + 14;
	private final BlockPos found;
	private final boolean[] sprouted = new boolean[RING.length];
	private BlockPos feet;
	private Vec3 geode;
	private float tint;

	public AmethystScene(BlockPos amethyst) {
		this.found = amethyst;
	}

	@Override
	public int length() {
		return CRUSH + 20;
	}

	@Override
	public int photoTick() {
		return FEET + 6;
	}

	@Override
	public int screenTint() {
		return tint > 0.0F ? ((int) (Math.min(tint, 1.0F) * 170.0F) << 24) | 0x8A5CD6 : 0;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.amethyst", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		Vec3 head = player.getEyePosition();

		if (tick == 0) {
			feet = player.blockPosition();
			BlockPos block = found;
			if (block == null) {
				// Asked for by name with no amethyst around: a piece of geode is put up in front, only on this client.
				block = BlockPos.containing(groundSpot(level, player, 3.0, 0.0));
				placeBlock(level, block, Blocks.BUDDING_AMETHYST.defaultBlockState());
				placeBlock(level, block.above(), Blocks.AMETHYST_CLUSTER.defaultBlockState());
			}
			geode = Vec3.atCenterOf(block);
		}

		// The ringing: single chimes that come closer together and climb.
		int every = tick < SPROUT ? 6 : tick < FEET ? 4 : 2;
		if (tick < CRUSH && tick % every == 0) {
			float climb = Math.min(1.0F, tick / (float) CRUSH);
			sound(level, tick < FEET ? geode : head, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.5F + climb * 2.0F, 0.6F + climb * 1.2F);
			particles(level, ParticleTypes.END_ROD, geode.add(0.0, 0.8, 0.0), 2, 0.4, 0.02);
		}
		if (tick == 0 || tick == SPROUT || tick == FEET) {
			sound(level, geode, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 3.0F, tick == 0 ? 0.7F : 1.0F);
		}

		if (tick < SPROUT) {
			turnTowards(player, geode, 0.15F);
		} else if (tick < FEET) {
			// The crystals come up all around, one glance for each side.
			float angle = (tick - SPROUT) * 0.11F;
			turnTowards(player, player.position().add(Math.cos(angle) * 2.0, 0.3, Math.sin(angle) * 2.0), 0.14F);
		} else {
			turnTowards(player, head.add(horizontalLook(player).scale(0.4)).add(0.0, tick < LID ? 0.0 : 2.0, 0.0), 0.15F);
		}

		for (int stage = 0; stage < STAGES.length; stage++) {
			if (tick != SPROUT + stage * GROW) {
				continue;
			}
			for (int i = 0; i < RING.length; i++) {
				BlockPos spot = BlockPos.containing(feet.getX() + RING[i][0] + 0.5, floor(level, feet.getX() + RING[i][0] + 0.5, feet.getZ() + RING[i][1] + 0.5, feet.getY()), feet.getZ() + RING[i][1] + 0.5);
				if (stage == 0) {
					sprouted[i] = placeBlock(level, spot, STAGES[0].defaultBlockState());
				} else if (sprouted[i]) {
					replaceBlock(level, spot, STAGES[stage].defaultBlockState());
				}
				if (sprouted[i]) {
					particles(level, ParticleTypes.END_ROD, Vec3.atCenterOf(spot), 2, 0.25, 0.02);
				}
			}
			sound(level, player.position(), SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.BLOCKS, 2.0F, 0.8F + stage * 0.15F);
		}

		if (tick == FEET || tick == HEAD) {
			// Now it closes in: solid amethyst right next to the body, first around the legs, then around the head.
			for (int[] side : CLOSE) {
				placeBlock(level, feet.offset(side[0], tick == FEET ? 0 : 1, side[1]), Blocks.AMETHYST_BLOCK.defaultBlockState());
			}
			sound(level, head, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 2.5F, tick == FEET ? 0.8F : 1.0F);
			particles(level, ParticleTypes.END_ROD, player.position().add(0.0, tick == FEET ? 0.5 : 1.5, 0.0), 30, 0.9, 0.05);
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.25F);
			cameraShake(0.12F);
		}
		if (tick == FEET + 3 || tick == HEAD + 3 || tick == LID + 3) {
			cameraShake(0.0F);
		}

		if (tick > FEET) {
			tint = Math.min(1.0F, (tick - FEET) / (float) (CRUSH - FEET));
		}

		if (tick == LID) {
			placeBlock(level, feet.above(2), Blocks.AMETHYST_BLOCK.defaultBlockState());
			placeBlock(level, feet.above(3), Blocks.AMETHYST_CLUSTER.defaultBlockState());
			sound(level, head, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 2.5F, 1.2F);
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.25F);
			cameraShake(0.12F);
		}

		if (tick == CRUSH) {
			sound(level, head, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 3.0F, 0.6F);
			sound(level, head, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 3.0F, 1.6F);
			particles(level, ParticleTypes.END_ROD, head, 40, 0.4, 0.15);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		moveCamera(Vec3.ZERO);
		super.end(level, player);
	}
}
