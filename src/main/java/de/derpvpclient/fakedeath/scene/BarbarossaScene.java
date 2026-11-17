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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Inside the Kyffhäuser mountain. In the light of a few lanterns Emperor Barbarossa sleeps at a stone table, as he has
 * for eight hundred years, his red beard grown right through the table and on along the floor. Ravens circle in the
 * dark. He mumbles in his sleep: "Are the ravens still flying round the mountain?" They fly off, one by one. His eyes
 * open: "Then it is time!" He rises, the beard tears the stone table out of the ground, and it comes flying.
 */
public class BarbarossaScene extends CastScene {
	private static final int MUMBLE = ARRIVE + 15;
	private static final int RAVENS_GO = ARRIVE + 70;
	private static final int WAKE = ARRIVE + 110;
	private static final int RISE = WAKE + 40;
	private static final int THROW = 14;
	private static final int DEATH = RISE + THROW;
	private static final int RAVENS = 3;
	private static final int BEARD = 10;
	private static final ChatFormatting EMPEROR = ChatFormatting.GOLD;
	/** The scene's own tune, slow and old, in the Dorian mode. */
	private static final int[][] TUNE = {{62, 4}, {64, 2}, {65, 2}, {67, 4}, {69, 4}, {67, 2}, {65, 2}, {64, 4}, {62, 8},
		{69, 4}, {71, 2}, {72, 2}, {74, 4}, {72, 4}, {71, 2}, {69, 2}, {67, 4}, {69, 8}};
	private static final float STEP = 4.0F;

	private final List<Display.BlockDisplay> ravens = new ArrayList<>();
	private final List<Display.BlockDisplay> beard = new ArrayList<>();
	private final List<Display.BlockDisplay> table = new ArrayList<>();
	private RemotePlayer barbarossa;
	private Vec3 tableAt;
	private Vec3 look;
	private float yaw;

	@Override
	public int length() {
		return DEATH + 2;
	}

	@Override
	public int photoTick() {
		return WAKE + 8;
	}

	@Override
	public int screenTint() {
		// Deep in the mountain: dark, only the lanterns.
		return 0x8A06040A;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.barbarossa", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (barbarossa == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();
		if (tick < WAKE) {
			Band.play(level, tableAt, TUNE, (tick - 2) % Band.length(TUNE, STEP), STEP, 0.8F, Band.FLUTE, Band.HARP);
			if (tick % 32 == 2) {
				Band.note(level, tableAt, Band.DIDGERIDOO, 38, 0.6F);
			}
		}

		// Asleep with the head on the chest, then awake and looking at the player.
		face(barbarossa, tick < WAKE ? barbarossa.getEyePosition().add(look.scale(-1.0)).add(0.0, -1.2, 0.0) : eyes);
		if (tick == MUMBLE) {
			speak(level, barbarossa, "fakedeath.barbarossa.ravens", ChatFormatting.GRAY, 50);
		}
		if (tick == WAKE) {
			Skins.give(barbarossa.getId(), "barbarossa", false, false);
			speak(level, barbarossa, "fakedeath.barbarossa.time", EMPEROR, 40);
			Scene.sound(level, tableAt, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.AMBIENT, 1.0F, 0.5F);
			// A big chord, like an organ.
			Band.chord(level, tableAt, Band.HARP, new int[] {62, 65, 69}, 0.6F);
			Band.note(level, tableAt, Band.BASS, 38, 1.2F);
			Band.note(level, tableAt, Band.DIDGERIDOO, 38, 1.0F);
			cameraShake(0.08F);
		}
		if (tick == WAKE + 10) {
			cameraShake(0.0F);
		}

		moveRavens(level, tick);
		moveBeard(tick, eyes);
		moveTable(level, player, tick, eyes);

		if (tick < MUMBLE) {
			turnTowards(player, barbarossa.getEyePosition(), 0.06F);
		} else if (tick < WAKE - 10) {
			// Up at the ravens, circling in the dark.
			Display.BlockDisplay raven = ravens.get((tick / 30) % ravens.size());
			turnTowards(player, raven.position(), 0.05F);
		} else if (tick < RISE) {
			turnTowards(player, barbarossa.getEyePosition(), 0.12F);
		} else {
			turnTowards(player, table.getFirst().position(), 0.3F);
		}
	}

	/** Three ravens circling high in the hall, wings beating, then off into the dark one by one. */
	private void moveRavens(ClientLevel level, int tick) {
		for (int r = 0; r < RAVENS; r++) {
			double angle = tick * 0.04 + r * Mth.TWO_PI / RAVENS;
			Vec3 center = tableAt.add(look.scale(-0.8)).add(0.0, 4.2 + r * 0.4, 0.0);
			Vec3 at = center.add(Math.cos(angle) * 3.2, Math.sin(tick * 0.1 + r) * 0.3, Math.sin(angle) * 3.2);
			int away = tick - RAVENS_GO - r * 12;
			if (away > 0) {
				// Off and away into the dark.
				at = at.add(look.scale(away * 0.4)).add(0.0, away * 0.08, 0.0);
			}
			if (tick % 40 == r * 13) {
				Scene.sound(level, at, SoundEvents.PARROT_AMBIENT, SoundSource.NEUTRAL, 1.0F, 0.45F);
			}
			float heading = (float) -angle;
			float flap = Mth.sin(tick * 0.7F + r) * 0.6F;
			int base = r * 4;
			place(ravens.get(base), at, new Vector3f(0.22F, 0.2F, 0.5F), new Quaternionf().rotateY(heading));
			place(ravens.get(base + 1), at.add(0.0, 0.12, 0.0).add(new Vec3(Math.sin(heading), 0.0, Math.cos(heading)).scale(0.3)), new Vector3f(0.16F), new Quaternionf().rotateY(heading));
			for (int w = 0; w < 2; w++) {
				int s = w == 0 ? 1 : -1;
				Vec3 wing = at.add(new Vec3(Math.cos(heading), 0.0, -Math.sin(heading)).scale(s * 0.3));
				place(ravens.get(base + 2 + w), wing, new Vector3f(0.5F, 0.04F, 0.3F), new Quaternionf().rotateY(heading).rotateZ(s * flap));
			}
		}
	}

	/** The beard, down from the chin, through the table and along the floor towards the player, growing all the time. */
	private void moveBeard(int tick, Vec3 eyes) {
		Vec3 chin = barbarossa.position().add(look.scale(-0.2)).add(0.0, 1.35, 0.0);
		float grow = Math.min(1.0F, tick / (float) WAKE);
		Vec3 floor = tableAt.add(look.scale(-0.6)).add(0.0, 0.05, 0.0);
		Vec3 end = floor.lerp(new Vec3(eyes.x, floor.y, eyes.z).add(look.scale(-0.3)), grow);
		for (int i = 0; i < BEARD; i++) {
			float t = i / (float) (BEARD - 1);
			// Down to the table top first, then along the floor.
			Vec3 at = t < 0.3F ? chin.lerp(floor.add(0.0, 0.8, 0.0), t / 0.3F) : floor.lerp(end, (t - 0.3F) / 0.7F);
			if (tick >= RISE && t < 0.3F) {
				// Torn upwards with him.
				at = at.add(0.0, Math.min(1.0, (tick - RISE) / 6.0) * 0.3, 0.0);
			}
			place(beard.get(i), at, new Vector3f(0.22F - t * 0.08F, 0.16F, 0.12F), new Quaternionf().rotateY(yaw + i * 0.4F));
		}
	}

	/** The stone table: still, then torn up with the beard and thrown at the player. */
	private void moveTable(ClientLevel level, LocalPlayer player, int tick, Vec3 eyes) {
		Vec3 at = tableAt;
		Quaternionf turn = new Quaternionf().rotateY(yaw);
		if (tick >= RISE) {
			float t = Math.min(1.0F, (tick - RISE) / (float) THROW);
			at = tableAt.lerp(eyes.add(0.0, -0.6, 0.0), t * t).add(0.0, Math.sin(t * Math.PI) * 1.2, 0.0);
			turn.rotateX(-t * 1.8F);
			if (tick == RISE) {
				Scene.sound(level, tableAt, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 2.0F, 0.5F);
				Scene.sound(level, tableAt, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.0F, 0.6F);
				particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), tableAt.add(0.0, 0.3, 0.0), 30, 0.8, 0.2);
				pose(barbarossa, Skins.Pose.CHEER);
			}
		}
		place(table.get(0), at.add(0.0, 0.85, 0.0), new Vector3f(1.6F, 0.2F, 1.0F), turn);
		place(table.get(1), at.add(0.0, 0.4, 0.0), new Vector3f(0.5F, 0.8F, 0.5F), turn);
		if (tick == DEATH) {
			Scene.sound(level, eyes, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 2.0F, 0.6F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), eyes, 30, 0.5, 0.3);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		yaw = (float) Math.atan2(look.x, look.z);
		tableAt = onGround(level, player, player.position().add(look.scale(3.2)));
		BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
		for (int i = 0; i < 2; i++) {
			table.add(blockDisplay(level, i == 0 ? Blocks.SMOOTH_STONE.defaultBlockState() : stone, tableAt, 12.0F));
		}
		// The hall inside the mountain: rock walls all round, the ceiling over the ravens, a stone floor.
		Vec3 middle = tableAt.add(look.scale(-0.8));
		Quaternionf straight = new Quaternionf().rotateY(yaw);
		BlockState rock = Blocks.STONE.defaultBlockState();
		BlockState rough = Blocks.COBBLESTONE.defaultBlockState();
		place(blockDisplay(level, rock, middle, 24.0F), middle.add(look.scale(4.6)).add(0.0, 3.2, 0.0), new Vector3f(10.0F, 6.6F, 0.6F), straight);
		place(blockDisplay(level, rock, middle, 24.0F), middle.add(look.scale(-4.6)).add(0.0, 3.2, 0.0), new Vector3f(10.0F, 6.6F, 0.6F), straight);
		place(blockDisplay(level, rough, middle, 24.0F), middle.add(side.scale(4.8)).add(0.0, 3.2, 0.0), new Vector3f(0.6F, 6.6F, 9.6F), straight);
		place(blockDisplay(level, rough, middle, 24.0F), middle.add(side.scale(-4.8)).add(0.0, 3.2, 0.0), new Vector3f(0.6F, 6.6F, 9.6F), straight);
		place(blockDisplay(level, rough, middle, 24.0F), middle.add(0.0, 6.5, 0.0), new Vector3f(10.2F, 0.6F, 9.8F), straight);
		place(blockDisplay(level, Blocks.SMOOTH_STONE.defaultBlockState(), middle, 24.0F), middle.add(0.0, 0.02, 0.0), new Vector3f(9.6F, 0.04F, 9.2F), straight);
		// Rough rock pillars and lanterns in the dark.
		for (int s : new int[] {-1, 1}) {
			Vec3 foot = onGround(level, player, tableAt.add(side.scale(s * 2.8)).add(look.scale(0.6)));
			place(blockDisplay(level, Blocks.COBBLESTONE.defaultBlockState(), foot, 6.0F), foot.add(0.0, 2.0, 0.0), new Vector3f(1.0F, 4.0F, 1.0F), new Quaternionf().rotateY(yaw + s * 0.3F));
			Display.BlockDisplay lantern = blockDisplay(level, Blocks.LANTERN.defaultBlockState(), foot, 2.0F);
			Looks.glow(lantern);
			place(lantern, foot.add(side.scale(-s * 0.6)).add(0.0, 1.8, 0.0), new Vector3f(0.6F), new Quaternionf());
		}
		Display.BlockDisplay candle = blockDisplay(level, Blocks.LANTERN.defaultBlockState(), tableAt, 2.0F);
		Looks.glow(candle);
		place(candle, tableAt.add(side.scale(0.5)).add(0.0, 1.1, 0.0), new Vector3f(0.4F), new Quaternionf());
		for (int i = 0; i < BEARD; i++) {
			beard.add(blockDisplay(level, Blocks.ORANGE_WOOL.defaultBlockState(), tableAt, 12.0F));
		}
		for (int i = 0; i < RAVENS * 4; i++) {
			Display.BlockDisplay part = blockDisplay(level, i % 4 == 1 ? Blocks.GRAY_CONCRETE.defaultBlockState() : Looks.BLACK_CONCRETE, tableAt, 24.0F);
			ravens.add(part);
		}
		barbarossa = actor(level, "fakedeath.barbarossa.name", "barbarossa_sleep", false, onGround(level, player, tableAt.add(look.scale(0.9))));
		wear(barbarossa, Looks.prop(Items.IRON_NUGGET, "crown"), 0.3);
	}
}
