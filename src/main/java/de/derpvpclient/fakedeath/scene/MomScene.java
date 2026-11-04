package de.derpvpclient.fakedeath.scene;

import com.mojang.math.Transformation;
import de.derpvpclient.fakedeath.mixin.DisplayAccessor;
import de.derpvpclient.fakedeath.mixin.ItemDisplayAccessor;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Played too late into the night: Mom calls out with the real time, comes up the stairs, counts to
 * three, and then things fly. A slipper first, then whatever she finds in the player's inventory,
 * and the slipper again for the end. Heavy things hurt more than light ones.
 */
public class MomScene extends Scene {
	private static final Component NAME = Component.translatable("fakedeath.mom.name").withStyle(ChatFormatting.LIGHT_PURPLE);
	private static final ItemStack SLIPPER = new ItemStack(Items.LEATHER_BOOTS);
	private static final int STEPS = 45;
	private static final int DOOR = 105;
	private static final int SCHOOL = 115;
	private static final int COUNT = 195;
	private static final int ONE = 250;
	private static final int TWO = 290;
	private static final int THREE = 330;
	private static final int THROW_START = 340;
	private static final int THROW_EVERY = 15;
	private static final int FLIGHT = 9;
	private static final int FROM_INVENTORY = 4;
	private static final int MAX_THROWS = FROM_INVENTORY + 2;
	private static final int DEATH = THROW_START + MAX_THROWS * THROW_EVERY + 60;

	private final List<ItemStack> ammo = new ArrayList<>();
	private Villager mom;
	private Vec3 behind;
	private Display.ItemDisplay flying;
	private ItemStack flyingStack;
	private Vec3 throwFrom;
	private int thrown = -1;
	private int throwIndex;

	/** Late at night on the real clock, when Mom would really come in. */
	public static boolean isLate() {
		int hour = LocalTime.now().getHour();
		return hour >= 23 || hour < 5;
	}

	@Override
	public int length() {
		return DEATH;
	}

	@Override
	public int photoTick() {
		return THROW_START + THROW_EVERY + 4;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.mom", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			behind = groundSpot(level, player, 2.5, 180.0);
			String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
			say(minecraft, NAME, Component.translatable("fakedeath.mom.awake", player.getName(), time));
		}

		if (tick >= STEPS && tick < DOOR && (tick - STEPS) % 8 == 0) {
			// Up the stairs, closer with every step.
			float closer = (tick - STEPS) / (float) (DOOR - STEPS);
			Vec3 step = player.position().add(behind.subtract(player.position()).scale(4.0 - closer * 2.5));
			sound(level, step, SoundEvents.WOOD_STEP, SoundSource.NEUTRAL, 1.0F + closer, 0.9F);
		}

		if (tick == DOOR) {
			sound(level, behind, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
			mom = spawn(level, EntityTypes.VILLAGER, behind);
			mom.setCustomName(NAME);
			mom.setCustomNameVisible(true);
			mom.noPhysics = true;
		}

		if (mom == null) {
			return;
		}

		face(mom, player.getEyePosition());
		turnTowards(player, flying != null ? flying.position() : mom.getEyePosition(), flying != null ? 0.35F : 0.15F);

		line(minecraft, level, tick, SCHOOL, "fakedeath.mom.school", 1.0F);
		line(minecraft, level, tick, COUNT, "fakedeath.mom.count", 1.0F);
		line(minecraft, level, tick, ONE, "fakedeath.mom.one", 1.0F);
		line(minecraft, level, tick, TWO, "fakedeath.mom.two", 0.9F);
		line(minecraft, level, tick, THREE, "fakedeath.mom.three", 0.8F);

		if (tick == THREE) {
			mom.setUnhappyCounter(200);
			particles(level, ParticleTypes.ANGRY_VILLAGER, mom.getEyePosition().add(0.0, 0.5, 0.0), 5, 0.3, 0.0);
			loadAmmo(player);
		}

		int since = tick - THROW_START;
		if (since >= 0 && since % THROW_EVERY == 0 && throwIndex < ammo.size() && flying == null) {
			throwNext(level, tick);

			if (throwIndex == 2 && ammo.size() > 2) {
				// The first thing out of the inventory.
				line(minecraft, level, tick, tick, "fakedeath.mom.stuff", 1.1F);
			}
		}

		if (flying != null) {
			fly(minecraft, level, player, tick);
		}
	}

	/** A slipper, up to four things from the inventory, and the slipper again to finish. */
	private void loadAmmo(LocalPlayer player) {
		ammo.add(SLIPPER);

		Inventory inventory = player.getInventory();
		List<ItemStack> found = new ArrayList<>();
		Set<Item> seen = new HashSet<>();
		seen.add(Items.LEATHER_BOOTS);

		for (int slot = 0; slot < Math.min(Inventory.INVENTORY_SIZE, inventory.getContainerSize()); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && seen.add(stack.getItem())) {
				found.add(stack.copyWithCount(1));
			}
		}

		while (!found.isEmpty() && ammo.size() <= FROM_INVENTORY) {
			ammo.add(found.remove(random.nextInt(found.size())));
		}

		ammo.add(SLIPPER);
	}

	private void throwNext(ClientLevel level, int tick) {
		flyingStack = ammo.get(throwIndex++);
		throwFrom = mom.getEyePosition().add(0.0, 0.2, 0.0);
		flying = spawn(level, EntityTypes.ITEM_DISPLAY, throwFrom);
		((ItemDisplayAccessor) flying).fakedeath$setItemStack(flyingStack);
		((ItemDisplayAccessor) flying).fakedeath$setItemTransform(ItemDisplayContext.FIXED);
		((DisplayAccessor) flying).fakedeath$setInterpolationDuration(1);
		thrown = tick;
		mom.swing(InteractionHand.MAIN_HAND);
		sound(level, throwFrom, SoundEvents.WITCH_THROW, SoundSource.NEUTRAL, 1.0F, 1.0F + random.nextFloat() * 0.3F);
	}

	private void fly(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		float t = (tick - thrown) / (float) FLIGHT;
		Vec3 at = throwFrom.lerp(player.getEyePosition(), Math.min(1.0F, t)).add(0.0, Math.sin(Math.PI * Math.min(1.0F, t)) * 0.5, 0.0);
		flying.setPos(at.x, at.y, at.z);
		float size = flyingStack.getItem() instanceof BlockItem ? 0.45F : 0.6F;
		Quaternionf spin = new Quaternionf().rotateZ((tick - thrown) * 0.9F).rotateY((float) Math.toRadians(-player.getYRot()));
		transform(flying, new Transformation(new Vector3f(), spin, new Vector3f(size), new Quaternionf()));

		if (t >= 1.0F) {
			boolean last = throwIndex >= ammo.size();
			sound(level, player.position(), impactSound(flyingStack), SoundSource.PLAYERS, 1.5F, 0.9F);
			particles(level, new ItemParticleOption(ParticleTypes.ITEM, flyingStack.getItem()), player.getEyePosition(), 10, 0.3, 0.15);
			hit(level, player, SoundEvents.PLAYER_HURT, weight(flyingStack), last);
			despawn(level, flying);
			flying = null;

			if (last) {
				sound(level, mom.position(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.0F);
				say(minecraft, NAME, Component.translatable("fakedeath.mom.bed"));
			}
		}
	}

	/** How much a hit hurts: an anvil a lot, a stone block or a sword quite a bit, a cookie hardly at all. */
	private static float weight(ItemStack stack) {
		if (stack.getItem() instanceof BlockItem block) {
			BlockState state = block.getBlock().defaultBlockState();
			if (state.is(BlockTags.ANVIL)) {
				return 0.5F;
			}
			return state.requiresCorrectToolForDrops() ? 0.3F : 0.18F;
		}
		if (stack.has(DataComponents.WEAPON) || stack.has(DataComponents.TOOL)) {
			return 0.3F;
		}
		if (stack.is(Items.LEATHER_BOOTS)) {
			return 0.2F;
		}
		if (stack.has(DataComponents.EQUIPPABLE)) {
			return 0.25F;
		}
		if (stack.has(DataComponents.FOOD)) {
			return 0.08F;
		}
		return 0.12F;
	}

	private static SoundEvent impactSound(ItemStack stack) {
		if (stack.getItem() instanceof BlockItem block) {
			return block.getBlock().defaultBlockState().getSoundType().getBreakSound();
		}
		if (stack.is(Items.LEATHER_BOOTS)) {
			return SoundEvents.ARMOR_EQUIP_LEATHER.value();
		}
		if (stack.has(DataComponents.WEAPON) || stack.has(DataComponents.TOOL)) {
			return SoundEvents.PLAYER_ATTACK_STRONG;
		}
		if (stack.has(DataComponents.EQUIPPABLE)) {
			return SoundEvents.ARMOR_EQUIP_IRON.value();
		}
		if (stack.has(DataComponents.FOOD)) {
			return SoundEvents.SLIME_SQUISH_SMALL;
		}
		return SoundEvents.PLAYER_ATTACK_WEAK;
	}

	private void line(Minecraft minecraft, ClientLevel level, int tick, int at, String key, float pitch) {
		if (tick == at) {
			sound(level, mom.position(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 1.0F, pitch);
			say(minecraft, NAME, Component.translatable(key));
		}
	}
}
