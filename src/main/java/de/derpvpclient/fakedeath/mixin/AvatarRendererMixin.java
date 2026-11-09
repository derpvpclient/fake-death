package de.derpvpclient.fakedeath.mixin;

import de.derpvpclient.fakedeath.CameraDirector;
import de.derpvpclient.fakedeath.FakeDeath;
import de.derpvpclient.fakedeath.scene.Scene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The player's own pose in third person, set after the game has filled in its own, like swimming or flying. */
@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
	private void fakedeath$lying(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
		Scene scene = FakeDeath.currentScene();
		if (entity == Minecraft.getInstance().player) {
			CameraDirector.pushUpState = scene != null && scene.pushUp() ? state : null;
		}
		if (entity != Minecraft.getInstance().player || scene == null || scene.lying() <= 0.0F) {
			return;
		}
		// Face down on the ground like crawling, arms and legs struggling.
		state.swimAmount = scene.lying();
		state.isVisuallySwimming = scene.lying() >= 1.0F;
		state.walkAnimationPos = state.ageInTicks * 0.8F;
		state.walkAnimationSpeed = 0.5F;
		if (scene.pushUp()) {
			state.walkAnimationSpeed = 0.0F;
		}
	}
}
