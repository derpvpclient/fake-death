package de.derpvpclient.fakedeath.scene;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The barber: "Just the tips?" The scissors snip faster and faster, and soon it is a lot more than the tips. */
public class BarberScene extends NpcScene {
	private static final DustParticleOptions HAIR = new DustParticleOptions(0x5A3A22, 1.0F);
	private static final int CUT = 90;
	private int nextSnip;

	public BarberScene() {
		super("friseur", ChatFormatting.AQUA);
	}

	@Override
	protected int lineCount() {
		return 3;
	}

	@Override
	protected int actLength() {
		return CUT + 50;
	}

	@Override
	protected double standDistance() {
		return 1.3;
	}

	@Override
	protected VillagerProfession profession() {
		return VillagerProfession.SHEPHERD;
	}

	@Override
	protected ItemStack held() {
		return Looks.prop(Items.SHEARS, "barber_scissors");
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		if (t < CUT && t >= nextSnip) {
			// From one snip every 12 ticks to one every 2.
			nextSnip = t + Math.max(2, 12 - t / 8);
			Looks.swing(npc);
			sound(level, player.getEyePosition(), SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 1.0F, 1.0F + t / 90.0F);
			particles(level, HAIR, player.getEyePosition().add(0.0, 0.4, 0.0), 6, 0.3, 0.05);
			cameraShake(t / 1500.0F);
		}
		if (t == 35 || t == 60 || t == 78) {
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.25F);
		}
		if (t == CUT) {
			cameraShake(0.0F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
		if (t == CUT + 25) {
			speak(minecraft, level, "fakedeath.friseur.short");
		}
	}
}
