package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The player sinks into powder snow up to the chest. A snow golem slides over, looks at the stuck player for a
 * while, takes its own pumpkin off and puts it on the player's head. Then the snow takes the rest.
 */
public class SnowmanScene extends Scene {
	private static final int STUCK = 26;
	private static final int ARRIVE = 62;
	private static final int TAKE_OFF = 74;
	private static final int PUT_ON = 94;
	private static final int UNDER = PUT_ON + 44;
	private final BlockPos found;
	private SnowGolem golem;
	private Vec3 from;
	private Vec3 near;
	private Display.ItemDisplay pumpkin;
	private ItemStack hat;
	private boolean wearing;

	public SnowmanScene(BlockPos powderSnow) {
		this.found = powderSnow;
	}

	@Override
	public int length() {
		return UNDER + 20;
	}

	@Override
	public int photoTick() {
		return TAKE_OFF + 12;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.schneemann", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		Vec3 head = player.getEyePosition();

		if (tick == 0) {
			// The snow under the feet gives way. With no powder snow around, some is put there, only on this client.
			placeBlock(level, player.blockPosition(), Blocks.POWDER_SNOW.defaultBlockState());
			if (found == null) {
				for (int[] side : new int[][] {{1, 0}, {0, 1}, {-1, 0}, {0, -1}}) {
					BlockPos beside = player.blockPosition().offset(side[0], -1, side[1]);
					if (!level.getBlockState(beside).isAir()) {
						replaceBlock(level, beside, Blocks.POWDER_SNOW.defaultBlockState());
					}
				}
			}

			from = groundSpot(level, player, 7.0, 25.0);
			near = groundSpot(level, player, 1.7, 0.0);
			golem = spawn(level, EntityType.SNOW_GOLEM, from);
			golem.noPhysics = true;
			sound(level, player.position(), SoundEvents.POWDER_SNOW_BREAK, SoundSource.BLOCKS, 1.5F, 0.8F);
		}

		// Sinking: chest deep at first, and after the pumpkin all the way.
		double deep = tick < STUCK ? 0.5 * tick / STUCK : tick < PUT_ON ? 0.5 : 0.5 + 1.0 * Math.min(1.0, (tick - PUT_ON) / (double) (UNDER - PUT_ON));
		moveCamera(new Vec3(0.0, -deep, 0.0));
		chill(player, Math.min(1.0F, tick / (float) PUT_ON));
		if (tick % 5 == 0) {
			particles(level, ParticleTypes.SNOWFLAKE, player.position().add(0.0, 0.9, 0.0), 4, 0.5, 0.02);
		}
		if (tick < STUCK && tick % 6 == 0) {
			sound(level, player.position(), SoundEvents.POWDER_SNOW_STEP, SoundSource.BLOCKS, 1.5F, 0.8F);
		}

		Vec3 at;
		if (tick < ARRIVE) {
			float p = Math.max(0.0F, (tick - 8) / (float) (ARRIVE - 8));
			Vec3 flat = from.lerp(near, p * p * (3.0F - 2.0F * p));
			at = new Vec3(flat.x, floor(level, flat.x, flat.z, player.getY()), flat.z);
			if (tick % 8 == 0) {
				sound(level, at, SoundEvents.SNOW_STEP, SoundSource.NEUTRAL, 1.2F, 1.0F);
				particles(level, ParticleTypes.ITEM_SNOWBALL, at, 3, 0.2, 0.02);
			}
		} else {
			at = new Vec3(near.x, floor(level, near.x, near.z, player.getY()), near.z);
		}

		walk(golem, at);
		// It looks down at the stuck player and tilts its head, left, right.
		face(golem, head.add(0.0, -deep, 0.0));
		if (tick >= ARRIVE && tick < TAKE_OFF) {
			float tilt = (float) Math.sin((tick - ARRIVE) * 0.55) * 22.0F;
			golem.setYHeadRot(golem.getYRot() + tilt);
		}
		turnTowards(player, golem.position().add(0.0, 1.9 + deep, 0.0), 0.16F);

		if (tick == ARRIVE) {
			sound(level, at, SoundEvents.SNOW_GOLEM_AMBIENT, SoundSource.NEUTRAL, 1.5F, 1.0F);
		}

		if (tick == TAKE_OFF) {
			golem.setPumpkin(false);
			hat = new ItemStack(Items.CARVED_PUMPKIN);
			pumpkin = itemDisplay(level, hat.copy(), at.add(0.0, 1.75, 0.0));
			sound(level, at, SoundEvents.SNOW_GOLEM_SHEAR, SoundSource.NEUTRAL, 1.5F, 0.8F);
		}

		if (pumpkin != null) {
			// Up off its own head, a moment in the air, then down over the player's.
			float p = (tick - TAKE_OFF) / (float) (PUT_ON - TAKE_OFF);
			Vec3 off = at.add(0.0, 1.75, 0.0);
			Vec3 target = head.add(0.0, -deep + 0.1, 0.0);
			Vec3 spot = off.lerp(target, p * p).add(0.0, Math.sin(p * Math.PI) * 0.9, 0.0);
			float yaw = (float) Math.atan2(target.x - off.x, target.z - off.z);
			place(pumpkin, spot, new Vector3f(0.7F + p * 0.5F), new Quaternionf().rotateY(yaw));
		}

		if (tick == PUT_ON) {
			despawn(level, pumpkin);
			pumpkin = null;
			// The real pumpkin view: through the eye holes.
			hat = player.getItemBySlot(EquipmentSlot.HEAD).copy();
			player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
			wearing = true;
			sound(level, head, SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 1.5F, 0.6F);
			sound(level, at, SoundEvents.SNOW_GOLEM_AMBIENT, SoundSource.NEUTRAL, 1.5F, 1.3F);
			hurt(level, player, SoundEvents.PLAYER_HURT_FREEZE, 0.3F);
		}

		if (tick > PUT_ON && tick < UNDER && (tick - PUT_ON) % 13 == 0) {
			// A pat on the pumpkin, and down it goes a little more.
			Looks.swing(golem);
			sound(level, head, SoundEvents.SNOW_PLACE, SoundSource.NEUTRAL, 1.5F, 0.7F);
			hurt(level, player, SoundEvents.PLAYER_HURT_FREEZE, 0.2F);
		}

		if (tick == UNDER) {
			sound(level, head, SoundEvents.POWDER_SNOW_PLACE, SoundSource.BLOCKS, 2.0F, 0.6F);
			particles(level, ParticleTypes.SNOWFLAKE, player.position().add(0.0, 0.4, 0.0), 40, 0.5, 0.05);
			kill(level, player, SoundEvents.PLAYER_HURT_FREEZE);
		}
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		if (wearing) {
			player.setItemSlot(EquipmentSlot.HEAD, hat);
			wearing = false;
		}
		moveCamera(Vec3.ZERO);
		super.end(level, player);
	}
}
