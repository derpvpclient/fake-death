package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** A bee nest wobbles high above the player, comes loose and lands right on their head. Then the bees come out. */
public class BeeNestScene extends Scene {
	private static final int LOOSE = 55;
	private static final int HIT = LOOSE + 12;
	private static final int SWARM = HIT + 10;
	private static final int BEES = 7;
	private static final int[] STINGS = {SWARM + 25, SWARM + 45, SWARM + 65};
	private final List<Bee> bees = new ArrayList<>();
	private Display.BlockDisplay nest;
	private Vec3 top;

	@Override
	public int length() {
		return STINGS[STINGS.length - 1] + 30;
	}

	@Override
	public int photoTick() {
		return SWARM + 20;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.bienennest", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			top = player.getEyePosition().add(horizontalLook(player).scale(0.6)).add(0.0, 5.5, 0.0);
			nest = blockDisplay(level, Blocks.BEE_NEST.defaultBlockState(), top, 4.0F);
		}
		if (nest == null) {
			return;
		}
		if (tick < LOOSE) {
			place(nest, top, new Vector3f(1.0F), new Quaternionf().rotateZ((float) Math.sin(tick * 0.6) * (tick / 400.0F)));
			turnTowards(player, top, 0.1F);
			if (tick % 15 == 0) {
				sound(level, top, SoundEvents.BEEHIVE_WORK, SoundSource.NEUTRAL, 1.0F, 1.0F);
				particles(level, ParticleTypes.DRIPPING_HONEY, top.add(0.0, -0.5, 0.0), 2, 0.3, 0.0);
			}
		} else if (tick <= HIT) {
			float p = (tick - LOOSE) / (float) (HIT - LOOSE);
			Vec3 at = top.lerp(player.getEyePosition().add(0.0, 0.4, 0.0), p * p);
			place(nest, at, new Vector3f(1.0F), new Quaternionf().rotateZ(p * 0.6F));
			turnTowards(player, at, 0.4F);
		}
		if (tick == HIT) {
			sound(level, player.position(), SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.5F, 0.8F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BEE_NEST.defaultBlockState()), player.getEyePosition(), 25, 0.5, 0.2);
			particles(level, ParticleTypes.FALLING_HONEY, player.getEyePosition(), 15, 0.5, 0.0);
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.3F);
			place(nest, player.position().add(horizontalLook(player).scale(0.9)).add(0.0, 0.5, 0.0), new Vector3f(1.0F), new Quaternionf().rotateZ(0.4F));
		}
		if (tick == SWARM) {
			for (int i = 0; i < BEES; i++) {
				Bee bee = spawn(level, EntityType.BEE, nest.position());
				bee.noPhysics = true;
				bee.setNoGravity(true);
				bees.add(bee);
			}
			sound(level, player.position(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.HOSTILE, 1.5F, 1.0F);
		}
		for (int i = 0; i < bees.size(); i++) {
			// Circling around the head, closer and closer.
			double radius = Math.max(0.7, 2.5 - (tick - SWARM) / 50.0);
			Vec3 at = orbit(player.getEyePosition(), radius, tick * 0.25 + i * Math.PI * 2 / BEES, Math.sin(tick * 0.3 + i) * 0.5);
			bees.get(i).setPos(at.x, at.y, at.z);
			face(bees.get(i), player.getEyePosition());
		}
		if (tick > SWARM) {
			turnTowards(player, bees.get((tick / 20) % BEES).position(), 0.2F);
			if (tick % 30 == 0) {
				sound(level, player.position(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.HOSTILE, 1.2F, 1.0F);
			}
		}
		for (int i = 0; i < STINGS.length; i++) {
			if (tick == STINGS[i]) {
				sound(level, player.position(), SoundEvents.BEE_STING, SoundSource.HOSTILE, 1.2F, 1.0F);
				hit(level, player, SoundEvents.PLAYER_HURT, 0.25F, i == STINGS.length - 1);
			}
		}
	}
}
