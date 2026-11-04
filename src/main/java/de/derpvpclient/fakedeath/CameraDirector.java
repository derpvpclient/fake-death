package de.derpvpclient.fakedeath;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Turns only the camera during a scene, never the player. The real rotation is what the server and
 * other players see, so it stays where the player left it and nobody sees the head turning.
 */
public final class CameraDirector {
	private static float yaw;
	private static float pitch;
	private static float prevYaw;
	private static float prevPitch;
	private static float partialTick;
	private static boolean released;
	private static Vec3 offset = Vec3.ZERO;
	private static Vec3 prevOffset = Vec3.ZERO;

	private CameraDirector() {
	}

	/** After the reveal the camera belongs to the player again, even while a scene's actors are still around. */
	public static void release(boolean released) {
		CameraDirector.released = released;
	}

	public static void startTick() {
		prevYaw = yaw;
		prevPitch = pitch;
		prevOffset = offset;
	}

	/** Without a scene the camera glides back to where the player really looks. */
	public static void endTick(boolean holding) {
		if (!holding) {
			yaw *= 0.6F;
			pitch *= 0.6F;

			if (Math.abs(yaw) < 0.05F && Math.abs(pitch) < 0.05F) {
				yaw = 0.0F;
				pitch = 0.0F;
			}

			offset = offset.lengthSqr() < 1.0E-4 ? Vec3.ZERO : offset.scale(0.5);
		}
	}

	public static void reset() {
		yaw = pitch = prevYaw = prevPitch = 0.0F;
		offset = prevOffset = Vec3.ZERO;
	}

	/** Turns the camera a bit further towards a point each tick, like a camera pan. */
	public static void turnTowards(LocalPlayer player, Vec3 target, float speed) {
		if (released) {
			return;
		}

		Vec3 delta = target.subtract(player.getEyePosition().add(offset));
		float targetYaw = (float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90.0F;
		float targetPitch = (float) -(Mth.atan2(delta.y, delta.horizontalDistance()) * Mth.RAD_TO_DEG);

		// The front view looks back at the player from where they face, so face away to get the target behind them in the shot.
		if (Minecraft.getInstance().options.getCameraType().isMirrored()) {
			targetYaw += 180.0F;
			targetPitch = -targetPitch;
		}
		float viewYaw = player.getYRot() + yaw;
		float viewPitch = player.getXRot() + pitch;

		yaw += Mth.wrapDegrees(targetYaw - viewYaw) * speed;
		pitch = Mth.clamp(viewPitch + (targetPitch - viewPitch) * speed, -90.0F, 90.0F) - player.getXRot();
	}

	public static void setPartialTick(float partialTick) {
		CameraDirector.partialTick = partialTick;
	}

	public static float yawOffset() {
		return Mth.lerp(partialTick, prevYaw, yaw);
	}

	public static float pitchOffset() {
		return Mth.lerp(partialTick, prevPitch, pitch);
	}

	/** Moves only the camera away from the eyes, like being pulled somewhere. The player stays where they are. */
	public static void moveTo(Vec3 fromEyes) {
		if (!released) {
			offset = fromEyes;
		}
	}

	public static Vec3 positionOffset() {
		return prevOffset.lerp(offset, partialTick);
	}

	public static Vec3 positionOffsetNow() {
		return offset;
	}
}
