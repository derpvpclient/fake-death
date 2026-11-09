package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Pistons pop up out of the ground on all four sides of the player, one after the other, facing inwards. The player
 * turns round to each of them. They push once, twice, and the third time they meet in the middle.
 */
public class PistonScene extends Scene {
	private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
	private static final int RISE_EVERY = 10;
	private static final int[] PUSHES = {60, 90, 120};
	private static final int PUSH = 6;
	private static final double FAR = 1.7;

	private final List<Display.BlockDisplay> bodies = new ArrayList<>();
	private final List<Display.BlockDisplay> heads = new ArrayList<>();
	private final List<Vec3> spots = new ArrayList<>();
	private final List<Integer> order = new ArrayList<>();
	private Vec3 center;
	private boolean extended;

	@Override
	public int length() {
		return PUSHES[PUSHES.length - 1] + 30;
	}

	@Override
	public int photoTick() {
		return PUSHES[1] + 3;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.kolben", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			center = player.position();
			// Front, right, back, left of where the player looks, so turning round to each one is one smooth circle.
			Vec3 look = horizontalLook(player);
			double start = Math.atan2(look.z, look.x);
			for (int k = 0; k < 4; k++) {
				double angle = start + k * Math.PI / 2.0;
				int best = 0;
				for (int i = 1; i < SIDES.length; i++) {
					if (Math.cos(angle - sideAngle(i)) > Math.cos(angle - sideAngle(best))) {
						best = i;
					}
				}
				order.add(best);
			}
			for (Direction side : SIDES) {
				double x = center.x + side.getStepX() * FAR;
				double z = center.z + side.getStepZ() * FAR;
				// Standing on the ground right there, not floating or stuck in a hill.
				spots.add(new Vec3(x, floor(level, x, z, center.y) + 0.5, z));
			}
		}

		for (int k = 0; k < order.size(); k++) {
			if (tick == 5 + k * RISE_EVERY) {
				int i = order.get(k);
				Direction facing = SIDES[i].getOpposite();
				Display.BlockDisplay body = blockDisplay(level, piston(facing, false), spots.get(i), 4.0F);
				Display.BlockDisplay head = blockDisplay(level, Blocks.PISTON_HEAD.defaultBlockState().setValue(BlockStateProperties.FACING, facing).setValue(PistonHeadBlock.SHORT, true), spots.get(i), 4.0F);
				while (bodies.size() <= i) {
					bodies.add(null);
					heads.add(null);
				}
				bodies.set(i, body);
				heads.set(i, head);
				sound(level, spots.get(i), SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 1.0F, 0.8F);
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
			}
		}
		if (tick >= PUSHES[PUSHES.length - 1] + PUSH) {
			extend = 1.0;
		}

		boolean out = extend > 0.02;
		for (int i = 0; i < bodies.size(); i++) {
			Display.BlockDisplay body = bodies.get(i);
			if (body == null) {
				continue;
			}
			int k = order.indexOf(i);
			Vec3 side = new Vec3(SIDES[i].getStepX(), 0.0, SIDES[i].getStepZ());
			double rise = Math.min(1.0, (tick - 5 - k * RISE_EVERY) / 8.0);
			Vec3 at = spots.get(i).add(0.0, (rise - 1.0) * 1.1, 0.0);
			place(body, at, new Vector3f(1.0F), new Quaternionf());
			if (rise < 1.0 && tick % 2 == 0) {
				particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), spots.get(i).add(0.0, -0.4, 0.0), 4, 0.4, 0.2);
			}
			if (out != extended) {
				// Pushed out the base loses its wooden face, which is the head now.
				((de.derpvpclient.fakedeath.mixin.BlockDisplayAccessor) body).fakedeath$setBlockState(piston(SIDES[i].getOpposite(), out));
			}
			// The head only shows while it is out, resting inside the base it would flicker through the wooden face.
			place(heads.get(i), at.subtract(side.scale(extend * 0.95)), new Vector3f(out ? 1.0F : 0.001F), new Quaternionf());
		}
		extended = out;

		camera(player, tick, extend);
	}

	/**
	 * In the player's own eyes: a turn round to each piston as it comes up, then facing the front one. Every push jolts
	 * the view, between the pushes it glances to the sides, and at the end it is squeezed down.
	 */
	private void camera(LocalPlayer player, int tick, double extend) {
		Vec3 eyes = player.getEyePosition();
		Vec3 target;
		float speed = 0.25F;
		int last = 5 + (order.size() - 1) * RISE_EVERY;
		if (order.isEmpty()) {
			target = eyes.add(horizontalLook(player).scale(3.0));
		} else if (tick < last + 12) {
			int k = Math.max(0, Math.min(order.size() - 1, (tick - 3) / RISE_EVERY));
			target = spots.get(order.get(k)).add(0.0, 0.3, 0.0);
		} else if (tick < PUSHES[0] || tick >= PUSHES[PUSHES.length - 1]) {
			target = spots.get(order.getFirst()).add(0.0, 0.5, 0.0);
		} else {
			// Between the pushes: a quick glance to the right, then to the left, and back to the front.
			int since = (tick - PUSHES[0]) % (PUSHES[1] - PUSHES[0]);
			int glance = since > 14 && since < 22 ? 1 : since >= 22 && since < 28 ? 3 : 0;
			target = spots.get(order.get(glance)).add(0.0, 0.4, 0.0);
			speed = 0.3F;
		}
		turnTowards(player, target, speed);

		Vec3 offset = Vec3.ZERO;
		for (int push : PUSHES) {
			int t = tick - push - PUSH;
			if (t >= 0 && t < 4) {
				float shake = 0.25F * (4 - t) / 4.0F;
				offset = new Vec3((random.nextFloat() - 0.5F) * shake, (random.nextFloat() - 0.5F) * shake, (random.nextFloat() - 0.5F) * shake);
			}
		}
		if (tick >= PUSHES[PUSHES.length - 1] + PUSH) {
			// Squeezed: the view sinks.
			double p = Math.min(1.0, (tick - PUSHES[PUSHES.length - 1] - PUSH) / 6.0);
			offset = offset.add(0.0, -0.5 * p, 0.0);
		}
		moveCamera(offset);
	}

	private static BlockState piston(Direction facing, boolean extended) {
		return Blocks.PISTON.defaultBlockState().setValue(BlockStateProperties.FACING, facing).setValue(PistonBaseBlock.EXTENDED, extended);
	}

	private static double sideAngle(int i) {
		return Math.atan2(SIDES[i].getStepZ(), SIDES[i].getStepX());
	}
}
