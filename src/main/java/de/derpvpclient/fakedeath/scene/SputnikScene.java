package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
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
 * October 1957. A faint "beep … beep … beep" from the sky. Radio Moscow proudly announces the first artificial
 * satellite of the Earth. A second message: it has, unfortunately, left its orbit. The beeping gets faster and louder,
 * a glowing dot up there grows into a shining ball with four long antennas, trailing fire, and it comes down right on
 * the player.
 */
public class SputnikScene extends Scene {
	private static final int ANNOUNCE = 30;
	private static final int ORBIT = 95;
	private static final int FALL = 120;
	private static final int FALL_TICKS = 90;
	private static final int HIT = FALL + FALL_TICKS;
	private static final ChatFormatting RADIO = ChatFormatting.RED;

	private final List<Display.BlockDisplay> parts = new ArrayList<>();
	private Vec3 sky;
	private Vec3 at;
	private int nextBeep;
	private boolean high;
	private float flash;

	@Override
	public int length() {
		return HIT + 2;
	}

	@Override
	public int photoTick() {
		return HIT - 20;
	}

	@Override
	public int screenTint() {
		return flash > 0.0F ? ((int) (Math.min(flash, 1.0F) * 255.0F) << 24) | 0xFFFFFF : 0;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.sputnik", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		Vec3 eyes = player.getEyePosition();
		Vec3 look = horizontalLook(player);
		if (tick == 0) {
			// High up in front, a little to the side.
			sky = eyes.add(look.scale(22.0)).add(look.yRot(Mth.HALF_PI).scale(8.0)).add(0.0, 34.0, 0.0);
			at = sky;
			parts.add(blockDisplay(level, Blocks.IRON_BLOCK.defaultBlockState(), sky, 64.0F));
			for (int i = 0; i < 4; i++) {
				parts.add(blockDisplay(level, Blocks.IRON_BARS.defaultBlockState(), sky, 64.0F));
			}
			for (Display.BlockDisplay part : parts) {
				Looks.glow(part);
				((de.derpvpclient.fakedeath.mixin.DisplayAccessor) part).fakedeath$setViewRange(8.0F);
			}
			nextBeep = 8;
		}
		if (parts.isEmpty()) {
			return;
		}

		// The famous beeping: slow and faint at first, faster and louder as it comes down.
		if (tick == nextBeep && tick < HIT) {
			float closeness = tick < FALL ? 0.0F : (tick - FALL) / (float) FALL_TICKS;
			sound(level, eyes.add(at.subtract(eyes).normalize().scale(4.0)), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.NEUTRAL, 0.4F + closeness * 1.4F, high ? 1.6F : 1.2F);
			high = !high;
			nextBeep = tick + Math.max(2, (int) (14 - closeness * 12));
		}
		if (tick == ANNOUNCE) {
			say(minecraft, Component.translatable("fakedeath.sputnik.radio").withStyle(RADIO), Component.translatable("fakedeath.sputnik.launched"));
			sound(level, eyes, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.NEUTRAL, 0.8F, 1.0F);
		}
		if (tick == ORBIT) {
			say(minecraft, Component.translatable("fakedeath.sputnik.radio").withStyle(RADIO), Component.translatable("fakedeath.sputnik.orbit"));
			sound(level, eyes, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.NEUTRAL, 0.8F, 0.7F);
		}

		Vec3 target = eyes.add(0.0, 0.2, 0.0);
		if (tick < FALL) {
			// Up there, crossing the sky slowly.
			at = sky.add(look.yRot(Mth.HALF_PI).scale(-tick * 0.05));
		} else if (tick <= HIT) {
			float p = (tick - FALL) / (float) FALL_TICKS;
			Vec3 from = sky.add(look.yRot(Mth.HALF_PI).scale(-FALL * 0.05));
			at = from.lerp(target, p * p);
			// Burning up in the air on the way down.
			Vec3 back = from.subtract(target).normalize();
			for (int i = 0; i < 4; i++) {
				Vec3 trail = at.add(back.scale(0.6 + random.nextDouble() * 1.2));
				level.addParticle(i % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.LARGE_SMOKE, trail.x, trail.y, trail.z, 0.0, 0.02, 0.0);
			}
			if (tick % 12 == 0) {
				sound(level, at, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 2.0F, 0.6F);
			}
		}
		// It sparkles in the sun, so it can be seen from far away.
		level.addParticle(ParticleTypes.END_ROD, at.x, at.y, at.z, 0.0, 0.0, 0.0);
		// The shining ball and its four antennas, swept back.
		Vec3 back = sky.subtract(target).normalize();
		float spin = tick * 0.05F;
		place(parts.get(0), at, new Vector3f(1.5F), new Quaternionf().rotateY(spin).rotateX(spin * 0.7F));
		for (int i = 0; i < 4; i++) {
			Vec3 out = back.yRot(i * Mth.HALF_PI + 0.4F).add(back.scale(2.0)).normalize();
			Vec3 mid = at.add(out.scale(1.8));
			Quaternionf along = new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), new Vector3f((float) out.x, (float) out.y, (float) out.z));
			place(parts.get(1 + i), mid, new Vector3f(0.09F, 3.0F, 0.09F), along);
		}
		turnTowards(player, at, tick < FALL ? 0.04F : 0.2F);
		if (tick == HIT) {
			flash = 1.0F;
			sound(level, eyes, Looks.EXPLODE, SoundSource.HOSTILE, 2.0F, 0.8F);
			sound(level, eyes, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.5F, 0.6F);
			particles(level, ParticleTypes.EXPLOSION, eyes, 6, 0.6, 0.1);
			cameraShake(0.5F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}
}
