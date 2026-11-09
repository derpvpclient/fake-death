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
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A fortune teller holds up her glowing crystal ball: "I see … an early death." It slips out of her hands and lands on
 * the player's foot. Hopping on one leg, they tip over. "See? The ball is never wrong."
 */
public class FortuneTellerScene extends NpcScene {
	private static final int SLIP = 50;
	private static final int FOOT = SLIP + 14;
	private static final int OUCH = FOOT + 22;
	private static final int TIP = FOOT + 40;
	private static final int DEATH = TIP + 14;
	private Display.BlockDisplay ball;
	private Display.BlockDisplay core;
	private Vec3 high;
	private Vec3 foot;

	public FortuneTellerScene() {
		super("wahrsagerin", ChatFormatting.LIGHT_PURPLE);
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
	protected int photoOffset() {
		return SLIP + 6;
	}

	@Override
	protected double standDistance() {
		return 2.2;
	}

	@Override
	protected ItemStack held() {
		return Looks.prop(Items.PAPER, "tarot_card");
	}

	@Override
	protected ResourceKey<VillagerProfession> profession() {
		return VillagerProfession.CLERIC;
	}

	@Override
	protected Vec3 focus(LocalPlayer player) {
		return ball != null ? ball.position() : npc.getEyePosition();
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		if (t == 0) {
			high = npc.getEyePosition().add(towardPlayer(player).scale(0.5)).add(0.0, 0.7, 0.0);
			foot = player.position().add(horizontalLook(player).scale(0.35)).add(0.0, 0.25, 0.0);
			ball = blockDisplay(level, Looks.PURPLE_GLASS, high, 3.0F);
			Looks.glow(ball);
			core = blockDisplay(level, net.minecraft.world.level.block.Blocks.AMETHYST_BLOCK.defaultBlockState(), high, 3.0F);
			Looks.glow(core);
			sound(level, high, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 1.5F, 0.8F);
		}
		Vec3 at;
		if (t < SLIP) {
			// Held up high, slowly turning, with magic swirling around it.
			at = high.add(0.0, Mth.sin(t * 0.15F) * 0.08, 0.0);
			for (int i = 0; i < 2; i++) {
				double a = t * 0.3 + i * Math.PI;
				level.addParticle(ParticleTypes.WITCH, at.x + Math.cos(a) * 0.6, at.y, at.z + Math.sin(a) * 0.6, 0, 0.02, 0);
			}
			if (t % 10 == 0) {
				particles(level, ParticleTypes.ENCHANT, at, 6, 0.5, 0.5);
			}
			if (t == 15) {
				sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.5F, 0.6F);
			}
		} else if (t <= FOOT) {
			// Slips, a little bounce up, and right down onto the foot.
			float p = (t - SLIP) / (float) (FOOT - SLIP);
			at = high.lerp(foot, p * p).add(0.0, Mth.sin(p * Mth.PI) * 0.6, 0.0);
		} else {
			at = foot;
		}
		place(ball, at, new Vector3f(0.5F), new Quaternionf().rotateY(t * 0.05F).rotateX(t * 0.03F));
		// A glowing core inside, turning the other way and pulsing.
		place(core, at, new Vector3f(0.22F + Mth.sin(t * 0.25F) * 0.04F), new Quaternionf().rotateY(-t * 0.12F).rotateZ(t * 0.07F));

		if (t == SLIP) {
			speak(minecraft, level, "fakedeath.wahrsagerin.oops");
		}
		if (t == FOOT) {
			sound(level, foot, SoundEvents.GLASS_HIT, SoundSource.NEUTRAL, 1.5F, 0.5F);
			sound(level, foot, SoundEvents.STONE_FALL, SoundSource.NEUTRAL, 1.5F, 0.7F);
			particles(level, ParticleTypes.CRIT, foot, 20, 0.3, 0.2);
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.35F);
		}
		if (t > FOOT && t < TIP) {
			// Hopping on one leg, wobbling more and more.
			int h = t - FOOT;
			double hop = Math.abs(Mth.sin(h * 0.45F)) * 0.45;
			moveCamera(new Vec3(horizontalLook(player).yRot(Mth.HALF_PI).scale(Mth.sin(h * 0.2F) * 0.25 * h / 20.0).x, hop, horizontalLook(player).yRot(Mth.HALF_PI).scale(Mth.sin(h * 0.2F) * 0.25 * h / 20.0).z));
			turnTowards(player, foot, 0.2F);
		}
		if (t == OUCH) {
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.3F);
			particles(level, ParticleTypes.CRIT, foot, 15, 0.3, 0.2);
		}
		if (t >= TIP && t <= DEATH) {
			// And over backwards, looking up at the sky.
			float p = (t - TIP) / (float) (DEATH - TIP);
			moveCamera(horizontalLook(player).scale(-0.8 * p).add(0.0, -1.3 * p, 0.0));
			turnTowards(player, player.getEyePosition().add(horizontalLook(player).scale(1.0)).add(0.0, 6.0, 0.0), 0.3F);
		}
		if (t == DEATH) {
			sound(level, player.position(), SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 1.0F, 1.0F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
		if (t > DEATH) {
			moveCamera(horizontalLook(player).scale(-0.8).add(0.0, -1.3, 0.0));
		}
		if (t == DEATH + 25) {
			speak(minecraft, level, "fakedeath.wahrsagerin.told");
		}
	}
}
