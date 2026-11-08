package de.derpvpclient.fakedeath.scene;

import de.derpvpclient.fakedeath.FakeDeath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Something dangerous right next to the player: lava, fire, a cactus, powder snow, water or hunger.
 * It takes a few seconds, the hearts go down hit by hit, fire spreads around the player.
 */
public class HazardScene extends Scene {
	private static final int HIT_EVERY = 20;
	private static final int LENGTH = 140;

	private static final int FOOD_GONE = 60;

	private enum Extra { NONE, FIRE, FREEZE, LAVA, HUNGER }

	private int startFood;
	private int now;

	private final String messageKey;
	private final SoundEvent hurtSound;
	private final ParticleOptions particle;
	private final Extra extra;
	private final BlockPos source;

	private HazardScene(String messageKey, SoundEvent hurtSound, ParticleOptions particle, Extra extra, BlockPos source) {
		this.messageKey = messageKey;
		this.hurtSound = hurtSound;
		this.particle = particle;
		this.extra = extra;
		this.source = source;
	}

	public static HazardScene lava(BlockPos lava) {
		return new HazardScene("death.attack.lava", SoundEvents.PLAYER_HURT_ON_FIRE, ParticleTypes.LAVA, Extra.LAVA, lava);
	}

	public static HazardScene fire() {
		return new HazardScene("death.attack.onFire", SoundEvents.PLAYER_HURT_ON_FIRE, ParticleTypes.FLAME, Extra.FIRE, null);
	}

	public static HazardScene cactus(BlockPos cactus) {
		return new HazardScene("death.attack.cactus", SoundEvents.PLAYER_HURT, ParticleTypes.CRIT, Extra.NONE, cactus);
	}

	public static HazardScene freeze(BlockPos snow) {
		return new HazardScene("death.attack.freeze", SoundEvents.PLAYER_HURT_FREEZE, ParticleTypes.SNOWFLAKE, Extra.FREEZE, snow);
	}

	public static HazardScene starve() {
		return new HazardScene("death.attack.starve", SoundEvents.PLAYER_HURT, null, Extra.HUNGER, null);
	}

	@Override
	public int length() {
		return LENGTH;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable(messageKey, player.getDisplayName());
	}

	@Override
	public int screenTint() {
		// Starving: everything goes darker and darker.
		if (extra != Extra.HUNGER || now < FOOD_GONE) {
			return 0;
		}
		return Math.min(170, 30 + (now - FOOD_GONE) * 140 / (LENGTH - FOOD_GONE)) << 24;
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		now = tick;
		Vec3 body = player.position().add(0, 0.9, 0);
		boolean burning = extra == Extra.FIRE || extra == Extra.LAVA;

		if (tick == 0) {
			switch (extra) {
				case FIRE -> sound(level, body, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
				case LAVA -> sound(level, Vec3.atCenterOf(source), SoundEvents.LAVA_POP, SoundSource.BLOCKS, 1.5F, 0.8F);
				case FREEZE -> freeze(player);
				case HUNGER -> {
					startFood = player.getFoodData().getFoodLevel();
					// Green hunger icons, like after rotten flesh.
					addEffect(player, MobEffects.HUNGER, 200);
				}
				default -> {
				}
			}
		}

		if (extra == Extra.HUNGER) {
			// Weaker with every second: the view sways, sinks lower and the stomach growls.
			float weak = tick / (float) LENGTH;
			moveCamera(new Vec3(Math.sin(tick * 0.09) * 0.25 * weak, -weak * weak * 0.9 + Math.sin(tick * 0.05) * 0.08, 0.0));
			if (tick % 35 == 10) {
				sound(level, body, SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.7F, 0.4F);
			}
		}

		if (extra == Extra.HUNGER) {
			// The hunger bar runs empty first, the damage only starts once it is.
			FakeDeath.setFakeFood(Math.round(startFood * Math.max(0.0F, 1.0F - tick / (float) FOOD_GONE)));

			if (tick < FOOD_GONE) {
				return;
			}

			if (tick == FOOD_GONE) {
				// Weak with hunger: the view narrows and sways a little.
				addEffect(player, MobEffects.CONFUSION, LENGTH);
			}
		}

		if (source != null && tick < 15) {
			turnTowards(player, Vec3.atCenterOf(source), 0.15F);
		}

		if (burning) {
			burn(level, player, tick);
		}

		if (extra == Extra.FREEZE) {
			// Sinking into the powder snow: the view slowly goes down, everything turns white and the body shivers.
			float t = Math.min(1.0F, tick / (float) (LENGTH - 20));
			// Only when standing in it, next to it the view would sink into the ground.
			if (level.getBlockState(player.blockPosition()).is(Blocks.POWDER_SNOW)) {
				moveCamera(new Vec3(0.0, -1.0 * t, 0.0));
			}
			particles(level, ParticleTypes.SNOWFLAKE, player.getEyePosition(), 6, 0.6, 0.05);
			if (tick % 2 == 0) {
				Vec3 shiver = player.getEyePosition().add(horizontalLook(player).scale(3.0)).add((random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.2, 0.0);
				turnTowards(player, shiver, 0.25F);
			}
			if (tick % 30 == 10) {
				sound(level, player.position(), SoundEvents.POWDER_SNOW_STEP, SoundSource.BLOCKS, 1.2F, 0.7F);
			}
		}

		if (particle != null && tick % 2 == 0) {
			particles(level, particle, body, burning ? 5 : 3, 0.5, 0.05);
		}

		if (tick > 0 && tick % HIT_EVERY == 0) {
			if (extra == Extra.FREEZE) {
				particles(level, ParticleTypes.SNOWFLAKE, player.getEyePosition(), 25, 0.5, 0.15);
				sound(level, player.position(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.6F);
			}
			// Every hit takes a fifth of the hearts, the last one kills.
			hit(level, player, hurtSound, 0.19F, tick + HIT_EVERY >= LENGTH);
		}
	}

	/** The fire catches on, spreads around the player, crackles and smokes. */
	private void burn(ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 8) {
			setOnFire(player);
			hurt(level, player, SoundEvents.PLAYER_HURT_ON_FIRE, 0.1F);
		}

		if (tick % 5 == 0 && tick < 110) {
			igniteNear(level, player, tick < 30 ? 1 : 2);
		}

		if (tick % 18 == 0) {
			sound(level, player.position(), SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.5F, 0.8F + random.nextFloat() * 0.3F);
		}

		if (extra == Extra.LAVA && source != null && tick % 7 == 0) {
			Vec3 lava = Vec3.atCenterOf(source).add(0, 0.6, 0);
			particles(level, ParticleTypes.LAVA, lava, 6, 0.4, 0.2);
			sound(level, lava, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 1.2F, 0.9F + random.nextFloat() * 0.3F);
		}

		particles(level, ParticleTypes.LARGE_SMOKE, player.position().add(0, 1.4, 0), 1, 0.4, 0.05);
	}
}
