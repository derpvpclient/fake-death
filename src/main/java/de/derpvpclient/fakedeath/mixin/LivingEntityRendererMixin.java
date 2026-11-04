package de.derpvpclient.fakedeath.mixin;

import de.derpvpclient.fakedeath.FakeDeath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
	// In third person the own player tips over and turns red like on a real death. Only on this client.
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
	private void fakedeath$deathPose(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {
		if (FakeDeath.isActive() && entity == Minecraft.getInstance().player) {
			state.deathTime = FakeDeath.deathTicks() + partialTicks;
			state.hasRedOverlay = true;
		}
	}
}
