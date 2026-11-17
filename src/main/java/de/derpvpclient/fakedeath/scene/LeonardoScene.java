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
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Florence, about 1490. Leonardo da Vinci shows off his flying machine on a wooden tower, a lute playing
 * "Greensleeves": "Look! Man will fly!" He climbs in, the great canvas wings beat, and it really lifts off, a circle
 * over the player's head. Then the wings falter. "Hm. The weight ..." It comes down. Exactly on the player.
 */
public class LeonardoScene extends CastScene {
	private static final int FLY = ARRIVE + 8;
	private static final int CLIMB = ARRIVE + 55;
	private static final int TAKEOFF = CLIMB + 25;
	private static final int CIRCLE = 90;
	private static final int FALTER = TAKEOFF + CIRCLE;
	private static final int DIVE = 28;
	private static final int DEATH = FALTER + 20 + DIVE;
	private static final ChatFormatting ITALY = ChatFormatting.GREEN;
	/** "Greensleeves", the old English tune, in A minor, 6/8. */
	private static final int[][] GREENSLEEVES = {{69, 2}, {72, 4}, {74, 2}, {76, 3}, {77, 1}, {76, 2}, {74, 4}, {71, 2}, {67, 3}, {69, 1}, {71, 2},
		{72, 4}, {69, 2}, {69, 3}, {68, 1}, {69, 2}, {71, 4}, {68, 2}, {64, 4}, {69, 2}, {72, 4}, {74, 2}, {76, 3}, {77, 1}, {76, 2},
		{74, 4}, {71, 2}, {67, 3}, {69, 1}, {71, 2}, {72, 3}, {71, 1}, {69, 2}, {68, 3}, {66, 1}, {68, 2}, {69, 12}};
	private static final float STEP = 2.5F;

	private final List<Display.BlockDisplay> machine = new ArrayList<>();
	private RemotePlayer leonardo;
	private Vec3 tower;
	private Vec3 look;
	private Vec3 at;

	@Override
	public int length() {
		return DEATH + 2;
	}

	@Override
	public int photoTick() {
		return TAKEOFF + 40;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.leonardo", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (leonardo == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();
		if (tick < FALTER) {
			int loop = Band.length(GREENSLEEVES, STEP);
			Band.play(level, tower, GREENSLEEVES, (tick - 2) % loop, STEP, 1.0F, Band.HARP, Band.FLUTE);
		}

		// The flight: on the tower, up and round over the player, then down onto them.
		Vec3 start = tower.add(0.0, 3.4, 0.0);
		float flap = 0.0F;
		if (tick < TAKEOFF) {
			at = start;
		} else if (tick < FALTER) {
			float p = (tick - TAKEOFF) / (float) CIRCLE;
			double angle = p * Mth.TWO_PI;
			Vec3 center = player.position().add(look.scale(2.0)).add(0.0, 7.0, 0.0);
			Vec3 round = center.add(look.yRot((float) angle).scale(-4.0));
			at = start.lerp(round, Math.min(1.0, p * 4.0));
			flap = Mth.sin(tick * 0.6F) * 0.5F;
		} else if (tick < FALTER + 20) {
			// The wings falter, it hangs in the air, sinking.
			flap = Mth.sin(tick * 0.2F) * 0.15F;
			at = at.add(0.0, -0.03, 0.0);
		} else {
			float t = Math.min(1.0F, (tick - FALTER - 20) / (float) DIVE);
			at = at.lerp(eyes.add(0.0, 0.2, 0.0), t * t * 0.6F);
		}
		Vec3 heading = tick < TAKEOFF ? look.reverse() : look.yRot((float) ((tick - TAKEOFF) / (float) CIRCLE * Mth.TWO_PI) + Mth.HALF_PI).reverse();
		float yaw = (float) Math.atan2(heading.x, heading.z);
		float nose = tick > FALTER + 20 ? 0.9F : 0.0F;
		Quaternionf turn = new Quaternionf().rotateY(yaw).rotateX(nose);
		// The frame, the two wings beating, the tail.
		place(machine.get(0), at, new Vector3f(0.3F, 0.3F, 2.6F), new Quaternionf(turn));
		place(machine.get(1), at.add(0.0, -0.4, 0.0), new Vector3f(0.8F, 0.1F, 1.2F), new Quaternionf(turn));
		for (int w = 0; w < 2; w++) {
			int s = w == 0 ? 1 : -1;
			Vec3 out = new Vec3(Math.cos(yaw), 0.0, -Math.sin(yaw)).scale(s * 1.6);
			place(machine.get(2 + w), at.add(out).add(0.0, s * 0.0 + Math.abs(flap) * 0.6, 0.0), new Vector3f(3.0F, 0.06F, 1.4F), new Quaternionf(turn).rotateZ(s * flap));
			place(machine.get(4 + w), at.add(out).add(0.0, 0.05 + Math.abs(flap) * 0.6, 0.0), new Vector3f(3.0F, 0.08F, 0.1F), new Quaternionf(turn).rotateZ(s * flap));
		}
		place(machine.get(6), at.add(heading.scale(-1.5)), new Vector3f(1.2F, 0.06F, 0.8F), new Quaternionf(turn));
		if (tick >= CLIMB + 20) {
			// Leonardo in the frame.
			leonardo.setPos(at.x, at.y - 0.9, at.z);
		}
		if (tick >= TAKEOFF && tick < FALTER && (tick - TAKEOFF) % 10 == 0) {
			Scene.sound(level, at, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.NEUTRAL, 1.0F, 1.6F);
		}

		face(leonardo, tick < TAKEOFF ? eyes : at.add(heading.scale(3.0)));
		if (tick == FLY) {
			pose(leonardo, Skins.Pose.RAISE);
			speak(level, leonardo, "fakedeath.leonardo.fly", ITALY, 45);
		}
		if (tick == CLIMB) {
			pose(leonardo, null);
		}
		if (tick > CLIMB && tick < CLIMB + 20) {
			stepTowards(level, player, leonardo, tower, 0.12);
		}
		if (tick == TAKEOFF) {
			pose(leonardo, Skins.Pose.CHEER);
			Scene.sound(level, at, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.NEUTRAL, 1.0F, 0.6F);
		}
		if (tick == FALTER) {
			pose(leonardo, null);
			speak(level, leonardo, "fakedeath.leonardo.weight", ITALY, 40);
			Scene.sound(level, at, SoundEvents.WOOD_BREAK, SoundSource.NEUTRAL, 1.0F, 1.2F);
		}
		if (tick == DEATH) {
			Scene.sound(level, eyes, SoundEvents.WOOD_BREAK, SoundSource.NEUTRAL, 2.0F, 0.6F);
			Scene.sound(level, eyes, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.NEUTRAL, 1.5F, 0.8F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_PLANKS.defaultBlockState()), eyes, 40, 1.0, 0.3);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.WHITE_WOOL.defaultBlockState()), eyes, 30, 1.0, 0.3);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}

		if (tick < TAKEOFF) {
			turnTowards(player, tick < CLIMB ? leonardo.getEyePosition() : at, 0.08F);
		} else {
			turnTowards(player, at, tick > FALTER + 20 ? 0.3F : 0.12F);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		float yaw = (float) Math.atan2(look.x, look.z);
		// A wooden tower to start from, at the left; a workbench.
		tower = onGround(level, player, player.position().add(look.scale(6.0)).add(side.scale(2.5)));
		for (int sx : new int[] {-1, 1}) {
			for (int sz : new int[] {-1, 1}) {
				Vec3 leg = tower.add(side.scale(sx * 0.7)).add(look.scale(sz * 0.7));
				place(blockDisplay(level, Blocks.SPRUCE_LOG.defaultBlockState(), leg, 6.0F), leg.add(0.0, 1.5, 0.0), new Vector3f(0.2F, 3.0F, 0.2F), new Quaternionf().rotateY(yaw));
			}
		}
		place(blockDisplay(level, Blocks.SPRUCE_PLANKS.defaultBlockState(), tower, 4.0F), tower.add(0.0, 3.0, 0.0), new Vector3f(1.8F, 0.12F, 1.8F), new Quaternionf().rotateY(yaw));
		Vec3 bench = onGround(level, player, player.position().add(look.scale(4.0)).add(side.scale(-1.6)));
		place(blockDisplay(level, Blocks.CRAFTING_TABLE.defaultBlockState(), bench, 2.0F), bench.add(0.0, 0.45, 0.0), new Vector3f(0.9F), new Quaternionf().rotateY(yaw));
		// The machine: a wooden frame, a seat board, two canvas wings with wooden spars, a tail.
		machine.add(blockDisplay(level, Blocks.SPRUCE_PLANKS.defaultBlockState(), tower, 12.0F));
		machine.add(blockDisplay(level, Blocks.SPRUCE_PLANKS.defaultBlockState(), tower, 12.0F));
		machine.add(blockDisplay(level, Blocks.WHITE_WOOL.defaultBlockState(), tower, 12.0F));
		machine.add(blockDisplay(level, Blocks.WHITE_WOOL.defaultBlockState(), tower, 12.0F));
		machine.add(blockDisplay(level, Blocks.SPRUCE_PLANKS.defaultBlockState(), tower, 12.0F));
		machine.add(blockDisplay(level, Blocks.SPRUCE_PLANKS.defaultBlockState(), tower, 12.0F));
		machine.add(blockDisplay(level, Blocks.WHITE_WOOL.defaultBlockState(), tower, 12.0F));
		leonardo = actor(level, "fakedeath.leonardo.name", "leonardo", false, onGround(level, player, bench.add(side.scale(1.0)).add(look.scale(0.6))));
		arrive(level, player, leonardo, 0);
	}
}
