package de.derpvpclient.fakedeath.scene;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** A magician at the fair needs a volunteer: "I will now saw you in half!" The saw goes back and forth. The trick fails. */
public class MagicianScene extends NpcScene {
	private static final int SAW = 70;
	private Display.ItemDisplay saw;

	public MagicianScene() {
		super("zauberer", ChatFormatting.DARK_PURPLE);
	}

	@Override
	protected int lineCount() {
		return 3;
	}

	@Override
	protected int actLength() {
		return SAW + 60;
	}

	@Override
	protected ResourceKey<VillagerProfession> profession() {
		return VillagerProfession.CLERIC;
	}

	@Override
	protected Vec3 focus(LocalPlayer player) {
		return saw != null ? saw.position() : npc.getEyePosition();
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		if (t == 0) {
			sound(level, npc.position(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6F, 1.5F);
			particles(level, ParticleTypes.WITCH, npc.getEyePosition(), 20, 0.5, 0.1);
			saw = itemDisplay(level, new ItemStack(Items.IRON_SWORD), player.position().add(0.0, 0.9, 0.0));
		}
		if (t <= SAW) {
			// Through the middle, from side to side.
			Vec3 side = towardPlayer(player).yRot(Mth.HALF_PI);
			Vec3 at = player.position().add(0.0, 0.85, 0.0).add(side.scale(Mth.sin(t * 0.45F) * 0.9)).subtract(towardPlayer(player).scale(0.15));
			Quaternionf rotation = new Quaternionf().rotateY((float) Math.atan2(side.x, side.z)).rotateZ(-Mth.HALF_PI / 2);
			place(saw, at, new Vector3f(1.4F), rotation);
			if (t % 7 == 0) {
				sound(level, at, Looks.SAW, SoundSource.NEUTRAL, 1.0F, 0.8F + random.nextFloat() * 0.3F);
				particles(level, ParticleTypes.CRIT, at, 4, 0.2, 0.2);
			}
			if (t == 20 || t == 45) {
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.3F);
			}
		}
		if (t == SAW - 6) {
			speak(minecraft, level, "fakedeath.zauberer.oops");
		}
		if (t == SAW) {
			sound(level, player.position(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.5F, 0.7F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
		if (t == SAW + 35) {
			speak(minecraft, level, "fakedeath.zauberer.applause");
		}
	}
}
