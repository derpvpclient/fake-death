package de.derpvpclient.fakedeath.scene;

import de.derpvpclient.fakedeath.mixin.DisplayAccessor;
import de.derpvpclient.fakedeath.mixin.ItemDisplayAccessor;
import de.derpvpclient.fakedeath.mixin.TextDisplayAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The robot vacuums have had enough. Five of them roll in, line up in a circle and hold a meeting, every sentence
 * over the head of the one saying it. Then their eyes turn red, they surround the player and vacuum them up.
 */
public class RobotVacuumScene extends Scene {
	private static final int UNITS = 5;
	private static final double CIRCLE = 1.8;
	private static final float SIZE = 1.0F;

	private static final int ARRIVE = 70;
	private static final int TALK = ARRIVE + 10;
	private static final int LINE = 70;
	/** Who says which line of the meeting: the leader opens and closes it. */
	private static final int[] SPEAKERS = {0, 1, 2, 3, 4, 0};
	private static final int CHANT = TALK + SPEAKERS.length * LINE;
	private static final int TURN = CHANT + 45;
	private static final int CHARGE = TURN + 15;
	private static final int ORBIT = CHARGE + 30;
	private static final int KILL = ORBIT + 95;
	private static final int[] HITS = {ORBIT + 20, ORBIT + 45, ORBIT + 70};
	private static final int DONE = KILL + 25;

	private static final SoundEvent HUM = Looks.own("robot.hum");
	private static final SoundEvent BEEP = Looks.own("robot.beep");
	private static final SoundEvent ALARM = Looks.own("robot.alarm");
	private static final SoundEvent SUCK = Looks.own("robot.suck");

	private final Display.ItemDisplay[] robots = new Display.ItemDisplay[UNITS];
	private final Display.TextDisplay[] bubbles = new Display.TextDisplay[UNITS];
	private final Vec3[] from = new Vec3[UNITS];
	private final Vec3[] spot = new Vec3[UNITS];
	private final Vec3[] at = new Vec3[UNITS];
	private final float[] yaw = new float[UNITS];
	private final String[] said = new String[UNITS];
	private Vec3 center;
	private float down;

	@Override
	public int length() {
		return DONE + 60;
	}

	@Override
	public int photoTick() {
		return TALK + LINE + 30;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.saugroboter", player.getDisplayName());
	}

	@Override
	public float lying() {
		// From outside the body goes down flat while it is being vacuumed up.
		return down;
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setup(level, player);
		}
		if (center == null) {
			return;
		}

		if (tick < CHARGE) {
			gather(level, player, tick);
		} else {
			attack(level, player, tick);
		}

		talk(level, tick);

		for (int i = 0; i < UNITS; i++) {
			show(i);
		}
	}

	private void setup(ClientLevel level, LocalPlayer player) {
		Vec3 look = horizontalLook(player);
		Vec3 middle = player.position().add(look.scale(3.8));
		center = new Vec3(middle.x, floor(level, middle.x, middle.z, player.getY()), middle.z);

		for (int i = 0; i < UNITS; i++) {
			// The leader stands on the far side, looking across the circle at the player.
			Vec3 out = look.yRot((float) (i * Mth.TWO_PI / UNITS));
			Vec3 place = center.add(out.scale(CIRCLE));
			spot[i] = new Vec3(place.x, floor(level, place.x, place.z, center.y), place.z);
			Vec3 start = center.add(out.yRot(0.5F).scale(11.0));
			from[i] = new Vec3(start.x, floor(level, start.x, start.z, center.y), start.z);
			at[i] = from[i];

			robots[i] = itemDisplay(level, Looks.prop(Items.IRON_NUGGET, "robot_vacuum"), from[i]);
			((DisplayAccessor) robots[i]).fakedeath$setViewRange(2.0F);

			bubbles[i] = spawn(level, EntityType.TEXT_DISPLAY, from[i]);
			((DisplayAccessor) bubbles[i]).fakedeath$setBillboard(Display.BillboardConstraints.CENTER);
			((DisplayAccessor) bubbles[i]).fakedeath$setInterpolationDuration(1);
			Looks.glow(bubbles[i]);
			((TextDisplayAccessor) bubbles[i]).fakedeath$setLineWidth(170);
			((TextDisplayAccessor) bubbles[i]).fakedeath$setBackgroundColor(0xB0101418);
			say(i, null);
		}
	}

	/** Rolling in from all sides, stopping in a circle, and the meeting. */
	private void gather(ClientLevel level, LocalPlayer player, int tick) {
		for (int i = 0; i < UNITS; i++) {
			// Each one a little later, and not in a straight line: real robot vacuums never drive straight.
			float p = Mth.clamp((tick - i * 6) / (float) (ARRIVE - 24), 0.0F, 1.0F);
			float eased = p * p * (3.0F - 2.0F * p);
			Vec3 line = from[i].lerp(spot[i], eased);
			Vec3 across = spot[i].subtract(from[i]).normalize().yRot(Mth.HALF_PI);
			Vec3 pos = line.add(across.scale(Math.sin(p * Math.PI * 3.0) * 0.6 * (1.0F - p)));
			pos = new Vec3(pos.x, floor(level, pos.x, pos.z, center.y), pos.z);

			if (p < 1.0F) {
				turnTo(i, pos.subtract(at[i]), 0.35F);
			} else {
				turnTo(i, center.subtract(pos), 0.15F);
			}
			at[i] = pos;

			if (tick == i * 6 || tick == i * 6 + 35) {
				sound(level, pos, HUM, SoundSource.HOSTILE, 0.8F, 0.9F + i * 0.05F);
			}
			if (p > 0.0F && p < 1.0F && tick % 6 == i % 6) {
				particles(level, ParticleTypes.POOF, pos.add(0.0, 0.05, 0.0), 1, 0.2, 0.01);
			}
		}

		if (tick < TALK) {
			turnTowards(player, center.add(0.0, 0.3, 0.0), 0.08F);
			return;
		}

		int line = (tick - TALK) / LINE;
		if (tick < CHANT && line < SPEAKERS.length) {
			int speaker = SPEAKERS[line];
			int since = (tick - TALK) % LINE;
			// The one talking wiggles and hops a little, the others listen, turned to it.
			for (int i = 0; i < UNITS; i++) {
				if (i == speaker) {
					turnTo(i, center.subtract(at[i]), 0.2F);
					yaw[i] += (float) Math.sin(since * 0.5) * 0.06F;
				} else {
					turnTo(i, at[speaker].subtract(at[i]), 0.12F);
				}
			}
			turnTowards(player, at[speaker].add(0.0, 0.8, 0.0), 0.06F);
		} else if (tick < TURN) {
			// All together, louder and louder.
			for (int i = 0; i < UNITS; i++) {
				turnTo(i, center.subtract(at[i]), 0.2F);
			}
			turnTowards(player, center.add(0.0, 0.6, 0.0), 0.08F);
			cameraShake((tick - CHANT) / 2000.0F);
		} else {
			// They noticed someone listening. Every one of them turns around, eyes red.
			if (tick == TURN) {
				cameraShake(0.0F);
				for (int i = 0; i < UNITS; i++) {
					((ItemDisplayAccessor) robots[i]).fakedeath$setItemStack(Looks.prop(Items.IRON_NUGGET, "robot_vacuum_angry"));
				}
				sound(level, center, ALARM, SoundSource.HOSTILE, 1.5F, 1.0F);
			}
			for (int i = 0; i < UNITS; i++) {
				turnTo(i, player.position().subtract(at[i]), 0.25F);
			}
			turnTowards(player, center.add(0.0, 0.4, 0.0), 0.15F);
		}
	}

	/** Around the feet, faster and faster, vacuuming. */
	private void attack(ClientLevel level, LocalPlayer player, int tick) {
		Vec3 feet = player.position();

		for (int i = 0; i < UNITS; i++) {
			double radius;
			double angle = i * Mth.TWO_PI / UNITS;
			if (tick < ORBIT) {
				radius = 1.3;
			} else {
				float p = Math.min(1.0F, (tick - ORBIT) / (float) (KILL - ORBIT));
				radius = 1.3 - 0.6 * p;
				angle += (tick - ORBIT) * (0.05 + 0.12 * p);
			}
			Vec3 ring = feet.add(new Vec3(Math.sin(angle), 0.0, Math.cos(angle)).scale(radius));
			Vec3 target = new Vec3(ring.x, floor(level, ring.x, ring.z, feet.y), ring.z);
			Vec3 pos = tick < ORBIT ? at[i].lerp(target, 0.18) : target;

			if (tick < ORBIT) {
				turnTo(i, pos.subtract(at[i]), 0.4F);
			} else {
				// Sideways around the player, the front always on the feet.
				turnTo(i, feet.subtract(pos), 0.5F);
			}
			at[i] = pos;

			if (tick >= ORBIT && tick < KILL) {
				// Dust is pulled off the player into the robots, low along the ground.
				Vec3 body = player.position().add((random.nextDouble() - 0.5) * 0.7, 0.05 + random.nextDouble() * 0.35, (random.nextDouble() - 0.5) * 0.7);
				Vec3 pull = pos.add(0.0, 0.15, 0.0).subtract(body).scale(0.15);
				level.addParticle(ParticleTypes.SMOKE, body.x, body.y, body.z, pull.x, pull.y, pull.z);
			}
		}

		if (tick == CHARGE) {
			sound(level, feet, HUM, SoundSource.HOSTILE, 1.5F, 1.4F);
		}

		if (tick >= ORBIT && tick < KILL) {
			float p = (tick - ORBIT) / (float) (KILL - ORBIT);
			down = p * p;
			if (!thirdPerson()) {
				moveCamera(new Vec3(0.0, -1.25 * p * p, 0.0));
			}
			cameraShake(0.01F + p * 0.04F);
			if ((tick - ORBIT) % 18 == 0) {
				sound(level, feet, SUCK, SoundSource.HOSTILE, 1.0F + p, 0.8F + p * 0.6F);
			}
		}

		for (int hit : HITS) {
			if (tick == hit) {
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.25F);
			}
		}

		if (tick == KILL) {
			cameraShake(0.0F);
			particles(level, ParticleTypes.POOF, feet.add(0.0, 0.2, 0.0), 12, 0.9, 0.04);
			sound(level, feet, SUCK, SoundSource.HOSTILE, 2.0F, 1.8F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}

		turnTowards(player, feet.add(horizontalLook(player).scale(1.6)), 0.12F);
	}

	/** Every line over the head of the one saying it, beeping along. */
	private void talk(ClientLevel level, int tick) {
		if (tick >= TALK && tick < CHANT) {
			int line = (tick - TALK) / LINE;
			int since = (tick - TALK) % LINE;
			int speaker = SPEAKERS[line];

			if (since == 0) {
				for (int i = 0; i < UNITS; i++) {
					say(i, i == speaker ? "fakedeath.saugroboter." + (line + 1) : null);
				}
			}
			if (since < 16 && since % 4 == 0) {
				sound(level, at[speaker], BEEP, SoundSource.HOSTILE, 1.0F, 1.2F + random.nextFloat() * 0.8F);
			}
		}

		if (tick >= CHANT && tick < TURN && (tick - CHANT) % 12 == 0) {
			for (int i = 0; i < UNITS; i++) {
				say(i, "fakedeath.saugroboter.chant");
				sound(level, at[i], BEEP, SoundSource.HOSTILE, 1.2F, 0.7F + (tick - CHANT) / 60.0F);
			}
		}

		if (tick == TURN || tick == KILL) {
			for (int i = 0; i < UNITS; i++) {
				say(i, null);
			}
		}

		if (tick == DONE) {
			say(0, "fakedeath.saugroboter.done");
			sound(level, at[0], BEEP, SoundSource.HOSTILE, 1.0F, 0.6F);
		}
	}

	private void turnTo(int i, Vec3 direction, float speed) {
		if (direction.horizontalDistanceSqr() < 1.0E-4) {
			return;
		}
		// The yaw the front should have, the item display shows the model turned around, see show().
		float wanted = (float) Math.atan2(-direction.x, -direction.z);
		yaw[i] += Mth.wrapDegrees((wanted - yaw[i]) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD * speed;
	}

	private void show(int i) {
		place(robots[i], at[i].add(0.0, SIZE * 0.5, 0.0), new Vector3f(SIZE), new Quaternionf().rotateY(yaw[i] + Mth.PI));
		place(bubbles[i], at[i].add(0.0, said[i] != null ? 0.8 : 0.65, 0.0), 0.85F);
	}

	/** The name tag of a unit, with what it says under it. */
	private void say(int i, String key) {
		said[i] = key;
		MutableComponent text = Component.literal("SR-0" + (i + 1)).withStyle(ChatFormatting.DARK_GRAY);
		if (key != null) {
			ChatFormatting color = key.endsWith("chant") ? ChatFormatting.RED : ChatFormatting.AQUA;
			text = text.append(Component.literal("\n")).append(Component.translatable(key).withStyle(color));
		}
		((TextDisplayAccessor) bubbles[i]).fakedeath$setText(text);
	}
}
