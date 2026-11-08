package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.phys.Vec3;

/**
 * Five screaming goats line up in front of the player like a choir and scream in canon, each one with its own scream
 * and its own pitch. A second round, faster. Then all together, twice, and that is too much.
 */
public class GoatChoirScene extends Scene {
	private static final int SINGERS = 5;
	private static final int ARRIVE = 40;
	private static final int ROUND_1 = 60;
	private static final int ROUND_2 = 130;
	private static final int[] TUTTI = {180, 200};
	private static final SoundEvent[] SCREAMS = {
		SoundEvents.GOAT_SCREAMING_AMBIENT, SoundEvents.GOAT_SCREAMING_PREPARE_RAM, SoundEvents.GOAT_SCREAMING_HURT,
		SoundEvents.GOAT_SCREAMING_LONG_JUMP, SoundEvents.GOAT_SCREAMING_MILK};
	/** Low to high, from the left to the right, like a real choir. */
	private static final float[] PITCH = {0.6F, 0.8F, 1.0F, 1.25F, 1.55F};

	private final List<Goat> goats = new ArrayList<>();
	private final List<Vec3> starts = new ArrayList<>();
	private final List<Vec3> places = new ArrayList<>();
	private final int[] screamed = new int[SINGERS];

	@Override
	public int length() {
		return TUTTI[TUTTI.length - 1] + 30;
	}

	@Override
	public int photoTick() {
		return ROUND_2 + 10;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.ziegenchor", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			for (int i = 0; i < SINGERS; i++) {
				double angle = -44.0 + i * 22.0;
				Vec3 spot = groundSpot(level, player, 4.2, angle);
				Vec3 start = groundSpot(level, player, 13.0, angle * 1.5);
				Goat goat = spawn(level, EntityTypes.GOAT, start);
				goat.noPhysics = true;
				goat.setScreamingGoat(true);
				goats.add(goat);
				starts.add(start);
				places.add(spot);
				screamed[i] = -100;
			}
		}
		if (goats.isEmpty()) {
			return;
		}

		// Round one in canon, round two twice as fast, then all of them at once.
		for (int i = 0; i < SINGERS; i++) {
			if (tick == ROUND_1 + i * 12 || tick == ROUND_2 + i * 6) {
				scream(level, i, 1.2F, tick);
			}
			for (int tutti : TUTTI) {
				if (tick == tutti) {
					scream(level, i, 2.0F, tick);
				}
			}
		}
		if (tick == ROUND_1 + SINGERS * 12 + 4) {
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.25F);
		}
		if (tick == ROUND_2 + SINGERS * 6 + 4) {
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.3F);
		}
		if (tick == TUTTI[0] + 2) {
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.25F);
		}
		if (tick >= TUTTI[0] && tick < TUTTI[TUTTI.length - 1] + 6) {
			cameraShake(0.5F);
		}
		if (tick == TUTTI[TUTTI.length - 1] + 2) {
			kill(level, player, SoundEvents.PLAYER_HURT);
		}

		int loudest = -1;
		for (int i = 0; i < SINGERS; i++) {
			Goat goat = goats.get(i);
			Vec3 at = tick < ARRIVE ? starts.get(i).lerp(places.get(i), tick / (double) ARRIVE) : places.get(i);
			int since = tick - screamed[i];
			// Screaming: head thrown up, a little hop.
			double hop = since >= 0 && since < 10 ? Mth.sin(since / 10.0F * Mth.PI) * 0.4 : 0.0;
			walk(goat, new Vec3(at.x, floor(level, at.x, at.z, player.getY()) + hop, at.z));
			face(goat, since >= 0 && since < 12 ? player.getEyePosition().add(0.0, 3.0, 0.0) : player.getEyePosition());
			if (since >= 0 && since < 12 && (loudest < 0 || screamed[i] > screamed[loudest])) {
				loudest = i;
			}
		}
		Vec3 middle = places.get(SINGERS / 2).add(0.0, 0.9, 0.0);
		turnTowards(player, tick < ROUND_1 || tick >= TUTTI[0] || loudest < 0 ? middle : goats.get(loudest).getEyePosition(), 0.15F);
	}

	private void scream(ClientLevel level, int i, float volume, int tick) {
		Goat goat = goats.get(i);
		screamed[i] = tick;
		sound(level, goat.position(), SCREAMS[i], SoundSource.NEUTRAL, volume, PITCH[i] + (random.nextFloat() - 0.5F) * 0.08F);
		particles(level, ParticleTypes.NOTE, goat.getEyePosition().add(0.0, 0.6, 0.0), 2, 0.3, 0.0);
	}
}
