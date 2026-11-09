package de.derpvpclient.fakedeath.mixin;

import de.derpvpclient.fakedeath.CameraDirector;
import de.derpvpclient.fakedeath.FakeDeath;
import de.derpvpclient.fakedeath.scene.Scene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
	private void fakedeath$deathPose(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {
		if (entity != Minecraft.getInstance().player) {
			return;
		}

		// In third person the body goes where the scene took the camera and looks where the camera looks:
		// it rides the horse, hangs in the panda's paws, walks into the wall. Only on this client.
		Vec3 shift = CameraDirector.bodyOffset(entity);
		state.x += shift.x;
		state.y += shift.y;
		state.z += shift.z;
		state.bodyRot += CameraDirector.yawOffset();
		state.xRot = Mth.clamp(state.xRot + CameraDirector.pitchOffset(), -90.0F, 90.0F);

		Scene scene = FakeDeath.currentScene();
		Vec3 faces = scene != null ? scene.bodyFaces() : null;
		if (faces != null) {
			Vec3 delta = faces.subtract(entity.getPosition(partialTicks).add(shift));
			state.bodyRot = (float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90.0F;
			state.yRot = 0.0F;
			state.xRot = 0.0F;
		}
		if (scene != null && state instanceof HumanoidRenderState humanoid && scene.seated()) {
			humanoid.isPassenger = true;
		}
		if (scene != null && scene.walking()) {
			state.walkAnimationPos = CameraDirector.walkPos();
			state.walkAnimationSpeed = CameraDirector.walkSpeed();
		}

		// On the death screen it tips over and turns red like on a real death.
		if (FakeDeath.isActive()) {
			state.deathTime = FakeDeath.deathTicks() + partialTicks;
			state.hasRedOverlay = true;
		}
	}
}
