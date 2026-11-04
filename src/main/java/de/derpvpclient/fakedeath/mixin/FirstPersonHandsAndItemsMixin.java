package de.derpvpclient.fakedeath.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.derpvpclient.fakedeath.FakeDeath;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * While the giant item floats above the player, it is gone from their hand: the first-person view lowers it
 * like when switching items and raises an empty hand. Only the drawing, the inventory is untouched.
 */
@Mixin(ItemInHandRenderer.class)
public class FirstPersonHandsAndItemsMixin {
	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack fakedeath$hideMainHand(ItemStack stack) {
		return FakeDeath.hidesHeldItem(InteractionHand.MAIN_HAND) ? ItemStack.EMPTY : stack;
	}

	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getOffhandItem()Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack fakedeath$hideOffHand(ItemStack stack) {
		return FakeDeath.hidesHeldItem(InteractionHand.OFF_HAND) ? ItemStack.EMPTY : stack;
	}
}
