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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * A raid on the village: the horn sounds twice, pillagers march in with crossbows, two volleys,
 * and then the ravager roars and runs the player over.
 */
public class RaidScene extends Scene {
	private static final byte RAVAGER_ATTACK = 4;
	private static final int[] HORNS = {0, 45};
	private static final int APPEAR = 70;
	private static final int ARRIVE = 125;
	private static final int CHARGE_1 = 130;
	private static final int VOLLEY_1 = 150;
	private static final int CHARGE_2 = 162;
	private static final int VOLLEY_2 = 182;
	private static final int ROAR = 196;
	private static final int RAM = 214;
	private static final int HIT = 236;
	private static final int DEATH = HIT + 8;
	private static final int FLIGHT = 6;
	private static final double[] SIDES = {-3.0, -1.0, 1.0, 3.0};

	private final List<Walker> pillagers = new ArrayList<>();
	private Walker ravager;
	private Vec3 direction;
	private Vec3 side;
	private final List<Vec3> targets = new ArrayList<>();

	@Override
	public int length() {
		return DEATH;
	}

	@Override
	public int photoTick() {
		return ROAR + 6;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.mob", player.getDisplayName(), EntityType.RAVAGER.getDescription());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		for (int horn : HORNS) {
			if (tick == horn) {
				Vec3 far = groundSpot(level, player, 40.0, 0.0);
				sound(level, far, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 12.0F, 1.0F);
			}
		}

		if (tick == APPEAR) {
			appear(level, player);
		}

		if (ravager == null) {
			if (tick < APPEAR) {
				turnTowards(player, groundSpot(level, player, 20.0, 0.0).add(0.0, 2.0, 0.0), 0.05F);
			}
			return;
		}

		for (Walker walker : pillagers) {
			walker.walk(level, player, tick);
		}
		ravager.walk(level, player, tick);

		if (tick < ROAR) {
			turnTowards(player, pillagers.get(1).mob.getEyePosition(), 0.1F);
		} else if (tick != HIT) {
			turnTowards(player, ravager.mob.getEyePosition(), 0.25F);
		}

		if (tick == APPEAR + 10 || tick == ARRIVE) {
			sound(level, pillagers.get(0).pos, SoundEvents.PILLAGER_AMBIENT, SoundSource.HOSTILE, 1.5F, 1.0F);
			sound(level, ravager.pos, SoundEvents.RAVAGER_AMBIENT, SoundSource.HOSTILE, 1.5F, 1.0F);
		}

		if (tick == CHARGE_1 || tick == CHARGE_2) {
			for (Walker walker : pillagers) {
				((Pillager) walker.mob).setChargingCrossbow(true);
			}
			sound(level, pillagers.get(1).pos, SoundEvents.CROSSBOW_LOADING_START.value(), SoundSource.HOSTILE, 1.5F, 1.0F);
		}

		if (tick == CHARGE_1 + 10 || tick == CHARGE_2 + 10) {
			sound(level, pillagers.get(2).pos, SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), SoundSource.HOSTILE, 1.5F, 1.0F);
		}

		if (tick == VOLLEY_1 || tick == VOLLEY_2) {
			volley(level, player, tick == VOLLEY_2);
		}

		if (tick > VOLLEY_1 && tick <= VOLLEY_1 + FLIGHT || tick > VOLLEY_2 && tick <= VOLLEY_2 + FLIGHT) {
			int shot = tick > VOLLEY_2 ? VOLLEY_2 : VOLLEY_1;
			arrows(level, player, tick - shot, tick == shot + FLIGHT);
		}

		if (tick == ROAR) {
			sound(level, ravager.pos, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3.0F, 1.0F);
			particles(level, ParticleTypes.POOF, ravager.pos.add(0.0, 1.0, 0.0), 30, 1.5, 0.2);
		}

		if (tick == RAM) {
			ravager.walkTo(player.position().add(direction.scale(1.6)), tick, HIT - RAM, true);
		}

		if (tick > RAM && tick < HIT && tick % 4 == 0) {
			sound(level, ravager.pos, SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 2.0F, 1.0F);
			particles(level, ParticleTypes.POOF, ravager.pos, 4, 0.8, 0.05);
		}

		if (tick == HIT) {
			ravager.mob.handleEntityEvent(RAVAGER_ATTACK);
			sound(level, player.position(), SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 2.0F, 1.0F);
			particles(level, ParticleTypes.CRIT, player.getEyePosition().add(0.0, -0.4, 0.0), 20, 0.5, 0.3);
			kill(level, player, SoundEvents.PLAYER_HURT);
			turnTowards(player, player.getEyePosition().add(0.0, 4.0, 0.0), 0.6F);
		}

		if (tick == DEATH + 12) {
			sound(level, pillagers.get(0).pos, SoundEvents.PILLAGER_CELEBRATE, SoundSource.HOSTILE, 1.5F, 1.0F);
			sound(level, ravager.pos, SoundEvents.RAVAGER_CELEBRATE, SoundSource.HOSTILE, 1.5F, 1.0F);
		}
	}

	private void appear(ClientLevel level, LocalPlayer player) {
		Vec3 spot = groundSpot(level, player, 20.0, (random.nextDouble() - 0.5) * 50.0);
		Vec3 away = spot.subtract(player.position());
		direction = new Vec3(away.x, 0.0, away.z).normalize();
		side = new Vec3(-direction.z, 0.0, direction.x);

		for (double offset : SIDES) {
			Pillager pillager = spawn(level, EntityType.PILLAGER, spot.add(side.scale(offset)));
			pillager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
			Walker walker = new Walker(pillager, spot.add(side.scale(offset)));
			walker.walkTo(player.position().add(direction.scale(9.0)).add(side.scale(offset * 0.7)), APPEAR, ARRIVE - APPEAR, false);
			pillagers.add(walker);
		}

		Vec3 behind = spot.add(direction.scale(3.0));
		ravager = new Walker(spawn(level, EntityType.RAVAGER, behind), behind);
		ravager.walkTo(player.position().add(direction.scale(13.0)), APPEAR, ARRIVE - APPEAR + 20, false);
	}

	private void volley(ClientLevel level, LocalPlayer player, boolean hits) {
		targets.clear();

		for (int i = 0; i < pillagers.size(); i++) {
			Pillager pillager = (Pillager) pillagers.get(i).mob;
			pillager.setChargingCrossbow(false);
			// The first volley only lands around the player, in the second one two arrows hit.
			boolean hit = hits && i % 2 == 1;
			Vec3 miss = player.position().add(side.scale((random.nextDouble() - 0.5) * 4.0)).add(direction.scale(random.nextDouble() * 1.5 - 0.5));
			targets.add(hit ? player.getEyePosition().add(0.0, -0.3, 0.0) : miss);
		}

		sound(level, pillagers.get(1).pos, SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.5F, 1.0F);
		sound(level, pillagers.get(2).pos, SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.5F, 1.1F);
	}

	private void arrows(ClientLevel level, LocalPlayer player, int flown, boolean landed) {
		for (int i = 0; i < pillagers.size() && i < targets.size(); i++) {
			Vec3 from = pillagers.get(i).mob.getEyePosition();
			Vec3 delta = targets.get(i).subtract(from);
			line(level, ParticleTypes.CRIT, from.add(delta.scale((flown - 1) / (double) FLIGHT)), from.add(delta.scale(flown / (double) FLIGHT)), 0.3);

			if (landed) {
				boolean hit = targets.get(i).distanceTo(player.getEyePosition()) < 1.0;
				if (hit) {
					sound(level, player.position(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.0F, 1.0F);
					hurt(level, player, SoundEvents.PLAYER_HURT, 0.15F);
				} else {
					sound(level, targets.get(i), SoundEvents.ARROW_HIT, SoundSource.HOSTILE, 1.0F, 1.0F);
				}
			}
		}
	}

	private static final class Walker {
		final Mob mob;
		Vec3 pos;
		Vec3 from;
		Vec3 to;
		int start = -1;
		int ticks;
		boolean rush;

		Walker(Mob mob, Vec3 pos) {
			this.mob = mob;
			this.pos = pos;
			mob.noPhysics = true;
		}

		void walkTo(Vec3 target, int tick, int duration, boolean fast) {
			from = pos;
			to = target;
			start = tick;
			ticks = duration;
			rush = fast;
		}

		void walk(ClientLevel level, LocalPlayer player, int tick) {
			if (start >= 0) {
				float t = Math.min(1.0F, (tick - start) / (float) ticks);
				Vec3 at = from.lerp(to, rush ? t * t : t);
				pos = new Vec3(at.x, floor(level, at.x, at.z, player.getY()), at.z);
				if (t >= 1.0F) {
					start = -1;
				}
			}

			Scene.walk(mob, pos);
			face(mob, player.getEyePosition());
		}
	}
}
