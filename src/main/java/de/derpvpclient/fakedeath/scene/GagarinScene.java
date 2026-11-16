package de.derpvpclient.fakedeath.scene;

import de.derpvpclient.fakedeath.mixin.DisplayAccessor;
import de.derpvpclient.fakedeath.mixin.TextDisplayAccessor;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Baikonur, 12 April 1961. The Vostok rocket on its pad, white and grey, "СССР" down its side. Yuri Gagarin in his
 * orange suit and white helmet walks out, waves with his famous smile, and climbs up into the capsule. Mission
 * control counts down, the engines roar, and over the radio: "Poyekhali!" ("Let's go!"). The rocket lifts off. The
 * player stood far too close: the blast of fire and smoke rolls across the ground and over them.
 */
public class GagarinScene extends CastScene {
	private static final int WAVE = ARRIVE + 6;
	private static final int READY = ARRIVE + 12;
	private static final int BOARD = ARRIVE + 50;
	private static final int CLIMB = 24;
	private static final int[] COUNT = {BOARD + CLIMB + 14, BOARD + CLIMB + 34, BOARD + CLIMB + 54};
	private static final int IGNITE = BOARD + CLIMB + 74;
	private static final int LIFT = IGNITE + 26;
	private static final int BLAST_TICKS = 30;
	private static final int DEATH = LIFT + BLAST_TICKS + 8;
	private static final ChatFormatting RU = ChatFormatting.GOLD;
	private static final ChatFormatting CONTROL = ChatFormatting.GRAY;

	private final List<Part> rocket = new ArrayList<>();
	private RemotePlayer gagarin;
	private Vec3 pad;
	private Vec3 mark;
	private Vec3 look;
	private float yaw;
	private float burn;

	@Override
	public int length() {
		return DEATH + 1;
	}

	@Override
	public int photoTick() {
		return LIFT + 12;
	}

	@Override
	public int screenTint() {
		return burn > 0.0F ? ((int) (Math.min(burn, 1.0F) * 220.0F) << 24) | 0xFF7A1A : 0;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.gagarin", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (gagarin == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();

		if (tick < BOARD) {
			face(gagarin, eyes);
		}
		if (tick == WAVE) {
			pose(gagarin, Skins.Pose.RAISE);
		}
		if (tick == READY) {
			speak(level, gagarin, "fakedeath.gagarin.ready", RU, 40);
		}
		if (tick == BOARD) {
			pose(gagarin, null);
		}
		if (tick >= BOARD && tick < BOARD + CLIMB) {
			// Up the gantry and into the capsule at the top.
			float p = (tick - BOARD) / (float) CLIMB;
			Vec3 foot = pad.add(look.scale(-1.6));
			Vec3 top = pad.add(look.scale(-0.9)).add(0.0, 9.5, 0.0);
			if (mark == null) {
				mark = gagarin.position();
			}
			// Over to the gantry first, then up.
			Vec3 at = p < 0.4F ? mark.lerp(foot, p / 0.4F) : foot.lerp(top, (p - 0.4F) / 0.6F);
			walk(gagarin, at);
			face(gagarin, pad.add(0.0, at.y - pad.y + 1.5, 0.0));
		}
		if (tick == BOARD + CLIMB) {
			// Inside the capsule now.
			gagarin.setInvisible(true);
			showName(gagarin, false);
			sound(level, pad.add(0.0, 9.0, 0.0), SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.5F, 0.8F);
		}
		for (int i = 0; i < COUNT.length; i++) {
			if (tick == COUNT[i]) {
				say(minecraft, Component.translatable("fakedeath.gagarin.control").withStyle(CONTROL), Component.translatable("fakedeath.gagarin.count" + (3 - i)));
				sound(level, eyes, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 0.8F, 1.0F);
			}
		}
		if (tick >= IGNITE) {
			// Fire and smoke at the bottom, the ground shaking.
			Vec3 base = pad.add(0.0, 0.5, 0.0);
			for (int i = 0; i < 6; i++) {
				level.addParticle(ParticleTypes.FLAME, base.x + (random.nextDouble() - 0.5) * 2.0, base.y, base.z + (random.nextDouble() - 0.5) * 2.0,
					(random.nextDouble() - 0.5) * 0.4, -0.05, (random.nextDouble() - 0.5) * 0.4);
				level.addParticle(ParticleTypes.LARGE_SMOKE, base.x + (random.nextDouble() - 0.5) * 3.0, base.y, base.z + (random.nextDouble() - 0.5) * 3.0,
					(random.nextDouble() - 0.5) * 0.1, 0.02, (random.nextDouble() - 0.5) * 0.1);
			}
			cameraShake(Math.min(0.35F, 0.05F + (tick - IGNITE) * 0.006F));
			if (tick % 6 == 0) {
				sound(level, base, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3.0F, 0.4F);
				sound(level, base, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0F, 0.3F);
			}
		}
		if (tick == IGNITE) {
			say(minecraft, Component.translatable("fakedeath.gagarin.control").withStyle(CONTROL), Component.translatable("fakedeath.gagarin.ignition"));
		}
		if (tick == IGNITE + 14) {
			say(minecraft, Component.translatable("fakedeath.gagarin.name").withStyle(RU), Component.translatable("fakedeath.gagarin.poyekhali"));
			sound(level, eyes, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 3.0F, 0.5F);
		}
		double rise = tick >= LIFT ? Math.pow((tick - LIFT) / 20.0, 2.0) * 6.0 : 0.0;
		if (tick >= LIFT) {
			for (Part part : rocket) {
				place(part.display(), part.at().add(0.0, rise, 0.0), part.size(), new Quaternionf().rotateY(yaw + part.turn()));
			}
			// The trail of fire under the rising rocket.
			Vec3 tail = pad.add(0.0, rise, 0.0);
			for (int i = 0; i < 5; i++) {
				level.addParticle(ParticleTypes.FLAME, tail.x + (random.nextDouble() - 0.5) * 1.2, tail.y, tail.z + (random.nextDouble() - 0.5) * 1.2, 0.0, -0.4, 0.0);
			}
			// The blast rolls out over the ground, towards the player.
			float p = Math.min(1.0F, (tick - LIFT) / (float) BLAST_TICKS);
			double reach = p * pad.distanceTo(player.position()) * 1.1;
			for (int i = 0; i < 26; i++) {
				double a = random.nextDouble() * Math.PI * 2.0;
				double r = reach * (0.7 + random.nextDouble() * 0.3);
				level.addParticle(i % 3 == 0 ? ParticleTypes.FLAME : ParticleTypes.LARGE_SMOKE, pad.x + Math.cos(a) * r, pad.y + 0.3 + random.nextDouble(), pad.z + Math.sin(a) * r,
					Math.cos(a) * 0.15, 0.02, Math.sin(a) * 0.15);
			}
			if (reach >= pad.distanceTo(player.position()) - 1.0) {
				burn = Math.min(1.0F, burn + 0.12F);
				if (tick % 4 == 0) {
					hurt(level, player, SoundEvents.PLAYER_HURT_ON_FIRE, 0.25F);
				}
			}
		}
		if (tick < BOARD + 4) {
			turnTowards(player, gagarin.getEyePosition(), 0.08F);
		} else if (tick < LIFT) {
			turnTowards(player, pad.add(0.0, tick < BOARD + CLIMB ? 5.0 : 7.0, 0.0), 0.06F);
		} else {
			turnTowards(player, pad.add(0.0, 6.0 + rise * 0.8, 0.0), 0.08F);
		}
		if (tick == DEATH) {
			kill(level, player, SoundEvents.PLAYER_HURT_ON_FIRE);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		yaw = (float) Math.atan2(look.x, look.z);
		pad = onGround(level, player, player.position().add(look.scale(10.0)));
		Quaternionf turn = new Quaternionf().rotateY(yaw);
		// The pad: a dark concrete slab.
		place(blockDisplay(level, Looks.GRAY_CONCRETE, pad, 12.0F), pad.add(0.0, 0.1, 0.0), new Vector3f(5.0F, 0.2F, 5.0F), turn);
		// The rocket: a white core, four grey boosters round its bottom, the capsule and its cone on top.
		BlockState white = Looks.WHITE_CONCRETE;
		BlockState grey = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
		add(level, white, pad.add(0.0, 5.0, 0.0), new Vector3f(1.3F, 9.0F, 1.3F));
		// The boosters stand on the diagonals, so none of them hides the letters on the side towards the player.
		for (int s : new int[] {-1, 1}) {
			for (int t : new int[] {-1, 1}) {
				Vec3 corner = side.scale(s).add(look.scale(t)).normalize().scale(1.05);
				add(level, grey, pad.add(corner).add(0.0, 2.6, 0.0), new Vector3f(0.8F, 4.4F, 0.8F), Mth.PI / 4.0F);
			}
		}
		add(level, grey, pad.add(0.0, 9.9, 0.0), new Vector3f(1.0F, 0.8F, 1.0F));
		add(level, white, pad.add(0.0, 10.6, 0.0), new Vector3f(0.6F, 0.6F, 0.6F));
		add(level, white, pad.add(0.0, 11.1, 0.0), new Vector3f(0.25F, 0.5F, 0.25F));
		// "СССР" down its side, towards the player.
		String word = "СССР";
		for (int i = 0; i < word.length(); i++) {
			// One big letter under the other, from the top down.
			Vec3 at = pad.add(look.scale(-0.67)).add(0.0, 8.3 - i * 0.95, 0.0);
			Display.TextDisplay letter = spawn(level, EntityType.TEXT_DISPLAY, at);
			((TextDisplayAccessor) letter).fakedeath$setText(Component.literal(word.substring(i, i + 1)).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
			((TextDisplayAccessor) letter).fakedeath$setBackgroundColor(0);
			((DisplayAccessor) letter).fakedeath$setInterpolationDuration(1);
			rocket.add(new Part(letter, at, new Vector3f(3.2F), Mth.PI));
			place(letter, at, new Vector3f(3.2F), new Quaternionf().rotateY(yaw + Mth.PI));
		}
		// The gantry beside it, staying on the ground.
		Vec3 gantry = pad.add(look.scale(-1.6)).add(side.scale(1.4));
		place(blockDisplay(level, Blocks.RED_CONCRETE.defaultBlockState(), gantry, 12.0F), gantry.add(0.0, 5.0, 0.0), new Vector3f(0.5F, 10.0F, 0.5F), turn);
		place(blockDisplay(level, Blocks.IRON_BARS.defaultBlockState(), gantry, 12.0F), gantry.add(side.scale(-0.7)).add(0.0, 9.6, 0.0), new Vector3f(1.0F, 0.2F, 0.6F), turn);
		standFlag(level, player, pad.add(look.scale(-3.0)).add(side.scale(-3.0)), "flag_soviet", 1.2F);

		gagarin = actor(level, "fakedeath.gagarin.name", "gagarin", false, onGround(level, player, player.position().add(look.scale(4.0)).add(side.scale(0.6))));
		arrive(level, player, gagarin, 0);
	}

	private void add(ClientLevel level, BlockState state, Vec3 at, Vector3f size) {
		add(level, state, at, size, 0.0F);
	}

	private void add(ClientLevel level, BlockState state, Vec3 at, Vector3f size, float turn) {
		Display.BlockDisplay display = blockDisplay(level, state, at, 24.0F);
		place(display, at, size, new Quaternionf().rotateY(yaw + turn));
		rocket.add(new Part(display, at, size, turn));
	}

	private record Part(Display display, Vec3 at, Vector3f size, float turn) {
	}
}
