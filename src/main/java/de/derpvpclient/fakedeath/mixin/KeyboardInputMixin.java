package de.derpvpclient.fakedeath.mixin;

import de.derpvpclient.fakedeath.FakeDeath;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While a scene plays the player stands still, like frozen with fear: no walking away from the ghast
 * or the tax office. Only the keys are ignored on this client, the server just sees someone standing.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
	@Inject(method = "tick", at = @At("TAIL"))
	private void fakedeath$freeze(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
		if (FakeDeath.freezesMovement()) {
			up = false;
			down = false;
			left = false;
			right = false;
			jumping = false;
			shiftKeyDown = false;
			leftImpulse = 0.0F;
			forwardImpulse = 0.0F;
		}
	}
}
