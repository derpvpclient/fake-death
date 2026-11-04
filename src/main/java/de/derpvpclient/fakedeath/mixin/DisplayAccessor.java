package de.derpvpclient.fakedeath.mixin;

import com.mojang.math.Transformation;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Display entities are normally only changed by the server, the scenes animate their own client-only ones. */
@Mixin(Display.class)
public interface DisplayAccessor {
	@Invoker("setTransformation")
	void fakedeath$setTransformation(Transformation transformation);

	@Invoker("setTransformationInterpolationDuration")
	void fakedeath$setInterpolationDuration(int ticks);

	@Invoker("setTransformationInterpolationDelay")
	void fakedeath$setInterpolationDelay(int ticks);

	@Invoker("setWidth")
	void fakedeath$setWidth(float width);

	@Invoker("setBillboardConstraints")
	void fakedeath$setBillboard(Display.BillboardConstraints constraints);

	@Invoker("setViewRange")
	void fakedeath$setViewRange(float range);

	@Invoker("setBrightnessOverride")
	void fakedeath$setBrightnessOverride(Brightness brightness);

	@Invoker("setGlowColorOverride")
	void fakedeath$setGlowColorOverride(int color);

	@Invoker("setHeight")
	void fakedeath$setHeight(float height);
}
