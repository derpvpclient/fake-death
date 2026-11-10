package de.derpvpclient.fakedeath.scene;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The book on the enchanting table flips its pages faster and faster, then it enchants the player with the Curse of
 * Vanishing. The curse does what it says: the player fades away.
 */
public class EnchantingTableScene extends Scene {
	private static final int CURSE = 56;
	private static final int FADE = CURSE + 34;
	private static final int GONE = FADE + 40;
	private final BlockPos found;
	private BlockPos table;
	private Display.ItemDisplay book;
	private float tint;

	public EnchantingTableScene(BlockPos table) {
		this.found = table;
	}

	@Override
	public int length() {
		return GONE + 25;
	}

	@Override
	public int photoTick() {
		return CURSE - 16;
	}

	@Override
	public int screenTint() {
		return tint > 0.0F ? ((int) (Math.min(tint, 1.0F) * 225.0F) << 24) | 0xEDE3FF : 0;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.verzauberungstisch", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			table = found;
			if (table == null) {
				// Asked for by name with no table around: one is put up in front, only on this client.
				table = BlockPos.containing(groundSpot(level, player, 2.0, 0.0));
				placeBlock(level, table, Blocks.ENCHANTING_TABLE.defaultBlockState());
			}
		}

		Vec3 top = Vec3.atBottomCenterOf(table).add(0.0, 1.15, 0.0);
		Vec3 head = player.getEyePosition();

		if (tick < FADE) {
			turnTowards(player, top, 0.15F);
		}

		if (tick < CURSE) {
			// The real book of the table: wide open, and one page after the other as fast as it goes.
			if (level.getBlockEntity(table) instanceof EnchantingTableBlockEntity pages) {
				pages.open = 1.0F;
				pages.flipT += 0.9F;
			}

			int every = tick < 20 ? 5 : tick < 40 ? 3 : 2;
			if (tick % every == 0) {
				sound(level, top, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.4F, 0.8F + tick / (float) CURSE * 0.9F);
			}
			particles(level, ParticleTypes.ENCHANT, top.add(0.0, 0.7, 0.0), 2 + tick / 6, 0.5, 1.0);
			if (tick > 30 && tick % 4 == 0) {
				particles(level, ParticleTypes.WITCH, top, 3, 0.3, 0.05);
			}
		}

		if (tick == CURSE) {
			sound(level, head, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 2.0F, 0.6F);
			line(level, ParticleTypes.WITCH, top, head.add(0.0, -0.3, 0.0), 0.2);
			particles(level, ParticleTypes.ENCHANT, head, 60, 0.6, 1.5);
			say(minecraft, Component.translatable("block.minecraft.enchanting_table").withStyle(ChatFormatting.LIGHT_PURPLE),
				Component.translatable("enchantment.minecraft.vanishing_curse").withStyle(ChatFormatting.RED));
			book = itemDisplay(level, new ItemStack(Items.ENCHANTED_BOOK), top);
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.2F);
		}

		if (book != null) {
			// The curse as a book: up from the table, in front of the face, and then right into it.
			int t = tick - CURSE;
			Vec3 look = horizontalLook(player);
			Vec3 front = head.add(look.scale(1.1)).add(0.0, -0.05 + Math.sin(tick * 0.3) * 0.04, 0.0);
			float in = Math.min(1.0F, t / 14.0F);
			Vec3 at = top.lerp(front, in * in * (3.0F - 2.0F * in));
			float size = 0.35F + in * 0.45F;

			if (t > 26) {
				float p = Math.min(1.0F, (t - 26) / 7.0F);
				at = front.lerp(head, p);
				size += p * 0.8F;
			}

			float yaw = (float) Math.atan2(head.x - at.x, head.z - at.z);
			place(book, at, new Vector3f(size), new Quaternionf().rotateY(yaw));
			if (tick % 2 == 0) {
				particles(level, ParticleTypes.ENCHANT, at, 3, 0.2, 0.6);
			}

			if (tick == FADE) {
				despawn(level, book);
				book = null;
				sound(level, head, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 2.0F, 1.4F);
				particles(level, ParticleTypes.POOF, head, 25, 0.4, 0.05);
				addEffect(player, MobEffects.INVISIBILITY, GONE - FADE + 40);
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.2F);
			}
		}

		if (tick > FADE) {
			// Vanishing: everything turns pale, and a little more of the player goes up in smoke.
			tint = (tick - FADE) / (float) (GONE - FADE);
			if (tick % 2 == 0) {
				particles(level, ParticleTypes.POOF, player.position().add(0.0, 0.9, 0.0), 3, 0.4, 0.02);
			}
			if (tick == FADE + 14 || tick == FADE + 27) {
				sound(level, head, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.5F, 0.5F);
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.2F);
			}
		}

		if (tick == GONE) {
			sound(level, head, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.5F, 0.4F);
			particles(level, ParticleTypes.POOF, player.position().add(0.0, 0.9, 0.0), 50, 0.5, 0.1);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}
}
