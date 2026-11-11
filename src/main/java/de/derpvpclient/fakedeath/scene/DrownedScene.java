package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Bubbles in the water nearby, a drowned rises with a trident and throws it, again and again. */
public class DrownedScene extends Scene {
	private static final int RISE = 30;
	private static final int RISE_TICKS = 25;
	private static final int[] THROWS = {70, 100, 128};
	private static final int FLIGHT = 6;

	private final BlockPos water;
	private Drowned drowned;
	private net.minecraft.world.entity.Entity trident;
	private Vec3 surface;

	public DrownedScene(BlockPos water) {
		this.water = water;
	}

	@Override
	public int length() {
		return THROWS[THROWS.length - 1] + FLIGHT + 8;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.trident", player.getDisplayName(), EntityTypes.DROWNED.getDescription());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			surface = water != null ? Vec3.atBottomCenterOf(water) : groundSpot(level, player, 6.0, 0.0);
		}

		if (tick < RISE + RISE_TICKS && tick % 3 == 0) {
			particles(level, ParticleTypes.BUBBLE_POP, surface.add(0, 0.9, 0), 4, 0.5, 0.05);
			particles(level, ParticleTypes.SPLASH, surface.add(0, 1.0, 0), 3, 0.5, 0.05);
		}

		if (tick % 25 == 0 && tick < THROWS[0]) {
			sound(level, surface, SoundEvents.DROWNED_AMBIENT_WATER, SoundSource.HOSTILE, 1.2F, 0.9F);
		}

		turnTowards(player, surface.add(0, 1.5, 0), 0.1F);

		if (tick == RISE) {
			drowned = spawn(level, EntityTypes.DROWNED, surface.add(0, -2.0, 0));
			drowned.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
			// Arm raised for the throw.
			drowned.setAggressive(true);
		}

		if (drowned == null) {
			return;
		}

		if (tick <= RISE + RISE_TICKS) {
			Vec3 pos = surface.add(0, -2.0 + 2.0 * (tick - RISE) / RISE_TICKS, 0);
			drowned.setPos(pos.x, pos.y, pos.z);
		}

		face(drowned, player.getEyePosition());

		for (int i = 0; i < THROWS.length; i++) {
			int shot = THROWS[i];
			Vec3 from = drowned.getEyePosition();
			Vec3 to = player.getEyePosition().add(0, -0.3, 0);

			if (tick == shot) {
				Looks.swing(drowned);
				sound(level, from, SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 1.5F, 1.0F);
			}

			if (tick >= shot && tick < shot + FLIGHT) {
				Vec3 delta = to.subtract(from);
				line(level, ParticleTypes.CRIT, from.add(delta.scale((tick - shot) / (double) FLIGHT)), from.add(delta.scale((tick - shot + 1) / (double) FLIGHT)), 0.25);
				// A real trident in the air, not only a trail.
				if (tick == shot) {
					trident = spawn(level, EntityTypes.TRIDENT, from);
					trident.setNoGravity(true);
				}
				Vec3 at = from.add(delta.scale((tick - shot + 1) / (double) FLIGHT));
				trident.setPos(at.x, at.y, at.z);
				trident.setYRot((float) (Math.atan2(delta.x, delta.z) * 180.0 / Math.PI));
				trident.setXRot((float) (Math.atan2(delta.y, delta.horizontalDistance()) * 180.0 / Math.PI));
				trident.yRotO = trident.getYRot();
				trident.xRotO = trident.getXRot();
			}

			if (tick == shot + FLIGHT) {
				if (trident != null) {
					despawn(level, trident);
					trident = null;
				}
				sound(level, to, SoundEvents.TRIDENT_HIT, SoundSource.HOSTILE, 1.5F, 1.0F);
				hit(level, player, SoundEvents.PLAYER_HURT, 0.35F, i == THROWS.length - 1);
			}
		}
	}
}
