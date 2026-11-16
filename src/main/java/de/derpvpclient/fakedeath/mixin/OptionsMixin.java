package de.derpvpclient.fakedeath.mixin;

import de.derpvpclient.fakedeath.FakeDeath;
import de.derpvpclient.fakedeath.scene.Scene;
import net.minecraft.client.CameraType;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * F5 does nothing while a scene made for the eyes plays: the camera never leaves first person, not even for the
 * moment until the scene's next tick would switch it back.
 */
@Mixin(Options.class)
public class OptionsMixin {
	@Inject(method = "setCameraType", at = @At("HEAD"), cancellable = true)
	private void fakedeath$lockFirstPerson(CameraType type, CallbackInfo ci) {
		Scene scene = FakeDeath.currentScene();
		if (!type.isFirstPerson() && scene != null && (scene.firstPersonOnly() || FakeDeath.firstPersonLocked())) {
			ci.cancel();
		}
	}
}
