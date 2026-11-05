package de.derpvpclient.fakedeath.scene;

import de.derpvpclient.fakedeath.mixin.AxolotlAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.phys.Vec3;

/**
 * One cute axolotl looks up at the player and tilts its head. Then a second one. A third. Then twenty more pop up all
 * around, every one staring. A moment of silence, and they all come nibbling.
 */
public class AxolotlScene extends Scene {
	private static final int SECOND = 45;
	private static final int THIRD = 70;
	private static final int ARMY = 95;
	private static final int ARMY_SIZE = 22;
	private static final int STARE = 150;
	private static final int RUSH = 175;
	private static final int NIBBLE = RUSH + 20;
	private static final int DEATH = NIBBLE + 48;
	private static final Axolotl.Variant[] COLORS = {Axolotl.Variant.LUCY, Axolotl.Variant.BLUE, Axolotl.Variant.GOLD, Axolotl.Variant.CYAN, Axolotl.Variant.WILD};

	private final List<Axolotl> axolotls = new ArrayList<>();
	private final List<Vec3> spots = new ArrayList<>();

	@Override
	public int length() {
		return DEATH + 25;
	}

	@Override
	public int photoTick() {
		return STARE;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.axolotl", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			// In a dark cave they would not be seen at all.
			if (level.getMaxLocalRawBrightness(player.blockPosition().above()) < 8) {
				addEffect(player, net.minecraft.world.effect.MobEffects.NIGHT_VISION, DEATH + 40);
			}
			add(level, player, groundSpot(level, player, 2.4, 0.0), 0);
		}
		if (tick == SECOND) {
			add(level, player, groundSpot(level, player, 2.6, 28.0), 1);
		}
		if (tick == THIRD) {
			add(level, player, groundSpot(level, player, 2.5, -30.0), 2);
		}
		if (tick >= ARMY && tick < ARMY + ARMY_SIZE * 2 && (tick - ARMY) % 2 == 0) {
			// All around, popping up one after the other, closer to the front so the camera sees most of them.
			int i = (tick - ARMY) / 2;
			double angle = (i * 137.5) % 360.0 - 180.0;
			add(level, player, groundSpot(level, player, 3.0 + (i % 4) * 0.8, angle), i + 3);
		}

		for (int i = 0; i < axolotls.size(); i++) {
			Axolotl axolotl = axolotls.get(i);
			Vec3 home = spots.get(i);
			Vec3 to = home;
			double hop = 0.0;
			if (tick >= RUSH) {
				// All of them at once, little hops towards the feet.
				Vec3 flat = player.position().subtract(home);
				flat = new Vec3(flat.x, 0.0, flat.z);
				double close = 0.55 + (i % 3) * 0.3;
				float p = Math.min(1.0F, (tick - RUSH) / 20.0F);
				Vec3 end = player.position().subtract(flat.normalize().scale(close));
				to = home.lerp(new Vec3(end.x, floor(level, end.x, end.z, player.getY()), end.z), p);
				hop = Math.abs(Mth.sin((tick + i * 3) * 0.6F)) * 0.25;
			}
			walk(axolotl, to.add(0.0, hop, 0.0));
			// Cute: the head tilts from side to side, each one at its own pace, until the stare.
			Vec3 eyes = player.getEyePosition();
			if (tick < STARE) {
				eyes = eyes.add(towards(player, home).yRot(Mth.HALF_PI).scale(Mth.sin((tick + i * 11) * 0.12F) * 1.5));
			}
			face(axolotl, eyes);
		}

		// The camera follows the newcomers, then slowly looks round at the whole crowd.
		if (tick < ARMY && !axolotls.isEmpty()) {
			turnTowards(player, axolotls.get(axolotls.size() - 1).position(), 0.15F);
		} else if (tick < STARE) {
			turnTowards(player, player.getEyePosition().add(horizontalLook(player).yRot((float) Math.sin((tick - ARMY) * 0.05) * 1.6F).scale(4.0)).add(0.0, -1.5, 0.0), 0.08F);
		} else if (!axolotls.isEmpty()) {
			turnTowards(player, axolotls.get(0).position(), 0.15F);
		}

		if (tick == STARE) {
			// Silence. All of them look straight at you.
			sound(level, player.position(), SoundEvents.AXOLOTL_IDLE_AIR, SoundSource.NEUTRAL, 1.0F, 0.5F);
		}
		if (tick == RUSH) {
			sound(level, player.position(), SoundEvents.AXOLOTL_ATTACK, SoundSource.NEUTRAL, 1.5F, 0.8F);
		}
		if (tick >= RUSH && tick % 3 == 0) {
			sound(level, player.position().add(horizontalLook(player).yRot(random.nextFloat() * 6.0F).scale(2.0)), SoundEvents.AXOLOTL_IDLE_AIR, SoundSource.NEUTRAL, 0.8F, 0.8F + random.nextFloat() * 0.6F);
		}
		if (tick >= NIBBLE && tick < DEATH && (tick - NIBBLE) % 12 == 0) {
			particles(level, ParticleTypes.CRIT, player.position().add(0.0, 0.4, 0.0), 12, 0.5, 0.1);
			hurt(level, player, SoundEvents.AXOLOTL_ATTACK, 0.18F);
		}
		if (tick == DEATH) {
			particles(level, ParticleTypes.HEART, player.getEyePosition(), 8, 0.6, 0.1);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void add(ClientLevel level, LocalPlayer player, Vec3 spot, int i) {
		Axolotl axolotl = spawn(level, EntityTypes.AXOLOTL, spot);
		axolotl.noPhysics = true;
		((AxolotlAccessor) axolotl).fakedeath$setVariant(i == 0 ? Axolotl.Variant.LUCY : COLORS[i % COLORS.length]);
		axolotls.add(axolotl);
		spots.add(spot);
		particles(level, ParticleTypes.POOF, spot.add(0.0, 0.2, 0.0), 6, 0.2, 0.02);
		particles(level, ParticleTypes.HEART, spot.add(0.0, 0.6, 0.0), 1, 0.1, 0.0);
		sound(level, spot, SoundEvents.AXOLOTL_IDLE_AIR, SoundSource.NEUTRAL, 0.9F, 1.0F + random.nextFloat() * 0.4F);
	}

	private static Vec3 towards(LocalPlayer player, Vec3 from) {
		Vec3 delta = player.position().subtract(from);
		return new Vec3(delta.x, 0.0, delta.z).normalize();
	}
}
