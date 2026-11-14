package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.phys.Vec3;

/**
 * One kitten. It trots up, meows, rubs against the legs and purrs. Then ten more come running from everywhere. Then
 * the sky opens and it rains kittens, fifty of them, tumbling down, piling up around the player, higher and higher,
 * over the chest, over the head. Death by cuteness.
 */
public class KittenScene extends Scene {
	private static final int FIRST = 10;
	private static final int TEN = 75;
	private static final int RAIN = 140;
	private static final int RAIN_COUNT = 50;
	private static final int RAIN_EVERY = 2;
	private static final int FALL = 14;
	private static final int DEATH = RAIN + RAIN_COUNT * RAIN_EVERY + FALL + 15;

	private final List<Kitten> kittens = new ArrayList<>();
	private float buried;

	@Override
	public int length() {
		return DEATH + 1;
	}

	@Override
	public int photoTick() {
		return RAIN + 50;
	}

	@Override
	public int screenTint() {
		// Under the pile it gets dark and warm.
		return buried > 0.0F && !thirdPerson() ? ((int) (buried * 200.0F) << 24) | 0x2A1A0E : 0;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.kaetzchen", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		Vec3 feet = player.position();
		if (tick == FIRST) {
			Vec3 from = groundSpot(level, player, 6.0, 10.0);
			kittens.add(new Kitten(baby(level, from), from, feet.add(horizontalLook(player).scale(0.7)), tick, 50, false));
		}
		if (tick == TEN) {
			for (int i = 0; i < 10; i++) {
				float angle = i * Mth.TWO_PI / 10.0F + random.nextFloat() * 0.3F;
				Vec3 from = new Vec3(feet.x + Mth.cos(angle) * 9.0, feet.y, feet.z + Mth.sin(angle) * 9.0);
				from = new Vec3(from.x, floor(level, from.x, from.z, feet.y), from.z);
				Vec3 to = feet.add(Mth.cos(angle) * (0.7 + random.nextFloat() * 0.5), 0.0, Mth.sin(angle) * (0.7 + random.nextFloat() * 0.5));
				kittens.add(new Kitten(baby(level, from), from, to, tick, 30 + random.nextInt(15), false));
			}
		}
		int raining = tick - RAIN;
		if (raining >= 0 && raining % RAIN_EVERY == 0 && raining / RAIN_EVERY < RAIN_COUNT) {
			int index = raining / RAIN_EVERY;
			// They land on the pile: in a ring around the player, each a little higher, the last ones on the head.
			float angle = index * 2.4F;
			double r = Math.max(0.15, 0.9 - index * 0.012);
			double height = index * 0.042;
			Vec3 to = feet.add(Mth.cos(angle) * r, height, Mth.sin(angle) * r);
			Vec3 from = to.add((random.nextDouble() - 0.5) * 3.0, 10.0 + random.nextDouble() * 4.0, (random.nextDouble() - 0.5) * 3.0);
			kittens.add(new Kitten(baby(level, from), from, to, tick, FALL, true));
		}

		for (Kitten kitten : kittens) {
			kitten.move(this, level, player, tick);
		}

		if (tick == FIRST + 50) {
			sound(level, feet, Looks.own("kitten.purr"), SoundSource.NEUTRAL, 1.0F, 1.0F);
		}
		if (tick > FIRST + 50 && tick % 20 == 0) {
			sound(level, feet, Looks.own("kitten.purr"), SoundSource.NEUTRAL, 1.2F, 1.0F);
		}
		if (tick >= TEN && tick % 6 == 0) {
			Kitten someone = kittens.get(random.nextInt(kittens.size()));
			sound(level, someone.cat.position(), Looks.own("kitten.meow"), SoundSource.NEUTRAL, 1.2F, 0.9F + random.nextFloat() * 0.3F);
		}
		if (tick == RAIN - 10) {
			// Something above. A lot of something.
			sound(level, feet.add(0.0, 10.0, 0.0), Looks.own("kitten.meow"), SoundSource.NEUTRAL, 3.0F, 0.8F);
		}
		if (raining >= 0) {
			buried = Math.min(1.0F, raining / (float) (RAIN_COUNT * RAIN_EVERY) * 1.1F);
		}
		if (raining == 30 || raining == 70) {
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.3F);
		}

		if (tick < TEN) {
			turnTowards(player, kittens.isEmpty() ? feet.add(horizontalLook(player).scale(4.0)) : kittens.getFirst().cat.position(), 0.1F);
		} else if (tick < RAIN) {
			turnTowards(player, feet.add(horizontalLook(player).yRot((tick - TEN) * 0.05F).scale(3.0)), 0.06F);
		} else {
			// Looking up at what is coming down.
			turnTowards(player, player.getEyePosition().add(horizontalLook(player).scale(2.0)).add(0.0, 6.0, 0.0), 0.1F);
		}
		if (tick == DEATH) {
			sound(level, feet, Looks.own("kitten.purr"), SoundSource.NEUTRAL, 2.0F, 0.9F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private Cat baby(ClientLevel level, Vec3 at) {
		Cat cat = spawn(level, EntityType.CAT, at);
		cat.setBaby(true);
		cat.noPhysics = true;
		return cat;
	}

	private static final class Kitten {
		final Cat cat;
		final Vec3 from;
		final Vec3 to;
		final int start;
		final int ticks;
		final boolean falling;

		Kitten(Cat cat, Vec3 from, Vec3 to, int start, int ticks, boolean falling) {
			this.cat = cat;
			this.from = from;
			this.to = to;
			this.start = start;
			this.ticks = ticks;
			this.falling = falling;
		}

		void move(KittenScene scene, ClientLevel level, LocalPlayer player, int tick) {
			float p = Math.min(1.0F, (tick - start) / (float) ticks);
			if (falling) {
				// Tumbling down out of the sky, faster and faster.
				Vec3 at = from.lerp(to, p * p);
				cat.setPos(at.x, at.y, at.z);
				cat.setYRot(cat.getYRot() + (p < 1.0F ? 25.0F : 0.0F));
				if (p >= 1.0F && tick == start + ticks) {
					level.addParticle(ParticleTypes.HEART, at.x, at.y + 0.4, at.z, 0.0, 0.1, 0.0);
				}
			} else {
				Vec3 at = from.lerp(to, p);
				walk(cat, new Vec3(at.x, floor(level, at.x, at.z, player.getY()), at.z));
				face(cat, p < 1.0F ? to.add(0.0, 0.3, 0.0) : player.getEyePosition());
			}
		}
	}
}
