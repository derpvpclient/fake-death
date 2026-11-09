package de.derpvpclient.fakedeath.scene;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** A chef: "Taste this!" A spoonful of his secret recipe, the player turns green, seasoned with spider eye. */
public class ChefScene extends NpcScene {
	private static final int SPOON = 16;
	private static final int[] SICK = {50, 80};
	private static final int DEATH = 110;
	private Display.ItemDisplay spoon;

	public ChefScene() {
		super("koch", ChatFormatting.GOLD);
	}

	@Override
	protected int lineCount() {
		return 2;
	}

	@Override
	protected int actLength() {
		return DEATH + 50;
	}

	@Override
	protected double standDistance() {
		return 1.6;
	}

	@Override
	protected VillagerProfession profession() {
		return VillagerProfession.BUTCHER;
	}

	@Override
	protected ItemStack held() {
		// His soup ladle with the green soup, the mod's own prop.
		return Looks.prop(Items.SUSPICIOUS_STEW, "soup_ladle");
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		if (t == 0) {
			// He holds his ladle of soup right up to the player's mouth, it leaves his hand for that.
			spoon = itemDisplay(level, held(), npc.getEyePosition());
			((de.derpvpclient.fakedeath.mixin.DisplayAccessor) spoon).fakedeath$setBillboard(Display.BillboardConstraints.CENTER);
			npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		}
		if (spoon != null) {
			Vec3 hand = npc.getEyePosition().add(0.0, -0.7, 0.0).add(towardPlayer(player).scale(0.4));
			Vec3 mouth = player.getEyePosition().add(0.0, -0.3, 0.0).subtract(towardPlayer(player).scale(0.5));
			double p = t <= SPOON ? Math.min(1.0, t / (double) SPOON) : Math.max(0.0, 1.0 - (t - SPOON - 4) / 6.0);
			p = p * p * (3.0 - 2.0 * p);
			Vec3 at = hand.lerp(mouth, p);
			place(spoon, at, 0.4F);
			if (t % 3 == 0) {
				// Steaming, and something green bubbling in it.
				level.addParticle(ParticleTypes.POOF, at.x, at.y + 0.2, at.z, 0.0, 0.03, 0.0);
				particles(level, ParticleTypes.ITEM_SLIME, at.add(0.0, 0.15, 0.0), 1, 0.08, 0.02);
			}
		}
		if (t == SPOON) {
			sound(level, player.position(), Looks.EAT, SoundSource.PLAYERS, 1.0F, 1.0F);
			sound(level, player.position(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0F, 0.8F);
		}
		if (t == SPOON + 10) {
			despawn(level, spoon);
			spoon = null;
			npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, held());
			speak(minecraft, level, "fakedeath.koch.good");
		}
		if (t == 36) {
			addEffect(player, MobEffects.POISON, 400);
			addEffect(player, MobEffects.CONFUSION, 400);
		}
		if (t > 36 && t % 5 == 0) {
			particles(level, ParticleTypes.ITEM_SLIME, player.getEyePosition(), 3, 0.4, 0.1);
		}
		for (int hit : SICK) {
			if (t == hit) {
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.3F);
			}
		}
		if (t == 90) {
			speak(minecraft, level, "fakedeath.koch.spider");
		}
		if (t == DEATH) {
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}
}
