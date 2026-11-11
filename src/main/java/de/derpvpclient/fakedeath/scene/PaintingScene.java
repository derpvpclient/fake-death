package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The painting in front of the player rattles on its nail, a bony figure climbs out of the canvas, walks over, takes
 * the player by the collar and drags them, walking backwards, to the picture. The figure goes in first, the player
 * right behind it.
 */
public class PaintingScene extends Scene {
	private static final int OUT = 22;
	private static final int STANDS = 46;
	private static final int ARRIVE = 72;
	private static final int GRAB = 80;
	private static final int INSIDE = 134;
	private static final int CLIMB = 9;
	private final Entity found;
	private Vec3 canvas;
	private Vec3 normal;
	private Vec3 side;
	private LivingEntity figure;
	private Vec3 near;
	private Vec3 last;
	private int climbing = -1;
	private float tint;

	public PaintingScene(Entity painting) {
		this.found = painting;
	}

	@Override
	public int length() {
		return INSIDE + 20;
	}

	@Override
	public int photoTick() {
		return GRAB + 22;
	}

	@Override
	public int screenTint() {
		return tint > 0.0F ? ((int) (Math.min(tint, 1.0F) * 250.0F) << 24) | 0x1A120B : 0;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.gemaelde", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		Vec3 head = player.getEyePosition();

		if (tick == 0) {
			if (found != null && found.isAlive()) {
				canvas = found.getBoundingBox().getCenter();
				Direction facing = found.getDirection();
				normal = new Vec3(facing.getStepX(), 0.0, facing.getStepZ());
				if (normal.lengthSqr() < 0.5) {
					Vec3 out = player.position().subtract(canvas);
					normal = new Vec3(out.x, 0.0, out.z).normalize();
				}
			} else {
				// Asked for by name with no painting around: a bit of wall with a picture on it, only on this client.
				Vec3 look = horizontalLook(player);
				Direction ahead = Math.abs(look.x) > Math.abs(look.z) ? (look.x > 0.0 ? Direction.EAST : Direction.WEST) : (look.z > 0.0 ? Direction.SOUTH : Direction.NORTH);
				normal = new Vec3(-ahead.getStepX(), 0.0, -ahead.getStepZ());
				Vec3 spot = player.position().add(ahead.getStepX() * 4.0, 0.0, ahead.getStepZ() * 4.0);
				BlockPos base = BlockPos.containing(spot.x, floor(level, spot.x, spot.z, player.getY()) + 0.01, spot.z);
				Direction across = ahead.getClockWise();
				for (int width = -1; width <= 1; width++) {
					for (int height = 0; height < 3; height++) {
						placeBlock(level, base.relative(across, width).above(height), Blocks.STONE_BRICKS.defaultBlockState());
					}
				}
				canvas = Vec3.atCenterOf(base.above()).add(normal.scale(0.54));
				Display.ItemDisplay picture = itemDisplay(level, new ItemStack(Items.PAINTING), canvas);
				place(picture, canvas, new Vector3f(2.0F, 2.0F, 0.5F), new Quaternionf().rotateY((float) Math.atan2(normal.x, normal.z)));
			}

			side = new Vec3(-normal.z, 0.0, normal.x);
			Vec3 to = canvas.subtract(player.position());
			Vec3 flat = new Vec3(to.x, 0.0, to.z);
			flat = flat.lengthSqr() < 0.05 ? normal.scale(-1.0) : flat.normalize();
			near = player.position().add(flat.scale(1.4));
		}

		Vec3 foot = new Vec3(canvas.x, floor(level, canvas.x + normal.x * 0.7, canvas.z + normal.z * 0.7, player.getY()), canvas.z).add(normal.scale(0.7));

		if (tick < OUT) {
			// The picture rattles on its nail.
			turnTowards(player, canvas, 0.2F);
			if (tick % 7 == 2) {
				sound(level, canvas, SoundEvents.PAINTING_PLACE, SoundSource.NEUTRAL, 1.4F, 0.7F + tick * 0.02F);
				particles(level, ParticleTypes.POOF, canvas.add(normal.scale(0.2)), 3, 0.4, 0.01);
			}
			return;
		}

		if (tick == OUT) {
			figure = spawn(level, EntityType.SKELETON, canvas.add(0.0, -0.4, 0.0));
			figure.noPhysics = true;
			scale(0.15);
			sound(level, canvas, SoundEvents.SKELETON_AMBIENT, SoundSource.HOSTILE, 1.5F, 0.6F);
			sound(level, canvas, SoundEvents.PAINTING_BREAK, SoundSource.NEUTRAL, 1.2F, 0.8F);
		}

		if (tick < GRAB) {
			Vec3 at;
			if (tick < STANDS) {
				// Small and flat it comes out of the canvas and grows to full size on the way down.
				float p = (tick - OUT) / (float) (STANDS - OUT);
				scale(0.15 + 0.85 * p);
				at = canvas.add(0.0, -0.4, 0.0).lerp(foot, p * p);
				if (tick % 4 == 0) {
					particles(level, ParticleTypes.POOF, canvas.add(normal.scale(0.2)), 1, 0.3, 0.01);
				}
			} else if (tick < ARRIVE) {
				float p = (tick - STANDS) / (float) (ARRIVE - STANDS);
				Vec3 flat = foot.lerp(near, p);
				at = new Vec3(flat.x, floor(level, flat.x, flat.z, player.getY()), flat.z);
				if (tick % 6 == 0) {
					sound(level, at, SoundEvents.SKELETON_STEP, SoundSource.HOSTILE, 1.2F, 0.9F);
				}
			} else {
				at = new Vec3(near.x, floor(level, near.x, near.z, player.getY()), near.z);
			}
			walk(figure, at);
			face(figure, head);
			turnTowards(player, figure.getEyePosition(), 0.2F);
			last = at;
			return;
		}

		if (tick == GRAB) {
			Looks.swing(figure);
			sound(level, head, SoundEvents.SKELETON_HURT, SoundSource.HOSTILE, 1.4F, 0.6F);
			particles(level, ParticleTypes.CRIT, head.add(0.0, -0.4, 0.0), 10, 0.3, 0.2);
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.2F);
		}

		// The way to the picture: slowly at first, heels in the ground, then there is no holding.
		float p = Math.min(1.0F, (tick - GRAB) / (float) (INSIDE - GRAB));
		float eased = p * p;
		Vec3 pulled = head.lerp(canvas.add(normal.scale(0.3)), eased);
		double left = pulled.subtract(canvas).dot(normal);
		moveCamera(pulled.subtract(head).add(0.0, Math.sin(tick * 0.9) * 0.04 * (1.0F - p), 0.0));

		if (figure != null) {
			if (climbing < 0 && left < 2.3) {
				climbing = tick;
				sound(level, canvas, SoundEvents.PAINTING_BREAK, SoundSource.NEUTRAL, 1.2F, 0.7F);
			}
			if (climbing < 0) {
				// It walks backwards half a step to the side, one hand on the collar, and never lets the player out of its sight.
				Vec3 flat = pulled.subtract(normal.scale(1.45)).add(side.scale(0.45));
				last = new Vec3(flat.x, floor(level, flat.x, flat.z, player.getY()), flat.z);
				walk(figure, last);
				face(figure, pulled);
				turnTowards(player, figure.getEyePosition().lerp(canvas, 0.35), 0.25F);
				if (tick % 9 == 0) {
					Looks.swing(figure);
					sound(level, pulled, SoundEvents.SKELETON_STEP, SoundSource.HOSTILE, 1.2F, 0.7F);
					particles(level, ParticleTypes.POOF, player.position().add(pulled.subtract(head)), 3, 0.2, 0.01);
					hurt(level, player, SoundEvents.PLAYER_HURT, 0.1F);
				}
			} else {
				// Up and into the canvas it goes, smaller and smaller, the way it came.
				float q = (tick - climbing) / (float) CLIMB;
				scale(1.0 - 0.85 * Math.min(1.0F, q));
				walk(figure, last.lerp(canvas.add(0.0, -0.3, 0.0), Math.min(1.0F, q)));
				face(figure, canvas.subtract(normal));
				turnTowards(player, canvas, 0.3F);
				if (q >= 1.0F) {
					despawn(level, figure);
					figure = null;
					sound(level, canvas, SoundEvents.SKELETON_AMBIENT, SoundSource.HOSTILE, 1.5F, 0.5F);
					particles(level, ParticleTypes.POOF, canvas.add(normal.scale(0.2)), 8, 0.4, 0.02);
				}
			}
		} else {
			// Alone on the last metre, and the picture gets bigger and bigger.
			turnTowards(player, canvas, 0.4F);
			if (tick % 4 == 0) {
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.1F);
			}
		}
		tint = Math.max(0.0F, (p - 0.9F) / 0.1F);

		if (tick == INSIDE) {
			tint = 1.0F;
			sound(level, canvas, SoundEvents.PAINTING_PLACE, SoundSource.NEUTRAL, 2.0F, 0.6F);
			particles(level, ParticleTypes.POOF, canvas.add(normal.scale(0.2)), 20, 0.5, 0.03);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void scale(double size) {
		AttributeInstance scale = figure.getAttribute(Attributes.SCALE);
		if (scale != null) {
			scale.setBaseValue(size);
		}
		figure.refreshDimensions();
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		figure = null;
		climbing = -1;
		tint = 0.0F;
		moveCamera(Vec3.ZERO);
		super.end(level, player);
	}
}
