package de.derpvpclient.fakedeath.scene;

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
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A keynote. A black stage, a huge white screen, soft piano music: Satie's first Gymnopédie. Steve Jobs in his black
 * turtleneck: "Today we are introducing three things: an iPod, a phone and an internet communicator." He holds it up:
 * "Are you getting it? These are not three devices. This is one device!" Then: "Oh, and ... one more thing." High above
 * the stage it comes down, slowly, enormous, glowing. It hovers over the player. Then it drops.
 */
public class JobsScene extends CastScene {
	private static final int THREE = ARRIVE + 8;
	private static final int ONE = ARRIVE + 75;
	private static final int MORE = ONE + 60;
	private static final int DESCEND = MORE + 30;
	private static final int HOVER = DESCEND + 60;
	private static final int DROP = HOVER + 15;
	private static final int DEATH = DROP + 8;
	private static final ChatFormatting STAGE = ChatFormatting.WHITE;
	/** Satie's first Gymnopédie (1888): two bars of the swaying chords, then the tune, slow. */
	private static final int[][] TUNE = {{0, 24}, {0, 4}, {66, 4}, {69, 4}, {67, 4}, {66, 4}, {61, 4}, {59, 4}, {61, 4}, {62, 4}, {57, 12}};
	private static final int[][] CHORDS = {{59, 62, 66}, {57, 61, 66}};
	private static final int[] BASS = {43, 38};
	private static final float STEP = 4.5F;

	private RemotePlayer jobs;
	private Display.TextDisplay screen;
	private Display.ItemDisplay giant;
	private Vec3 stage;
	private Vec3 look;

	@Override
	public int length() {
		return DEATH + 2;
	}

	@Override
	public int photoTick() {
		return HOVER;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.jobs", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (jobs == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();
		if (tick < DROP) {
			piano(level, tick - 2);
		}

		face(jobs, tick < DESCEND ? eyes : eyes.add(0.0, 6.0, 0.0));
		if (tick == THREE) {
			speak(level, jobs, "fakedeath.jobs.three", STAGE, 60);
			relabel(screen, Component.literal("iPod  ·  Phone  ·  Internet").withStyle(ChatFormatting.BLACK));
		}
		if (tick == ONE) {
			pose(jobs, Skins.Pose.RAISE);
			speak(level, jobs, "fakedeath.jobs.one", STAGE, 50);
			relabel(screen, Component.literal("1").withStyle(ChatFormatting.BLACK, ChatFormatting.BOLD));
		}
		if (tick == MORE) {
			pose(jobs, null);
			speak(level, jobs, "fakedeath.jobs.more", STAGE, 40);
			relabel(screen, Component.literal("One more thing …").withStyle(ChatFormatting.BLACK));
		}
		if (tick == DESCEND) {
			giant = itemDisplay(level, Looks.prop(Items.SPYGLASS, "phone"), eyes.add(0.0, 30.0, 0.0));
			Looks.farView(giant);
			Looks.glow(giant);
			Band.chord(level, eyes, Band.CHIME, new int[] {62, 66, 69, 74}, 1.0F);
		}
		if (giant != null) {
			Vec3 above = eyes.add(look.scale(0.6)).add(0.0, 4.5, 0.0);
			Vec3 at;
			if (tick < HOVER) {
				float t = (tick - DESCEND) / (float) (HOVER - DESCEND);
				at = eyes.add(look.scale(3.0)).add(0.0, 25.0, 0.0).lerp(above, 1.0 - (1.0 - t) * (1.0 - t));
			} else if (tick < DROP) {
				at = above.add(0.0, Math.sin(tick * 0.3) * 0.08, 0.0);
			} else {
				float t = Math.min(1.0F, (tick - DROP) / (float) (DEATH - DROP));
				at = above.lerp(eyes.add(0.0, 0.6, 0.0), t * t);
			}
			// Lying flat, screen down, like it is about to land on a table.
			place(giant, at, new Vector3f(9.0F), new Quaternionf().rotateY((float) Math.atan2(look.x, look.z)).rotateX(Mth.HALF_PI));
			if (tick == HOVER) {
				Scene.sound(level, eyes, Band.CHIME, SoundSource.RECORDS, 1.0F, 2.0F);
			}
		}
		if (tick == DEATH) {
			Scene.sound(level, eyes, SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 2.0F, 0.6F);
			Scene.sound(level, eyes, SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 1.5F, 0.8F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GLASS.defaultBlockState()), eyes, 40, 1.0, 0.3);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}

		if (tick < DESCEND) {
			turnTowards(player, tick > THREE + 20 && tick < ONE ? screen.position() : jobs.getEyePosition(), 0.07F);
		} else {
			turnTowards(player, giant.position(), 0.12F);
		}
	}

	/** The Gymnopédie: the bass on the first beat of each bar, the chord on the second, the tune over it, round and round. */
	private void piano(ClientLevel level, int since) {
		if (since < 0) {
			return;
		}
		int loop = Band.length(TUNE, STEP);
		int inLoop = since % loop;
		Band.play(level, stage, TUNE, inLoop, STEP, 0.9F, Band.HARP, Band.FLUTE);
		int bar = Math.round(12 * STEP);
		int beat = Math.round(4 * STEP);
		int which = (inLoop / bar) % 2;
		if (inLoop % bar == 0) {
			Band.note(level, stage, Band.BASS, BASS[which], 0.7F);
		} else if (inLoop % bar == beat) {
			Band.chord(level, stage, Band.HARP, CHORDS[which], 0.35F);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		look = horizontalLook(player);
		float yaw = (float) Math.atan2(look.x, look.z);
		stage = onGround(level, player, player.position().add(look.scale(4.5)));
		Vec3 wall = stage.add(look.scale(3.0));
		// The black stage wall with the big white screen.
		place(blockDisplay(level, Looks.BLACK_CONCRETE, wall, 16.0F), wall.add(0.0, 2.5, 0.0), new Vector3f(11.0F, 5.0F, 0.3F), new Quaternionf().rotateY(yaw));
		Display.BlockDisplay white = blockDisplay(level, Blocks.WHITE_CONCRETE.defaultBlockState(), wall, 8.0F);
		Looks.glow(white);
		place(white, wall.add(look.scale(-0.2)).add(0.0, 2.8, 0.0), new Vector3f(5.0F, 2.6F, 0.05F), new Quaternionf().rotateY(yaw));
		place(blockDisplay(level, Looks.BLACK_CONCRETE, stage, 12.0F), stage.add(0.0, 0.1, 0.0), new Vector3f(8.0F, 0.2F, 3.0F), new Quaternionf().rotateY(yaw));
		screen = sign(level, wall.add(look.scale(-0.26)).add(0.0, 2.6, 0.0), Component.literal(""), 2.2F, yaw, 0);
		jobs = actor(level, "fakedeath.jobs.name", "jobs", false, onGround(level, player, stage.add(look.scale(0.2))).add(0.0, 0.2, 0.0));
		hold(jobs, Looks.prop(Items.SPYGLASS, "phone"));
		arrive(level, player, jobs, 0);
	}
}
