package de.derpvpclient.fakedeath.mixin;

import de.derpvpclient.fakedeath.CameraDirector;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Push-ups in third person: the body lies face down, the arms reach straight down to the floor, the legs stay straight. */
@Mixin(PlayerModel.class)
public class PlayerModelMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
	private void fakedeath$pushUp(AvatarRenderState state, CallbackInfo ci) {
		if (state != CameraDirector.pushUpState) {
			return;
		}
		PlayerModel model = (PlayerModel) (Object) this;
		model.rightArm.xRot = -Mth.HALF_PI;
		model.rightArm.yRot = 0.0F;
		model.rightArm.zRot = 0.12F;
		model.leftArm.xRot = -Mth.HALF_PI;
		model.leftArm.yRot = 0.0F;
		model.leftArm.zRot = -0.12F;
		model.rightLeg.xRot = 0.0F;
		model.rightLeg.yRot = 0.0F;
		model.rightLeg.zRot = 0.0F;
		model.leftLeg.xRot = 0.0F;
		model.leftLeg.yRot = 0.0F;
		model.leftLeg.zRot = 0.0F;
	}
}
