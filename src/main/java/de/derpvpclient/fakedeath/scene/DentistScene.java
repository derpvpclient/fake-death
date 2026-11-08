package de.derpvpclient.fakedeath.scene;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** At the dentist: "Open wide." Then the drill, louder and louder, until everything goes black. */
public class DentistScene extends NpcScene {
	private static final int DRILL = 90;
	private int dark;

	public DentistScene() {
		super("zahnarzt", ChatFormatting.DARK_AQUA);
	}

	@Override
	protected int lineCount() {
		return 3;
	}

	@Override
	protected int actLength() {
		return DRILL + 50;
	}

	@Override
	protected double standDistance() {
		return 1.4;
	}

	@Override
	protected ResourceKey<VillagerProfession> profession() {
		return VillagerProfession.CLERIC;
	}

	@Override
	protected ItemStack held() {
		return new ItemStack(Items.IRON_PICKAXE);
	}

	@Override
	public int screenTint() {
		return dark << 24;
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		if (t < DRILL) {
			if (t % 3 == 0) {
				sound(level, player.getEyePosition(), SoundEvents.GRINDSTONE_USE, SoundSource.NEUTRAL, 0.6F + t / 120.0F, 1.8F + t / 200.0F);
			}
			cameraShake(0.04F + t / 900.0F);
			if (t % 4 == 0) {
				Looks.swing(npc);
				particles(level, ParticleTypes.CRIT, player.getEyePosition().add(towardPlayer(player).scale(-0.3)), 2, 0.1, 0.2);
			}
			if (t == 25 || t == 50 || t == 70) {
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.25F);
			}
			dark = Math.min(255, Math.max(0, (t - 40) * 6));
		}
		if (t == DRILL) {
			dark = 255;
			cameraShake(0.0F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
		if (t == DRILL + 25) {
			speak(minecraft, level, "fakedeath.zahnarzt.rinse");
		}
	}
}
