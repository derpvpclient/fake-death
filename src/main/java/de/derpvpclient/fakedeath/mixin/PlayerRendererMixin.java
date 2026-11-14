package de.derpvpclient.fakedeath.mixin;

import de.derpvpclient.fakedeath.CameraDirector;
import de.derpvpclient.fakedeath.FakeDeath;
import de.derpvpclient.fakedeath.scene.Scene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The player's own pose in third person, set after the game has filled in its own, like swimming or flying. */
@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V", at = @At("TAIL"))
	private void fakedeath$lying(AbstractClientPlayer entity, PlayerRenderState state, float partialTicks, CallbackInfo ci) {
		// The people in the scenes wear the mod's own skins, some without a name over the head.
		net.minecraft.client.resources.PlayerSkin skin = de.derpvpclient.fakedeath.scene.Skins.of(entity.getId());
		if (skin != null) {
			state.skin = skin;
			if (de.derpvpclient.fakedeath.scene.Skins.hidesName(entity.getId())) {
				state.nameTag = null;
			}
		}
		de.derpvpclient.fakedeath.scene.Skins.posed(state, de.derpvpclient.fakedeath.scene.Skins.poseOf(entity.getId()));
		Scene scene = FakeDeath.currentScene();
		if (entity == Minecraft.getInstance().player) {
			CameraDirector.pushUpState = scene != null && scene.pushUp() ? state : null;
		}
		if (entity == Minecraft.getInstance().player && scene != null && scene.hidesBody()) {
			state.isInvisible = true;
			state.isInvisibleToPlayer = true;
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
