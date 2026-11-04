package de.derpvpclient.fakedeath.mixin;

import de.derpvpclient.fakedeath.FakeDeath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
	// The "Respawn" button: never send a respawn request for a fake death.
	@Inject(method = "respawn", at = @At("HEAD"), cancellable = true)
	private void fakedeath$skipRespawn(CallbackInfo ci) {
		if (FakeDeath.isActive() && !((LocalPlayer) (Object) this).isDeadOrDying()) {
			FakeDeath.reveal(Minecraft.getInstance());
			ci.cancel();
		}
	}
}
