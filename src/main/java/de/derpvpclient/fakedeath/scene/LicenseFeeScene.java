package de.derpvpclient.fakedeath.scene;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** The license fee office comes for the money. You never paid, so he brought the TV along and drops it on you. */
public class LicenseFeeScene extends NpcScene {
	private static final float W = 1.0F;
	private static final float H = 0.75F;
	private static final float D = 0.45F;
	private static final int LIFT = 20;
	private static final int HIT = 42;
	private Display.BlockDisplay body;
	private Display.BlockDisplay screen;

	public LicenseFeeScene() {
		super("rundfunk", ChatFormatting.BLUE);
	}

	@Override
	protected int lineCount() {
		return 3;
	}

	@Override
	protected int actLength() {
		return 100;
	}

	@Override
	protected ResourceKey<VillagerProfession> profession() {
		return VillagerProfession.LIBRARIAN;
	}

	@Override
	protected ItemStack held() {
		return new ItemStack(Items.PAPER);
	}

	@Override
	protected Vec3 focus(LocalPlayer player) {
		return body != null ? body.position() : npc.getEyePosition();
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		if (t == 0) {
			body = blockDisplay(level, Looks.BLACK_CONCRETE, npc.position(), 3.0F);
			screen = blockDisplay(level, Looks.LIGHT_BLUE_GLASS, npc.position(), 3.0F);
		}
		Vec3 dir = towardPlayer(player);
		float yaw = (float) Math.atan2(dir.x, dir.z);
		Vec3 up = npc.position().add(0.0, 2.6, 0.0);
		Vec3 at;
		float spin = 0.0F;
		if (t < LIFT) {
			at = npc.position().add(0.0, 0.9, 0.0).lerp(up, t / (float) LIFT);
		} else if (t < HIT) {
			float p = (t - LIFT) / (float) (HIT - LIFT);
			at = up.lerp(player.getEyePosition().add(0.0, 0.3, 0.0), p).add(0.0, Mth.sin(p * Mth.PI) * 2.5, 0.0);
			spin = p * 3.0F;
			if (t == LIFT) {
				Looks.swing(npc);
				sound(level, up, SoundEvents.WITCH_THROW, SoundSource.NEUTRAL, 1.0F, 0.6F);
			}
		} else {
			at = player.getEyePosition().add(dir.scale(0.6)).add(0.0, -1.0, 0.0);
			spin = 1.4F;
		}
		Quaternionf rotation = new Quaternionf().rotateY(yaw).rotateX(spin);
		place(body, at, new Vector3f(W, H, D), rotation);
		Vector3f front = new Vector3f(0.0F, 0.0F, -D * 0.55F).rotate(rotation);
		place(screen, at.add(front.x, front.y, front.z), new Vector3f(W * 0.85F, H * 0.75F, 0.05F), rotation);
		if (t == HIT) {
			sound(level, player.position(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.5F, 0.8F);
			sound(level, player.position(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 1.0F, 1.2F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Looks.LIGHT_BLUE_GLASS), player.getEyePosition(), 25, 0.5, 0.2);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
		if (t == HIT + 30) {
			speak(minecraft, level, "fakedeath.rundfunk.bye");
		}
	}
}
