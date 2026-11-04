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
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** An influencer films the player for a prank video, runs at them shouting "Prank!", slips and takes them down. */
public class InfluencerScene extends NpcScene {
	private static final int RUN = 12;
	private static final int HIT = 16;
	private Vec3 from;

	public InfluencerScene() {
		super("influencer", ChatFormatting.LIGHT_PURPLE);
	}

	@Override
	protected int lineCount() {
		return 3;
	}

	@Override
	protected int actLength() {
		return 90;
	}

	@Override
	protected VillagerProfession profession() {
		return VillagerProfession.NITWIT;
	}

	@Override
	protected ItemStack held() {
		return new ItemStack(Items.SPYGLASS);
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		if (t == 0) {
			from = npc.position();
			speak(minecraft, level, "fakedeath.influencer.prank");
		}
		if (t <= HIT) {
			// Running in, then the feet go up and he flies right into the player.
			float p = t / (float) HIT;
			double hop = t > RUN ? Mth.sin((t - RUN) / (float) (HIT - RUN) * Mth.PI) * 0.8 : 0.0;
			walk(npc, from.lerp(player.position(), p).add(0.0, hop, 0.0));
			if (t == RUN) {
				sound(level, npc.position(), SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 1.2F, 1.4F);
				particles(level, ParticleTypes.SPLASH, npc.position(), 10, 0.4, 0.1);
			}
		}
		if (t == HIT) {
			sound(level, player.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.5F, 0.8F);
			sound(level, player.position(), SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 1.2F, 1.0F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
		if (t > HIT) {
			// Knocked onto the back, looking at the sky.
			moveCamera(new Vec3(0.0, -1.1, 0.0).add(towardPlayer(player).scale(0.8)));
			turnTowards(player, player.getEyePosition().add(towardPlayer(player).scale(2.0)).add(0.0, 6.0, 0.0), 0.3F);
		}
		if (t == 50) {
			speak(minecraft, level, "fakedeath.influencer.subscribe");
		}
	}
}
