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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Petrograd, April 1917. Lenin stands on an armoured car, arm stretched out, a crowd of workers around him under red
 * banners. "Comrades! All power to the Soviets!" The crowd cheers, arms up. Then he points at the player: "And that
 * one there ... is a bourgeois!" The crowd turns round. And runs.
 */
public class RevolutionScene extends CastScene {
	private static final int HOP_UP = ARRIVE;
	private static final int COMRADES = ARRIVE + 22;
	private static final int POWER = ARRIVE + 62;
	private static final int CHEER = POWER + 20;
	private static final int POINT = CHEER + 40;
	private static final int TURN = POINT + 25;
	private static final int STORM = TURN + 10;
	private static final int[] HITS = {STORM + 28, STORM + 36, STORM + 44};
	private static final int DEATH = STORM + 52;
	private static final int CROWD = 10;
	private static final ChatFormatting RED = ChatFormatting.RED;
	/**
	 * The refrain of the Internationale (Pierre De Geyter, 1888, public domain), as in the score in G major: "Völker,
	 * hört die Signale! Auf zum letzten Gefecht!" Midi note and length in sixteenths, a pickup of a quarter first.
	 */
	private static final int[][] TUNE = {{71, 3}, {69, 1}, {67, 8}, {62, 6}, {59, 2}, {64, 8}, {60, 4}, {69, 3}, {67, 1}, {66, 8}, {64, 6}, {62, 2}, {62, 8}};
	private static final int PICKUP = 4;
	/** One chord and bass note to each bar: G, C, D7, G. */
	private static final int[][] CHORDS = {{55, 59, 62}, {55, 60, 64}, {54, 57, 60, 62}, {55, 59, 62}};
	private static final int[] BASS = {43, 36, 38, 43};
	/** The band starts while the crowd is still coming in, 120 beats a minute like the score: two and a half ticks to a sixteenth. */
	private static final int MUSIC = 8;
	private static final int[] NOTE_TICKS = new int[TUNE.length];
	static {
		int units = 0;
		for (int i = 0; i < TUNE.length; i++) {
			NOTE_TICKS[i] = MUSIC + at(units);
			units += TUNE[i][1];
		}
	}

	private final List<RemotePlayer> crowd = new ArrayList<>();
	private final List<Display.ItemDisplay> banners = new ArrayList<>();
	private RemotePlayer lenin;
	private Vec3 car;
	private Vec3 carSide;
	private boolean down;

	@Override
	public int length() {
		return DEATH + 1;
	}

	/** The tick of the refrain at a number of sixteenths, two and a half ticks each. */
	private static int at(int sixteenths) {
		return Math.round(sixteenths * 2.5F);
	}

	private static float pitchOf(int midi, int base) {
		return (float) Math.pow(2.0, (midi - base) / 12.0);
	}

	/**
	 * A brass band plays the Internationale while the crowd gathers: the tune on trumpet and flute, the chords, bass and
	 * a marching drum. It breaks off when Lenin points at the player; when the crowd storms, only the drum rolls.
	 */
	private void band(ClientLevel level, int tick) {
		if (tick < POINT) {
			for (int i = 0; i < TUNE.length; i++) {
				if (NOTE_TICKS[i] == tick) {
					float pitch = pitchOf(TUNE[i][0], 66);
					sound(level, car, SoundEvents.NOTE_BLOCK_TRUMPET.value(), SoundSource.RECORDS, 1.4F, pitch);
					sound(level, car, SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.RECORDS, 0.6F, pitch);
				}
			}
			int sinceBar = tick - MUSIC - at(PICKUP);
			for (int beat = 0; beat < CHORDS.length * 4; beat++) {
				if (sinceBar != at(beat * 4)) {
					continue;
				}
				int bar = beat / 4;
				boolean strong = beat % 2 == 0;
				for (int midi : CHORDS[bar]) {
					sound(level, car, SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.RECORDS, strong ? 0.5F : 0.35F, pitchOf(midi, 66));
				}
				if (strong) {
					sound(level, car, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.RECORDS, 1.0F, pitchOf(BASS[bar] + (beat % 4 == 2 ? 7 : 0), 42));
					sound(level, car, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.RECORDS, 0.6F, 1.0F);
				} else {
					sound(level, car, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.RECORDS, 0.5F, 1.0F);
				}
			}
		}
		if (tick >= STORM && tick < HITS[0] && tick % 2 == 0) {
			// The drum roll while they come.
			sound(level, car, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.RECORDS, 0.4F + (tick - STORM) * 0.03F, 1.0F);
		}
	}

	@Override
	public int photoTick() {
		return CHEER + 10;
	}

	@Override
	public float lying() {
		return down ? 1.0F : 0.0F;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.revolution", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (lenin == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();
		band(level, tick);
		if (tick >= HOP_UP && tick <= HOP_UP + 8) {
			// Up onto the armoured car.
			float p = (tick - HOP_UP) / 8.0F;
			Vec3 at = carSide.lerp(car.add(0.0, 2.1, 0.0), p).add(0.0, Math.sin(p * Math.PI) * 0.8, 0.0);
			lenin.setPos(at.x, at.y, at.z);
		}
		face(lenin, tick < POINT ? lenin.getEyePosition().add(horizontalLook(player).scale(-4.0)).add(0.0, 0.5, 0.0) : eyes);

		if (tick == COMRADES) {
			speak(level, lenin, "fakedeath.revolution.comrades", RED, 35);
			pose(lenin, Skins.Pose.POINT);
		}
		if (tick == POWER) {
			speak(level, lenin, "fakedeath.revolution.power", RED, 40);
		}
		if (tick == CHEER) {
			for (RemotePlayer worker : crowd) {
				pose(worker, Skins.Pose.CHEER);
			}
		}
		if (tick >= CHEER && tick < POINT && tick % 6 == 0) {
			RemotePlayer worker = crowd.get(random.nextInt(crowd.size()));
			sound(level, worker.position(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.5F, 0.7F + random.nextFloat() * 0.3F);
		}
		if (tick == POINT) {
			for (RemotePlayer worker : crowd) {
				pose(worker, null);
			}
			speak(level, lenin, "fakedeath.revolution.bourgeois", RED, 50);
		}
		for (int i = 0; i < crowd.size(); i++) {
			RemotePlayer worker = crowd.get(i);
			if (tick < TURN) {
				face(worker, lenin.getEyePosition());
			} else if (tick < STORM) {
				face(worker, eyes);
			} else {
				// All of them at once, straight at the player.
				Vec3 target = player.position().add(horizontalLook(player).yRot((i - CROWD / 2) * 0.25F).scale(0.6));
				stepTowards(level, player, worker, target, 0.32);
				face(worker, eyes);
				if (tick % 5 == i % 5) {
					sound(level, worker.position(), SoundEvents.GRASS_STEP, SoundSource.NEUTRAL, 1.0F, 0.8F);
				}
			}
		}
		if (tick == STORM) {
			sound(level, eyes, SoundEvents.RAVAGER_ROAR, SoundSource.NEUTRAL, 0.8F, 1.4F);
			for (RemotePlayer worker : crowd) {
				pose(worker, Skins.Pose.RAISE);
			}
		}
		for (int i = 0; i < banners.size(); i++) {
			// The pole in the hand at the side, the flag high above the crowd, the printed side to the player.
			RemotePlayer bearer = crowd.get(i * 5);
			Vec3 facing = Vec3.directionFromRotation(0.0F, bearer.getYRot());
			Vec3 hand = bearer.position().add(new Vec3(-facing.z, 0.0, facing.x).scale(0.42)).add(facing.scale(0.15));
			Vec3 toPlayer = player.position().subtract(bearer.position());
			float yaw = (float) Math.atan2(toPlayer.x, toPlayer.z);
			Vec3 pole = new Vec3(Math.cos(yaw), 0.0, -Math.sin(yaw)).scale(0.5 * 1.3);
			place(banners.get(i), hand.subtract(pole).add(0.0, 2.7, 0.0), new Vector3f(1.3F), new Quaternionf().rotateY(yaw));
		}
		turnTowards(player, tick < TURN ? lenin.getEyePosition() : crowd.get(CROWD / 2).getEyePosition(), 0.1F);
		for (int i = 0; i < HITS.length; i++) {
			if (tick == HITS[i]) {
				particles(level, ParticleTypes.CRIT, eyes, 12, 0.4, 0.2);
				cameraShake(0.25F);
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.28F);
				down = i > 0;
			}
		}
		if (down) {
			moveCamera(new Vec3(0.0, -1.3, 0.0));
		}
		if (tick == DEATH) {
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		Vec3 look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		float yaw = (float) Math.atan2(side.x, side.z);
		car = onGround(level, player, player.position().add(look.scale(9.0)));
		// The armoured car: a grey hull across the view, a turret, black wheels.
		place(blockDisplay(level, Looks.GRAY_CONCRETE, car, 8.0F), car.add(0.0, 1.0, 0.0), new Vector3f(1.8F, 1.3F, 4.0F), new Quaternionf().rotateY(yaw));
		place(blockDisplay(level, Blocks.CONCRETE.green().defaultBlockState(), car, 8.0F), car.add(0.0, 1.85, 0.0), new Vector3f(1.2F, 0.5F, 1.4F), new Quaternionf().rotateY(yaw));
		for (int s : new int[] {-1, 1}) {
			for (int f : new int[] {-1, 1}) {
				Vec3 wheel = car.add(look.scale(s * 0.95)).add(side.scale(f * 1.4)).add(0.0, 0.4, 0.0);
				place(blockDisplay(level, Looks.BLACK_CONCRETE, wheel, 2.0F), wheel, new Vector3f(0.3F, 0.8F, 0.8F), new Quaternionf().rotateY(yaw + Mth.HALF_PI));
			}
		}
		// Lenin walks up to the car and climbs on, the crowd comes in from both sides.
		carSide = onGround(level, player, car.add(look.scale(-1.3)).add(side.scale(0.8)));
		lenin = actor(level, "fakedeath.revolution.lenin", "lenin", false, carSide);
		arrive(level, player, lenin, 0);
		for (int i = 0; i < CROWD; i++) {
			double angle = (i - (CROWD - 1) / 2.0) * 0.3;
			Vec3 at = car.add(look.yRot((float) angle).scale(-3.0 - (i % 2) * 0.8));
			RemotePlayer worker = extra(level, "worker", onGround(level, player, at));
			arrive(level, player, worker, 4 + i * 2);
			crowd.add(worker);
		}
		for (int i = 0; i < 2; i++) {
			banners.add(itemDisplay(level, Looks.prop(net.minecraft.world.item.Items.IRON_NUGGET, "flag_soviet"), crowd.get(i * 5).position()));
		}
	}
}
