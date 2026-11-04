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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.phys.Vec3;

/** The player smacks a chicken. A moment of silence. Then chickens come from everywhere, a hundred of them, and peck. */
public class ChickenScene extends Scene {
	private static final int SMACK = 25;
	private static final int CALL = 50;
	private static final int ARMY = 40;
	private static final int RUN = 70;
	private static final int PECK = CALL + RUN;
	private static final int DEATH = PECK + 60;
	private final List<Chicken> army = new ArrayList<>();
	private final List<Vec3> from = new ArrayList<>();
	private final List<Vec3> to = new ArrayList<>();
	private Chicken victim;

	@Override
	public int length() {
		return DEATH + 20;
	}

	@Override
	public int photoTick() {
		return PECK - 15;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.huehner", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			victim = spawn(level, EntityType.CHICKEN, groundSpot(level, player, 1.8, 0.0));
		}
		if (victim == null) {
			return;
		}
		if (tick < CALL) {
			face(victim, player.getEyePosition());
			turnTowards(player, victim.position(), 0.2F);
		}
		if (tick == SMACK) {
			Looks.swing(player);
			victim.animateHurt(0.0F);
			sound(level, victim.position(), Looks.CHICKEN_HURT, SoundSource.NEUTRAL, 1.2F, 1.0F);
			particles(level, ParticleTypes.CRIT, victim.position().add(0.0, 0.4, 0.0), 8, 0.3, 0.1);
		}
		if (tick == CALL) {
			// The whole flock answers.
			for (int i = 0; i < ARMY; i++) {
				double angle = i * Math.PI * 2 / ARMY + random.nextDouble() * 0.2;
				Vec3 start = player.position().add(Math.cos(angle) * (12.0 + random.nextDouble() * 6.0), 0.0, Math.sin(angle) * (12.0 + random.nextDouble() * 6.0));
				Vec3 end = player.position().add(Math.cos(angle) * (0.7 + (i % 3) * 0.55), 0.0, Math.sin(angle) * (0.7 + (i % 3) * 0.55));
				start = new Vec3(start.x, floor(level, start.x, start.z, player.getY()), start.z);
				end = new Vec3(end.x, floor(level, end.x, end.z, player.getY()), end.z);
				Chicken chicken = spawn(level, EntityType.CHICKEN, start);
				chicken.noPhysics = true;
				army.add(chicken);
				from.add(start);
				to.add(end);
			}
		}
		for (int i = 0; i < army.size(); i++) {
			Chicken chicken = army.get(i);
			float p = Math.min(1.0F, (tick - CALL) / (float) (RUN - i % 10));
			walk(chicken, from.get(i).lerp(to.get(i), p));
			face(chicken, player.getEyePosition());
		}
		if (tick > CALL) {
			turnTowards(player, player.getEyePosition().add(horizontalLook(player).yRot((tick - CALL) * 0.02F).scale(6.0)).add(0.0, -1.0, 0.0), 0.05F);
			if (tick % 4 == 0) {
				sound(level, player.position().add(horizontalLook(player).yRot(random.nextFloat() * 6.0F).scale(4.0)), Looks.CHICKEN, SoundSource.NEUTRAL, 1.0F, 0.8F + random.nextFloat() * 0.5F);
			}
		}
		if (tick >= PECK && tick < DEATH && (tick - PECK) % 12 == 0) {
			particles(level, ParticleTypes.CRIT, player.position().add(0.0, 0.5, 0.0), 10, 0.5, 0.1);
			hurt(level, player, Looks.CHICKEN_HURT, 0.15F);
		}
		if (tick == DEATH) {
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}
}
