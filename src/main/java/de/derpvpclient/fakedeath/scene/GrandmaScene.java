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
 * Grandma makes videos now: a ring light on its tripod, a tin of cookies, her knitting, and a selfie stick with a pink
 * phone. "Say cheese!" Flash. "And now really close!" Three pokes with the stick, the last one too close. "Oh. Is this
 * live now?"
 */
public class GrandmaScene extends NpcScene {
	private static final int CHEESE = 20;
	private static final int CLOSE = 55;
	private static final int[] POKES = {70, 92, 114};
	private static final int POKE = 8;
	private Display.ItemDisplay stick;
	private int flash;
	private Display.ItemDisplay ringLight;
	private Display.ItemDisplay cookies;

	public GrandmaScene() {
		super("oma", ChatFormatting.LIGHT_PURPLE);
	}

	@Override
	protected ItemStack held() {
		// Knitting while she talks, the stick comes out for the video.
		return Looks.prop(Items.STRING, "knitting");
	}

	@Override
	protected void setup(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
		// Her studio: the ring light glowing next to her, the cookie tin on the ground at her other side.
		Vec3 toward = towardPlayer(player);
		Vec3 side = toward.yRot(Mth.HALF_PI);
		Vec3 light = stand.add(side.scale(1.3)).add(toward.scale(-0.3));
		light = new Vec3(light.x, floor(level, light.x, light.z, player.getY()), light.z);
		ringLight = itemDisplay(level, Looks.prop(Items.IRON_NUGGET, "ring_light"), light);
		Looks.glow(ringLight);
		float yaw = (float) Math.atan2(toward.x, toward.z);
		place(ringLight, light.add(0.0, 1.0, 0.0), new Vector3f(1.0F), new Quaternionf().rotateY(yaw + Mth.PI));
		Vec3 tin = stand.add(side.scale(-1.0)).add(toward.scale(0.4));
		tin = new Vec3(tin.x, floor(level, tin.x, tin.z, player.getY()), tin.z);
		cookies = itemDisplay(level, Looks.prop(Items.COOKIE, "cookie_tin"), tin);
		place(cookies, tin.add(0.0, 0.5, 0.0), new Vector3f(0.8F), new Quaternionf().rotateY(yaw));
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
		// The camera flash, only when flashing effects are allowed in the settings.
		return flash > 0 && de.derpvpclient.fakedeath.FakeDeathConfig.flashing ? (Math.min(255, flash * 60) << 24) | 0xFFFFFF : 0;
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
			stick = itemDisplay(level, Looks.prop(Items.STICK, "grandma_selfie_stick"), npc.getEyePosition());
			npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
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
				sound(level, player.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.NEUTRAL, 1.5F, 1.2F);
				particles(level, ParticleTypes.CRIT, player.getEyePosition(), 15, 0.4, 0.2);
				hit(level, player, SoundEvents.PLAYER_HURT, 0.3F, i == POKES.length - 1);
				if (i < POKES.length - 1) {
					flash = 3;
					speak(minecraft, level, "fakedeath.oma.again");
				}
			}
		}
		Vec3 at = hand.add(aim.scale(reach + 0.6));
		// The stick runs up its model, the phone at the top: turned so the phone end points along the aim.
		place(stick, at, new Vector3f(0.55F), new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), new Vector3f((float) aim.x, (float) aim.y, (float) aim.z)));

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
