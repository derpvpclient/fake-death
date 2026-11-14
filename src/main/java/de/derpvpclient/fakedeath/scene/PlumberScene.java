package de.derpvpclient.fakedeath.scene;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** A plumber fixes a leaking pipe next to the player. It bursts, and lava comes out instead of water. */
public class PlumberScene extends NpcScene {
	// The first burn comes as soon as the jet reaches the player, a few ticks after the pipe bursts.
	private static final int[] BURNS = {6, 26, 46};
	private Display.BlockDisplay pipe;
	private Vec3 pipeAt;

	public PlumberScene() {
		super("klempner", ChatFormatting.DARK_AQUA);
	}

	@Override
	protected int lineCount() {
		return 3;
	}

	@Override
	protected int actLength() {
		return 110;
	}

	@Override
	protected ResourceKey<VillagerProfession> profession() {
		return VillagerProfession.MASON;
	}

	@Override
	protected ItemStack held() {
		// His pipe wrench, the mod's own prop.
		return Looks.prop(Items.IRON_INGOT, "pipe_wrench");
	}

	@Override
	protected Vec3 focus(LocalPlayer player) {
		return pipeAt != null ? pipeAt : npc.getEyePosition();
	}

	@Override
	protected void setup(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
		// The pipe stands between him and the player, a little to the side.
		Vec3 dir = player.position().subtract(stand);
		dir = new Vec3(dir.x, 0.0, dir.z).normalize();
		pipeAt = stand.add(dir.scale(1.3)).add(dir.yRot(Mth.HALF_PI).scale(0.9)).add(0.0, 1.5, 0.0);
		pipe = blockDisplay(level, Looks.GRAY_CONCRETE, pipeAt, 4.0F);
		place(pipe, pipeAt, new Vector3f(0.35F, 3.0F, 0.35F), new Quaternionf());
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		if (t == 0) {
			Looks.swing(npc);
			sound(level, pipeAt, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.8F, 1.6F);
			sound(level, pipeAt, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.5F, 0.8F);
		}
		if (t <= BURNS[BURNS.length - 1]) {
			// A jet of lava out of the side of the pipe, right at the player.
			Vec3 to = player.getEyePosition().subtract(pipeAt).normalize();
			for (int i = 0; i < 4; i++) {
				level.addParticle(ParticleTypes.LAVA, pipeAt.x, pipeAt.y, pipeAt.z, to.x * 0.4, to.y * 0.4 + 0.15, to.z * 0.4);
				level.addParticle(ParticleTypes.FLAME, pipeAt.x, pipeAt.y, pipeAt.z, to.x * 0.35, to.y * 0.35, to.z * 0.35);
			}
			if (t % 5 == 0) {
				sound(level, pipeAt, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 1.0F, 0.8F);
			}
			if (t == 6) {
				setOnFire(player);
			}
		}
		for (int i = 0; i < BURNS.length; i++) {
			if (t == BURNS[i]) {
				hit(level, player, SoundEvents.PLAYER_HURT_ON_FIRE, 0.3F, i == BURNS.length - 1);
			}
		}
		if (t == 34) {
			speak(minecraft, level, "fakedeath.klempner.lava");
		}
	}
}
