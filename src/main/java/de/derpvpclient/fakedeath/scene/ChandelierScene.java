package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** A lantern hangs on a chain from the ceiling right above the player. It creaks, sways, the chain snaps. */
public class ChandelierScene extends Scene {
	private static final int LINKS = 3;
	private static final int SNAP = 90;
	private static final int HIT = SNAP + 9;
	private final List<Display.BlockDisplay> chain = new ArrayList<>();
	private Display.BlockDisplay lantern;
	private Vec3 top;

	@Override
	public int length() {
		return HIT + 30;
	}

	@Override
	public int photoTick() {
		return SNAP + 4;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.kronleuchter", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			BlockPos head = player.blockPosition().above();
			int ceiling = 6;
			for (int i = 2; i <= 8; i++) {
				if (!level.getBlockState(head.above(i)).isAir()) {
					ceiling = i;
					break;
				}
			}
			top = Vec3.atBottomCenterOf(head.above(ceiling)).add(horizontalLook(player).scale(0.4));
			for (int i = 0; i < LINKS; i++) {
				Display.BlockDisplay link = blockDisplay(level, Looks.CHAIN, top, 4.0F);
				Looks.glow(link);
				chain.add(link);
			}
			lantern = blockDisplay(level, Blocks.GLOWSTONE.defaultBlockState(), top, 4.0F);
			Looks.glow(lantern);
		}

		float sway = tick < SNAP ? Mth.sin(tick * 0.2F) * (0.05F + tick / 900.0F) : 0.0F;
		Quaternionf tilt = new Quaternionf().rotateZ(sway);
		if (tick < SNAP) {
			for (int i = 0; i < LINKS; i++) {
				Vector3f down = new Vector3f(0.0F, -(i + 0.5F), 0.0F).rotate(tilt);
				place(chain.get(i), top.add(down.x, down.y, down.z), new Vector3f(1.0F), tilt);
			}
			Vector3f down = new Vector3f(0.0F, -(LINKS + 0.5F), 0.0F).rotate(tilt);
			place(lantern, top.add(down.x, down.y - 0.1, down.z), new Vector3f(0.8F), tilt);
			turnTowards(player, top.add(0.0, -LINKS, 0.0), 0.1F);
			if (tick % 18 == 0) {
				sound(level, top, SoundEvents.CHAIN_STEP, SoundSource.BLOCKS, 1.0F, 0.6F + tick / 200.0F);
			}
			if (tick > 50 && tick % 6 == 0) {
				particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Looks.CHAIN), top, 2, 0.2, 0.0);
			}
		}
		if (tick == SNAP) {
			sound(level, top, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.5F, 0.8F);
		}
		if (tick >= SNAP && tick <= HIT) {
			// Chain and lantern come down together.
			float p = (tick - SNAP) / (float) (HIT - SNAP);
			double drop = p * p * (top.y - LINKS - player.getEyePosition().y - 0.2);
			for (int i = 0; i < LINKS; i++) {
				place(chain.get(i), top.add(0.0, -(i + 0.5) - drop, 0.0), new Vector3f(1.0F), new Quaternionf().rotateZ(p * 0.8F));
			}
			place(lantern, top.add(0.0, -(LINKS + 0.8) - drop, 0.0), new Vector3f(0.8F), new Quaternionf().rotateZ(p * 0.5F));
			turnTowards(player, lantern.position(), 0.4F);
		}
		if (tick == HIT) {
			sound(level, player.position(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.5F, 0.8F);
			sound(level, player.position(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.8F, 1.4F);
			particles(level, ParticleTypes.FLAME, player.getEyePosition(), 15, 0.4, 0.05);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}
}
