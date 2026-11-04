package de.derpvpclient.fakedeath.mixin;

import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Item displays only get their item from the server, the client-only giant item sets it itself. */
@Mixin(Display.ItemDisplay.class)
public interface ItemDisplayAccessor {
	@Invoker("setItemStack")
	void fakedeath$setItemStack(ItemStack stack);

	@Invoker("setItemTransform")
	void fakedeath$setItemTransform(ItemDisplayContext context);
}
