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
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Berlin, a parade. Black, white and red flags, a Zeppelin drifting over the rooftops, a military band playing "Heil dir
 * im Siegerkranz". Kaiser Wilhelm II rides up on his white horse, the golden spike on his helmet, the moustache turned
 * up: "I no longer know any parties, I know only Germans!" Then he sees the player standing in the middle of the
 * parade ground. "Who is that? Never mind. Parade ... march!" The column of spiked helmets marches in step, straight
 * over the player, and does not stop.
 */
public class WilhelmScene extends CastScene {
	private static final int PARTIES = ARRIVE + 8;
	private static final int WHO = ARRIVE + 64;
	private static final int MARCH = ARRIVE + 104;
	private static final int ROWS = 4;
	private static final double START = 8.0;
	/** A step every quarter of the band: 8 ticks, a good stride each. */
	private static final int BEAT = 8;
	private static final double PACE = 0.7 / BEAT;
	private static final ChatFormatting KAISER = ChatFormatting.GOLD;
	/** "Heil dir im Siegerkranz", the anthem of the Empire, to the old melody of "God Save the King". G major, 3/4. */
	private static final int[][] ANTHEM = {{67, 4}, {67, 4}, {69, 4}, {66, 6}, {67, 2}, {69, 4}, {71, 4}, {71, 4}, {72, 4}, {71, 6}, {69, 2}, {67, 4},
		{69, 4}, {67, 4}, {66, 4}, {67, 12},
		{74, 4}, {74, 4}, {74, 4}, {74, 6}, {72, 2}, {71, 4}, {72, 4}, {72, 4}, {72, 4}, {72, 6}, {71, 2}, {69, 4},
		{71, 4}, {72, 2}, {71, 2}, {69, 2}, {67, 2}, {71, 6}, {72, 2}, {74, 4}, {76, 2}, {72, 2}, {71, 4}, {69, 4}, {67, 12}};
	/** One chord and bass note to each bar. */
	private static final int[][] CHORDS = {{55, 59, 62}, {54, 57, 62}, {55, 59, 64}, {54, 57, 62}, {57, 62, 66}, {55, 59, 62},
		{55, 59, 62}, {55, 59, 62}, {55, 60, 64}, {54, 57, 62}, {55, 59, 62}, {55, 59, 62}, {55, 60, 64}, {55, 59, 62}};
	private static final int[] BASS = {43, 38, 40, 38, 38, 43, 43, 43, 36, 38, 43, 43, 36, 43};
	private static final float STEP = 2.0F;
	private static final int MUSIC = 6;

	private final List<RemotePlayer> soldiers = new ArrayList<>();
	private final List<Vec3> files = new ArrayList<>();
	private final List<RemotePlayer> band = new ArrayList<>();
	private final List<Display.BlockDisplay> zeppelin = new ArrayList<>();
	private RemotePlayer kaiser;
	private Horse horse;
	private Vec3 look;
	private Vec3 airship;
	private int trampled;
	private int death = Integer.MAX_VALUE;

	@Override
	public int length() {
		return MARCH + 170;
	}

	@Override
	public int photoTick() {
		return MARCH + 50;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.wilhelm", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (kaiser == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();
		music(level, tick);

		// The Kaiser on his horse at the side of the parade ground.
		horse.positionRider(kaiser);
		face(horse, eyes.add(look.scale(2.0)));
		face(kaiser, tick < MARCH + 20 ? eyes : soldiers.getFirst().getEyePosition());
		if (tick == PARTIES) {
			speak(level, kaiser, "fakedeath.wilhelm.parties", KAISER, 55);
		}
		if (tick == WHO) {
			pose(kaiser, Skins.Pose.POINT);
			speak(level, kaiser, "fakedeath.wilhelm.who", KAISER, 38);
		}
		if (tick == MARCH) {
			pose(kaiser, Skins.Pose.RAISE);
			speak(level, kaiser, "fakedeath.wilhelm.march", KAISER, 40);
			horse.setStanding(20);
			Scene.sound(level, kaiser.position(), SoundEvents.HORSE_ANGRY, SoundSource.NEUTRAL, 1.2F, 1.0F);
		}

		for (int i = 0; i < band.size(); i++) {
			RemotePlayer musician = band.get(i);
			face(musician, eyes);
			if (tick >= MUSIC && (tick - MUSIC) % BEAT == 0 && i == band.size() - 1) {
				Looks.swing(musician);
			}
		}

		// The column: standing to attention, then marching in step, straight through where the player stands.
		double marched = tick < MARCH ? 0.0 : (tick - MARCH) * PACE;
		for (int i = 0; i < soldiers.size(); i++) {
			RemotePlayer soldier = soldiers.get(i);
			int row = i / 2;
			Vec3 at = player.position().add(files.get(i)).add(look.scale(START + row * 1.4 - marched));
			if (tick >= MARCH) {
				walk(soldier, onGround(level, player, at));
			}
			face(soldier, at.add(look.scale(-4.0)).add(0.0, 1.6, 0.0));
			if (tick >= MARCH && (tick - MARCH) % BEAT == 0 && i % 2 == 0) {
				Scene.sound(level, soldier.position(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 0.6F, 0.7F);
				Scene.sound(level, soldier.position(), SoundEvents.STONE_STEP, SoundSource.NEUTRAL, 1.2F, 0.7F);
			}
		}
		// Every pair of boots that reaches the player hurts; the last pair is the end.
		if (tick >= MARCH) {
			int reached = (int) Math.floor((marched - START + 0.3) / 1.4) + 1;
			while (trampled < Math.min(ROWS, reached)) {
				trampled++;
				cameraShake(0.25F);
				particles(level, ParticleTypes.CRIT, eyes.add(0.0, -0.8, 0.0), 10, 0.3, 0.2);
				if (trampled < ROWS) {
					hurt(level, player, SoundEvents.PLAYER_HURT, 0.28F);
				} else {
					death = tick + 3;
				}
			}
			if ((tick - MARCH) % BEAT == 3) {
				cameraShake(0.0F);
			}
		}
		if (tick == death) {
			kill(level, player, SoundEvents.PLAYER_HURT);
		}

		// The Zeppelin drifts slowly over the rooftops.
		Vec3 ship = airship.add(look.yRot(Mth.HALF_PI).scale(-tick * 0.05));
		Vec3 across = look.yRot(Mth.HALF_PI);
		float shipYaw = (float) Math.atan2(-across.z, across.x);
		for (int i = 0; i < zeppelin.size(); i++) {
			Quaternionf turn = new Quaternionf().rotateY(shipYaw);
			Vector3f size;
			Vec3 at = ship;
			if (i < 3) {
				// The body: three long boxes turned round its axis make it round.
				turn.rotateX(i * Mth.PI / 6.0F);
				size = new Vector3f(14.0F, 2.8F, 2.8F);
			} else if (i < 5) {
				// Nose and tail, a little smaller.
				Vec3 along = look.yRot(Mth.HALF_PI).scale(i == 3 ? 7.6 : -7.6);
				at = ship.add(along);
				size = new Vector3f(1.6F, 2.0F, 2.0F);
			} else if (i == 5) {
				// The gondola underneath.
				at = ship.add(0.0, -1.8, 0.0);
				size = new Vector3f(2.6F, 0.7F, 0.8F);
			} else {
				// The fins at the tail.
				at = ship.add(look.yRot(Mth.HALF_PI).scale(-6.4)).add(0.0, i == 6 ? 1.6 : 0.0, 0.0);
				size = i == 6 ? new Vector3f(2.0F, 1.6F, 0.12F) : new Vector3f(2.0F, 0.12F, 4.4F);
			}
			place(zeppelin.get(i), at, size, turn);
		}

		if (tick < MARCH + 10) {
			turnTowards(player, kaiser.getEyePosition(), 0.07F);
		} else if (trampled < ROWS) {
			// The column coming, the spikes glinting, closer and closer.
			RemotePlayer next = soldiers.get(Math.min(soldiers.size() - 1, trampled * 2));
			turnTowards(player, next.getEyePosition().add(0.0, 0.3, 0.0), 0.12F);
		}
	}

	private void music(ClientLevel level, int tick) {
		Vec3 at = band.isEmpty() ? kaiser.position() : band.getFirst().position();
		int since = tick - MUSIC;
		int loop = Band.length(ANTHEM, STEP);
		if (since < 0 || tick >= death) {
			return;
		}
		since %= loop;
		Band.play(level, at, ANTHEM, since, STEP, 1.2F, Band.TRUMPET, Band.FLUTE);
		if (since % (12 * (int) STEP) == 0) {
			int bar = since / (12 * (int) STEP);
			if (bar < CHORDS.length) {
				Band.chord(level, at, Band.HARP, CHORDS[bar], 0.35F);
				Band.note(level, at, Band.BASS, BASS[bar], 1.0F);
			}
		}
		// The drum: big on the first beat of the bar, the snare on the others.
		if (since % (4 * (int) STEP) == 0) {
			boolean first = since % (12 * (int) STEP) == 0;
			Scene.sound(level, at, first ? Band.DRUM : Band.SNARE, SoundSource.RECORDS, first ? 0.9F : 0.5F, 1.0F);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		Vec3 ground = onGround(level, player, player.position().add(look.scale(5.0)));
		// The flags of the Empire at both sides of the parade ground.
		standFlag(level, player, ground.add(side.scale(4.2)).add(look.scale(3.0)), "flag_empire", 1.3F);
		standFlag(level, player, ground.add(side.scale(-4.2)).add(look.scale(3.0)), "flag_empire", 1.3F);

		// The Kaiser on his white horse, at the left of the view.
		Vec3 spot = onGround(level, player, player.position().add(look.scale(5.0)).add(side.scale(3.0)));
		horse = spawn(level, EntityTypes.HORSE, spot);
		horse.noPhysics = true;
		Looks.saddle(horse);
		kaiser = actor(level, "fakedeath.wilhelm.name", "wilhelm", false, spot.add(0.0, 1.0, 0.0));
		wear(kaiser, Looks.prop(Items.IRON_NUGGET, "pickelhaube_gold"), 0.45);
		kaiser.startRiding(horse);

		// The band at the right, with horns and a big drum.
		for (int i = 0; i < 3; i++) {
			RemotePlayer musician = extra(level, "prussian", onGround(level, player, ground.add(side.scale(-3.0 - i * 0.9)).add(look.scale(0.5 + i * 0.4))));
			musician.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, Looks.prop(Items.IRON_NUGGET, "pickelhaube"));
			hold(musician, new ItemStack(i == 2 ? Items.NOTE_BLOCK : Items.GOAT_HORN));
			arrive(level, player, musician, 4 + i * 4);
			band.add(musician);
		}

		// The column, two files, the spiked helmets, the bayonets shouldered.
		for (int i = 0; i < ROWS * 2; i++) {
			Vec3 file = side.scale(i % 2 == 0 ? 0.42 : -0.42);
			files.add(file);
			int row = i / 2;
			Vec3 at = onGround(level, player, player.position().add(file).add(look.scale(START + row * 1.4)));
			RemotePlayer soldier = extra(level, "prussian", at);
			soldier.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, Looks.prop(Items.IRON_NUGGET, "pickelhaube"));
			hold(soldier, new ItemStack(Items.IRON_SWORD));
			soldiers.add(soldier);
		}

		// The Zeppelin, high over the rooftops, drifting across.
		airship = player.position().add(look.scale(30.0)).add(side.scale(10.0)).add(0.0, 20.0, 0.0);
		for (int i = 0; i < 8; i++) {
			Display.BlockDisplay part = blockDisplay(level, i == 5 ? Blocks.DARK_OAK_PLANKS.defaultBlockState()
				: i >= 6 ? Blocks.CONCRETE.gray().defaultBlockState() : Blocks.CONCRETE.lightGray().defaultBlockState(), airship, 40.0F);
			Looks.farView(part);
			zeppelin.add(part);
		}
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		if (kaiser != null) {
			kaiser.stopRiding();
		}
		super.end(level, player);
	}
}
