package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.Comparator;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Mining under ground: it rumbles, dust trickles down, cracks run through the ceiling, then the
 * blocks above the player break loose and bury them. Everything is back afterwards.
 */
public class CaveInScene extends Scene {
	private static final int CRACKS = 25;
	private static final int COLLAPSE = 75;
	private static final int BURIED = COLLAPSE + 14;
	private static final int DEATH = BURIED + 12;
	// Ids for the crack overlay, far away from the ids real players break blocks with.
	private static final int CRACK_ID = -2_000_000;

	private final List<BlockPos> ceiling = new ArrayList<>();
	private BlockState rock = Blocks.STONE.defaultBlockState();

	@Override
	public int length() {
		return DEATH;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.fallingBlock", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		Vec3 head = player.getEyePosition();

		if (tick == 0) {
			findCeiling(level, player);
			if (ceiling.isEmpty()) {
				makeCeiling(level, player);
			}
			sound(level, head.add(0, 2, 0), SoundEvents.WARDEN_DIG, SoundSource.BLOCKS, 1.5F, 0.7F);
			sound(level, head, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.8F);
		}

		if (tick < COLLAPSE) {
			trickle(level, player, tick);
		}

		if (tick % 20 == 10 && tick < COLLAPSE) {
			sound(level, head.add(0, 2, 0), SoundEvents.WARDEN_DIG, SoundSource.BLOCKS, 1.0F + tick / 60.0F, 0.6F + random.nextFloat() * 0.2F);
			sound(level, head.add(0, 2, 0), SoundEvents.GRAVEL_FALL, SoundSource.BLOCKS, 1.0F, 0.7F);
		}

		camera(player, tick);

		if (tick >= CRACKS && tick < COLLAPSE) {
			crack(level, player, tick);
		}

		if (tick == COLLAPSE) {
			collapse(level, player);
		}

		if (tick > COLLAPSE && tick < BURIED && tick % 3 == 0) {
			rubble(level, player);
		}

		if (tick == BURIED) {
			sound(level, head, SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 2.0F, 0.5F);
			sound(level, head, SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 1.0F, 0.7F);
			level.addDestroyBlockEffect(BlockPos.containing(head), rock);
			// Buried: everything goes dark.
			addEffect(player, MobEffects.BLINDNESS, 200);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	/**
	 * The view: a nervous look around when it starts to rumble, then up at the ceiling while it cracks, following the
	 * blocks as they come down, and finally pressed down under the rubble. It shakes harder and harder.
	 */
	private void camera(LocalPlayer player, int tick) {
		Vec3 eyes = player.getEyePosition();
		Vec3 look = horizontalLook(player);
		Vec3 up = ceiling.isEmpty() ? eyes.add(0.0, 4.0, 0.0) : Vec3.atBottomCenterOf(ceiling.getFirst());
		Vec3 target;
		float speed;
		if (tick < 18) {
			target = eyes.add(look.yRot((tick / 9) % 2 == 0 ? 0.8F : -0.8F).scale(4.0)).add(0.0, 0.6, 0.0);
			speed = 0.15F;
		} else if (tick < BURIED) {
			// Straight up, a little ahead so the cracks and the falling blocks are right in the middle of the view.
			target = up.add(look.scale(0.6));
			if (thirdPerson()) {
				// In F5 from above instead: looking up would put the camera into the floor, from above the blocks come
				// down onto the player right in the picture.
				target = eyes.add(look.scale(4.0)).add(0.0, -2.3, 0.0);
			}
			speed = tick < COLLAPSE ? 0.12F : 0.3F;
		} else {
			target = eyes.add(look.scale(2.0)).add(0.0, -2.0, 0.0);
			speed = 0.4F;
		}
		turnTowards(player, target, speed);

		float shake = tick < COLLAPSE ? 0.02F + 0.1F * tick / COLLAPSE : tick < BURIED ? 0.25F : 0.0F;
		Vec3 offset = new Vec3((random.nextFloat() - 0.5F) * shake, (random.nextFloat() - 0.5F) * shake, (random.nextFloat() - 0.5F) * shake);
		if (tick >= BURIED - 4) {
			// Knocked down under the rubble.
			offset = offset.add(0.0, -0.9 * Math.min(1.0, (tick - (BURIED - 4)) / 4.0), 0.0);
		}
		moveCamera(offset);
	}

	/** Under an open sky or a very high cave there is no ceiling close enough, so one is put above the player. */
	private void makeCeiling(ClientLevel level, LocalPlayer player) {
		BlockPos top = player.blockPosition().above(4);
		rock = player.getY() < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos pos = top.offset(dx, 0, dz);
				if (level.getBlockState(pos).isAir()) {
					replaceBlock(level, pos, rock);
					ceiling.add(pos);
				}
			}
		}
		ceiling.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(player.getX(), pos.getY() + 0.5, player.getZ())));
	}

	/** The blocks right above the player that will come down, the middle one first. */
	private void findCeiling(ClientLevel level, LocalPlayer player) {
		BlockPos feet = player.blockPosition();

		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				for (int dy = 2; dy <= 10; dy++) {
					BlockPos pos = feet.offset(dx, dy, dz);
					BlockState state = level.getBlockState(pos);

					if (!state.isAir()) {
						if (state.isSolid() && !state.hasBlockEntity()) {
							ceiling.add(pos);
						}
						break;
					}
				}
			}
		}

		ceiling.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(player.getX(), pos.getY() + 0.5, player.getZ())));

		if (!ceiling.isEmpty()) {
			rock = level.getBlockState(ceiling.getFirst());
		}
	}

	private void trickle(ClientLevel level, LocalPlayer player, int tick) {
		BlockParticleOption dust = new BlockParticleOption(ParticleTypes.FALLING_DUST, rock);
		int amount = 1 + tick / 25;

		for (int i = 0; i < amount; i++) {
			Vec3 from = ceiling.isEmpty()
				? player.getEyePosition().add((random.nextDouble() - 0.5) * 3, 1.5, (random.nextDouble() - 0.5) * 3)
				: Vec3.atBottomCenterOf(ceiling.get(random.nextInt(ceiling.size()))).add((random.nextDouble() - 0.5) * 0.9, -0.05, (random.nextDouble() - 0.5) * 0.9);
			level.addParticle(dust, from.x, from.y, from.z, 0.0, 0.0, 0.0);
		}
	}

	/** Cracks spread from the middle of the ceiling outwards. */
	private void crack(ClientLevel level, LocalPlayer player, int tick) {
		for (int i = 0; i < ceiling.size(); i++) {
			int start = CRACKS + i * 4;

			if (tick >= start) {
				int progress = Math.min(9, (tick - start) * 10 / (COLLAPSE - CRACKS - 8));
				level.destroyBlockProgress(CRACK_ID - i, ceiling.get(i), progress);
			}
		}

		if (tick % 6 == 0 && !ceiling.isEmpty()) {
			sound(level, Vec3.atCenterOf(ceiling.get(random.nextInt(ceiling.size()))), SoundEvents.STONE_HIT, SoundSource.BLOCKS, 1.2F, 0.5F + random.nextFloat() * 0.3F);
		}

	}

	private void collapse(ClientLevel level, LocalPlayer player) {
		clearCracks(level);

		for (BlockPos pos : ceiling) {
			level.addDestroyBlockEffect(pos, level.getBlockState(pos));
			dropBlock(level, pos, new Vec3(0.0, -0.15, 0.0));
		}

		Vec3 above = player.getEyePosition().add(0, 2, 0);
		sound(level, above, SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 2.0F, 0.6F);
		sound(level, above, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 2.0F, 0.5F);
		sound(level, above, SoundEvents.GRAVEL_FALL, SoundSource.BLOCKS, 2.0F, 0.6F);
		hurt(level, player, SoundEvents.PLAYER_HURT, 0.35F);
	}

	/** More gravel and cobblestone coming down after the ceiling. */
	private void rubble(ClientLevel level, LocalPlayer player) {
		BlockState piece = random.nextInt(3) == 0 ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
		Vec3 spot = new Vec3(player.getX() + (random.nextDouble() - 0.5) * 2.4, player.getY() + 2.2, player.getZ() + (random.nextDouble() - 0.5) * 2.4);

		if (level.getBlockState(BlockPos.containing(spot)).isAir()) {
			fallingBlock(level, piece, spot, Vec3.ZERO);
		}

		sound(level, spot, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 1.0F, 0.7F + random.nextFloat() * 0.3F);
		player.animateHurt(random.nextFloat() * 360.0F);
	}

	private void clearCracks(ClientLevel level) {
		for (int i = 0; i < ceiling.size(); i++) {
			level.destroyBlockProgress(CRACK_ID - i, ceiling.get(i), -1);
		}
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		clearCracks(level);
		super.end(level, player);
	}
}
