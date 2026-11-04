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
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.Vec3;

/** Night without sleep: phantoms circle high above, screech, then dive at the player one after the other. */
public class PhantomScene extends Scene {
	private static final int COUNT = 3;
	private static final int FIRST_DIVE = 50;
	private static final int DIVE_EVERY = 30;
	private static final int DIVE_TICKS = 14;
	private static final double CIRCLE_HEIGHT = 14.0;
	private static final double CIRCLE_RADIUS = 7.0;

	private final List<Phantom> phantoms = new ArrayList<>();
	private Vec3 center;

	@Override
	public int length() {
		return FIRST_DIVE + (COUNT - 1) * DIVE_EVERY + DIVE_TICKS + 6;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.mob", player.getDisplayName(), EntityType.PHANTOM.getDescription());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			center = player.position().add(0, CIRCLE_HEIGHT, 0);

			for (int i = 0; i < COUNT; i++) {
				phantoms.add(spawn(level, EntityType.PHANTOM, circle(i, 0)));
			}

			sound(level, center, SoundEvents.PHANTOM_AMBIENT, SoundSource.HOSTILE, 3.0F, 0.8F);
		}

		if (phantoms.isEmpty()) {
			return;
		}

		Vec3 head = player.getEyePosition();

		for (int i = 0; i < phantoms.size(); i++) {
			Phantom phantom = phantoms.get(i);
			int diveStart = FIRST_DIVE + i * DIVE_EVERY;
			Vec3 pos;

			if (tick >= diveStart && tick < diveStart + DIVE_TICKS * 2) {
				// Down right in front of the player's face, so the bite is seen, then back up into the circle.
				float t = (tick - diveStart) / (float) DIVE_TICKS;
				Vec3 top = circle(i, tick);
				Vec3 face = head.add(horizontalLook(player).scale(0.9));
				pos = t <= 1.0F ? top.lerp(face, t * t) : face.lerp(top, t - 1.0F);

				if (tick == diveStart) {
					sound(level, top, SoundEvents.PHANTOM_SWOOP, SoundSource.HOSTILE, 3.0F, 1.0F);
				}

				if (tick == diveStart + DIVE_TICKS) {
					sound(level, head, SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 1.5F, 1.0F);
					particles(level, ParticleTypes.CRIT, head.add(horizontalLook(player).scale(0.5)), 14, 0.3, 0.3);
					sound(level, head, SoundEvents.PHANTOM_HURT, SoundSource.HOSTILE, 1.5F, 0.7F);
					hit(level, player, SoundEvents.PLAYER_HURT, 0.4F, i == COUNT - 1);
				}

				if (t <= 1.0F) {
					turnTowards(player, pos, 0.3F);
				}
			} else {
				pos = circle(i, tick);
			}

			Vec3 travel = pos.subtract(phantom.position());
			phantom.setPos(pos.x, pos.y, pos.z);

			if (travel.lengthSqr() > 1.0E-4) {
				face(phantom, pos.add(travel));
			}
		}

		if (tick < FIRST_DIVE) {
			turnTowards(player, center, 0.05F);

			if (tick % 25 == 10) {
				sound(level, center, SoundEvents.PHANTOM_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.9F + random.nextFloat() * 0.2F);
			}
		}
	}

	private Vec3 circle(int index, int tick) {
		float angle = index * Mth.TWO_PI / COUNT + tick * 0.06F;
		return center.add(Mth.cos(angle) * CIRCLE_RADIUS, Mth.sin(tick * 0.1F + index) * 1.5, Mth.sin(angle) * CIRCLE_RADIUS);
	}
}
