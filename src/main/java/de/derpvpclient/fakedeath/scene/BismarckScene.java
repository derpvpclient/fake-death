package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The Prussian parliament, 1862. "Preußens Gloria" plays while Otto von Bismarck strides to the lectern. "Not by
 * speeches and majority decisions are the great questions of the day decided ..." He bangs on the lectern: "... but
 * by iron and blood!" And iron it is: blocks of iron come down out of the sky all around the player, closer and
 * closer. "Politics is the art of the possible." The last one is the biggest.
 */
public class BismarckScene extends CastScene {
	private static final int SPEECH = ARRIVE + 10;
	private static final int IRON = ARRIVE + 78;
	private static final int DROPS = 9;
	private static final int DROP_EVERY = 11;
	private static final int FALL = 16;
	private static final int ART = IRON + DROPS * DROP_EVERY - 30;
	private static final int LAST = IRON + DROPS * DROP_EVERY + 14;
	private static final ChatFormatting PRUSSIA = ChatFormatting.GRAY;
	/** "Preußens Gloria" (Gottfried Piefke, 1871), as in the score, in D flat. */
	private static final int[][] GLORIA = {{68, 2}, {68, 2}, {73, 4}, {68, 4}, {65, 4}, {68, 4}, {65, 4}, {68, 4}, {73, 4}, {77, 4},
		{75, 4}, {75, 2}, {75, 2}, {75, 4}, {73, 2}, {75, 2}, {77, 4}, {75, 4}, {73, 8}};
	/** Two bars a chord: D flat, D flat, A flat seventh, D flat. */
	private static final int[][] CHORDS = {{61, 65, 68}, {61, 65, 68}, {60, 63, 66, 68}, {61, 65, 68}};
	private static final int[] BASS = {37, 37, 32, 37};
	private static final float STEP = 1.5F;

	private final List<Drop> drops = new ArrayList<>();
	private final List<RemotePlayer> deputies = new ArrayList<>();
	private RemotePlayer bismarck;
	private Vec3 lectern;
	private int dropped;

	@Override
	public int length() {
		return LAST + 4;
	}

	@Override
	public int photoTick() {
		return IRON + 30;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.bismarck", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (bismarck == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();
		march(level, tick - 2);

		face(bismarck, eyes);
		for (RemotePlayer deputy : deputies) {
			face(deputy, tick < IRON ? bismarck.getEyePosition() : eyes.add(0.0, 4.0, 0.0));
		}
		if (tick == SPEECH) {
			speak(level, bismarck, "fakedeath.bismarck.speeches", PRUSSIA, 60);
		}
		if (tick == IRON) {
			Looks.swing(bismarck);
			pose(bismarck, Skins.Pose.RAISE);
			speak(level, bismarck, "fakedeath.bismarck.iron", ChatFormatting.DARK_RED, 45);
			Scene.sound(level, lectern, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.NEUTRAL, 1.2F, 0.6F);
			cameraShake(0.1F);
		}
		if (tick == IRON + 4) {
			cameraShake(0.0F);
		}
		if (tick == ART) {
			pose(bismarck, null);
			speak(level, bismarck, "fakedeath.bismarck.art", PRUSSIA, 40);
		}

		// Iron out of the sky, all around, each one a little closer; the last and biggest right on the player.
		int since = tick - IRON - 10;
		if (since >= 0 && since % DROP_EVERY == 0 && dropped < DROPS) {
			boolean last = dropped == DROPS - 1;
			double distance = last ? 0.0 : 4.5 - dropped * 0.45;
			float angle = dropped * 2.3F;
			Vec3 to = last ? player.position() : onGround(level, player, player.position().add(horizontalLook(player).yRot(angle).scale(distance)));
			float size = last ? 1.6F : 0.9F + random.nextFloat() * 0.3F;
			BlockState iron = dropped % 3 == 2 ? Blocks.ANVIL.defaultBlockState() : Blocks.IRON_BLOCK.defaultBlockState();
			Display.BlockDisplay block = blockDisplay(level, iron, to.add(0.0, 30.0, 0.0), 40.0F);
			Looks.farView(block);
			drops.add(new Drop(block, to, tick, size, last));
			Scene.sound(level, to.add(0.0, 10.0, 0.0), SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 0.6F, 0.5F);
			dropped++;
		}
		for (Iterator<Drop> it = drops.iterator(); it.hasNext();) {
			Drop drop = it.next();
			float t = Math.min(1.0F, (tick - drop.start()) / (float) FALL);
			Vec3 at = drop.to().add(0.0, drop.size() * 0.5 + 30.0 * (1.0F - t * t), 0.0);
			place(drop.display(), at, new Vector3f(drop.size()), new Quaternionf().rotateY(drop.start() * 0.7F));
			if (t >= 1.0F) {
				// The crash: sparks, dust, the ground shakes under the player.
				Scene.sound(level, drop.to(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 2.0F, 0.6F + random.nextFloat() * 0.2F);
				Scene.sound(level, drop.to(), Band.DRUM, SoundSource.RECORDS, 1.5F, 0.5F);
				particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()), drop.to().add(0.0, 0.3, 0.0), 20, 0.6, 0.2);
				particles(level, ParticleTypes.LAVA, drop.to().add(0.0, 0.3, 0.0), 4, 0.3, 0.1);
				cameraShake(drop.last() ? 0.4F : 0.15F);
				if (drop.last()) {
					kill(level, player, SoundEvents.PLAYER_HURT);
				} else if (dropped % 3 == 0) {
					hurt(level, player, SoundEvents.PLAYER_HURT, 0.15F);
				}
				it.remove();
			} else if (t > 0.1F && (tick - drop.start()) == FALL / 2) {
				cameraShake(0.0F);
			}
		}

		if (tick < IRON + 6) {
			turnTowards(player, bismarck.getEyePosition(), 0.08F);
		} else if (!drops.isEmpty()) {
			// Looking up at what comes down next.
			Drop next = drops.getLast();
			turnTowards(player, next.display().position(), drops.getLast().last() ? 0.25F : 0.1F);
		}
	}

	/** The march as he strides in, with a chord, the bass and the drum on every bar. */
	private void march(ClientLevel level, int since) {
		if (since < 0 || since >= Band.length(GLORIA, STEP) || bismarck == null) {
			return;
		}
		Vec3 at = lectern;
		Band.play(level, at, GLORIA, since, STEP, 1.1F, Band.TRUMPET, Band.FLUTE);
		int sinceBar = since - Math.round(4 * STEP);
		int bar = Math.round(8 * STEP);
		if (sinceBar >= 0 && sinceBar % bar == 0) {
			int index = Math.min(CHORDS.length - 1, sinceBar / bar / 2);
			Band.chord(level, at, Band.HARP, CHORDS[index], 0.35F);
			Band.note(level, at, Band.BASS, BASS[index], 0.9F);
			Scene.sound(level, at, Band.DRUM, SoundSource.RECORDS, 0.8F, 1.0F);
		} else if (sinceBar >= 0 && sinceBar % bar == bar / 2) {
			Scene.sound(level, at, Band.SNARE, SoundSource.RECORDS, 0.5F, 1.0F);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		Vec3 look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		float yaw = (float) Math.atan2(look.x, look.z);
		lectern = onGround(level, player, player.position().add(look.scale(4.5)));
		// The lectern of dark wood, the flag of the Empire behind it.
		place(blockDisplay(level, Blocks.DARK_OAK_PLANKS.defaultBlockState(), lectern, 3.0F), lectern.add(0.0, 0.55, 0.0), new Vector3f(0.9F, 1.1F, 0.6F), new Quaternionf().rotateY(yaw));
		place(blockDisplay(level, Blocks.DARK_OAK_PLANKS.defaultBlockState(), lectern, 3.0F), lectern.add(look.scale(0.05)).add(0.0, 1.15, 0.0), new Vector3f(1.0F, 0.1F, 0.7F),
			new Quaternionf().rotateY(yaw).rotateX(-0.25F));
		standFlag(level, player, lectern.add(look.scale(2.2)).add(side.scale(1.6)), "flag_empire", 1.2F);
		standFlag(level, player, lectern.add(look.scale(2.2)).add(side.scale(-1.6)), "flag_empire", 1.2F);
		bismarck = actor(level, "fakedeath.bismarck.name", "bismarck", false, onGround(level, player, lectern.add(look.scale(0.7))));
		wear(bismarck, Looks.prop(Items.IRON_NUGGET, "pickelhaube"), 0.45);
		arrive(level, player, bismarck, 0);
		for (int s : new int[] {-1, 1}) {
			RemotePlayer deputy = extra(level, s < 0 ? "general" : "bodyguard", onGround(level, player, lectern.add(side.scale(s * 2.6)).add(look.scale(-0.6))));
			arrive(level, player, deputy, s < 0 ? 6 : 10);
			deputies.add(deputy);
		}
	}

	private record Drop(Display.BlockDisplay display, Vec3 to, int start, float size, boolean last) {
	}
}
