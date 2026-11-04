package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Like in a cartoon: something whistles above, the player looks up, and an anvil comes down on their head. */
public class AnvilScene extends Scene {
	private static final int DROP = 45;
	private static final double HEIGHT = 32.0;

	private FallingBlockEntity anvil;
	private int impact = -1;

	@Override
	public int length() {
		return impact >= 0 ? impact + 6 : DROP + 100;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.anvil", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		Vec3 above = player.position().add(0, HEIGHT, 0);

		if (tick == 0 || tick == 14 || tick == 28) {
			// High above something heavy hangs on a chain, and the chain creaks.
			sound(level, above, SoundEvents.CHAIN_STEP, SoundSource.BLOCKS, 4.0F, 0.5F);
		}

		if (tick == 38) {
			sound(level, above, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 4.0F, 0.6F);
		}

		if (tick == DROP) {
			// The chain snaps.
			sound(level, above, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 4.0F, 0.7F);
			anvil = fallingBlock(level, Blocks.ANVIL.defaultBlockState(), above, new Vec3(0.0, -0.1, 0.0));
		}

		if (anvil != null && impact < 0) {
			turnTowards(player, anvil.position().add(0, 0.5, 0), 0.25F);

			if (anvil.getY() <= player.getEyeY()) {
				impact = tick;
				sound(level, player.position(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 3.0F, 0.8F);
				sound(level, player.position(), SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 2.0F, 0.9F);
				particles(level, ParticleTypes.CRIT, player.getEyePosition(), 20, 0.5, 0.4);
				kill(level, player, SoundEvents.PLAYER_HURT);
			}
		}

		if (anvil == null && tick < DROP) {
			turnTowards(player, above, 0.08F);
		}
	}
}
