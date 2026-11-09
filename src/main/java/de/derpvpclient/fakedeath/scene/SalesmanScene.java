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

/**
 * "Have you heard about our offer?" He never stops. The chat fills up line after line, the view sinks, the eyes fall
 * shut, and the player dies of boredom.
 */
public class SalesmanScene extends NpcScene {
	private static final int PITCH = 12;
	private static final int EVERY = 18;
	private static final int DEATH = PITCH * EVERY + 6;
	private int now = -1;

	public SalesmanScene() {
		super("werbung", ChatFormatting.GREEN);
	}

	@Override
	protected int lineCount() {
		return 1;
	}

	@Override
	protected int actLength() {
		return DEATH + 50;
	}

	@Override
	protected int photoOffset() {
		return EVERY * 8;
	}

	@Override
	protected double standDistance() {
		return 2.5;
	}

	@Override
	protected VillagerProfession profession() {
		return VillagerProfession.CARTOGRAPHER;
	}

	@Override
	protected ItemStack held() {
		return Looks.prop(Items.BOOK, "briefcase");
	}

	@Override
	public int screenTint() {
		// The eyes falling shut, a little more with every line.
		if (now < 0) {
			return 0;
		}
		float bored = Math.min(1.0F, now / (float) DEATH);
		float blink = now % 50 < 6 ? 0.35F : 0.0F;
		return ((int) (Math.min(1.0F, bored * bored * 0.85F + blink * bored) * 255) << 24) | 0x050505;
	}

	@Override
	protected Vec3 focus(LocalPlayer player) {
		// The head sinks lower and lower.
		float bored = now < 0 ? 0.0F : Math.min(1.0F, now / (float) DEATH);
		return npc.getEyePosition().add(0.0, -2.2 * bored, 0.0);
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		now = t;
		if (t < DEATH && t % EVERY == 0 && t / EVERY < PITCH) {
			speak(minecraft, level, "fakedeath.werbung.a" + (t / EVERY + 1));
			Looks.swing(npc);
			particles(level, ParticleTypes.HAPPY_VILLAGER, npc.getEyePosition().add(0.0, 0.5, 0.0), 3, 0.3, 0.0);
		}
		// He talks with his whole body, bobbing up and down.
		Vec3 at = npc.position();
		walk(npc, new Vec3(at.x, floor(level, at.x, at.z, player.getY()) + Math.abs(Mth.sin(t * 0.4F)) * 0.15, at.z));
		face(npc, player.getEyePosition().add(0.0, Mth.sin(t * 0.3F) * 0.3, 0.0));

		// Swaying, yawning, and every few lines it hurts a bit more.
		float bored = Math.min(1.0F, t / (float) DEATH);
		moveCamera(new Vec3(0.0, -0.4 * bored, 0.0).add(towardPlayer(player).yRot(Mth.HALF_PI).scale(Mth.sin(t * 0.06F) * 0.2 * bored)));
		if (t > 0 && t < DEATH && t % (EVERY * 3) == 0) {
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.2F);
			sound(level, player.position(), SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 1.0F, 0.6F);
		}
		if (t == DEATH) {
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
		if (t == DEATH + 25) {
			speak(minecraft, level, "fakedeath.werbung.tomorrow");
		}
	}
}
