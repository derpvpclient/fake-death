package de.derpvpclient.fakedeath.scene;

import java.time.Year;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** A villager from the tax office shows up for an audit and settles it with a laser. */
public class TaxAuditScene extends Scene {
	private static final List<Item> VALUABLES = List.of(
		Items.NETHERITE_INGOT, Items.ANCIENT_DEBRIS, Items.DIAMOND_BLOCK, Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT, Items.IRON_INGOT);
	private static final Component NAME = Component.translatable("fakedeath.tax.name").withStyle(ChatFormatting.GOLD);
	private static final DustParticleOptions LASER = new DustParticleOptions(0xFF2020, 1.2F);
	private static final DustParticleOptions LASER_CORE = new DustParticleOptions(0xFFFFFF, 0.6F);
	private static final DustParticleOptions AIM = new DustParticleOptions(0xFF2020, 0.5F);
	// The audit takes its time: about four seconds per line, so every line can be read before the next one.
	private static final int QUESTIONS = 85;
	private static final int FINDING = 170;
	private static final int RECEIPTS = 255;
	private static final int EVASION = 335;
	private static final int PAYMENT = 415;
	private static final int LASER_START = 495;
	private static final int LASER_FULL = 545;
	private static final int ZAP = 620;
	private static final int BYE = 75;
	private static final int OUTRO = BYE + 70;

	private Villager villager;
	private int lastTick;
	private int revealedAt = -1;
	private boolean warned;

	/** Nobody runs away from the tax office: too far away, and the officer is right in front of the player again. */
	private void follow(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
		double distance = villager.position().distanceTo(player.position());

		if (distance > 3.5 && distance <= 6.0) {
			// Walks after the player, briskly.
			Vec3 way = player.position().subtract(villager.position());
			Vec3 step = new Vec3(way.x, 0.0, way.z).normalize().scale(Math.min(0.25, distance - 3.5));
			Vec3 at = villager.position().add(step);
			walk(villager, new Vec3(at.x, floor(level, at.x, at.z, player.getY()), at.z));
		} else if (distance > 6.0) {
			particles(level, ParticleTypes.POOF, villager.position().add(0, 1, 0), 15, 0.4, 0.05);
			Vec3 front = groundSpot(level, player, 3.0, 0.0);
			villager.setPos(front.x, front.y, front.z);
			particles(level, ParticleTypes.POOF, front.add(0, 1, 0), 15, 0.4, 0.05);
			sound(level, front, SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);

			if (!warned) {
				warned = true;
				say(minecraft, NAME, Component.translatable("fakedeath.tax.no_escape"));
			}
		}
	}

	@Override
	public int length() {
		return ZAP + 5;
	}

	@Override
	public int photoTick() {
		return LASER_FULL + 30;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.tax", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		lastTick = tick;

		if (revealedAt >= 0 && tick == revealedAt + BYE && villager != null) {
			say(minecraft, NAME, Component.translatable("fakedeath.tax.bye"));
			sound(level, villager.position(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.0F);
		}

		if (tick == 0) {
			villager = spawn(level, EntityType.VILLAGER, groundSpot(level, player, 3.5, 0.0));
			villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.LIBRARIAN));
			villager.setCustomName(NAME);
			villager.setCustomNameVisible(true);
			particles(level, ParticleTypes.POOF, villager.position().add(0, 1, 0), 20, 0.5, 0.1);
			sound(level, villager.position(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.0F);
			say(minecraft, NAME, Component.translatable("fakedeath.tax.hello"));
		}

		if (villager == null) {
			return;
		}

		if (tick < ZAP && revealedAt < 0) {
			follow(minecraft, level, player);
		}

		Vec3 eyes = villager.getEyePosition();
		face(villager, player.getEyePosition());

		if (tick < 30) {
			turnTowards(player, eyes, 0.15F);
		}

		if (tick == QUESTIONS) {
			sound(level, villager.position(), SoundEvents.BOOK_PAGE_TURN, SoundSource.NEUTRAL, 1.0F, 1.0F);
			say(minecraft, NAME, Component.translatable("fakedeath.tax.questions"));
		}

		if (tick == FINDING - 10 || tick == FINDING - 4) {
			sound(level, villager.position(), SoundEvents.BOOK_PAGE_TURN, SoundSource.NEUTRAL, 1.0F, 0.9F + random.nextFloat() * 0.2F);
		}

		if (tick == FINDING) {
			sound(level, villager.position(), SoundEvents.VILLAGER_WORK_LIBRARIAN, SoundSource.NEUTRAL, 1.0F, 1.0F);
			say(minecraft, NAME, auditFinding(player));
		}

		if (tick == RECEIPTS) {
			sound(level, villager.position(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.3F);
			say(minecraft, NAME, Component.translatable("fakedeath.tax.receipts"));
		}

		if (tick == EVASION) {
			// Shakes its head like a villager that does not want to trade.
			villager.setUnhappyCounter(40);
			sound(level, villager.position(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
			particles(level, ParticleTypes.ANGRY_VILLAGER, eyes.add(0, 0.5, 0), 4, 0.3, 0.0);
			say(minecraft, NAME, Component.translatable("fakedeath.tax.evasion"));
		}

		if (tick == PAYMENT) {
			sound(level, villager.position(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 0.8F);
			particles(level, ParticleTypes.ANGRY_VILLAGER, eyes.add(0, 0.5, 0), 6, 0.3, 0.0);
			say(minecraft, NAME, Component.translatable("fakedeath.tax.payment"));
		}

		if (tick == LASER_START) {
			say(minecraft, NAME, Component.translatable("fakedeath.tax.hold_still"));
			sound(level, villager.position(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.5F, 1.5F);
		}

		if (tick == LASER_FULL) {
			// One beam sound when the laser fires for real. It is a long one, started several times it piled up and
			// droned on into the death screen.
			sound(level, villager.position(), SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 1.2F, 1.0F);
		}

		if (tick >= LASER_START && tick <= ZAP) {
			Vec3 target = player.getEyePosition().add(0, -0.25, 0);

			if (tick < LASER_FULL) {
				// Aiming first: a thin flickering beam.
				if (tick % 3 != 0) {
					line(level, AIM, eyes, target, 0.35);
				}
			} else {
				line(level, LASER, eyes, target, 0.2);
				line(level, LASER_CORE, eyes, target, 0.3);
				particles(level, ParticleTypes.ELECTRIC_SPARK, target, 3, 0.2, 0.3);
			}

			if (tick == LASER_FULL + 15 || tick == LASER_FULL + 35 || tick == LASER_FULL + 55) {
				// Every hit can be heard, not only seen in the hearts.
				hurt(level, player, SoundEvents.PLAYER_HURT_ON_FIRE, 0.25F);
			}
		}

		if (tick == ZAP) {
			stopLaser(minecraft);
			sound(level, player.position(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.5F, 1.4F);
			particles(level, ParticleTypes.ELECTRIC_SPARK, player.getEyePosition(), 40, 0.5, 0.8);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	@Override
	public void onReveal(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
		revealedAt = lastTick + 1;

		if (villager != null) {
			say(minecraft, NAME, Component.translatable("fakedeath.tax.joke"));
			sound(level, villager.position(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
			particles(level, ParticleTypes.HAPPY_VILLAGER, villager.getEyePosition().add(0, 0.4, 0), 6, 0.4, 0.0);
		}
	}

	@Override
	public int outroLength() {
		return OUTRO;
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		stopLaser(Minecraft.getInstance());
		if (villager != null) {
			particles(level, ParticleTypes.POOF, villager.position().add(0, 1, 0), 20, 0.5, 0.1);
			sound(level, villager.position(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.2F);
			villager = null;
		}

		super.end(level, player);
	}

	/** Ends the laser sounds, so they do not drone on over the death screen. */
	private static void stopLaser(Minecraft minecraft) {
		minecraft.getSoundManager().stop(SoundEvents.GUARDIAN_ATTACK.location(), SoundSource.HOSTILE);
		minecraft.getSoundManager().stop(SoundEvents.BEACON_ACTIVATE.location(), SoundSource.HOSTILE);
	}

	/** Picks the most valuable thing the player carries as the reason for the audit. */
	private static Component auditFinding(LocalPlayer player) {
		Inventory inventory = player.getInventory();

		for (Item item : VALUABLES) {
			int count = 0;
			ItemStack example = null;

			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				ItemStack stack = inventory.getItem(slot);

				if (stack.is(item)) {
					count += stack.getCount();
					example = stack;
				}
			}

			if (example != null) {
				return Component.translatable("fakedeath.tax.finding", count, example.getHoverName());
			}
		}

		return Component.translatable("fakedeath.tax.no_return", Year.now().getValue());
	}
}
