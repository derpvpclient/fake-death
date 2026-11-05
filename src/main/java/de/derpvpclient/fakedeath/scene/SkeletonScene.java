package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** A skeleton rattles somewhere behind and shoots from a distance, the first arrow misses, the rest hit. */
public class SkeletonScene extends Scene {
	private static final int[] ARROWS = {35, 60, 82, 100};
	private static final int FLIGHT = 7;

	private Skeleton skeleton;
	private net.minecraft.world.entity.Entity arrow;

	@Override
	public int length() {
		return ARROWS[ARROWS.length - 1] + FLIGHT + 8;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.arrow", player.getDisplayName(), EntityType.SKELETON.getDescription());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			skeleton = spawn(level, EntityType.SKELETON, groundSpot(level, player, 7.0, 160.0));
			skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
			// Drawn bow.
			skeleton.setAggressive(true);
		}

		if (skeleton == null) {
			return;
		}

		face(skeleton, player.getEyePosition());

		if (tick == 0 || tick == 20) {
			sound(level, skeleton.position(), SoundEvents.SKELETON_AMBIENT, SoundSource.HOSTILE, 1.0F, 1.0F);
		}

		if (tick >= 10) {
			turnTowards(player, skeleton.getEyePosition(), 0.12F);
		}

		for (int i = 0; i < ARROWS.length; i++) {
			int shot = ARROWS[i];
			boolean miss = i == 0;
			Vec3 from = skeleton.getEyePosition();
			Vec3 target = miss ? player.position().add(horizontalLook(player).yRot(1.4F).scale(1.2)) : player.getEyePosition().add(0, -0.3, 0);

			if (tick == shot) {
				sound(level, skeleton.position(), SoundEvents.SKELETON_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F);
			}

			if (tick >= shot && tick < shot + FLIGHT) {
				Vec3 delta = target.subtract(from);
				line(level, ParticleTypes.CRIT, from.add(delta.scale((tick - shot) / (double) FLIGHT)), from.add(delta.scale((tick - shot + 1) / (double) FLIGHT)), 0.3);
				// A real arrow in the air, not only a trail.
				if (tick == shot) {
					arrow = spawn(level, EntityType.ARROW, from);
					arrow.setNoGravity(true);
				}
				Vec3 at = from.add(delta.scale((tick - shot + 1) / (double) FLIGHT));
				arrow.setPos(at.x, at.y, at.z);
				arrow.setYRot((float) (Math.atan2(delta.x, delta.z) * 180.0 / Math.PI));
				arrow.setXRot((float) (Math.atan2(delta.y, delta.horizontalDistance()) * 180.0 / Math.PI));
				arrow.yRotO = arrow.getYRot();
				arrow.xRotO = arrow.getXRot();
				turnTowards(player, at, 0.25F);
			}

			if (tick == shot + FLIGHT) {
				if (arrow != null) {
					despawn(level, arrow);
					arrow = null;
				}
				if (miss) {
					sound(level, target, SoundEvents.ARROW_HIT, SoundSource.HOSTILE, 1.0F, 1.0F);
				} else {
					sound(level, player.position(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.0F, 1.0F);
					hit(level, player, SoundEvents.PLAYER_HURT, 0.3F, i == ARROWS.length - 1);
				}
			}
		}
	}
}
