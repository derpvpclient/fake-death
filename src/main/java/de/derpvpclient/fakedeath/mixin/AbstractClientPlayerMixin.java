package de.derpvpclient.fakedeath.mixin;

import de.derpvpclient.fakedeath.scene.Skins;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The people in the scenes wear the mod's own skins, no real player's skin is looked up for them. */
@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {
	@Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
	private void fakedeath$ownSkin(CallbackInfoReturnable<PlayerSkin> cir) {
		PlayerSkin skin = Skins.of(((AbstractClientPlayer) (Object) this).getId());
		if (skin != null) {
			cir.setReturnValue(skin);
		}
	}
}
