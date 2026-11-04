package de.derpvpclient.fakedeath.scene;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** The electrician hands the player a cable to hold and switches it on. Sparks, zzzt, and the screen flashes white. */
public class ElectricianScene extends NpcScene {
	private static final DustParticleOptions WIRE = new DustParticleOptions(0xD01010, 0.8F);
	private static final int[] SHOCKS = {12, 32, 52, 72};
	private int flash;

	public ElectricianScene() {
		super("elektriker", ChatFormatting.YELLOW);
	}

	@Override
	protected int lineCount() {
		return 3;
	}

	@Override
	protected int actLength() {
		return 120;
	}

	@Override
	protected ResourceKey<VillagerProfession> profession() {
		return VillagerProfession.TOOLSMITH;
	}

	@Override
	protected ItemStack held() {
		return new ItemStack(Items.REDSTONE);
	}

	@Override
	public int screenTint() {
		return flash > 0 ? (Math.min(255, flash * 40) << 24) | 0xFFFFE0 : 0;
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		if (flash > 0) {
			flash--;
		}
		if (t <= SHOCKS[SHOCKS.length - 1]) {
			// The red cable from his hand into the player's.
			line(level, WIRE, npc.position().add(0.0, 1.0, 0.0), player.position().add(0.0, 1.0, 0.0), 0.2);
			if (t % 3 == 0) {
				particles(level, ParticleTypes.ELECTRIC_SPARK, player.position().add(0.0, 1.0, 0.0), 4, 0.4, 0.3);
				sound(level, player.position(), SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 0.3F, 1.6F + random.nextFloat() * 0.4F);
			}
			cameraShake(t >= 12 ? 0.06F : 0.0F);
		}
		for (int i = 0; i < SHOCKS.length; i++) {
			if (t == SHOCKS[i]) {
				flash = 6;
				cameraShake(0.4F);
				sound(level, player.position(), SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 1.5F, 0.6F);
				particles(level, ParticleTypes.ELECTRIC_SPARK, player.getEyePosition(), 40, 0.6, 0.6);
				hit(level, player, SoundEvents.PLAYER_HURT, 0.3F, i == SHOCKS.length - 1);
			}
		}
		if (t == SHOCKS[SHOCKS.length - 1] + 1) {
			cameraShake(0.0F);
		}
		if (t == 100) {
			speak(minecraft, level, "fakedeath.elektriker.wrong");
		}
	}
}
