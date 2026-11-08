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
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * A sat nav voice in the chat. "Turn right in 100 meters. Turn right now. RIGHT!" The view walks off as told, straight
 * into a wall. "Recalculating route …" The next wall. "Please turn around." The last wall. "You have reached your
 * destination."
 */
public class NavScene extends Scene {
	private static final double SPEED = 0.13;
	private static final double EYES = 1.62;
	private static final int BONKS = 3;

	private Vec3 feet;
	private float heading;
	private float shown;
	private boolean walking;
	private Vec3 wall;
	private int bonks;
	private int lastBonk = -1;
	private int end = -1;
	private double bump;

	@Override
	public int length() {
		return end >= 0 ? end : 290;
	}

	@Override
	public boolean walking() {
		return walking;
	}

	@Override
	public int photoTick() {
		return 110;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.navi", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			feet = player.position();
			Vec3 look = horizontalLook(player);
			heading = shown = (float) Mth.atan2(look.z, look.x);
			voice(minecraft, level, "calc");
		}
		if (feet == null) {
			return;
		}

		// The first instructions are on a timer, after that each one follows a bump.
		if (tick == 20) {
			voice(minecraft, level, "100m");
			walking = true;
		}
		if (tick == 60 && bonks == 0) {
			voice(minecraft, level, "now");
			heading += Mth.HALF_PI;
		}
		if (tick == 85 && bonks == 0) {
			voice(minecraft, level, "right");
			heading += Mth.HALF_PI;
			buildWall(level);
		}
		if (lastBonk >= 0) {
			int since = tick - lastBonk;
			if (bonks == 1 && since == 25) {
				voice(minecraft, level, "recalc");
			}
			if (bonks == 1 && since == 45) {
				voice(minecraft, level, "again");
				heading += Mth.HALF_PI;
				buildWall(level);
			}
			if (bonks == 2 && since == 25) {
				voice(minecraft, level, "turn");
				heading += Mth.PI;
				buildWall(level);
			}
			if (bonks == BONKS && since == 25) {
				voice(minecraft, level, "arrived");
				end = tick + 40;
			}
		}

		// The view turns smoothly to the new heading before it walks on.
		shown += Mth.wrapDegrees((heading - shown) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD * 0.25F;
		Vec3 dir = new Vec3(Mth.cos(shown), 0.0, Mth.sin(shown));
		boolean straight = Math.abs(Mth.wrapDegrees((heading - shown) * Mth.RAD_TO_DEG)) < 6.0F;
		if (walking && straight && bonks < BONKS && (lastBonk < 0 || wall != null)) {
			Vec3 next = feet.add(dir.scale(SPEED));
			Vec3 ahead = feet.add(dir.scale(0.45));
			if (solid(level, ahead.add(0.0, 0.6, 0.0)) || solid(level, ahead.add(0.0, EYES, 0.0))) {
				// A real tree or wall in the way: walked right into it.
				bonk(level, player, tick, dir);
			} else {
				feet = new Vec3(next.x, floor(level, next.x, next.z, feet.y), next.z);
			}
			if (tick % 6 == 0) {
				sound(level, feet, SoundEvents.STONE_STEP, SoundSource.PLAYERS, 0.5F, 1.0F);
			}
		}
		if (wall != null && feet.distanceTo(new Vec3(wall.x, feet.y, wall.z)) < 0.75) {
			bonk(level, player, tick, dir);
		}

		bump *= 0.7;
		double bob = walking && straight && wall != null || lastBonk < 0 ? Math.abs(Mth.sin(tick * 0.6F)) * 0.06 : 0.0;
		Vec3 eye = feet.add(0.0, EYES + bob, 0.0).subtract(dir.scale(bump));
		moveCamera(eye.subtract(player.getEyePosition()));
		turnTowards(player, eye.add(dir.scale(5.0)), 0.3F);
	}

	private void bonk(ClientLevel level, LocalPlayer player, int tick, Vec3 dir) {
		bonks++;
		lastBonk = tick;
		Vec3 face = feet.add(dir.scale(0.6)).add(0.0, EYES, 0.0);
		sound(level, face, SoundEvents.STONE_HIT, SoundSource.PLAYERS, 1.5F, 0.6F);
		sound(level, face, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 0.6F);
		particles(level, ParticleTypes.CRIT, face, 20, 0.3, 0.2);
		// Bounced back a step, a little dizzy.
		feet = feet.subtract(dir.scale(0.4));
		bump = 0.3;
		wall = null;
		hit(level, player, SoundEvents.PLAYER_HURT, 0.3F, bonks == BONKS);
	}

	private static boolean solid(ClientLevel level, Vec3 at) {
		BlockPos pos = BlockPos.containing(at);
		return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() || level.getBlockState(pos).is(net.minecraft.tags.BlockTags.LEAVES);
	}

	/** A wall right ahead, three wide and three high, only where there is air. */
	private void buildWall(ClientLevel level) {
		Vec3 dir = new Vec3(Mth.cos(heading), 0.0, Mth.sin(heading));
		Vec3 center = feet.add(dir.scale(2.6));
		Vec3 side = dir.yRot(Mth.HALF_PI);
		double ground = floor(level, center.x, center.z, feet.y);
		for (int w = -1; w <= 1; w++) {
			for (int h = 0; h < 3; h++) {
				Vec3 spot = center.add(side.scale(w)).add(0.0, h, 0.0);
				BlockPos pos = BlockPos.containing(spot.x, ground + h + 0.5, spot.z);
				if (level.getBlockState(pos).isAir()) {
					replaceBlock(level, pos, Blocks.STONE_BRICKS.defaultBlockState());
				}
			}
		}
		wall = Vec3.atCenterOf(BlockPos.containing(center.x, ground, center.z)).subtract(dir.scale(0.5));
	}

	private void voice(Minecraft minecraft, ClientLevel level, String key) {
		sound(level, feet, Looks.PLING, SoundSource.PLAYERS, 0.8F, 1.5F);
		say(minecraft, Component.translatable("fakedeath.navi.name").withStyle(ChatFormatting.AQUA), Component.translatable("fakedeath.navi." + key));
	}
}
