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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A prank influencer puts a cartoon bomb down next to the player: "Only cardboard!" He lights it, runs off filming and
 * counts down. The fuse fizzles out, nothing happens, "PRANK!", he laughs and comes back. Then it starts hissing again.
 */
public class BombPrankScene extends NpcScene {
	private static final int LIGHT = 6;
	private static final int COUNTDOWN = 24;
	private static final int FIZZLE = 70;
	private static final int LAUGH = FIZZLE + 6;
	private static final int HISS = LAUGH + 50;
	private static final int BOOM = HISS + 26;
	private static final int OOPS = BOOM + 30;
	private static final float FUSE = 0.32F;

	private Display.BlockDisplay bomb;
	private Display.BlockDisplay bombRound;
	private Display.BlockDisplay fuse;
	private Vec3 bombAt;
	private Vec3 runFrom;
	private Vec3 runTo;
	private Vec3 backTo;
	private Vec3 lookAt;
	private boolean gone;

	public BombPrankScene() {
		super("bombenprank", ChatFormatting.LIGHT_PURPLE);
	}

	@Override
	protected int lineCount() {
		return 3;
	}

	@Override
	protected int actLength() {
		return OOPS + 30;
	}

	@Override
	protected int photoOffset() {
		return HISS + 10;
	}

	@Override
	protected ResourceKey<VillagerProfession> profession() {
		return VillagerProfession.NITWIT;
	}

	@Override
	protected ItemStack held() {
		// The phone he films with.
		return new ItemStack(Items.SPYGLASS);
	}

	@Override
	protected Vec3 focus(LocalPlayer player) {
		return lookAt != null ? lookAt : npc.getEyePosition();
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		super.tick(minecraft, level, player, tick);
		if (npc != null && tick == actStart() - 2 * LINE + 20) {
			// While he says the second line.
			// Puts the bomb down between himself and the player.
			Looks.swing(npc);
			Vec3 toward = npc.position().subtract(player.position());
			Vec3 flat = new Vec3(toward.x, 0.0, toward.z).normalize();
			Vec3 spot = player.position().add(flat.scale(1.4));
			bombAt = new Vec3(spot.x, floor(level, spot.x, spot.z, player.getY()), spot.z);
			bomb = blockDisplay(level, Blocks.COAL_BLOCK.defaultBlockState(), bombAt, 1.0F);
			bombRound = blockDisplay(level, Blocks.COAL_BLOCK.defaultBlockState(), bombAt, 1.0F);
			fuse = blockDisplay(level, Blocks.OAK_PLANKS.defaultBlockState(), bombAt, 1.0F);
			sound(level, bombAt, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
		}
		if (bomb != null && !gone) {
			placeBomb(tick);
		}
	}

	/** A round black bomb, two turned cubes, with a fuse on top that burns down. */
	private void placeBomb(int tick) {
		int t = tick - actStart();
		float swell = t >= HISS ? 1.0F + Mth.sin((t - HISS) * 0.9F) * 0.06F * (t - HISS) / (float) (BOOM - HISS) : 1.0F;
		Vec3 center = bombAt.add(0.0, 0.22 * swell, 0.0);
		place(bomb, center, new Vector3f(0.42F * swell), new Quaternionf());
		place(bombRound, center, new Vector3f(0.42F * swell), new Quaternionf().rotateY(Mth.PI / 4.0F).rotateX(Mth.PI / 4.0F));
		float left = t < LIGHT ? 1.0F : t < FIZZLE ? 1.0F - 0.75F * (t - LIGHT) / (FIZZLE - LIGHT) : t < HISS ? 0.25F : Math.max(0.05F, 0.25F - 0.2F * (t - HISS) / (BOOM - HISS));
		float length = FUSE * left;
		place(fuse, center.add(0.0, 0.25 * swell + length / 2.0, 0.0), new Vector3f(0.05F, length, 0.05F), new Quaternionf().rotateZ(0.3F));
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		if (bomb == null) {
			return;
		}
		Vec3 away = npc.position().subtract(player.position());
		away = new Vec3(away.x, 0.0, away.z).normalize();

		if (t == LIGHT) {
			Looks.swing(npc);
			sound(level, bombAt, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
			runFrom = npc.position();
			Vec3 far = player.position().add(away.scale(7.0));
			runTo = new Vec3(far.x, floor(level, far.x, far.z, player.getY()), far.z);
		}
		if (runFrom != null && t > LIGHT && t <= COUNTDOWN) {
			// Off he runs, filming over his shoulder.
			float p = (t - LIGHT) / (float) (COUNTDOWN - LIGHT);
			walk(npc, runFrom.lerp(runTo, p));
			face(npc, runTo.add(away));
		}
		if (t > COUNTDOWN && t < BOOM) {
			face(npc, player.getEyePosition());
		}
		if (t >= LIGHT && t < FIZZLE || t >= HISS && t < BOOM) {
			// The burning fuse: sparks and a fizz.
			Vec3 tip = bombAt.add(0.0, 0.5, 0.0);
			particles(level, ParticleTypes.FLAME, tip, 1, 0.03, 0.02);
			particles(level, ParticleTypes.SMOKE, tip, 1, 0.03, 0.02);
			if (t % 8 == 0) {
				sound(level, tip, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.6F, 1.6F);
			}
		}
		if (t == COUNTDOWN) {
			speak(minecraft, level, "fakedeath.bombenprank.countdown");
		}
		if (t == FIZZLE) {
			// Pff. Nothing.
			sound(level, bombAt, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.4F);
			particles(level, ParticleTypes.SMOKE, bombAt.add(0.0, 0.5, 0.0), 8, 0.1, 0.02);
		}
		if (t == LAUGH) {
			speak(minecraft, level, "fakedeath.bombenprank.prank");
			backTo = npc.position().lerp(player.position(), 0.5);
		}
		if (backTo != null && t > LAUGH && t < HISS) {
			// Comes back laughing, hopping.
			float p = Math.min(1.0F, (t - LAUGH) / 30.0F);
			Vec3 at = runTo.lerp(backTo, p);
			walk(npc, new Vec3(at.x, floor(level, at.x, at.z, player.getY()) + Math.abs(Mth.sin(t * 0.5F)) * 0.2, at.z));
			if (t % 12 == 0) {
				sound(level, npc.position(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.4F);
			}
		}
		if (t == HISS) {
			sound(level, bombAt, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.2F, 1.0F);
			sound(level, npc.position(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
			speak(minecraft, level, "fakedeath.bombenprank.uh");
		}
		if (t == BOOM) {
			gone = true;
			despawn(level, bomb);
			despawn(level, bombRound);
			despawn(level, fuse);
			level.addParticle(ParticleTypes.EXPLOSION_EMITTER, bombAt.x, bombAt.y + 0.5, bombAt.z, 1.0, 0.0, 0.0);
			particles(level, ParticleTypes.EXPLOSION, bombAt.add(0.0, 0.5, 0.0), 8, 1.5, 0.0);
			sound(level, bombAt, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F, 0.8F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
		if (t > BOOM && t < BOOM + 10) {
			// Blown back, singed.
			Vec3 at = npc.position().add(away.scale(0.25));
			walk(npc, new Vec3(at.x, floor(level, at.x, at.z, player.getY()), at.z));
			particles(level, ParticleTypes.LARGE_SMOKE, npc.position().add(0.0, 1.0, 0.0), 1, 0.3, 0.02);
		}
		if (t == OOPS) {
			speak(minecraft, level, "fakedeath.bombenprank.real");
		}

		// The camera: the bomb while it burns, him while he talks.
		if (t < COUNTDOWN || t >= FIZZLE - 10 && t < LAUGH || t >= HISS && t <= BOOM) {
			lookAt = bombAt.add(0.0, 0.3, 0.0);
		} else {
			lookAt = npc.getEyePosition();
		}
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		gone = true;
		super.end(level, player);
	}
}
