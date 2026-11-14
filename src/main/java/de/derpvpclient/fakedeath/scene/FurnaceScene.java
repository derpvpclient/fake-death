package de.derpvpclient.fakedeath.scene;

import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The furnace comes alive. It lights up by itself, glows, crackles, rattles on the spot, its door clattering, smoke
 * pouring out and lumps of coal flying. Then it hops: once, twice, three times, right up to the player, turns its
 * glowing mouth to them and spits a ball of fire into their face.
 */
public class FurnaceScene extends Scene {
	private static final int RATTLE = 20;
	private static final int HOPS = 75;
	private static final int HOP = 14;
	private static final int HOP_COUNT = 3;
	private static final int AIM = HOPS + HOP * HOP_COUNT + 6;
	private static final int SPIT = AIM + 14;
	private static final int FLIGHT = 8;
	private static final int HIT = SPIT + FLIGHT;

	private final BlockPos found;
	private final List<Coal> coal = new ArrayList<>();
	private Display.BlockDisplay body;
	private Display.ItemDisplay fireball;
	private BlockState lit;
	private Vec3 home;
	private Vec3 at;
	private float yaw;
	private int flash;

	public FurnaceScene(BlockPos furnace) {
		this.found = furnace;
	}

	@Override
	public int length() {
		return HIT + 16;
	}

	@Override
	public int photoTick() {
		return SPIT + 3;
	}

	@Override
	public int screenTint() {
		return flash > 0 ? (Math.min(200, flash * 25) << 24) | 0xFF8030 : 0;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.ofen", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			// The player's own furnace, swapped for a moving copy of itself that is burning.
			BlockState real = level.getBlockState(found);
			// Facing north in the model, the copy is then turned so its glowing front looks at the player.
			lit = real.hasProperty(FurnaceBlock.LIT) ? real.setValue(FurnaceBlock.LIT, true).setValue(FurnaceBlock.FACING, Direction.NORTH) : real;
			home = Vec3.atBottomCenterOf(found);
			at = home;
			hideBlock(level, found);
			body = blockDisplay(level, lit, home, 4.0F);
			Vec3 toPlayer = player.position().subtract(home);
			yaw = (float) Math.atan2(toPlayer.x, toPlayer.z);
			sound(level, home, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.5F, 0.6F);
		}
		if (flash > 0) {
			flash--;
		}

		float tilt = 0.0F;
		float lift = 0.0F;
		float turn = 0.0F;
		if (tick >= RATTLE && tick < HOPS) {
			// Rattling on the spot, harder and harder, the door clattering.
			float p = (tick - RATTLE) / (float) (HOPS - RATTLE);
			tilt = Mth.sin(tick * 1.7F) * 0.08F * (0.3F + p);
			lift = Math.abs(Mth.sin(tick * 1.1F)) * 0.08F * p;
			if (tick % Math.max(2, 7 - (int) (p * 5)) == 0) {
				sound(level, at, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.7F, 1.4F + random.nextFloat() * 0.3F);
			}
			if (tick % 9 == 0) {
				throwCoal(level, tick);
			}
		}
		int hopping = tick - HOPS;
		if (hopping >= 0 && hopping < HOP * HOP_COUNT) {
			// Three hops towards the player, a thud at each landing.
			int hop = hopping / HOP;
			float p = (hopping % HOP) / (float) HOP;
			Vec3 target = player.position().subtract(home).normalize().scale(Math.max(0.0, home.distanceTo(player.position()) - 1.4));
			Vec3 from = home.add(target.scale(hop / (double) HOP_COUNT));
			Vec3 to = home.add(target.scale((hop + 1) / (double) HOP_COUNT));
			at = from.lerp(to, p);
			at = new Vec3(at.x, floor(level, at.x, at.z, player.getY()), at.z);
			lift = Mth.sin(p * Mth.PI) * 0.9F;
			tilt = Mth.sin(p * Mth.TWO_PI) * 0.15F;
			if (hopping % HOP == HOP - 1) {
				sound(level, at, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6F, 0.6F);
				sound(level, at, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.6F);
				particles(level, ParticleTypes.LARGE_SMOKE, at.add(0.0, 0.2, 0.0), 6, 0.4, 0.05);
				cameraShake(0.15F);
			}
		}
		if (tick >= AIM && tick < SPIT) {
			// Rearing back to spit.
			float p = (tick - AIM) / (float) (SPIT - AIM);
			tilt = -0.25F * p;
			lift = 0.1F * p;
			if (tick == AIM) {
				sound(level, at, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.6F, 0.5F);
			}
		}
		if (tick >= SPIT && tick < SPIT + 4) {
			tilt = 0.2F;
		}
		place(tick, tilt, lift, turn);

		// Glowing, smoking and crackling all the way.
		Vec3 mouth = at.add(Math.sin(yaw) * 0.55, 0.5, Math.cos(yaw) * 0.55);
		if (tick > 5 && tick < HIT) {
			particles(level, ParticleTypes.FLAME, mouth, 1 + tick / 40, 0.15, 0.02);
			if (tick % 3 == 0) {
				particles(level, ParticleTypes.LARGE_SMOKE, at.add(0.0, 1.1, 0.0), 1, 0.2, 0.02);
			}
			if (tick % 11 == 0) {
				sound(level, at, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.2F, 0.8F);
			}
		}
		for (Coal piece : coal) {
			int age = tick - piece.start();
			if (age <= 16) {
				Vec3 pos = piece.from().add(piece.velocity().x * age, piece.velocity().y * age - 0.03 * age * age, piece.velocity().z * age);
				pos = new Vec3(pos.x, Math.max(pos.y, floor(level, pos.x, pos.z, home.y) + 0.1), pos.z);
				place(piece.item(), pos, new Vector3f(0.35F), new Quaternionf().rotateXYZ(age * 0.4F, age * 0.3F, 0.0F));
			}
		}

		if (!thirdPerson() || tick < SPIT) {
			turnTowards(player, at.add(0.0, 0.6, 0.0), 0.12F);
		}

		if (tick == SPIT) {
			fireball = itemDisplay(level, new ItemStack(Items.FIRE_CHARGE), mouth);
			((de.derpvpclient.fakedeath.mixin.DisplayAccessor) fireball).fakedeath$setBillboard(Display.BillboardConstraints.CENTER);
			sound(level, mouth, SoundEvents.GHAST_SHOOT, SoundSource.BLOCKS, 1.5F, 0.7F);
			sound(level, mouth, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.5F, 0.8F);
		}
		if (fireball != null && tick > SPIT && tick <= HIT) {
			float p = (tick - SPIT) / (float) FLIGHT;
			Vec3 pos = mouth.lerp(player.getEyePosition(), p);
			place(fireball, pos, 0.6F + p * 0.6F);
			particles(level, ParticleTypes.FLAME, pos, 4, 0.1, 0.03);
		}
		if (tick == HIT) {
			despawn(level, fireball);
			fireball = null;
			flash = 8;
			sound(level, player.position(), Looks.EXPLODE, SoundSource.BLOCKS, 1.0F, 1.3F);
			particles(level, ParticleTypes.FLAME, player.getEyePosition(), 30, 0.5, 0.15);
			setOnFire(player);
			kill(level, player, SoundEvents.PLAYER_HURT_ON_FIRE);
		}
	}

	/** The furnace's copy, turned towards the player, leaning and jumping around its foot. */
	private void place(int tick, float tilt, float lift, float turn) {
		body.setPos(at.x, at.y, at.z);
		Quaternionf rotation = new Quaternionf().rotateY(yaw + Mth.PI + turn).rotateX(tilt);
		Vector3f offset = new Vector3f(-0.5F, 0.0F, -0.5F).rotate(rotation).add(0.0F, lift, 0.0F);
		transform(body, new Transformation(offset, rotation, new Vector3f(1.0F), new Quaternionf()));
	}

	private void throwCoal(ClientLevel level, int tick) {
		Vec3 from = at.add(Math.sin(yaw) * 0.55, 0.5, Math.cos(yaw) * 0.55);
		Display.ItemDisplay piece = itemDisplay(level, new ItemStack(random.nextBoolean() ? Items.COAL : Items.CHARCOAL), from);
		Vec3 out = new Vec3(Math.sin(yaw + (random.nextFloat() - 0.5F) * 1.4F), 0.0, Math.cos(yaw + (random.nextFloat() - 0.5F) * 1.4F));
		coal.add(new Coal(piece, from, out.scale(0.12).add(0.0, 0.22, 0.0), tick));
		sound(level, from, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 0.5F);
	}

	private record Coal(Display.ItemDisplay item, Vec3 from, Vec3 velocity, int start) {
	}
}
