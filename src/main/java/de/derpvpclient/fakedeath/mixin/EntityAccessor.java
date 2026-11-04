package de.derpvpclient.fakedeath.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** On the client an entity glows by its synced flag, which client-only actors have to set themselves. */
@Mixin(Entity.class)
public interface EntityAccessor {
	@Invoker("setSharedFlag")
	void fakedeath$setSharedFlag(int flag, boolean value);
}
