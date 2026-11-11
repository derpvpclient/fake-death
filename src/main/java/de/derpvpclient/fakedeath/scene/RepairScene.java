package de.derpvpclient.fakedeath.scene;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The player repairs something on the anvil, three strokes. Then the anvil has had enough of being hit: it hops up,
 * sails over the player's head and repairs them. Also three strokes.
 */
public class RepairScene extends Scene {
	private static final int[] STROKES = {12, 26, 40};
	private static final int WAKE = 52;
	private static final int JUMP = 64;
	private static final int ABOVE = 80;
	private static final int[] SLAMS = {90, 110, 130};
	private static final int FALL = 5;
	private final BlockPos found;
	private BlockPos block;
	private Vec3 top;
	private Display.ItemDisplay tool;
	private Display.BlockDisplay anvil;
	private double sunk;
	private Vec3 ahead = Vec3.ZERO;

	public RepairScene(BlockPos anvil) {
		this.found = anvil;
	}

	@Override
	public int length() {
		return SLAMS[SLAMS.length - 1] + 20;
	}

	@Override
	public int photoTick() {
		return SLAMS[0] - FALL - 2;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.reparatur", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		Vec3 head = player.getEyePosition();

		if (tick == 0) {
			block = found;
			if (block == null) {
				// Asked for by name with no anvil around: one is put up in front, only on this client.
				block = BlockPos.containing(groundSpot(level, player, 2.0, 0.0));
				placeBlock(level, block, Blocks.ANVIL.defaultBlockState());
			}
			top = Vec3.atCenterOf(block).add(0.0, 0.53, 0.0);

			ItemStack held = player.getMainHandItem();
			tool = itemDisplay(level, held.isEmpty() ? new ItemStack(Items.IRON_PICKAXE) : held.copyWithCount(1), top);
			place(tool, top, new Vector3f(0.6F), new Quaternionf().rotateX((float) Math.PI / 2.0F));
		}

		if (tick < WAKE) {
			turnTowards(player, top, 0.25F);
		}

		for (int stroke : STROKES) {
			if (tick == stroke) {
				sound(level, top, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
				particles(level, ParticleTypes.CRIT, top.add(0.0, 0.1, 0.0), 10, 0.2, 0.5);
				particles(level, ParticleTypes.ELECTRIC_SPARK, top.add(0.0, 0.1, 0.0), 6, 0.2, 0.4);
			}
		}

		if (tick == WAKE) {
			// The real anvil goes, a movable one takes its place.
			BlockState state = level.getBlockState(block);
			despawn(level, tool);
			tool = null;
			hideBlock(level, block);
			anvil = blockDisplay(level, state.isAir() ? Blocks.ANVIL.defaultBlockState() : state, Vec3.atCenterOf(block), 6.0F);
			place(anvil, Vec3.atCenterOf(block), 1.0F);
			sound(level, top, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 1.0F, 0.6F);
			ahead = horizontalLook(player);
		}

		if (anvil == null) {
			return;
		}

		Vec3 home = Vec3.atCenterOf(block);
		Vec3 over = head.add(ahead.scale(1.5)).add(0.0, 1.5 - sunk, 0.0);
		Vec3 crown = head.add(ahead.scale(0.35)).add(0.0, 0.7 - sunk, 0.0);
		Vec3 at;
		float tilt = 0.0F;

		if (tick < JUMP) {
			// It shakes itself, like after a long day of being hammered on.
			at = home.add(0.0, Math.abs(Math.sin((tick - WAKE) * 1.1)) * 0.15, 0.0);
			tilt = (float) Math.sin((tick - WAKE) * 1.6) * 0.18F;
			if ((tick - WAKE) % 4 == 2) {
				sound(level, home, SoundEvents.ANVIL_STEP, SoundSource.BLOCKS, 1.5F, 0.8F);
			}
		} else if (tick < ABOVE) {
			// One big hop, up and over the player's head.
			float p = (tick - JUMP) / (float) (ABOVE - JUMP);
			at = home.lerp(over, p).add(0.0, Math.sin(p * Math.PI) * 1.3, 0.0);
			tilt = (float) Math.sin(p * Math.PI) * 0.5F;
		} else {
			at = over;
			for (int slam : SLAMS) {
				int t = tick - (slam - FALL);
				if (t >= 0 && t <= FALL) {
					float p = t / (float) FALL;
					at = over.lerp(crown, p * p);
				} else if (t > FALL && t < FALL + 9) {
					at = crown.lerp(over, (t - FALL) / 9.0F);
				}
			}
			if (tick > SLAMS[SLAMS.length - 1]) {
				at = crown;
			}
		}

		place(anvil, at, new Vector3f(1.0F), new Quaternionf().rotateZ(tilt));
		if (tick >= WAKE) {
			turnTowards(player, tick < ABOVE ? at.add(0.0, -0.3, 0.0) : over.add(0.0, sunk - 0.3, 0.0), tick < ABOVE ? 0.3F : 0.2F);
		}

		if (tick == JUMP) {
			sound(level, home, SoundEvents.ANVIL_BREAK, SoundSource.BLOCKS, 0.8F, 1.4F);
			particles(level, ParticleTypes.POOF, home.add(0.0, -0.4, 0.0), 10, 0.3, 0.03);
		}

		if (tick == ABOVE + 2) {
			say(minecraft, Component.translatable("block.minecraft.anvil").withStyle(ChatFormatting.GRAY), Component.translatable("container.repair"));
		}
		if (tick == SLAMS[1] + 6) {
			say(minecraft, Component.translatable("block.minecraft.anvil").withStyle(ChatFormatting.GRAY), Component.translatable("container.repair.expensive"));
		}

		for (int i = 0; i < SLAMS.length; i++) {
			if (tick == SLAMS[i]) {
				// Every stroke drives the player a bit further into the ground, like a nail.
				sunk += 0.3;
				sound(level, head, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.5F, 0.9F + i * 0.1F);
				particles(level, ParticleTypes.CRIT, head.add(0.0, 0.3, 0.0), 20, 0.4, 0.5);
				particles(level, ParticleTypes.ELECTRIC_SPARK, head.add(0.0, 0.3, 0.0), 10, 0.4, 0.5);
				particles(level, ParticleTypes.POOF, player.position(), 8, 0.4, 0.03);
				hit(level, player, SoundEvents.PLAYER_HURT, 0.34F, i == SLAMS.length - 1);
			}
		}
		moveCamera(new Vec3(0.0, -sunk, 0.0));
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		anvil = null;
		tool = null;
		sunk = 0.0;
		moveCamera(Vec3.ZERO);
		super.end(level, player);
	}
}
