package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The caretaker is mopping, the floor shines wet, a yellow warning sign stands next to it. "No running in here!" The
 * player slides over the wet floor, arms flailing, straight into the open hatch. From down below they see him look
 * over the edge: "Told you."
 */
public class CaretakerScene extends NpcScene {
	private static final double SLIDE = 3.0;
	private static final int DEPTH = 8;
	private static final int SLID = 28;
	private static final int FALL = SLID + 22;
	private static final int PEEK = FALL + 10;
	private static final int PEEKED = FALL + 26;
	/** His bucket slips out of his hand and comes down the shaft. */
	private static final int DROP = PEEKED + 40;
	private static final int HIT = DROP + 12;
	private static final int MOP_EVERY = 16;
	/** Before the slide he mops right under the feet, and the last swipe of the mop takes them away. */
	private static final int SWEEP = 18;
	private static final int SWIPE = 13;

	private final List<Display.BlockDisplay> puddles = new ArrayList<>();
	private Vec3 slideDir;
	private Vec3 wayDir;
	private BlockPos hole;
	private Vec3 peekFrom;
	private boolean landed;
	private Display.ItemDisplay bucket;
	private Vec3 dropFrom;
	private Vec3 sweepFrom;

	public CaretakerScene() {
		super("hausmeister", ChatFormatting.DARK_GREEN);
	}

	@Override
	protected int lineCount() {
		return 3;
	}

	@Override
	protected int actLength() {
		return SWEEP + HIT + 40;
	}

	@Override
	protected ResourceKey<VillagerProfession> profession() {
		return VillagerProfession.NITWIT;
	}

	@Override
	protected ItemStack held() {
		return Looks.prop(Items.BRUSH, "mop");
	}

	@Override
	protected Vec3 focus(LocalPlayer player) {
		if (sweepFrom != null && slideDir == null) {
			// Looking down at the mop sweeping around the own feet.
			return player.position().add(0.0, 0.1, 0.0);
		}
		if (slideDir == null) {
			return npc.getEyePosition();
		}
		if (peekFrom != null) {
			// Down in the hole, looking up at him over the edge.
			return npc.getEyePosition();
		}
		if (landed) {
			return Vec3.atBottomCenterOf(hole).add(0.0, 3.0, 0.0);
		}
		return player.getEyePosition().add(slideDir.scale(5.0)).add(0.0, -2.0, 0.0);
	}

	/** The wet floor from the caretaker to beyond the player, and the warning sign next to it. */
	@Override
	protected void setup(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
		Vec3 toward = player.position().subtract(stand);
		wayDir = new Vec3(toward.x, 0.0, toward.z).normalize();
		// From where he mops up to the last tile before the hatch.
		double length = stand.distanceTo(player.position()) + SLIDE - 1.0;
		for (double d = 0.0; d <= length; d += 1.0) {
			BlockPos block = BlockPos.containing(stand.add(wayDir.scale(d)));
			Vec3 at = new Vec3(block.getX() + 0.5, floor(level, block.getX() + 0.5, block.getZ() + 0.5, player.getY()) + 0.01, block.getZ() + 0.5);
			// A thin shiny layer of water on the floor.
			Display.BlockDisplay puddle = blockDisplay(level, Looks.LIGHT_BLUE_GLASS, at, 2.0F);
			place(puddle, at, new Vector3f(0.98F, 0.02F, 0.98F), new Quaternionf());
			puddles.add(puddle);
		}
		warningSign(level, player.position().add(wayDir.yRot((float) Math.PI / 2.0F).scale(1.4)).add(wayDir.scale(1.0)), player);
	}

	/** The classic yellow wet floor sign: two yellow boards leaning against each other, a black "!" on each. */
	private void warningSign(ClientLevel level, Vec3 base, LocalPlayer player) {
		Vec3 ground = new Vec3(base.x, floor(level, base.x, base.z, player.getY()), base.z);
		Vec3 toPlayer = player.position().subtract(ground);
		float yaw = (float) Math.atan2(toPlayer.x, toPlayer.z);
		for (int side : new int[] {-1, 1}) {
			Quaternionf turn = new Quaternionf().rotateY(yaw).rotateX(side * 0.28F);
			Vector3f normal = new Vector3f(0.0F, 0.0F, side).rotate(new Quaternionf().rotateY(yaw));
			Vec3 board = ground.add(normal.x * 0.11, 0.4, normal.z * 0.11);
			place(blockDisplay(level, Looks.YELLOW_CONCRETE, board, 2.0F), board, new Vector3f(0.5F, 0.8F, 0.03F), turn);
			Vector3f out = new Vector3f(0.0F, 0.0F, side * 0.02F).rotate(turn);
			Vec3 bar = board.add(out.x, 0.08 + out.y, out.z);
			place(blockDisplay(level, Looks.BLACK_CONCRETE, bar, 1.0F), bar, new Vector3f(0.08F, 0.28F, 0.03F), turn);
			Vec3 dot = board.add(out.x, -0.16 + out.y, out.z);
			place(blockDisplay(level, Looks.BLACK_CONCRETE, dot, 1.0F), dot, new Vector3f(0.08F, 0.08F, 0.03F), turn);
		}
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		super.tick(minecraft, level, player, tick);
		// Mopping while talking: the mop swings, the floor splashes.
		if (npc != null && tick < actStart() && tick % MOP_EVERY == 0) {
			Looks.swing(npc);
			particles(level, ParticleTypes.SPLASH, npc.position().add(wayDir.scale(0.8)).add(0.0, 0.1, 0.0), 10, 0.4, 0.1);
			sound(level, npc.position(), SoundEvents.BUCKET_EMPTY, SoundSource.NEUTRAL, 0.3F, 1.6F);
		}
		if (tick % 5 == 0) {
			for (Display.BlockDisplay puddle : puddles) {
				if (random.nextInt(4) == 0) {
					level.addParticle(ParticleTypes.FALLING_WATER, puddle.getX() + random.nextDouble() - 0.5, puddle.getY() + 0.05, puddle.getZ() + random.nextDouble() - 0.5, 0.0, 0.0, 0.0);
				}
			}
		}
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		if (t < SWEEP) {
			sweep(minecraft, level, player, t);
			return;
		}
		slide(minecraft, level, player, t - SWEEP);
	}

	/** He steps up to the player and mops the floor right around their feet, soap and water everywhere; the last swipe sweeps the feet away. */
	private void sweep(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		Vec3 dir = towardPlayer(player);
		if (t == 0) {
			sweepFrom = npc.position();
			speak(minecraft, level, "fakedeath.hausmeister.feet");
		}
		Vec3 feet = player.position();
		Vec3 beside = feet.subtract(dir.scale(1.2));
		float p = Math.min(1.0F, t / 8.0F);
		Vec3 at = sweepFrom.lerp(beside, p);
		walk(npc, new Vec3(at.x, floor(level, at.x, at.z, player.getY()), at.z));
		face(npc, feet);
		if (t % 4 == 1) {
			Looks.swing(npc);
			particles(level, ParticleTypes.SPLASH, feet.add(0.0, 0.1, 0.0), 12, 0.35, 0.15);
			particles(level, ParticleTypes.BUBBLE_POP, feet.add(0.0, 0.15, 0.0), 6, 0.3, 0.02);
			sound(level, feet, SoundEvents.BUCKET_EMPTY, SoundSource.NEUTRAL, 0.4F, 1.7F);
		}
		if (t == SWIPE) {
			// The big swipe: the mop goes right through the feet.
			Looks.swing(npc);
			sound(level, feet, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 1.0F, 1.2F);
			particles(level, ParticleTypes.SPLASH, feet.add(0.0, 0.1, 0.0), 30, 0.5, 0.3);
		}
		if (t > SWIPE) {
			// The feet go, the body tips back for a moment.
			moveCamera(new Vec3(0.0, -0.08 * (t - SWIPE), 0.0));
		}
	}

	private void slide(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		if (t == 0) {
			// Away from the caretaker, over the wet floor, to the hatch he left open.
			slideDir = towardPlayer(player);
			hole = BlockPos.containing(player.position().add(slideDir.scale(SLIDE)));
			for (int i = 0; i <= DEPTH; i++) {
				hideBlock(level, hole.below(i));
			}
			hideBlock(level, hole.above());
			Direction away = Math.abs(slideDir.x) > Math.abs(slideDir.z) ? (slideDir.x > 0 ? Direction.EAST : Direction.WEST) : (slideDir.z > 0 ? Direction.SOUTH : Direction.NORTH);
			// The lid stands open on the far side of the hole.
			replaceBlock(level, hole, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, true).setValue(TrapDoorBlock.FACING, away.getOpposite()));
			sound(level, player.position(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.PLAYERS, 1.5F, 1.4F);
		}
		if (t < SLID) {
			float p = t / (float) SLID;
			Vec3 side = slideDir.yRot((float) Math.PI / 2.0F);
			// Sliding faster and faster, wobbling and flailing to keep the balance.
			double wobble = Math.sin(t * 0.7) * 0.18 * p;
			moveCamera(slideDir.scale(SLIDE * p * p).add(side.scale(wobble)).add(0.0, Math.abs(Math.sin(t * 0.9)) * 0.08, 0.0));
			particles(level, ParticleTypes.SPLASH, player.position().add(slideDir.scale(SLIDE * p * p)), 6, 0.3, 0.15);
			if (t % 4 == 0) {
				Looks.swing(player);
			}
			if (t % 8 == 4) {
				sound(level, player.position(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.PLAYERS, 1.0F, 1.2F + p * 0.5F);
			}
		} else {
			// Down the middle of the shaft onto its floor, lying there looking up.
			float p = Math.min(1.0F, (t - SLID) / (float) (FALL - SLID));
			Vec3 eyes = player.getEyePosition();
			Vec3 bottom = Vec3.atBottomCenterOf(hole.below(DEPTH)).add(0.0, 0.6, 0.0).subtract(eyes);
			Vec3 edge = slideDir.scale(SLIDE);
			Vec3 middle = new Vec3(bottom.x, 0.0, bottom.z);
			Vec3 across = edge.lerp(middle, Math.min(1.0F, p * 3.0F));
			moveCamera(new Vec3(across.x, bottom.y * p * p, across.z));
			if (t == SLID) {
				sound(level, Vec3.atCenterOf(hole), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.8F);
			}
		}
		if (t == FALL) {
			landed = true;
			sound(level, player.position(), SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 1.5F, 1.0F);
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.75F);
		}
		if (t == PEEK) {
			peekFrom = npc.position();
		}
		Vec3 camera = player.getEyePosition().add(de.derpvpclient.fakedeath.CameraDirector.positionOffsetNow());
		if (peekFrom != null) {
			// He walks to the edge, toes over it, and looks down.
			Vec3 edge = Vec3.atBottomCenterOf(hole).subtract(slideDir.scale(0.45));
			float p = Math.min(1.0F, (t - PEEK) / (float) (PEEKED - PEEK));
			Vec3 at = peekFrom.lerp(edge, p);
			walk(npc, new Vec3(at.x, floor(level, at.x, at.z, player.getY()), at.z));
			face(npc, camera);
		}
		if (t == PEEKED + 4) {
			speak(minecraft, level, "fakedeath.hausmeister.told");
		}
		dropBucket(level, player, camera, t);
	}

	@Override
	public float lying() {
		// In F5 the body lies at the bottom of the shaft.
		return landed ? 1.0F : 0.0F;
	}

	/** While he looks down, his mop bucket slips out of his hand and falls straight down the shaft onto the player. */
	private void dropBucket(ClientLevel level, LocalPlayer player, Vec3 camera, int t) {
		if (t == DROP) {
			dropFrom = npc.getEyePosition().add(slideDir.scale(0.4)).add(0.0, -0.5, 0.0);
			bucket = itemDisplay(level, new ItemStack(Items.WATER_BUCKET), dropFrom);
			((de.derpvpclient.fakedeath.mixin.DisplayAccessor) bucket).fakedeath$setBillboard(Display.BillboardConstraints.CENTER);
			place(bucket, dropFrom, 0.5F);
			sound(level, dropFrom, SoundEvents.VILLAGER_HURT, SoundSource.NEUTRAL, 1.0F, 1.3F);
			speak(Minecraft.getInstance(), level, "fakedeath.hausmeister.oops");
		}
		if (bucket != null && t > DROP && t <= HIT) {
			float p = (t - DROP) / (float) (HIT - DROP);
			// Into the middle of the shaft first, then down it, faster and faster.
			Vec3 above = new Vec3(camera.x, dropFrom.y, camera.z);
			Vec3 at = dropFrom.lerp(above, Math.min(1.0F, p * 4.0F)).add(0.0, (camera.y + 0.3 - dropFrom.y) * p * p, 0.0);
			place(bucket, at, 0.5F + p * 0.3F);
		}
		if (t == HIT) {
			despawn(level, bucket);
			bucket = null;
			particles(level, ParticleTypes.SPLASH, camera, 40, 0.4, 0.3);
			sound(level, camera, SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.5F, 0.9F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		if (bucket != null) {
			despawn(level, bucket);
			bucket = null;
		}
		super.end(level, player);
	}
}
