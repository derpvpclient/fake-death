package de.derpvpclient.fakedeath.mixin;

import de.derpvpclient.fakedeath.FakeDeath;
import de.derpvpclient.fakedeath.scene.Scene;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.util.Mth;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The horizon goes along with a tinted sky, otherwise a band of the normal blue would stay at the bottom of it. */
@Mixin(FogRenderer.class)
public class FogRendererMixin {
	@Inject(method = "computeFogColor", at = @At("TAIL"))
	private void fakedeath$fogTint(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darken, Vector4f color, CallbackInfo ci) {
		Scene scene = FakeDeath.currentScene();
		int tint = scene != null ? scene.skyTint() : 0;
		if (tint == 0) {
			return;
		}
		float share = (tint >>> 24) / 255.0F;
		color.set(
			Mth.lerp(share, color.x(), ((tint >> 16) & 255) / 255.0F),
			Mth.lerp(share, color.y(), ((tint >> 8) & 255) / 255.0F),
			Mth.lerp(share, color.z(), (tint & 255) / 255.0F),
			color.w());
	}
}
