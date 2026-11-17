package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Woolsthorpe, 1666. Isaac Newton reads under an apple tree. An apple falls on his head. He looks up: "A force that
 * pulls everything down ...!" Then: "I shall call it: gravity." He turns to the player: "Allow me a small
 * demonstration." An apple drops on the player's head. A bigger one. A much bigger one. Then the organ starts, Bach's
 * Toccata in D minor, and high above something enormous and red comes down.
 */
public class NewtonScene extends CastScene {
	private static final int BONK = ARRIVE + 12;
	private static final int FORCE = BONK + 16;
	private static final int GRAVITY = FORCE + 50;
	private static final int DEMO = GRAVITY + 45;
	private static final int[] APPLES = {DEMO + 30, DEMO + 52, DEMO + 76};
	private static final float[] SIZES = {0.5F, 1.2F, 2.6F};
	private static final int GIANT = DEMO + 110;
	private static final int GIANT_FALL = 60;
	private static final int DEATH = GIANT + GIANT_FALL;
	private static final int FALL = 12;
	private static final ChatFormatting ENGLAND = ChatFormatting.AQUA;
	/** The opening of Bach's Toccata in D minor (BWV 565), the first two phrases, on the high side; a tick a unit. */
	private static final int[][] TOCCATA = {{69, 2}, {67, 1}, {69, 9}, {0, 4}, {67, 1}, {65, 1}, {64, 1}, {62, 1}, {61, 3}, {62, 12}, {0, 6},
		{57, 2}, {55, 1}, {57, 9}, {0, 4}, {64, 1}, {65, 1}, {61, 1}, {62, 14}};

	private final List<Fall> falling = new ArrayList<>();
	private RemotePlayer newton;
	private Vec3 tree;
	private Display.ItemDisplay giant;
	private Vec3 look;

	@Override
	public int length() {
		return DEATH + 2;
	}

	@Override
	public int photoTick() {
		return GIANT + 30;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.newton", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (newton == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();

		face(newton, tick < BONK ? newton.getEyePosition().add(look.scale(-1.0)).add(0.0, -0.8, 0.0)
			: tick < FORCE ? newton.getEyePosition().add(0.0, 3.0, 0.0) : eyes);
		if (tick == BONK - FALL) {
			drop(level, newton.getEyePosition().add(0.0, 0.3, 0.0), 0.4F, tick, false);
		}
		if (tick == FORCE) {
			pose(newton, Skins.Pose.RAISE);
			speak(level, newton, "fakedeath.newton.force", ENGLAND, 45);
		}
		if (tick == GRAVITY) {
			pose(newton, null);
			speak(level, newton, "fakedeath.newton.gravity", ENGLAND, 40);
		}
		if (tick == DEMO) {
			pose(newton, Skins.Pose.POINT);
			speak(level, newton, "fakedeath.newton.demo", ENGLAND, 40);
		}
		for (int i = 0; i < APPLES.length; i++) {
			if (tick == APPLES[i] - FALL) {
				drop(level, eyes.add(0.0, 0.2 + SIZES[i] * 0.4, 0.0).add(look.scale(0.15)), SIZES[i], tick, true);
			}
		}
		for (int i = falling.size() - 1; i >= 0; i--) {
			Fall fall = falling.get(i);
			float t = Math.min(1.0F, (tick - fall.start()) / (float) FALL);
			Vec3 at = fall.to().add(0.0, 8.0 * (1.0F - t * t), 0.0);
			place(fall.display(), at, new Vector3f(fall.size()), new Quaternionf().rotateY(tick * 0.3F));
			if (t >= 1.0F) {
				Scene.sound(level, at, SoundEvents.WOOD_HIT, SoundSource.NEUTRAL, 1.0F + fall.size() * 0.3F, 1.4F / (0.6F + fall.size()));
				Scene.sound(level, at, Band.DRUM, SoundSource.RECORDS, 0.6F + fall.size() * 0.3F, 1.2F);
				particles(level, new ItemParticleOption(ParticleTypes.ITEM, Items.APPLE), at, 8 + (int) (fall.size() * 8), 0.3 * fall.size(), 0.1);
				if (fall.player()) {
					hurt(level, player, SoundEvents.PLAYER_HURT, 0.12F * fall.size());
					cameraShake(0.06F * fall.size());
				}
				despawn(level, fall.display());
				falling.remove(i);
			} else if (t > 0.5F) {
				cameraShake(0.0F);
			}
		}

		if (tick == GIANT) {
			giant = itemDisplay(level, new ItemStack(Items.APPLE), eyes.add(0.0, 40.0, 0.0));
			Looks.farView(giant);
			Scene.sound(level, eyes, SoundEvents.RAID_HORN.value(), SoundSource.NEUTRAL, 0.5F, 0.5F);
		}
		if (giant != null) {
			// Bach on the organ while it comes down: the tune high, the bass under it.
			int since = tick - GIANT;
			int midi = Band.noteAt(TOCCATA, since, 1.0F);
			if (midi > 0) {
				Band.note(level, eyes, Band.HARP, midi, 1.0F);
				Band.note(level, eyes, Band.FLUTE, midi, 0.6F);
				Band.note(level, eyes, Band.BASS, midi - 12, 0.9F);
			}
			float t = Math.min(1.0F, since / (float) GIANT_FALL);
			float drop = t < 0.7F ? t / 0.7F * 0.25F : 0.25F + (t - 0.7F) / 0.3F * 0.75F;
			Vec3 at = eyes.add(look.scale(0.4)).add(0.0, 2.2 + 38.0 * (1.0F - drop), 0.0);
			place(giant, at, new Vector3f(9.0F), new Quaternionf().rotateY(since * 0.05F));
			if (tick == DEATH) {
				Band.chord(level, eyes, Band.HARP, new int[] {62, 65, 69}, 1.0F);
				Band.note(level, eyes, Band.BASS, 38, 1.2F);
				Scene.sound(level, eyes, SoundEvents.WOOD_BREAK, SoundSource.NEUTRAL, 2.0F, 0.4F);
				particles(level, new ItemParticleOption(ParticleTypes.ITEM, Items.APPLE), eyes, 40, 1.2, 0.3);
				kill(level, player, SoundEvents.PLAYER_HURT);
			}
		}

		if (tick < DEMO + 20) {
			turnTowards(player, newton.getEyePosition(), 0.08F);
		} else if (giant == null) {
			// Up into the tree, where they come from.
			turnTowards(player, eyes.add(look.scale(0.6)).add(0.0, 2.5, 0.0), 0.12F);
		} else {
			turnTowards(player, giant.position(), 0.2F);
		}
	}

	private void drop(ClientLevel level, Vec3 to, float size, int tick, boolean player) {
		Display.ItemDisplay apple = itemDisplay(level, new ItemStack(Items.APPLE), to.add(0.0, 8.0, 0.0));
		Looks.farView(apple);
		falling.add(new Fall(apple, to, tick, size, player));
		Scene.sound(level, to.add(0.0, 3.0, 0.0), SoundEvents.AZALEA_LEAVES_STEP, SoundSource.NEUTRAL, 1.0F, 1.0F);
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		// An apple tree, with the player right under its crown and Newton at its trunk.
		tree = onGround(level, player, player.position().add(look.scale(2.4)).add(side.scale(1.2)));
		place(blockDisplay(level, Blocks.OAK_LOG.defaultBlockState(), tree, 6.0F), tree.add(0.0, 1.6, 0.0), new Vector3f(0.6F, 3.2F, 0.6F), new Quaternionf());
		for (int[] o : new int[][] {{0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {1, 0, 1}, {-1, 0, -1}, {-2, 0, -1}, {-1, 0, -2}}) {
			Vec3 at = tree.add(look.scale(-o[2] * 0.9 - 0.4)).add(side.scale(-o[0] * 0.9)).add(0.0, 3.6 + o[1] * 0.9, 0.0);
			place(blockDisplay(level, Blocks.AZALEA_LEAVES.defaultBlockState(), at, 6.0F), at, new Vector3f(1.3F), new Quaternionf());
		}
		for (int i = 0; i < 7; i++) {
			Vec3 at = tree.add(look.scale(-0.9 - (i % 3) * 0.5)).add(side.scale(-(i - 3) * 0.45)).add(0.0, 3.0 + (i % 2) * 0.3, 0.0);
			place(itemDisplay(level, new ItemStack(Items.APPLE), at), at, new Vector3f(0.4F), new Quaternionf().rotateY(i));
		}
		newton = actor(level, "fakedeath.newton.name", "newton", false, onGround(level, player, tree.add(look.scale(-0.3)).add(side.scale(-0.7))));
		hold(newton, new ItemStack(Items.BOOK));
		arrive(level, player, newton, 0);
	}

	private record Fall(Display.ItemDisplay display, Vec3 to, int start, float size, boolean player) {
	}
}
