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
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** The ground glows, cracks fill with lava around the player, and then the volcano erupts right under their feet. */
public class VolcanoScene extends Scene {
	private static final int GLOW = 20;
	private static final int LAVA = 70;
	private static final int ERUPT = 95;
	private static final int[] BURNS = {ERUPT + 10, ERUPT + 28};
	private static final int DEATH = ERUPT + 45;
	private final List<BlockPos> ring = new ArrayList<>();
	private final List<Display.BlockDisplay> bombs = new ArrayList<>();
	private final List<Vec3> bombSpeed = new ArrayList<>();
	private int heat;

	@Override
	public int length() {
		return DEATH + 20;
	}

	@Override
	public int photoTick() {
		return ERUPT + 8;
	}

	@Override
	public int screenTint() {
		return (heat << 24) | 0xFF5000;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.vulkan", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		BlockPos below = player.blockPosition().below();
		if (tick == 0) {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					int d = Math.max(Math.abs(dx), Math.abs(dz));
					if (d == 2 || (d == 1 && random.nextFloat() < 0.4F)) {
						ring.add(below.offset(dx, 0, dz));
					}
				}
			}
		}
		cameraShake(tick < DEATH ? 0.04F + Math.min(0.3F, tick / 400.0F) : 0.0F);
		heat = Math.min(110, tick);
		if (tick % 15 == 0 && tick < DEATH) {
			sound(level, player.position(), SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 1.5F, 0.6F);
			sound(level, player.position(), Looks.CAVE, SoundSource.AMBIENT, 1.0F, 0.4F);
		}
		if (tick == GLOW) {
			for (BlockPos pos : ring) {
				if (!level.getBlockState(pos).isAir()) {
					replaceBlock(level, pos, Blocks.MAGMA_BLOCK.defaultBlockState());
				}
			}
			sound(level, player.position(), SoundEvents.BASALT_BREAK, SoundSource.BLOCKS, 1.5F, 0.6F);
		}
		if (tick == LAVA) {
			for (BlockPos pos : ring) {
				if (level.getBlockState(pos).is(Blocks.MAGMA_BLOCK)) {
					replaceBlock(level, pos, Blocks.LAVA.defaultBlockState());
				}
			}
			sound(level, player.position(), SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 2.0F, 0.6F);
		}
		if (tick > GLOW && tick % 4 == 0) {
			for (BlockPos pos : ring) {
				if (random.nextFloat() < 0.3F) {
					particles(level, tick > LAVA ? ParticleTypes.LAVA : ParticleTypes.SMOKE, Vec3.atCenterOf(pos).add(0.0, 0.6, 0.0), 1, 0.3, 0.05);
				}
			}
		}
		turnTowards(player, player.position().add(horizontalLook(player).scale(2.0)), 0.08F);

		if (tick == ERUPT) {
			replaceBlock(level, below, Blocks.MAGMA_BLOCK.defaultBlockState());
			sound(level, player.position(), Looks.EXPLODE, SoundSource.BLOCKS, 2.0F, 0.5F);
			setOnFire(player);
			for (int i = 0; i < 6; i++) {
				bombs.add(blockDisplay(level, Blocks.MAGMA_BLOCK.defaultBlockState(), player.position(), 4.0F));
				bombSpeed.add(new Vec3((random.nextDouble() - 0.5) * 0.5, 0.7 + random.nextDouble() * 0.4, (random.nextDouble() - 0.5) * 0.5));
			}
		}
		if (tick >= ERUPT && tick < DEATH) {
			// The fountain right under the feet.
			for (int i = 0; i < 6; i++) {
				level.addParticle(ParticleTypes.LAVA, player.getX(), player.getY(), player.getZ(), (random.nextDouble() - 0.5) * 0.4, 0.8, (random.nextDouble() - 0.5) * 0.4);
				level.addParticle(ParticleTypes.FLAME, player.getX(), player.getY() + 0.5, player.getZ(), (random.nextDouble() - 0.5) * 0.2, 0.4, (random.nextDouble() - 0.5) * 0.2);
			}
			particles(level, ParticleTypes.LARGE_SMOKE, player.getEyePosition().add(0.0, 2.0, 0.0), 3, 1.0, 0.1);
			int age = tick - ERUPT;
			for (int i = 0; i < bombs.size(); i++) {
				Vec3 v = bombSpeed.get(i);
				Vec3 at = player.position().add(v.x * age, v.y * age - 0.03 * age * age, v.z * age);
				place(bombs.get(i), at, new Vector3f(0.5F), new Quaternionf().rotateXYZ(age * 0.2F, age * 0.3F, i));
			}
			turnTowards(player, player.getEyePosition().add(0.0, 10.0, 0.0).add(horizontalLook(player)), 0.1F);
		}
		for (int burn : BURNS) {
			if (tick == burn) {
				hurt(level, player, SoundEvents.PLAYER_HURT_ON_FIRE, 0.35F);
			}
		}
		if (tick == DEATH) {
			kill(level, player, SoundEvents.PLAYER_HURT_ON_FIRE);
		}
	}
}
