package de.derpvpclient.fakedeath.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.derpvpclient.fakedeath.CameraDirector;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	public abstract Vec3 getPosition();

	@Shadow
	protected abstract void setPosition(Vec3 position);

	@Inject(method = "setup", at = @At("HEAD"))
	private void fakedeath$rememberPartialTick(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTicks, CallbackInfo ci) {
		CameraDirector.setPartialTick(partialTicks);
	}

	// Some scenes pull the camera away from the eyes, like the giant frog's tongue. Only the camera moves.
	@Inject(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", shift = At.Shift.AFTER))
	private void fakedeath$sceneOffset(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTicks, CallbackInfo ci) {
		Vec3 offset = CameraDirector.positionOffset();

		if (offset.lengthSqr() > 1.0E-6) {
			setPosition(getPosition().add(offset));
		}
	}

	// The scenes turn the camera, not the player, so the server never gets a rotation from them.
	@ModifyExpressionValue(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
	private float fakedeath$sceneYaw(float yaw) {
		return yaw + CameraDirector.yawOffset();
	}

	@ModifyExpressionValue(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
	private float fakedeath$scenePitch(float pitch) {
		return Mth.clamp(pitch + CameraDirector.pitchOffset(), -90.0F, 90.0F);
	}
}
