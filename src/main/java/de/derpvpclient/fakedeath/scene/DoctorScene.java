package de.derpvpclient.fakedeath.scene;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** At the doctor: "Just a little shot." The syringe is as big as the doctor and flies right at the player. */
public class DoctorScene extends NpcScene {
	private static final int RAISE = 20;
	private static final int HIT = 34;
	private Display.BlockDisplay glass;
	private Display.BlockDisplay plunger;
	private Display.BlockDisplay needle;

	public DoctorScene() {
		super("arzt", ChatFormatting.AQUA);
	}

	@Override
	protected int lineCount() {
		return 3;
	}

	@Override
	protected int actLength() {
		return HIT + 60;
	}

	@Override
	protected double standDistance() {
		return 3.2;
	}

	@Override
	protected ResourceKey<VillagerProfession> profession() {
		return VillagerProfession.CLERIC;
	}

	@Override
	protected Vec3 focus(LocalPlayer player) {
		return glass != null ? glass.position() : npc.getEyePosition();
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		if (t == 0) {
			glass = blockDisplay(level, Looks.LIGHT_BLUE_GLASS, npc.position(), 4.0F);
			plunger = blockDisplay(level, Looks.WHITE_CONCRETE, npc.position(), 4.0F);
			needle = blockDisplay(level, Blocks.IRON_BLOCK.defaultBlockState(), npc.position(), 4.0F);
			sound(level, npc.position(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 0.6F);
		}
		Vec3 high = npc.position().add(0.0, 2.8, 0.0);
		Vec3 target = player.getEyePosition().add(0.0, -0.3, 0.0);
		Vec3 at = t < RAISE ? npc.position().add(0.0, 1.2, 0.0).lerp(high, t / (float) RAISE)
			: t < HIT ? high.lerp(target, (t - RAISE) / (float) (HIT - RAISE)) : target.subtract(towardPlayer(player).scale(0.9));
		Vec3 point = target.subtract(at).lengthSqr() < 1.0E-3 ? towardPlayer(player) : target.subtract(at).normalize();
		if (t >= HIT) {
			point = towardPlayer(player);
		}
		Quaternionf rotation = new Quaternionf().rotationTo(new Vector3f(0.0F, 0.0F, 1.0F), new Vector3f((float) point.x, (float) point.y, (float) point.z));
		place(glass, at, new Vector3f(0.45F, 0.45F, 1.6F), rotation);
		Vector3f back = new Vector3f(0.0F, 0.0F, -1.1F).rotate(rotation);
		place(plunger, at.add(back.x, back.y, back.z), new Vector3f(0.25F, 0.25F, 0.9F), rotation);
		Vector3f tip = new Vector3f(0.0F, 0.0F, 1.2F).rotate(rotation);
		place(needle, at.add(tip.x, tip.y, tip.z), new Vector3f(0.06F, 0.06F, 0.9F), rotation);
		if (t == RAISE) {
			Looks.swing(npc);
			sound(level, high, SoundEvents.WITCH_THROW, SoundSource.NEUTRAL, 1.0F, 0.6F);
		}
		if (t == HIT) {
			sound(level, player.position(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.5F, 1.4F);
			sound(level, player.position(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 0.6F, 1.6F);
			particles(level, ParticleTypes.CRIT, player.getEyePosition(), 30, 0.5, 0.2);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
		if (t == HIT + 30) {
			speak(minecraft, level, "fakedeath.arzt.next");
		}
	}
}
