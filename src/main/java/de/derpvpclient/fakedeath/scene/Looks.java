package de.derpvpclient.fakedeath.scene;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Blocks, sounds and moves the newer scenes use whose names change between Minecraft versions. Kept in one place, so
 * the ports to older versions only have to touch this file.
 */
final class Looks {
	static final BlockState BLACK_CONCRETE = Blocks.CONCRETE.black().defaultBlockState();
	static final BlockState WHITE_CONCRETE = Blocks.CONCRETE.white().defaultBlockState();
	static final BlockState LIGHT_BLUE_GLASS = Blocks.STAINED_GLASS.lightBlue().defaultBlockState();
	static final BlockState RED_WOOL = Blocks.WOOL.red().defaultBlockState();
	static final BlockState WHITE_WOOL = Blocks.WOOL.white().defaultBlockState();
	static final BlockState GRAY_CONCRETE = Blocks.CONCRETE.gray().defaultBlockState();

	static final SoundEvent EXPLODE = SoundEvents.GENERIC_EXPLODE.value();
	static final SoundEvent EAT = SoundEvents.GENERIC_EAT.value();
	static final SoundEvent SAW = SoundEvents.AXE_STRIP.value();
	static final SoundEvent CAVE = SoundEvents.AMBIENT_CAVE.value();
	static final SoundEvent CHICKEN = SoundEvent.createVariableRangeEvent(net.minecraft.resources.Identifier.withDefaultNamespace("entity.chicken.ambient"));
	static final SoundEvent CHICKEN_HURT = SoundEvent.createVariableRangeEvent(net.minecraft.resources.Identifier.withDefaultNamespace("entity.chicken.hurt"));
	static final BlockState CHAIN = Blocks.IRON_CHAIN.defaultBlockState();
	static final SoundEvent SADDLE = SoundEvents.HORSE_SADDLE.value();
	static final BlockState PURPLE_GLASS = Blocks.STAINED_GLASS.purple().defaultBlockState();
	/** The glowing tiles of the disco floor. */
	static final BlockState[] DISCO_FLOOR = {Blocks.CONCRETE.magenta().defaultBlockState(), Blocks.CONCRETE.lightBlue().defaultBlockState(),
		Blocks.CONCRETE.yellow().defaultBlockState(), Blocks.CONCRETE.lime().defaultBlockState(), Blocks.CONCRETE.purple().defaultBlockState()};
	/** Yellow like a wet floor warning sign, the same yellow as on the disco floor. */
	static final BlockState YELLOW_CONCRETE = DISCO_FLOOR[2];
	static final SoundEvent CAT_PURR = SoundEvent.createVariableRangeEvent(net.minecraft.resources.Identifier.withDefaultNamespace("entity.cat.purr"));
	static final SoundEvent CAT_PURREOW = SoundEvent.createVariableRangeEvent(net.minecraft.resources.Identifier.withDefaultNamespace("entity.cat.purreow"));
	static final SoundEvent PLING = SoundEvents.NOTE_BLOCK_PLING.value();

	private Looks() {
	}

	/** One of the mod's own sound events from its sounds.json. */
	static SoundEvent own(String path) {
		return SoundEvent.createVariableRangeEvent(net.minecraft.resources.Identifier.fromNamespaceAndPath("fakedeath", path));
	}

	/** Stops one of the mod's own sounds wherever it is still playing, long ones would run on past the scene. */
	static void stopOwn(String path) {
		net.minecraft.client.Minecraft.getInstance().getSoundManager().stop(net.minecraft.resources.Identifier.fromNamespaceAndPath("fakedeath", path), null);
	}

	/** A pointed dripstone hanging down, the tip or the thicker part above it. */
	static BlockState dripstone(boolean tip) {
		return Blocks.POINTED_DRIPSTONE.defaultBlockState()
			.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.VERTICAL_DIRECTION, net.minecraft.core.Direction.DOWN)
			.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SPELEOTHEM_THICKNESS,
				tip ? net.minecraft.world.level.block.state.properties.SpeleothemThickness.TIP : net.minecraft.world.level.block.state.properties.SpeleothemThickness.FRUSTUM);
	}

	/** Lets a display be drawn from far away, like the moon. */
	static void farView(net.minecraft.world.entity.Display display) {
		((de.derpvpclient.fakedeath.mixin.DisplayAccessor) display).fakedeath$setViewRange(16.0F);
	}

	/** Draws a display at full brightness, so it never shows up dark against the sky. */
	static void glow(net.minecraft.world.entity.Display display) {
		((de.derpvpclient.fakedeath.mixin.DisplayAccessor) display).fakedeath$setBrightnessOverride(net.minecraft.util.Brightness.FULL_BRIGHT);
	}

	/** Puts a saddle on a horse. */
	static void saddle(net.minecraft.world.entity.animal.equine.AbstractHorse horse) {
		horse.setItemSlot(net.minecraft.world.entity.EquipmentSlot.SADDLE, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE));
	}

	/** Up on its hind legs. */
	static void rear(net.minecraft.world.entity.animal.equine.AbstractHorse horse) {
		horse.setStanding(40);
	}

	/** Gives each cat of a crowd its own coat. */
	static void catColor(net.minecraft.world.entity.Entity cat, int i) {
		var coats = cat.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.CAT_VARIANT).listElements().toList();
		if (!coats.isEmpty()) {
			cat.setComponent(net.minecraft.core.component.DataComponents.CAT_VARIANT, coats.get(i % coats.size()));
		}
	}

	/** The snowy village coat, for Santa. */
	static void snowCoat(net.minecraft.world.entity.npc.villager.Villager villager, net.minecraft.client.multiplayer.ClientLevel level) {
		villager.setVillagerData(villager.getVillagerData().withType(level.registryAccess(), net.minecraft.world.entity.npc.villager.VillagerType.SNOW));
	}

	/** Bamboo crumbs, for the panda munching. */
	static net.minecraft.core.particles.ParticleOptions bambooCrumbs() {
		return new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, Blocks.BAMBOO_BLOCK.defaultBlockState());
	}

	/**
	 * One of the mod's own props, drawn with its own model and texture from the mod's assets. It is only a look: the
	 * item underneath stays a normal one, so the mod still needs nothing on the server.
	 */
	static net.minecraft.world.item.ItemStack prop(net.minecraft.world.item.Item base, String model) {
		net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(base);
		stack.set(net.minecraft.core.component.DataComponents.ITEM_MODEL, net.minecraft.resources.Identifier.fromNamespaceAndPath("fakedeath", model));
		return stack;
	}

	/** Feathers flying off, for the chickens. */
	static net.minecraft.core.particles.ParticleOptions feathers() {
		return new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM, net.minecraft.world.item.Items.FEATHER);
	}

	/** Bits of a sweet berry, for munching. */
	static net.minecraft.core.particles.ParticleOptions berryCrumbs() {
		return new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM, net.minecraft.world.item.Items.SWEET_BERRIES);
	}

	/** Swings the main hand, like hitting or throwing. */
	static void swing(LivingEntity entity) {
		entity.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
	}
}
