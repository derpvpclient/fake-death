package de.derpvpclient.fakedeath.scene;

import com.mojang.math.Transformation;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The desert comes alive. The sand trembles, and all around the player cacti shoot up out of the ground, a whole ring
 * of them, three blocks tall. Then the ring starts to move: the cacti slide closer, scraping through the sand, tighter
 * and tighter, every touch a sting, until they press together right on the player.
 */
public class CactusScene extends Scene {
	private static final int COUNT = 12;
	private static final double START = 4.5;
	private static final double END = 0.55;
	private static final int TREMBLE = 25;
	private static final int GROW_EVERY = 3;
	private static final int GROW = 10;
	private static final int CLOSE = TREMBLE + COUNT * GROW_EVERY + GROW + 15;
	private static final int CLOSE_TICKS = 80;
	private static final double[] STINGS = {1.6, 1.15, 0.85};

	private final List<Column> columns = new ArrayList<>();
	private Vec3 center;
	private BlockState ground;
	private int stung;

	@Override
	public int length() {
		return CLOSE + CLOSE_TICKS + 2;
	}

	@Override
	public int photoTick() {
		return CLOSE + CLOSE_TICKS / 2;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.kaktus", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			center = player.position();
			ground = level.getBlockState(player.blockPosition().below());
			if (ground.isAir()) {
				ground = Blocks.SAND.defaultBlockState();
			}
		}

		if (tick < TREMBLE) {
			// The sand trembles all around.
			cameraShake(0.04F);
			if (tick % 5 == 0) {
				sound(level, center, SoundEvents.SAND_STEP, SoundSource.BLOCKS, 1.0F, 0.6F);
				particles(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), center.add(0.0, 0.05, 0.0), 10, START, 0.05);
			}
		}

		int growing = tick - TREMBLE;
		if (growing >= 0 && growing % GROW_EVERY == 0 && growing / GROW_EVERY < COUNT) {
			sprout(level, player, growing / GROW_EVERY, tick);
		}

		double radius = START;
		if (tick >= CLOSE) {
			float p = Math.min(1.0F, (tick - CLOSE) / (float) CLOSE_TICKS);
			// Slow at first, then faster and faster.
			radius = START + (END - START) * p * p;
			if (tick % 6 == 0) {
				sound(level, center, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 1.2F, 0.5F + p * 0.4F);
			}
			while (stung < STINGS.length && radius <= STINGS[stung]) {
				stung++;
				sound(level, center, SoundEvents.SWEET_BERRY_BUSH_BREAK, SoundSource.BLOCKS, 1.5F, 0.8F);
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.25F);
				cameraShake(0.2F);
			}
		}

		for (Column column : columns) {
			move(level, column, tick, radius);
		}

		// Looking around at the ring, then at the cacti right in front.
		if (tick < CLOSE + CLOSE_TICKS - 10) {
			float angle = (float) (Math.sin(tick * 0.035) * 1.9);
			Vec3 at = center.add(horizontalLook(player).yRot(angle).scale(radius)).add(0.0, 1.2, 0.0);
			turnTowards(player, at, 0.08F);
		}

		if (tick == CLOSE + CLOSE_TICKS) {
			sound(level, center, SoundEvents.SWEET_BERRY_BUSH_BREAK, SoundSource.BLOCKS, 2.0F, 0.6F);
			sound(level, center, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.8F, 1.6F);
			particles(level, ParticleTypes.CRIT, player.getEyePosition(), 20, 0.4, 0.3);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	/** One cactus column, three blocks, pushed up out of the sand. */
	private void sprout(ClientLevel level, LocalPlayer player, int index, int tick) {
		float angle = index * Mth.TWO_PI / COUNT + (random.nextFloat() - 0.5F) * 0.2F;
		int height = 2 + random.nextInt(2);
		List<Display.BlockDisplay> blocks = new ArrayList<>();
		for (int i = 0; i < height; i++) {
			blocks.add(blockDisplay(level, Blocks.CACTUS.defaultBlockState(), center, 6.0F));
		}
		columns.add(new Column(blocks, angle, tick));
		Vec3 at = center.add(Mth.cos(angle) * START, 0.0, Mth.sin(angle) * START);
		particles(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), at.add(0.0, 0.1, 0.0), 16, 0.4, 0.2);
		sound(level, at, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 1.2F, 0.7F + random.nextFloat() * 0.3F);
	}

	private void move(ClientLevel level, Column column, int tick, double radius) {
		float rise = Math.min(1.0F, (tick - column.start()) / (float) GROW);
		// Overshooting a little, then settling, like it bursts out of the sand.
		double up = rise < 1.0F ? Mth.sin(rise * Mth.HALF_PI) * 1.08 : 1.0;
		double x = center.x + Mth.cos(column.angle()) * radius;
		double z = center.z + Mth.sin(column.angle()) * radius;
		double floor = floor(level, x, z, center.y);
		int height = column.blocks().size();
		for (int i = 0; i < height; i++) {
			// The column comes out of the ground block by block; below the floor it is hidden.
			double y = floor - height + up * height + i;
			Display.BlockDisplay block = column.blocks().get(i);
			float shown = (float) Mth.clamp(y + 1.0 - floor, 0.0, 1.0);
			block.setPos(x, floor, z);
			transform(block, new Transformation(new Vector3f(-0.5F, (float) (y - floor + 1.0 - shown), -0.5F), new Quaternionf(),
				new Vector3f(1.0F, shown, 1.0F), new Quaternionf()));
		}
	}

	private record Column(List<Display.BlockDisplay> blocks, float angle, int start) {
	}
}
