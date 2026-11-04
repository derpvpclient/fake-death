package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
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

/** A chest pops open by itself and more and more things pour out of it, until the player is buried under them. */
public class ChestScene extends Scene {
	private static final List<Item> LOOT = List.of(Items.COBBLESTONE, Items.DIRT, Items.OAK_LOG, Items.GOLD_INGOT, Items.DIAMOND,
		Items.BREAD, Items.IRON_SWORD, Items.TORCH, Items.ROTTEN_FLESH, Items.BONE, Items.STRING, Items.COAL, Items.APPLE, Items.ARROW, Items.GRAVEL);
	private static final int OPEN = 30;
	private static final int EVERY = 3;
	private static final int FLIGHT = 10;
	private static final int ITEMS = 32;
	private static final int DEATH = OPEN + ITEMS * EVERY + FLIGHT + 5;

	private final BlockPos found;
	private final List<Flight> flights = new ArrayList<>();
	private Vec3 chest;
	private int landed;

	public ChestScene(BlockPos chest) {
		this.found = chest;
	}

	@Override
	public int length() {
		return DEATH + 20;
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
		turnTowards(player, tick < OPEN + 20 ? chest : chest.lerp(player.getEyePosition(), 0.5), 0.15F);

		if (tick == OPEN - 15 || tick == OPEN - 8) {
			sound(level, chest, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 0.8F);
		}
		if (tick == OPEN) {
			sound(level, chest, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.2F, 0.8F);
			particles(level, ParticleTypes.POOF, chest.add(0.0, 0.6, 0.0), 10, 0.3, 0.1);
		}

		int since = tick - OPEN;
		if (since >= 0 && since % EVERY == 0 && since / EVERY < ITEMS) {
			throwItem(level, player, since / EVERY, tick);
			if (since % 9 == 0) {
				sound(level, chest, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 0.6F + random.nextFloat() * 0.6F);
			}
		}

		for (Flight flight : flights) {
			int age = tick - flight.start();
			if (age > FLIGHT) {
				continue;
			}
			float p = age / (float) FLIGHT;
			Vec3 at = chest.add(0.0, 0.6, 0.0).lerp(flight.target(), p).add(0.0, Mth.sin(p * Mth.PI) * 1.8, 0.0);
			place(flight.item(), at, new Vector3f(0.5F), flight.spin());
			if (age == FLIGHT && ++landed % 8 == 0 && landed < ITEMS) {
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.22F);
			}
		}

		if (tick == DEATH) {
			sound(level, player.position(), SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 1.5F, 0.6F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void throwItem(ClientLevel level, LocalPlayer player, int index, int tick) {
		ItemStack stack = new ItemStack(LOOT.get(random.nextInt(LOOT.size())));
		Display.ItemDisplay item = itemDisplay(level, stack, chest);
		// Each lands a little higher on the pile around the player, the last ones over the head.
		double angle = random.nextDouble() * Math.PI * 2;
		double spread = Math.max(0.1, 0.6 - index * 0.015);
		Vec3 target = player.position().add(Math.cos(angle) * spread, 0.15 + index * 0.06, Math.sin(angle) * spread);
		Quaternionf spin = new Quaternionf().rotateXYZ(random.nextFloat() * 6, random.nextFloat() * 6, random.nextFloat() * 6);
		place(item, chest, new Vector3f(0.5F), spin);
		flights.add(new Flight(item, target, spin, tick));
	}

	private record Flight(Display.ItemDisplay item, Vec3 target, Quaternionf spin, int start) {
	}
}
