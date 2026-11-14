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
		de.derpvpclient.fakedeath.scene.Skins.Pose pose = de.derpvpclient.fakedeath.scene.Skins.posedState(state);
		if (pose != null) {
			arms((PlayerModel) (Object) this, pose);
			return;
		}
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

	/** The arms of a person in a scene, posed after the game set them, so the pose wins. */
	private static void arms(PlayerModel model, de.derpvpclient.fakedeath.scene.Skins.Pose pose) {
		switch (pose) {
			case RAUTE -> {
				model.rightArm.xRot = -0.8F;
				model.rightArm.yRot = -0.55F;
				model.rightArm.zRot = 0.0F;
				model.leftArm.xRot = -0.8F;
				model.leftArm.yRot = 0.55F;
				model.leftArm.zRot = 0.0F;
			}
			case POINT -> {
				model.rightArm.xRot = -Mth.HALF_PI;
				model.rightArm.yRot = -0.1F;
				model.rightArm.zRot = 0.0F;
			}
			case CHEER -> {
				model.rightArm.xRot = -2.9F;
				model.rightArm.zRot = 0.25F;
				model.leftArm.xRot = -2.9F;
				model.leftArm.zRot = -0.25F;
			}
			case RAISE -> {
				model.rightArm.xRot = -2.4F;
				model.rightArm.yRot = 0.0F;
				model.rightArm.zRot = 0.1F;
			}
			case SALUTE -> {
				model.rightArm.xRot = -2.2F;
				model.rightArm.yRot = -0.6F;
				model.rightArm.zRot = 0.6F;
			}
		}
	}
}
