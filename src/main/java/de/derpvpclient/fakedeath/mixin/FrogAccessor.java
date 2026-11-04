package de.derpvpclient.fakedeath.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.FrogVariant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The frog plague mixes all three frog colors, the setter is private. */
@Mixin(Frog.class)
public interface FrogAccessor {
	@Invoker("setVariant")
	void fakedeath$setVariant(Holder<FrogVariant> variant);
}
