package de.derpvpclient.fakedeath.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The spell type is hidden, but the client only reads the synced spell id: raised arms and spell particles. */
@Mixin(SpellcasterIllager.class)
public interface SpellcasterIllagerAccessor {
	@Accessor("DATA_SPELL_CASTING_ID")
	static EntityDataAccessor<Byte> fakedeath$spellData() {
		throw new AssertionError();
	}
}
