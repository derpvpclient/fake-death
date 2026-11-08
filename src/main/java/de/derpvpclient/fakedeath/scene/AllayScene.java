package de.derpvpclient.fakedeath.scene;

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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * An allay brings a present: a flower. Then another one: an iron ingot, right on the head. Then it comes back, very
 * slowly, straining under something heavy. An anvil.
 */
public class AllayScene extends Scene {
	private static final int IN = 25;
	private static final int HOVER = 12;
	private static final int OUT = 20;
	private static final int[] TRIPS = {0, IN + HOVER + OUT, (IN + HOVER + OUT) * 2};
	/** The last trip is slower, the anvil is heavy. */
	private static final int HEAVY_IN = 45;
	private static final int FALL = 7;

	private LivingEntity allay;
	private Vec3 away;
	private Display gift;
	private Vec3 giftFrom;
	private int giftDrop = -1;
	private int done = -1;

	@Override
	public int length() {
		return done >= 0 ? done + 25 : TRIPS[2] + HEAVY_IN + HOVER + FALL + 40;
	}

	@Override
	public int photoTick() {
		return TRIPS[2] + HEAVY_IN;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.allay", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			away = groundSpot(level, player, 12.0, 25.0).add(0.0, 4.0, 0.0);
			allay = spawn(level, EntityTypes.ALLAY, away);
			allay.setNoGravity(true);
			allay.noPhysics = true;
		}
		if (allay == null) {
			return;
		}

		Vec3 above = player.getEyePosition().add(horizontalLook(player).scale(2.2)).add(0.0, 1.6, 0.0);
		int trip = tick >= TRIPS[2] ? 2 : tick >= TRIPS[1] ? 1 : 0;
		int t = tick - TRIPS[trip];
		int in = trip == 2 ? HEAVY_IN : IN;

		if (t == 0) {
			// The last present is gone before the next one comes.
			if (gift != null) {
				despawn(level, gift);
				gift = null;
			}
			giftDrop = -1;
			ItemStack stack = trip == 0 ? new ItemStack(Items.POPPY) : trip == 1 ? new ItemStack(Items.IRON_INGOT) : ItemStack.EMPTY;
			allay.setItemSlot(EquipmentSlot.MAINHAND, stack);
			if (trip == 2) {
				gift = blockDisplay(level, Blocks.ANVIL.defaultBlockState(), allay.position(), 3.0F);
				Looks.glow(gift);
			}
			sound(level, allay.position(), SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundSource.NEUTRAL, 1.2F, 1.0F);
		}

		Vec3 at;
		if (t < in) {
			float p = t / (float) in;
			at = away.lerp(above, p * (2 - p));
			if (trip == 2) {
				// Struggling, sagging, wobbling all the way.
				at = at.add(Math.sin(t * 0.5) * 0.15, -Math.abs(Math.sin(t * 0.25)) * 0.4, 0.0);
				if (t % 10 == 0) {
					sound(level, at, SoundEvents.ALLAY_HURT, SoundSource.NEUTRAL, 1.0F, 0.7F);
				}
			}
		} else if (t < in + HOVER) {
			at = above.add(0.0, Mth.sin(t * 0.4F) * 0.1, 0.0);
		} else {
			float p = Math.min(1.0F, (t - in - HOVER) / (float) OUT);
			at = above.lerp(away, p * p);
		}
		allay.setPos(at.x, at.y, at.z);
		face(allay, trip == 2 || t < in + HOVER ? player.getEyePosition() : at.add(away.subtract(above)));
		if (gift instanceof Display.BlockDisplay && giftDrop < 0) {
			place(gift, at.add(0.0, -0.55, 0.0), new Vector3f(0.6F), new Quaternionf().rotateY(t * 0.03F));
		}
		if (tick % 6 == 0) {
			particles(level, ParticleTypes.NOTE, at.add(0.0, 0.5, 0.0), 1, 0.2, 0.0);
		}

		if (t == in + HOVER - 1) {
			// Let go, with a happy chirp.
			sound(level, at, SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.NEUTRAL, 1.5F, 1.0F);
			particles(level, ParticleTypes.HEART, at, 3, 0.3, 0.0);
			if (trip < 2) {
				ItemStack stack = trip == 0 ? new ItemStack(Items.POPPY) : new ItemStack(Items.IRON_INGOT);
				allay.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
				if (gift != null) {
					despawn(level, gift);
				}
				gift = itemDisplay(level, stack, at.add(0.0, -0.3, 0.0));
				((de.derpvpclient.fakedeath.mixin.DisplayAccessor) gift).fakedeath$setBillboard(Display.BillboardConstraints.CENTER);
			}
			giftFrom = at.add(0.0, trip == 2 ? -0.55 : -0.3, 0.0);
			giftDrop = tick;
		}

		if (giftDrop >= 0 && gift != null) {
			int f = tick - giftDrop;
			// The flower floats down in front, the ingot and the anvil land on the head.
			Vec3 target = trip == 0 && f < 40 ? player.position().add(horizontalLook(player).scale(0.9)).add(0.0, 0.2, 0.0) : player.getEyePosition().add(0.0, 0.35, 0.0);
			int fall = trip == 0 ? 20 : FALL;
			float p = Math.min(1.0F, f / (float) fall);
			Vec3 drop = giftFrom.lerp(target, p * p);
			if (trip == 1 && f > fall) {
				// The ingot bounces off the head and lands in the grass in front.
				float b = Math.min(1.0F, (f - fall) / 10.0F);
				Vec3 grass = player.position().add(horizontalLook(player).scale(1.3)).add(0.0, 0.15, 0.0);
				drop = target.lerp(grass, b).add(0.0, Mth.sin(b * Mth.PI) * 0.5, 0.0);
			}
			if (gift instanceof Display.BlockDisplay) {
				place(gift, drop, new Vector3f(0.6F), new Quaternionf().rotateZ(f * 0.1F));
			} else {
				place(gift, drop, 0.5F);
			}
			if (f == fall) {
				if (trip == 1) {
					sound(level, player.position(), SoundEvents.IRON_GOLEM_HURT, SoundSource.NEUTRAL, 1.0F, 1.6F);
					particles(level, ParticleTypes.CRIT, player.getEyePosition(), 15, 0.4, 0.2);
					hurt(level, player, SoundEvents.PLAYER_HURT, 0.3F);
				}
				if (trip == 2) {
					sound(level, player.position(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 3.0F, 0.8F);
					particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ANVIL.defaultBlockState()), player.getEyePosition(), 25, 0.5, 0.3);
					kill(level, player, SoundEvents.PLAYER_HURT);
					done = tick;
				}
			}
			if (trip < 2 && f > fall + 15) {
				despawn(level, gift);
				gift = null;
				giftDrop = -1;
			}
		}
		if (done >= 0 && tick == done + 15) {
			sound(level, allay.position(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.NEUTRAL, 1.2F, 1.2F);
			particles(level, ParticleTypes.HEART, allay.position(), 5, 0.4, 0.0);
		}

		// Following the gift down, but never staring at it from a hand's width away.
		boolean watchGift = gift != null && giftDrop >= 0 && gift.position().distanceTo(player.getEyePosition()) > 1.2;
		turnTowards(player, watchGift ? gift.position() : allay.position(), 0.2F);
	}
}
