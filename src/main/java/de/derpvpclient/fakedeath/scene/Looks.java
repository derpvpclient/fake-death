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
	static final BlockState CHAIN = Blocks.IRON_CHAIN.defaultBlockState();

	private Looks() {
	}

	/** Lets a display be drawn from far away, like the moon. */
	static void farView(net.minecraft.world.entity.Display display) {
		((de.derpvpclient.fakedeath.mixin.DisplayAccessor) display).fakedeath$setViewRange(16.0F);
	}

	/** Draws a display at full brightness, so it never shows up dark against the sky. */
	static void glow(net.minecraft.world.entity.Display display) {
		((de.derpvpclient.fakedeath.mixin.DisplayAccessor) display).fakedeath$setBrightnessOverride(net.minecraft.util.Brightness.FULL_BRIGHT);
	}

	/** Swings the main hand, like hitting or throwing. */
	static void swing(LivingEntity entity) {
		entity.swing(InteractionHand.MAIN_HAND);
	}
}
