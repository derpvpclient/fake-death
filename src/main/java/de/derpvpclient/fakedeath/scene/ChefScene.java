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
import org.joml.Quaternionf;
import org.joml.Vector3f;

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
	protected ResourceKey<VillagerProfession> profession() {
		return VillagerProfession.BUTCHER;
	}

	@Override
	protected ItemStack held() {
		return new ItemStack(Items.SUSPICIOUS_STEW);
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		if (t == 0) {
			spoon = itemDisplay(level, new ItemStack(Items.WOODEN_SHOVEL), npc.getEyePosition());
		}
		if (spoon != null && t <= SPOON + 6) {
			Vec3 mouth = player.getEyePosition().add(0.0, -0.25, 0.0).subtract(towardPlayer(player).scale(0.4));
			Vec3 at = npc.getEyePosition().add(0.0, -0.4, 0.0).lerp(mouth, Math.min(1.0, t / (double) SPOON));
			place(spoon, at, new Vector3f(0.3F), new Quaternionf().rotateY((float) Math.atan2(towardPlayer(player).x, towardPlayer(player).z)).rotateX(1.2F));
		}
		if (t == SPOON) {
			sound(level, player.position(), Looks.EAT, SoundSource.PLAYERS, 1.0F, 1.0F);
			sound(level, player.position(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0F, 0.8F);
		}
		if (t == SPOON + 6) {
			despawn(level, spoon);
			spoon = null;
			speak(minecraft, level, "fakedeath.koch.good");
		}
		if (t == 36) {
			addEffect(player, MobEffects.POISON, 400);
			addEffect(player, MobEffects.NAUSEA, 400);
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
