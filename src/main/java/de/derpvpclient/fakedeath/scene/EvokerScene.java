package de.derpvpclient.fakedeath.scene;

import de.derpvpclient.fakedeath.mixin.SpellcasterIllagerAccessor;
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
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.phys.Vec3;

/**
 * In the dark forest an evoker steps out of the trees. A line of fangs stops right in front of
 * the player, a ring of fangs snaps around them, and the third spell comes up right under their feet.
 */
public class EvokerScene extends Scene {
	private static final byte BITE = 4;
	private static final byte NO_SPELL = 0;
	private static final byte FANGS = 2;
	private static final int FANG_LIFE = 22;
	private static final int APPEAR = 15;
	private static final int LINE_CAST = 35;
	private static final int LINE = 55;
	private static final int CIRCLE_CAST = 100;
	private static final int CIRCLE = 118;
	private static final int FINAL_CAST = 158;
	private static final int FINAL = 176;
	private static final int DEATH = FINAL + 20;

	private final List<Fang> fangs = new ArrayList<>();
	private Evoker evoker;
	private Vec3 direction;
	private int castUntil = -1;
	private int now;

	@Override
	public int length() {
		return DEATH;
	}

	@Override
	public int photoTick() {
		return CIRCLE + 4;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.indirectMagic", player.getDisplayName(), EntityType.EVOKER.getDescription());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		now = tick;

		if (tick == 0) {
			sound(level, groundSpot(level, player, 8.0, 0.0), SoundEvents.EVOKER_AMBIENT, SoundSource.HOSTILE, 1.5F, 0.9F);
		}

		if (tick == APPEAR) {
			Vec3 spot = groundSpot(level, player, 8.0, (random.nextDouble() - 0.5) * 60.0);
			Vec3 away = spot.subtract(player.position());
			direction = new Vec3(away.x, 0.0, away.z).normalize();
			evoker = spawn(level, EntityType.EVOKER, spot);
			evoker.noPhysics = true;
			particles(level, ParticleTypes.WITCH, spot.add(0.0, 1.0, 0.0), 20, 0.5, 0.1);
			particles(level, ParticleTypes.POOF, spot.add(0.0, 1.0, 0.0), 10, 0.4, 0.05);
			sound(level, spot, SoundEvents.EVOKER_AMBIENT, SoundSource.HOSTILE, 1.5F, 1.0F);
		}

		fangs.removeIf(fang -> {
			if (tick >= fang.gone) {
				despawn(level, fang.entity);
				return true;
			}
			return false;
		});

		if (evoker == null) {
			return;
		}

		face(evoker, player.getEyePosition());
		turnTowards(player, tick < FINAL ? evoker.getEyePosition() : player.position(), tick < FINAL ? 0.12F : 0.2F);

		if (tick == LINE_CAST || tick == CIRCLE_CAST || tick == FINAL_CAST) {
			cast(FANGS);
			castUntil = tick + 30;
			sound(level, evoker.position(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 1.5F, 1.0F);
		}

		if (castUntil >= 0 && tick >= castUntil) {
			cast(NO_SPELL);
			castUntil = -1;
		}

		// A line of fangs towards the player, the last one bites right in front of them.
		Vec3 start = evoker.position();
		double reach = player.position().subtract(start).horizontalDistance() - 1.4;
		int count = Math.max(1, (int) (reach / 0.9));
		int line = tick - LINE;
		if (line >= 0 && line < count * 2 && line % 2 == 0) {
			Vec3 at = start.add(direction.scale(-(1.2 + line / 2 * (reach - 1.2) / Math.max(1, count - 1))));
			fang(level, player, at);
		}

		if (tick == CIRCLE || tick == CIRCLE + 4) {
			int ring = tick == CIRCLE ? 5 : 8;
			double radius = tick == CIRCLE ? 1.5 : 2.5;
			for (int i = 0; i < ring; i++) {
				float angle = i * Mth.TWO_PI / ring + (tick == CIRCLE ? 0.0F : 0.4F);
				fang(level, player, player.position().add(Mth.cos(angle) * radius, 0.0, Mth.sin(angle) * radius));
			}
		}

		if (tick == CIRCLE + 10) {
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.2F);
		}

		if (tick == FINAL) {
			fang(level, player, player.position());
			fang(level, player, player.position().add(direction.scale(0.6)));
			fang(level, player, player.position().add(direction.scale(-0.6)));
		}

		if (tick == FINAL + 6 || tick == FINAL + 13) {
			hit(level, player, SoundEvents.PLAYER_HURT, 0.5F, tick == FINAL + 13);
		}

		if (tick == DEATH + 10) {
			sound(level, evoker.position(), SoundEvents.EVOKER_CELEBRATE, SoundSource.HOSTILE, 1.5F, 1.0F);
		}
	}

	/** Raised arms and spell particles, the client draws them from the synced spell id. */
	private void cast(byte spell) {
		evoker.getEntityData().set(SpellcasterIllagerAccessor.fakedeath$spellData(), spell);
	}

	private void fang(ClientLevel level, LocalPlayer player, Vec3 at) {
		Vec3 ground = new Vec3(at.x, floor(level, at.x, at.z, player.getY()), at.z);
		float yaw = (float) Mth.atan2(player.getZ() - ground.z, player.getX() - ground.x);
		EvokerFangs entity = spawn(level, new EvokerFangs(level, ground.x, ground.y, ground.z, yaw, 0, evoker), ground);
		entity.handleEntityEvent(BITE);
		sound(level, ground, SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 1.0F, 0.9F + random.nextFloat() * 0.2F);
		fangs.add(new Fang(entity, now + FANG_LIFE));
	}

	private record Fang(EvokerFangs entity, int gone) {
	}
}
