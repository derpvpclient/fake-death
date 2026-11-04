package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Pistons pop up on all four sides of the player, facing inwards. They push once, twice, and the third time they meet. */
public class PistonScene extends Scene {
	private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
	private static final int[] PUSHES = {55, 85, 115};
	private static final int PUSH = 6;
	private static final double FAR = 1.7;
	private final List<Display.BlockDisplay> bodies = new ArrayList<>();
	private final List<Display.BlockDisplay> heads = new ArrayList<>();
	private Vec3 center;

	@Override
	public int length() {
		return PUSHES[PUSHES.length - 1] + 30;
	}

	@Override
	public int photoTick() {
		return PUSHES[1] + 2;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.kolben", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			center = player.position().add(0.0, 0.5, 0.0);
		}
		for (int i = 0; i < SIDES.length; i++) {
			if (tick == 5 + i * 8) {
				// Out of the ground, one after the other, all pointing at the player.
				Direction facing = SIDES[i].getOpposite();
				Vec3 at = center.add(SIDES[i].getStepX() * FAR, 0.0, SIDES[i].getStepZ() * FAR);
				bodies.add(blockDisplay(level, Blocks.PISTON.defaultBlockState().setValue(BlockStateProperties.FACING, facing), at, 4.0F));
				heads.add(blockDisplay(level, Blocks.PISTON_HEAD.defaultBlockState().setValue(BlockStateProperties.FACING, facing).setValue(PistonHeadBlock.SHORT, true), at, 4.0F));
				sound(level, at, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 1.0F, 0.8F);
			}
		}

		double extend = 0.0;
		for (int push : PUSHES) {
			int t = tick - push;
			if (t >= 0 && t < PUSH * 2) {
				extend = t < PUSH ? t / (double) PUSH : (PUSH * 2 - t) / (double) PUSH;
			}
			if (t == 0) {
				sound(level, center, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.5F, 0.7F);
			}
			if (t == PUSH) {
				hit(level, player, SoundEvents.PLAYER_HURT, 0.35F, push == PUSHES[PUSHES.length - 1]);
				cameraShake(0.3F);
			}
			if (t == PUSH + 2) {
				cameraShake(0.0F);
			}
		}
		if (tick >= PUSHES[PUSHES.length - 1] + PUSH) {
			extend = 1.0;
		}

		for (int i = 0; i < bodies.size(); i++) {
			Vec3 side = new Vec3(SIDES[i].getStepX(), 0.0, SIDES[i].getStepZ());
			Vec3 at = center.add(side.scale(FAR));
			place(bodies.get(i), at, new Vector3f(1.0F), new Quaternionf());
			// The head only shows while it moves out, resting it would flicker inside the piston face.
			place(heads.get(i), at.subtract(side.scale(extend * 0.95)), new Vector3f(extend > 0.02 ? 1.0F : 0.001F), new Quaternionf());
		}
		turnTowards(player, center.add(horizontalLook(player).scale(FAR)), 0.1F);
	}
}
