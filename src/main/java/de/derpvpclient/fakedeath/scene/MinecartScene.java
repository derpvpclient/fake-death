package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

/**
 * A minecart with nobody in it comes down the rails, slow at first and then faster and faster. Whoever stands on the
 * track is run over, whoever stands next to it is too: the cart jumps the rails on the last blocks.
 */
public class MinecartScene extends Scene {
	private static final int HIT = 84;
	private static final double FAR = 30.0;
	private static final double END_SPEED = 2.0 * FAR / HIT;
	private final BlockPos found;
	private Vec3 point;
	private Vec3 from;
	private Entity cart;
	private boolean down;

	public MinecartScene(BlockPos rail) {
		this.found = rail;
	}

	@Override
	public int length() {
		return HIT + 25;
	}

	@Override
	public int photoTick() {
		return HIT - 12;
	}

	@Override
	public float lying() {
		return down ? 1.0F : 0.0F;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.lore", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			prepare(level, player);
			cart = spawn(level, EntityTypes.MINECART, onTrack(level, FAR));
		}

		// Slow at first, at full speed when it arrives.
		float p = Math.min(1.0F, tick / (float) HIT);
		double s = tick <= HIT ? FAR * (1.0 - p * p) : -END_SPEED * (tick - HIT);
		Vec3 at = onTrack(level, s);

		// Someone next to the track is not safe either: off the rails on the last blocks.
		Vec3 feet = player.position();
		if (tick <= HIT && tick > HIT - 7 && feet.distanceTo(onTrack(level, 0.0)) > 0.6) {
			float jump = (tick - (HIT - 7)) / 7.0F;
			at = at.lerp(feet, jump).add(0.0, Math.sin(jump * Math.PI) * 0.5, 0.0);
		}

		float yaw = (float) Math.toDegrees(Math.atan2(-from.z, -from.x));
		cart.setPos(at.x, at.y, at.z);
		cart.setYRot(yaw);
		cart.yRotO = yaw;

		if (tick <= HIT) {
			turnTowards(player, at.add(0.0, 0.5, 0.0), 0.2F);
			if (tick % 10 == 0) {
				sound(level, at, SoundEvents.MINECART_RIDING, SoundSource.NEUTRAL, 0.4F + p * 2.0F, 0.7F + p * 0.7F);
			}
			if (tick > 25 && tick % 2 == 0) {
				particles(level, ParticleTypes.CRIT, at.add(0.0, 0.1, 0.0), 1 + (int) (p * 4.0F), 0.25, 0.2);
			}
			if (tick > HIT - 30) {
				particles(level, ParticleTypes.SMALL_FLAME, at.add(0.0, 0.05, 0.0), 2, 0.25, 0.02);
				cameraShake((tick - (HIT - 30)) * 0.006F);
			}
		}

		if (tick == HIT) {
			down = true;
			cameraShake(0.0F);
			sound(level, feet, SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 1.5F, 0.7F);
			particles(level, ParticleTypes.CRIT, player.getEyePosition(), 40, 0.5, 0.6);
			particles(level, ParticleTypes.POOF, feet.add(0.0, 0.5, 0.0), 15, 0.4, 0.05);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	/** Finds which way the track runs, picks the side the player looks at, and lays rails where the real ones end, only on this client. */
	private void prepare(ClientLevel level, LocalPlayer player) {
		BlockPos rail = found != null ? found : player.blockPosition();
		Vec3 look = horizontalLook(player);
		int alongX = run(level, rail, 1, 0);
		int alongZ = run(level, rail, 0, 1);
		boolean x = alongX != alongZ ? alongX > alongZ : Math.abs(look.x) >= Math.abs(look.z);
		Vec3 axis = x ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 0.0, 1.0);

		// The cart comes from where the player looks, so they see it coming.
		from = look.dot(axis) >= 0.0 ? axis : axis.scale(-1.0);
		Vec3 start = Vec3.atBottomCenterOf(rail);
		point = start.add(axis.scale(player.position().subtract(start).dot(axis)));

		BlockState track = Blocks.RAIL.defaultBlockState().setValue(BlockStateProperties.RAIL_SHAPE, x ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH);
		for (int k = -18; k <= (int) FAR + 1; k++) {
			Vec3 spot = point.add(from.scale(k));
			BlockPos pos = BlockPos.containing(spot.x, floor(level, spot.x, spot.z, point.y), spot.z);
			if (!level.getBlockState(pos.below()).isAir()) {
				placeBlock(level, pos, track);
			}
		}
	}

	/** How many rails lie in one line through a rail. */
	private static int run(ClientLevel level, BlockPos rail, int dx, int dz) {
		int count = 0;
		for (int sign = -1; sign <= 1; sign += 2) {
			for (int k = 1; k <= 8; k++) {
				BlockPos pos = rail.offset(dx * k * sign, 0, dz * k * sign);
				if (!level.getBlockState(pos).is(BlockTags.RAILS) && !level.getBlockState(pos.above()).is(BlockTags.RAILS)
					&& !level.getBlockState(pos.below()).is(BlockTags.RAILS)) {
					break;
				}
				count++;
			}
		}
		return count;
	}

	private Vec3 onTrack(ClientLevel level, double s) {
		Vec3 pos = point.add(from.scale(s));
		return new Vec3(pos.x, floor(level, pos.x, pos.z, point.y) + 0.0625, pos.z);
	}
}
