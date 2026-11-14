package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
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
 * Petrograd, April 1917. Lenin stands on an armoured car, arm stretched out, a crowd of workers around him under red
 * banners. "Comrades! All power to the Soviets!" The crowd cheers, arms up. Then he points at the player: "And that
 * one there ... is a bourgeois!" The crowd turns round. And runs.
 */
public class RevolutionScene extends CastScene {
	private static final int COMRADES = 15;
	private static final int POWER = 55;
	private static final int CHEER = POWER + 20;
	private static final int POINT = CHEER + 40;
	private static final int TURN = POINT + 25;
	private static final int STORM = TURN + 10;
	private static final int[] HITS = {STORM + 28, STORM + 36, STORM + 44};
	private static final int DEATH = STORM + 52;
	private static final int CROWD = 10;
	private static final ChatFormatting RED = ChatFormatting.RED;

	private final List<RemotePlayer> crowd = new ArrayList<>();
	private final List<Display.ItemDisplay> banners = new ArrayList<>();
	private RemotePlayer lenin;
	private Vec3 car;
	private boolean down;

	@Override
	public int length() {
		return DEATH + 1;
	}

	@Override
	public int photoTick() {
		return CHEER + 10;
	}

	@Override
	public float lying() {
		return down ? 1.0F : 0.0F;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.revolution", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (lenin == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();
		face(lenin, tick < POINT ? lenin.getEyePosition().add(horizontalLook(player).scale(-4.0)).add(0.0, 0.5, 0.0) : eyes);

		if (tick == COMRADES) {
			speak(level, lenin, "fakedeath.revolution.comrades", RED, 35);
			pose(lenin, Skins.Pose.POINT);
		}
		if (tick == POWER) {
			speak(level, lenin, "fakedeath.revolution.power", RED, 40);
		}
		if (tick == CHEER) {
			for (RemotePlayer worker : crowd) {
				pose(worker, Skins.Pose.CHEER);
			}
		}
		if (tick >= CHEER && tick < POINT && tick % 6 == 0) {
			RemotePlayer worker = crowd.get(random.nextInt(crowd.size()));
			sound(level, worker.position(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.5F, 0.7F + random.nextFloat() * 0.3F);
		}
		if (tick == POINT) {
			for (RemotePlayer worker : crowd) {
				pose(worker, null);
			}
			speak(level, lenin, "fakedeath.revolution.bourgeois", RED, 50);
		}
		for (int i = 0; i < crowd.size(); i++) {
			RemotePlayer worker = crowd.get(i);
			if (tick < TURN) {
				face(worker, lenin.getEyePosition());
			} else if (tick < STORM) {
				face(worker, eyes);
			} else {
				// All of them at once, straight at the player.
				Vec3 target = player.position().add(horizontalLook(player).yRot((i - CROWD / 2) * 0.25F).scale(0.6));
				stepTowards(level, player, worker, target, 0.32);
				face(worker, eyes);
				if (tick % 5 == i % 5) {
					sound(level, worker.position(), SoundEvents.GRASS_STEP, SoundSource.NEUTRAL, 1.0F, 0.8F);
				}
			}
		}
		if (tick == STORM) {
			sound(level, eyes, SoundEvents.RAVAGER_ROAR, SoundSource.NEUTRAL, 0.8F, 1.4F);
			for (RemotePlayer worker : crowd) {
				pose(worker, Skins.Pose.RAISE);
			}
		}
		for (int i = 0; i < banners.size(); i++) {
			RemotePlayer bearer = crowd.get(i * 5);
			Vec3 toPlayer = player.position().subtract(bearer.position());
			float yaw = (float) Math.atan2(toPlayer.x, toPlayer.z);
			place(banners.get(i), bearer.position().add(0.0, 2.7, 0.0), new Vector3f(1.3F), new Quaternionf().rotateY(yaw).rotateZ(Mth.sin(tick * 0.08F + i) * 0.07F));
		}
		turnTowards(player, tick < TURN ? lenin.getEyePosition() : crowd.get(CROWD / 2).getEyePosition(), 0.1F);
		for (int i = 0; i < HITS.length; i++) {
			if (tick == HITS[i]) {
				particles(level, ParticleTypes.CRIT, eyes, 12, 0.4, 0.2);
				cameraShake(0.25F);
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.28F);
				down = i > 0;
			}
		}
		if (down) {
			moveCamera(new Vec3(0.0, -1.3, 0.0));
		}
		if (tick == DEATH) {
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		Vec3 look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		float yaw = (float) Math.atan2(side.x, side.z);
		car = onGround(level, player, player.position().add(look.scale(9.0)));
		// The armoured car: a grey hull across the view, a turret, black wheels.
		place(blockDisplay(level, Looks.GRAY_CONCRETE, car, 8.0F), car.add(0.0, 1.0, 0.0), new Vector3f(1.8F, 1.3F, 4.0F), new Quaternionf().rotateY(yaw));
		place(blockDisplay(level, Blocks.CONCRETE.green().defaultBlockState(), car, 8.0F), car.add(0.0, 1.85, 0.0), new Vector3f(1.2F, 0.5F, 1.4F), new Quaternionf().rotateY(yaw));
		for (int s : new int[] {-1, 1}) {
			for (int f : new int[] {-1, 1}) {
				Vec3 wheel = car.add(look.scale(s * 0.95)).add(side.scale(f * 1.4)).add(0.0, 0.4, 0.0);
				place(blockDisplay(level, Looks.BLACK_CONCRETE, wheel, 2.0F), wheel, new Vector3f(0.3F, 0.8F, 0.8F), new Quaternionf().rotateY(yaw + Mth.HALF_PI));
			}
		}
		lenin = actor(level, "fakedeath.revolution.lenin", "lenin", false, car.add(0.0, 2.1, 0.0));
		for (int i = 0; i < CROWD; i++) {
			double angle = (i - (CROWD - 1) / 2.0) * 0.3;
			Vec3 at = car.add(look.yRot((float) angle).scale(-3.0 - (i % 2) * 0.8));
			crowd.add(extra(level, "worker", onGround(level, player, at)));
		}
		for (int i = 0; i < 2; i++) {
			banners.add(itemDisplay(level, Looks.prop(net.minecraft.world.item.Items.IRON_NUGGET, "flag_red"), crowd.get(i * 5).position()));
		}
	}
}
