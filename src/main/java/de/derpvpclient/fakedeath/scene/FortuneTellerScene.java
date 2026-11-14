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

/**
 * A fortune teller holds up her glowing crystal ball: "I see ... an early death." The ball turns red, and inside it a
 * tiny anvil spins: that is how it ends. The ball cracks and bursts in her hands. "The ball is never wrong." Both look
 * up, and out of the sky comes the anvil, right onto the player.
 */
public class FortuneTellerScene extends NpcScene {
	private static final int VISION = 22;
	private static final int CRACK = 62;
	private static final int BURST = 78;
	private static final int LOOK_UP = BURST + 18;
	private static final int FALL = 22;
	private static final int DEATH = LOOK_UP + 20 + FALL;
	private Display.BlockDisplay ball;
	private Display.BlockDisplay core;
	private Vec3 high;
	private Vec3 foot;
	private Display.ItemDisplay vision;
	private Display.BlockDisplay anvil;
	private Vec3 sky;

	public FortuneTellerScene() {
		super("wahrsagerin", ChatFormatting.LIGHT_PURPLE);
	}

	@Override
	protected int lineCount() {
		return 2;
	}

	@Override
	protected int actLength() {
		return DEATH + 10;
	}

	@Override
	protected int photoOffset() {
		return VISION + 20;
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
		if (anvil != null) {
			return anvil.position();
		}
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

		actNew(minecraft, level, player, t);
	}

	/** The prophecy: the ball shows the anvil, bursts, and the anvil comes. */
	private void actNew(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		Vec3 at = high.add(0.0, Mth.sin(t * 0.15F) * 0.08, 0.0);
		if (ball != null) {
			float shake = t >= CRACK ? (t - CRACK) * 0.004F : 0.0F;
			at = at.add((random.nextDouble() - 0.5) * shake, (random.nextDouble() - 0.5) * shake, (random.nextDouble() - 0.5) * shake);
			place(ball, at, new Vector3f(0.5F), new Quaternionf().rotateY(t * 0.05F).rotateX(t * 0.03F));
			place(core, at, new Vector3f(0.22F + Mth.sin(t * 0.25F) * 0.04F), new Quaternionf().rotateY(-t * 0.12F).rotateZ(t * 0.07F));
			for (int i = 0; i < 2; i++) {
				double a = t * 0.3 + i * Math.PI;
				level.addParticle(t < VISION ? ParticleTypes.WITCH : ParticleTypes.CRIMSON_SPORE, at.x + Math.cos(a) * 0.6, at.y, at.z + Math.sin(a) * 0.6, 0, 0.02, 0);
			}
		}
		if (t == 15) {
			sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.5F, 0.6F);
		}
		if (t == VISION) {
			// The ball turns blood red, and inside it spins the future: a little anvil.
			despawn(level, ball);
			ball = blockDisplay(level, net.minecraft.world.level.block.Blocks.RED_STAINED_GLASS.defaultBlockState(), at, 3.0F);
			Looks.glow(ball);
			vision = itemDisplay(level, new ItemStack(Items.ANVIL), at);
			Looks.glow(vision);
			sound(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 1.5F, 0.4F);
			speak(minecraft, level, "fakedeath.wahrsagerin.vision");
		}
		if (vision != null) {
			place(vision, at, new Vector3f(0.3F), new Quaternionf().rotateY(t * 0.2F));
		}
		if (t == CRACK) {
			sound(level, at, SoundEvents.GLASS_HIT, SoundSource.NEUTRAL, 1.5F, 0.6F);
		}
		if (t > CRACK && t < BURST && t % 4 == 0) {
			sound(level, at, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.NEUTRAL, 1.0F, 0.5F + (t - CRACK) * 0.04F);
		}
		if (t == BURST) {
			// It bursts in her hands.
			despawn(level, ball);
			despawn(level, core);
			despawn(level, vision);
			ball = null;
			vision = null;
			sound(level, at, SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 2.0F, 0.6F);
			particles(level, new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.RED_STAINED_GLASS.defaultBlockState()), at, 30, 0.4, 0.2);
			speak(minecraft, level, "fakedeath.wahrsagerin.told");
		}
		if (t == LOOK_UP) {
			// A whistling from above. She looks up, so does the player.
			sky = player.getEyePosition().add(0.0, 18.0, 0.0);
			anvil = blockDisplay(level, net.minecraft.world.level.block.Blocks.ANVIL.defaultBlockState(), sky, 4.0F);
			sound(level, sky, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 1.5F, 0.5F);
		}
		if (t >= LOOK_UP) {
			face(npc, player.getEyePosition().add(0.0, 6.0, 0.0));
		}
		if (anvil != null) {
			float p = Math.min(1.0F, Math.max(0.0F, (t - LOOK_UP - 20) / (float) FALL));
			Vec3 head = player.getEyePosition().add(0.0, 0.5, 0.0);
			place(anvil, sky.lerp(head, p * p), 1.0F);
			if (t < LOOK_UP + 20 && t % 6 == 0) {
				sound(level, player.position(), SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 0.6F, 0.4F);
			}
		}
		if (t == DEATH) {
			sound(level, player.position(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 2.0F, 0.8F);
			cameraShake(0.3F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}
}
