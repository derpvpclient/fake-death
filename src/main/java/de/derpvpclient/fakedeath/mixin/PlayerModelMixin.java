package de.derpvpclient.fakedeath.mixin;

import de.derpvpclient.fakedeath.FakeDeath;
import de.derpvpclient.fakedeath.scene.Scene;
import de.derpvpclient.fakedeath.scene.Skins;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Push-ups in third person, and the arm poses of the people in the scenes (hands together, pointing, cheering). */
@Mixin(PlayerModel.class)
public class PlayerModelMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void fakedeath$pushUp(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
		PlayerModel<?> model = (PlayerModel<?>) (Object) this;
		Skins.Pose pose = Skins.poseOf(entity.getId());
		if (pose != null) {
			arms(model, pose);
			model.rightSleeve.copyFrom(model.rightArm);
			model.leftSleeve.copyFrom(model.leftArm);
			return;
		}
		Scene scene = FakeDeath.currentScene();
		if (entity != Minecraft.getInstance().player || scene == null || !scene.pushUp()) {
			return;
		}
		model.rightArm.setRotation(-Mth.HALF_PI, 0.0F, 0.12F);
		model.leftArm.setRotation(-Mth.HALF_PI, 0.0F, -0.12F);
		model.rightLeg.setRotation(0.0F, 0.0F, 0.0F);
		model.leftLeg.setRotation(0.0F, 0.0F, 0.0F);
		// Before 1.21.2 the sleeves and trousers are separate parts that copy the limbs at the end.
		model.rightSleeve.copyFrom(model.rightArm);
		model.leftSleeve.copyFrom(model.leftArm);
		model.rightPants.copyFrom(model.rightLeg);
		model.leftPants.copyFrom(model.leftLeg);
	}

	private static void arms(PlayerModel<?> model, Skins.Pose pose) {
		switch (pose) {
			case RAUTE -> {
				model.rightArm.setRotation(-0.8F, -0.55F, 0.0F);
				model.leftArm.setRotation(-0.8F, 0.55F, 0.0F);
			}
			case POINT -> model.rightArm.setRotation(-Mth.HALF_PI, -0.1F, 0.0F);
			case CHEER -> {
				model.rightArm.setRotation(-2.9F, 0.0F, 0.25F);
				model.leftArm.setRotation(-2.9F, 0.0F, -0.25F);
			}
			case RAISE -> model.rightArm.setRotation(-2.4F, 0.0F, 0.1F);
			case SALUTE -> model.rightArm.setRotation(-2.2F, -0.6F, 0.6F);
		}
	}
}
