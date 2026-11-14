package de.derpvpclient.fakedeath.scene;

import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Good Bye, Lenin! A giant stone Lenin on a pedestal, one arm stretched out over the land. Workers have ropes round
 * his neck and pull: "Heave ... ho!" The statue groans, rocks, something cracks, and it comes down, slowly at first,
 * then all at once, right onto the player.
 */
public class MonumentScene extends CastScene {
	private static final int[] HEAVES = {20, 50, 78};
	private static final int CRACK = 92;
	private static final int FALL = 30;
	private static final int IMPACT = CRACK + FALL;
	private static final ChatFormatting WORKERS = ChatFormatting.GRAY;

	private final List<Part> parts = new ArrayList<>();
	private final List<RemotePlayer> pullers = new ArrayList<>();
	private final List<Display.BlockDisplay> ropes = new ArrayList<>();
	private Vec3 pivot;
	private Vec3 toward;
	private Vector3f axis;
	private float angle;

	@Override
	public int length() {
		return IMPACT + 3;
	}

	@Override
	public int photoTick() {
		return CRACK + 14;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.denkmal", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (parts.isEmpty()) {
			return;
		}
		bubbles();

		for (int i = 0; i < HEAVES.length; i++) {
			if (tick == HEAVES[i]) {
				speak(level, pullers.get(i % pullers.size()), i == 2 ? "fakedeath.denkmal.ho" : "fakedeath.denkmal.heave", WORKERS, 25);
				sound(level, pivot.add(0.0, 5.0, 0.0), SoundEvents.CHAIN_STEP, SoundSource.BLOCKS, 2.0F, 0.5F);
			}
		}
		if (tick < CRACK) {
			// Rocking a little more with every heave.
			float strength = tick / (float) CRACK;
			angle = Math.max(0.0F, Mth.sin(tick * 0.2F)) * 5.0F * strength;
			if (tick % 15 == 0) {
				sound(level, pivot, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 1.5F, 0.5F);
			}
		} else {
			float p = Math.min(1.0F, (tick - CRACK) / (float) FALL);
			angle = 5.0F + 83.0F * p * p;
		}
		if (tick == CRACK) {
			sound(level, pivot, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 2.5F, 0.4F);
			sound(level, pivot, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 1.5F, 0.5F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), pivot, 30, 0.8, 0.2);
		}
		if (tick == CRACK + 6) {
			speak(level, pullers.getFirst(), "fakedeath.denkmal.goodbye", WORKERS, 30);
		}

		Quaternionf rotation = new Quaternionf().rotateAxis(angle * Mth.DEG_TO_RAD, axis);
		for (Part part : parts) {
			Vector3f offset = new Vector3f(part.from()).rotate(rotation);
			part.display().setPos(pivot.x, pivot.y, pivot.z);
			transform(part.display(), new Transformation(offset, new Quaternionf(rotation).mul(part.turn()), part.size(), new Quaternionf()));
		}

		// The pullers lean back on their ropes, and run when it comes down.
		Vec3 neck = pivot.add(new Vec3(0.0, 7.0, 0.0).zRot(0.0F)).add(toward.scale(Math.sin(angle * Mth.DEG_TO_RAD) * 7.0))
			.add(0.0, (Math.cos(angle * Mth.DEG_TO_RAD) - 1.0) * 7.0, 0.0);
		for (int i = 0; i < pullers.size(); i++) {
			RemotePlayer puller = pullers.get(i);
			if (tick >= CRACK) {
				stepTowards(level, player, puller, puller.position().add(toward.yRot(i == 0 ? 1.4F : -1.4F).scale(3.0)), 0.25);
			}
			face(puller, neck);
			if (i < ropes.size()) {
				Vec3 hand = puller.position().add(0.0, 1.2, 0.0);
				Vec3 mid = hand.lerp(neck, 0.5);
				Vec3 dir = neck.subtract(hand);
				Quaternionf along = new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), new Vector3f((float) dir.x, (float) dir.y, (float) dir.z).normalize());
				place(ropes.get(i), mid, new Vector3f(0.08F, (float) dir.length(), 0.08F), along);
			}
		}
		if (tick >= CRACK) {
			for (Display.BlockDisplay rope : ropes) {
				despawn(level, rope);
			}
			ropes.clear();
		}

		turnTowards(player, tick < CRACK ? pivot.add(0.0, 6.0, 0.0) : neck, tick < CRACK ? 0.08F : 0.3F);
		if (tick > CRACK) {
			cameraShake(0.02F + (tick - CRACK) * 0.006F);
		}
		if (tick == IMPACT) {
			sound(level, player.position(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 2.0F, 0.4F);
			sound(level, player.position(), Looks.EXPLODE, SoundSource.BLOCKS, 1.5F, 0.6F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), player.getEyePosition(), 60, 1.5, 0.3);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		Vec3 look = horizontalLook(player);
		toward = look.reverse();
		Vec3 base = onGround(level, player, player.position().add(look.scale(7.5)));
		float yaw = (float) Math.atan2(toward.x, toward.z);
		Quaternionf facing = new Quaternionf().rotateY(yaw);
		// The pedestal stays, the statue on it tips over its front edge, towards the player.
		place(blockDisplay(level, Blocks.STONE_BRICKS.defaultBlockState(), base, 6.0F), base.add(0.0, 1.0, 0.0), new Vector3f(3.0F, 2.0F, 3.0F), facing);
		pivot = base.add(toward.scale(1.2)).add(0.0, 2.0, 0.0);
		axis = new Vector3f((float) toward.z, 0.0F, (float) -toward.x);
		BlockState stone = Blocks.STONE.defaultBlockState();
		BlockState dark = Blocks.DEEPSLATE.defaultBlockState();
		// In the statue's own frame: x across, y up, z towards the player, measured from the front edge of its foot.
		add(level, dark, -0.95F, 0.0F, -2.0F, 0.8F, 3.0F, 0.8F, facing);
		add(level, dark, 0.15F, 0.0F, -2.0F, 0.8F, 3.0F, 0.8F, facing);
		add(level, stone, -1.1F, 2.6F, -2.2F, 2.2F, 3.4F, 1.3F, facing);
		add(level, stone, -0.6F, 6.0F, -2.0F, 1.2F, 1.2F, 1.2F, facing);
		// The famous arm, stretched out over the land, towards the player.
		add(level, stone, 1.1F, 5.2F, -1.9F, 0.6F, 0.6F, 2.6F, facing);
		add(level, stone, -1.7F, 3.2F, -1.85F, 0.6F, 2.6F, 0.6F, facing);
		add(level, dark, -0.65F, 7.15F, -2.05F, 1.3F, 0.3F, 1.3F, facing);
		Vec3 side = look.yRot(Mth.HALF_PI);
		for (int s : new int[] {-1, 1}) {
			RemotePlayer puller = actor(level, s < 0 ? "fakedeath.denkmal.worker1" : "fakedeath.denkmal.worker2", "worker", false,
				onGround(level, player, base.add(toward.scale(3.5)).add(side.scale(s * 3.5))));
			pullers.add(puller);
			ropes.add(blockDisplay(level, Blocks.WOOL.brown().defaultBlockState(), puller.position(), 16.0F));
		}
	}

	private void add(ClientLevel level, BlockState state, float x, float y, float z, float w, float h, float d, Quaternionf facing) {
		Display.BlockDisplay display = blockDisplay(level, state, pivot, 24.0F);
		Vector3f from = new Vector3f(x, y, z).rotate(facing);
		parts.add(new Part(display, from, new Vector3f(w, h, d), new Quaternionf(facing)));
	}

	private record Part(Display.BlockDisplay display, Vector3f from, Vector3f size, Quaternionf turn) {
	}
}
