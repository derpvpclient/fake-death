package de.derpvpclient.fakedeath.scene;

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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.phys.Vec3;

/**
 * Vladimir Putin rides in from the side, shirtless on a bear, waving like at a parade. "Privet!" He jumps off, bows
 * like before a judo fight: "You want to fight? I have a black belt." He grabs the player, throws them over his
 * shoulder, and they land flat on their back. "So. Who is lying on the mat now?" Then he whistles: "Misha, sit!" The
 * bear rears up, lumbers over and sits down. Right on the player.
 */
public class PutinScene extends CastScene {
	private static final int RIDE = 60;
	private static final int PRIVET = RIDE + 4;
	private static final int OFF = PRIVET + 30;
	private static final int HOP = 8;
	private static final int BOW = OFF + HOP + 4;
	private static final int JUDO = BOW + 18;
	private static final int GRAB = JUDO + 45;
	private static final int THROW = 18;
	private static final int LAND = GRAB + THROW;
	private static final int MAT = LAND + 12;
	private static final int SIT = MAT + 40;
	private static final int BEAR_GO = SIT + 16;
	private static final int BEAR_WALK = 22;
	private static final int SIT_DOWN = BEAR_GO + BEAR_WALK;
	private static final int DEATH = SIT_DOWN + 14;
	private static final ChatFormatting RU = ChatFormatting.AQUA;

	private RemotePlayer putin;
	private PolarBear bear;
	private Vec3 start;
	private Vec3 stop;
	private Vec3 jumpTo;
	private Vec3 landed;
	private Vec3 bearFrom;
	private boolean down;
	private float dark;
	private int now;

	@Override
	public int length() {
		return DEATH + 1;
	}

	@Override
	public int photoTick() {
		return RIDE - 12;
	}

	@Override
	public boolean firstPersonOnly() {
		// The throw and the bear sitting down are filmed from the eyes.
		return now >= GRAB;
	}

	@Override
	public float lying() {
		// After the throw the body lies flat on the ground in F5.
		return down ? 1.0F : 0.0F;
	}

	@Override
	public int screenTint() {
		// Under the bear: white fur, then dark.
		return dark > 0.0F ? ((int) (Math.min(dark, 1.0F) * 245.0F) << 24) | 0x2A2622 : 0;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.putin", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		now = tick;
		Vec3 look = horizontalLook(player);
		if (tick == 0) {
			Vec3 side = look.yRot(Mth.HALF_PI);
			start = onGround(level, player, player.position().add(look.scale(4.5)).add(side.scale(14.0)));
			stop = onGround(level, player, player.position().add(look.scale(3.8)));
			jumpTo = onGround(level, player, player.position().add(look.scale(1.6)));
			bear = spawn(level, EntityType.POLAR_BEAR, start);
			bear.noPhysics = true;
			putin = actor(level, "fakedeath.putin.name", "putin_bare", false, start.add(0.0, 1.2, 0.0));
			putin.startRiding(bear);
			sound(level, start, SoundEvents.POLAR_BEAR_AMBIENT, SoundSource.NEUTRAL, 2.0F, 0.8F);
		}
		if (putin == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();

		if (tick <= RIDE) {
			// Riding in at a stately pace, waving, slowing down at the end.
			float p = tick / (float) RIDE;
			float eased = 1.0F - (1.0F - p) * (1.0F - p);
			Vec3 at = onGround(level, player, start.lerp(stop, eased));
			Vec3 way = stop.subtract(start);
			walk(bear, at);
			face(bear, tick < RIDE - 10 ? at.add(way.normalize().scale(3.0)).add(0.0, 1.0, 0.0) : eyes);
			bear.positionRider(putin);
			face(putin, eyes);
			if (tick % 10 == 0) {
				sound(level, at, SoundEvents.POLAR_BEAR_STEP, SoundSource.NEUTRAL, 1.2F, 0.8F);
			}
			if (tick % 16 == 0) {
				pose(putin, tick % 32 == 0 ? Skins.Pose.RAISE : null);
			}
		}
		if (tick > RIDE && tick < OFF) {
			bear.positionRider(putin);
			face(putin, eyes);
			face(bear, eyes);
		}
		if (tick == PRIVET) {
			pose(putin, Skins.Pose.RAISE);
			speak(level, putin, "fakedeath.putin.privet", RU, 30);
			sound(level, bear.position(), SoundEvents.POLAR_BEAR_WARNING, SoundSource.NEUTRAL, 1.5F, 1.0F);
		}
		if (tick == OFF) {
			// A hop down off the bear, right in front of the player.
			pose(putin, null);
			putin.stopRiding();
		}
		if (tick >= OFF && tick <= OFF + HOP) {
			float p = (tick - OFF) / (float) HOP;
			Vec3 from = stop.add(0.0, 1.15, 0.0);
			Vec3 at = from.lerp(jumpTo, p).add(0.0, Math.sin(p * Math.PI) * 0.6, 0.0);
			putin.setPos(at.x, at.y, at.z);
			if (tick == OFF + HOP) {
				sound(level, jumpTo, SoundEvents.PLAYER_SMALL_FALL, SoundSource.NEUTRAL, 1.0F, 1.0F);
			}
		}
		if (tick > OFF && tick < SIT) {
			face(bear, putin.getEyePosition());
		}
		if (tick >= BOW && tick < BOW + 14) {
			// The bow before the fight: head down, then up again.
			face(putin, jumpTo.add(look.scale(-0.4)));
			if (tick == BOW) {
				sound(level, putin.position(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.NEUTRAL, 1.0F, 1.2F);
			}
		} else if (tick > OFF + HOP && tick < LAND) {
			face(putin, eyes);
		}
		if (tick == JUDO) {
			speak(level, putin, "fakedeath.putin.judo", RU, 45);
		}
		if (tick > JUDO && tick < GRAB && (tick - JUDO) % 12 == 0) {
			// Cracking the knuckles, warming up.
			Looks.swing(putin);
			sound(level, putin.position(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.NEUTRAL, 1.0F, 1.6F);
		}
		if (tick < GRAB) {
			turnTowards(player, tick <= RIDE ? putin.getEyePosition().add(0.0, -0.4, 0.0) : putin.getEyePosition(), 0.12F);
		}
		if (tick == GRAB) {
			pose(putin, Skins.Pose.CHEER);
			sound(level, eyes, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.5F, 0.8F);
		}
		Vec3 thrown = look.scale(2.4).add(0.0, -1.35, 0.0);
		if (tick >= GRAB && tick <= LAND) {
			// Up over his shoulder in an arc and down flat on the back, the view turning over with it.
			float p = (tick - GRAB) / (float) THROW;
			double over = Math.sin(p * Math.PI) * 1.6;
			Vec3 offset = look.scale(2.4 * p).add(0.0, over - 1.35 * p * p, 0.0);
			moveCamera(offset);
			turnTowards(player, eyes.add(offset).add(look.scale(p < 0.5 ? 1.0 : -1.0)).add(0.0, p < 0.5 ? 2.0 : 4.0, 0.0), 0.5F);
		}
		if (tick == LAND) {
			down = true;
			landed = player.position().add(look.scale(2.4));
			pose(putin, null);
			sound(level, player.position(), SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 2.0F, 0.8F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(player.blockPosition().below())), landed, 30, 0.6, 0.2);
			cameraShake(0.3F);
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.5F);
		}
		if (tick > LAND) {
			moveCamera(thrown);
		}
		if (tick > LAND && tick < BEAR_GO) {
			// Lying there, looking up at him standing over the player.
			turnTowards(player, putin.getEyePosition(), 0.15F);
			face(putin, landed);
		}
		if (tick == MAT) {
			speak(level, putin, "fakedeath.putin.mat", RU, 35);
		}
		if (tick == SIT) {
			// A whistle for the bear.
			speak(level, putin, "fakedeath.putin.sit", RU, 30);
			pose(putin, Skins.Pose.POINT);
			sound(level, putin.position(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.NEUTRAL, 1.2F, 1.8F);
			bear.setStanding(true);
			sound(level, bear.position(), SoundEvents.POLAR_BEAR_WARNING, SoundSource.NEUTRAL, 2.0F, 0.7F);
		}
		if (tick == BEAR_GO) {
			bear.setStanding(false);
			pose(putin, null);
			bearFrom = bear.position();
		}
		if (tick >= BEAR_GO && tick < SIT_DOWN) {
			// The bear lumbers over to the player lying on the ground.
			float p = (tick - BEAR_GO) / (float) BEAR_WALK;
			Vec3 at = onGround(level, player, bearFrom.lerp(landed, p));
			walk(bear, at);
			face(bear, landed.add(0.0, 0.3, 0.0));
			if (tick % 6 == 0) {
				sound(level, at, SoundEvents.POLAR_BEAR_STEP, SoundSource.NEUTRAL, 1.5F, 0.7F);
			}
			turnTowards(player, bear.getEyePosition(), 0.2F);
			face(putin, bear.getEyePosition());
		}
		if (tick >= SIT_DOWN) {
			// And sits down. Right on the player.
			float p = Math.min(1.0F, (tick - SIT_DOWN) / 8.0F);
			Vec3 seat = landed.add(look.scale(-0.3)).add(0.0, 0.6 * (1.0F - p), 0.0);
			bear.setPos(seat.x, seat.y, seat.z);
			dark = p;
			if (tick == SIT_DOWN + 6) {
				sound(level, eyes, SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 2.0F, 0.5F);
				sound(level, eyes, SoundEvents.POLAR_BEAR_AMBIENT, SoundSource.NEUTRAL, 1.5F, 0.7F);
				cameraShake(0.5F);
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.5F);
			}
		}
		if (tick == DEATH) {
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		if (putin != null) {
			putin.stopRiding();
		}
		putin = null;
		bear = null;
		super.end(level, player);
	}
}
