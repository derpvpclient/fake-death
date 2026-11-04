package de.derpvpclient.fakedeath.scene;

import de.derpvpclient.fakedeath.FakeDeath;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.phys.Vec3;

/** A fitness trainer: "Ten more push-ups!" The camera goes down and up, the hunger bar runs empty, and the player gives out. */
public class TrainerScene extends NpcScene {
	private static final int REP = 14;
	private static final int REPS = 8;
	private static final int DONE = REP * REPS;

	public TrainerScene() {
		super("trainer", ChatFormatting.RED);
	}

	@Override
	protected int lineCount() {
		return 2;
	}

	@Override
	protected int actLength() {
		return DONE + 50;
	}

	@Override
	protected ResourceKey<VillagerProfession> profession() {
		return VillagerProfession.LEATHERWORKER;
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		if (t < DONE) {
			// Down to the floor and up again, slower with every one.
			int rep = t / REP;
			float phase = (t % REP) / (float) REP;
			moveCamera(new Vec3(0.0, -Mth.sin(phase * Mth.PI) * 1.1, 0.0));
			if (t % REP == 0) {
				FakeDeath.setFakeFood(20 - rep * 3);
				sound(level, npc.position(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 0.8F, 1.3F);
			}
			if (rep >= 5 && t % REP == REP / 2) {
				particles(level, ParticleTypes.SPLASH, player.getEyePosition(), 6, 0.3, 0.1);
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.25F);
			}
		}
		if (t == DONE - 10) {
			speak(minecraft, level, "fakedeath.trainer.last");
		}
		if (t == DONE) {
			FakeDeath.setFakeFood(0);
			moveCamera(new Vec3(0.0, -1.2, 0.0));
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
		if (t == DONE + 30) {
			speak(minecraft, level, "fakedeath.trainer.weak");
		}
	}
}
