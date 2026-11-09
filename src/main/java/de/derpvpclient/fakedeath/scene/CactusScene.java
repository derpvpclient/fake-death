package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Cactus pinball: the player steps back right into a cactus, jumps away from the sting, straight into the next one,
 * bounces off that one into the next, faster and faster, until there are no hearts left.
 */
public class CactusScene extends Scene {
	private static final int STEP_BACK = 18;
	/** The moments the player hits a cactus, each flight shorter than the one before. */
	private static final int[] HITS = {30, 46, 59, 69, 77};
	/** Where the cacti stand, as angles from the view and distances: behind, front right, left, back right, front right again. */
	private static final double[][] SPOTS = {{180.0, 1.3}, {40.0, 2.3}, {-80.0, 2.4}, {140.0, 2.3}};
	private static final int[] ORDER = {0, 1, 2, 3, 1};

	private final BlockPos found;
	private final List<Vec3> cacti = new ArrayList<>();
	private Vec3 look;
	private Vec3 offset = Vec3.ZERO;

	/** @param found a real cactus right next to the player, used as the first one, or null */
	public CactusScene(BlockPos found) {
		this.found = found;
	}

	@Override
	public int length() {
		return HITS[HITS.length - 1] + 25;
	}

	@Override
	public int photoTick() {
		return HITS[2] - 4;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.cactus", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		Vec3 eyes = player.getEyePosition();
		if (tick == 0) {
			look = horizontalLook(player);
			for (int i = 0; i < SPOTS.length; i++) {
				Vec3 at = player.position().add(look.yRot((float) Math.toRadians(SPOTS[i][0])).scale(SPOTS[i][1]));
				BlockPos pos = i == 0 && found != null ? found : grow(level, BlockPos.containing(at));
				cacti.add(pos != null ? Vec3.atBottomCenterOf(pos) : new Vec3(at.x, player.getY(), at.z));
			}
		}

		// Where the camera is: still, a step back, then flying from cactus to cactus.
		Vec3 target;
		if (tick < STEP_BACK) {
			target = Vec3.ZERO;
			look(player, eyes.add(look.scale(5.0)).add(0.0, -0.5, 0.0), 0.1F);
		} else if (tick <= HITS[0]) {
			float p = (tick - STEP_BACK) / (float) (HITS[0] - STEP_BACK);
			target = before(eyes, 0).scale(p * p);
			look(player, eyes.add(look.scale(5.0)).add(0.0, -0.5, 0.0), 0.1F);
		} else {
			int leg = 1;
			while (leg < HITS.length && tick > HITS[leg]) {
				leg++;
			}
			if (leg >= HITS.length) {
				target = offset;
			} else {
				float p = (tick - HITS[leg - 1]) / (float) (HITS[leg] - HITS[leg - 1]);
				Vec3 from = before(eyes, ORDER[leg - 1]);
				Vec3 to = before(eyes, ORDER[leg]);
				// A flat arc through the air, flailing.
				target = from.lerp(to, p).add(0.0, Math.sin(p * Math.PI) * 0.5, 0.0);
				look(player, cacti.get(ORDER[leg]).add(0.0, 1.0, 0.0), 0.45F);
			}
		}
		offset = target;
		moveCamera(offset);

		for (int i = 0; i < HITS.length; i++) {
			if (tick == HITS[i]) {
				Vec3 at = cacti.get(ORDER[i]).add(0.0, 1.0, 0.0);
				particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CACTUS.defaultBlockState()), at, 15, 0.4, 0.15);
				particles(level, ParticleTypes.CRIT, at, 10, 0.3, 0.2);
				// A cartoon boing with every bounce.
				sound(level, at, SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 1.2F, 1.4F + i * 0.1F);
				hit(level, player, SoundEvents.PLAYER_HURT, 0.15F + i * 0.02F, i == HITS.length - 1);
				Looks.swing(player);
			}
		}
	}

	private void look(LocalPlayer player, Vec3 target, float speed) {
		turnTowards(player, target, speed);
	}

	/** The camera offset right in front of a cactus, on the side the player comes from. */
	private Vec3 before(Vec3 eyes, int index) {
		Vec3 cactus = cacti.get(index).add(0.0, 1.0, 0.0);
		Vec3 out = eyes.subtract(cactus);
		out = new Vec3(out.x, 0.0, out.z);
		out = out.lengthSqr() < 1.0E-4 ? look : out.normalize();
		Vec3 spot = cactus.add(out.scale(0.75));
		return new Vec3(spot.x - eyes.x, 0.0, spot.z - eyes.z);
	}

	/** A two block cactus on free ground at or a step up or down from a spot, or null if there is no room. */
	private BlockPos grow(ClientLevel level, BlockPos around) {
		for (int dy : new int[] {0, 1, -1}) {
			BlockPos pos = around.above(dy);
			BlockState below = level.getBlockState(pos.below());
			if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
				&& below.isFaceSturdy(level, pos.below(), Direction.UP) && !onTree(level, pos)) {
				replaceBlock(level, pos, Blocks.CACTUS.defaultBlockState());
				replaceBlock(level, pos.above(), Blocks.CACTUS.defaultBlockState());
				particles(level, ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(pos.above()), 3, 0.3, 0.0);
				return pos;
			}
		}
		return null;
	}
}
