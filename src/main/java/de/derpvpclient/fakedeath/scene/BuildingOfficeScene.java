package de.derpvpclient.fakedeath.scene;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** The building authority inspects the house: not approved. It has to go, starting with the block under the player. */
public class BuildingOfficeScene extends NpcScene {
	private static final int DEPTH = 9;
	private static final int FALL = 30;

	public BuildingOfficeScene() {
		super("bauamt", ChatFormatting.GRAY);
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
	protected ResourceKey<VillagerProfession> profession() {
		return VillagerProfession.CARTOGRAPHER;
	}

	@Override
	protected ItemStack held() {
		return Looks.prop(Items.PAPER, "clipboard");
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		BlockPos below = player.blockPosition().below();
		if (t == 0) {
			BlockState ground = level.getBlockState(below);
			Looks.swing(npc);
			sound(level, player.position(), SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.5F, 0.8F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, ground.isAir() ? Blocks.DIRT.defaultBlockState() : ground), player.position(), 30, 0.5, 0.2);
			openShaft(level, player, DEPTH);
		}
		if (t > 2 && t <= FALL) {
			float p = (t - 2) / (float) (FALL - 2);
			moveCamera(new Vec3(0.0, -p * p * (DEPTH - 1), 0.0));
		}
		if (t == FALL) {
			sound(level, player.position(), SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 1.5F, 1.0F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
		if (t == FALL + 30) {
			speak(minecraft, level, "fakedeath.bauamt.done");
		}
	}
}
