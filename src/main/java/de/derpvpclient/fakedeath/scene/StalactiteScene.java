package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** A pointed dripstone above the player drips, cracks, breaks off and comes down on them. */
public class StalactiteScene extends Scene {
	private static final int CRACKS = 40;
	private static final int FALL = 95;
	private static final int CRACK_ID = -2_100_000;

	/** The dripstone column, the tip first. Empty when testing without one: then it falls from out of sight. */
	private final List<BlockPos> column = new ArrayList<>();
	private final List<FallingBlockEntity> falling = new ArrayList<>();
	private int impact = -1;

	private StalactiteScene(List<BlockPos> column) {
		this.column.addAll(column);
	}

	/** The lowest pointed dripstone right above the player, with the ones hanging above it. */
	public static StalactiteScene above(ClientLevel level, LocalPlayer player) {
		BlockPos head = player.blockPosition().above(2);

		for (int dy = 0; dy < 12; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					BlockPos tip = head.offset(dx, dy, dz);

					if (level.getBlockState(tip).is(Blocks.POINTED_DRIPSTONE)) {
						List<BlockPos> column = new ArrayList<>();

						for (BlockPos pos = tip; column.size() < 6 && level.getBlockState(pos).is(Blocks.POINTED_DRIPSTONE); pos = pos.above()) {
							column.add(pos);
						}

						return new StalactiteScene(column);
					}
				}
			}
		}

		return null;
	}

	public static StalactiteScene anywhere(ClientLevel level, LocalPlayer player) {
		StalactiteScene real = above(level, player);
		return real != null ? real : new StalactiteScene(List.of());
	}

	@Override
	public int length() {
		return impact >= 0 ? impact + 6 : FALL + 80;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.fallingStalactite", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			prepare(level, player);
		}
		Vec3 tip = column.isEmpty() ? player.getEyePosition().add(0, 8, 0) : Vec3.atBottomCenterOf(column.getFirst());

		if (tick < FALL) {
			if (tick % 8 == 0) {
				sound(level, tip, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, SoundSource.BLOCKS, 1.0F, 0.9F + random.nextFloat() * 0.2F);
				level.addParticle(ParticleTypes.DRIPPING_DRIPSTONE_WATER, tip.x, tip.y - 0.1, tip.z, 0.0, 0.0, 0.0);
			}

			turnTowards(player, tip, tick < CRACKS ? 0.04F : 0.1F);
		}

		if (tick >= CRACKS && tick < FALL && !column.isEmpty()) {
			for (int i = 0; i < column.size(); i++) {
				level.destroyBlockProgress(CRACK_ID - i, column.get(i), Math.min(9, (tick - CRACKS) * 10 / (FALL - CRACKS)));
			}

			if (tick % 6 == 0) {
				sound(level, tip, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 1.0F, 1.2F);
			}
		}

		if (tick == FALL) {
			clearCracks(level);
			sound(level, tip, SoundEvents.POINTED_DRIPSTONE_FALL, SoundSource.BLOCKS, 2.0F, 1.0F);

			if (column.isEmpty()) {
				falling.add(fallingBlock(level, Blocks.POINTED_DRIPSTONE.defaultBlockState(), tip, Vec3.ZERO));
			} else {
				for (BlockPos pos : column) {
					falling.add(dropBlock(level, pos, Vec3.ZERO));
				}
			}
		}

		if (tick > FALL && impact < 0 && !falling.isEmpty() && falling.getFirst().getY() <= player.getEyeY()) {
			impact = tick;
			sound(level, player.position(), SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.BLOCKS, 2.0F, 1.0F);
			level.addDestroyBlockEffect(player.blockPosition().above(), Blocks.POINTED_DRIPSTONE.defaultBlockState());
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	/**
	 * In a dark cave nothing could be seen, so a soft light comes up around the player, only on this client. Without a
	 * real dripstone above, one grows out of the ceiling first, so there is something to come down.
	 */
	private void prepare(ClientLevel level, LocalPlayer player) {
		BlockPos head = player.blockPosition().above();
		if (level.getMaxLocalRawBrightness(head) < 8) {
			addEffect(player, net.minecraft.world.effect.MobEffects.NIGHT_VISION, FALL + 100);
		}
		if (!column.isEmpty()) {
			return;
		}
		for (int up = 3; up <= 12; up++) {
			BlockPos ceiling = head.above(up);
			if (!level.getBlockState(ceiling).isAir()) {
				BlockPos base = ceiling.below();
				BlockPos tipPos = base.below();
				if (placeBlock(level, base, Looks.dripstone(false)) && placeBlock(level, tipPos, Looks.dripstone(true))) {
					column.add(tipPos);
					column.add(base);
				}
				return;
			}
		}
	}

	private void clearCracks(ClientLevel level) {
		for (int i = 0; i < column.size(); i++) {
			level.destroyBlockProgress(CRACK_ID - i, column.get(i), -1);
		}
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		clearCracks(level);
		super.end(level, player);
	}
}
