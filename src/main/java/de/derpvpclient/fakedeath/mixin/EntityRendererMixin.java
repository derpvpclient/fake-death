package de.derpvpclient.fakedeath.mixin;

import de.derpvpclient.fakedeath.scene.Skins;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Some people in the scenes carry no name over the head, like Herobrine, or a sign of their own instead. */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
	@Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
	private void fakedeath$noName(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (Skins.hidesName(entity.getId())) {
			cir.setReturnValue(false);
		}
	}
}
