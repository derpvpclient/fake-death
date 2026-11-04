package de.derpvpclient.fakedeath.scene;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** The caretaker just mopped: "No running in here!" The player slides over the wet floor straight into an open hole. */
public class CaretakerScene extends NpcScene {
	private static final double SLIDE = 3.0;
	private static final int DEPTH = 8;
	private static final int SLID = 24;
	private static final int FALL = SLID + 22;
	private Vec3 slideDir;

	public CaretakerScene() {
		super("hausmeister", ChatFormatting.DARK_GREEN);
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
		return VillagerProfession.NITWIT;
	}

	@Override
	protected ItemStack held() {
		return new ItemStack(Items.BRUSH);
	}

	@Override
	protected Vec3 focus(LocalPlayer player) {
		return slideDir != null ? player.getEyePosition().add(slideDir.scale(5.0)).add(0.0, -2.0, 0.0) : npc.getEyePosition();
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		if (t == 0) {
			// Away from the caretaker, over the wet floor.
			slideDir = towardPlayer(player);
			BlockPos hole = BlockPos.containing(player.position().add(slideDir.scale(SLIDE)));
			for (int i = 0; i <= DEPTH; i++) {
				hideBlock(level, hole.below(i));
			}
			hideBlock(level, hole.above());
			sound(level, player.position(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.PLAYERS, 1.5F, 1.4F);
		}
		if (t < SLID) {
			float p = t / (float) SLID;
			moveCamera(slideDir.scale(SLIDE * p * p));
			particles(level, ParticleTypes.SPLASH, player.position().add(slideDir.scale(SLIDE * p * p)), 4, 0.3, 0.1);
		} else if (t <= FALL) {
			float p = (t - SLID) / (float) (FALL - SLID);
			moveCamera(slideDir.scale(SLIDE).add(0.0, -p * p * (DEPTH - 1), 0.0));
		}
		if (t == FALL) {
			sound(level, player.position(), SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 1.5F, 1.0F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
		if (t == FALL + 30) {
			speak(minecraft, level, "fakedeath.hausmeister.told");
		}
	}
}
