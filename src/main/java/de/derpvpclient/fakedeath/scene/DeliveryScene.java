package de.derpvpclient.fakedeath.scene;

import com.mojang.math.Transformation;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A delivery guy rushes over with a package, stands right in front of the player and still cannot
 * find them in his system. So he throws the package over the fence, high up, right onto their head.
 */
public class DeliveryScene extends Scene {
	private static final Component NAME = Component.translatable("fakedeath.delivery.name").withStyle(ChatFormatting.YELLOW);
	private static final BlockState PACKAGE = Blocks.BARREL.defaultBlockState();
	private static final float BOX = 0.55F;
	private static final int ARRIVE = 40;
	private static final int HELLO = 42;
	private static final int NOT_HOME = 125;
	private static final int SYSTEM = 210;
	private static final int FENCE = 295;
	private static final int THROW = 318;
	private static final int FLIGHT = 40;
	private static final int IMPACT = THROW + FLIGHT;
	private static final int DELIVERED = IMPACT + 8;
	private static final int DEATH = DELIVERED + 45;
	private static final double ARC = 9.0;

	private Villager courier;
	private Display.BlockDisplay box;
	private Vec3 start;
	private Vec3 stand;
	private Vec3 throwFrom;
	private Vec3 landed;

	@Override
	public int length() {
		return DEATH;
	}

	@Override
	public int photoTick() {
		return SYSTEM + 10;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.delivery", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			start = groundSpot(level, player, 10.0, (random.nextDouble() - 0.5) * 60.0);
			Vec3 toward = player.position().subtract(start);
			Vec3 flat = new Vec3(toward.x, 0.0, toward.z).normalize();
			stand = player.position().subtract(flat.scale(2.8));
			courier = spawn(level, EntityType.VILLAGER, start);
			courier.setCustomName(NAME);
			courier.setCustomNameVisible(true);
			courier.noPhysics = true;
			box = blockDisplay(level, PACKAGE, start, 2.0F);
		}

		if (courier == null) {
			return;
		}

		// Rushing over, he is always in a hurry.
		Vec3 pos = tick < ARRIVE ? start.lerp(stand, tick / (double) ARRIVE) : stand;
		pos = new Vec3(pos.x, floor(level, pos.x, pos.z, player.getY()), pos.z);
		walk(courier, pos);
		face(courier, player.getEyePosition());

		if (tick < ARRIVE && tick % 5 == 0) {
			sound(level, pos, SoundEvents.GRASS_STEP, SoundSource.NEUTRAL, 0.8F, 1.2F);
		}

		if (tick < IMPACT) {
			turnTowards(player, tick < THROW ? courier.getEyePosition() : box.position(), tick < THROW ? 0.12F : 0.3F);
		}

		if (tick == HELLO) {
			sound(level, pos, SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.2F);
			say(minecraft, NAME, Component.translatable("fakedeath.delivery.hello"));
		}

		if (tick == NOT_HOME - 20 || tick == NOT_HOME - 12) {
			// Scanning the package.
			sound(level, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 0.8F, 2.0F);
		}

		if (tick == NOT_HOME) {
			sound(level, pos, SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
			say(minecraft, NAME, Component.translatable("fakedeath.delivery.not_home"));
		}

		if (tick == SYSTEM) {
			courier.setUnhappyCounter(40);
			sound(level, pos, SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 0.9F);
			say(minecraft, NAME, Component.translatable("fakedeath.delivery.system"));
		}

		if (tick == FENCE) {
			sound(level, pos, SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.0F);
			say(minecraft, NAME, Component.translatable("fakedeath.delivery.fence"));
		}

		moveBox(level, player, pos, tick);

		if (tick == DELIVERED) {
			sound(level, pos, SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.2F);
			say(minecraft, NAME, Component.translatable("fakedeath.delivery.delivered"));
		}
	}

	private void moveBox(ClientLevel level, LocalPlayer player, Vec3 courierPos, int tick) {
		Vec3 boxPos;
		float spin = 0.0F;

		if (tick < THROW) {
			// Carried in front of the belly.
			Vec3 toward = player.position().subtract(courierPos);
			Vec3 flat = new Vec3(toward.x, 0.0, toward.z).normalize();
			boxPos = courierPos.add(flat.scale(0.5)).add(0.0, 0.85, 0.0);
			throwFrom = boxPos;
		} else if (tick < IMPACT) {
			if (tick == THROW) {
				sound(level, throwFrom, SoundEvents.WITCH_THROW, SoundSource.NEUTRAL, 1.0F, 0.6F);
			}
			float t = (tick - THROW) / (float) FLIGHT;
			boxPos = throwFrom.lerp(player.getEyePosition().add(0.0, 0.3, 0.0), t).add(0.0, Mth.sin(Mth.PI * t) * ARC, 0.0);
			spin = (tick - THROW) * 0.25F;
		} else {
			if (landed == null) {
				landed = player.position().add(horizontalLook(player).scale(0.8));
				landed = new Vec3(landed.x, floor(level, landed.x, landed.z, player.getY()) + BOX / 2.0, landed.z);
				sound(level, player.position(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 1.2F, 0.8F);
				sound(level, player.position(), SoundEvents.WOOD_BREAK, SoundSource.NEUTRAL, 1.5F, 0.8F);
				particles(level, new BlockParticleOption(ParticleTypes.BLOCK, PACKAGE), player.getEyePosition(), 20, 0.4, 0.2);
				kill(level, player, SoundEvents.PLAYER_HURT);
			}
			boxPos = landed;
			spin = 0.3F;
		}

		box.setPos(boxPos.x, boxPos.y, boxPos.z);
		Quaternionf rotation = new Quaternionf().rotateXYZ(spin, spin * 0.6F, 0.0F);
		transform(box, new Transformation(new Vector3f(-BOX / 2.0F).rotate(rotation), rotation, new Vector3f(BOX), new Quaternionf()));
	}
}
