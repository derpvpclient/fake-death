package de.derpvpclient.fakedeath.scene;

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
 * A building site: half a house in scaffolding, a big yellow crane, a pallet of bricks hanging right over the player.
 * Angela Merkel in a hard hat: "The building site is right on schedule." A builder warns her about the crane. She
 * folds her hands into the famous diamond and says calmly: "Wir schaffen das!" The chain snaps.
 */
public class MerkelScene extends CastScene {
	private static final int SCHEDULE = 15;
	private static final int WARN = 70;
	private static final int CREAK = 105;
	private static final int SCHAFFEN = 130;
	private static final int SNAP = 170;
	private static final int FALL = 14;
	private static final int HIT = SNAP + FALL;
	private static final double HANG = 9.0;
	private static final ChatFormatting BUND = ChatFormatting.GOLD;
	private static final ChatFormatting BAU = ChatFormatting.GRAY;

	private final List<Display.BlockDisplay> chain = new ArrayList<>();
	private RemotePlayer merkel;
	private RemotePlayer builder;
	private Display.BlockDisplay pallet;
	private Display.BlockDisplay bricks;
	private Vec3 hook;

	@Override
	public int length() {
		return HIT + 2;
	}

	@Override
	public int photoTick() {
		return SCHAFFEN + 15;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.merkel", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (merkel == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();
		face(merkel, eyes);
		face(builder, tick > WARN - 10 && tick < SNAP ? hook : eyes);

		if (tick == SCHEDULE) {
			speak(level, merkel, "fakedeath.merkel.schedule", BUND, 50);
		}
		if (tick == WARN) {
			speak(level, builder, "fakedeath.merkel.warn", BAU, 40);
			pose(builder, Skins.Pose.POINT);
		}
		if (tick == SCHAFFEN - 8) {
			pose(merkel, Skins.Pose.RAUTE);
		}
		if (tick == SCHAFFEN) {
			speak(level, merkel, "fakedeath.merkel.schaffen", BUND, 60);
		}

		// The load sways on its chain, more and more once the crane starts to creak.
		float sway = tick < CREAK ? 0.05F : 0.05F + (tick - CREAK) * 0.004F;
		Vec3 swing = new Vec3(Mth.sin(tick * 0.12F) * sway * 3.0, 0.0, Mth.cos(tick * 0.09F) * sway * 2.0);
		Vec3 load = tick < SNAP ? hook.add(swing).add(0.0, -3.0, 0.0) : null;
		if (tick >= CREAK && tick < SNAP && tick % 9 == 0) {
			sound(level, hook, SoundEvents.CHAIN_STEP, SoundSource.BLOCKS, 1.5F, 0.5F);
			sound(level, hook, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.4F, 0.5F);
		}
		if (tick == SNAP) {
			sound(level, hook, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 2.0F, 0.6F);
			sound(level, hook, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.8F, 1.5F);
			for (Display.BlockDisplay link : chain.subList(chain.size() / 2, chain.size())) {
				despawn(level, link);
			}
		}
		if (tick >= SNAP) {
			float p = Math.min(1.0F, (tick - SNAP) / (float) FALL);
			Vec3 start = hook.add(0.0, -3.0, 0.0);
			Vec3 head = eyes.add(0.0, 0.6, 0.0);
			load = start.lerp(head, p * p);
		}
		if (load != null && tick <= HIT) {
			place(pallet, load.add(0.0, -0.45, 0.0), new Vector3f(1.2F, 0.15F, 1.2F), new Quaternionf().rotateY(tick * 0.01F));
			place(bricks, load, new Vector3f(1.0F, 0.75F, 1.0F), new Quaternionf().rotateY(tick * 0.01F));
		}
		for (int i = 0; i < chain.size() / 2 + (tick < SNAP ? chain.size() / 2 : 0); i++) {
			Vec3 link = hook.lerp(load != null && tick < SNAP ? load.add(0.0, 0.4, 0.0) : hook, i / (double) chain.size());
			place(chain.get(i), link, new Vector3f(0.18F, 0.4F, 0.18F), new Quaternionf());
		}

		if (tick < SNAP - 6) {
			turnTowards(player, tick < CREAK || (tick > SCHAFFEN - 10 && tick < SCHAFFEN + 25) ? merkel.getEyePosition() : load, 0.1F);
		} else if (load != null) {
			turnTowards(player, load, 0.3F);
		}
		if (tick == HIT) {
			sound(level, eyes, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 2.0F, 0.6F);
			sound(level, eyes, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.2F, 0.7F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BRICKS.defaultBlockState()), eyes, 40, 0.6, 0.3);
			despawn(level, bricks);
			despawn(level, pallet);
			cameraShake(0.4F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		Vec3 look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		float yaw = (float) Math.atan2(look.x, look.z);
		merkel = actor(level, "fakedeath.merkel.name", "merkel_helmet", false, onGround(level, player, player.position().add(look.scale(4.2)).add(side.scale(-0.9))));
		builder = actor(level, "fakedeath.merkel.builder", "builder", false, onGround(level, player, player.position().add(look.scale(4.6)).add(side.scale(1.3))));

		// The half-built house: brick walls three high, scaffolding in front.
		BlockState brick = Blocks.BRICKS.defaultBlockState();
		Vec3 house = onGround(level, player, player.position().add(look.scale(8.5)).add(side.scale(3.0)));
		for (int dx = -2; dx <= 2; dx++) {
			int height = 3 - Math.abs(dx) / 2;
			for (int y = 0; y < height; y++) {
				Vec3 at = house.add(side.scale(dx)).add(0.0, y + 0.5, 0.0);
				place(blockDisplay(level, brick, at, 2.0F), at, new Vector3f(1.0F), new Quaternionf().rotateY(yaw));
			}
			Vec3 scaffold = house.add(side.scale(dx)).add(look.scale(-1.0)).add(0.0, 0.5, 0.0);
			for (int y = 0; y < 3; y++) {
				place(blockDisplay(level, Blocks.SCAFFOLDING.defaultBlockState(), scaffold, 2.0F), scaffold.add(0.0, y, 0.0), new Vector3f(1.0F), new Quaternionf().rotateY(yaw));
			}
		}

		// The crane: a yellow tower, the jib over to the player, the hook right above their head.
		BlockState yellow = Blocks.CONCRETE.yellow().defaultBlockState();
		Vec3 tower = onGround(level, player, player.position().add(look.scale(7.0)).add(side.scale(-4.0)));
		double top = player.getY() + HANG + 3.0;
		Vec3 towerMid = new Vec3(tower.x, (tower.y + top) / 2.0, tower.z);
		place(blockDisplay(level, yellow, towerMid, 16.0F), towerMid, new Vector3f(0.7F, (float) (top - tower.y), 0.7F), new Quaternionf().rotateY(yaw));
		hook = new Vec3(player.getX(), top - 0.4, player.getZ());
		Vec3 jibFrom = new Vec3(tower.x, top, tower.z);
		Vec3 jibTo = new Vec3(hook.x, top, hook.z).add(jibFrom.subtract(hook).normalize().scale(-2.0));
		Vec3 jibMid = jibFrom.lerp(jibTo, 0.5).add(0.0, 0.2, 0.0);
		Vec3 jibDir = jibTo.subtract(jibFrom);
		float jibYaw = (float) Math.atan2(jibDir.x, jibDir.z);
		place(blockDisplay(level, yellow, jibMid, 24.0F), jibMid, new Vector3f(0.5F, 0.5F, (float) jibDir.length()), new Quaternionf().rotateY(jibYaw));
		Vec3 cab = jibFrom.add(0.0, -1.0, 0.0);
		place(blockDisplay(level, Blocks.STAINED_GLASS.lightBlue().defaultBlockState(), cab, 3.0F), cab, new Vector3f(1.0F), new Quaternionf().rotateY(jibYaw));
		for (int i = 0; i < 8; i++) {
			chain.add(blockDisplay(level, Looks.CHAIN, hook, 6.0F));
		}
		pallet = blockDisplay(level, Blocks.OAK_PLANKS.defaultBlockState(), hook, 6.0F);
		bricks = blockDisplay(level, brick, hook, 6.0F);
		standFlag(level, player, player.position().add(look.scale(5.5)).add(side.scale(-2.4)), "flag_germany", 1.0F);
	}
}
