package de.derpvpclient.fakedeath.mixin;

import net.minecraft.world.entity.animal.axolotl.Axolotl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The axolotl army comes in all five colors, the setter is private. */
@Mixin(Axolotl.class)
public interface AxolotlAccessor {
	@Invoker("setVariant")
	void fakedeath$setVariant(Axolotl.Variant variant);
}
