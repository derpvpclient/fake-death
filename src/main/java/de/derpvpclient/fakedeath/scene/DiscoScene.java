package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Out of nowhere a party: music starts, a disco ball comes down on a chain, a glowing dance floor lights up tile by
 * tile, colored beams sweep over it, parrots dance and a creeper dances right in front of the player. When the beat
 * drops, the lights go out and so does the creeper. The lights are in the world, the screen itself never flashes.
 */
public class DiscoScene extends Scene {
	/** Colors of the light beams, one per beam. */
	private static final int[] BEAMS = {0xFF2060, 0x20E0FF, 0xFFE020, 0x40FF40};
	private static final int BEAT = 8;
	/** The floor colors move on every second beat, calm enough to never flicker. */
	private static final int FLOOR_STEP = BEAT * 2;
	private static final int FLOOR_RADIUS = 2;
	private static final int BALL_DOWN = 40;
	private static final double BALL_HEIGHT = 2.9;
	private static final int CREEPER = 20;
	private static final int PRIME = 150;
	private static final int EXPLODE = PRIME + 30;

	private final List<Parrot> parrots = new ArrayList<>();
	private final List<Display.BlockDisplay> tiles = new ArrayList<>();
	private final List<int[]> tileSpots = new ArrayList<>();
	private Creeper creeper;
	private Vec3 floor;
	private Display.BlockDisplay ball;
	private Display.BlockDisplay chain;

	@Override
	public int length() {
		return EXPLODE + 10;
	}

	@Override
	public int photoTick() {
		return PRIME - 20;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.explosion.player", player.getDisplayName(), EntityTypes.CREEPER.getDescription());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			sound(level, player.position(), SoundEvents.MUSIC_DISC_PIGSTEP.value(), SoundSource.RECORDS, 2.0F, 1.0F);
			floor = groundSpot(level, player, 3.0, 0.0);
		}

		danceFloor(level, tick);
		discoBall(level, tick);

		if (tick < PRIME) {
			if (tick >= BALL_DOWN && tick % 3 == 0) {
				beams(level, tick);
			}
			if (tick % 4 == 0) {
				Vec3 spot = floor.add((random.nextDouble() - 0.5) * 5.0, 0.5 + random.nextDouble() * 2.0, (random.nextDouble() - 0.5) * 5.0);
				level.addParticle(ParticleTypes.NOTE, spot.x, spot.y, spot.z, random.nextDouble(), 0.0, 0.0);
			}
		}

		if (tick == CREEPER) {
			creeper = spawn(level, EntityTypes.CREEPER, floor);
			creeper.noPhysics = true;

			Vec3 side = horizontalLook(player).yRot(Mth.HALF_PI);
			for (int s : new int[] {-1, 1}) {
				Parrot parrot = spawn(level, EntityTypes.PARROT, groundSpot(level, player, 3.0, 0.0).add(side.scale(s * 1.6)));
				parrot.noPhysics = true;
				parrots.add(parrot);
			}
		}

		for (Parrot parrot : parrots) {
			parrot.setRecordPlayingNearby(parrot.blockPosition(), tick < PRIME);
			face(parrot, player.getEyePosition());
		}

		if (creeper != null) {
			dance(level, player, tick);
		}

		if (tick == PRIME) {
			// The beat drops: the music stops, the lights go out.
			minecraft.getSoundManager().stop(SoundEvents.MUSIC_DISC_PIGSTEP.value().location(), SoundSource.RECORDS);
			sound(level, floor, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.0F, 0.6F);
		}

		if (creeper != null && tick == EXPLODE) {
			Vec3 blast = creeper.position().add(0.0, 0.8, 0.0);
			despawn(level, creeper);
			creeper = null;
			level.addParticle(ParticleTypes.EXPLOSION_EMITTER, blast.x, blast.y, blast.z, 1.0, 0.0, 0.0);
			particles(level, ParticleTypes.EXPLOSION, blast, 8, 1.5, 0.0);
			sound(level, blast, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F, 0.75F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	/** Lights up ring by ring around the creeper, then the colors wander across it. Dark once the beat drops. */
	private void danceFloor(ClientLevel level, int tick) {
		for (int ring = 0; ring <= FLOOR_RADIUS; ring++) {
			if (tick != ring * BEAT) {
				continue;
			}
			for (int dx = -ring; dx <= ring; dx++) {
				for (int dz = -ring; dz <= ring; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
						continue;
					}
					double x = Math.floor(floor.x) + dx + 0.5;
					double z = Math.floor(floor.z) + dz + 0.5;
					Vec3 pos = new Vec3(x, floor(level, x, z, floor.y) + 0.02, z);
					Display.BlockDisplay tile = blockDisplay(level, Looks.DISCO_FLOOR[0], pos, 2.0F);
					Looks.glow(tile);
					place(tile, pos, new Vector3f(0.96F, 0.04F, 0.96F), new Quaternionf());
					tiles.add(tile);
					tileSpots.add(new int[] {dx, dz});
					color(tile, dx, dz, tick);
				}
			}
			sound(level, floor, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.RECORDS, 0.8F, 0.8F + ring * 0.25F);
		}

		if (tick % FLOOR_STEP == 0 || tick == PRIME) {
			for (int i = 0; i < tiles.size(); i++) {
				color(tiles.get(i), tileSpots.get(i)[0], tileSpots.get(i)[1], tick);
			}
		}
	}

	private static void color(Display.BlockDisplay tile, int dx, int dz, int tick) {
		((de.derpvpclient.fakedeath.mixin.BlockDisplayAccessor) tile).fakedeath$setBlockState(tick >= PRIME
			? Looks.BLACK_CONCRETE
			: Looks.DISCO_FLOOR[Math.floorMod(dx + dz + tick / FLOOR_STEP, Looks.DISCO_FLOOR.length)]);
	}

	/** A glowing ball comes down on its chain, spins and throws little sparks of light. It stops once the music does. */
	private void discoBall(ClientLevel level, int tick) {
		float down = Math.min(1.0F, tick / (float) BALL_DOWN);
		Vec3 at = floor.add(0.0, BALL_HEIGHT + (1.0 - down * (2.0 - down)) * 3.0, 0.0);
		if (ball == null) {
			ball = blockDisplay(level, Blocks.SEA_LANTERN.defaultBlockState(), at, 3.0F);
			Looks.glow(ball);
			chain = blockDisplay(level, Looks.CHAIN, at, 8.0F);
		}
		float spin = Math.min(tick, PRIME) * 0.12F;
		place(ball, at, new Vector3f(0.55F), new Quaternionf().rotateY(spin).rotateX(0.35F).rotateZ(0.35F));
		place(chain, at.add(0.0, 2.3, 0.0), new Vector3f(1.0F, 4.0F, 1.0F), new Quaternionf());

		if (tick >= BALL_DOWN && tick < PRIME && tick % 3 == 0) {
			particles(level, ParticleTypes.END_ROD, at, 2, 0.3, 0.25);
		}
	}

	/** Four colored spotlights from the ball, sweeping in circles over the dance floor. */
	private void beams(ClientLevel level, int tick) {
		Vec3 from = floor.add(0.0, BALL_HEIGHT, 0.0);
		for (int i = 0; i < BEAMS.length; i++) {
			double angle = tick * 0.07 + i * Mth.HALF_PI;
			Vec3 to = orbit(floor, 2.2, angle, 0.1);
			// Thin and sparse, so the beams sweep around the creeper instead of hiding it.
			line(level, new DustParticleOptions(BEAMS[i], 0.7F), from, to, 0.55);
		}
	}

	/** Hops on every beat and turns around, then stops, looks at the player and hisses. */
	private void dance(ClientLevel level, LocalPlayer player, int tick) {
		Vec3 eyes = player.getEyePosition();

		if (tick < PRIME) {
			double hop = Math.abs(Mth.sin(Mth.PI * (tick % BEAT) / BEAT)) * 0.4;
			creeper.setPos(floor.x, floor.y + hop, floor.z);
			float spin = tick * 18.0F;
			creeper.setYRot(spin);
			creeper.setYBodyRot(spin);
			creeper.setYHeadRot(spin);
			turnTowards(player, creeper.getEyePosition(), 0.15F);
		} else {
			// The music stops for a moment, and the creeper looks the player in the eyes.
			creeper.setPos(floor.x, floor.y, floor.z);
			face(creeper, eyes);
			turnTowards(player, creeper.getEyePosition(), 0.3F);
		}

		if (tick == PRIME) {
			creeper.setSwellDir(1);
			sound(level, creeper.position(), SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1.5F, 0.5F);
		}
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		Minecraft.getInstance().getSoundManager().stop(SoundEvents.MUSIC_DISC_PIGSTEP.value().location(), SoundSource.RECORDS);
		for (Parrot parrot : parrots) {
			parrot.setRecordPlayingNearby(parrot.blockPosition(), false);
		}
		super.end(level, player);
	}
}
