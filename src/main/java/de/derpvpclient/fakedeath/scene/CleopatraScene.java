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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Alexandria. Sandstone columns, a golden throne, a flute playing. Cleopatra welcomes the guest and sets down a basket
 * of figs in front of them: "A fig? Freshly picked." The figs move. A cobra rises out of the basket, sways to the
 * flute, spreads its hood and hisses. "Oh. The figs have a tenant." It strikes.
 */
public class CleopatraScene extends CastScene {
	private static final int WELCOME = ARRIVE + 8;
	private static final int BASKET = ARRIVE + 50;
	private static final int FIG = BASKET + 22;
	private static final int RUSTLE = FIG + 40;
	private static final int RISE = RUSTLE + 20;
	private static final int RISE_TICKS = 30;
	private static final int TENANT = RISE + RISE_TICKS + 10;
	private static final int STRIKE = TENANT + 45;
	private static final int DEATH = STRIKE + 40;
	private static final int SEGMENTS = 12;
	private static final ChatFormatting EGYPT = ChatFormatting.GOLD;
	/** The hiss of a cat: adult cat sounds depend on their sound variant in 26.3, this is the classic one by id. */
	private static final net.minecraft.sounds.SoundEvent HISS = net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.Identifier.withDefaultNamespace("entity.cat.hiss"));
	/** The scene's own tune on the flute, in the scale of the snake charmers, over a low drone. */
	private static final int[][] TUNE = {{64, 4}, {65, 2}, {68, 2}, {69, 4}, {71, 4}, {72, 2}, {71, 2}, {69, 2}, {68, 2}, {65, 4}, {64, 8},
		{68, 2}, {69, 2}, {71, 2}, {72, 2}, {74, 4}, {72, 2}, {71, 2}, {69, 4}, {68, 2}, {65, 2}, {64, 12}};
	private static final int[][] CHARMER = {{76, 2}, {77, 2}, {76, 2}, {74, 2}, {72, 4}, {71, 2}, {72, 2}, {71, 2}, {69, 2}, {68, 4}, {69, 2},
		{71, 2}, {72, 2}, {71, 2}, {69, 2}, {68, 2}, {65, 4}, {64, 8}};

	private final List<Display.BlockDisplay> snake = new ArrayList<>();
	private RemotePlayer cleopatra;
	private Display.BlockDisplay basket;
	private final List<Display.ItemDisplay> figs = new ArrayList<>();
	private Vec3 basketAt;
	private Vec3 look;
	private float poison;

	@Override
	public int length() {
		return DEATH + 1;
	}

	@Override
	public int photoTick() {
		return TENANT + 10;
	}

	@Override
	public int screenTint() {
		// Desert light, then the poison: green at the edges, darker.
		return poison > 0.0F ? (int) (Math.min(1.0F, poison) * 170.0F) << 24 | 0x1E3A0A : 0x16FFD080;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.cleopatra", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (cleopatra == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();
		// The flute, and the drone under it.
		int tuneLength = Band.length(TUNE, 3.0F);
		if (tick < RISE) {
			Band.play(level, cleopatra.position(), TUNE, (tick - 2) % tuneLength, 3.0F, 1.0F, Band.FLUTE, Band.HARP);
		} else if (tick < STRIKE) {
			Band.play(level, basketAt, CHARMER, (tick - RISE) % Band.length(CHARMER, 2.5F), 2.5F, 1.2F, Band.FLUTE);
		}
		if (tick < STRIKE && tick % 24 == 2) {
			Band.note(level, cleopatra.position(), Band.DIDGERIDOO, 40, 0.7F);
		}

		face(cleopatra, tick >= RUSTLE && tick < TENANT ? basketAt : eyes);
		if (tick == WELCOME) {
			speak(level, cleopatra, "fakedeath.cleopatra.welcome", EGYPT, 45);
		}
		if (tick == BASKET) {
			// She brings the basket over and sets it down in front of the player.
			hold(cleopatra, ItemStack.EMPTY);
			basket = blockDisplay(level, Blocks.BARREL.defaultBlockState(), basketAt, 2.0F);
			place(basket, basketAt.add(0.0, 0.2, 0.0), new Vector3f(0.5F, 0.4F, 0.5F), new Quaternionf());
			for (int i = 0; i < 5; i++) {
				Vec3 at = basketAt.add((i % 3 - 1) * 0.12, 0.44 + (i / 3) * 0.06, (i / 3 - 0.5) * 0.12);
				Display.ItemDisplay fig = itemDisplay(level, new ItemStack(Items.CHORUS_FRUIT), at);
				place(fig, at, new Vector3f(0.22F), new Quaternionf().rotateY(i * 1.3F).rotateX(Mth.HALF_PI));
				figs.add(fig);
			}
			Scene.sound(level, basketAt, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
		}
		if (tick == FIG) {
			pose(cleopatra, Skins.Pose.POINT);
			speak(level, cleopatra, "fakedeath.cleopatra.fig", EGYPT, 40);
		}
		if (tick == FIG + 30) {
			pose(cleopatra, null);
		}
		if (tick >= RUSTLE && tick < RISE && tick % 4 == 0) {
			// Something moves under the figs.
			particles(level, ParticleTypes.CRIT, basketAt.add(0.0, 0.5, 0.0), 2, 0.15, 0.02);
			Scene.sound(level, basketAt, SoundEvents.AZALEA_LEAVES_STEP, SoundSource.NEUTRAL, 0.8F, 1.4F);
			for (Display.ItemDisplay fig : figs) {
				fig.setPos(fig.getX() + (random.nextDouble() - 0.5) * 0.03, fig.getY(), fig.getZ() + (random.nextDouble() - 0.5) * 0.03);
			}
		}
		if (tick == RISE) {
			// A slim brown body, the hood lighter, the head, two black eyes and the red tongue.
			for (int i = 0; i < SEGMENTS; i++) {
				boolean hood = i >= SEGMENTS - 4 && i < SEGMENTS - 1;
				snake.add(blockDisplay(level, hood ? Blocks.CONCRETE.orange().defaultBlockState() : Blocks.CONCRETE.brown().defaultBlockState(), basketAt, 4.0F));
			}
			for (int i = 0; i < 2; i++) {
				snake.add(blockDisplay(level, Looks.BLACK_CONCRETE, basketAt, 4.0F));
			}
			snake.add(blockDisplay(level, Blocks.CONCRETE.red().defaultBlockState(), basketAt, 4.0F));
			Scene.sound(level, basketAt, HISS, SoundSource.HOSTILE, 1.5F, 0.6F);
		}
		if (!snake.isEmpty()) {
			moveSnake(tick, eyes);
		}
		if (tick >= RISE && tick < STRIKE && tick % 20 == 5) {
			Scene.sound(level, basketAt.add(0.0, 1.0, 0.0), HISS, SoundSource.HOSTILE, 1.0F, 0.5F + random.nextFloat() * 0.2F);
		}
		if (tick == TENANT) {
			speak(level, cleopatra, "fakedeath.cleopatra.tenant", EGYPT, 40);
		}
		if (tick == STRIKE) {
			Scene.sound(level, eyes, HISS, SoundSource.HOSTILE, 2.0F, 0.8F);
			Scene.sound(level, eyes, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2F, 1.6F);
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.35F);
			addEffect(player, MobEffects.POISON, 200);
			cameraShake(0.2F);
		}
		if (tick == STRIKE + 6) {
			cameraShake(0.0F);
		}
		if (tick > STRIKE) {
			// The poison spreads, the view sinks and sways.
			poison = Math.min(1.0F, (tick - STRIKE) / 40.0F);
			moveCamera(new Vec3(0.0, -0.9 * poison * poison, 0.0));
		}
		if (tick == DEATH) {
			kill(level, player, SoundEvents.PLAYER_HURT);
		}

		if (tick < RUSTLE) {
			turnTowards(player, cleopatra.getEyePosition(), 0.08F);
		} else if (tick < STRIKE) {
			turnTowards(player, snake.isEmpty() ? basketAt.add(0.0, 0.4, 0.0) : snake.get(SEGMENTS - 2).position().add(0.0, -0.1, 0.0), 0.12F);
		} else {
			turnTowards(player, cleopatra.getEyePosition(), 0.04F);
		}
	}

	/** The cobra rising out of the basket, swaying, the hood spread, and then the strike at the face. */
	private void moveSnake(int tick, Vec3 eyes) {
		float rise = Mth.clamp((tick - RISE) / (float) RISE_TICKS, 0.0F, 1.0F);
		double sway = Math.sin(tick * 0.12) * 0.12 * rise;
		Vec3 side = look.yRot(Mth.HALF_PI);
		float strike = tick >= STRIKE ? Math.min(1.0F, (tick - STRIKE) / 4.0F) : 0.0F;
		Vec3 base = basketAt.add(0.0, 0.42, 0.0);
		float yaw = (float) Math.atan2(-look.x, -look.z);
		Vec3 head = base;
		float[] hoodWidth = {0.26F, 0.38F, 0.32F};
		for (int i = 0; i < SEGMENTS; i++) {
			float t = i / (float) (SEGMENTS - 1);
			// Upright and swaying, more at the top, the neck bent towards the player.
			Vec3 at = base.add(0.0, t * 1.2 * rise, 0.0).add(side.scale(sway * t * t)).add(look.scale(-0.15 * t * t * rise));
			if (strike > 0.0F) {
				at = at.lerp(eyes.add(0.0, -0.2, 0.0).add(look.scale(0.3 * (1.0F - t))), strike * t * t);
			}
			boolean isHead = i == SEGMENTS - 1;
			int hood = i - (SEGMENTS - 4);
			Vector3f size = isHead ? new Vector3f(0.18F, 0.1F, 0.24F)
				: hood >= 0 ? new Vector3f(0.12F + (hoodWidth[hood] - 0.12F) * rise, 0.17F, 0.05F) : new Vector3f(0.12F, 0.16F, 0.12F);
			place(snake.get(i), isHead ? at.add(look.scale(-0.06)) : at, size, new Quaternionf().rotateY(yaw));
			if (isHead) {
				head = at.add(look.scale(-0.06));
			}
		}
		// The eyes on the head, the tongue flicking out now and then.
		for (int e = 0; e < 2; e++) {
			Vec3 eye = head.add(side.scale(e == 0 ? 0.06 : -0.06)).add(look.scale(-0.1)).add(0.0, 0.04, 0.0);
			place(snake.get(SEGMENTS + e), eye, new Vector3f(0.04F), new Quaternionf().rotateY(yaw));
		}
		boolean flick = tick % 12 < 3 || strike > 0.0F;
		place(snake.get(SEGMENTS + 2), head.add(look.scale(flick ? -0.2 : -0.08)), new Vector3f(0.02F, 0.02F, flick ? 0.14F : 0.01F), new Quaternionf().rotateY(yaw));
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		float yaw = (float) Math.atan2(look.x, look.z);
		Vec3 hall = onGround(level, player, player.position().add(look.scale(4.5)));
		// Sandstone columns in two rows, a golden throne between them.
		BlockState column = Blocks.CUT_SANDSTONE.defaultBlockState();
		for (int s : new int[] {-1, 1}) {
			for (int k = 0; k < 3; k++) {
				Vec3 foot = onGround(level, player, hall.add(side.scale(s * 2.6)).add(look.scale(-1.5 + k * 1.8)));
				place(blockDisplay(level, column, foot, 8.0F), foot.add(0.0, 2.0, 0.0), new Vector3f(0.6F, 4.0F, 0.6F), new Quaternionf().rotateY(yaw));
				place(blockDisplay(level, Blocks.GOLD_BLOCK.defaultBlockState(), foot, 2.0F), foot.add(0.0, 4.1, 0.0), new Vector3f(0.8F, 0.2F, 0.8F), new Quaternionf().rotateY(yaw));
			}
		}
		Vec3 throne = hall.add(look.scale(1.2));
		place(blockDisplay(level, Blocks.GOLD_BLOCK.defaultBlockState(), throne, 3.0F), throne.add(0.0, 0.3, 0.0), new Vector3f(1.0F, 0.6F, 0.8F), new Quaternionf().rotateY(yaw));
		place(blockDisplay(level, Blocks.GOLD_BLOCK.defaultBlockState(), throne, 3.0F), throne.add(look.scale(0.35)).add(0.0, 1.1, 0.0), new Vector3f(1.0F, 1.6F, 0.15F), new Quaternionf().rotateY(yaw));
		place(blockDisplay(level, Blocks.CONCRETE.blue().defaultBlockState(), throne, 3.0F), throne.add(0.0, 0.62, 0.0), new Vector3f(0.8F, 0.06F, 0.6F), new Quaternionf().rotateY(yaw));
		cleopatra = actor(level, "fakedeath.cleopatra.name", "cleopatra", true, onGround(level, player, hall.add(look.scale(-0.6))));
		hold(cleopatra, new ItemStack(Items.CHORUS_FRUIT));
		arrive(level, player, cleopatra, 0);
		basketAt = onGround(level, player, player.position().add(look.scale(1.8)));
	}
}
