package de.derpvpclient.fakedeath.scene;

import com.mojang.math.Transformation;
import de.derpvpclient.fakedeath.mixin.DisplayAccessor;
import de.derpvpclient.fakedeath.mixin.ItemDisplayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The item in the player's hand starts to hum and slips out of it, the hand is empty. The item floats up
 * above their head and grows and grows, as big as the room allows. It hangs there, wobbles and creaks, slips
 * a little, and then it falls with full weight right onto them.
 */
public class GiantItemScene extends Scene {
	private static final int OUT = 30;
	private static final int GROWN = 110;
	private static final int SLIP = 140;
	private static final int DROP = 155;
	private static final double GRAVITY = 0.11;
	private static final int DEATH = DROP + 60;
	private static final float START_SIZE = 0.5F;
	private static final int BOUNCE = 8;

	private final ItemStack stack;
	private final InteractionHand hand;
	private Display.ItemDisplay display;
	private Vec3 handPos;
	private Vec3 above;
	private float size = 7.0F;
	private double hover = 6.5;
	private double fallSpeed;
	private Vec3 fallPos;
	private int impact = -1;
	private int tint;

	public GiantItemScene(LocalPlayer player) {
		if (!player.getMainHandItem().isEmpty()) {
			hand = InteractionHand.MAIN_HAND;
		} else if (!player.getOffhandItem().isEmpty()) {
			hand = InteractionHand.OFF_HAND;
		} else {
			hand = null;
		}
		stack = hand != null ? player.getItemInHand(hand).copyWithCount(1) : new ItemStack(Items.COOKIE);
	}

	/** The item the player holds, or the other hand's. */
	public static ItemStack heldBy(LocalPlayer player) {
		return player.getMainHandItem().isEmpty() ? player.getOffhandItem() : player.getMainHandItem();
	}

	@Override
	public int length() {
		return DEATH;
	}

	@Override
	public int photoTick() {
		return GROWN + 15;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.giantitem", player.getDisplayName(), stack.getDisplayName());
	}

	@Override
	public int screenTint() {
		return tint;
	}

	@Override
	public InteractionHand hiddenHand() {
		return display != null || impact >= 0 ? hand : null;
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			fitToRoom(level, player);
		}

		if (tick < OUT) {
			// The item in the hand hums and sparkles.
			Vec3 at = handPosition(player);
			if (tick % 6 == 0) {
				sound(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.6F + tick / 40.0F);
			}
			particles(level, ParticleTypes.ENCHANT, at, 3, 0.3, 0.5);
			turnTowards(player, at, 0.12F);
			return;
		}

		if (tick == OUT) {
			handPos = handPosition(player);
			above = player.getEyePosition().add(0.0, hover, 0.0);
			display = spawn(level, EntityType.ITEM_DISPLAY, handPos);
			((ItemDisplayAccessor) display).fakedeath$setItemStack(stack);
			((ItemDisplayAccessor) display).fakedeath$setItemTransform(ItemDisplayContext.FIXED);
			// Always turned to the camera, so a flat item like a sword is never seen edge-on.
			((DisplayAccessor) display).fakedeath$setBillboard(Display.BillboardConstraints.CENTER);
			((DisplayAccessor) display).fakedeath$setWidth(size * 2.0F);
			((DisplayAccessor) display).fakedeath$setHeight(size * 2.0F);
			((DisplayAccessor) display).fakedeath$setInterpolationDuration(1);
			sound(level, handPos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 0.5F);
		}

		if (display == null) {
			return;
		}

		Vec3 pos;
		float scale = size;
		float roll = 0.0F;

		if (tick < GROWN) {
			// Up above the head, growing bigger and bigger on the way, turning slowly.
			float t = (tick - OUT) / (float) (GROWN - OUT);
			float eased = t * t * (3.0F - 2.0F * t);
			pos = handPos.lerp(above, eased);
			scale = Mth.lerp(eased, START_SIZE, size);
			roll = (1.0F - eased) * 360.0F * 2.0F;

			if (tick % 8 == 0) {
				sound(level, pos, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.5F, 0.5F + t);
			}
			turnTowards(player, pos, 0.12F);
		} else if (tick < DROP) {
			// It hangs there, sways and creaks, sheds a little dust, and slips once.
			double slip = tick >= SLIP ? Math.min(1.0, (tick - SLIP) / 3.0) * 0.6 : 0.0;
			pos = above.add(0.0, Mth.sin(tick * 0.2F) * 0.1 - slip, 0.0);
			roll = Mth.sin((tick - GROWN) * 0.45F) * (tick >= SLIP ? 14.0F : 6.0F);

			if (tick == GROWN + 5 || tick == GROWN + 22 || tick == SLIP) {
				sound(level, pos, SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, tick == SLIP ? 1.5F : 0.8F, 0.5F);
			}
			if (tick % 3 == 0) {
				particles(level, ParticleTypes.WHITE_ASH, pos.add(0.0, -scale * 0.4, 0.0), 6, scale * 0.3, 0.0);
			}
			turnTowards(player, pos, 0.12F);
		} else if (impact < 0) {
			// And down it comes, faster and faster.
			if (tick == DROP) {
				fallPos = above.add(0.0, -0.6, 0.0);
				sound(level, fallPos, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 3.0F, 0.4F);
			}
			fallSpeed += GRAVITY;
			Vec3 target = player.getEyePosition().add(0.0, 0.5, 0.0);
			fallPos = fallPos.add(0.0, -fallSpeed, 0.0);
			roll = (tick - DROP) * 4.0F;

			if (fallPos.y <= target.y) {
				fallPos = target;
				impact = tick;
				crash(level, player);
			}
			pos = fallPos;
			turnTowards(player, pos, 0.5F);
		} else {
			// Lying on top of the player, after one heavy bounce.
			int since = tick - impact;
			double bounce = since < BOUNCE ? Mth.sin(Mth.PI * since / BOUNCE) * 0.35 : 0.0;
			pos = fallPos.add(0.0, bounce, 0.0);
			roll = 8.0F;
		}

		display.setPos(pos.x, pos.y, pos.z);
		Quaternionf rotation = new Quaternionf().rotateZ((float) Math.toRadians(roll));
		transform(display, new Transformation(new Vector3f(), rotation, new Vector3f(scale), new Quaternionf()));
	}

	/** Big under the open sky, smaller in a room, so it never vanishes in the ceiling. */
	private void fitToRoom(ClientLevel level, LocalPlayer player) {
		BlockPos head = BlockPos.containing(player.getEyePosition());
		int free = 0;
		while (free < 9 && level.getBlockState(head.above(free + 1)).getCollisionShape(level, head.above(free + 1)).isEmpty()) {
			free++;
		}
		size = Mth.clamp(free * 0.85F, 2.5F, 7.0F);
		hover = Math.max(1.6, Math.min(6.5, free - size * 0.45));
	}

	private void crash(ClientLevel level, LocalPlayer player) {
		Vec3 feet = player.position();
		sound(level, feet, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 2.5F, 0.5F);
		if (stack.getItem() instanceof BlockItem block) {
			sound(level, feet, block.getBlock().defaultBlockState().getSoundType().getBreakSound(), SoundSource.PLAYERS, 2.5F, 0.6F);
		} else {
			sound(level, feet, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 2.0F, 0.6F);
		}

		particles(level, new ItemParticleOption(ParticleTypes.ITEM, stack), player.getEyePosition(), 40, 1.5, 0.35);
		particles(level, new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(player.blockPosition().below())), feet.add(0.0, 0.2, 0.0), 40, 1.8, 0.3);
		particles(level, ParticleTypes.POOF, feet, 25, 2.0, 0.1);

		// Squashed: the view drops to the ground under the weight, and it gets dark.
		moveCamera(new Vec3(0.0, -1.1, 0.0));
		tint = 170 << 24;
		kill(level, player, SoundEvents.PLAYER_HURT);
	}

	private static Vec3 handPosition(LocalPlayer player) {
		Vec3 look = horizontalLook(player);
		Vec3 right = new Vec3(-look.z, 0.0, look.x);
		return player.getEyePosition().add(look.scale(0.6)).add(right.scale(0.35)).add(0.0, -0.35, 0.0);
	}
}
