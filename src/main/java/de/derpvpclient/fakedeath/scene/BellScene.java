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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The player rings the village bell. The whole village panics and runs for the houses, right over the player, the
 * small ones too. The iron golem comes last.
 */
public class BellScene extends Scene {
	private static final int RUN = 56;
	private static final int VILLAGERS = 9;
	private static final double SPEED = 0.27;
	private static final double FIRST = 17.0;
	private static final double GAP = 1.7;
	private final BlockPos found;
	private final List<LivingEntity> crowd = new ArrayList<>();
	private final double[] offsets = new double[VILLAGERS + 1];
	private final boolean[] passed = new boolean[VILLAGERS + 1];
	private BlockPos bell;
	private Vec3 spot;
	private Vec3 from;
	private Vec3 side;
	private boolean down;

	public BellScene(BlockPos bell) {
		this.found = bell;
	}

	@Override
	public int length() {
		return RUN + (int) Math.ceil(distance(VILLAGERS) / SPEED) + 25;
	}

	@Override
	public int photoTick() {
		return RUN + (int) (FIRST / SPEED) - 9;
	}

	@Override
	public float lying() {
		return down ? 1.0F : 0.0F;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.glocke", player.getDisplayName());
	}

	/** How far away each one starts, one behind the other. The golem is the last and has some room. */
	private static double distance(int i) {
		return FIRST + i * GAP + (i == VILLAGERS ? 2.5 : 0.0);
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			bell = found;
			if (bell == null) {
				// Asked for by name with no bell around: one is put up in front, only on this client.
				bell = BlockPos.containing(groundSpot(level, player, 2.0, 0.0));
				placeBlock(level, bell, Blocks.BELL.defaultBlockState());
			}

			spot = player.position();
			Vec3 toBell = Vec3.atBottomCenterOf(bell).subtract(spot);
			Vec3 flat = new Vec3(toBell.x, 0.0, toBell.z);
			from = flat.lengthSqr() < 0.05 ? horizontalLook(player) : flat.normalize();
			side = new Vec3(-from.z, 0.0, from.x);
		}

		Vec3 bellCenter = Vec3.atCenterOf(bell);

		if (tick == 6 || tick == 24 || tick == 42) {
			Looks.swing(player);
			BlockEntity entity = level.getBlockEntity(bell);
			if (entity != null) {
				// The same event the server sends when a bell is rung: it swings.
				entity.triggerEvent(1, player.getDirection().get3DDataValue());
			}
			sound(level, bellCenter, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, 1.0F);
		}

		if (tick < RUN) {
			turnTowards(player, bellCenter, 0.2F);
			return;
		}

		if (tick == RUN) {
			for (int i = 0; i <= VILLAGERS; i++) {
				offsets[i] = i == VILLAGERS ? 0.0 : (random.nextDouble() - 0.5) * 7.0;
				Vec3 start = at(level, i, distance(i));

				if (i == VILLAGERS) {
					var golem = spawn(level, EntityType.IRON_GOLEM, start);
					golem.noPhysics = true;
					crowd.add(golem);
				} else {
					var villager = spawn(level, EntityType.VILLAGER, start);
					villager.noPhysics = true;
					if (i % 4 == 2) {
						villager.setBaby(true);
					}
					crowd.add(villager);
				}
			}
			sound(level, spot.add(from.scale(8.0)), SoundEvents.VILLAGER_HURT, SoundSource.NEUTRAL, 2.0F, 1.0F);
		}

		int t = tick - RUN;
		turnTowards(player, spot.add(from.scale(6.0)).add(0.0, down ? 2.2 : 1.2, 0.0), 0.15F);
		// The ground shakes more the closer they are.
		cameraShake(Math.min(0.22F, 0.02F + t * 0.0025F));

		for (int i = 0; i < crowd.size(); i++) {
			LivingEntity runner = crowd.get(i);
			double s = distance(i) - SPEED * t;

			if (s > -16.0) {
				Vec3 pos = at(level, i, s);
				walk(runner, pos);
				face(runner, pos.subtract(from.scale(5.0)).add(0.0, runner.getEyeHeight(), 0.0));

				if (tick % 5 == i % 5) {
					particles(level, ParticleTypes.SPLASH, pos.add(0.0, runner.getBbHeight() + 0.2, 0.0), 2, 0.2, 0.1);
				}
				if (i < VILLAGERS && (tick + i * 3) % 14 == 0) {
					sound(level, pos, random.nextBoolean() ? SoundEvents.VILLAGER_HURT : SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.2F,
						(runner.isBaby() ? 1.6F : 1.0F) + random.nextFloat() * 0.3F);
				}
				if (i == VILLAGERS && t % 9 == 0) {
					sound(level, pos, SoundEvents.IRON_GOLEM_STEP, SoundSource.NEUTRAL, 2.0F, 0.8F);
				}
			}

			if (!passed[i] && s <= 0.4) {
				passed[i] = true;
				down = true;
				particles(level, ParticleTypes.POOF, player.position().add(0.0, 0.3, 0.0), 6, 0.4, 0.02);

				if (i == VILLAGERS) {
					sound(level, player.position(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.NEUTRAL, 2.0F, 0.7F);
					kill(level, player, SoundEvents.PLAYER_HURT);
				} else {
					hurt(level, player, SoundEvents.PLAYER_HURT, 0.1F);
				}
			}
		}
	}

	/** Where one of the crowd is at a distance in front of the player. They come in a wide front and all squeeze over the same spot. */
	private Vec3 at(ClientLevel level, int i, double s) {
		double wide = Math.max(0.08, Math.min(1.0, Math.abs(s) / 8.0));
		Vec3 pos = spot.add(from.scale(s)).add(side.scale(offsets[i] * wide));
		return new Vec3(pos.x, floor(level, pos.x, pos.z, spot.y), pos.z);
	}
}
