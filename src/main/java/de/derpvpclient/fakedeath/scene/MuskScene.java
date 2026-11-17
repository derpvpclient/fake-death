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
 * A launch site in Texas. A huge steel rocket on its launch mount, the tower beside it. Elon Musk in a black T-shirt:
 * "Next stop: Mars!" A countdown on a big board, the opening fanfare of "Also sprach Zarathustra", and it lifts off on a
 * column of fire. Three seconds later it explodes. Musk, delighted: "Rapid unscheduled disassembly!" Pieces of steel
 * rain down, and the nose comes down right on the player.
 */
public class MuskScene extends CastScene {
	private static final int MARS = ARRIVE + 8;
	private static final int COUNT = ARRIVE + 50;
	private static final int COUNT_EVERY = 10;
	private static final int LIFTOFF = COUNT + 10 * COUNT_EVERY;
	private static final int BOOM = LIFTOFF + 60;
	private static final int RUD = BOOM + 12;
	private static final int NOSE_FALL = 50;
	private static final int DEATH = BOOM + 20 + NOSE_FALL;
	private static final ChatFormatting SPACE = ChatFormatting.WHITE;
	/** The opening of "Also sprach Zarathustra" (Richard Strauss, 1896): C, G, C, then E and E flat; the timpani under it. */
	private static final int[][] SUNRISE = {{48, 16}, {55, 16}, {60, 16}, {64, 4}, {63, 20}};

	private final List<Part> rocket = new ArrayList<>();
	private final List<Part> debris = new ArrayList<>();
	private RemotePlayer musk;
	private Display.TextDisplay board;
	private Vec3 pad;
	private Vec3 look;
	private double height;
	private Part nose;

	@Override
	public int length() {
		return DEATH + 2;
	}

	@Override
	public int photoTick() {
		return BOOM + 4;
	}

	@Override
	public int screenTint() {
		int since = now - BOOM;
		return since >= 0 && since < 12 ? (200 - since * 16) << 24 | 0xFFE8C0 : 0;
	}

	private int now;

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.musk", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		now = tick;
		if (tick == 0) {
			setUp(level, player);
		}
		if (musk == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();
		face(musk, tick < LIFTOFF ? eyes : pad.add(0.0, height + 10.0, 0.0));
		if (tick == MARS) {
			pose(musk, Skins.Pose.POINT);
			speak(level, musk, "fakedeath.musk.mars", SPACE, 45);
		}
		if (tick == MARS + 40) {
			pose(musk, null);
		}
		for (int i = 0; i <= 10; i++) {
			if (tick == COUNT + i * COUNT_EVERY) {
				int left = 10 - i;
				relabel(board, Component.literal(left > 0 ? "T-" + left : "LIFTOFF").withStyle(left > 3 ? ChatFormatting.WHITE : ChatFormatting.RED, ChatFormatting.BOLD));
				Scene.sound(level, eyes, Band.PLING, SoundSource.RECORDS, 0.8F, left > 0 ? 1.0F : 2.0F);
			}
		}
		// The sunrise fanfare from "3" on: trumpets, organ bass, the timpani.
		int sunrise = tick - (LIFTOFF - 3 * COUNT_EVERY);
		int midi = Band.noteAt(SUNRISE, sunrise, 1.5F);
		if (midi > 0) {
			Band.note(level, eyes, Band.TRUMPET, midi + 12, 1.3F);
			Band.note(level, eyes, Band.BASS, midi - 12, 1.0F);
			Band.note(level, eyes, Band.DIDGERIDOO, midi - 12, 0.8F);
		}
		if (sunrise >= Math.round(52 * 1.5F) && sunrise < Math.round(76 * 1.5F) && sunrise % 6 == 0) {
			Scene.sound(level, eyes, Band.DRUM, SoundSource.RECORDS, 1.4F, sunrise % 12 == 0 ? 0.7F : 0.9F);
		}

		// Up it goes, slowly at first, on fire and smoke.
		if (tick >= LIFTOFF && tick < BOOM) {
			float t = (tick - LIFTOFF) / 60.0F;
			height = 30.0 * t * t;
			particles(level, ParticleTypes.FLAME, pad.add(0.0, height - 0.5, 0.0), 20, 0.6, 0.2);
			particles(level, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, pad.add(0.0, Math.max(0.0, height - 2.0), 0.0), 6, 1.5, 0.05);
			particles(level, ParticleTypes.LARGE_SMOKE, pad.add(0.0, 0.5, 0.0), 10, 3.0, 0.1);
			if (tick % 8 == 0) {
				Scene.sound(level, pad.add(0.0, height, 0.0), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 4.0F, 0.3F);
				Scene.sound(level, eyes, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.5F, 0.4F);
			}
			cameraShake(0.06F);
		}
		if (tick < BOOM) {
			for (Part part : rocket) {
				place(part.display(), pad.add(0.0, height, 0.0).add(part.offset()), part.size(), new Quaternionf().rotateY(part.turn()));
			}
		}
		if (tick == BOOM) {
			cameraShake(0.3F);
			Vec3 blast = pad.add(0.0, height + 8.0, 0.0);
			level.addParticle(ParticleTypes.EXPLOSION_EMITTER, blast.x, blast.y, blast.z, 1.0, 0.0, 0.0);
			particles(level, ParticleTypes.EXPLOSION, blast, 20, 4.0, 0.0);
			particles(level, ParticleTypes.FLAME, blast, 80, 4.0, 0.3);
			Scene.sound(level, eyes, Looks.EXPLODE, SoundSource.HOSTILE, 4.0F, 0.5F);
			Scene.sound(level, eyes, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.0F, 0.4F);
			// The rocket flies apart: the pieces tumble down all around, the nose straight at the player.
			for (Part part : rocket) {
				if (part == nose) {
					continue;
				}
				Vec3 to = onGround(level, player, player.position().add(look.yRot(random.nextFloat() * Mth.TWO_PI).scale(4.0 + random.nextDouble() * 10.0)));
				debris.add(new Part(part.display(), to.subtract(pad.add(0.0, height, 0.0).add(part.offset())), part.size(), random.nextFloat() * 3.0F));
			}
		}
		if (tick == BOOM + 6) {
			cameraShake(0.0F);
		}
		if (tick > BOOM) {
			float t = Math.min(1.0F, (tick - BOOM) / 40.0F);
			for (int i = 0; i < debris.size(); i++) {
				Part piece = debris.get(i);
				Part start = rocket.get(rocket.indexOf(rocket.stream().filter(p -> p.display() == piece.display()).findFirst().orElse(rocket.getFirst())));
				Vec3 from = pad.add(0.0, height, 0.0).add(start.offset());
				Vec3 at = from.add(piece.offset().scale(t)).add(0.0, Math.sin(t * Math.PI) * 6.0, 0.0);
				place(piece.display(), at, piece.size(), new Quaternionf().rotateXYZ(t * piece.turn() * 4.0F, t * piece.turn() * 2.0F, 0.0F));
				if (tick == BOOM + 40) {
					particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()), at, 8, 0.6, 0.2);
					Scene.sound(level, at, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5F, 0.6F);
				}
			}
			if (tick > BOOM + 20) {
				// The nose, tumbling, right at the player.
				float n = Math.min(1.0F, (tick - BOOM - 20) / (float) NOSE_FALL);
				Vec3 from = pad.add(0.0, height + 15.0, 0.0);
				Vec3 at = from.lerp(eyes.add(0.0, 0.8, 0.0), n * n);
				place(nose.display(), at, new Vector3f(2.2F), new Quaternionf().rotateXYZ(n * 6.0F, n * 3.0F, 0.0F));
				if (tick % 6 == 0) {
					particles(level, ParticleTypes.LARGE_SMOKE, at, 3, 0.5, 0.02);
				}
			}
		}
		if (tick == RUD) {
			pose(musk, Skins.Pose.CHEER);
			speak(level, musk, "fakedeath.musk.rud", SPACE, 45);
		}
		if (tick == DEATH) {
			Scene.sound(level, eyes, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 2.0F, 0.5F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()), eyes, 30, 0.8, 0.3);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}

		if (tick < COUNT) {
			turnTowards(player, musk.getEyePosition(), 0.08F);
		} else if (tick < BOOM + 22) {
			turnTowards(player, pad.add(0.0, height + 6.0, 0.0), tick < LIFTOFF ? 0.06F : 0.12F);
		} else {
			turnTowards(player, nose.display().position(), 0.2F);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		float yaw = (float) Math.atan2(look.x, look.z);
		pad = onGround(level, player, player.position().add(look.scale(24.0)).add(side.scale(4.0)));
		BlockState steel = Blocks.IRON_BLOCK.defaultBlockState();
		BlockState dark = Blocks.CONCRETE.gray().defaultBlockState();
		// The booster and the ship on top, steel, round from three boxes turned against each other; grid fins, flaps, the nose.
		for (int k = 0; k < 3; k++) {
			rocket.add(new Part(blockDisplay(level, steel, pad, 60.0F), new Vec3(0.0, 6.0, 0.0), new Vector3f(2.6F, 12.0F, 2.6F), yaw + k * Mth.PI / 6.0F));
			rocket.add(new Part(blockDisplay(level, steel, pad, 60.0F), new Vec3(0.0, 16.0, 0.0), new Vector3f(2.5F, 8.0F, 2.5F), yaw + k * Mth.PI / 6.0F));
		}
		rocket.add(new Part(blockDisplay(level, dark, pad, 60.0F), new Vec3(0.0, 0.6, 0.0), new Vector3f(2.8F, 1.2F, 2.8F), yaw));
		rocket.add(new Part(blockDisplay(level, dark, pad, 60.0F), new Vec3(0.0, 11.4, 0.0), new Vector3f(3.6F, 0.3F, 0.5F), yaw));
		rocket.add(new Part(blockDisplay(level, dark, pad, 60.0F), new Vec3(0.0, 18.5, 0.0), new Vector3f(3.8F, 1.0F, 0.2F), yaw));
		nose = new Part(blockDisplay(level, steel, pad, 60.0F), new Vec3(0.0, 21.0, 0.0), new Vector3f(1.9F, 2.2F, 1.9F), yaw + Mth.PI / 4.0F);
		rocket.add(nose);
		for (Part part : rocket) {
			Looks.farView(part.display());
		}
		// The launch tower beside it.
		Vec3 tower = pad.add(side.scale(-3.5));
		Display.BlockDisplay mast = blockDisplay(level, Looks.BLACK_CONCRETE, tower, 60.0F);
		Looks.farView(mast);
		place(mast, tower.add(0.0, 11.0, 0.0), new Vector3f(1.4F, 22.0F, 1.4F), new Quaternionf().rotateY(yaw));
		// The countdown board, big, in front.
		Vec3 boardAt = onGround(level, player, player.position().add(look.scale(10.0)).add(side.scale(4.5))).add(0.0, 3.6, 0.0);
		place(blockDisplay(level, Looks.BLACK_CONCRETE, boardAt, 6.0F), boardAt.add(look.scale(0.15)).add(0.0, 0.3, 0.0), new Vector3f(3.2F, 1.4F, 0.15F), new Quaternionf().rotateY(yaw));
		place(blockDisplay(level, Looks.GRAY_CONCRETE, boardAt, 6.0F), boardAt.add(look.scale(0.2)).add(0.0, -1.7, 0.0), new Vector3f(0.2F, 3.0F, 0.2F), new Quaternionf().rotateY(yaw));
		board = sign(level, boardAt, Component.literal("T-10").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), 3.0F, yaw, 0);
		musk = actor(level, "fakedeath.musk.name", "musk", false, onGround(level, player, player.position().add(look.scale(3.5)).add(side.scale(-1.2))));
		arrive(level, player, musk, 0);
	}

	private record Part(Display.BlockDisplay display, Vec3 offset, Vector3f size, float turn) {
	}
}
