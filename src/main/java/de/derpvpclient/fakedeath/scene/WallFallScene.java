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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Berlin, 9 November 1989, evening. The Wall right in front of the player, graffiti on it. At a table at the side,
 * Günter Schabowski reads from his note. A reporter: "When does that come into force?" He searches the note: "As far
 * as I know ... immediately, without delay." The crowd streams up to the Wall, cheering, fireworks go up, "Ode to Joy"
 * plays, and they hammer away at the concrete. Then the segment right in front of the player gives way. Towards the
 * player.
 */
public class WallFallScene extends CastScene {
	private static final int QUESTION = ARRIVE + 8;
	private static final int IMMEDIATELY = ARRIVE + 40;
	private static final int RUSH = ARRIVE + 90;
	private static final int HAMMER = RUSH + 30;
	private static final int TOPPLE = HAMMER + 110;
	private static final int FALL = 22;
	private static final int DEATH = TOPPLE + FALL;
	private static final int SEGMENTS = 9;
	private static final int FALLING = SEGMENTS / 2;
	private static final int CROWD = 6;
	private static final ChatFormatting GDR = ChatFormatting.GOLD;
	/** "Ode to Joy" (Beethoven, 1824), played when the Wall fell: D major, quarter = 4 sixteenths. */
	private static final int[][] JOY = {{66, 4}, {66, 4}, {67, 4}, {69, 4}, {69, 4}, {67, 4}, {66, 4}, {64, 4}, {62, 4}, {62, 4}, {64, 4}, {66, 4},
		{66, 6}, {64, 2}, {64, 8}, {66, 4}, {66, 4}, {67, 4}, {69, 4}, {69, 4}, {67, 4}, {66, 4}, {64, 4}, {62, 4}, {62, 4}, {64, 4}, {66, 4},
		{64, 6}, {62, 2}, {62, 8}};
	/** One chord and bass note to each bar: D, A, D, A, D, A, D, D. */
	private static final int[][] CHORDS = {{62, 66, 69}, {61, 64, 69}, {62, 66, 69}, {61, 64, 69}, {62, 66, 69}, {61, 64, 69}, {62, 66, 69}, {62, 66, 69}};
	private static final int[] BASS = {38, 33, 38, 33, 38, 33, 38, 38};
	private static final float STEP = 2.5F;

	private final List<Display.BlockDisplay> segments = new ArrayList<>();
	private final List<Display.BlockDisplay> pipes = new ArrayList<>();
	private final List<Display.BlockDisplay> paint = new ArrayList<>();
	private final List<RemotePlayer> crowd = new ArrayList<>();
	private RemotePlayer schabowski;
	private RemotePlayer reporter;
	private Vec3 wall;
	private Vec3 look;
	private Vec3 side;
	private float yaw;

	@Override
	public int length() {
		return DEATH + 2;
	}

	@Override
	public int photoTick() {
		return HAMMER + 40;
	}

	@Override
	public int skyTint() {
		// A November evening.
		return 0xB0101830;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.mauerfall", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (schabowski == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();
		face(schabowski, tick < IMMEDIATELY - 10 ? schabowski.getEyePosition().add(look.scale(-1.0)).add(0.0, -0.8, 0.0) : eyes);
		face(reporter, schabowski.getEyePosition());
		if (tick == QUESTION) {
			speak(level, reporter, "fakedeath.mauerfall.question", ChatFormatting.GRAY, 35);
		}
		if (tick == IMMEDIATELY) {
			speak(level, schabowski, "fakedeath.mauerfall.immediately", GDR, 50);
		}

		// The crowd: up to the Wall, cheering, then hammering away at it.
		for (int i = 0; i < crowd.size(); i++) {
			RemotePlayer person = crowd.get(i);
			Vec3 spot = wall.add(side.scale((i - (CROWD - 1) / 2.0) * 1.3)).add(look.scale(-0.9));
			if (tick >= TOPPLE - 12 && Math.abs(i - (CROWD - 1) / 2.0) < 1.0) {
				// The two in front of the cracking segment jump aside. The player does not.
				spot = spot.add(side.scale(i < CROWD / 2 ? -1.8 : 1.8));
			}
			if (tick >= RUSH) {
				stepTowards(level, player, person, spot, 0.16);
			}
			face(person, tick < HAMMER ? wall.add(0.0, 2.5, 0.0) : spot.add(look.scale(1.0)).add(0.0, 1.2, 0.0));
			if (tick == RUSH + 18 + i * 2) {
				pose(person, Skins.Pose.CHEER);
				Scene.sound(level, person.position(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.2F, 0.8F + random.nextFloat() * 0.3F);
			}
			if (tick == HAMMER) {
				pose(person, null);
				hold(person, new ItemStack(i % 2 == 0 ? Items.IRON_PICKAXE : Items.STONE_PICKAXE));
			}
			if (tick >= HAMMER && tick < TOPPLE && (tick + i * 3) % 9 == 0) {
				Looks.swing(person);
				Vec3 hit = spot.add(look.scale(0.8)).add(0.0, 1.2, 0.0);
				Scene.sound(level, hit, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 1.0F, 0.7F + random.nextFloat() * 0.3F);
				particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState()), hit, 6, 0.2, 0.1);
			}
		}
		if (tick == RUSH) {
			speak(level, crowd.getFirst(), "fakedeath.mauerfall.mustgo", ChatFormatting.WHITE, 40);
		}
		if (tick >= RUSH && tick < DEATH && tick % 14 == 0) {
			// Fireworks over the Wall.
			Vec3 burst = wall.add(look.scale(8.0)).add(side.scale((random.nextDouble() - 0.5) * 16.0)).add(0.0, 12.0 + random.nextDouble() * 6.0, 0.0);
			particles(level, ParticleTypes.FIREWORK, burst, 40, 1.5, 0.15);
			Scene.sound(level, burst, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR, SoundSource.AMBIENT, 2.0F, 0.9F + random.nextFloat() * 0.2F);
		}
		if (tick >= RUSH) {
			music(level, tick - RUSH);
		}

		// The segment in front of the player cracks and tips over towards them, its paint and its pipe with it.
		if (tick == TOPPLE - 10) {
			Scene.sound(level, wall, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 2.0F, 0.5F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState()), wall.add(0.0, 0.3, 0.0), 30, 0.6, 0.1);
		}
		float tip = tick < TOPPLE ? 0.0F : Math.min(1.0F, (tick - TOPPLE) / (float) FALL);
		float angle = tip * tip * 1.45F;
		placeSegment(FALLING, angle);
		if (tick == DEATH) {
			Scene.sound(level, eyes, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 2.0F, 0.5F);
			Scene.sound(level, eyes, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 2.0F, 0.6F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState()), eyes, 40, 1.0, 0.2);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}

		if (tick < RUSH) {
			turnTowards(player, tick < QUESTION + 20 ? reporter.getEyePosition() : schabowski.getEyePosition(), 0.08F);
		} else if (tick < TOPPLE - 10) {
			turnTowards(player, wall.add(side.scale(Math.sin(tick * 0.02) * 2.5)).add(0.0, 2.6, 0.0), 0.06F);
		} else {
			turnTowards(player, wall.add(look.scale(-angle * 1.5)).add(0.0, 3.2 - angle, 0.0), 0.2F);
		}
	}

	/** The band: the tune on trumpet and flute, a chord, the bass and the drum on every bar, the bells over it. */
	private void music(ClientLevel level, int since) {
		int loop = Band.length(JOY, STEP);
		int inLoop = since % loop;
		Vec3 at = wall.add(look.scale(-2.0));
		Band.play(level, at, JOY, inLoop, STEP, 1.1F, Band.TRUMPET, Band.FLUTE, Band.BELL);
		int bar = Math.round(16 * STEP);
		if (inLoop % bar == 0) {
			int index = (inLoop / bar) % CHORDS.length;
			Band.chord(level, at, Band.HARP, CHORDS[index], 0.4F);
			Band.note(level, at, Band.BASS, BASS[index], 0.9F);
			Scene.sound(level, at, Band.DRUM, SoundSource.RECORDS, 0.8F, 1.0F);
		}
	}

	/** A segment of the Wall standing up, or tipped over towards the player around the foot of its front face. */
	private void placeSegment(int i, float angle) {
		Vec3 base = wall.add(side.scale((i - (SEGMENTS - 1) / 2.0) * 1.22));
		Vec3 pivot = base.add(look.scale(-0.13));
		Vec3 toward = look.reverse();
		Vector3f axis = new Vector3f((float) toward.z, 0.0F, (float) -toward.x);
		Quaternionf fall = new Quaternionf().rotateAxis(angle, axis.x, axis.y, axis.z);
		Quaternionf turn = new Quaternionf(fall).mul(new Quaternionf().rotateY(yaw));
		Vector3f body = fall.transform(new Vector3f((float) look.x * 0.13F, 1.8F, (float) look.z * 0.13F));
		place(segments.get(i), pivot.add(body.x, body.y, body.z), new Vector3f(1.2F, 3.6F, 0.26F), turn);
		Vector3f top = fall.transform(new Vector3f((float) look.x * 0.13F, 3.75F, (float) look.z * 0.13F));
		place(pipes.get(i), pivot.add(top.x, top.y, top.z), new Vector3f(1.22F, 0.4F, 0.4F), turn);
		for (int p = 0; p < 2; p++) {
			Display.BlockDisplay patch = paint.get(i * 2 + p);
			Vector3f spot = fall.transform(new Vector3f((float) toward.x * 0.01F + (float) side.x * (p == 0 ? -0.25F : 0.25F),
				p == 0 ? 1.2F : 2.3F, (float) toward.z * 0.01F + (float) side.z * (p == 0 ? -0.25F : 0.25F)));
			place(patch, pivot.add(spot.x, spot.y, spot.z), new Vector3f(p == 0 ? 0.7F : 0.5F, p == 0 ? 0.6F : 0.4F, 0.02F), turn);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		look = horizontalLook(player);
		side = look.yRot(Mth.HALF_PI);
		yaw = (float) Math.atan2(look.x, look.z);
		wall = onGround(level, player, player.position().add(look.scale(3.6)));
		BlockState concrete = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
		BlockState[] colors = {Blocks.RED_CONCRETE.defaultBlockState(), Blocks.YELLOW_CONCRETE.defaultBlockState(), Blocks.LIME_CONCRETE.defaultBlockState(),
			Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(), Blocks.MAGENTA_CONCRETE.defaultBlockState(), Blocks.ORANGE_CONCRETE.defaultBlockState()};
		// The Wall: a row of concrete segments with the round pipe on top and graffiti on the western side.
		for (int i = 0; i < SEGMENTS; i++) {
			segments.add(blockDisplay(level, concrete, wall, 12.0F));
			pipes.add(blockDisplay(level, Blocks.SMOOTH_STONE.defaultBlockState(), wall, 12.0F));
			for (int p = 0; p < 2; p++) {
				paint.add(blockDisplay(level, colors[(i * 2 + p) % colors.length], wall, 12.0F));
			}
		}
		for (int i = 0; i < SEGMENTS; i++) {
			placeSegment(i, 0.0F);
		}
		// Schabowski's table at the left, the reporter in front of it.
		Vec3 table = onGround(level, player, player.position().add(look.scale(2.0)).add(side.scale(3.0)));
		place(blockDisplay(level, Blocks.BIRCH_PLANKS.defaultBlockState(), table, 3.0F), table.add(0.0, 0.8, 0.0), new Vector3f(1.4F, 0.1F, 0.7F), new Quaternionf().rotateY(yaw + 0.6F));
		schabowski = actor(level, "fakedeath.mauerfall.schabowski", "schabowski", false, onGround(level, player, table.add(look.scale(0.6)).add(side.scale(0.3))));
		hold(schabowski, Looks.prop(Items.PAPER, "invoice"));
		arrive(level, player, schabowski, 0);
		reporter = actor(level, "fakedeath.mauerfall.reporter", "berlinerin", true, onGround(level, player, table.add(look.scale(-1.0)).add(side.scale(-0.4))));
		arrive(level, player, reporter, 6);
		for (int i = 0; i < CROWD; i++) {
			Vec3 at = onGround(level, player, player.position().add(look.scale(-6.0 - i % 2)).add(side.scale((i - (CROWD - 1) / 2.0) * 1.6)));
			crowd.add(extra(level, i % 2 == 0 ? "berliner" : "berlinerin", at));
		}
	}
}
