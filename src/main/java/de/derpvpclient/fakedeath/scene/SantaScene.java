package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Santa: "Were you good this year?" He unrolls his list, all the way down to the ground. "Hm. No." Two presents full of
 * coal, the second one right at the head.
 */
public class SantaScene extends NpcScene {
	private static final int UNROLL = 25;
	private static final int HMM = 30;
	private static final int NO = 70;
	private static final int[] THROWS = {95, 130};
	private static final int FLIGHT = 14;
	private Display.BlockDisplay list;
	private Display.BlockDisplay box;
	private Display.BlockDisplay ribbonX;
	private Display.BlockDisplay ribbonZ;
	private final List<Display.BlockDisplay> writing = new ArrayList<>();
	private Display.BlockDisplay hat;
	private Display.BlockDisplay hatTop;
	private Display.BlockDisplay brim;
	private Display.BlockDisplay bobble;
	private Display.BlockDisplay beard;
	private final List<Display.ItemDisplay> coal = new ArrayList<>();
	private final List<Vec3> coalSpeed = new ArrayList<>();

	public SantaScene() {
		super("weihnachtsmann", ChatFormatting.RED);
	}

	@Override
	protected int lineCount() {
		return 2;
	}

	@Override
	protected int actLength() {
		return THROWS[1] + FLIGHT + 60;
	}

	@Override
	protected int photoOffset() {
		return 40;
	}

	@Override
	protected double standDistance() {
		return 3.0;
	}

	@Override
	protected ItemStack held() {
		return new ItemStack(Items.PAPER);
	}

	@Override
	protected Vec3 focus(LocalPlayer player) {
		return box != null ? box.position() : list != null ? list.position() : npc.getEyePosition();
	}

	@Override
	protected void setup(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
		// A snowy coat, a red hat with a white brim and bobble, and a white beard.
		Looks.snowCoat(npc, level);
		hat = blockDisplay(level, Looks.RED_WOOL, npc.getEyePosition(), 2.0F);
		hatTop = blockDisplay(level, Looks.RED_WOOL, npc.getEyePosition(), 2.0F);
		brim = blockDisplay(level, Looks.WHITE_WOOL, npc.getEyePosition(), 2.0F);
		bobble = blockDisplay(level, Looks.WHITE_WOOL, npc.getEyePosition(), 2.0F);
		beard = blockDisplay(level, Looks.WHITE_WOOL, npc.getEyePosition(), 2.0F);
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		super.tick(minecraft, level, player, tick);
		if (hat != null) {
			float yaw = npc.getYHeadRot() * Mth.DEG_TO_RAD;
			Vec3 front = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
			Quaternionf turn = new Quaternionf().rotateY(-yaw);
			Vec3 eyes = npc.getEyePosition();
			place(brim, eyes.add(0.0, 0.42, 0.0), new Vector3f(0.6F, 0.12F, 0.6F), turn);
			place(hat, eyes.add(0.0, 0.58, 0.0).subtract(front.scale(0.03)), new Vector3f(0.46F, 0.24F, 0.46F), turn);
			place(hatTop, eyes.add(0.0, 0.76, 0.0).subtract(front.scale(0.12)), new Vector3f(0.3F, 0.2F, 0.3F), new Quaternionf(turn).rotateX(-0.35F));
			place(bobble, eyes.add(0.0, 0.86, 0.0).subtract(front.scale(0.26)), new Vector3f(0.15F), turn);
			place(beard, eyes.add(0.0, -0.32, 0.0).add(front.scale(0.27)), new Vector3f(0.46F, 0.36F, 0.1F), turn);
		}
		// It is always snowing around him.
		for (int i = 0; i < 3; i++) {
			level.addParticle(ParticleTypes.SNOWFLAKE, player.getX() + (random.nextDouble() - 0.5) * 10, player.getY() + 4 + random.nextDouble() * 2,
				player.getZ() + (random.nextDouble() - 0.5) * 10, 0, -0.05, 0);
		}
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		Vec3 toward = towardPlayer(player);
		Vec3 hands = npc.getEyePosition().add(toward.scale(0.5)).add(0.0, -0.5, 0.0);
		Quaternionf facing = new Quaternionf().rotateY((float) Math.atan2(toward.x, toward.z));

		if (t == 0) {
			list = blockDisplay(level, Looks.WHITE_WOOL, hands, 4.0F);
			for (int i = 0; i < 7; i++) {
				writing.add(blockDisplay(level, Looks.GRAY_CONCRETE, hands, 4.0F));
			}
			sound(level, hands, SoundEvents.BOOK_PAGE_TURN, SoundSource.NEUTRAL, 1.5F, 0.8F);
		}
		if (list != null) {
			// The list hangs from his hand beside him, rolls out down to the ground, line after line of names.
			Vec3 top = hands.add(toward.yRot(Mth.HALF_PI).scale(0.5)).add(0.0, 0.25, 0.0);
			float length = Math.min(1.0F, t / (float) UNROLL) * 2.2F;
			place(list, top.add(toward.scale(0.1 + length * 0.08)).add(0.0, -length / 2.0, 0.0), new Vector3f(0.55F, length, 0.03F), facing);
			for (int i = 0; i < writing.size(); i++) {
				double down = 0.15 + i * 0.3;
				boolean shown = down < length - 0.05;
				place(writing.get(i), top.add(toward.scale(0.13 + down * 0.08)).add(0.0, -down, 0.0),
					shown ? new Vector3f(0.4F - (i % 3) * 0.08F, 0.035F, 0.01F) : new Vector3f(0.0F), facing);
			}
			if (t % 8 == 0 && t < UNROLL) {
				sound(level, hands, SoundEvents.BOOK_PAGE_TURN, SoundSource.NEUTRAL, 1.0F, 1.0F + random.nextFloat() * 0.3F);
			}
		}
		if (t == HMM) {
			speak(minecraft, level, "fakedeath.weihnachtsmann.hmm");
		}
		if (t == NO) {
			speak(minecraft, level, "fakedeath.weihnachtsmann.no");
		}
		if (t == NO + 15) {
			despawn(level, list);
			list = null;
			writing.forEach(line -> despawn(level, line));
			writing.clear();
		}

		for (int i = 0; i < THROWS.length; i++) {
			int f = t - THROWS[i];
			if (f == -12) {
				// A present: red with a golden ribbon.
				box = blockDisplay(level, Looks.RED_WOOL, hands, 3.0F);
				ribbonX = blockDisplay(level, Blocks.GOLD_BLOCK.defaultBlockState(), hands, 3.0F);
				ribbonZ = blockDisplay(level, Blocks.GOLD_BLOCK.defaultBlockState(), hands, 3.0F);
			}
			if (box == null || f < -12 || f > FLIGHT) {
				continue;
			}
			Vec3 target = i == 0 ? player.position().add(0.0, 1.0, 0.0) : player.getEyePosition();
			Vec3 at;
			if (f < 0) {
				at = hands.add(0.0, 0.8 * (f + 12) / 12.0, 0.0);
			} else {
				float p = f / (float) FLIGHT;
				at = hands.add(0.0, 0.8, 0.0).lerp(target, p).add(0.0, Mth.sin(p * Mth.PI) * 1.2, 0.0);
			}
			Quaternionf spin = new Quaternionf(facing).rotateX(Math.max(0, f) * 0.4F);
			place(box, at, new Vector3f(0.55F), spin);
			place(ribbonX, at, new Vector3f(0.6F, 0.6F, 0.1F), spin);
			place(ribbonZ, at, new Vector3f(0.1F, 0.6F, 0.6F), spin);
			if (f == 0) {
				Looks.swing(npc);
				sound(level, at, SoundEvents.WITCH_THROW, SoundSource.NEUTRAL, 1.0F, 0.8F);
			}
			if (f == FLIGHT) {
				// It bursts open: coal, nothing but coal.
				despawn(level, box);
				despawn(level, ribbonX);
				despawn(level, ribbonZ);
				box = null;
				sound(level, target, SoundEvents.WOOL_BREAK, SoundSource.NEUTRAL, 1.5F, 0.8F);
				sound(level, target, SoundEvents.STONE_BREAK, SoundSource.NEUTRAL, 1.5F, 0.9F);
				particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COAL_BLOCK.defaultBlockState()), target, 25, 0.4, 0.2);
				for (int c = 0; c < 6; c++) {
					Display.ItemDisplay piece = itemDisplay(level, new ItemStack(Items.COAL), target);
					coal.add(piece);
					coalSpeed.add(new Vec3((random.nextDouble() - 0.5) * 0.3, 0.15 + random.nextDouble() * 0.15, (random.nextDouble() - 0.5) * 0.3));
				}
				hit(level, player, SoundEvents.PLAYER_HURT, 0.45F, i == THROWS.length - 1);
			}
		}

		for (int c = 0; c < coal.size(); c++) {
			Display.ItemDisplay piece = coal.get(c);
			Vec3 speed = coalSpeed.get(c);
			Vec3 at = piece.position().add(speed);
			double ground = floor(level, at.x, at.z, player.getY()) + 0.15;
			if (at.y < ground) {
				at = new Vec3(at.x, ground, at.z);
				speed = Vec3.ZERO;
			} else {
				speed = speed.add(0.0, -0.04, 0.0);
			}
			coalSpeed.set(c, speed);
			place(piece, at, new Vector3f(0.4F), new Quaternionf().rotateY(c + t * 0.2F));
		}

		if (t == THROWS[1] + FLIGHT + 30) {
			speak(minecraft, level, "fakedeath.weihnachtsmann.next");
		}
	}
}
