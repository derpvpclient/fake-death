package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.phys.Vec3;

/** An angry swarm pours out of the hive next to the player, circles them and stings until they drop. */
public class BeeScene extends Scene {
	private static final int SWARM = 7;
	private static final int ARRIVE = 40;
	private static final int FIRST_STING = 50;
	private static final int STING_EVERY = 14;
	private static final int DEATH = 130;

	private final BlockPos hive;
	private final List<Bee> bees = new ArrayList<>();
	private Vec3 home;
	private int stinging = -1;

	public BeeScene(BlockPos hive) {
		this.hive = hive;
	}

	@Override
	public int length() {
		return DEATH;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.sting", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			home = hive != null ? Vec3.atCenterOf(hive) : groundSpot(level, player, 6.0, 40.0).add(0, 1.5, 0);

			for (int i = 0; i < SWARM; i++) {
				Bee bee = spawn(level, EntityType.BEE, home);
				// Angry eyes and the red stinger look, as long as the scene runs.
				bee.setPersistentAngerEndTime(level.getGameTime() + 2000);
				bees.add(bee);
			}
		}

		if (bees.isEmpty()) {
			return;
		}

		if (tick % 20 == 0) {
			sound(level, player.position(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.HOSTILE, 1.2F, 1.0F);
		}

		Vec3 head = player.getEyePosition();

		for (int i = 0; i < bees.size(); i++) {
			Bee bee = bees.get(i);
			float phase = i * Mth.TWO_PI / bees.size() + tick * 0.18F;
			double radius = i == stinging ? 0.45 : 1.3 + 0.2 * Mth.sin(tick * 0.3F + i);
			Vec3 orbit = head.add(Mth.cos(phase) * radius, -0.4 + 0.4 * Mth.sin(tick * 0.25F + i * 1.7F), Mth.sin(phase) * radius);
			// Out of the hive first, then circling the player.
			Vec3 pos = tick < ARRIVE ? home.lerp(orbit, tick / (double) ARRIVE) : orbit;
			bee.setPos(pos.x, pos.y, pos.z);
			face(bee, head);
		}

		if (tick < ARRIVE) {
			turnTowards(player, home, 0.12F);
		}

		if (tick >= FIRST_STING && (tick - FIRST_STING) % STING_EVERY == 0) {
			stinging = random.nextInt(bees.size());
			sound(level, player.position(), SoundEvents.BEE_STING, SoundSource.HOSTILE, 1.0F, 1.0F);
			particles(level, ParticleTypes.CRIT, player.getEyePosition().add(0, -0.3, 0), 8, 0.3, 0.3);
			hit(level, player, SoundEvents.PLAYER_HURT, 0.18F, tick + STING_EVERY >= DEATH);

			if (tick == FIRST_STING) {
				// Green hearts from the poison.
				addEffect(player, MobEffects.POISON, 400);
			}
		} else if (tick >= FIRST_STING && (tick - FIRST_STING) % STING_EVERY == 4) {
			stinging = -1;
		}
	}
}
