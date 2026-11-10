package de.derpvpclient.fakedeath.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scene actors are only pictures on this client: they never push the real player or anything else around. */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
	@Inject(method = "doPush", at = @At("HEAD"), cancellable = true)
	private void fakedeath$actorsPushNobody(Entity other, CallbackInfo ci) {
		if (((Entity) (Object) this).getId() < 0 || other.getId() < 0) {
			ci.cancel();
		}
	}
}
