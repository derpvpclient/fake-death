package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
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
 * An office. Bill Gates in his sweater at a computer: "Welcome! Just a few quick updates first." The screen counts
 * up, "Für Elise" plays as hold music, and the count gets stuck at 99. "Only a moment longer." The music stutters. Then
 * the world goes blue, panel by panel all around the player, a big sad face and "Your world ran into a problem and needs
 * to restart", the percentage counting up. At 100 % the player is restarted. Gates: "That never happens to me."
 */
public class GatesScene extends CastScene {
	private static final int WELCOME = ARRIVE + 8;
	private static final int UPDATES = ARRIVE + 30;
	private static final int STUCK = UPDATES + 100;
	private static final int MOMENT = STUCK + 10;
	private static final int BLUE = STUCK + 50;
	private static final int PANELS = 16;
	private static final int PANEL_EVERY = 2;
	private static final int CRASH = BLUE + PANELS * PANEL_EVERY + 10;
	private static final int DEATH = CRASH + 100;
	private static final ChatFormatting OFFICE = ChatFormatting.AQUA;
	/** "Für Elise" (Beethoven, 1810), the famous opening, as hold music. */
	private static final int[][] ELISE = {{76, 1}, {75, 1}, {76, 1}, {75, 1}, {76, 1}, {71, 1}, {74, 1}, {72, 1}, {69, 3}, {60, 1}, {64, 1}, {69, 1},
		{71, 3}, {64, 1}, {68, 1}, {71, 1}, {72, 3}, {64, 1}, {76, 1}, {75, 1}, {76, 1}, {75, 1}, {76, 1}, {71, 1}, {74, 1}, {72, 1},
		{69, 3}, {60, 1}, {64, 1}, {69, 1}, {71, 3}, {64, 1}, {72, 1}, {71, 1}, {69, 6}, {0, 6}};
	private static final float STEP = 3.0F;

	private final List<Display.BlockDisplay> panels = new ArrayList<>();
	private RemotePlayer gates;
	private Display.TextDisplay monitor;
	private Display.TextDisplay sad;
	private Display.TextDisplay problem;
	private Display.TextDisplay percent;
	private Vec3 desk;
	private Vec3 look;
	private Vec3 center;
	private float yaw;

	@Override
	public int length() {
		return DEATH + 2;
	}

	@Override
	public int photoTick() {
		return CRASH + 30;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.gates", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (gates == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();
		face(gates, tick < BLUE ? eyes : eyes.add(0.0, 0.5, 0.0));

		if (tick == WELCOME) {
			speak(level, gates, "fakedeath.gates.welcome", OFFICE, 45);
		}
		if (tick >= UPDATES && tick < BLUE) {
			// The hold music, stuttering once it is stuck.
			int since = (tick - UPDATES) % Band.length(ELISE, STEP);
			int midi = Band.noteAt(ELISE, since, STEP);
			if (midi > 0 && (tick < STUCK || random.nextFloat() < 0.7F)) {
				int wrong = tick >= STUCK ? random.nextInt(5) - 2 : 0;
				Band.note(level, desk, Band.CHIME, midi + wrong, 0.7F);
			}
			int shown = tick < STUCK ? (tick - UPDATES) * 99 / (STUCK - UPDATES) : 99;
			if (tick % 5 == 0) {
				relabel(monitor, Component.translatable("fakedeath.gates.installing", shown).withStyle(ChatFormatting.WHITE));
			}
		}
		if (tick == MOMENT) {
			speak(level, gates, "fakedeath.gates.moment", OFFICE, 40);
		}

		// The world goes blue around the player, panel by panel: walls on four sides, the ceiling last.
		int added = tick - BLUE;
		if (added >= 0 && added % PANEL_EVERY == 0 && added / PANEL_EVERY < PANELS) {
			int i = added / PANEL_EVERY;
			Display.BlockDisplay panel = blockDisplay(level, Blocks.CONCRETE.blue().defaultBlockState(), center, 10.0F);
			Looks.glow(panel);
			panels.add(panel);
			placePanel(panel, i);
			Scene.sound(level, eyes, Band.BIT, SoundSource.RECORDS, 0.6F, 0.5F + i * 0.05F);
		}
		if (tick == CRASH) {
			// A big sad face, the message, the counter.
			Vec3 front = center.add(look.scale(2.3)).add(0.0, 1.2, 0.0);
			sad = sign(level, front.add(look.scale(-0.05)).add(look.yRot(Mth.HALF_PI).scale(0.6)).add(0.0, 1.1, 0.0), Component.literal(":(").withStyle(ChatFormatting.WHITE), 4.0F, yaw, 0);
			problem = sign(level, front.add(look.scale(-0.05)).add(0.0, 0.55, 0.0), Component.translatable("fakedeath.gates.problem").withStyle(ChatFormatting.WHITE), 0.9F, yaw, 0);
			percent = sign(level, front.add(look.scale(-0.05)).add(0.0, 0.15, 0.0), Component.translatable("fakedeath.gates.percent", 0).withStyle(ChatFormatting.WHITE), 0.9F, yaw, 0);
			Scene.sound(level, eyes, Band.BIT, SoundSource.RECORDS, 1.2F, 0.5F);
		}
		if (tick > CRASH && percent != null && tick % 4 == 0) {
			int done = Math.min(100, (tick - CRASH) * 100 / (DEATH - CRASH));
			relabel(percent, Component.translatable("fakedeath.gates.percent", done).withStyle(ChatFormatting.WHITE));
		}
		if (tick == CRASH + 30) {
			speak(level, gates, "fakedeath.gates.never", OFFICE, 40);
		}
		if (tick == DEATH) {
			Scene.sound(level, eyes, Band.BIT, SoundSource.RECORDS, 1.5F, 2.0F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}

		if (tick < UPDATES + 10 || tick >= MOMENT && tick < MOMENT + 25) {
			turnTowards(player, gates.getEyePosition(), 0.08F);
		} else if (tick < BLUE + 6) {
			turnTowards(player, monitor.position(), 0.08F);
		} else {
			turnTowards(player, center.add(look.scale(2.3)).add(0.0, 2.2, 0.0), 0.1F);
		}
	}

	/** One panel of the blue box round the player: three across each wall, front first, then the ceiling. */
	private void placePanel(Display.BlockDisplay panel, int i) {
		Quaternionf turn = new Quaternionf().rotateY(yaw);
		Vec3 side = look.yRot(Mth.HALF_PI);
		if (i < 12) {
			int wall = i / 3;
			int k = i % 3 - 1;
			Vec3 normal = wall == 0 ? look : wall == 1 ? side : wall == 2 ? side.reverse() : look.reverse();
			Vec3 along = normal.yRot(Mth.HALF_PI);
			Vec3 at = center.add(normal.scale(2.4)).add(along.scale(k * 1.6)).add(0.0, 1.6, 0.0);
			boolean front = wall == 0 || wall == 3;
			place(panel, at, front ? new Vector3f(1.62F, 3.2F, 0.05F) : new Vector3f(0.05F, 3.2F, 1.62F), turn);
		} else {
			int k = i - 12;
			Vec3 at = center.add(look.scale((k / 2 - 0.5) * 2.4)).add(side.scale((k % 2 - 0.5) * 2.4)).add(0.0, 3.2, 0.0);
			place(panel, at, new Vector3f(2.42F, 0.05F, 2.42F), turn);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		yaw = (float) Math.atan2(look.x, look.z);
		center = player.position();
		desk = onGround(level, player, player.position().add(look.scale(3.5)).add(side.scale(0.8)));
		// The desk, the computer under it, the monitor on it.
		place(blockDisplay(level, Blocks.BIRCH_PLANKS.defaultBlockState(), desk, 3.0F), desk.add(0.0, 0.8, 0.0), new Vector3f(1.6F, 0.1F, 0.8F), new Quaternionf().rotateY(yaw));
		place(blockDisplay(level, Looks.GRAY_CONCRETE, desk, 3.0F), desk.add(side.scale(0.55)).add(0.0, 0.35, 0.0), new Vector3f(0.35F, 0.7F, 0.6F), new Quaternionf().rotateY(yaw));
		place(blockDisplay(level, Looks.BLACK_CONCRETE, desk, 3.0F), desk.add(look.scale(0.15)).add(0.0, 1.25, 0.0), new Vector3f(1.0F, 0.65F, 0.08F), new Quaternionf().rotateY(yaw));
		Display.BlockDisplay glass = blockDisplay(level, Blocks.CONCRETE.blue().defaultBlockState(), desk, 3.0F);
		Looks.glow(glass);
		place(glass, desk.add(look.scale(0.1)).add(0.0, 1.25, 0.0), new Vector3f(0.9F, 0.55F, 0.02F), new Quaternionf().rotateY(yaw));
		monitor = sign(level, desk.add(look.scale(0.08)).add(0.0, 1.2, 0.0), Component.literal(""), 0.35F, yaw, 0);
		gates = actor(level, "fakedeath.gates.name", "gates", false, onGround(level, player, desk.add(look.scale(0.7)).add(side.scale(-0.8))));
		arrive(level, player, gates, 0);
	}
}
