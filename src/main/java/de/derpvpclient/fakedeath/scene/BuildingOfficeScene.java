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
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** The building authority inspects the house: not approved. It has to go, starting with the block under the player. */
public class BuildingOfficeScene extends NpcScene {
	private static final int DEPTH = 9;
	private static final int FALL = 30;
	// Before anything is torn down it is stamped: the stamp goes up, comes down in front of the feet, and then the ground goes.
	private static final int STAMP = 22;
	private static final int STAMP_DOWN = 16;
	private static final net.minecraft.core.particles.DustParticleOptions INK = new net.minecraft.core.particles.DustParticleOptions(0xD23A2C, 1.6F);
	private net.minecraft.world.entity.Display.ItemDisplay stamp;

	public BuildingOfficeScene() {
		super("bauamt", ChatFormatting.GRAY);
	}

	@Override
	protected int lineCount() {
		return 3;
	}

	@Override
	protected int actLength() {
		return STAMP + FALL + 60;
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
	protected Vec3 focus(LocalPlayer player) {
		return stamp != null ? stamp.position() : npc.getEyePosition();
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		BlockPos below = player.blockPosition().below();
		if (t == 0) {
			Looks.swing(npc);
			stamp = itemDisplay(level, Looks.prop(Items.PAPER, "rubber_stamp"), npc.getEyePosition());
			((de.derpvpclient.fakedeath.mixin.DisplayAccessor) stamp).fakedeath$setBillboard(net.minecraft.world.entity.Display.BillboardConstraints.CENTER);
			sound(level, npc.position(), SoundEvents.BOOK_PAGE_TURN, SoundSource.NEUTRAL, 1.0F, 0.8F);
		}
		if (stamp != null) {
			Vec3 ground = player.position().subtract(towardPlayer(player).scale(0.9)).add(0.0, 0.5, 0.0);
			Vec3 high = ground.add(0.0, 2.3, 0.0);
			Vec3 at;
			if (t < 10) {
				at = npc.getEyePosition().lerp(high, t / 10.0);
			} else {
				double p = Math.min(1.0, (t - 10) / (double) (STAMP_DOWN - 10));
				at = high.lerp(ground, p * p);
			}
			place(stamp, at, 0.6F + Math.min(1.0F, t / 10.0F) * 1.0F);
			if (t == STAMP_DOWN) {
				sound(level, ground, SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 0.5F, 0.6F);
				particles(level, INK, ground.add(0.0, -0.3, 0.0), 30, 0.5, 0.0);
				cameraShake(0.03F);
			}
			if (t == STAMP) {
				cameraShake(0.0F);
				despawn(level, stamp);
				stamp = null;
			}
		}
		t -= STAMP;
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
