package de.derpvpclient.fakedeath.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.derpvpclient.fakedeath.FakeDeath;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Lets the vanilla death camera roll play, without the player really being dead. */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@ModifyExpressionValue(method = "bobHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isDeadOrDying()Z"))
	private boolean fakedeath$dying(boolean dying) {
		return dying || FakeDeath.isActive();
	}

	@ModifyExpressionValue(method = "bobHurt", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;deathTime:I"))
	private int fakedeath$deathTime(int deathTime) {
		return FakeDeath.isActive() ? FakeDeath.deathTicks() : deathTime;
	}
}
