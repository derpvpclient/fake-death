package de.derpvpclient.fakedeath.scene;

import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Three o'clock at night. A clock strikes three. Headlights in the dark: a black car, the "black raven", rolls up
 * slowly and stops. Doors. Two men in long leather coats come over. Three knocks, like at a door. "Citizen. You are
 * coming with us." They take the player by the arms and drag them to the car. The door slams. Darkness.
 */
public class BlackRavenScene extends CastScene {
	private static final int[] BELL = {5, 20, 35};
	private static final int DRIVE = 40;
	private static final int ARRIVE = DRIVE + 60;
	private static final int DOORS = ARRIVE + 10;
	private static final int[] KNOCKS = {DOORS + 30, DOORS + 38, DOORS + 46};
	private static final int CITIZEN = DOORS + 58;
	private static final int GRAB = CITIZEN + 35;
	private static final int DRAG = 30;
	private static final int SLAM = GRAB + DRAG + 4;
	private static final int DEATH = SLAM + 14;
	private static final ChatFormatting COLD = ChatFormatting.DARK_GRAY;

	private final List<Part> car = new ArrayList<>();
	private final List<RemotePlayer> agents = new ArrayList<>();
	private Vec3 from;
	private Vec3 stop;
	private Vec3 at;
	private float yaw;
	private float dark = 0.55F;

	@Override
	public int length() {
		return DEATH + 1;
	}

	@Override
	public int photoTick() {
		return CITIZEN + 10;
	}

	@Override
	public int screenTint() {
		return ((int) (Math.min(dark, 1.0F) * 235.0F) << 24) | 0x05070F;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.schwarzerrabe", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (car.isEmpty()) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();

		for (int bell : BELL) {
			if (tick == bell) {
				// Somewhere a clock strikes three.
				sound(level, eyes.add(horizontalLook(player).scale(-20.0)), SoundEvents.BELL_BLOCK, SoundSource.AMBIENT, 2.0F, 0.6F);
			}
		}
		if (tick >= DRIVE && tick <= ARRIVE) {
			float p = (tick - DRIVE) / (float) (ARRIVE - DRIVE);
			// Rolling up slowly, braking at the end.
			float eased = 1.0F - (1.0F - p) * (1.0F - p);
			at = from.lerp(stop, eased);
			if (tick % 12 == 0) {
				sound(level, at, SoundEvents.MINECART_RIDING, SoundSource.NEUTRAL, 1.5F, 0.4F);
			}
		}
		if (at != null) {
			placeCar(at);
		}
		if (tick == ARRIVE) {
			sound(level, at, SoundEvents.PISTON_CONTRACT, SoundSource.NEUTRAL, 1.0F, 0.5F);
		}
		if (tick == DOORS) {
			// Two doors, two men in leather coats.
			sound(level, at, SoundEvents.IRON_DOOR_OPEN, SoundSource.NEUTRAL, 1.2F, 0.6F);
			Vec3 side = horizontalLook(player).yRot(Mth.HALF_PI);
			for (int s : new int[] {-1, 1}) {
				agents.add(actor(level, "fakedeath.schwarzerrabe.agent", "nkvd", false, onGround(level, player, at.add(side.scale(s * 0.9)).add(horizontalLook(player).scale(-1.2)))));
			}
			sound(level, at, SoundEvents.IRON_DOOR_CLOSE, SoundSource.NEUTRAL, 1.2F, 0.6F);
		}
		for (int i = 0; i < agents.size(); i++) {
			RemotePlayer agent = agents.get(i);
			Vec3 side = horizontalLook(player).yRot(Mth.HALF_PI).scale(i == 0 ? -0.7 : 0.7);
			if (tick < GRAB) {
				stepTowards(level, player, agent, onGround(level, player, player.position().add(side).add(horizontalLook(player).scale(0.4))), 0.09);
				face(agent, eyes);
			} else {
				// Holding the player by the arms, walking them to the car.
				Vec3 body = player.position().add(de.derpvpclient.fakedeath.CameraDirector.positionOffsetNow());
				walk(agent, onGround(level, player, body.add(side)));
				face(agent, at);
			}
		}
		for (int knock : KNOCKS) {
			if (tick == knock) {
				sound(level, eyes, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.NEUTRAL, 0.7F, 0.9F);
			}
		}
		if (tick == CITIZEN) {
			speak(level, agents.getFirst(), "fakedeath.schwarzerrabe.citizen", COLD, 45);
		}
		if (tick == GRAB) {
			for (RemotePlayer agent : agents) {
				pose(agent, Skins.Pose.POINT);
			}
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.25F);
		}
		if (tick >= GRAB && tick < SLAM) {
			// Dragged across to the car, feet scraping over the ground.
			float p = Math.min(1.0F, (tick - GRAB) / (float) DRAG);
			Vec3 door = at.subtract(player.position()).scale(0.85);
			moveCamera(new Vec3(door.x * p, -0.2 * p, door.z * p));
			if (tick % 6 == 0) {
				sound(level, player.position(), SoundEvents.GRAVEL_STEP, SoundSource.PLAYERS, 1.0F, 0.7F);
			}
		}
		if (tick < GRAB) {
			turnTowards(player, agents.isEmpty() ? at.add(0.0, 1.0, 0.0) : agents.getFirst().getEyePosition(), 0.08F);
		} else {
			turnTowards(player, at.add(0.0, 0.8, 0.0), 0.15F);
		}
		if (tick > GRAB) {
			dark = Math.min(1.0F, 0.55F + (tick - GRAB) / (float) (SLAM - GRAB) * 0.45F);
		}
		if (tick == SLAM) {
			sound(level, at, SoundEvents.IRON_DOOR_CLOSE, SoundSource.NEUTRAL, 2.0F, 0.5F);
			dark = 1.0F;
		}
		if (tick == DEATH) {
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		Vec3 look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		stop = onGround(level, player, player.position().add(look.scale(4.5)));
		from = onGround(level, player, stop.add(side.scale(-28.0)));
		// The car drives in from the side, its long side towards the player.
		yaw = (float) Math.atan2(side.x, side.z);
		BlockState black = Looks.BLACK_CONCRETE;
		car.add(new Part(blockDisplay(level, black, from, 10.0F), new Vector3f(0.0F, 0.6F, 0.0F), new Vector3f(1.8F, 0.75F, 4.2F), false));
		car.add(new Part(blockDisplay(level, black, from, 10.0F), new Vector3f(0.0F, 1.25F, -0.2F), new Vector3f(1.6F, 0.6F, 2.2F), false));
		car.add(new Part(blockDisplay(level, Blocks.TINTED_GLASS.defaultBlockState(), from, 10.0F), new Vector3f(0.0F, 1.27F, -0.2F), new Vector3f(1.65F, 0.45F, 2.0F), false));
		for (int s : new int[] {-1, 1}) {
			// Headlights, burning bright in the dark.
			car.add(new Part(blockDisplay(level, Blocks.CONCRETE.yellow().defaultBlockState(), from, 10.0F), new Vector3f(s * 0.6F, 0.7F, 2.1F), new Vector3f(0.3F, 0.2F, 0.05F), true));
			for (int f : new int[] {-1, 1}) {
				car.add(new Part(blockDisplay(level, Looks.GRAY_CONCRETE, from, 10.0F), new Vector3f(s * 0.92F, 0.3F, f * 1.3F), new Vector3f(0.2F, 0.6F, 0.6F), false));
			}
		}
		for (Part part : car) {
			if (part.light()) {
				((de.derpvpclient.fakedeath.mixin.DisplayAccessor) part.display()).fakedeath$setBrightnessOverride(new Brightness(15, 15));
			}
		}
		at = from;
	}

	private void placeCar(Vec3 where) {
		Quaternionf turn = new Quaternionf().rotateY(yaw);
		for (Part part : car) {
			Vector3f center = new Vector3f(part.offset()).rotate(turn);
			Vector3f corner = new Vector3f(part.size()).mul(-0.5F).rotate(turn);
			part.display().setPos(where.x, where.y, where.z);
			transform(part.display(), new Transformation(center.add(corner), new Quaternionf(turn), part.size(), new Quaternionf()));
		}
	}

	private record Part(Display.BlockDisplay display, Vector3f offset, Vector3f size, boolean light) {
	}
}
