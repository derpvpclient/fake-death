package de.derpvpclient.fakedeath.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.derpvpclient.fakedeath.FakeDeath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
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
