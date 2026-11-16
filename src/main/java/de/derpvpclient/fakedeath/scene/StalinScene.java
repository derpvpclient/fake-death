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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The Soviet Union. Stalin walks to his desk, two soldiers march in holding up great red flags with the hammer and
 * sickle, a choir of villagers lines up and sings the anthem: the first lines of the verse, then the refrain. Everybody sings.
 * Except the player. Stalin notices. "Comrade ... why are you not singing?" He takes the red pencil and crosses a name
 * off the list. The colours drain out of the world, and the player is retouched out of history like out of the old
 * photographs.
 */
public class StalinScene extends CastScene {
	/**
	 * The anthem's melody (Alexandrov, 1944; public domain music) after the score: the first two lines of the verse,
	 * then the first two lines of the refrain. MIDI notes and lengths in sixteenths.
	 */
	private static final int[][] MELODY = {
		// Со-юз не-ру-ши-мый рес-пуб-лик сво-бод-ных
		{67, 2}, {72, 4}, {67, 3}, {69, 1}, {71, 4}, {64, 2}, {64, 2}, {69, 4}, {67, 3}, {65, 1}, {67, 4}, {60, 3}, {60, 1},
		// Спло-ти-ла на-ве-ки Ве-ли-ка-я Русь.
		{62, 4}, {62, 3}, {64, 1}, {65, 4}, {65, 2}, {67, 2}, {69, 4}, {71, 3}, {72, 1}, {74, 6}, {67, 2},
		// Сла-вься, О-те-че-ство на-ше сво-бод-ное,
		{76, 8}, {74, 2}, {72, 2}, {71, 2}, {72, 2}, {74, 6}, {67, 2}, {67, 8},
		// Друж-бы на-ро-дов на-дёж-ный оп-лот!
		{72, 8}, {71, 2}, {69, 2}, {67, 2}, {69, 2}, {71, 6}, {64, 2}, {64, 8}};
	/** The first note comes before the first bar. */
	private static final int PICKUP = 2;
	private static final int[] CHORD_C = {55, 60, 64};
	private static final int[] CHORD_EM = {55, 59, 64};
	private static final int[] CHORD_F = {57, 60, 65};
	private static final int[] CHORD_DM = {57, 62, 65};
	private static final int[] CHORD_D = {57, 62, 66};
	private static final int[] CHORD_G7 = {55, 59, 62, 65};
	/** The harmony of the score, one chord to each half bar: C Em F C, Dm F D G7, then the refrain C Em G7 G7, F Em Em Em. */
	private static final int[][] CHORDS = {CHORD_C, CHORD_EM, CHORD_F, CHORD_C, CHORD_DM, CHORD_F, CHORD_D, CHORD_G7,
		CHORD_C, CHORD_EM, CHORD_G7, CHORD_G7, CHORD_F, CHORD_EM, CHORD_EM, CHORD_EM};
	private static final int[] BASS = {36, 40, 41, 36, 38, 41, 38, 43, 36, 40, 43, 43, 41, 40, 40, 40};
	/** Ticks to a sixteenth: a beat is 16 ticks, 75 beats a minute, the tempo of the score. */
	private static final int STEP = 4;
	private static final int SING = ARRIVE + 12;
	/** He notices when the second line of the refrain starts. */
	private static final int NOTICE = SING + at(98);
	private static final int WHY = NOTICE + 12;
	private static final int PENCIL = WHY + 55;
	/** Erased once the choir has sung the last note of the line. */
	private static final int ERASE = SING + at(122) + 6;
	private static final int ERASE_TICKS = 50;
	private static final int DEATH = ERASE + ERASE_TICKS;
	private static final ChatFormatting RED = ChatFormatting.RED;

	private final List<Villager> choir = new ArrayList<>();
	private final List<Display.ItemDisplay> flags = new ArrayList<>();
	private final List<RemotePlayer> bearers = new ArrayList<>();
	private final List<Integer> noteTicks = new ArrayList<>();
	private RemotePlayer stalin;
	private Display.ItemDisplay pencil;
	private Vec3 desk;
	private float grey;
	private boolean gone;
	private int now;

	@Override
	public int length() {
		return DEATH + 1;
	}

	@Override
	public int photoTick() {
		return SING + at(40);
	}

	/** The tick of the song at a number of sixteenths. */
	private static int at(int sixteenths) {
		return sixteenths * STEP;
	}

	private static float pitchOf(int midi, int base) {
		return (float) Math.pow(2.0, (midi - base) / 12.0);
	}

	@Override
	public int screenTint() {
		return grey > 0.0F ? ((int) (Math.min(grey, 1.0F) * 235.0F) << 24) | 0x8E8E8E : 0;
	}

	@Override
	public InteractionHand hiddenHand() {
		// Erased: the own hand is the first thing to go.
		return now >= ERASE + 10 ? InteractionHand.MAIN_HAND : null;
	}

	@Override
	public boolean hidesBody() {
		return gone;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.stalin", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		now = tick;
		if (tick == 0) {
			setUp(level, player);
		}
		if (stalin == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();

		// The anthem, sung by the whole choir, quieter once he has noticed.
		int beat = tick - SING;
		float volume = tick < NOTICE ? 1.4F : 0.5F;
		int note = noteTicks.indexOf(beat);
		if (note >= 0 && tick < ERASE) {
			// The tune on trumpet, with flute an octave above and harp.
			int midi = MELODY[note][0];
			float pitch = pitchOf(midi, 66);
			sound(level, desk, SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.RECORDS, volume, pitch);
			sound(level, desk, SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.RECORDS, volume * 0.5F, pitch);
			sound(level, desk, SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.RECORDS, volume * 0.6F, pitch);
			Villager singer = choir.get(note % choir.size());
			if (MELODY[note][1] >= 3) {
				// The long notes are sung out by the choir.
				sound(level, singer.position(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, volume * 0.4F, pitch);
			}
			Vec3 by = singer.getEyePosition().add(0.0, 0.6, 0.0);
			level.addParticle(ParticleTypes.NOTE, by.x, by.y, by.z, (midi - 60) / 24.0, 0.0, 0.0);
		}
		// The chords on every beat, a new chord and the bass every half bar, the drum on the first beat of the bar.
		int sinceBar = beat - at(PICKUP);
		if (sinceBar >= 0 && sinceBar % at(4) == 0 && tick < ERASE) {
			int quarter = sinceBar / at(4);
			int half = quarter / 2;
			if (half < CHORDS.length) {
				boolean strong = quarter % 2 == 0;
				for (int midi : CHORDS[half]) {
					sound(level, desk, SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.RECORDS, volume * (strong ? 0.45F : 0.3F), pitchOf(midi, 66));
				}
				if (strong) {
					boolean first = quarter % 4 == 0;
					sound(level, desk, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.RECORDS, volume * (first ? 1.0F : 0.8F), pitchOf(BASS[half], 42));
					if (first) {
						sound(level, desk, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.RECORDS, volume * 0.5F, 1.0F);
					}
				}
			}
		}

		for (int i = 0; i < choir.size(); i++) {
			// Singing with feeling: heads up, nodding along to the beat.
			Villager singer = choir.get(i);
			double nod = tick >= SING && tick < ERASE ? Math.sin((tick + i * 3) * 0.4) * 0.25 : 0.0;
			face(singer, tick >= WHY ? eyes : stalin.getEyePosition().add(0.0, 1.0 + nod, 0.0));
		}
		for (int i = 0; i < bearers.size(); i++) {
			// The pole held upright in front of the chest, the arm out, the flag high above, swinging a little.
			RemotePlayer bearer = bearers.get(i);
			face(bearer, eyes);
			if (tick == ARRIVE - 6 + i * 2) {
				pose(bearer, Skins.Pose.POINT);
			}
			Vec3 facing = Vec3.directionFromRotation(0.0F, bearer.getYRot());
			Vec3 right = new Vec3(-facing.z, 0.0, facing.x);
			boolean held = Skins.poseOf(bearer.getId()) == Skins.Pose.POINT;
			// The hand at the end of the stretched arm, or down at the side while marching in.
			Vec3 hand = bearer.position().add(right.scale(held ? 0.33 : 0.42)).add(facing.scale(held ? 0.66 : 0.12));
			Vec3 toPlayer = player.position().subtract(bearer.position());
			float yaw = (float) Math.atan2(toPlayer.x, toPlayer.z);
			// The pole sits half a block off the middle of the model (the display turns it around): shifted so it runs right through the hand.
			Vec3 pole = new Vec3(Math.cos(yaw), 0.0, -Math.sin(yaw)).scale(0.5 * 1.5);
			place(flags.get(i), hand.subtract(pole).add(0.0, 2.6, 0.0), new Vector3f(1.5F), new Quaternionf().rotateY(yaw));
		}

		face(stalin, tick < NOTICE ? desk.add(horizontalLook(player).scale(-3.0)).add(0.0, 1.0, 0.0) : eyes);
		if (tick >= SING + at(PICKUP) && tick < NOTICE && (tick - SING - at(PICKUP)) % at(16) == 0) {
			// Conducting, once a bar.
			Looks.swing(stalin);
		}
		if (tick == NOTICE) {
			sound(level, stalin.position(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 0.6F);
		}
		if (tick == WHY) {
			speak(level, stalin, "fakedeath.stalin.why", RED, 50);
		}
		if (tick == PENCIL) {
			// The red pencil comes out, the list on the desk, a line through a name.
			pose(stalin, Skins.Pose.POINT);
			speak(level, stalin, "fakedeath.stalin.cross", RED, 40);
			pencil = itemDisplay(level, Looks.prop(Items.PAPER, "invoice"), desk.add(0.0, 1.0, 0.0));
		}
		if (pencil != null && tick < ERASE + 10) {
			Vec3 paper = desk.add(horizontalLook(player).scale(0.3)).add(0.0, 0.95, 0.0);
			place(pencil, paper, new Vector3f(0.5F), new Quaternionf().rotateX(Mth.HALF_PI).rotateZ(tick * 0.02F));
			if (tick > PENCIL + 5 && tick < ERASE && tick % 4 == 0) {
				sound(level, paper, SoundEvents.BOOK_PAGE_TURN, SoundSource.NEUTRAL, 1.0F, 1.6F);
				particles(level, new net.minecraft.core.particles.DustParticleOptions(new Vector3f(0.82F, 0.06F, 0.06F), 0.6F), paper, 2, 0.15, 0.0);
			}
		}
		if (tick >= ERASE) {
			// The colours drain away, the player fades out of the picture.
			float p = (tick - ERASE) / (float) ERASE_TICKS;
			grey = p * 0.95F;
			gone = p > 0.4F;
			if (tick % 10 == 0) {
				sound(level, eyes, SoundEvents.BOOK_PAGE_TURN, SoundSource.MASTER, 1.0F, 0.5F);
			}
		}
		Vec3 watch = tick < ARRIVE ? stalin.getEyePosition() : stalin.getEyePosition().add(horizontalLook(player).yRot(Mth.HALF_PI).scale(Math.sin((tick - ARRIVE) * 0.02) * 1.6));
		turnTowards(player, tick < WHY - 5 ? watch : stalin.getEyePosition(), 0.08F);
		if (tick == DEATH) {
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		Vec3 look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		float yaw = (float) Math.atan2(look.x, look.z);
		desk = onGround(level, player, player.position().add(look.scale(3.4)));
		place(blockDisplay(level, Blocks.DARK_OAK_PLANKS.defaultBlockState(), desk, 4.0F), desk.add(0.0, 0.85, 0.0), new Vector3f(2.2F, 0.12F, 1.0F), new Quaternionf().rotateY(yaw));
		place(blockDisplay(level, Blocks.RED_CONCRETE.defaultBlockState(), desk, 4.0F), desk.add(0.0, 0.42, 0.0), new Vector3f(2.1F, 0.8F, 0.9F), new Quaternionf().rotateY(yaw));
		stalin = actor(level, "fakedeath.stalin.name", "stalin", false, onGround(level, player, desk.add(look.scale(0.9))));
		arrive(level, player, stalin, 0);
		// Behind him the big flag of the Soviet Union.
		standFlag(level, player, desk.add(look.scale(2.2)).add(side.scale(-1.2)), "flag_soviet", 2.0F);
		// Two soldiers holding up flags at his sides.
		for (int s : new int[] {-1, 1}) {
			RemotePlayer bearer = extra(level, "general", onGround(level, player, desk.add(side.scale(s * 2.3)).add(look.scale(0.4))));
			arrive(level, player, bearer, s < 0 ? 4 : 7);
			bearers.add(bearer);
			flags.add(itemDisplay(level, Looks.prop(Items.IRON_NUGGET, "flag_soviet"), bearer.position()));
		}
		// The choir in a row in front of the desk, off to the side, all facing him.
		for (int i = 0; i < 6; i++) {
			Vec3 at = onGround(level, player, player.position().add(look.scale(2.5 + (i % 2) * 0.6)).add(side.scale(2.7 + i * 0.75)));
			Villager singer = spawn(level, EntityType.VILLAGER, at);
			singer.noPhysics = true;
			arrive(level, player, singer, 10 + i * 3);
			choir.add(singer);
		}
		int eighths = 0;
		for (int[] note : MELODY) {
			noteTicks.add(at(eighths));
			eighths += note[1];
		}
	}
}
