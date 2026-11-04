package de.derpvpclient.fakedeath.scene;

import com.mojang.math.Transformation;
import de.derpvpclient.fakedeath.mixin.DisplayAccessor;
import de.derpvpclient.fakedeath.mixin.EntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A clear night: a light appears in the sky, grows, glows orange and comes closer and closer
 * until a glowing rock hits right next to the player in a white flash.
 */
public class MeteorScene extends Scene {
	private static final int FALL = 160;
	private static final double DISTANCE = 45.0;
	private static final double HEIGHT = 40.0;
	private static final float SIZE = 2.6F;
	private static final int FLASH = 35;
	private static final int DEATH = FALL + 14;
	private static final int GLOW = 0xFF7A20;
	private static final int GLOWING_FLAG = 6;

	private Display.BlockDisplay rock;
	private Vec3 start;
	private Vec3 end;
	private int tint;

	@Override
	public int length() {
		return DEATH;
	}

	@Override
	public int photoTick() {
		return FALL - 25;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.meteor", player.getDisplayName());
	}

	@Override
	public int screenTint() {
		return tint;
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			Vec3 look = horizontalLook(player).yRot((float) Math.toRadians((random.nextDouble() - 0.5) * 70.0));
			start = player.position().add(look.scale(DISTANCE)).add(0.0, HEIGHT, 0.0);
			end = player.position().add(look.scale(1.5)).add(0.0, 0.3, 0.0);
			rock = blockDisplay(level, Blocks.MAGMA_BLOCK.defaultBlockState(), start, SIZE * 2.0F);
			// Visible from far away: drawn up to 256 blocks, glowing in full brightness with an orange outline.
			((DisplayAccessor) rock).fakedeath$setViewRange(4.0F);
			((DisplayAccessor) rock).fakedeath$setBrightnessOverride(Brightness.FULL_BRIGHT);
			((DisplayAccessor) rock).fakedeath$setGlowColorOverride(GLOW);
			((EntityAccessor) rock).fakedeath$setSharedFlag(GLOWING_FLAG, true);
			sound(level, start, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.0F, 0.5F);
		}

		if (tick < FALL && rock != null) {
			float t = tick / (float) FALL;
			// Slow far away, then faster and faster.
			float eased = (float) Math.pow(t, 2.4);
			Vec3 pos = start.lerp(end, eased);
			rock.setPos(pos.x, pos.y, pos.z);

			Quaternionf spin = new Quaternionf().rotateXYZ(tick * 0.11F, tick * 0.07F, tick * 0.05F);
			Vector3f center = new Vector3f(-SIZE / 2.0F).rotate(spin);
			transform(rock, new Transformation(center, spin, new Vector3f(SIZE), new Quaternionf()));

			// A fire trail that is also seen from far away, normal particles vanish beyond 32 blocks.
			trail(level, ParticleTypes.FLAME, pos, 8, SIZE * 0.4, 0.05);
			trail(level, ParticleTypes.LARGE_SMOKE, pos, 4, SIZE * 0.4, 0.02);
			if (tick % 2 == 0) {
				trail(level, ParticleTypes.END_ROD, pos, 2, SIZE * 0.3, 0.01);
			}
			if (tick % 3 == 0) {
				trail(level, ParticleTypes.LAVA, pos, 1, SIZE * 0.3, 0.0);
			}

			if (tick % 12 == 0) {
				sound(level, pos, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.0F + eased * 4.0F, 0.5F);
			}
			if (tick == 60 || tick == 110 || tick == 140) {
				sound(level, pos, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 3.0F, 0.4F + tick / 400.0F);
			}

			// The sky glows orange the closer it gets.
			tint = (int) (eased * 90.0F) << 24 | GLOW;
			turnTowards(player, pos, 0.08F + eased * 0.3F);
		}

		if (tick == FALL) {
			despawn(level, rock);
			rock = null;
			sound(level, end, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4.0F, 0.6F);
			particles(level, ParticleTypes.EXPLOSION_EMITTER, end, 2, 1.0, 0.0);
			particles(level, ParticleTypes.EXPLOSION, end, 10, 2.0, 0.0);
			particles(level, ParticleTypes.LAVA, end, 20, 1.5, 0.0);
			kill(level, player, SoundEvents.PLAYER_HURT);

			for (int i = 0; i < 6; i++) {
				igniteNear(level, player, 3);
			}
		}

		if (tick >= FALL) {
			// A white flash that fades out.
			int since = tick - FALL;
			int alpha = Math.max(0, 240 - since * 240 / FLASH);
			tint = alpha << 24 | 0xFFFFFF;
		}
	}

	private void trail(ClientLevel level, ParticleOptions particle, Vec3 pos, int count, double spread, double speed) {
		for (int i = 0; i < count; i++) {
			level.addAlwaysVisibleParticle(particle, true,
				pos.x + (random.nextDouble() - 0.5) * 2 * spread, pos.y + (random.nextDouble() - 0.5) * 2 * spread, pos.z + (random.nextDouble() - 0.5) * 2 * spread,
				(random.nextDouble() - 0.5) * speed, (random.nextDouble() - 0.5) * speed, (random.nextDouble() - 0.5) * speed);
		}
	}
}
