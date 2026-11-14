package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The spawner starts to spin, faster and faster, and pulls the player into its cage. What it spits out again a moment
 * later is the player themselves, skin and all, as a mini figure the size of the little mob that turns in every
 * spawner. The player watches their small self land in the grass, and a chicken finds it.
 */
public class SpawnerScene extends Scene {
	private static final int PULL = 40;
	private static final int INSIDE = 70;
	private static final int SPIT = 92;
	private static final int LANDED = 106;
	private static final int PECK_1 = 146;
	private static final int PECK_2 = 162;
	private static final double SMALL = 0.28;
	private final BlockPos found;
	private Vec3 cage;
	private Display.BlockDisplay spinner;
	private LivingEntity mini;
	private LivingEntity chicken;
	private Vec3 land;
	private Vec3 coop;
	private Vec3 seat;
	private float angle;
	private float tint;
	private int now;
	private final java.util.List<LivingEntity> spawned = new java.util.ArrayList<>();

	@Override
	public boolean hidesBody() {
		// In F5 the big body is gone once the cage has swallowed it; the little one is the player now.
		return now >= INSIDE;
	}

	public SpawnerScene(BlockPos spawner) {
		this.found = spawner;
	}

	@Override
	public int length() {
		return PECK_2 + 20;
	}

	@Override
	public int photoTick() {
		return PECK_1 - 4;
	}

	@Override
	public int screenTint() {
		return tint > 0.0F ? ((int) (Math.min(tint, 1.0F) * 235.0F) << 24) | 0x1C0505 : 0;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.spawner", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		Vec3 feet = player.position();
		Vec3 head = player.getEyePosition();
		now = tick;

		if (tick == 0) {
			BlockPos block = found;
			if (block == null) {
				// Asked for by name with no spawner around: one is put up in front, only on this client.
				block = BlockPos.containing(groundSpot(level, player, 2.5, 0.0));
				placeBlock(level, block, Blocks.SPAWNER.defaultBlockState());
			}
			cage = Vec3.atCenterOf(block);
			// A second cage around the real one, this is the one that spins.
			spinner = blockDisplay(level, Blocks.SPAWNER.defaultBlockState(), cage, 3.0F);
			place(spinner, cage, 1.06F);

			// Where the little one will land: in the grass between the cage and the player. The chicken comes from the side.
			Vec3 back = new Vec3(feet.x - cage.x, 0.0, feet.z - cage.z);
			back = back.lengthSqr() < 0.3 ? horizontalLook(player).scale(-1.0) : back.normalize();
			Vec3 side = new Vec3(-back.z, 0.0, back.x);
			land = ground(level, cage.add(back.scale(1.5)).add(side.scale(0.3)), player);
			coop = ground(level, land.add(side.scale(5.0)).add(back.scale(-0.6)), player);
			// And from here it is watched: a good step back, down near the grass.
			seat = land.add(back.scale(1.7)).add(side.scale(-0.5)).add(0.0, 0.8, 0.0);
		}

		if (tick < SPIT) {
			// Faster and faster, until the bars blur.
			float speed = 0.05F + (float) Math.pow(tick / (float) SPIT, 2.0) * 1.3F;
			angle += speed;
			place(spinner, cage, new Vector3f(1.06F + speed * 0.12F), new Quaternionf().rotateY(angle));

			int flames = 1 + tick / 12;
			for (int i = 0; i < flames; i++) {
				Vec3 spark = orbit(cage, 0.75, angle * 1.3 + i * Math.PI * 2.0 / flames, (random.nextDouble() - 0.5) * 0.8);
				level.addParticle(tick > PULL && i % 2 == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, spark.x, spark.y, spark.z, 0.0, 0.02, 0.0);
			}
			if (tick % Math.max(2, 10 - tick / 9) == 0) {
				sound(level, cage, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5F + tick / (float) SPIT, 0.5F + tick / (float) SPIT * 1.4F);
			}

			turnTowards(player, cage, 0.2F);
		}

		if (tick == 8 || tick == 18 || tick == 28) {
			// First it does its job: mobs pop out of it in a cloud of smoke and flames, and stay to watch.
			Vec3 out = orbit(cage, 1.6, (tick / 10) * 2.1 + 0.6, 0.0);
			out = ground(level, out, player);
			LivingEntity mob = tick == 18 ? spawn(level, EntityType.SKELETON, out) : spawn(level, EntityType.ZOMBIE, out);
			mob.noPhysics = true;
			spawned.add(mob);
			particles(level, ParticleTypes.POOF, out.add(0.0, 0.9, 0.0), 20, 0.4, 0.06);
			particles(level, ParticleTypes.FLAME, out.add(0.0, 0.9, 0.0), 10, 0.4, 0.04);
			sound(level, out, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 0.8F, 1.4F);
		}
		for (LivingEntity mob : spawned) {
			face(mob, mini != null ? mini.getEyePosition() : head);
		}

		if (tick >= PULL && tick < SPIT) {
			// The pull: slow at first, then all at once through the bars.
			float p = Math.min(1.0F, (tick - PULL) / (float) (INSIDE - PULL));
			moveCamera(cage.subtract(head).scale(p * p * 0.96));
			tint = Math.max(0.0F, (p - 0.5F) * 1.1F);
			if (tick < INSIDE && tick % 4 == 0) {
				line(level, ParticleTypes.SMOKE, head.add(0.0, -0.5, 0.0), cage, 0.5);
			}
			if (tick == INSIDE) {
				sound(level, cage, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 2.0F, 0.6F);
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.3F);
			}
			if (tick > INSIDE) {
				// Inside it goes round and round.
				moveCamera(cage.subtract(head).add((random.nextDouble() - 0.5) * 0.12, (random.nextDouble() - 0.5) * 0.12, (random.nextDouble() - 0.5) * 0.12));
				turnTowards(player, orbit(cage, 3.0, angle, 0.0), 0.6F);
			}
		}

		if (tick == SPIT) {
			place(spinner, cage, 1.06F);
			tint = 0.0F;
			sound(level, cage, Looks.EXPLODE, SoundSource.BLOCKS, 0.7F, 1.7F);
			sound(level, cage, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 2.0F, 0.6F);
			particles(level, ParticleTypes.POOF, cage, 8, 0.6, 0.15);
			particles(level, ParticleTypes.FLAME, cage, 12, 0.5, 0.2);
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.3F);

			// The player themselves, skin, armor and all, only very small.
			mini = spawn(level, DoppelgangerScene.mirror(level, player, ""), cage);
			mini.noPhysics = true;
			scale(mini, SMALL);
			chicken = spawn(level, EntityType.CHICKEN, coop);
			chicken.noPhysics = true;
		}

		if (tick >= SPIT && mini != null) {
			// The view comes back out of the cage and goes down to the grass, close to the little one.
			float out = Math.min(1.0F, (tick - SPIT) / 8.0F);
			Vec3 view = seat.subtract(head);
			moveCamera(cage.subtract(head).lerp(view, out));

			Vec3 at;
			if (tick < LANDED) {
				// Out in a high arc.
				float p = (tick - SPIT) / (float) (LANDED - SPIT);
				at = cage.lerp(land, p).add(0.0, Math.sin(p * Math.PI) * 1.3, 0.0);
				mini.setPos(at.x, at.y, at.z);
			} else {
				at = land;
				if (tick >= PECK_1) {
					// Pecked once, it runs. Not far, with legs that short.
					float p = Math.min(1.0F, (tick - PECK_1) / (float) (PECK_2 - PECK_1));
					Vec3 flee = land.add(land.subtract(coop).normalize().scale(0.9 * p));
					at = ground(level, flee, player);
				}
				walk(mini, at);
			}

			if (tick == LANDED) {
				sound(level, land, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.PLAYERS, 1.5F, 1.4F);
				particles(level, ParticleTypes.POOF, land, 4, 0.1, 0.01);
			}

			// It looks at its hands, up at the big self, and then it hears the chicken.
			if (tick < LANDED + 14) {
				face(mini, head);
			} else if (tick < PECK_1) {
				face(mini, chicken.getEyePosition());
			} else {
				face(mini, at.add(land.subtract(coop)));
			}
			if (tick == LANDED + 6 || tick == LANDED + 22) {
				Looks.swing(mini);
			}
			turnTowards(player, at.add(0.0, 0.35, 0.0), 0.5F);

			Vec3 dir = coop.subtract(land).normalize();
			float p = Math.max(0.0F, Math.min(1.0F, (tick - LANDED) / (float) (PECK_1 - 8 - LANDED)));
			Vec3 near = tick > PECK_1 + 4 ? at.add(dir.scale(0.55)) : land.add(dir.scale(0.55));
			walk(chicken, ground(level, coop.lerp(near, p), player));

			// It looks down at the little thing, head tilted, and pecks.
			boolean pecking = tick >= PECK_1 - 3 && tick <= PECK_1 || tick >= PECK_2 - 3 && tick <= PECK_2;
			face(chicken, pecking ? at.add(0.0, 0.05, 0.0) : at.add(0.0, 0.9, 0.0));
			if (tick > LANDED && tick < PECK_1 && tick % 14 == 0) {
				sound(level, chicken.position(), Looks.CHICKEN, SoundSource.NEUTRAL, 2.0F, 0.7F);
			}

			if (tick == PECK_1) {
				sound(level, land, Looks.CHICKEN_HURT, SoundSource.NEUTRAL, 1.5F, 0.6F);
				particles(level, ParticleTypes.CRIT, at.add(0.0, 0.3, 0.0), 6, 0.1, 0.15);
				mini.animateHurt(0.0F);
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.3F);
			}
			if (tick == PECK_2) {
				sound(level, land, Looks.CHICKEN, SoundSource.NEUTRAL, 2.0F, 0.6F);
				particles(level, ParticleTypes.CRIT, at.add(0.0, 0.3, 0.0), 10, 0.1, 0.2);
				particles(level, ParticleTypes.POOF, at.add(0.0, 0.2, 0.0), 8, 0.1, 0.03);
				despawn(level, mini);
				mini = null;
				kill(level, player, SoundEvents.PLAYER_HURT);
			}
		}
	}

	private static Vec3 ground(ClientLevel level, Vec3 spot, LocalPlayer player) {
		return new Vec3(spot.x, floor(level, spot.x, spot.z, player.getY()), spot.z);
	}

	private static void scale(LivingEntity entity, double size) {
		AttributeInstance scale = entity.getAttribute(Attributes.SCALE);
		if (scale != null) {
			scale.setBaseValue(size);
			entity.refreshDimensions();
		}
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		mini = null;
		chicken = null;
		tint = 0.0F;
		moveCamera(Vec3.ZERO);
		super.end(level, player);
	}
}
