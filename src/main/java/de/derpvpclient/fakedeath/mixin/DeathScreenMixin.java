package de.derpvpclient.fakedeath.mixin;

import de.derpvpclient.fakedeath.FakeDeath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public class DeathScreenMixin {
	// The "Title screen" button: stay on the server, the fake is just over.
	@Inject(method = "handleExitToTitleScreen", at = @At("HEAD"), cancellable = true)
	private void fakedeath$stayInWorld(CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();

		if (FakeDeath.isActive() && minecraft.player != null && !minecraft.player.isDeadOrDying()) {
			FakeDeath.reveal(minecraft);
			ci.cancel();
		}
	}
}
