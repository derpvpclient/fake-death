package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Mark Zuckerberg in a grey T-shirt comes over with a VR headset: "Welcome to the Metaverse!" He puts it on the player.
 * The sky turns purple, a cheerful synth tune plays, and cartoon avatars pop up all around, floating, waving, every one
 * of them without legs. "Have you bought legs yet? Only 9.99!" Then the world falls apart into flat coloured polygons,
 * and the player falls through into the nothing. "Connection to the Metaverse lost."
 */
public class ZuckerbergScene extends CastScene {
	private static final int WELCOME = ARRIVE + 8;
	private static final int HEADSET = ARRIVE + 50;
	private static final int ON = HEADSET + 20;
	private static final int AVATARS = 6;
	private static final int POP_EVERY = 8;
	private static final int LEGS = ON + 60;
	private static final int BREAK = LEGS + 60;
	private static final int FALL = 40;
	private static final int DEATH = BREAK + 20 + FALL;
	private static final ChatFormatting META = ChatFormatting.BLUE;
	/** The scene's own cheerful synth tune for the Metaverse. */
	private static final int[][] JINGLE = {{60, 2}, {64, 2}, {67, 2}, {72, 4}, {67, 2}, {64, 2}, {65, 2}, {69, 2}, {72, 4}, {69, 2}, {65, 2},
		{67, 2}, {71, 2}, {74, 4}, {71, 2}, {67, 2}, {60, 8}, {0, 4}};
	private static final float STEP = 2.0F;

	private final List<RemotePlayer> avatars = new ArrayList<>();
	private final List<Display.BlockDisplay> shards = new ArrayList<>();
	private RemotePlayer zuckerberg;
	private Vec3 look;
	private float purple;
	private float dark;

	@Override
	public int length() {
		return DEATH + 2;
	}

	@Override
	public int photoTick() {
		return LEGS + 10;
	}

	@Override
	public int skyTint() {
		return (int) (purple * 190.0F) << 24 | 0x7A2AD8;
	}

	@Override
	public int screenTint() {
		// The headset: the world a little darker, then black as the player falls.
		return dark > 0.0F ? (int) (Math.min(1.0F, dark) * 240.0F) << 24 : purple > 0.0F ? 0x30100828 : 0;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.zuckerberg", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (zuckerberg == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();
		face(zuckerberg, eyes);

		if (tick == WELCOME) {
			pose(zuckerberg, Skins.Pose.CHEER);
			speak(level, zuckerberg, "fakedeath.zuckerberg.welcome", META, 45);
		}
		if (tick == WELCOME + 30) {
			pose(zuckerberg, null);
		}
		if (tick > WELCOME + 20 && tick < HEADSET) {
			// Up close with the headset in his hands.
			stepTowards(level, player, zuckerberg, player.position().add(look.scale(1.4)), 0.08);
		}
		if (tick == HEADSET) {
			Looks.swing(zuckerberg);
			speak(level, zuckerberg, "fakedeath.zuckerberg.puton", META, 30);
		}
		if (tick == ON) {
			// The headset is on: everything goes purple.
			hold(zuckerberg, ItemStack.EMPTY);
			wear(zuckerberg, Looks.prop(Items.IRON_NUGGET, "vr_headset"), 0.0);
			Scene.sound(level, eyes, SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 1.0F, 1.6F);
		}
		if (tick >= ON) {
			purple = Math.min(1.0F, (tick - ON) / 20.0F);
			if (tick < BREAK) {
				Band.play(level, eyes, JINGLE, (tick - ON) % Band.length(JINGLE, STEP), STEP, 0.8F, Band.BIT, Band.CHIME);
			}
		}

		// The avatars pop up around the player, floating, bobbing, waving.
		int popped = tick - ON - 10;
		if (popped >= 0 && popped % POP_EVERY == 0 && popped / POP_EVERY < AVATARS) {
			int i = popped / POP_EVERY;
			float angle = (i - (AVATARS - 1) / 2.0F) * 0.55F;
			Vec3 at = player.position().add(look.yRot(angle).scale(2.6 + (i % 2) * 0.8)).add(0.0, 0.9, 0.0);
			RemotePlayer avatar = extra(level, i % 2 == 0 ? "avatar" : "avatar2", at);
			pose(avatar, i % 3 == 0 ? Skins.Pose.CHEER : Skins.Pose.RAISE);
			avatars.add(avatar);
			particles(level, ParticleTypes.END_ROD, at.add(0.0, 1.0, 0.0), 12, 0.4, 0.05);
			Scene.sound(level, at, Band.PLING, SoundSource.RECORDS, 0.8F, 1.2F + i * 0.1F);
		}
		for (int i = 0; i < avatars.size(); i++) {
			RemotePlayer avatar = avatars.get(i);
			float angle = (i - (AVATARS - 1) / 2.0F) * 0.55F;
			double distance = tick < LEGS ? 2.6 + (i % 2) * 0.8 : Math.max(1.3, 2.6 + (i % 2) * 0.8 - (tick - LEGS) * 0.02);
			Vec3 at = player.position().add(look.yRot(angle).scale(distance)).add(0.0, 0.9 + Math.sin(tick * 0.15 + i) * 0.15, 0.0);
			if (tick >= BREAK) {
				at = at.add(0.0, -(tick - BREAK) * 0.15, 0.0);
			}
			avatar.setPos(at.x, at.y, at.z);
			face(avatar, eyes);
		}
		if (tick == LEGS && avatars.size() > 1) {
			speak(level, zuckerberg, "fakedeath.zuckerberg.legs", META, 50);
		}

		// The world breaks into flat coloured polygons and falls away, the player with it.
		if (tick == BREAK) {
			Scene.sound(level, eyes, SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 2.0F, 0.5F);
			Scene.sound(level, eyes, Band.BIT, SoundSource.RECORDS, 1.5F, 0.5F);
			BlockState[] colors = {Blocks.CONCRETE.purple().defaultBlockState(), Blocks.CONCRETE.magenta().defaultBlockState(),
				Blocks.CONCRETE.cyan().defaultBlockState(), Blocks.CONCRETE.blue().defaultBlockState(), Blocks.CONCRETE.lightBlue().defaultBlockState()};
			for (int i = 0; i < 24; i++) {
				Display.BlockDisplay shard = blockDisplay(level, colors[i % colors.length], player.position(), 16.0F);
				Looks.glow(shard);
				shards.add(shard);
			}
		}
		for (int i = 0; i < shards.size(); i++) {
			int since = tick - BREAK;
			double angle = i * 2.39996;
			double r = 0.8 + (i % 6) * 0.7;
			Vec3 at = player.position().add(Math.cos(angle) * r, -0.05 - since * since * 0.004 * (1 + i % 3), Math.sin(angle) * r);
			place(shards.get(i), at, new Vector3f(1.2F, 0.05F, 0.7F), new Quaternionf().rotateY((float) angle).rotateX(since * 0.05F * (i % 4 - 1.5F)).rotateZ(0.6F));
		}
		if (tick >= BREAK + 20) {
			float t = Math.min(1.0F, (tick - BREAK - 20) / (float) FALL);
			moveCamera(new Vec3(0.0, -12.0 * t * t, 0.0));
			dark = t;
		}
		if (tick == BREAK + 40) {
			say(minecraft, Component.literal("Metaverse").withStyle(META), Component.translatable("fakedeath.zuckerberg.lost"));
		}
		if (tick == DEATH) {
			moveCamera(Vec3.ZERO);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}

		if (tick < ON + 10) {
			turnTowards(player, zuckerberg.getEyePosition(), 0.1F);
		} else if (tick < BREAK && !avatars.isEmpty()) {
			RemotePlayer next = avatars.get(Math.min(avatars.size() - 1, (tick - ON - 10) / 20));
			turnTowards(player, next.getEyePosition(), 0.08F);
		} else {
			turnTowards(player, eyes.add(look.scale(2.0)).add(0.0, -3.0, 0.0), 0.1F);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		look = horizontalLook(player);
		zuckerberg = actor(level, "fakedeath.zuckerberg.name", "zuckerberg", false, onGround(level, player, player.position().add(look.scale(4.0))));
		hold(zuckerberg, Looks.prop(Items.IRON_NUGGET, "vr_headset"));
		arrive(level, player, zuckerberg, 0);
	}
}
