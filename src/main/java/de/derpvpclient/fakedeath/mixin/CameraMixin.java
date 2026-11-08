package de.derpvpclient.fakedeath.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.derpvpclient.fakedeath.CameraDirector;
import de.derpvpclient.fakedeath.FakeDeath;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.state.level.CameraEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	public abstract Vec3 position();

	@Shadow
	protected abstract void setPosition(Vec3 position);

	// Some scenes pull the camera away from the eyes, like the giant frog's tongue. Only the camera moves.
	@Inject(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", shift = At.Shift.AFTER))
	private void fakedeath$sceneOffset(float partialTicks, CallbackInfo ci) {
		Vec3 offset = CameraDirector.positionOffset();

		if (offset.lengthSqr() > 1.0E-6) {
			setPosition(position().add(offset));
		}
	}

	// Lets the vanilla death camera roll play, without the player really being dead.
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void fakedeath$deathRoll(CameraRenderState state, float partialTicks, CallbackInfo ci) {
		CameraEntityRenderState entity = state.entityRenderState;

		if (FakeDeath.isActive() && entity != null && entity.isLiving) {
			entity.isDeadOrDying = true;
			entity.deathTime = FakeDeath.deathTicks() + partialTicks;
		}
	}

	@Inject(method = "alignWithEntity", at = @At("HEAD"))
	private void fakedeath$rememberPartialTick(float partialTicks, CallbackInfo ci) {
		CameraDirector.setPartialTick(partialTicks);
	}

	// The scenes turn the camera, not the player, so the server never gets a rotation from them.
	// Hooked where the view rotation is read, the front view (F5 twice) then mirrors the turned value once.
	@ModifyExpressionValue(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
	private float fakedeath$sceneYaw(float yaw) {
		return yaw + CameraDirector.yawOffset();
	}

	@ModifyExpressionValue(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
	private float fakedeath$scenePitch(float pitch) {
		return Mth.clamp(pitch + CameraDirector.pitchOffset(), -90.0F, 90.0F);
	}
}
