package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Howling far off, from one side, then from the other. At the edge of the trees eyes light up in the dark, a whole
 * pack. They come out and circle the player, the circle getting tighter and faster, snapping at them one after the
 * other, the heart beating faster and faster, their breath steaming in the cold. Dusk falls while they come. Then the
 * big leader of the pack, which held back the whole time, leaps right at the throat and knocks the player down: from
 * the ground they look up at it, the whole pack gathers round and looks down, and the leader snaps once more.
 */
public class WolfScene extends Scene {
	// Adult wolf sounds depend on the wolf's sound variant in 26.3, these are the angry ones by id.
	private static final SoundEvent GROWL = SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace("entity.wolf_angry.growl"));
	private static final SoundEvent SNARL = SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace("entity.wolf_angry.ambient"));
	private static final int PACK = 6;
	private static final double FAR = 13.0;
	private static final int[] HOWLS = {0, 22, 40};
	private static final int RUN = 70;
	private static final int RUN_TICKS = 30;
	private static final int CIRCLE = RUN + RUN_TICKS;
	/** The pack snaps at the player, the leader's leap is the last one. */
	private static final int[] BITES = {CIRCLE + 25, CIRCLE + 42, CIRCLE + 58};
	/** Which wolf snaps at each bite, never the leader in the middle. */
	private static final int[] BITERS = {1, 4, 0};
	private static final int LEADER_LEAP = CIRCLE + 82;
	private static final int LUNGE = 8;
	private static final int LEADER_LUNGE = 12;
	/** Knocked down by the leader: from here on the view is on the ground. */
	private static final int DOWN = LEADER_LEAP + LEADER_LUNGE;
	private static final int DEATH = DOWN + 30;

	private final List<Wolf> wolves = new ArrayList<>();
	private final List<Float> angles = new ArrayList<>();
	private final List<Vec3> starts = new ArrayList<>();
	private final List<Display.BlockDisplay> eyes = new ArrayList<>();
	private Wolf leader;
	private boolean dark;
	private int now;
	private int nextBeat = CIRCLE;

	@Override
	public int length() {
		return DEATH + 4;
	}

	@Override
	public boolean firstPersonOnly() {
		// The pack closing in and the leap at the throat are made for the own eyes.
		return true;
	}

	@Override
	public int photoTick() {
		return CIRCLE + 30;
	}

	/** How far dusk has fallen: it gets darker while the pack comes, darkest lying on the ground. */
	private float dusk() {
		return Mth.clamp(now / (float) CIRCLE, 0.0F, 1.0F) * 0.7F + (now >= DOWN ? Math.min(0.3F, (now - DOWN) * 0.02F) : 0.0F);
	}

	@Override
	public int screenTint() {
		return (int) (dusk() * 120.0F) << 24 | 0x060A18;
	}

	@Override
	public int skyTint() {
		return (int) (dusk() * 200.0F) << 24 | 0x0B1020;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.mob", player.getDisplayName(), EntityType.WOLF.getDescription());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		now = tick;
		if (tick == 0) {
			setUp(level, player);
		}
		if (wolves.isEmpty()) {
			return;
		}
		Vec3 look = horizontalLook(player);
		// Lying on the ground after the leap, a little back from where the player stood.
		Vec3 ground = player.getEyePosition().add(look.scale(-0.3)).add(0.0, -1.35, 0.0);

		for (int i = 0; i < HOWLS.length; i++) {
			if (tick == HOWLS[i]) {
				// From far away, a different direction each time.
				Vec3 from = player.position().add(horizontalLook(player).yRot((i - 1) * 1.6F).scale(30.0));
				sound(level, from, Looks.own("wolf.howl"), SoundSource.HOSTILE, 3.0F, 0.9F + i * 0.05F);
			}
		}
		if (tick > RUN - 25 && tick % 12 == 0) {
			Vec3 from = wolves.get(random.nextInt(wolves.size())).position();
			sound(level, from, random.nextBoolean() ? GROWL : SNARL, SoundSource.HOSTILE, 1.5F, 0.8F + random.nextFloat() * 0.2F);
		}

		for (int i = 0; i < wolves.size(); i++) {
			Wolf wolf = wolves.get(i);
			boolean isLeader = wolf == leader;
			Vec3 pos;
			float angle = angles.get(i);

			if (tick < RUN) {
				// Waiting at the edge of the trees, pacing a little.
				pos = starts.get(i).add(Math.sin(tick * 0.08 + i) * 0.4, 0.0, Math.cos(tick * 0.08 + i) * 0.4);
			} else if (tick < CIRCLE) {
				float t = Mth.clamp((tick - RUN - i * 2) / (float) (RUN_TICKS - PACK * 2), 0.0F, 1.0F);
				pos = starts.get(i).lerp(ring(level, player, angle, isLeader ? 6.0 : 5.0), t);
				if (tick % 4 == i % 4 && t > 0.0F && t < 1.0F) {
					sound(level, pos, SoundEvents.WOLF_STEP, SoundSource.HOSTILE, 1.0F, 1.2F);
				}
			} else {
				// Circling, the ring getting tighter and faster. The leader keeps its distance until it leaps.
				float since = tick - CIRCLE;
				double radius = isLeader ? 4.5 : Math.max(2.4, 5.0 - since * 0.03);
				float around = angle + since * (isLeader ? 0.01F : 0.03F + since * 0.0003F);
				pos = ring(level, player, around, radius);
				int lunge = lungeTick(i, tick);
				if (lunge >= 0) {
					double t = Math.sin(Math.PI * lunge / LUNGE);
					Vec3 face = player.position().add(horizontalLook(player).scale(0.7));
					pos = pos.lerp(face, t * 0.85).add(0.0, t * 0.9, 0.0);
				}
				if (isLeader && tick >= LEADER_LEAP && tick < DOWN) {
					// The leap: up and right at the throat.
					float t = Math.min(1.0F, (tick - LEADER_LEAP) / (float) LEADER_LUNGE);
					Vec3 throat = player.getEyePosition().add(look.scale(0.4)).add(0.0, -0.9, 0.0);
					pos = pos.lerp(throat, t).add(0.0, Math.sin(t * Math.PI) * 1.2, 0.0);
				}
				if (tick >= DOWN) {
					// The player is down: the leader stands over them, the pack closes in all around and looks down.
					float t = Math.min(1.0F, (tick - DOWN) / 10.0F);
					Vec3 gather = isLeader ? player.position().add(look.scale(0.6))
						: player.position().add(Mth.cos(angle + i) * 1.5, 0.0, Mth.sin(angle + i) * 1.5);
					pos = pos.lerp(gather, t * t * (3.0F - 2.0F * t));
					// The last snap of the leader, down at the face.
					if (isLeader && tick >= DEATH - 6) {
						double snap = Math.sin(Math.PI * (tick - (DEATH - 6)) / 6.0);
						pos = pos.lerp(ground, snap * 0.35);
					}
				}
			}
			if (!(isLeader && tick >= LEADER_LEAP && tick < DOWN)) {
				pos = new Vec3(pos.x, floor(level, pos.x, pos.z, player.getY()), pos.z);
			}
			walk(wolf, pos);
			face(wolf, tick >= DOWN ? ground : player.getEyePosition());
			if (tick > RUN - 20 && (tick + i * 3) % 9 == 0) {
				// Breath steaming in the cold, out of the snout.
				float yaw = wolf.getYHeadRot() * Mth.DEG_TO_RAD;
				double scale = isLeader ? 1.6 : 1.0;
				Vec3 snout = wolf.position().add(-Math.sin(yaw) * 0.55 * scale, 0.5 * scale, Math.cos(yaw) * 0.55 * scale);
				particles(level, ParticleTypes.WHITE_SMOKE, snout, 2, 0.04, 0.01);
			}
		}
		placeEyes(tick);

		int biter = biterAround(tick);
		if (tick >= DOWN) {
			// Knocked onto the back, looking up at the leader.
			float t = Math.min(1.0F, (tick - DOWN) / 6.0F);
			moveCamera(ground.subtract(player.getEyePosition()).scale(t * t));
			turnTowards(player, leader.getEyePosition().add(0.0, 0.3, 0.0), 0.3F);
		} else if (tick < RUN + 10) {
			turnTowards(player, wolves.get(PACK / 2).getEyePosition(), 0.06F);
		} else if (tick >= LEADER_LEAP - 12) {
			turnTowards(player, leader.getEyePosition(), 0.35F);
		} else if (biter >= 0) {
			// Whipping round to whoever snaps.
			turnTowards(player, wolves.get(biter).getEyePosition(), 0.3F);
		} else {
			// Turning with the pack, trying to keep an eye on all of them.
			turnTowards(player, wolves.get((tick / 25) % PACK).getEyePosition(), 0.08F);
		}

		if (tick >= CIRCLE && tick >= nextBeat && tick < DEATH) {
			// The heart beats faster and faster.
			sound(level, player.getEyePosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.2F, 1.0F);
			nextBeat = tick + Math.max(7, 22 - (tick - CIRCLE) / 6);
		}

		for (int i = 0; i < BITES.length; i++) {
			int bite = BITES[i];
			if (tick == bite - 2) {
				sound(level, wolves.get(BITERS[i]).position(), SNARL, SoundSource.HOSTILE, 1.5F, 1.0F);
			}
			if (tick == bite + LUNGE / 2) {
				sound(level, player.position(), GROWL, SoundSource.HOSTILE, 1.5F, 1.1F);
				particles(level, ParticleTypes.CRIT, player.getEyePosition().add(0, -0.4, 0), 12, 0.3, 0.3);
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.24F);
			}
		}
		if (tick == LEADER_LEAP - 10) {
			// The leader growls, deep and long: here it comes.
			sound(level, leader.position(), GROWL, SoundSource.HOSTILE, 2.5F, 0.6F);
		}
		if (tick == DOWN) {
			sound(level, player.position(), SNARL, SoundSource.HOSTILE, 2.0F, 0.7F);
			sound(level, player.position(), SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 1.0F, 0.9F);
			particles(level, ParticleTypes.CRIT, player.getEyePosition().add(0, -0.4, 0), 25, 0.4, 0.4);
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.2F);
		}
		if (tick == DOWN + 12) {
			sound(level, leader.position(), GROWL, SoundSource.HOSTILE, 2.5F, 0.55F);
		}
		if (tick == DEATH - 3) {
			sound(level, leader.position(), SNARL, SoundSource.HOSTILE, 2.5F, 0.6F);
		}
		if (tick == DEATH) {
			particles(level, ParticleTypes.CRIT, ground, 25, 0.4, 0.4);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	/** The wolf that is about to snap or snapping right now, so the view can whip round to it, or -1. */
	private int biterAround(int tick) {
		for (int i = 0; i < BITES.length; i++) {
			if (tick >= BITES[i] - 10 && tick < BITES[i] + LUNGE) {
				return BITERS[i];
			}
		}
		return -1;
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		// In an arc in front of the player, so they see the pack coming, the leader in the middle and a bit back.
		Vec3 look = horizontalLook(player);
		float facing = (float) Mth.atan2(look.z, look.x);
		dark = level.getMaxLocalRawBrightness(player.blockPosition().above()) < 9;
		for (int i = 0; i < PACK; i++) {
			float angle = facing + (i - (PACK - 1) / 2.0F) * 0.42F + (random.nextFloat() - 0.5F) * 0.12F;
			angles.add(angle);
			boolean isLeader = i == PACK / 2;
			Vec3 start = ring(level, player, angle, FAR + (isLeader ? 3.0 : random.nextDouble() * 2.0));
			starts.add(start);
			Wolf wolf = spawn(level, EntityType.WOLF, start);
			// Red eyes and bared teeth.
			wolf.setRemainingPersistentAngerTime(2000);
			wolf.noPhysics = true;
			if (isLeader) {
				// The leader is a good deal bigger than the others.
				wolf.getAttribute(Attributes.SCALE).setBaseValue(1.6);
				leader = wolf;
			}
			wolves.add(wolf);
			// In the dark only the eyes are seen at first: two small lights.
			if (dark) {
				for (int e = 0; e < 2; e++) {
					Display.BlockDisplay eye = blockDisplay(level, Looks.YELLOW_GLOW, start, 2.0F);
					((de.derpvpclient.fakedeath.mixin.DisplayAccessor) eye).fakedeath$setBrightnessOverride(new Brightness(15, 15));
					eyes.add(eye);
				}
			}
		}
	}

	/** The glowing eyes sit on each wolf's face; once the pack runs they fade out, the wolves are close enough to see. */
	private void placeEyes(int tick) {
		for (int i = 0; i < eyes.size(); i++) {
			Wolf wolf = wolves.get(i / 2);
			float size = tick < RUN + 15 ? 0.07F * (float) (wolf == leader ? 1.6 : 1.0) : 0.0F;
			float yaw = wolf.getYHeadRot() * Mth.DEG_TO_RAD;
			Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
			Vec3 side = new Vec3(forward.z, 0.0, -forward.x);
			double scale = wolf == leader ? 1.6 : 1.0;
			Vec3 at = wolf.position().add(0.0, 0.62 * scale, 0.0).add(forward.scale(0.42 * scale)).add(side.scale((i % 2 == 0 ? -0.1 : 0.1) * scale));
			place(eyes.get(i), at, new Vector3f(size, size * 0.6F, 0.01F), new Quaternionf().rotateY(-yaw));
		}
	}

	/** Which ticks of its leap the given wolf is in, or -1 when it is not leaping. */
	private int lungeTick(int wolf, int tick) {
		if (wolves.get(wolf) == leader) {
			return -1;
		}
		for (int i = 0; i < BITES.length; i++) {
			int biter = BITERS[i];
			if (biter == wolf && tick >= BITES[i] && tick < BITES[i] + LUNGE) {
				return tick - BITES[i];
			}
		}
		return -1;
	}

	private static Vec3 ring(ClientLevel level, LocalPlayer player, float angle, double radius) {
		Vec3 spot = player.position().add(Mth.cos(angle) * radius, 0.0, Mth.sin(angle) * radius);
		return new Vec3(spot.x, floor(level, spot.x, spot.z, player.getY()), spot.z);
	}
}
