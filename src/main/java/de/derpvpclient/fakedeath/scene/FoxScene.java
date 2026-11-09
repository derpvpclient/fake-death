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
			walk(biter, biterAt(level, player, tick));
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

	/**
	 * The second fox leaps right at the player's face for the first bite, drops down in front of them and jumps up again
	 * for every further bite. It never lands on the player's own spot, the view would look straight down into the feet.
	 */
	private Vec3 biterAt(ClientLevel level, LocalPlayer player, int tick) {
		int t = tick - POUNCE;
		if (t < 0) {
			return behind;
		}
		Vec3 toFox = behind.subtract(player.position());
		toFox = new Vec3(toFox.x, 0.0, toFox.z).normalize();
		Vec3 landing = ground(level, player, player.position().add(toFox.scale(1.1)));
		Vec3 face = player.position().add(toFox.scale(0.55)).add(0.0, 1.0, 0.0);
		if (t <= 10) {
			float f = t / 10.0F;
			return behind.lerp(face, f).add(0.0, Mth.sin(f * Mth.PI) * 0.8, 0.0);
		}
		// Falling back after the first bite, then up at the face again for each of the others.
		double lunge = t < 16 ? 1.0 - (t - 10) / 6.0 : 0.0;
		for (int i = 1; i < BITES.length; i++) {
			int s = tick - BITES[i];
			if (s >= -4 && s < 4) {
				lunge = Math.max(lunge, 1.0 - Math.abs(s) / 4.0);
			}
		}
		return landing.lerp(face, lunge);
	}

	private static Vec3 ground(ClientLevel level, LocalPlayer player, Vec3 pos) {
		return new Vec3(pos.x, floor(level, pos.x, pos.z, player.getY()), pos.z);
	}
}
