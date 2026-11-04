package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.phys.Vec3;

/**
 * Growling in the dark forest. A pack stands at the edge of the trees in front of the player, then they run at
 * them, circle them close and leap at them one after the other.
 */
public class WolfScene extends Scene {
	// Adult wolf sounds depend on the wolf's sound variant in 26.3, these are the angry ones by id.
	private static final SoundEvent GROWL = SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace("entity.wolf_angry.growl"));
	private static final SoundEvent SNARL = SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace("entity.wolf_angry.ambient"));
	private static final int PACK = 5;
	private static final double FAR = 14.0;
	private static final double CLOSE = 3.0;
	private static final int RUN = 35;
	private static final int RUN_TICKS = 28;
	private static final int[] BITES = {95, 111, 126, 140};
	private static final int LUNGE = 8;

	private final List<Wolf> wolves = new ArrayList<>();
	private final List<Float> angles = new ArrayList<>();
	private final List<Vec3> starts = new ArrayList<>();

	@Override
	public int length() {
		return BITES[BITES.length - 1] + 10;
	}

	@Override
	public int photoTick() {
		return RUN + RUN_TICKS / 2;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.mob", player.getDisplayName(), EntityType.WOLF.getDescription());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			// In an arc in front of the player, so they see the pack coming.
			Vec3 look = horizontalLook(player);
			float facing = (float) Mth.atan2(look.z, look.x);
			for (int i = 0; i < PACK; i++) {
				float angle = facing + (i - (PACK - 1) / 2.0F) * 0.45F + (random.nextFloat() - 0.5F) * 0.15F;
				angles.add(angle);
				Vec3 start = ring(level, player, angle, FAR + random.nextDouble() * 2.0);
				starts.add(start);
				Wolf wolf = spawn(level, EntityType.WOLF, start);
				// Red eyes and bared teeth.
				wolf.setRemainingPersistentAngerTime(2000);
				wolf.noPhysics = true;
				wolves.add(wolf);
			}
		}

		if (wolves.isEmpty()) {
			return;
		}

		if (tick % 14 == 0) {
			Vec3 from = wolves.get(random.nextInt(wolves.size())).position();
			sound(level, from, random.nextBoolean() ? GROWL : SNARL, SoundSource.HOSTILE, 1.5F, 0.8F + random.nextFloat() * 0.2F);
		}

		for (int i = 0; i < wolves.size(); i++) {
			Wolf wolf = wolves.get(i);
			Vec3 pos;

			if (tick < RUN) {
				// Waiting at the edge of the trees.
				pos = starts.get(i);
			} else if (tick < RUN + RUN_TICKS) {
				// Running straight at the player, each a little offset in time.
				float t = Mth.clamp((tick - RUN - i * 2) / (float) (RUN_TICKS - PACK * 2), 0.0F, 1.0F);
				pos = starts.get(i).lerp(ring(level, player, angles.get(i), CLOSE), t);
				pos = new Vec3(pos.x, floor(level, pos.x, pos.z, player.getY()), pos.z);
				if (tick % 4 == i % 4 && t > 0.0F && t < 1.0F) {
					sound(level, pos, SoundEvents.WOLF_STEP, SoundSource.HOSTILE, 1.0F, 1.2F);
				}
			} else {
				// Circling close, then the leaps.
				float angle = angles.get(i) + (tick - RUN - RUN_TICKS) * 0.03F;
				pos = ring(level, player, angle, CLOSE);
				int lunge = lungeTick(i, tick);

				if (lunge >= 0) {
					double t = Math.sin(Math.PI * lunge / LUNGE);
					Vec3 face = player.position().add(horizontalLook(player).scale(0.7));
					pos = pos.lerp(face, t * 0.85).add(0.0, t * 0.9, 0.0);
				}
			}

			walk(wolf, pos);
			face(wolf, player.getEyePosition());
		}

		if (tick < RUN + RUN_TICKS) {
			turnTowards(player, wolves.get(PACK / 2).getEyePosition(), 0.1F);
		}

		for (int i = 0; i < BITES.length; i++) {
			int bite = BITES[i];
			if (tick == bite - 2) {
				turnTowards(player, wolves.get(i % PACK).getEyePosition(), 0.6F);
				sound(level, wolves.get(i % PACK).position(), SNARL, SoundSource.HOSTILE, 1.5F, 1.0F);
			}
			if (tick == bite + LUNGE / 2) {
				sound(level, player.position(), GROWL, SoundSource.HOSTILE, 1.5F, 1.1F);
				particles(level, ParticleTypes.CRIT, player.getEyePosition().add(0, -0.4, 0), 12, 0.3, 0.3);
				hit(level, player, SoundEvents.PLAYER_HURT, 0.28F, bite == BITES[BITES.length - 1]);
			}
		}
	}

	/** Which ticks of its leap the given wolf is in, or -1 when it is not leaping. */
	private static int lungeTick(int wolf, int tick) {
		for (int i = 0; i < BITES.length; i++) {
			if (i % PACK == wolf && tick >= BITES[i] && tick < BITES[i] + LUNGE) {
				return tick - BITES[i];
			}
		}

		return -1;
	}

	private static Vec3 ring(ClientLevel level, LocalPlayer player, float angle, double radius) {
		Vec3 spot = player.position().add(Mth.cos(angle) * radius, 0.0, Mth.sin(angle) * radius);
		return new Vec3(spot.x, floor(level, spot.x, spot.z, player.getY()), spot.z);
	}
}
