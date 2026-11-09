package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** The furnace next to the player crackles, glows hotter and hotter, smokes, and then it blows up. */
public class FurnaceScene extends Scene {
	private static final int BOOM = 110;
	private final BlockPos found;
	private final List<Display.BlockDisplay> pieces = new ArrayList<>();
	private final List<Vec3> flying = new ArrayList<>();
	private BlockPos furnace;
	private Vec3 center;
	private BlockState look;
	private int flash;

	public FurnaceScene(BlockPos furnace) {
		this.found = furnace;
	}

	@Override
	public int length() {
		return BOOM + 30;
	}

	@Override
	public int photoTick() {
		return BOOM - 15;
	}

	@Override
	public int screenTint() {
		return flash > 0 ? (Math.min(200, flash * 25) << 24) | 0xFFA040 : 0;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.ofen", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			// Always the player's own furnace, none is ever put down.
			furnace = found;
			center = Vec3.atCenterOf(furnace);
			look = level.getBlockState(furnace);
		}
		if (flash > 0) {
			flash--;
		}
		turnTowards(player, center, 0.12F);

		if (tick < BOOM) {
			float p = tick / (float) BOOM;
			int every = Math.max(2, 14 - tick / 9);
			if (tick % every == 0) {
				sound(level, center, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F + p, 0.8F + p);
			}
			particles(level, ParticleTypes.FLAME, center, 1 + (int) (p * 5), 0.4, 0.05);
			particles(level, p > 0.5F ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, center.add(0.0, 0.6, 0.0), 1 + (int) (p * 3), 0.3, 0.05);
			if (tick % 20 == 0) {
				sound(level, center, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 1.0F, 1.0F);
				particles(level, ParticleTypes.LAVA, center, 3, 0.3, 0.1);
			}
			cameraShake(p > 0.6F ? (p - 0.6F) * 0.3F : 0.0F);
		}

		if (tick == BOOM) {
			cameraShake(0.0F);
			flash = 8;
			hideBlock(level, furnace);
			sound(level, center, Looks.EXPLODE, SoundSource.BLOCKS, 2.0F, 0.9F);
			particles(level, ParticleTypes.EXPLOSION_EMITTER, center, 1, 0.0, 0.0);
			for (int i = 0; i < 5; i++) {
				pieces.add(blockDisplay(level, look, center, 3.0F));
				flying.add(new Vec3(random.nextDouble() - 0.5, 0.3 + random.nextDouble() * 0.4, random.nextDouble() - 0.5).scale(0.6));
			}
			setOnFire(player);
			kill(level, player, SoundEvents.PLAYER_HURT_ON_FIRE);
		}
		if (tick > BOOM) {
			int age = tick - BOOM;
			for (int i = 0; i < pieces.size(); i++) {
				Vec3 v = flying.get(i);
				Vec3 at = center.add(v.x * age, v.y * age - 0.04 * age * age, v.z * age);
				place(pieces.get(i), at, new Vector3f(0.35F), new Quaternionf().rotateXYZ(age * 0.3F, age * 0.2F, i));
			}
		}
	}
}
