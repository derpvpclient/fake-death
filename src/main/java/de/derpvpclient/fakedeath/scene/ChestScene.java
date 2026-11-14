package de.derpvpclient.fakedeath.scene;

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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A chest rattles, pops open and spits out its loot. Then the real avalanche comes: chest after chest tumbles down out
 * of the sky, lids clapping, crashing all around the player and onto them, the last and biggest one right on the head.
 */
public class ChestScene extends Scene {
	private static final List<Item> LOOT = List.of(Items.COBBLESTONE, Items.DIRT, Items.OAK_LOG, Items.GOLD_INGOT, Items.DIAMOND,
		Items.BREAD, Items.IRON_SWORD, Items.TORCH, Items.ROTTEN_FLESH, Items.BONE, Items.STRING, Items.COAL, Items.APPLE, Items.ARROW, Items.GRAVEL);
	private static final Item[] CHESTS = {Items.CHEST, Items.CHEST, Items.TRAPPED_CHEST, Items.CHEST, Items.ENDER_CHEST};
	private static final int OPEN = 30;
	private static final int ITEM_EVERY = 2;
	private static final int ITEMS = 14;
	private static final int ITEM_FLIGHT = 10;
	/** The first chest rumbles down from above. */
	private static final int RAIN = OPEN + ITEMS * ITEM_EVERY + 10;
	private static final int CHEST_EVERY = 6;
	private static final int FALLING = 14;
	private static final int FALL = 14;
	/** The chests that land right on the player, each one a hard hit. */
	private static final int[] ON_PLAYER = {2, 5, 8, 11};
	private static final int BIG = FALLING;
	private static final int DEATH = RAIN + BIG * CHEST_EVERY + FALL;

	private final BlockPos found;
	private final List<Flight> flights = new ArrayList<>();
	private final List<Drop> drops = new ArrayList<>();
	private Vec3 chest;

	public ChestScene(BlockPos chest) {
		this.found = chest;
	}

	@Override
	public int length() {
		return DEATH + 20;
	}

	@Override
	public int photoTick() {
		return RAIN + 8 * CHEST_EVERY;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.truhe", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			BlockPos pos = found;
			if (pos == null) {
				// No chest around for a test: one appears in front, only on this client.
				pos = BlockPos.containing(groundSpot(level, player, 2.5, 0.0));
				placeBlock(level, pos, Blocks.CHEST.defaultBlockState());
			}
			chest = Vec3.atCenterOf(pos);
		}

		// First the chest, then up into the sky where the chests come from, then the last one coming down on the head.
		Vec3 look = tick < RAIN - 6 ? chest : tick < RAIN + 30 ? player.getEyePosition().add(horizontalLook(player).scale(3.0)).add(0.0, 6.0, 0.0)
			: player.getEyePosition().add(horizontalLook(player).scale(1.5)).add(0.0, 4.0, 0.0);
		// From outside (F5) the camera stays put and films the chests coming down around the body.
		if (!thirdPerson()) {
			turnTowards(player, look, 0.12F);
		}

		if (tick < OPEN && tick % 7 == 0) {
			// It rattles from the inside.
			sound(level, chest, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 0.7F + random.nextFloat() * 0.3F);
			particles(level, ParticleTypes.SMOKE, chest.add(0.0, 0.55, 0.0), 3, 0.3, 0.02);
		}
		if (tick == OPEN) {
			sound(level, chest, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.4F, 0.7F);
			particles(level, ParticleTypes.POOF, chest.add(0.0, 0.6, 0.0), 14, 0.3, 0.12);
		}

		int since = tick - OPEN;
		if (since >= 0 && since % ITEM_EVERY == 0 && since / ITEM_EVERY < ITEMS) {
			throwItem(level, player, since / ITEM_EVERY, tick);
			if (since % 6 == 0) {
				sound(level, chest, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 0.6F + random.nextFloat() * 0.6F);
			}
		}
		for (Flight flight : flights) {
			int age = tick - flight.start();
			if (age <= ITEM_FLIGHT) {
				float p = age / (float) ITEM_FLIGHT;
				Vec3 at = chest.add(0.0, 0.6, 0.0).lerp(flight.target(), p).add(0.0, Mth.sin(p * Mth.PI) * 1.8, 0.0);
				place(flight.item(), at, new Vector3f(0.5F), flight.spin());
			}
		}

		if (tick == RAIN - 12) {
			// Something big is coming from above.
			sound(level, player.position().add(0.0, 10.0, 0.0), SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 3.0F, 0.4F);
			sound(level, player.position(), SoundEvents.ELYTRA_FLYING, SoundSource.BLOCKS, 0.8F, 1.6F);
		}
		int rain = tick - RAIN;
		if (rain >= 0 && rain % CHEST_EVERY == 0 && rain / CHEST_EVERY <= BIG) {
			dropChest(level, player, rain / CHEST_EVERY, tick);
		}
		for (Drop drop : drops) {
			fall(level, player, drop, tick);
		}
	}

	private void throwItem(ClientLevel level, LocalPlayer player, int index, int tick) {
		ItemStack stack = new ItemStack(LOOT.get(random.nextInt(LOOT.size())));
		Display.ItemDisplay item = itemDisplay(level, stack, chest);
		double angle = random.nextDouble() * Math.PI * 2;
		double spread = 0.3 + random.nextDouble() * 0.5;
		Vec3 target = player.position().add(Math.cos(angle) * spread, 0.15 + index * 0.03, Math.sin(angle) * spread);
		Quaternionf spin = new Quaternionf().rotateXYZ(random.nextFloat() * 6, random.nextFloat() * 6, random.nextFloat() * 6);
		place(item, chest, new Vector3f(0.5F), spin);
		flights.add(new Flight(item, target, spin, tick));
	}

	/** One chest out of the sky: around the player, some right onto them, the last one twice as big on the head. */
	private void dropChest(ClientLevel level, LocalPlayer player, int index, int tick) {
		boolean big = index == BIG;
		boolean onPlayer = big || java.util.Arrays.stream(ON_PLAYER).anyMatch(i -> i == index);
		Vec3 land;
		if (onPlayer) {
			// Onto the head, where it really hurts.
			land = player.position().add((random.nextDouble() - 0.5) * 0.3, big ? 2.4 : 2.0, (random.nextDouble() - 0.5) * 0.3);
		} else {
			double angle = random.nextDouble() * Math.PI * 2;
			double r = 1.0 + random.nextDouble() * 2.2;
			double x = player.getX() + Math.cos(angle) * r;
			double z = player.getZ() + Math.sin(angle) * r;
			land = new Vec3(x, floor(level, x, z, player.getY()) + 0.5, z);
		}
		Vec3 from = land.add((random.nextDouble() - 0.5) * 3.0, 9.0 + random.nextDouble() * 3.0, (random.nextDouble() - 0.5) * 3.0);
		Item kind = big ? Items.CHEST : CHESTS[random.nextInt(CHESTS.length)];
		Display.ItemDisplay display = itemDisplay(level, new ItemStack(kind), from);
		Vector3f axis = new Vector3f(random.nextFloat() - 0.5F, 0.3F, random.nextFloat() - 0.5F).normalize();
		drops.add(new Drop(display, from, land, axis, tick, onPlayer, big, index));
	}

	private void fall(ClientLevel level, LocalPlayer player, Drop drop, int tick) {
		int age = tick - drop.start();
		float size = drop.big() ? 4.0F : 2.0F;
		if (age > FALL) {
			if (drop.onPlayer() && !drop.big() && age <= FALL + 6) {
				// Off the head and down beside the player.
				float q = (age - FALL) / 6.0F;
				Vec3 aside = drop.land().add(drop.axis().x * 1.2, 0.0, drop.axis().z * 1.2);
				Vec3 down = new Vec3(aside.x, floor(level, aside.x, aside.z, player.getY()) + 0.5, aside.z);
				Vec3 at = drop.land().lerp(down, q).add(0.0, Math.sin(q * Math.PI) * 0.6, 0.0);
				place(drop.item(), at, new Vector3f(size), new Quaternionf().rotateAxis(q * 3.0F, drop.axis()));
			}
			return;
		}
		float p = age / (float) FALL;
		// Faster and faster, tumbling all the way down.
		Vec3 at = drop.from().lerp(drop.land(), p * p);
		Quaternionf spin = age == FALL ? new Quaternionf().rotateY(random.nextFloat() * 6.0F) : new Quaternionf().rotateAxis(p * 7.0F, drop.axis());
		place(drop.item(), at, new Vector3f(size), spin);
		if (age % 4 == 0 && age < FALL) {
			sound(level, at, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.6F, 0.8F + random.nextFloat() * 0.4F);
		}
		if (age == FALL) {
			sound(level, drop.land(), SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.6F, 0.6F);
			sound(level, drop.land(), SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 1.4F, 0.6F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.defaultBlockState()), drop.land(), drop.big() ? 50 : 18, 0.6, 0.2);
			cameraShake(drop.onPlayer() ? 0.35F : 0.12F);
			if (drop.big()) {
				// The big one: right on the head, everything goes dark.
				sound(level, player.position(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.5F, 0.5F);
				kill(level, player, SoundEvents.PLAYER_HURT);
			} else if (drop.onPlayer()) {
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.24F);
			}
		}
	}

	private record Flight(Display.ItemDisplay item, Vec3 target, Quaternionf spin, int start) {
	}

	private record Drop(Display.ItemDisplay item, Vec3 from, Vec3 land, Vector3f axis, int start, boolean onPlayer, boolean big, int index) {
	}
}
