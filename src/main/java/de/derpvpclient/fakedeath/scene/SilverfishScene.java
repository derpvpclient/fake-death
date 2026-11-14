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
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Deep in the stone. Something scratches inside the walls, cracks run through the rock, silverfish crawl out one after
 * another and swarm around the feet. Then the floor gives way: it was hollow, a nest, and the player drops into a pit
 * that is crawling with them.
 */
public class SilverfishScene extends Scene {
	private static final int CRACK = 40;
	private static final int COUNT = 10;
	private static final int FIRST = 40;
	private static final int EVERY = 6;
	private static final int COLLAPSE = FIRST + COUNT * EVERY + 25;
	private static final int FALL = 12;
	private static final int DEPTH = 4;
	private static final int NEST = 18;
	private static final int FIRST_BITE = COLLAPSE + FALL + 14;
	private static final int BITE_EVERY = 10;
	private static final int BITES = 5;
	private static final int DEATH = FIRST_BITE + BITE_EVERY * (BITES - 1);
	private static final int CRACK_ID = -7_340_000;

	private final BlockPos broken;
	private final List<Crawler> crawlers = new ArrayList<>();
	private final List<Silverfish> nest = new ArrayList<>();
	private List<Hole> holes;
	private BlockPos pitTop;
	private boolean fallen;

	public SilverfishScene(BlockPos broken) {
		this.broken = broken;
	}

	@Override
	public int length() {
		return DEATH + 1;
	}

	@Override
	public int photoTick() {
		return FIRST_BITE + 5;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.silberfische", player.getDisplayName());
	}

	@Override
	public float lying() {
		// In F5 the body lies at the bottom of the pit.
		return fallen ? 1.0F : 0.0F;
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (holes == null) {
			holes = findHoles(level, player);
		}

		if (tick < FIRST + COUNT * EVERY) {
			// Scratching inside the stone, cracks spreading where they are about to come out.
			if (tick % 6 == 0) {
				Hole hole = holes.get(random.nextInt(holes.size()));
				sound(level, Vec3.atCenterOf(hole.stone), SoundEvents.SILVERFISH_STEP, SoundSource.HOSTILE, 1.0F, 0.8F + random.nextFloat() * 0.4F);
			}
			for (int i = 0; i < holes.size(); i++) {
				level.destroyBlockProgress(CRACK_ID - i, holes.get(i).stone, Math.min(9, tick * 10 / CRACK - i % 3));
			}
		}
		if (tick < FIRST) {
			turnTowards(player, Vec3.atCenterOf(broken), 0.08F);
		}

		int since = tick - FIRST;
		if (since >= 0 && since < COUNT * EVERY && since % EVERY == 0) {
			crawlOut(level, holes.get(crawlers.isEmpty() ? 0 : random.nextInt(holes.size())));
		}
		for (int i = 0; i < crawlers.size(); i++) {
			move(level, player, crawlers.get(i), i, tick);
		}
		if (tick >= FIRST && tick < COLLAPSE && !crawlers.isEmpty()) {
			turnTowards(player, crawlers.getLast().pos, 0.1F);
		}

		if (tick == COLLAPSE - 20) {
			// The floor under the feet starts to crack, it is hollow.
			sound(level, player.position(), SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.2F, 0.6F);
		}
		if (tick >= COLLAPSE - 20 && tick < COLLAPSE) {
			level.destroyBlockProgress(CRACK_ID - 100, player.blockPosition().below(), (tick - COLLAPSE + 20) / 2);
			turnTowards(player, player.position().add(horizontalLook(player).scale(0.5)), 0.2F);
		}
		if (tick == COLLAPSE) {
			collapse(level, player);
		}
		if (pitTop != null) {
			// Dropping to the bottom of the pit, then lying there looking up while they swarm over.
			float p = Math.min(1.0F, (tick - COLLAPSE) / (float) FALL);
			moveCamera(new Vec3(0.0, -(DEPTH + 0.2) * p * p, 0.0));
			if (tick == COLLAPSE + FALL) {
				fallen = true;
				sound(level, player.position(), SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 1.5F, 1.0F);
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.3F);
			}
			swarm(level, player, tick);
			if (tick > COLLAPSE + FALL) {
				turnTowards(player, player.getEyePosition().add(0.0, 4.0, 0.0).add(horizontalLook(player).scale(0.8)), 0.1F);
			}
		}

		int bite = tick - FIRST_BITE;
		if (bite >= 0 && bite % BITE_EVERY == 0 && bite / BITE_EVERY < BITES) {
			sound(level, player.position(), SoundEvents.SILVERFISH_AMBIENT, SoundSource.HOSTILE, 1.4F, 1.2F + random.nextFloat() * 0.2F);
			particles(level, ParticleTypes.CRIT, player.getEyePosition().add(0, -DEPTH, 0), 10, 0.4, 0.2);
			hit(level, player, SoundEvents.PLAYER_HURT, 0.16F, bite / BITE_EVERY == BITES - 1);
			cameraShake(0.0F);
		}
	}

	/**
	 * The floor gives way: three by three blocks and four deep become a pit, on this client only. Its top stays solid in
	 * secret (invisible barriers), so the player really keeps standing; only the camera drops.
	 */
	private void collapse(ClientLevel level, LocalPlayer player) {
		BlockPos below = player.blockPosition().below();
		pitTop = below;
		level.destroyBlockProgress(CRACK_ID - 100, below, -1);
		BlockState floor = level.getBlockState(below);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos top = below.offset(dx, 0, dz);
				particles(level, new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(top)), Vec3.atCenterOf(top), 12, 0.5, 0.2);
				replaceBlock(level, top, Blocks.BARRIER.defaultBlockState());
				for (int d = 1; d <= DEPTH; d++) {
					hideBlock(level, top.below(d));
				}
			}
		}
		sound(level, player.position(), SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 2.0F, 0.5F);
		sound(level, player.position(), SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 2.0F, 0.6F);
		// The nest down there: a whole swarm waiting at the bottom.
		Vec3 bottom = Vec3.atBottomCenterOf(below.below(DEPTH));
		for (int i = 0; i < NEST; i++) {
			Vec3 at = bottom.add((random.nextDouble() - 0.5) * 2.6, 0.0, (random.nextDouble() - 0.5) * 2.6);
			Silverfish silverfish = spawn(level, EntityType.SILVERFISH, at);
			silverfish.noPhysics = true;
			nest.add(silverfish);
		}
		// The ones from the walls go down with the player.
		for (Crawler crawler : crawlers) {
			crawler.falling = true;
		}
		if (floor.is(BlockTags.BASE_STONE_OVERWORLD)) {
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, floor), Vec3.atCenterOf(below), 30, 1.2, 0.3);
		}
	}

	/** At the bottom they crawl over each other and up the walls, more and more of them over the player. */
	private void swarm(ClientLevel level, LocalPlayer player, int tick) {
		Vec3 bottom = Vec3.atBottomCenterOf(pitTop.below(DEPTH));
		for (int i = 0; i < nest.size(); i++) {
			Silverfish silverfish = nest.get(i);
			float angle = i * Mth.TWO_PI / nest.size() + tick * (i % 2 == 0 ? 0.07F : -0.05F);
			double r = 0.4 + (i % 4) * 0.28 + Mth.sin(tick * 0.5F + i) * 0.06;
			// Some climb up the walls of the pit and drop again.
			double climb = i % 3 == 0 ? Math.max(0.0, Math.sin(tick * 0.09 + i)) * 2.2 : (i % 5) * 0.12;
			Vec3 at = bottom.add(Mth.cos(angle) * r, climb, Mth.sin(angle) * r);
			walk(silverfish, at);
			face(silverfish, at.add(-Mth.sin(angle), 0.0, Mth.cos(angle)));
		}
		if (tick % 9 == 0) {
			sound(level, bottom, SoundEvents.SILVERFISH_AMBIENT, SoundSource.HOSTILE, 1.2F, 0.8F + random.nextFloat() * 0.5F);
		}
	}

	private void crawlOut(ClientLevel level, Hole hole) {
		Silverfish silverfish = spawn(level, EntityType.SILVERFISH, hole.exit);
		silverfish.noPhysics = true;
		crawlers.add(new Crawler(silverfish, hole.exit));

		BlockParticleOption dust = new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(hole.stone));
		particles(level, dust, Vec3.atCenterOf(hole.stone), 14, 0.5, 0.1);
		sound(level, hole.exit, SoundEvents.SILVERFISH_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.9F + random.nextFloat() * 0.3F);
	}

	private void move(ClientLevel level, LocalPlayer player, Crawler crawler, int index, int tick) {
		if (crawler.falling) {
			// Down into the pit with the player.
			double bottom = pitTop.getY() - DEPTH;
			crawler.pos = new Vec3(crawler.pos.x * 0.85 + player.getX() * 0.15, Math.max(bottom, crawler.pos.y - 0.45), crawler.pos.z * 0.85 + player.getZ() * 0.15);
			walk(crawler.silverfish, crawler.pos);
			return;
		}
		// They circle around the feet, a little wobbly like real silverfish.
		float angle = index * Mth.TWO_PI / COUNT + tick * 0.06F;
		double wobble = Mth.sin(tick * 0.8F + index) * 0.08;
		Vec3 ring = player.position().add(Mth.cos(angle) * (0.75 + wobble), 0.0, Mth.sin(angle) * (0.75 + wobble));
		Vec3 way = ring.subtract(crawler.pos);
		Vec3 flat = new Vec3(way.x, 0.0, way.z);
		Vec3 step = flat.length() > 0.14 ? flat.normalize().scale(0.14) : flat;

		double x = crawler.pos.x + step.x;
		double z = crawler.pos.z + step.z;
		double ground = floor(level, x, z, player.getY());
		// Out of a wall or the ceiling they drop to the floor first.
		double y = crawler.pos.y > ground ? Math.max(ground, crawler.pos.y - 0.3) : ground;
		crawler.pos = new Vec3(x, y, z);

		walk(crawler.silverfish, crawler.pos);
		face(crawler.silverfish, step.lengthSqr() > 1.0E-4 ? crawler.pos.add(step) : player.position());
	}

	/** Stone blocks around the player that touch air, where a silverfish can come out. The dug block first. */
	private List<Hole> findHoles(ClientLevel level, LocalPlayer player) {
		List<Hole> found = new ArrayList<>();
		addHole(level, broken, found);

		BlockPos feet = player.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-4, -1, -4), feet.offset(4, 3, 4))) {
			if (found.size() >= 24) {
				break;
			}
			if (!pos.equals(broken) && Math.abs(pos.getX() - feet.getX()) + Math.abs(pos.getZ() - feet.getZ()) > 1
				&& level.getBlockState(pos).is(BlockTags.BASE_STONE_OVERWORLD)) {
				addHole(level, pos.immutable(), found);
			}
		}

		if (found.isEmpty()) {
			Vec3 spot = groundSpot(level, player, 3.0, 0.0);
			found.add(new Hole(BlockPos.containing(spot).below(), spot));
		}

		return found;
	}

	private static void addHole(ClientLevel level, BlockPos stone, List<Hole> found) {
		for (Direction side : Direction.values()) {
			BlockPos air = stone.relative(side);

			if (level.getBlockState(air).isAir()) {
				found.add(new Hole(stone, Vec3.atBottomCenterOf(air)));
				return;
			}
		}
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		if (holes != null) {
			for (int i = 0; i < holes.size(); i++) {
				level.destroyBlockProgress(CRACK_ID - i, holes.get(i).stone, -1);
			}
		}
		level.destroyBlockProgress(CRACK_ID - 100, player.blockPosition().below(), -1);
		super.end(level, player);
	}

	private record Hole(BlockPos stone, Vec3 exit) {
	}

	private static final class Crawler {
		final Silverfish silverfish;
		Vec3 pos;
		boolean falling;

		Crawler(Silverfish silverfish, Vec3 pos) {
			this.silverfish = silverfish;
			this.pos = pos;
		}
	}
}
