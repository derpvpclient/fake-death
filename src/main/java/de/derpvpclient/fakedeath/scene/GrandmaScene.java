package de.derpvpclient.fakedeath.scene;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Grandma makes videos now. Her selfie stick is a trident. "Say cheese!" Flash. "And now really close!" Three pokes, the
 * last one too close. "Oh. Is this live now?"
 */
public class GrandmaScene extends NpcScene {
	private static final int CHEESE = 20;
	private static final int CLOSE = 55;
	private static final int[] POKES = {70, 92, 114};
	private static final int POKE = 8;
	private Display.ItemDisplay stick;
	private int flash;

	public GrandmaScene() {
		super("oma", ChatFormatting.LIGHT_PURPLE);
	}

	@Override
	protected int lineCount() {
		return 2;
	}

	@Override
	protected int actLength() {
		return POKES[POKES.length - 1] + 60;
	}

	@Override
	protected int photoOffset() {
		return POKES[1] + 3;
	}

	@Override
	protected double standDistance() {
		return 2.4;
	}

	@Override
	public int screenTint() {
		return flash > 0 ? (Math.min(255, flash * 60) << 24) | 0xFFFFFF : 0;
	}

	@Override
	protected Vec3 focus(LocalPlayer player) {
		return npc.getEyePosition().add(0.0, -0.2, 0.0);
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		if (flash > 0) {
			flash--;
		}
		Vec3 toward = towardPlayer(player);
		if (t == 0) {
			stick = itemDisplay(level, new ItemStack(Items.TRIDENT), npc.getEyePosition());
		}

		// Waving it around looking for the right angle, then poking at the player's face.
		Vec3 hand = npc.getEyePosition().add(toward.scale(0.6)).add(0.0, -0.3, 0.0);
		Vec3 aim = player.getEyePosition().subtract(hand).normalize();
		double reach = 0.4;
		if (t < POKES[0] - 6) {
			aim = aim.add(toward.yRot(Mth.HALF_PI).scale(Mth.sin(t * 0.15F) * 0.6)).add(0.0, 0.4 + Mth.sin(t * 0.21F) * 0.4, 0.0).normalize();
		}
		for (int i = 0; i < POKES.length; i++) {
			int p = t - POKES[i];
			if (p >= -6 && p < 0) {
				reach = 0.4 - 0.3 * (p + 6) / 6.0;
			} else if (p >= 0 && p < POKE) {
				reach = 0.1 + Mth.sin(p / (float) POKE * Mth.PI) * (hand.distanceTo(player.getEyePosition()) - 1.1);
			}
			if (p == POKE / 2) {
				Looks.swing(npc);
				sound(level, player.position(), SoundEvents.TRIDENT_HIT, SoundSource.NEUTRAL, 1.5F, 1.0F);
				particles(level, ParticleTypes.CRIT, player.getEyePosition(), 15, 0.4, 0.2);
				hit(level, player, SoundEvents.PLAYER_HURT, 0.3F, i == POKES.length - 1);
				if (i < POKES.length - 1) {
					flash = 3;
					speak(minecraft, level, "fakedeath.oma.again");
				}
			}
		}
		Vec3 at = hand.add(aim.scale(reach + 0.6));
		// The trident icon points to the top right, turned so that the tip points along the aim.
		float yaw = (float) Math.atan2(-aim.z, aim.x);
		float pitch = (float) Math.atan2(aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z));
		place(stick, at, new Vector3f(1.0F), new Quaternionf().rotateY(yaw).rotateZ(pitch - Mth.PI / 4));

		if (t == CHEESE) {
			speak(minecraft, level, "fakedeath.oma.cheese");
		}
		if (t == CHEESE + 15) {
			flash = 4;
			sound(level, npc.position(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.NEUTRAL, 1.5F, 1.8F);
		}
		if (t == CLOSE) {
			speak(minecraft, level, "fakedeath.oma.close");
		}
		if (t == POKES[POKES.length - 1] + 30) {
			speak(minecraft, level, "fakedeath.oma.live");
		}
	}
}
