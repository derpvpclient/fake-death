package de.derpvpclient.fakedeath.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Text displays get their text from the server, the scenes' own speech bubbles set it themselves. */
@Mixin(Display.TextDisplay.class)
public interface TextDisplayAccessor {
	@Invoker("setText")
	void fakedeath$setText(Component text);

	@Invoker("setLineWidth")
	void fakedeath$setLineWidth(int width);

	@Invoker("setBackgroundColor")
	void fakedeath$setBackgroundColor(int color);
}
