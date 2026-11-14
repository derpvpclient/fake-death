package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The fox next to the player snatches the sword out of their hand. It does not run off with it: it hops back, takes
 * a good grip with its teeth, and comes at the player with their own blade, three times.
 */
public class SwordFoxScene extends Scene {
	private static final int GRAB = 26;
	private static final int BACK = 44;
	private static final int[] STRIKES = {70, 92, 114};
	private static final int LEAP = 8;
	private final Entity found;
	private ItemStack loot;
	private LivingEntity fox;
	private Vec3 start;
	private Vec3 dir;
	private boolean stolen;
	private Display.ItemDisplay blade;

	public SwordFoxScene(Entity fox) {
		this.found = fox;
	}

	@Override
	public int length() {
		return STRIKES[STRIKES.length - 1] + 20;
	}

	@Override
	public int photoTick() {
		return STRIKES[0] - 3;
	}

	@Override
	public InteractionHand hiddenHand() {
		return stolen ? InteractionHand.MAIN_HAND : null;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		// The message names what the fox really took: a totem is no sword.
		if (loot != null && !loot.is(net.minecraft.tags.ItemTags.SWORDS)) {
			return Component.translatable("fakedeath.death.schwertfuchs.item", player.getDisplayName(), loot.getDisplayName());
		}
		return Component.translatable("fakedeath.death.schwertfuchs", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		Vec3 feet = player.position();
		Vec3 head = player.getEyePosition();

		if (tick == 0) {
			ItemStack held = player.getMainHandItem();
			loot = held.isEmpty() ? new ItemStack(Items.IRON_SWORD) : held.copyWithCount(1);

			if (found != null && found.isAlive()) {
				// The real fox steps aside for the scene, its double does the fencing.
				start = found.position();
				found.setInvisible(true);
			} else {
				start = groundSpot(level, player, 3.5, 20.0);
			}

			Vec3 away = start.subtract(feet);
			Vec3 flat = new Vec3(away.x, 0.0, away.z);
			dir = flat.lengthSqr() < 0.3 ? horizontalLook(player) : flat.normalize();
			start = feet.add(dir.scale(Math.max(2.5, Math.min(4.0, flat.length()))));
			fox = spawn(level, EntityType.FOX, ground(level, start, player));
			fox.noPhysics = true;
			AttributeInstance size = fox.getAttribute(Attributes.SCALE);
			if (size != null) {
				size.setBaseValue(1.3);
				fox.refreshDimensions();
			}
			sound(level, start, SoundEvents.FOX_SNIFF, SoundSource.NEUTRAL, 1.5F, 1.0F);
		}

		Vec3 close = feet.add(dir.scale(0.9));
		Vec3 ready = feet.add(dir.scale(3.2));
		Vec3 at;

		if (tick <= GRAB) {
			// Sneaking up, low and slow, the last bit in one snap.
			float p = tick / (float) GRAB;
			at = ground(level, start.lerp(close, p * p), player);
		} else if (tick <= BACK) {
			// Three hops backwards with the loot.
			float p = (tick - GRAB) / (float) (BACK - GRAB);
			at = ground(level, close.lerp(ready, p), player).add(0.0, Math.abs(Math.sin(p * Math.PI * 3.0)) * 0.45, 0.0);
		} else {
			at = ground(level, ready, player);
			for (int strike : STRIKES) {
				int t = tick - (strike - LEAP);
				if (t >= 0 && t <= LEAP + 6) {
					// The leap: up at the chest with the blade, and back down in front of the player.
					Vec3 target = feet.add(dir.scale(0.7)).add(0.0, 0.9, 0.0);
					float p = t <= LEAP ? t / (float) LEAP : 1.0F - (t - LEAP) / 6.0F;
					at = at.lerp(target, p).add(0.0, Math.sin(Math.min(1.0F, t / (float) LEAP) * Math.PI) * 0.6, 0.0);
				}
			}
			if (tick > STRIKES[STRIKES.length - 1]) {
				at = ground(level, feet.add(dir.scale(1.2)), player);
			}
		}

		walk(fox, at);
		face(fox, head);
		if (tick > BACK && tick < STRIKES[0] - LEAP) {
			// Getting a grip: the head goes from side to side, the blade with it.
			float wag = (float) Math.sin((tick - BACK) * 0.8) * 30.0F;
			fox.setYHeadRot(fox.getYRot() + wag);
		}
		turnTowards(player, fox.position().add(0.0, 0.5, 0.0), tick > BACK ? 0.3F : 0.16F);

		if (blade != null) {
			Vec3 to = head.subtract(fox.position());
			Vec3 front = new Vec3(to.x, 0.0, to.z).normalize();
			float swing = 0.0F;
			if (tick > BACK && tick < STRIKES[0] - LEAP) {
				swing = (float) Math.sin((tick - BACK) * 0.8) * 0.35F;
			}
			for (int strike : STRIKES) {
				int t = tick - (strike - LEAP);
				if (t >= 0 && t <= LEAP + 6) {
					// Drawn back on the way up, through in one stroke, and back to rest.
					swing = t <= LEAP - 2 ? 1.1F * t / (LEAP - 2) : t <= LEAP + 1 ? 1.1F - 2.4F * (t - LEAP + 2) / 3.0F : -1.3F * (1.0F - (t - LEAP - 1) / 5.0F);
				}
			}
			Vec3 muzzle = fox.position().add(0.0, 0.62, 0.0).add(front.scale(0.75));
			place(blade, muzzle, new Vector3f(1.25F), new Quaternionf().rotateY((float) Math.atan2(front.x, front.z)).rotateZ((float) (-Math.PI / 4.0) + swing));
		}

		if (tick == GRAB) {
			stolen = true;
			blade = itemDisplay(level, loot, fox.position());
			sound(level, feet, SoundEvents.FOX_BITE, SoundSource.NEUTRAL, 1.5F, 1.2F);
			sound(level, feet, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.2F);
		}

		if (tick == BACK + 8) {
			sound(level, at, SoundEvents.FOX_SCREECH, SoundSource.NEUTRAL, 2.0F, 1.0F);
		}

		for (int i = 0; i < STRIKES.length; i++) {
			if (tick == STRIKES[i] - LEAP) {
				sound(level, at, SoundEvents.FOX_AGGRO, SoundSource.NEUTRAL, 1.5F, 1.0F + i * 0.15F);
			}
			if (tick == STRIKES[i]) {
				Vec3 cut = head.add(dir.scale(0.8)).add(0.0, -0.5, 0.0);
				level.addParticle(ParticleTypes.SWEEP_ATTACK, cut.x, cut.y, cut.z, 0.0, 0.0, 0.0);
				particles(level, ParticleTypes.CRIT, cut, 12, 0.3, 0.4);
				sound(level, feet, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 1.5F, 1.0F + i * 0.1F);
				hit(level, player, SoundEvents.PLAYER_HURT, 0.34F, i == STRIKES.length - 1);
			}
		}
	}

	private static Vec3 ground(ClientLevel level, Vec3 spot, LocalPlayer player) {
		return new Vec3(spot.x, floor(level, spot.x, spot.z, player.getY()), spot.z);
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		if (found != null) {
			found.setInvisible(false);
		}
		fox = null;
		blade = null;
		stolen = false;
		super.end(level, player);
	}
}
