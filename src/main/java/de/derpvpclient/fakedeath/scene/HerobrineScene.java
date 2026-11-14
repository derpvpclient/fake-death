package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * "Herobrine joined the game." The torches around go out one by one, darkness creeps in. Far off at the edge of the
 * view someone stands and stares with white glowing eyes; look at him and he is gone. Silence, a heartbeat, a glimpse
 * of him closer, gone again. The view turns around by itself, slowly, and he stands right there. He screams.
 */
public class HerobrineScene extends Scene {
	private static final int JOINED = 0;
	private static final int TORCHES = 6;
	private static final int APPEAR = 40;
	private static final int STARE = 70;
	private static final int VANISH = 100;
	private static final int GLIMPSE = 150;
	private static final int GLIMPSE_END = 156;
	private static final int TURN = 195;
	private static final int FACE = 235;
	private static final int SCREAM = 250;

	private final List<BlockPos> lights = new ArrayList<>();
	private RemotePlayer herobrine;
	private Display.BlockDisplay leftEye;
	private Display.BlockDisplay rightEye;
	private Vec3 behindSpot;
	private float gloom;

	@Override
	public int screenTint() {
		return (int) (gloom * 0x78) << 24 | 0x05050F;
	}

	@Override
	public int length() {
		return SCREAM + 8;
	}

	@Override
	public int photoTick() {
		return FACE + 6;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.herobrine", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == JOINED) {
			// Like a real player joining the server.
			minecraft.gui.getChat().addMessage(Component.translatable("multiplayer.player.joined", Component.literal("Herobrine"))
				.withStyle(ChatFormatting.YELLOW));
			findLights(level, player);
		}

		// The torches go out one after the other, the nearest last.
		if (tick >= TORCHES && !lights.isEmpty() && tick % 7 == 0) {
			BlockPos torch = lights.removeLast();
			replaceBlock(level, torch, Blocks.AIR.defaultBlockState());
			sound(level, Vec3.atCenterOf(torch), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 0.6F);
			particles(level, ParticleTypes.SMOKE, Vec3.atCenterOf(torch), 8, 0.1, 0.02);
		}
		// It gets darker and darker, but not so dark that he can not be seen out there.
		gloom = Math.min(1.0F, Math.max(0.0F, (tick - 10) / 60.0F));
		if (tick == 30) {
			sound(level, player.position(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.6F);
		}

		if (tick == APPEAR) {
			// Far away, a little off to the side, standing still.
			show(level, groundSpot(level, player, 18.0, 28.0), player);
		}
		if (tick > APPEAR && tick < VANISH) {
			// The view drifts over to him by itself.
			turnTowards(player, herobrine.getEyePosition(), tick < STARE ? 0.03F : 0.1F);
		}
		if (tick == VANISH) {
			hide(level);
			sound(level, player.position(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.2F, 0.5F);
		}
		if (tick > VANISH && tick < GLIMPSE) {
			// Looking around, where did he go?
			Vec3 eyes = player.getEyePosition();
			Vec3 look = horizontalLook(player).yRot((float) Math.sin((tick - VANISH) * 0.08) * 1.4F);
			turnTowards(player, eyes.add(look.scale(5.0)), 0.08F);
			if ((tick - VANISH) % 24 == 0) {
				sound(level, player.position(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.0F, 0.7F);
			}
		}
		if (tick == GLIMPSE) {
			// For a blink he stands much closer, then he is gone again.
			show(level, groundSpot(level, player, 7.0, -35.0), player);
			sound(level, player.position(), SoundEvents.GRASS_STEP, SoundSource.HOSTILE, 1.0F, 0.6F);
		}
		if (tick == GLIMPSE_END) {
			hide(level);
		}
		if (tick > GLIMPSE_END && tick < TURN && (tick - GLIMPSE_END) % 16 == 0) {
			sound(level, player.position(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.2F, 0.8F);
		}

		if (tick == TURN) {
			// Right behind the player. The view turns around on its own, slowly.
			behindSpot = player.position().add(horizontalLook(player).scale(-1.3));
			show(level, new Vec3(behindSpot.x, floor(level, behindSpot.x, behindSpot.z, player.getY()), behindSpot.z), player);
		}
		if (tick > TURN && tick < SCREAM) {
			turnTowards(player, herobrine.getEyePosition(), tick < FACE ? 0.05F : 0.4F);
			if (tick % 10 == 0) {
				sound(level, player.position(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.4F, 0.9F);
			}
		}
		if (tick == FACE) {
			// Face to face, total silence for a moment, then a low rumble.
			sound(level, player.position(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 0.6F, 0.5F);
		}
		if (tick == SCREAM) {
			sound(level, player.position(), SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 1.5F, 0.5F);
			sound(level, player.position(), SoundEvents.GHAST_SCREAM, SoundSource.HOSTILE, 1.0F, 0.4F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
		if (tick >= SCREAM) {
			cameraShake(0.3F);
		}

		if (herobrine != null) {
			face(herobrine, player.getEyePosition());
			placeEyes();
		}
	}

	/** The torches and lanterns around the player, farthest first, so the nearest goes out last. */
	private void findLights(ClientLevel level, LocalPlayer player) {
		BlockPos feet = player.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-12, -4, -12), feet.offset(12, 6, 12))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)
				|| state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN) || state.is(Blocks.CAMPFIRE)) {
				lights.add(pos.immutable());
			}
		}
		lights.sort((a, b) -> Double.compare(a.distSqr(feet), b.distSqr(feet)));
		while (lights.size() > 16) {
			lights.removeLast();
		}
	}

	private void show(ClientLevel level, Vec3 at, LocalPlayer player) {
		hide(level);
		herobrine = person(level, "Herobrine", "herobrine", false, false, at);
		face(herobrine, player.getEyePosition());
		// His eyes shine in the dark: two small white lights, always at full brightness.
		leftEye = eye(level, at);
		rightEye = eye(level, at);
		placeEyes();
	}

	private Display.BlockDisplay eye(ClientLevel level, Vec3 at) {
		Display.BlockDisplay eye = blockDisplay(level, Looks.WHITE_CONCRETE, at, 2.0F);
		((de.derpvpclient.fakedeath.mixin.DisplayAccessor) eye).fakedeath$setBrightnessOverride(new Brightness(15, 15));
		return eye;
	}

	private void hide(ClientLevel level) {
		if (herobrine != null) {
			despawn(level, herobrine);
			despawn(level, leftEye);
			despawn(level, rightEye);
			herobrine = null;
		}
	}

	/** The eyes sit just in front of the face, where the skin has them. */
	private void placeEyes() {
		float yaw = herobrine.getYHeadRot() * ((float) Math.PI / 180.0F);
		Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
		Vec3 side = new Vec3(forward.z, 0.0, -forward.x);
		Vec3 center = herobrine.position().add(0.0, 1.66, 0.0).add(forward.scale(0.255));
		Quaternionf turn = new Quaternionf().rotateY(-yaw);
		place(leftEye, center.add(side.scale(-0.125)), new Vector3f(0.12F, 0.06F, 0.01F), turn);
		place(rightEye, center.add(side.scale(0.125)), new Vector3f(0.12F, 0.06F, 0.01F), turn);
	}
}
