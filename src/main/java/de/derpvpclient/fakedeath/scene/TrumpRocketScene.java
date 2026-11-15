package de.derpvpclient.fakedeath.scene;

import com.mojang.math.Transformation;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Donald Trump and his generals and advisors walk up to a long table and all look at the player. "This guy. Very bad
 * guy." A general begs him not to. He holds up his phone: his button is much bigger. One tap. Far behind them a missile
 * climbs into the sky on a column of fire, everybody at the table runs. It turns over high up and comes down, straight
 * at the player: a blinding white flash, and when the light fades the mushroom cloud towers right overhead.
 */
public class TrumpRocketScene extends CastScene {
	private static final int L1 = ARRIVE + 10;
	private static final int L2 = ARRIVE + 60;
	private static final int L3 = ARRIVE + 100;
	private static final int TAP = ARRIVE + 145;
	private static final int LAUNCH = TAP + 15;
	private static final int FLIGHT = 80;
	private static final int IMPACT = LAUNCH + FLIGHT;
	private static final int AFTER = 40;
	private static final int DEATH = IMPACT + AFTER;
	private static final ChatFormatting US = ChatFormatting.RED;
	private static final ChatFormatting STAFF = ChatFormatting.GRAY;

	private final List<RemotePlayer> staff = new ArrayList<>();
	private final List<Part> missile = new ArrayList<>();
	private RemotePlayer trump;
	private Vec3 pad;
	private Vec3 apex;
	private Vec3 rocketAt;
	private Vec3 hit;
	private float flash;

	@Override
	public int length() {
		return DEATH + 1;
	}

	@Override
	public int photoTick() {
		return LAUNCH + 20;
	}

	@Override
	public int screenTint() {
		return flash > 0.0F ? ((int) (Math.min(flash, 1.0F) * 255.0F) << 24) | 0xFFFFF4 : 0;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.trumprakete", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (trump == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();

		if (tick < LAUNCH) {
			face(trump, tick >= TAP - 20 ? trump.getEyePosition().add(horizontalLook(player).scale(-1.0)).add(0.0, -0.6, 0.0) : eyes);
			for (int i = 0; i < staff.size(); i++) {
				face(staff.get(i), tick > L2 && tick < L3 + 30 && i == 0 ? trump.getEyePosition() : eyes);
			}
			turnTowards(player, trump.getEyePosition(), 0.08F);
		}
		if (tick == L1) {
			speak(level, trump, "fakedeath.trumprakete.l1", US, 45);
		}
		if (tick == L2) {
			speak(level, staff.getFirst(), "fakedeath.trumprakete.l2", STAFF, 35);
		}
		if (tick == L3) {
			speak(level, trump, "fakedeath.trumprakete.l3", US, 40);
			Looks.swing(trump);
		}
		if (tick == TAP) {
			speak(level, trump, "fakedeath.trumprakete.l4", US, 40);
			Looks.swing(trump);
			sound(level, trump.position(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.NEUTRAL, 1.0F, 2.0F);
		}

		if (tick == LAUNCH) {
			buildMissile(level);
			sound(level, pad, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 4.0F, 0.4F);
			sound(level, pad, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 4.0F, 0.5F);
			sound(level, eyes, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.5F, 0.8F);
			speak(level, staff.get(1), "fakedeath.trumprakete.cover", STAFF, 40);
		}
		if (tick >= LAUNCH && tick < IMPACT) {
			flyMissile(level, player, tick);
			// Everybody at the table runs off, away from the player.
			Vec3 away = horizontalLook(player);
			for (RemotePlayer person : staff) {
				stepTowards(level, player, person, person.position().add(away.scale(3.0)), 0.18);
				face(person, person.position().add(away.scale(3.0)).add(0.0, 1.6, 0.0));
			}
			stepTowards(level, player, trump, trump.position().add(away.scale(3.0)), 0.14);
			face(trump, trump.position().add(away.scale(3.0)).add(0.0, 1.6, 0.0));
		}
		if (tick == IMPACT) {
			// Right on top of the player.
			hit = player.position();
			for (Part part : missile) {
				despawn(level, part.display());
			}
			missile.clear();
			flash = 1.0F;
			sound(level, eyes, Looks.EXPLODE, SoundSource.HOSTILE, 6.0F, 0.4F);
			sound(level, eyes, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0F, 0.5F);
			level.addParticle(ParticleTypes.EXPLOSION_EMITTER, hit.x, hit.y + 1.0, hit.z, 1.0, 0.0, 0.0);
			hurt(level, player, SoundEvents.PLAYER_HURT_ON_FIRE, 0.7F);
		}
		if (tick > IMPACT) {
			// The light fades slowly, the cloud has grown over the player by then.
			int age = tick - IMPACT;
			flash = Math.max(0.0F, 1.0F - age * 0.035F);
			mushroom(level, age);
			for (int i = 0; i < 24; i++) {
				// The shock wave runs away over the ground in a ring.
				double a = i * Math.PI * 2.0 / 24.0;
				double r = age * 0.9;
				level.addParticle(ParticleTypes.CLOUD, hit.x + Math.cos(a) * r, hit.y + 0.3, hit.z + Math.sin(a) * r, Math.cos(a) * 0.2, 0.02, Math.sin(a) * 0.2);
			}
			cameraShake(Math.max(0.05F, 0.4F - age * 0.008F));
			if (tick % 8 == 0) {
				sound(level, eyes, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0F, 0.3F);
			}
			// Looking up into the cloud.
			turnTowards(player, hit.add(horizontalLook(player).scale(4.0)).add(0.0, 18.0, 0.0), 0.06F);
		}
		if (tick == DEATH) {
			kill(level, player, SoundEvents.PLAYER_HURT_ON_FIRE);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		Vec3 look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		Vec3 center = onGround(level, player, player.position().add(look.scale(6.0)));
		float yaw = (float) Math.atan2(look.x, look.z);
		// The long table, its top across the view, Trump at the far side in the middle.
		BlockState top = Blocks.DARK_OAK_PLANKS.defaultBlockState();
		Display.BlockDisplay board = blockDisplay(level, top, center, 6.0F);
		place(board, center.add(0.0, 0.8, 0.0), new Vector3f(4.2F, 0.12F, 1.3F), new Quaternionf().rotateY(yaw));
		for (int sx : new int[] {-1, 1}) {
			for (int sz : new int[] {-1, 1}) {
				Vec3 leg = center.add(side.scale(sx * 1.9)).add(look.scale(sz * 0.5));
				place(blockDisplay(level, top, leg, 2.0F), leg.add(0.0, 0.37, 0.0), new Vector3f(0.12F, 0.74F, 0.12F), new Quaternionf().rotateY(yaw));
			}
		}
		// On the table: papers.
		Vec3 papers = center.add(side.scale(1.0)).add(0.0, 0.87, 0.0);
		place(itemDisplay(level, Looks.prop(Items.PAPER, "invoice"), papers), papers, new Vector3f(0.4F), new Quaternionf().rotateY(yaw).rotateX(Mth.HALF_PI));
		trump = actor(level, "fakedeath.trumprakete.trump", "trump", false, onGround(level, player, center.add(look.scale(0.9))));
		hold(trump, Looks.prop(Items.SPYGLASS, "phone"));
		arrive(level, player, trump, 6);
		String[][] seats = {{"general", "fakedeath.trumprakete.general"}, {"bodyguard", "fakedeath.trumprakete.advisor"}, {"general", "fakedeath.trumprakete.general"}, {"bodyguard", "fakedeath.trumprakete.advisor"}};
		double[] at = {-1.4, -0.7, 0.7, 1.4};
		for (int i = 0; i < seats.length; i++) {
			RemotePlayer person = actor(level, seats[i][1], seats[i][0], false, onGround(level, player, center.add(look.scale(0.9)).add(side.scale(at[i] * 1.2))));
			arrive(level, player, person, i < 2 ? i * 4 : (i - 2) * 4 + 2);
			staff.add(person);
		}
		// The flag behind them.
		standFlag(level, player, center.add(look.scale(2.2)).add(side.scale(2.6)), "flag_usa", 1.4F);
		// The missile starts far behind them, climbs high and comes down on the player.
		pad = onGround(level, player, center.add(look.scale(30.0)).add(side.scale(-6.0)));
		apex = pad.lerp(player.position(), 0.45).add(0.0, 48.0, 0.0);
	}

	private void buildMissile(ClientLevel level) {
		// A white body, a red nose, grey fins.
		missile.add(new Part(blockDisplay(level, Looks.WHITE_CONCRETE, pad, 8.0F), new Vector3f(-0.25F, -1.2F, -0.25F), new Vector3f(0.5F, 2.4F, 0.5F)));
		missile.add(new Part(blockDisplay(level, Blocks.RED_CONCRETE.defaultBlockState(), pad, 8.0F), new Vector3f(-0.18F, 1.2F, -0.18F), new Vector3f(0.36F, 0.5F, 0.36F)));
		missile.add(new Part(blockDisplay(level, Looks.GRAY_CONCRETE, pad, 8.0F), new Vector3f(-0.5F, -1.2F, -0.05F), new Vector3f(1.0F, 0.6F, 0.1F)));
		missile.add(new Part(blockDisplay(level, Looks.GRAY_CONCRETE, pad, 8.0F), new Vector3f(-0.05F, -1.2F, -0.5F), new Vector3f(0.1F, 0.6F, 1.0F)));
	}

	/** Up, over and down onto the player: a curve through a high point, the nose always along the way it flies. */
	private void flyMissile(ClientLevel level, LocalPlayer player, int tick) {
		float p = (tick - LAUNCH) / (float) FLIGHT;
		// Slow at the start, fast at the end.
		float t = p * p * (1.5F - 0.5F * p);
		Vec3 target = player.position().add(0.0, 1.0, 0.0);
		Vec3 a = pad.lerp(apex, t);
		Vec3 b = apex.lerp(target, t);
		Vec3 at = a.lerp(b, t);
		Vec3 dir = rocketAt == null ? new Vec3(0.0, 1.0, 0.0) : at.subtract(rocketAt);
		rocketAt = at;
		if (dir.lengthSqr() < 1.0E-6) {
			dir = new Vec3(0.0, 1.0, 0.0);
		}
		Quaternionf turn = new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), new Vector3f((float) dir.x, (float) dir.y, (float) dir.z).normalize());
		for (Part part : missile) {
			part.display().setPos(at.x, at.y, at.z);
			transform(part.display(), new Transformation(new Vector3f(part.offset()).rotate(turn), new Quaternionf(turn), part.size(), new Quaternionf()));
		}
		// Fire and a long trail of smoke behind it.
		Vec3 tail = at.subtract(dir.normalize().scale(1.4));
		for (int i = 0; i < 4; i++) {
			level.addParticle(ParticleTypes.FLAME, tail.x, tail.y, tail.z, (random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.1);
		}
		level.addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, tail.x, tail.y, tail.z, 0.0, 0.01, 0.0);
		if (tick % 10 == 0) {
			sound(level, at, SoundEvents.ELYTRA_FLYING, SoundSource.HOSTILE, 3.0F, 0.6F + p * 0.6F);
		}
		if (tick == IMPACT - 18) {
			// The scream of the falling missile, coming closer.
			sound(level, at, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR, SoundSource.HOSTILE, 4.0F, 0.5F);
			sound(level, player.getEyePosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.0F, 1.2F);
		}
		// The view follows it up into the sky and down onto the player.
		turnTowards(player, at, 0.18F);
	}

	/** The mushroom cloud right over the player: a column of fire and smoke, a broad cap rolling out at the top. */
	private void mushroom(ClientLevel level, int age) {
		double height = Math.min(22.0, age * 1.1);
		for (int i = 0; i < 10; i++) {
			double y = random.nextDouble() * height;
			level.addParticle(i % 3 == 0 ? ParticleTypes.FLAME : ParticleTypes.LARGE_SMOKE, hit.x + (random.nextDouble() - 0.5) * 2.5, hit.y + y,
				hit.z + (random.nextDouble() - 0.5) * 2.5, 0.0, 0.25, 0.0);
		}
		double cap = Math.min(11.0, age * 0.5);
		for (int i = 0; i < 26; i++) {
			double a = random.nextDouble() * Math.PI * 2.0;
			double r = random.nextDouble() * cap;
			level.addParticle(i % 4 == 0 ? ParticleTypes.EXPLOSION : ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, hit.x + Math.cos(a) * r, hit.y + height + (random.nextDouble() - 0.3) * 3.0,
				hit.z + Math.sin(a) * r, 0.0, 0.02, 0.0);
		}
	}

	private record Part(Display.BlockDisplay display, Vector3f offset, Vector3f size) {
	}
}
