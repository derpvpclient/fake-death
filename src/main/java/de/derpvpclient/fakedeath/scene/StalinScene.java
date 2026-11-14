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
 * The Soviet Union. Stalin at his desk, two villagers holding up great red flags with the hammer and sickle, a choir of
 * villagers singing the anthem, note by note. Everybody sings. Except the player. Stalin notices. "Comrade ... why
 * are you not singing?" He takes the red pencil and crosses a name off the list. The colours drain out of the world,
 * and the player is retouched out of history like out of the old photographs.
 */
public class StalinScene extends CastScene {
	/** The opening of the anthem's melody (public domain music), as MIDI notes and beats. */
	private static final int[][] MELODY = {{67, 2}, {72, 4}, {67, 3}, {69, 1}, {71, 4}, {64, 2}, {64, 2}, {69, 4}, {67, 3}, {65, 1}, {67, 4},
		{60, 2}, {60, 2}, {62, 4}, {62, 3}, {64, 1}, {65, 4}, {65, 3}, {67, 1}, {69, 4}, {71, 2}, {72, 2}, {74, 6}};
	private static final int HALF_BEAT = 4;
	private static final int SING = 25;
	private static final int NOTICE = 135;
	private static final int WHY = NOTICE + 10;
	private static final int PENCIL = WHY + 55;
	private static final int ERASE = PENCIL + 25;
	private static final int ERASE_TICKS = 50;
	private static final int DEATH = ERASE + ERASE_TICKS;
	private static final ChatFormatting RED = ChatFormatting.RED;

	private final List<Villager> choir = new ArrayList<>();
	private final List<Display.ItemDisplay> flags = new ArrayList<>();
	private final List<Villager> bearers = new ArrayList<>();
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
		return SING + 60;
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
		int note = noteTicks.indexOf(tick - SING);
		if (note >= 0 && tick < ERASE) {
			float pitch = (float) Math.pow(2.0, (MELODY[note][0] - 66) / 12.0);
			float volume = tick < NOTICE ? 1.4F : 0.6F;
			sound(level, desk, SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.RECORDS, volume, pitch);
			sound(level, desk, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.RECORDS, volume * 0.8F, pitch);
			sound(level, choir.get(note % choir.size()).position(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, volume * 0.7F, pitch);
			Vec3 by = choir.get(note % choir.size()).getEyePosition().add(0.0, 0.6, 0.0);
			level.addParticle(ParticleTypes.NOTE, by.x, by.y, by.z, (MELODY[note][0] - 60) / 24.0, 0.0, 0.0);
		}
		for (int i = 0; i < choir.size(); i++) {
			// Singing with feeling: heads up, nodding along to the beat.
			Villager singer = choir.get(i);
			double nod = tick >= SING && tick < ERASE ? Math.sin((tick + i * 3) * 0.4) * 0.25 : 0.0;
			face(singer, tick >= WHY ? eyes : stalin.getEyePosition().add(0.0, 1.0 + nod, 0.0));
		}
		for (int i = 0; i < bearers.size(); i++) {
			// The flags are held up high and swing a little.
			Villager bearer = bearers.get(i);
			face(bearer, eyes);
			Vec3 hand = bearer.position().add(horizontalLook(player).yRot(i == 0 ? Mth.HALF_PI : -Mth.HALF_PI).scale(0.45)).add(0.0, 2.6, 0.0);
			Vec3 toPlayer = player.position().subtract(bearer.position());
			float yaw = (float) Math.atan2(toPlayer.x, toPlayer.z);
			place(flags.get(i), hand, new Vector3f(1.6F), new Quaternionf().rotateY(yaw).rotateZ(Mth.sin(tick * 0.06F + i) * 0.06F));
		}

		face(stalin, tick < NOTICE ? desk.add(horizontalLook(player).scale(-3.0)).add(0.0, 1.0, 0.0) : eyes);
		if (tick >= SING && tick < NOTICE && tick % 16 == 0) {
			// Conducting.
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
				particles(level, new net.minecraft.core.particles.DustParticleOptions(net.minecraft.world.phys.Vec3.fromRGB24(0xD01010).toVector3f(), 0.6F), paper, 2, 0.15, 0.0);
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
		turnTowards(player, tick < WHY - 5 ? stalin.getEyePosition().add(horizontalLook(player).yRot(Mth.HALF_PI).scale(Math.sin(tick * 0.02) * 2.0)) : stalin.getEyePosition(), 0.08F);
		if (tick == DEATH) {
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		Vec3 look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		float yaw = (float) Math.atan2(look.x, look.z);
		desk = onGround(level, player, player.position().add(look.scale(5.0)));
		place(blockDisplay(level, Blocks.DARK_OAK_PLANKS.defaultBlockState(), desk, 4.0F), desk.add(0.0, 0.85, 0.0), new Vector3f(2.2F, 0.12F, 1.0F), new Quaternionf().rotateY(yaw));
		place(blockDisplay(level, Blocks.RED_CONCRETE.defaultBlockState(), desk, 4.0F), desk.add(0.0, 0.42, 0.0), new Vector3f(2.1F, 0.8F, 0.9F), new Quaternionf().rotateY(yaw));
		stalin = actor(level, "fakedeath.stalin.name", "stalin", false, onGround(level, player, desk.add(look.scale(0.9))));
		// Behind him the big flag of the Soviet Union.
		standFlag(level, player, desk.add(look.scale(2.2)).add(side.scale(-1.2)), "flag_soviet", 2.0F);
		// Two villagers holding up flags at his sides.
		for (int s : new int[] {-1, 1}) {
			Villager bearer = spawn(level, EntityType.VILLAGER, onGround(level, player, desk.add(side.scale(s * 2.4)).add(look.scale(0.4))));
			bearer.noPhysics = true;
			bearers.add(bearer);
			flags.add(itemDisplay(level, Looks.prop(Items.IRON_NUGGET, "flag_soviet"), bearer.position()));
		}
		// The choir in a row in front of the desk, off to the side, all facing him.
		for (int i = 0; i < 6; i++) {
			Vec3 at = onGround(level, player, player.position().add(look.scale(2.2 + (i % 2) * 0.6)).add(side.scale(2.4 + i * 0.75)));
			Villager singer = spawn(level, EntityType.VILLAGER, at);
			singer.noPhysics = true;
			choir.add(singer);
		}
		int t = 0;
		for (int[] note : MELODY) {
			noteTicks.add(t);
			t += note[1] * HALF_BEAT;
		}
	}
}
