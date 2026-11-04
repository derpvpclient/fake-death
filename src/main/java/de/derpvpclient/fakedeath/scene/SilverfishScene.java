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
import net.minecraft.world.phys.Vec3;

/**
 * Mining in stone: something scratches inside the walls, then silverfish crawl out of the stone
 * one after another, swarm around the player's feet and nibble until nothing is left.
 */
public class SilverfishScene extends Scene {
	private static final int COUNT = 12;
	private static final int FIRST = 35;
	private static final int EVERY = 7;
	private static final int FIRST_BITE = FIRST + COUNT * EVERY + 10;
	private static final int BITE_EVERY = 11;
	private static final int BITES = 6;
	private static final int DEATH = FIRST_BITE + BITE_EVERY * BITES;
	private static final double RING = 0.75;

	private final BlockPos broken;
	private final List<Crawler> crawlers = new ArrayList<>();
	private List<Hole> holes;

	public SilverfishScene(BlockPos broken) {
		this.broken = broken;
	}

	@Override
	public int length() {
		return DEATH;
	}

	@Override
	public int photoTick() {
		return FIRST_BITE + 5;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.mob", player.getDisplayName(), EntityType.SILVERFISH.getDescription());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (holes == null) {
			holes = findHoles(level, player);
		}

		if (tick < FIRST) {
			// Scratching inside the stone, first where the player just dug.
			if (tick % 6 == 0) {
				Hole hole = holes.get(random.nextInt(holes.size()));
				sound(level, Vec3.atCenterOf(hole.stone), SoundEvents.SILVERFISH_STEP, SoundSource.HOSTILE, 1.0F, 0.8F + random.nextFloat() * 0.4F);
			}
			turnTowards(player, Vec3.atCenterOf(broken), 0.08F);
		}

		int since = tick - FIRST;
		if (since >= 0 && since < COUNT * EVERY && since % EVERY == 0) {
			crawlOut(level, holes.get(crawlers.isEmpty() ? 0 : random.nextInt(holes.size())));
		}

		for (int i = 0; i < crawlers.size(); i++) {
			move(level, player, crawlers.get(i), i, tick);
		}

		if (tick >= FIRST && tick < FIRST_BITE && !crawlers.isEmpty()) {
			turnTowards(player, crawlers.getLast().pos, 0.1F);
		}

		int bite = tick - FIRST_BITE;
		if (bite >= 0 && bite % BITE_EVERY == 0 && tick < DEATH) {
			sound(level, player.position(), SoundEvents.SILVERFISH_AMBIENT, SoundSource.HOSTILE, 1.2F, 1.3F);
			particles(level, ParticleTypes.CRIT, player.position().add(0, 0.3, 0), 8, 0.4, 0.2);
			hit(level, player, SoundEvents.PLAYER_HURT, 0.18F, bite / BITE_EVERY == BITES - 1);
			// The view goes down to the feet, where they bite.
			turnTowards(player, player.position().add(horizontalLook(player).scale(0.6)), 0.3F);
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
		// They circle around the feet, a little wobbly like real silverfish.
		float angle = index * Mth.TWO_PI / COUNT + tick * 0.06F;
		double wobble = Mth.sin(tick * 0.8F + index) * 0.08;
		Vec3 ring = player.position().add(Mth.cos(angle) * (RING + wobble), 0.0, Mth.sin(angle) * (RING + wobble));
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
			if (!pos.equals(broken) && level.getBlockState(pos).is(BlockTags.BASE_STONE_OVERWORLD)) {
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

	private record Hole(BlockPos stone, Vec3 exit) {
	}

	private static final class Crawler {
		final Silverfish silverfish;
		Vec3 pos;

		Crawler(Silverfish silverfish, Vec3 pos) {
			this.silverfish = silverfish;
			this.pos = pos;
		}
	}
}
