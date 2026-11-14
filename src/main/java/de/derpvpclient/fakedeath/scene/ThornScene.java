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
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A puff, a ringing in the ears, and the world grows: the player shrinks to the size of a mouse. Grass towers like
 * trees. Then berry bushes sprout all around, for someone that small they are walls of thorns, and they keep growing,
 * closer and closer, until the thorns are everywhere.
 */
public class ThornScene extends Scene {
	private static final int SHRINK = 30;
	private static final float SMALL = 0.22F;
	private static final int[] RINGS = {3, 2};
	private static final int RING_EVERY = 25;
	private static final int GIANTS = 7;
	private static final int GIANT_START = 60;
	private static final int GIANT_EVERY = 7;
	private static final int GROW = 30;
	private static final int[] STINGS = {125, 145, 162};
	private static final int DEATH = 178;
	private static final float GIANT_SIZE = 1.7F;

	private final List<BlockPos> grown = new ArrayList<>();
	private final List<Giant> giants = new ArrayList<>();
	private float size = 1.0F;

	@Override
	public int length() {
		return DEATH + 1;
	}

	@Override
	public int photoTick() {
		return STINGS[0] + 5;
	}

	@Override
	public float bodyScale() {
		return size;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.dornen", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			sound(level, player.position(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.2F, 1.6F);
			particles(level, ParticleTypes.POOF, player.position().add(0.0, 1.0, 0.0), 20, 0.4, 0.05);
		}
		if (tick <= SHRINK) {
			// Smaller and smaller: the eyes sink down to just above the grass.
			float p = tick / (float) SHRINK;
			float eased = p * p * (3.0F - 2.0F * p);
			size = 1.0F + (SMALL - 1.0F) * eased;
			moveCamera(new Vec3(0.0, -(player.getEyeHeight() * (1.0F - SMALL)) * eased, 0.0));
			if (tick % 6 == 0) {
				sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 2.0F - p);
			}
		}

		BlockPos feet = player.blockPosition();
		for (int r = 0; r < RINGS.length; r++) {
			if (tick == SHRINK + 5 + r * RING_EVERY) {
				ring(level, feet, RINGS[r]);
				sound(level, player.position(), SoundEvents.SWEET_BERRY_BUSH_PLACE, SoundSource.BLOCKS, 1.5F, 0.6F);
			}
		}
		// Every bush keeps growing until it is full of thorns.
		if (tick % 8 == 0) {
			for (BlockPos pos : grown) {
				BlockState state = level.getBlockState(pos);
				if (state.is(Blocks.SWEET_BERRY_BUSH) && state.getValue(SweetBerryBushBlock.AGE) < 3) {
					replaceBlock(level, pos, state.setValue(SweetBerryBushBlock.AGE, state.getValue(SweetBerryBushBlock.AGE) + 1));
				}
			}
		}

		// Right next to the tiny player, thorns as tall as houses push up out of the ground and lean in.
		int since = tick - GIANT_START;
		if (since >= 0 && since % GIANT_EVERY == 0 && since / GIANT_EVERY < GIANTS) {
			int i = since / GIANT_EVERY;
			float angle = i * Mth.TWO_PI / GIANTS + 0.3F;
			Display.BlockDisplay bush = blockDisplay(level, Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3), player.position(), 12.0F);
			giants.add(new Giant(bush, angle, tick));
			sound(level, player.position(), SoundEvents.SWEET_BERRY_BUSH_PLACE, SoundSource.BLOCKS, 2.0F, 0.4F);
		}
		float closing = tick < STINGS[0] ? 0.0F : Math.min(1.0F, (tick - STINGS[0]) / (float) (DEATH - STINGS[0]));
		for (Giant giant : giants) {
			float rise = Math.min(1.0F, (tick - giant.start()) / (float) GROW);
			double radius = 1.15 - closing * 0.7;
			Vec3 at = player.position().add(Mth.cos(giant.angle()) * radius, 0.0, Mth.sin(giant.angle()) * radius);
			float scale = GIANT_SIZE * (0.15F + 0.85F * rise);
			Quaternionf lean = new Quaternionf().rotateY(-giant.angle()).rotateZ(-closing * 0.35F);
			Vector3f offset = new Vector3f(-0.5F * scale, 0.0F, -0.5F * scale).rotate(lean);
			giant.bush().setPos(at.x, at.y, at.z);
			transform(giant.bush(), new Transformation(offset, lean, new Vector3f(scale), new Quaternionf()));
			if (tick == giant.start() + GROW / 2) {
				particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SWEET_BERRY_BUSH.defaultBlockState()), at.add(0.0, 0.3, 0.0), 10, 0.3, 0.1);
			}
		}

		if (tick > SHRINK) {
			// Looking around and up at the thorns towering over the tiny player.
			Vec3 eyes = player.getEyePosition().add(0.0, -(player.getEyeHeight() * (1.0F - SMALL)), 0.0);
			turnTowards(player, eyes.add(horizontalLook(player).yRot(tick * 0.02F).scale(2.0)).add(0.0, 1.2 + closing, 0.0), 0.05F);
		}
		for (int sting : STINGS) {
			if (tick == sting) {
				particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SWEET_BERRY_BUSH.defaultBlockState()), player.position().add(0.0, 0.2, 0.0), 15, 0.3, 0.1);
				hurt(level, player, SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, 0.3F);
				cameraShake(0.05F);
			}
		}
		if (tick == DEATH) {
			sound(level, player.position(), SoundEvents.SWEET_BERRY_BUSH_BREAK, SoundSource.BLOCKS, 2.0F, 0.5F);
			kill(level, player, SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH);
		}
	}

	private void ring(ClientLevel level, BlockPos feet, int radius) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
					continue;
				}
				// On the ground around the player, wherever there is room.
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

	private record Giant(Display.BlockDisplay bush, float angle, int start) {
	}
}
