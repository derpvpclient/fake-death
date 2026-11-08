package de.derpvpclient.fakedeath.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.derpvpclient.fakedeath.FakeDeath;
import de.derpvpclient.fakedeath.scene.Scene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The player's own body lies face down in third person when a scene throws it to the ground, like crawling. */
@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {
	@WrapOperation(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;getSwimAmount(F)F"))
	private float fakedeath$tipOver(AbstractClientPlayer player, float partialTick, Operation<Float> original) {
		float lying = lying(player);
		return lying > 0.0F ? lying : original.call(player, partialTick);
	}

	@WrapOperation(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isVisuallySwimming()Z"))
	private boolean fakedeath$lieDown(AbstractClientPlayer player, Operation<Boolean> original) {
		return lying(player) >= 1.0F || original.call(player);
	}

	private static float lying(AbstractClientPlayer player) {
		Scene scene = FakeDeath.currentScene();
		return player == Minecraft.getInstance().player && scene != null ? scene.lying() : 0.0F;
	}
}
