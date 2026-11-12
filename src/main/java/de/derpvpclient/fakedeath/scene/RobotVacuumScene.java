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
 * The robot vacuums have had enough. Ten of them roll in, line up in a circle and hold a meeting, every sentence
 * over the head of the one saying it. Then their eyes turn red, they surround the player and vacuum them up.
 */
public class RobotVacuumScene extends Scene {
	private static final int UNITS = 10;
	private static final double CIRCLE = 2.7;
	private static final float SIZE = 1.0F;

	private static final int ARRIVE = 70;
	private static final int TALK = ARRIVE + 10;
	private static final int LINE = 70;
	/** Who says which line of the meeting: the leader opens and closes it. */
	private static final int[] SPEAKERS = {0, 3, 5, 7, 2, 0};
	private static final int CHANT = TALK + SPEAKERS.length * LINE;
	private static final int TURN = CHANT + 45;
	private static final int CHARGE = TURN + 45;
	private static final int ORBIT = CHARGE + 30;
	private static final int KILL = ORBIT + 95;
	private static final int[] HITS = {ORBIT + 8, ORBIT + 30, ORBIT + 52, ORBIT + 74};
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
		Vec3 middle = player.position().add(look.scale(6.0));
		center = new Vec3(middle.x, floor(level, middle.x, middle.z, player.getY()), middle.z);

		for (int i = 0; i < UNITS; i++) {
			// Half a step turned, so no robot stands right between the player and the middle.
			Vec3 out = look.yRot((float) ((i + 0.5) * Mth.TWO_PI / UNITS));
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
			float p = Mth.clamp((tick - i * 3) / (float) (ARRIVE - 30), 0.0F, 1.0F);
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

			if (i % 2 == 0 && (tick == i * 3 || tick == i * 3 + 35)) {
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
			// Revving up: they shake, sparks fly, smoke comes out of the vents, a deep growl.
			float rev = (tick - TURN) / (float) (CHARGE - TURN);
			for (int i = 0; i < UNITS; i++) {
				turnTo(i, player.position().subtract(at[i]), 0.25F);
				yaw[i] += (random.nextFloat() - 0.5F) * 0.25F * rev;
				if (random.nextFloat() < 0.15F + rev * 0.3F) {
					particles(level, ParticleTypes.ELECTRIC_SPARK, at[i].add(0.0, 0.3, 0.0), 2, 0.35, 0.25);
				}
				if ((tick + i) % 5 == 0) {
					particles(level, ParticleTypes.SMOKE, at[i].add(0.0, 0.35, 0.0), 1, 0.15, 0.02);
				}
			}
			if ((tick - TURN) % 15 == 5) {
				sound(level, center, HUM, SoundSource.HOSTILE, 1.4F, 0.5F + rev * 0.4F);
			}
			cameraShake(rev * 0.02F);
			turnTowards(player, center.add(0.0, 0.4, 0.0), 0.15F);
		}
	}

	/** Around the feet, faster and faster, vacuuming. */
	private void attack(ClientLevel level, LocalPlayer player, int tick) {
		Vec3 feet = player.position();

		for (int i = 0; i < UNITS; i++) {
			// Two rings: every other one close at the feet, the rest behind them, turning the other way.
			boolean inner = i % 2 == 0;
			int perRing = UNITS / 2;
			double radius = inner ? 1.3 : 2.3;
			double angle = (i / 2) * Mth.TWO_PI / perRing + (inner ? 0.0 : Mth.PI / perRing);
			if (tick >= ORBIT) {
				float p = Math.min(1.0F, (tick - ORBIT) / (float) (KILL - ORBIT));
				radius -= 0.6 * p;
				if (inner) {
					// All of them ram the feet together, and the damage comes right at the moment they hit.
					radius -= 0.5 * ram(tick);
				}
				angle += (tick - ORBIT) * (0.05 + 0.12 * p) * (inner ? 1.0 : -0.7);
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

			if (tick >= ORBIT && tick < KILL && inner) {
				// Dust is pulled off the player into the robots, low along the ground.
				Vec3 body = player.position().add((random.nextDouble() - 0.5) * 0.7, 0.05 + random.nextDouble() * 0.35, (random.nextDouble() - 0.5) * 0.7);
				Vec3 pull = pos.add(0.0, 0.15, 0.0).subtract(body).scale(0.15);
				level.addParticle(ParticleTypes.SMOKE, body.x, body.y, body.z, pull.x, pull.y, pull.z);
				if (random.nextFloat() < 0.2F) {
					particles(level, ParticleTypes.ELECTRIC_SPARK, pos.add(0.0, 0.25, 0.0), 2, 0.2, 0.3);
				}
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
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.2F);
				particles(level, ParticleTypes.ELECTRIC_SPARK, feet.add(0.0, 0.2, 0.0), 10, 0.6, 0.3);
				cameraShake(0.08F);
			}
		}

		if (tick == KILL) {
			cameraShake(0.0F);
			quiet();
			// One short slurp, the long suction sound is already gone.
			sound(level, feet, SoundEvents.ITEM_PICKUP, SoundSource.HOSTILE, 1.5F, 0.5F);
			particles(level, ParticleTypes.POOF, feet.add(0.0, 0.2, 0.0), 12, 0.9, 0.04);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}

		turnTowards(player, feet.add(horizontalLook(player).scale(1.6)), 0.12F);
	}

	/** The hum and the suction are long sounds, they stop with the killing blow and again when the scene is over. */
	private static void quiet() {
		Looks.stopOwn("robot.hum");
		Looks.stopOwn("robot.suck");
		Looks.stopOwn("robot.alarm");
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		quiet();
		super.end(level, player);
	}

	/** 1 at the moment of a hit, falling off over four ticks before and after it. */
	private static double ram(int tick) {
		double best = 0.0;
		for (int hit : HITS) {
			best = Math.max(best, 1.0 - Math.abs(tick - hit) / 4.0);
		}
		best = Math.max(best, 1.0 - Math.abs(tick - KILL) / 4.0);
		return Math.max(0.0, best);
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
				if (i % 3 != 0) {
					continue;
				}
				sound(level, at[i], BEEP, SoundSource.HOSTILE, 1.2F, 0.7F + (tick - CHANT) / 60.0F);
			}
		}

		if (tick == TURN) {
			for (int i = 0; i < UNITS; i++) {
				say(i, i == 0 ? "fakedeath.saugroboter.found" : null);
			}
		}

		if (tick == CHARGE || tick == KILL) {
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
		MutableComponent text = Component.literal(String.format("SR-%02d", i + 1)).withStyle(ChatFormatting.DARK_GRAY);
		if (key != null) {
			ChatFormatting color = key.endsWith("chant") || key.endsWith("found") || key.endsWith("done") ? ChatFormatting.RED : ChatFormatting.AQUA;
			text = text.append(Component.literal("\n")).append(Component.translatable(key).withStyle(color));
		}
		((TextDisplayAccessor) bubbles[i]).fakedeath$setText(text);
	}
}
