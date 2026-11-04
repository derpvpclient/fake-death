package de.derpvpclient.fakedeath.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.derpvpclient.fakedeath.FakeDeath;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class HudMixin {
	// Some scenes cover the world in a color, drawn first so hearts and hotbar stay on top of it.
	@Inject(method = "render", at = @At("HEAD"))
	private void fakedeath$tint(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		int tint = FakeDeath.screenTint();

		if ((tint >>> 24) != 0) {
			graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), tint);
		}
	}

	// The hearts show the fake health during a scene. Only the drawing, the real health is untouched.
	@ModifyExpressionValue(method = "renderPlayerHealth", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getHealth()F"))
	private float fakedeath$fakeHearts(float health) {
		return FakeDeath.displayedHealth(health);
	}

	// The same for the hunger bar while starving: it runs empty, and without saturation the icons shake.
	@ModifyExpressionValue(method = "renderFood", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;getFoodLevel()I"))
	private int fakedeath$fakeFood(int food) {
		return FakeDeath.displayedFood(food);
	}

	@ModifyExpressionValue(method = "renderFood", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;getSaturationLevel()F"))
	private float fakedeath$fakeSaturation(float saturation) {
		return FakeDeath.displayedSaturation(saturation);
	}
}
