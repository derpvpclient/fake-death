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
	private static final double HEIGHT = 8.0;

	private FallingBlockEntity anvil;
	private net.minecraft.world.entity.Display.BlockDisplay hanging;
	private net.minecraft.world.entity.Display.BlockDisplay chain;
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
		// A little in front, so it does not hide in the glare of the sun right overhead.
		Vec3 above = player.position().add(horizontalLook(player).scale(2.2)).add(0, HEIGHT, 0);
		if (tick == 0) {
			// The anvil can be seen hanging up there on its chain the whole time.
			hanging = blockDisplay(level, Blocks.ANVIL.defaultBlockState(), above, 3.0F);
			chain = blockDisplay(level, Looks.CHAIN, above, 3.0F);
		}
		if (hanging != null && tick < DROP) {
			float sway = (float) Math.sin(tick * 0.25) * (0.05F + tick / 500.0F);
			place(hanging, above.add(0.0, 0.5, 0.0), new org.joml.Vector3f(1.5F), new org.joml.Quaternionf().rotateZ(sway));
			place(chain, above.add(0.0, 2.8, 0.0), new org.joml.Vector3f(1.0F, 3.0F, 1.0F), new org.joml.Quaternionf().rotateZ(sway));
			// Its shadow on the ground, a dark ring that grows while it creaks.
			if (tick % 2 == 0) {
				double r = 0.4 + tick / (double) DROP * 0.6;
				for (int i = 0; i < 8; i++) {
					double a = i * Math.PI / 4 + tick * 0.1;
					level.addParticle(ParticleTypes.SMOKE, player.getX() + Math.cos(a) * r, player.getY() + 0.05, player.getZ() + Math.sin(a) * r, 0, 0, 0);
				}
			}
		}
		if (tick == DROP && chain != null) {
			despawn(level, chain);
		}
		if (hanging != null && tick >= DROP && impact < 0) {
			// Falling faster and faster at an angle, right onto the head, tumbling a little.
			int t = tick - DROP;
			Vec3 head = player.getEyePosition().add(0.0, 0.4, 0.0);
			double drop = 0.04 * t * t;
			double p = Math.min(1.0, drop / (above.y + 0.5 - head.y));
			Vec3 at = above.add(0.0, 0.5, 0.0).lerp(head, p);
			place(hanging, at, new org.joml.Vector3f(1.5F), new org.joml.Quaternionf().rotateZ(t * 0.06F).rotateX(t * 0.03F));
			turnTowards(player, at, 0.3F);
			if (p >= 1.0) {
				impact = tick;
				sound(level, player.position(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 3.0F, 0.8F);
				sound(level, player.position(), SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 2.0F, 0.9F);
				particles(level, ParticleTypes.CRIT, player.getEyePosition(), 30, 0.6, 0.4);
				particles(level, new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, Blocks.ANVIL.defaultBlockState()), player.getEyePosition(), 25, 0.5, 0.3);
				kill(level, player, SoundEvents.PLAYER_HURT);
			}
		}

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
