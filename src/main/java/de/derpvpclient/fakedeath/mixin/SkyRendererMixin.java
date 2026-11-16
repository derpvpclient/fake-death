package de.derpvpclient.fakedeath.mixin;

import de.derpvpclient.fakedeath.FakeDeath;
import de.derpvpclient.fakedeath.scene.Scene;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A scene can tint the sky, like the glitch turning it the magenta of a missing texture. Only on this client. */
@Mixin(SkyRenderer.class)
public class SkyRendererMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void fakedeath$skyTint(ClientLevel level, float partialTick, Camera camera, SkyRenderState state, CallbackInfo ci) {
		Scene scene = FakeDeath.currentScene();
		int tint = scene != null ? scene.skyTint() : 0;
		if (tint == 0 || state.skyColor == null) {
			return;
		}
		float share = (tint >>> 24) / 255.0F;
		state.skyColor = new Vector3f(
			Mth.lerp(share, state.skyColor.x(), ((tint >> 16) & 255) / 255.0F),
			Mth.lerp(share, state.skyColor.y(), ((tint >> 8) & 255) / 255.0F),
			Mth.lerp(share, state.skyColor.z(), (tint & 255) / 255.0F));
	}
}
