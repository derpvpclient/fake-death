package de.derpvpclient.fakedeath.scene;

import de.derpvpclient.fakedeath.CameraDirector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Ripe berries right in front of the player. One handful after the other goes into the mouth, every time the player
 * reaches a little deeper into the bushes, until they lose their balance and fall in. Like in the game, every move in
 * there stings, and every try to get back up only makes it worse.
 */
public class BerryScene extends Scene {
	/** A handful from the bush in front, one from the bush behind it, and the last one far inside. */
	private static final int[] PICKS = {18, 60, 98};
	private static final int BURP = 42;
	private static final int FIRST_STING = 62;
	private static final int FALL_START = 102;
	private static final int FALL_END = 110;
	/** Every try to get up again ends with a sting, the last one kills. */
	private static final int[] TRIES = {128, 144, 160};
	private static final int TRY_LENGTH = 12;
	private static final int DEATH = TRIES[TRIES.length - 1];
	private static final int FLIGHT = 9;

	private static final BlockState RIPE = Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3);
	private static final BlockState PICKED = Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 1);

	private final BlockPos found;
	private final List<Berry> berries = new ArrayList<>();
	/** The bush picked in each round. */
	private final BlockPos[] picks = new BlockPos[PICKS.length];
	/** Where the player falls in. */
	private BlockPos bed;
	private Vec3 dir;
	private Vec3 side;
	private Vec3 look;
	/** Where the berries fly to: just under the view towards the bushes, also while the view flinches. */
	private Vec3 eatLook;
	private Vec3 offset = Vec3.ZERO;
	private Vec3 fallFrom = Vec3.ZERO;
	private Vec3 flinchLook = Vec3.ZERO;
	private int stung = -100;
	private int now;

	private static final class Berry {
		final Display.ItemDisplay display;
		final Vec3 from;
		final int start;
		/** The last handful is dropped while falling, those berries just tumble to the ground. */
		final Vec3 dropped;

		Berry(Display.ItemDisplay display, Vec3 from, int start, Vec3 dropped) {
			this.display = display;
			this.from = from;
			this.start = start;
			this.dropped = dropped;
		}
	}

	/** @param found a berry bush right next to the player, or null to grow one in front of them */
	public BerryScene(BlockPos found) {
		this.found = found;
	}

	@Override
	public int length() {
		return DEATH + 20;
	}

	@Override
	public int photoTick() {
		return TRIES[0] - 4;
	}

	@Override
	public float lying() {
		// In third person the body tips over with the fall and lies face down in the thorns.
		return now < FALL_START ? 0.0F : Math.min(1.0F, (now - FALL_START) / (float) (FALL_END - FALL_START));
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.sweetBerryBush", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		now = tick;
		if (tick == 0) {
			setup(level, player);
		}
		Vec3 eyes = player.getEyePosition();

		for (int round = 0; round < PICKS.length; round++) {
			if (tick == PICKS[round]) {
				pick(level, player, round, tick);
			}
		}
		if (tick == BURP) {
			sound(level, eyes, SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.6F, 1.1F);
		}
		if (tick == FIRST_STING) {
			sting(level, player, tick, 0.08F, false);
		}

		if (tick < FALL_START) {
			offset = lean(eyes, tick);
			fallFrom = offset;
			BlockPos bush = tick < 46 ? picks[0] : tick < 88 ? picks[1] : picks[2];
			eatLook = Vec3.atCenterOf(bush).add(0.0, -0.1, 0.0);
			look = tick - stung < 4 ? flinchLook : eatLook;
			turnTowards(player, look, tick - stung < 4 ? 0.35F : 0.18F);
		} else if (tick < FALL_END) {
			// Tipping over: slow at first, then all at once into the thorns.
			float f = (tick - FALL_START) / (float) (FALL_END - FALL_START);
			offset = fallFrom.lerp(lyingOffset(eyes), f * f);
			look = Vec3.atBottomCenterOf(bed).add(dir.scale(1.6));
			turnTowards(player, look, 0.3F);
		} else {
			struggle(level, player, eyes, tick);
		}
		moveCamera(offset);

		if (tick == FALL_END) {
			sound(level, Vec3.atCenterOf(bed), SoundEvents.SWEET_BERRY_BUSH_BREAK, SoundSource.BLOCKS, 1.2F, 0.7F);
			sound(level, Vec3.atCenterOf(bed), SoundEvents.PLAYER_SMALL_FALL, SoundSource.PLAYERS, 1.0F, 0.9F);
			particles(level, leaves(), Vec3.atCenterOf(bed), 30, 0.6, 0.15);
			sting(level, player, tick, 0.22F, false);
		}
		for (int t : TRIES) {
			if (tick == t) {
				sting(level, player, tick, 0.22F, t == DEATH);
			}
		}

		flyBerries(level, player, tick);
	}

	/**
	 * Lying in the bush the player fell into, the head just between the tops of the bushes. Any lower and the view would
	 * look up through the branches at the sky, which looks more like lying under a tree.
	 */
	private Vec3 lyingOffset(Vec3 eyes) {
		return Vec3.atBottomCenterOf(bed).add(0.0, 0.7, 0.0).subtract(eyes);
	}

	/** Leaning in a bit further for every handful, at the end right over the first bush and out of balance. */
	private Vec3 lean(Vec3 eyes, int tick) {
		Vec3 over = Vec3.atBottomCenterOf(picks[0]).add(0.0, 1.15, 0.0).subtract(eyes);
		Vec3 half = over.scale(0.45);
		Vec3 lean;
		if (tick < 44) {
			lean = Vec3.ZERO;
		} else if (tick < 58) {
			lean = half.scale(ease((tick - 44) / 14.0));
		} else if (tick < 84) {
			lean = half;
		} else if (tick < 98) {
			lean = half.lerp(over, ease((tick - 84) / 14.0));
		} else {
			lean = over;
		}
		// The first sting makes the player flinch back for a moment.
		if (tick >= FIRST_STING && tick < FIRST_STING + 8) {
			lean = lean.subtract(dir.scale(0.3 * (1.0 - (tick - FIRST_STING) / 8.0)));
		}
		// Too far over: the balance goes.
		if (tick >= 86) {
			double wobble = Math.min(1.0, (tick - 86) / 14.0);
			lean = lean.add(side.scale(Math.sin(tick * 0.55) * 0.14 * wobble)).add(0.0, Math.sin(tick * 0.9) * 0.04 * wobble, 0.0);
		}
		return lean;
	}

	/** Stuck in the thorns: every try to get up stings, the view drops back and looks around for a way out. */
	private void struggle(ClientLevel level, LocalPlayer player, Vec3 eyes, int tick) {
		double rise = 0.0;
		for (int t : TRIES) {
			if (tick >= t - TRY_LENGTH && tick < t) {
				rise = 0.45 * ease((tick - (t - TRY_LENGTH)) / (double) TRY_LENGTH);
			}
		}
		offset = lyingOffset(eyes).add(0.0, rise, 0.0);
		if (tick - stung < 4) {
			offset = offset.add((random.nextDouble() - 0.5) * 0.12, (random.nextDouble() - 0.5) * 0.06, (random.nextDouble() - 0.5) * 0.12);
			look = thirdPerson() ? new Vec3(flinchLook.x, eyes.y + offset.y - 1.2, flinchLook.z) : flinchLook;
			turnTowards(player, look, 0.4F);
			return;
		}
		// Looking around over the bushes and the ground for a way out, a little higher while trying to stand. In third
		// person the view stays well above the body, looking up would put the camera into the ground.
		double around = Math.sin(tick * 0.07) * 0.9;
		look = Vec3.atBottomCenterOf(bed).add(dir.yRot((float) around).scale(2.0)).add(0.0, rise * 1.2, 0.0);
		if (thirdPerson()) {
			look = new Vec3(look.x, eyes.y + offset.y - 1.4, look.z);
		}
		turnTowards(player, look, 0.07F);
	}

	/** Grows the patch: the bush in front, one behind it where the player falls in, and a few more around that one. */
	private void setup(ClientLevel level, LocalPlayer player) {
		BlockPos feet = player.blockPosition();
		Vec3 toward = found == null ? Vec3.ZERO : Vec3.atCenterOf(found).subtract(player.position());
		toward = new Vec3(toward.x, 0.0, toward.z);
		dir = toward.lengthSqr() < 0.16 ? roomiest(level, feet, horizontalLook(player)) : toward.normalize();
		side = new Vec3(-dir.z, 0.0, dir.x);
		BlockPos step = Math.abs(dir.x) > Math.abs(dir.z) ? new BlockPos(dir.x > 0 ? 1 : -1, 0, 0) : new BlockPos(0, 0, dir.z > 0 ? 1 : -1);
		BlockPos across = new BlockPos(step.getZ(), 0, -step.getX());

		BlockPos near = found != null && level.getBlockState(found).is(Blocks.SWEET_BERRY_BUSH) ? found : bush(level, feet.offset(step));
		if (near == null) {
			// No room in front: the player stands right in it.
			near = bush(level, feet);
		}
		if (near == null) {
			near = feet;
		}
		replaceBlock(level, near, RIPE);
		BlockPos deep = bush(level, near.offset(step));
		BlockPos far = deep == null ? null : bush(level, deep.offset(step));
		BlockPos left = deep == null ? null : bush(level, deep.offset(across));
		BlockPos right = deep == null ? null : bush(level, deep.subtract(across));

		picks[0] = near;
		picks[1] = deep != null ? deep : near;
		picks[2] = far != null ? far : left != null ? left : right != null ? right : picks[1];
		bed = picks[1];
		look = Vec3.atCenterOf(near);
		eatLook = look;
	}

	/**
	 * The way with the most room for a patch, the way the player looks if it has as much as any other. In a jungle
	 * full of leaf bushes the patch would otherwise grow right at the feet.
	 */
	private Vec3 roomiest(ClientLevel level, BlockPos feet, Vec3 look) {
		Vec3 best = look;
		int most = -1;
		for (int turn = 0; turn < 4; turn++) {
			Vec3 way = look.yRot((float) (turn * Math.PI / 2.0));
			BlockPos step = Math.abs(way.x) > Math.abs(way.z) ? new BlockPos(way.x > 0 ? 1 : -1, 0, 0) : new BlockPos(0, 0, way.z > 0 ? 1 : -1);
			BlockPos near = spot(level, feet.offset(step));
			int room = near == null ? 0 : 2 + (spot(level, near.offset(step)) != null ? 1 : 0) + (spot(level, near.offset(step).offset(step)) != null ? 1 : 0);
			if (room > most) {
				most = room;
				best = way;
			}
		}
		return best;
	}

	/** A ripe bush at a spot or a step up or down from it, where the ground holds one. Null if there is no room. */
	private BlockPos bush(ClientLevel level, BlockPos around) {
		BlockPos pos = spot(level, around);
		if (pos != null) {
			replaceBlock(level, pos, RIPE);
		}
		return pos;
	}

	/** Where a bush can grow at a spot or a step up or down from it: room, firm ground and no treetop. Null if nowhere. */
	private BlockPos spot(ClientLevel level, BlockPos around) {
		for (int dy : new int[] {0, 1, -1}) {
			BlockPos pos = around.above(dy);
			BlockState state = level.getBlockState(pos);
			boolean room = state.is(Blocks.SWEET_BERRY_BUSH) || state.isAir()
				|| state.canBeReplaced() && state.getFluidState().isEmpty() && !(state.getBlock() instanceof DoublePlantBlock);
			if (room && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP) && !onTree(level, pos)) {
				return pos;
			}
		}
		return null;
	}

	/** Reaches in and picks a handful: the bush loses its berries and they fly into the mouth, or fall when the player tips over. */
	private void pick(ClientLevel level, LocalPlayer player, int round, int tick) {
		BlockPos bush = picks[round];
		Looks.swing(player);
		sound(level, Vec3.atCenterOf(bush), SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + random.nextFloat() * 0.4F);
		replaceBlock(level, bush, PICKED);
		particles(level, leaves(), Vec3.atCenterOf(bush), 8, 0.3, 0.05);

		boolean last = round == PICKS.length - 1;
		for (int i = 0; i < 3; i++) {
			Vec3 from = Vec3.atCenterOf(bush).add((random.nextDouble() - 0.5) * 0.5, 0.1 + random.nextDouble() * 0.3, (random.nextDouble() - 0.5) * 0.5);
			Display.ItemDisplay display = itemDisplay(level, new ItemStack(Items.SWEET_BERRIES), from);
			Looks.glow(display);
			((de.derpvpclient.fakedeath.mixin.DisplayAccessor) display).fakedeath$setBillboard(Display.BillboardConstraints.CENTER);
			place(display, from, 0.3F);
			Vec3 drop = last ? side.scale((random.nextDouble() - 0.5) * 0.12).add(dir.scale(0.05)) : null;
			// The second handful waits until the player is over the first sting.
			berries.add(new Berry(display, from, tick + (round == 1 ? 8 : 0) + i * 3, drop));
		}
	}

	/** The berries of a handful fly one after the other in an arc into the mouth and get munched. */
	private void flyBerries(ClientLevel level, LocalPlayer player, int tick) {
		Vec3 camera = player.getEyePosition().add(CameraDirector.positionOffsetNow());
		Vec3 view = eatLook.subtract(camera);
		view = view.lengthSqr() < 1.0E-4 ? horizontalLook(player) : view.normalize();
		Vec3 mouth = camera.add(view.scale(0.45)).add(0.0, -0.22, 0.0);

		Iterator<Berry> it = berries.iterator();
		while (it.hasNext()) {
			Berry berry = it.next();
			int age = tick - berry.start;
			if (age < 0) {
				continue;
			}
			if (berry.dropped != null) {
				// Out of the hand and down to the ground.
				Vec3 pos = berry.from.add(berry.dropped.scale(age)).add(0.0, 0.08 * age - 0.025 * age * age, 0.0);
				double ground = Vec3.atBottomCenterOf(bed).y + 0.1;
				place(berry.display, new Vec3(pos.x, Math.max(ground, pos.y), pos.z), 0.3F);
				if (age > 40) {
					despawn(level, berry.display);
					it.remove();
				}
				continue;
			}
			float f = Math.min(1.0F, age / (float) FLIGHT);
			// The arc flattens out towards the mouth, so the berries come in from below and do not cover the view.
			Vec3 pos = berry.from.lerp(mouth, f).add(0.0, Math.sin(f * Math.PI) * 0.35 * (1.0 - f), 0.0);
			place(berry.display, pos, 0.3F - 0.1F * f);
			if (age >= FLIGHT) {
				despawn(level, berry.display);
				it.remove();
				sound(level, camera, Looks.EAT, SoundSource.PLAYERS, 0.8F, 0.9F + random.nextFloat() * 0.3F);
				particles(level, Looks.berryCrumbs(), mouth, 5, 0.05, 0.12);
			}
		}
	}

	/** A sting of the thorns: the view flinches, leaves fly around and the hearts go down. */
	private void sting(ClientLevel level, LocalPlayer player, int tick, float share, boolean last) {
		stung = tick;
		Vec3 camera = player.getEyePosition().add(CameraDirector.positionOffsetNow());
		// Reaching in it only makes the player jerk the head up, stuck in the thorns they look around in panic.
		flinchLook = tick < FALL_START
			? camera.add(dir.scale(3.0)).add(side.scale((random.nextDouble() - 0.5) * 1.6)).add(0.0, 1.2, 0.0)
			: camera.add((random.nextDouble() - 0.5) * 3.0, -0.6 + random.nextDouble() * 0.6, (random.nextDouble() - 0.5) * 3.0);
		// Leaves fly where the thorns got the player: around the hand in the bush, or in front of the face when lying in
		// it. Never right on the camera, so close they would show up as big dark squares.
		Vec3 view = look.subtract(camera);
		view = view.lengthSqr() < 1.0E-4 ? horizontalLook(player) : view.normalize();
		Vec3 at = tick < FALL_START ? Vec3.atCenterOf(picks[1]) : camera.add(view.scale(1.1));
		particles(level, leaves(), at, 12, 0.35, 0.1);
		sound(level, camera, SoundEvents.SWEET_BERRY_BUSH_PLACE, SoundSource.BLOCKS, 0.8F, 0.6F + random.nextFloat() * 0.3F);
		hit(level, player, SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, share, last);
	}

	private static ParticleOptions leaves() {
		return new BlockParticleOption(ParticleTypes.BLOCK, RIPE);
	}

	private static double ease(double f) {
		return f * f * (3.0 - 2.0 * f);
	}
}
