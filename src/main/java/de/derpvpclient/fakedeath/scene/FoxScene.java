package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** A fox sneaks up and steals the item out of the player's hand. While they watch it run off, a second fox bites. */
public class FoxScene extends Scene {
	private static final int GRAB = 30;
	private static final int GONE = 75;
	private static final int POUNCE = 95;
	private static final int[] BITES = {POUNCE + 10, POUNCE + 22, POUNCE + 36};
	private final ItemStack loot;
	private Fox thief;
	private Fox biter;
	private Vec3 start;
	private Vec3 behind;
	private boolean stolen;

	public FoxScene(LocalPlayer player) {
		ItemStack held = player.getMainHandItem();
		this.loot = held.isEmpty() ? new ItemStack(Items.SWEET_BERRIES) : held.copyWithCount(1);
	}

	@Override
	public int length() {
		return BITES[BITES.length - 1] + 30;
	}

	@Override
	public int photoTick() {
		return GRAB - 2;
	}

	@Override
	public InteractionHand hiddenHand() {
		return stolen ? InteractionHand.MAIN_HAND : null;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.fuchs", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			start = groundSpot(level, player, 9.0, 30.0);
			behind = groundSpot(level, player, 6.0, 180.0);
			thief = spawn(level, EntityTypes.FOX, start);
			thief.noPhysics = true;
		}
		if (thief == null) {
			return;
		}

		Vec3 grabAt = player.position().add(start.subtract(player.position()).normalize().scale(0.8));
		if (tick <= GRAB) {
			walk(thief, ground(level, player, start.lerp(grabAt, tick / (double) GRAB)));
			face(thief, player.getEyePosition());
		} else if (tick <= GONE) {
			Vec3 away = player.position().add(start.subtract(player.position()).normalize().scale(16.0));
			walk(thief, ground(level, player, grabAt.lerp(away, (tick - GRAB) / (double) (GONE - GRAB))));
			face(thief, thief.getEyePosition().add(start.subtract(player.position())));
		}
		if (tick == GRAB) {
			stolen = true;
			thief.setItemSlot(EquipmentSlot.MAINHAND, loot);
			sound(level, player.position(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.2F);
			sound(level, thief.position(), SoundEvents.FOX_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.4F);
		}
		if (tick < POUNCE) {
			turnTowards(player, thief.getEyePosition(), 0.15F);
		}
		if (tick == GONE) {
			despawn(level, thief);
		}

		if (tick == POUNCE - 15) {
			biter = spawn(level, EntityTypes.FOX, behind);
			biter.noPhysics = true;
			sound(level, behind, SoundEvents.FOX_SCREECH, SoundSource.NEUTRAL, 1.5F, 1.0F);
		}
		if (biter != null) {
			int t = tick - POUNCE;
			Vec3 at = t < 0 ? behind : behind.lerp(player.position(), Math.min(1.0, t / 10.0)).add(0.0, t >= 0 && t < 10 ? Mth.sin(t / 10.0F * Mth.PI) * 1.2 : 0.0, 0.0);
			walk(biter, at);
			face(biter, player.getEyePosition());
			turnTowards(player, biter.getEyePosition(), 0.3F);
		}
		for (int i = 0; i < BITES.length; i++) {
			if (tick == BITES[i]) {
				sound(level, player.position(), SoundEvents.FOX_BITE, SoundSource.NEUTRAL, 1.2F, 1.0F);
				hit(level, player, SoundEvents.PLAYER_HURT, 0.35F, i == BITES.length - 1);
			}
		}
	}

	private static Vec3 ground(ClientLevel level, LocalPlayer player, Vec3 pos) {
		return new Vec3(pos.x, floor(level, pos.x, pos.z, player.getY()), pos.z);
	}
}
