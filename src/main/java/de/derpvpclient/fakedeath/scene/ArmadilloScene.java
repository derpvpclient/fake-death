package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.phys.Vec3;

/**
 * The armadillo next to the player rolls itself up, takes a run-up like a bowling ball and knocks the player over.
 * One pin is not enough for it: it rolls back and comes a second time.
 */
public class ArmadilloScene extends Scene {
	private static final int ROLL_UP = 20;
	private static final int BACK = 34;
	private static final int GO_1 = 58;
	private static final int HIT_1 = 72;
	private static final int TURN = 92;
	private static final int GO_2 = 112;
	private static final int HIT_2 = 124;
	private final Entity found;
	private Armadillo ball;
	private Vec3 start;
	private Vec3 far;
	private Vec3 beyond;
	private float down;

	public ArmadilloScene(Entity armadillo) {
		this.found = armadillo;
	}

	@Override
	public int length() {
		return HIT_2 + 20;
	}

	@Override
	public int photoTick() {
		return HIT_1 - 3;
	}

	@Override
	public float lying() {
		return down;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.guerteltier", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		Vec3 feet = player.position();

		if (tick == 0) {
			if (found != null && found.isAlive()) {
				// The real one steps aside for the scene, its double does the bowling.
				start = found.position();
				found.setInvisible(true);
			} else {
				start = groundSpot(level, player, 3.0, 15.0);
			}

			Vec3 away = start.subtract(feet);
			Vec3 dir = new Vec3(away.x, 0.0, away.z);
			dir = dir.lengthSqr() < 0.3 ? horizontalLook(player) : dir.normalize();
			start = feet.add(dir.scale(Math.max(2.5, Math.min(4.0, Math.sqrt(away.x * away.x + away.z * away.z)))));
			far = feet.add(dir.scale(9.0));
			beyond = feet.add(dir.scale(-5.0));
			ball = spawn(level, EntityTypes.ARMADILLO, ground(level, start, player));
			ball.noPhysics = true;
			AttributeInstance size = ball.getAttribute(Attributes.SCALE);
			if (size != null) {
				size.setBaseValue(1.6);
				ball.refreshDimensions();
			}
			sound(level, start, SoundEvents.ARMADILLO_AMBIENT, SoundSource.NEUTRAL, 1.5F, 1.0F);
		}

		if (tick == ROLL_UP || tick == TURN + 2) {
			ball.switchToState(Armadillo.ArmadilloState.ROLLING);
			sound(level, ball.position(), SoundEvents.ARMADILLO_ROLL, SoundSource.NEUTRAL, 2.0F, 1.0F);
		}
		if (tick == ROLL_UP + 10 || tick == TURN + 12) {
			ball.switchToState(Armadillo.ArmadilloState.SCARED);
		}

		Vec3 at;
		boolean rolling = false;
		if (tick < BACK) {
			at = start;
		} else if (tick < GO_1) {
			// The run-up: slowly backwards, like someone who means it.
			float p = (tick - BACK) / (float) (GO_1 - BACK);
			at = start.lerp(far, p * p * (3.0F - 2.0F * p));
			rolling = true;
		} else if (tick <= HIT_1 + 10) {
			// And off it goes, faster and faster, right through.
			float p = (tick - GO_1) / (float) (HIT_1 - GO_1);
			at = far.lerp(feet, p * p);
			if (p > 1.0F) {
				at = feet.lerp(beyond, (p - 1.0F) * 1.4F);
			}
			rolling = true;
		} else if (tick < GO_2) {
			at = beyond;
		} else {
			float p = (tick - GO_2) / (float) (HIT_2 - GO_2);
			at = beyond.lerp(feet, Math.min(1.0F, p * p));
			rolling = true;
		}

		at = ground(level, at, player);
		if (rolling) {
			// A ball bounces a little.
			at = at.add(0.0, Math.abs(Math.sin(tick * 0.9)) * 0.3, 0.0);
			if (tick % 4 == 0) {
				particles(level, ParticleTypes.POOF, at.add(0.0, -0.1, 0.0), 1, 0.1, 0.01);
			}
			if (tick % 4 == 0) {
				sound(level, at, SoundEvents.ARMADILLO_LAND, SoundSource.NEUTRAL, 1.2F, 0.8F + (tick % 12) * 0.03F);
			}
		}

		ball.setPos(at.x, at.y, at.z);
		if (tick < HIT_1) {
			face(ball, player.getEyePosition());
			turnTowards(player, at.add(0.0, 0.5, 0.0), tick > GO_1 ? 0.4F : 0.2F);
		} else if (tick >= TURN) {
			face(ball, player.getEyePosition());
		}
		ball.setXRot(0.0F);

		if (tick == TURN) {
			// Still one pin standing: out of the shell for a look, and back in.
			ball.switchToState(Armadillo.ArmadilloState.IDLE);
			sound(level, at, SoundEvents.ARMADILLO_AMBIENT, SoundSource.NEUTRAL, 1.5F, 1.2F);
		}

		if (tick == HIT_1) {
			sound(level, feet, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.NEUTRAL, 2.0F, 0.7F);
			sound(level, feet, SoundEvents.WOOD_BREAK, SoundSource.NEUTRAL, 2.0F, 1.4F);
			particles(level, ParticleTypes.CRIT, feet.add(0.0, 0.5, 0.0), 20, 0.4, 0.4);
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.6F);
		}

		if (tick >= HIT_1) {
			// Bowled over: the view tips back to the sky and down to the floor.
			down = Math.min(1.0F, (tick - HIT_1) / 8.0F);
			moveCamera(new Vec3(0.0, -1.25 * down, 0.0));
			if (tick < TURN) {
				turnTowards(player, player.getEyePosition().add(horizontalLook(player).scale(0.2)).add(0.0, 3.0, 0.0), 0.3F);
			} else {
				turnTowards(player, at.add(0.0, 0.5, 0.0), 0.45F);
			}
		}

		if (tick == HIT_2) {
			sound(level, feet, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.NEUTRAL, 2.0F, 0.6F);
			sound(level, feet, SoundEvents.WOOD_BREAK, SoundSource.NEUTRAL, 2.0F, 1.2F);
			particles(level, ParticleTypes.CRIT, player.getEyePosition().add(0.0, -1.2, 0.0), 25, 0.4, 0.4);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private static Vec3 ground(ClientLevel level, Vec3 spot, LocalPlayer player) {
		return new Vec3(spot.x, floor(level, spot.x, spot.z, player.getY()), spot.z);
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		if (found != null) {
			found.setInvisible(false);
		}
		ball = null;
		moveCamera(Vec3.ZERO);
		super.end(level, player);
	}
}
