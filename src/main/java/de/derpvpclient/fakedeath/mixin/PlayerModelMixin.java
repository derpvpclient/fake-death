package de.derpvpclient.fakedeath.mixin;

import de.derpvpclient.fakedeath.FakeDeath;
import de.derpvpclient.fakedeath.scene.Scene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Push-ups in third person: the body lies face down, the arms reach straight down to the floor, the legs stay straight. */
@Mixin(PlayerModel.class)
public class PlayerModelMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void fakedeath$pushUp(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
		Scene scene = FakeDeath.currentScene();
		if (entity != Minecraft.getInstance().player || scene == null || !scene.pushUp()) {
			return;
		}
		PlayerModel<?> model = (PlayerModel<?>) (Object) this;
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
}
