package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The armor stand next to the player comes alive: it shakes, hops over, and pulls the player's own armor off over
 * their head, piece by piece, and puts it on. Fully dressed it finishes the job.
 */
public class ArmorStandScene extends Scene {
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final Item[] SPARE = {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS};
	private static final double[] WORN_AT = {1.7, 1.2, 0.7, 0.2};
	private static final int WAKE = 22;
	private static final int ARRIVE = 54;
	private static final int FIRST = 60;
	private static final int STEP = 16;
	private static final int PULL = 9;
	private static final int WEAR = 14;
	private static final int SLAP = FIRST + 3 * STEP + WEAR + 12;
	private final Entity found;
	private final ItemStack[] before = new ItemStack[SLOTS.length];
	private LivingEntity stand;
	private boolean real;
	private Vec3 home;
	private float homeYaw;
	private Vec3 near;
	private Display.ItemDisplay piece;
	private ItemStack pieceStack;

	public ArmorStandScene(Entity stand) {
		this.found = stand;
	}

	/** A visible armor stand close to the player, or null. Invisible ones are name tags and decoration of servers. */
	public static Entity near(ClientLevel level, LocalPlayer player, double reach) {
		Entity best = null;
		for (Entity entity : level.entitiesForRendering()) {
			if (entity.getType() == EntityType.ARMOR_STAND && entity instanceof LivingEntity && entity.isAlive() && !entity.isInvisible()
				&& entity.distanceToSqr(player) <= reach * reach && Math.abs(entity.getY() - player.getY()) < 2.0
				&& (best == null || entity.distanceToSqr(player) < best.distanceToSqr(player))) {
				best = entity;
			}
		}
		return best;
	}

	@Override
	public int length() {
		return SLAP + 25;
	}

	@Override
	public int photoTick() {
		return FIRST + STEP + 5;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.ruestungsstaender", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			if (found instanceof LivingEntity living && living.isAlive()) {
				stand = living;
				real = true;
			} else {
				// Asked for by name with no stand around: one is put up in front, only on this client.
				stand = spawn(level, EntityType.ARMOR_STAND, groundSpot(level, player, 3.0, 0.0));
				face(stand, player.getEyePosition());
			}

			home = stand.position();
			homeYaw = stand.getYRot();
			for (int i = 0; i < SLOTS.length; i++) {
				before[i] = stand.getItemBySlot(SLOTS[i]).copy();
			}

			Vec3 away = home.subtract(player.position());
			Vec3 flat = new Vec3(away.x, 0.0, away.z);
			Vec3 dir = flat.lengthSqr() < 0.05 ? horizontalLook(player) : flat.normalize();
			near = player.position().add(dir.scale(1.9));
		}

		Vec3 head = player.getEyePosition();
		Vec3 at;

		if (tick < WAKE) {
			// It wakes up: a twitch, a pause, a harder twitch.
			at = home;
			float shake = tick < 8 ? 10.0F : tick < 13 ? 0.0F : 28.0F;
			turn(homeYaw + (float) Math.sin(tick * 1.9) * shake);
			if (tick == 2 || tick == 14 || tick == 18) {
				sound(level, home, SoundEvents.ARMOR_STAND_HIT, SoundSource.NEUTRAL, 1.2F, 0.8F + tick * 0.02F);
			}
		} else if (tick < ARRIVE) {
			// Hop, hop, hop, over to the player.
			float p = (tick - WAKE) / (float) (ARRIVE - WAKE);
			double hop = Math.abs(Math.sin(p * Math.PI * 4.0));
			Vec3 flat = home.lerp(near, p);
			at = new Vec3(flat.x, floor(level, flat.x, flat.z, flat.y) + hop * 0.55, flat.z);
			if (hop < 0.2 && tick % 2 == 0) {
				sound(level, at, SoundEvents.ARMOR_STAND_PLACE, SoundSource.NEUTRAL, 1.2F, 1.1F);
				particles(level, ParticleTypes.POOF, at, 3, 0.15, 0.01);
			}
		} else {
			at = new Vec3(near.x, floor(level, near.x, near.z, near.y), near.z);
		}

		for (int i = 0; i < SLOTS.length; i++) {
			int t = tick - (FIRST + i * STEP);
			if (t < 0 || t > WEAR) {
				continue;
			}

			if (t == 0) {
				ItemStack worn = player.getItemBySlot(SLOTS[i]);
				pieceStack = worn.isEmpty() ? new ItemStack(SPARE[i]) : worn.copy();
				piece = itemDisplay(level, pieceStack.copy(), player.position().add(0.0, WORN_AT[i], 0.0));
				sound(level, head, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 1.2F, 0.7F);
			}

			// A grab: the stand jumps at the player for the pull.
			at = at.lerp(player.position(), t < PULL ? 0.35 * Math.sin(t / (double) PULL * Math.PI) : 0.0)
				.add(0.0, t < PULL ? 0.3 * Math.sin(t / (double) PULL * Math.PI) : 0.0, 0.0);

			Vec3 look = horizontalLook(player);
			Vec3 from = player.position().add(look.scale(0.75)).add(0.0, WORN_AT[i], 0.0);
			Vec3 over = head.add(look.scale(0.75)).add(0.0, 0.75, 0.0);
			Vec3 spot;
			float tilt;

			if (t <= PULL) {
				// Up along the body and over the ears, tilting as it comes off.
				float p = t / (float) PULL;
				spot = from.lerp(over, p * p);
				tilt = p * 1.4F;
			} else {
				float p = (t - PULL) / (float) (WEAR - PULL);
				spot = over.lerp(at.add(0.0, WORN_AT[i], 0.0), p);
				tilt = 1.4F * (1.0F - p);
			}

			float yaw = (float) Math.atan2(head.x - spot.x, head.z - spot.z);
			place(piece, spot, new Vector3f(0.6F), new Quaternionf().rotateY(yaw).rotateZ(tilt));

			if (t == PULL) {
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.17F);
				particles(level, ParticleTypes.CRIT, head, 8, 0.3, 0.3);
			}
			if (t == WEAR) {
				despawn(level, piece);
				piece = null;
				stand.setItemSlot(SLOTS[i], pieceStack);
				sound(level, at, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 1.5F, 1.0F);
			}
		}

		if (tick > SLAP - 8 && tick <= SLAP) {
			// Fully dressed: one run-up, one headbutt.
			float p = (tick - (SLAP - 8)) / 8.0F;
			at = p < 0.5F ? at.lerp(home, p * 0.3) : at.lerp(player.position(), (p - 0.5F) * 1.7);
			at = at.add(0.0, Math.sin(p * Math.PI) * 0.4, 0.0);
		}
		if (tick > SLAP) {
			at = at.lerp(player.position(), 0.5);
		}

		stand.setPos(at.x, at.y, at.z);
		if (tick >= WAKE) {
			face(stand, head);
			stand.setXRot(0.0F);
		}
		turnTowards(player, at.add(0.0, 1.5, 0.0), 0.18F);

		if (tick == SLAP) {
			sound(level, head, SoundEvents.ARMOR_STAND_BREAK, SoundSource.NEUTRAL, 1.5F, 0.7F);
			sound(level, head, SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 0.8F, 1.3F);
			particles(level, ParticleTypes.CRIT, head, 30, 0.4, 0.5);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void turn(float yaw) {
		stand.setYRot(yaw);
		stand.setYHeadRot(yaw);
		stand.setYBodyRot(yaw);
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		// A real stand was only borrowed: back to its place, in what it wore before.
		if (real && stand != null) {
			stand.setPos(home.x, home.y, home.z);
			turn(homeYaw);
			for (int i = 0; i < SLOTS.length; i++) {
				stand.setItemSlot(SLOTS[i], before[i]);
			}
		}
		stand = null;
		super.end(level, player);
	}
}
