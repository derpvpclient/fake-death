package de.derpvpclient.fakedeath.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.derpvpclient.fakedeath.CameraDirector;
import de.derpvpclient.fakedeath.FakeDeath;
import de.derpvpclient.fakedeath.scene.Scene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
	// In third person the body goes where the scene took the camera: it rides the horse, hangs in the panda's paws,
	// walks into the wall. Only on this client.
	@Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
	private void fakedeath$bodyFollows(LivingEntity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
		if (entity == Minecraft.getInstance().player) {
			Scene scene = FakeDeath.currentScene();
			if (scene != null && scene.hidesBody()) {
				// Swallowed, erased: the body is gone in third person.
				ci.cancel();
				return;
			}
			Vec3 shift = CameraDirector.bodyOffset(entity);
			poseStack.translate(shift.x, shift.y, shift.z);
			if (scene != null && scene.bodyScale() != 1.0F) {
				poseStack.scale(scene.bodyScale(), scene.bodyScale(), scene.bodyScale());
			}
		}
	}

	// Some people in the scenes carry no name over the head, like the extras, or a sign of their own instead.
	@Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
	private void fakedeath$noName(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
		if (de.derpvpclient.fakedeath.scene.Skins.hidesName(entity.getId())) {
			cir.setReturnValue(false);
		}
	}

	// And it turns where the camera looks.
	@WrapOperation(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V"))
	private void fakedeath$bodyTurns(LivingEntityRenderer<?, ?> renderer, LivingEntity entity, PoseStack poseStack, float bob, float bodyRot, float partialTicks, float scale, Operation<Void> original) {
		float turned = bodyRot;
		if (entity == Minecraft.getInstance().player) {
			turned += CameraDirector.yawOffset();
			Scene scene = FakeDeath.currentScene();
			Vec3 faces = scene != null ? scene.bodyFaces() : null;
			if (faces != null) {
				Vec3 delta = faces.subtract(entity.getPosition(partialTicks).add(CameraDirector.bodyOffset(entity)));
				turned = (float) (net.minecraft.util.Mth.atan2(delta.z, delta.x) * net.minecraft.util.Mth.RAD_TO_DEG) - 90.0F;
			}
		}
		original.call(renderer, entity, poseStack, bob, turned, partialTicks, scale);
	}

	// Sitting on the horse of the riding lesson.
	@WrapOperation(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isPassenger()Z", ordinal = 0))
	private boolean fakedeath$seated(LivingEntity entity, Operation<Boolean> original) {
		Scene scene = FakeDeath.currentScene();
		return original.call(entity) || entity == Minecraft.getInstance().player && scene != null && scene.seated();
	}

	// In third person the own player tips over and turns red like on a real death. Only on this client.
	@WrapOperation(method = "setupRotations", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;deathTime:I"))
	private int fakedeath$deathPose(LivingEntity entity, Operation<Integer> original) {
		return fakeDeathTime(entity, original);
	}

	@WrapOperation(method = "getOverlayCoords", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;deathTime:I"))
	private static int fakedeath$redOverlay(LivingEntity entity, Operation<Integer> original) {
		return fakeDeathTime(entity, original);
	}

	private static int fakeDeathTime(LivingEntity entity, Operation<Integer> original) {
		if (FakeDeath.isActive() && entity == Minecraft.getInstance().player) {
			return Math.max(1, FakeDeath.deathTicks());
		}
		return original.call(entity);
	}
}
