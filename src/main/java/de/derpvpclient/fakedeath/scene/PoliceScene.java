package de.derpvpclient.fakedeath.scene;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** A traffic check: no license for the horse, and way too fast. "Smile for the camera!" The flash is real lightning. */
public class PoliceScene extends NpcScene {
	private static final int FLASH = 8;
	private static final int STRIKE = 18;
	private int flash;

	public PoliceScene() {
		super("polizist", ChatFormatting.BLUE);
	}

	@Override
	protected int lineCount() {
		return 3;
	}

	@Override
	protected int actLength() {
		return STRIKE + 70;
	}

	@Override
	protected ResourceKey<VillagerProfession> profession() {
		return VillagerProfession.ARMORER;
	}

	@Override
	protected ItemStack held() {
		// His baton while he talks, the mod's own prop. For the photo he takes out his camera.
		return Looks.prop(Items.STICK, "police_baton");
	}

	@Override
	public int screenTint() {
		return flash > 0 ? (Math.min(255, flash * 50) << 24) | 0xFFFFFF : 0;
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		if (t == 0) {
			npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, Looks.prop(Items.SPYGLASS, "police_camera"));
			Looks.swing(npc);
			sound(level, npc.position(), SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.NEUTRAL, 1.0F, 1.4F);
		}
		if (flash > 0) {
			flash--;
		}
		if (t == FLASH) {
			// The speed camera.
			flash = 5;
			sound(level, npc.position(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.NEUTRAL, 1.5F, 1.8F);
		}
		if (t == STRIKE) {
			flash = 6;
			LightningBolt bolt = spawn(level, EntityTypes.LIGHTNING_BOLT, player.position());
			bolt.setVisualOnly(true);
			bolt.setSilent(false);
			setOnFire(player);
			kill(level, player, SoundEvents.PLAYER_HURT_ON_FIRE);
		}
		if (t == STRIKE + 40) {
			speak(minecraft, level, "fakedeath.polizist.fine");
		}
	}
}
