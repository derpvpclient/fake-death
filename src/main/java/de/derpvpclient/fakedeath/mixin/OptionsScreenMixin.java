package de.derpvpclient.fakedeath.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import de.derpvpclient.fakedeath.FakeDeathOptionsScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
	protected OptionsScreenMixin(Component title) {
		super(title);
	}

	// A "Fake Death..." button at the end of the options grid, next to Music & Sounds, Video Settings and the rest.
	@Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToContents(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"))
	private void fakedeath$addSettingsButton(CallbackInfo ci, @Local GridLayout.RowHelper rows) {
		rows.addChild(Button.builder(Component.translatable("fakedeath.options.button"), button -> minecraft.gui.setScreen(new FakeDeathOptionsScreen(this))).build());
	}
}
