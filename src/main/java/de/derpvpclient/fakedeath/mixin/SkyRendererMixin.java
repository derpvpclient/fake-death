package de.derpvpclient.fakedeath.mixin;

import de.derpvpclient.fakedeath.FakeDeath;
import de.derpvpclient.fakedeath.scene.Scene;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.util.Mth;
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
		if (tint == 0) {
			return;
		}
		float share = (tint >>> 24) / 255.0F;
		int sky = state.skyColor;
		int r = (int) Mth.lerp(share, (sky >> 16) & 255, (tint >> 16) & 255);
		int g = (int) Mth.lerp(share, (sky >> 8) & 255, (tint >> 8) & 255);
		int b = (int) Mth.lerp(share, sky & 255, tint & 255);
		state.skyColor = (sky & 0xFF000000) | r << 16 | g << 8 | b;
	}
}
