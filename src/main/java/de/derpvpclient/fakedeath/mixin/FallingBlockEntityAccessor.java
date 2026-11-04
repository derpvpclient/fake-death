package de.derpvpclient.fakedeath.mixin;

import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FallingBlockEntity.class)
public interface FallingBlockEntityAccessor {
	// The public factory places the block into the world and spawns the entity there, the scenes only want the entity.
	@Invoker("<init>")
	static FallingBlockEntity fakedeath$create(Level level, double x, double y, double z, BlockState state) {
		throw new AssertionError();
	}
}
