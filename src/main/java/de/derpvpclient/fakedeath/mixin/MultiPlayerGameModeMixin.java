package de.derpvpclient.fakedeath.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The scene actors only exist on this client, hitting or clicking them must never reach the server. */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
	@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
	private void fakedeath$ignoreActorAttack(Player player, Entity target, CallbackInfo ci) {
		if (target.getId() < 0) {
			ci.cancel();
		}
	}

	@Inject(method = "interact", at = @At("HEAD"), cancellable = true)
	private void fakedeath$ignoreActorInteract(Player player, Entity target, EntityHitResult hitResult, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (target.getId() < 0) {
			cir.setReturnValue(InteractionResult.PASS);
		}
	}
}
