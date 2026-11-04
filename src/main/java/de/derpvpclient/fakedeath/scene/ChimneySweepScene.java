package de.derpvpclient.fakedeath.scene;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** A chimney sweep stands up on a roof and promises luck. Then he slips and lands right on the player. */
public class ChimneySweepScene extends NpcScene {
	private static final double HEIGHT = 7.0;
	private static final int FALL = 16;
	private Vec3 roof;

	public ChimneySweepScene() {
		super("schornsteinfeger", ChatFormatting.DARK_GRAY);
	}

	@Override
	protected int lineCount() {
		return 3;
	}

	@Override
	protected int actLength() {
		return FALL + 60;
	}

	@Override
	protected boolean walksIn() {
		return false;
	}

	@Override
	protected double standDistance() {
		return 3.0;
	}

	@Override
	protected ResourceKey<VillagerProfession> profession() {
		return VillagerProfession.NITWIT;
	}

	@Override
	protected ItemStack held() {
		return new ItemStack(Items.COAL);
	}

	@Override
	protected void setup(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
		roof = stand.add(0.0, HEIGHT, 0.0);
		npc.setPos(roof.x, roof.y, roof.z);
		npc.setNoGravity(true);
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		super.tick(minecraft, level, player, tick);
		if (npc != null && tick < actStart()) {
			npc.setPos(roof.x, roof.y, roof.z);
			if (tick % 4 == 0) {
				particles(level, ParticleTypes.LARGE_SMOKE, roof.add(0.0, 1.0, 0.0), 2, 0.3, 0.05);
			}
		}
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		if (t == 0) {
			sound(level, roof, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 1.2F, 1.0F);
		}
		if (t <= FALL) {
			float p = t / (float) FALL;
			Vec3 at = roof.lerp(player.position().add(0.0, 1.2, 0.0), p * p);
			npc.setPos(at.x, at.y, at.z);
			particles(level, ParticleTypes.LARGE_SMOKE, at.add(0.0, 1.0, 0.0), 2, 0.3, 0.05);
		}
		if (t == FALL) {
			sound(level, player.position(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 1.0F, 0.8F);
			particles(level, ParticleTypes.LARGE_SMOKE, player.getEyePosition(), 30, 0.8, 0.05);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
		if (t == FALL + 30) {
			speak(minecraft, level, "fakedeath.schornsteinfeger.luck");
		}
	}
}
