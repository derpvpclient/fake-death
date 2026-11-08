package de.derpvpclient.fakedeath.scene;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Blocks, sounds and moves the newer scenes use whose names change between Minecraft versions. Kept in one place, so
 * the ports to older versions only have to touch this file.
 */
final class Looks {
	static final BlockState BLACK_CONCRETE = Blocks.BLACK_CONCRETE.defaultBlockState();
	static final BlockState WHITE_CONCRETE = Blocks.WHITE_CONCRETE.defaultBlockState();
	static final BlockState LIGHT_BLUE_GLASS = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
	static final BlockState RED_WOOL = Blocks.RED_WOOL.defaultBlockState();
	static final BlockState WHITE_WOOL = Blocks.WHITE_WOOL.defaultBlockState();
	static final BlockState GRAY_CONCRETE = Blocks.GRAY_CONCRETE.defaultBlockState();

	static final SoundEvent EXPLODE = SoundEvents.GENERIC_EXPLODE.value();
	static final SoundEvent EAT = SoundEvents.GENERIC_EAT.value();
	static final SoundEvent SAW = SoundEvents.AXE_STRIP;
	static final SoundEvent CAVE = SoundEvents.AMBIENT_CAVE.value();
	static final SoundEvent CHICKEN = SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.withDefaultNamespace("entity.chicken.ambient"));
	static final SoundEvent CHICKEN_HURT = SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.withDefaultNamespace("entity.chicken.hurt"));
	static final BlockState CHAIN = Blocks.CHAIN.defaultBlockState();

	static final SoundEvent SADDLE = SoundEvents.HORSE_SADDLE;
	static final BlockState PURPLE_GLASS = Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
	static final SoundEvent CAT_PURR = SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.withDefaultNamespace("entity.cat.purr"));
	static final SoundEvent CAT_PURREOW = SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.withDefaultNamespace("entity.cat.purreow"));
	static final SoundEvent PLING = SoundEvents.NOTE_BLOCK_PLING.value();

	/** Puts a saddle on a horse. */
	static void saddle(net.minecraft.world.entity.animal.horse.AbstractHorse horse) {
		horse.equipSaddle(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE), null);
	}

	/** Up on its hind legs. */
	static void rear(net.minecraft.world.entity.animal.horse.AbstractHorse horse) {
		horse.setStanding(true);
	}

	/** Gives each cat of a crowd its own coat. */
	static void catColor(net.minecraft.world.entity.Entity cat, int i) {
	}

	/** The snowy village coat, for Santa. */
	static void snowCoat(net.minecraft.world.entity.npc.Villager villager, net.minecraft.client.multiplayer.ClientLevel level) {
		villager.setVillagerData(villager.getVillagerData().setType(net.minecraft.world.entity.npc.VillagerType.SNOW));
	}

	/** Bamboo crumbs, for the panda munching. */
	static net.minecraft.core.particles.ParticleOptions bambooCrumbs() {
		return new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, Blocks.BAMBOO_BLOCK.defaultBlockState());
	}

	private Looks() {
	}

	/** A pointed dripstone hanging down, the tip or the thicker part above it. */
	static BlockState dripstone(boolean tip) {
		return Blocks.POINTED_DRIPSTONE.defaultBlockState()
			.setValue(net.minecraft.world.level.block.PointedDripstoneBlock.TIP_DIRECTION, net.minecraft.core.Direction.DOWN)
			.setValue(net.minecraft.world.level.block.PointedDripstoneBlock.THICKNESS,
				tip ? net.minecraft.world.level.block.state.properties.DripstoneThickness.TIP : net.minecraft.world.level.block.state.properties.DripstoneThickness.FRUSTUM);
	}

	/** Lets a display be drawn from far away, like the moon. */
	static void farView(net.minecraft.world.entity.Display display) {
		((de.derpvpclient.fakedeath.mixin.DisplayAccessor) display).fakedeath$setViewRange(16.0F);
	}

	/** Draws a display at full brightness, so it never shows up dark against the sky. */
	static void glow(net.minecraft.world.entity.Display display) {
		((de.derpvpclient.fakedeath.mixin.DisplayAccessor) display).fakedeath$setBrightnessOverride(net.minecraft.util.Brightness.FULL_BRIGHT);
	}

	/** Bits of a sweet berry, for munching. */
	static net.minecraft.core.particles.ParticleOptions berryCrumbs() {
		return new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SWEET_BERRIES));
	}

	/** Swings the main hand, like hitting or throwing. */
	static void swing(LivingEntity entity) {
		entity.swing(InteractionHand.MAIN_HAND);
	}
}
