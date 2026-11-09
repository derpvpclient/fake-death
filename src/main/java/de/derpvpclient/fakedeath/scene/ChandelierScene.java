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

/**
 * A chandelier hangs on a chain from the ceiling right above the player: a golden hub, four arms, a burning candle on
 * each. It creaks and sways more and more, wax drips, then the chain snaps and it comes down candles first.
 */
public class ChandelierScene extends Scene {
	private static final int LINKS = 3;
	private static final int SNAP = 90;
	private static final int HIT = SNAP + 9;
	private final List<Display.BlockDisplay> chain = new ArrayList<>();
	private final List<Display.BlockDisplay> arms = new ArrayList<>();
	private final List<Display.BlockDisplay> candles = new ArrayList<>();
	private Display.BlockDisplay hub;
	private Vec3 center;
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
			hub = blockDisplay(level, Blocks.GOLD_BLOCK.defaultBlockState(), top, 4.0F);
			Looks.glow(hub);
			for (int k = 0; k < 4; k++) {
				Display.BlockDisplay arm = blockDisplay(level, Blocks.GOLD_BLOCK.defaultBlockState(), top, 4.0F);
				Looks.glow(arm);
				arms.add(arm);
				Display.BlockDisplay candle = blockDisplay(level, Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true), top, 4.0F);
				Looks.glow(candle);
				candles.add(candle);
			}
		}

		float sway = tick < SNAP ? Mth.sin(tick * 0.2F) * (0.05F + tick / 900.0F) : 0.0F;
		Quaternionf tilt = new Quaternionf().rotateZ(sway);
		if (tick < SNAP) {
			for (int i = 0; i < LINKS; i++) {
				Vector3f down = new Vector3f(0.0F, -(i + 0.5F), 0.0F).rotate(tilt);
				place(chain.get(i), top.add(down.x, down.y, down.z), new Vector3f(1.0F), tilt);
			}
			Vector3f down = new Vector3f(0.0F, -(LINKS + 0.2F), 0.0F).rotate(tilt);
			chandelier(level, top.add(down.x, down.y, down.z), tilt, tick);
			turnTowards(player, top.add(0.0, -LINKS, 0.0), 0.1F);
			if (tick > 40 && tick % 9 == 0) {
				// Wax drips down from the swaying candles.
				Vec3 drip = candles.get(random.nextInt(candles.size())).position().add(0.0, -0.55, 0.0);
				level.addParticle(ParticleTypes.FALLING_HONEY, drip.x, drip.y, drip.z, 0.0, 0.0, 0.0);
			}
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
			// Chain and chandelier come down together.
			float p = (tick - SNAP) / (float) (HIT - SNAP);
			double drop = p * p * (top.y - LINKS - player.getEyePosition().y - 0.2);
			for (int i = 0; i < LINKS; i++) {
				place(chain.get(i), top.add(0.0, -(i + 0.5) - drop, 0.0), new Vector3f(1.0F), new Quaternionf().rotateZ(p * 0.8F));
			}
			chandelier(level, top.add(0.0, -(LINKS + 0.2) - drop, 0.0), new Quaternionf().rotateZ(p * 0.5F).rotateX(p * 0.3F), tick);
			turnTowards(player, center, 0.4F);
		}
		if (tick == HIT) {
			sound(level, player.position(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.5F, 0.8F);
			sound(level, player.position(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.8F, 1.4F);
			particles(level, ParticleTypes.FLAME, player.getEyePosition(), 15, 0.4, 0.05);
			particles(level, ParticleTypes.SMOKE, player.getEyePosition(), 12, 0.5, 0.05);
			for (Display.BlockDisplay candle : candles) {
				despawn(level, candle);
			}
			candles.clear();
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	/** The hub, four golden arms and a burning candle on the end of each, all tilted together with the swing. */
	private void chandelier(ClientLevel level, Vec3 at, Quaternionf tilt, int tick) {
		center = at;
		place(hub, at, new Vector3f(0.34F, 0.22F, 0.34F), tilt);
		for (int k = 0; k < arms.size(); k++) {
			Quaternionf turn = new Quaternionf(tilt).rotateY(k * Mth.HALF_PI + 0.4F);
			Vector3f mid = new Vector3f(0.42F, 0.0F, 0.0F).rotate(turn);
			place(arms.get(k), at.add(mid.x, mid.y, mid.z), new Vector3f(0.84F, 0.07F, 0.07F), turn);
			if (k >= candles.size()) {
				continue;
			}
			// Standing on the end of the arm: the candle model sits at the bottom of its block, so the block is half a
			// block above the arm.
			Vector3f base = new Vector3f(0.8F, 0.04F, 0.0F).rotate(turn);
			Vector3f up = new Vector3f(0.0F, 0.5F, 0.0F).rotate(tilt);
			Vec3 foot = at.add(base.x, base.y, base.z);
			place(candles.get(k), foot.add(up.x, up.y, up.z), new Vector3f(1.0F), tilt);
			if ((tick + k) % 3 == 0) {
				Vector3f tip = new Vector3f(0.0F, 0.47F, 0.0F).rotate(tilt);
				level.addParticle(ParticleTypes.SMALL_FLAME, foot.x + tip.x, foot.y + tip.y, foot.z + tip.z, 0.0, 0.005, 0.0);
			}
		}
	}
}
