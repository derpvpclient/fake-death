package de.derpvpclient.fakedeath.scene;

import com.mojang.math.Transformation;
import de.derpvpclient.fakedeath.CameraDirector;
import de.derpvpclient.fakedeath.FakeDeath;
import de.derpvpclient.fakedeath.mixin.BlockDisplayAccessor;
import de.derpvpclient.fakedeath.mixin.DisplayAccessor;
import de.derpvpclient.fakedeath.mixin.FallingBlockEntityAccessor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A short cutscene that plays before the fake death screen opens. Everything a scene spawns or
 * changes exists only on this client and is undone in {@link #end}, the server never hears of it.
 */
public abstract class Scene {
	private static final int BLOCK_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

	// Server entity ids count up from zero, so client-only actors stay far below that.
	private static int nextEntityId = -1_000_000;

	protected final RandomSource random = RandomSource.create();
	private final List<Entity> actors = new ArrayList<>();
	private final Map<BlockPos, BlockChange> changedBlocks = new LinkedHashMap<>();
	private final List<Holder<MobEffect>> addedEffects = new ArrayList<>();
	private boolean setOnFire;
	private int frozenBefore = -1;

	/** Ticks the scene plays before the death screen opens. */
	public abstract int length();

	public abstract Component deathMessage(LocalPlayer player);

	/** Called every tick from the start of the scene until the fake is over, also while the death screen is open. */
	public abstract void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick);

	/** The death screen was clicked away and the player learns it was a fake. */
	public void onReveal(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
	}

	/** Ticks the scene keeps playing after the reveal before its actors disappear. */
	public int outroLength() {
		return 0;
	}

	/** A hand whose item the scene has taken away, so it is not drawn in the first-person view. Null for none. */
	public InteractionHand hiddenHand() {
		return null;
	}

	/** True while the player sits on something the scene brought, like a horse: in third person the body sits too. */
	public boolean seated() {
		return false;
	}

	/** True while the moved camera walks along: in third person the body moves its legs. */
	public boolean walking() {
		return false;
	}

	/** How flat the body lies on the ground in third person, from 0 standing to 1 face down, like after falling into a bush. */
	public float lying() {
		return 0.0F;
	}

	/** The best moment of the scene for a picture, used by the demo run. */
	public int photoTick() {
		return length() * 2 / 3;
	}

	/** A color (ARGB) laid over the world below the HUD, like the dark inside of a frog. 0 for none. */
	public int screenTint() {
		return 0;
	}

	/** Removes every trace of the scene from the client. */
	public void end(ClientLevel level, LocalPlayer player) {
		for (Entity actor : actors) {
			level.removeEntity(actor.getId(), Entity.RemovalReason.DISCARDED);
		}
		actors.clear();

		changedBlocks.forEach((pos, change) -> {
			// Only put the block back if nothing else changed it in the meantime.
			if (level.getBlockState(pos).is(change.replacement().getBlock())) {
				level.setBlock(pos, change.original(), BLOCK_FLAGS);
			}
		});
		changedBlocks.clear();

		for (Holder<MobEffect> effect : addedEffects) {
			player.removeEffect(effect);
		}
		addedEffects.clear();

		if (setOnFire) {
			player.setRemainingFireTicks(0);
			setOnFire = false;
		}

		if (frozenBefore >= 0) {
			player.setTicksFrozen(frozenBefore);
			frozenBefore = -1;
		}
	}

	protected <T extends Entity> T spawn(ClientLevel level, EntityType<T> type, Vec3 pos) {
		T entity = type.create(level, EntitySpawnReason.COMMAND);
		if (entity == null) {
			throw new IllegalStateException("Could not create " + type);
		}
		entity.setId(nextEntityId--);
		entity.snapTo(pos);
		entity.setSilent(true);
		level.addEntity(entity);
		actors.add(entity);
		return entity;
	}

	protected <T extends Entity> T spawn(ClientLevel level, T entity, Vec3 pos) {
		entity.setId(nextEntityId--);
		entity.snapTo(pos);
		entity.setSilent(true);
		level.addEntity(entity);
		actors.add(entity);
		return entity;
	}

	protected void despawn(ClientLevel level, Entity entity) {
		if (actors.remove(entity)) {
			level.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
		}
	}

	/** Lets a block fall from where it stands, the real block is hidden until the scene ends. */
	protected FallingBlockEntity dropBlock(ClientLevel level, BlockPos pos, Vec3 velocity) {
		BlockState state = level.getBlockState(pos);
		hideBlock(level, pos);
		return fallingBlock(level, state, Vec3.atBottomCenterOf(pos), velocity);
	}

	protected FallingBlockEntity fallingBlock(ClientLevel level, BlockState state, Vec3 pos, Vec3 velocity) {
		FallingBlockEntity block = spawn(level, FallingBlockEntityAccessor.fakedeath$create(level, pos.x, pos.y, pos.z, state), pos);
		block.setDeltaMovement(velocity);
		return block;
	}

	protected void hideBlock(ClientLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).isAir()) {
			changeBlock(level, pos, Blocks.AIR.defaultBlockState());
		}
	}

	/** Puts a block into the client's world only, like fire around the player. Air only, nothing real is replaced. */
	protected boolean placeBlock(ClientLevel level, BlockPos pos, BlockState state) {
		if (!level.getBlockState(pos).isAir() || changedBlocks.containsKey(pos)) {
			return false;
		}

		changeBlock(level, pos, state);
		return true;
	}

	/** Lights a fire on a free spot around the player, only on this client. The player's own block stays free. */
	protected boolean igniteNear(ClientLevel level, LocalPlayer player, int radius) {
		BlockPos feet = player.blockPosition();

		for (int attempt = 0; attempt < 10; attempt++) {
			BlockPos pos = feet.offset(random.nextInt(radius * 2 + 1) - radius, random.nextInt(3) - 1, random.nextInt(radius * 2 + 1) - radius);

			if (!pos.equals(feet) && !pos.equals(feet.above()) && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP) && !onTree(level, pos)
				&& placeBlock(level, pos, Blocks.FIRE.defaultBlockState())) {
				return true;
			}
		}

		return false;
	}

	/** Swaps any block for another one on this client only, put back when the scene ends. */
	protected void replaceBlock(ClientLevel level, BlockPos pos, BlockState replacement) {
		changeBlock(level, pos, replacement);
	}

	/**
	 * Opens a shaft of the given depth right under the player. The block under the feet becomes an invisible barrier,
	 * so the player really keeps standing and the server sees nothing; only the camera falls into the hole.
	 */
	protected void openShaft(ClientLevel level, LocalPlayer player, int depth) {
		BlockPos below = player.blockPosition().below();
		replaceBlock(level, below, Blocks.BARRIER.defaultBlockState());

		for (int i = 1; i <= depth; i++) {
			hideBlock(level, below.below(i));
		}
	}

	/** An item as a client-only display entity, centered on its position. */
	protected Display.ItemDisplay itemDisplay(ClientLevel level, net.minecraft.world.item.ItemStack stack, Vec3 pos) {
		Display.ItemDisplay display = spawn(level, EntityType.ITEM_DISPLAY, pos);
		((de.derpvpclient.fakedeath.mixin.ItemDisplayAccessor) display).fakedeath$setItemStack(stack);
		((de.derpvpclient.fakedeath.mixin.ItemDisplayAccessor) display).fakedeath$setItemTransform(net.minecraft.world.item.ItemDisplayContext.FIXED);
		((DisplayAccessor) display).fakedeath$setWidth(4.0F);
		((DisplayAccessor) display).fakedeath$setHeight(4.0F);
		((DisplayAccessor) display).fakedeath$setInterpolationDuration(1);
		return display;
	}

	/** Puts a display at a spot with a size and a rotation, a block display stays centered on the spot. */
	protected static void place(Display display, Vec3 pos, Vector3f size, Quaternionf rotation) {
		display.setPos(pos.x, pos.y, pos.z);
		Vector3f offset = display instanceof Display.BlockDisplay ? new Vector3f(size).mul(-0.5F).rotate(rotation) : new Vector3f();
		transform(display, new Transformation(offset, rotation, size, new Quaternionf()));
	}

	protected static void place(Display display, Vec3 pos, float size) {
		place(display, pos, new Vector3f(size), new Quaternionf());
	}

	/** Shakes the camera a little, like an earthquake or heavy steps. 0 stops it. */
	protected void cameraShake(float strength) {
		moveCamera(strength <= 0.0F ? Vec3.ZERO
			: new Vec3((random.nextFloat() - 0.5F) * strength, (random.nextFloat() - 0.5F) * strength, (random.nextFloat() - 0.5F) * strength));
	}

	/** A point that circles around a center, for flying swarms and spinning cameras. */
	protected static Vec3 orbit(Vec3 center, double radius, double angle, double height) {
		return center.add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
	}

	private void changeBlock(ClientLevel level, BlockPos pos, BlockState replacement) {
		// A block changed twice (magma, then lava) still goes back to what was there first.
		BlockChange before = changedBlocks.get(pos);
		changedBlocks.put(pos.immutable(), new BlockChange(before != null ? before.original() : level.getBlockState(pos), replacement));
		level.setBlock(pos, replacement, BLOCK_FLAGS);
	}

	/** A block as a client-only display entity, so it can be moved and turned freely. */
	protected Display.BlockDisplay blockDisplay(ClientLevel level, BlockState state, Vec3 pos, float cullSize) {
		Display.BlockDisplay display = spawn(level, EntityType.BLOCK_DISPLAY, pos);
		((BlockDisplayAccessor) display).fakedeath$setBlockState(state);
		// Display entities are culled by a box around their position, the moving blocks reach far beyond it.
		((DisplayAccessor) display).fakedeath$setWidth(cullSize);
		((DisplayAccessor) display).fakedeath$setHeight(cullSize);
		((DisplayAccessor) display).fakedeath$setInterpolationDuration(1);
		return display;
	}

	/** Moves a display to a new transformation, smoothly over the next tick. */
	protected static void transform(Display display, Transformation transformation) {
		((DisplayAccessor) display).fakedeath$setTransformation(transformation);
		((DisplayAccessor) display).fakedeath$setInterpolationDelay(0);
	}

	private record BlockChange(BlockState original, BlockState replacement) {
	}

	protected void addEffect(LocalPlayer player, Holder<MobEffect> effect, int ticks) {
		if (!player.hasEffect(effect)) {
			player.addEffect(new MobEffectInstance(effect, ticks, 0));
			addedEffects.add(effect);
		}
	}

	protected void setOnFire(LocalPlayer player) {
		player.setRemainingFireTicks(20 * 20);
		setOnFire = true;
	}

	protected void freeze(LocalPlayer player) {
		chill(player, 1.0F);
	}

	/** Frost at the edge of the screen, from 0 (none) to 1 (frozen solid like in powder snow). */
	protected void chill(LocalPlayer player, float amount) {
		if (frozenBefore < 0) {
			frozenBefore = player.getTicksFrozen();
		}
		player.setTicksFrozen(Math.round(player.getTicksRequiredToFreeze() * amount) + (amount >= 1.0F ? 40 : 0));
	}

	/** A chat line that looks like a normal player message. */
	protected static void say(Minecraft minecraft, Component speaker, Component text) {
		minecraft.gui.getChat().addServerSystemMessage(Component.literal("<").append(speaker).append("> ").append(text));
	}

	protected static void sound(ClientLevel level, Vec3 pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
		level.playLocalSound(pos.x, pos.y, pos.z, sound, source, volume, pitch, false);
	}

	/** A fake hit: hurt animation, sound and the hearts drop by the given share of the starting health. */
	protected static void hurt(ClientLevel level, LocalPlayer player, SoundEvent sound, float share) {
		player.animateHurt(0.0F);
		sound(level, player.position(), sound, SoundSource.PLAYERS, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
		FakeDeath.fakeDamage(player, share);
	}

	protected static void hurt(ClientLevel level, LocalPlayer player, SoundEvent sound) {
		hurt(level, player, sound, 0.25F);
	}

	/** The killing blow: the hearts run out with this hit and the death screen opens right away, like a real death. */
	protected static void kill(ClientLevel level, LocalPlayer player, SoundEvent sound) {
		player.animateHurt(0.0F);
		sound(level, player.position(), sound, SoundSource.PLAYERS, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
		FakeDeath.killNow(player);
	}

	/** A normal hit, or the killing blow if it is the last one. */
	protected static void hit(ClientLevel level, LocalPlayer player, SoundEvent sound, float share, boolean last) {
		if (last) {
			kill(level, player, sound);
		} else {
			hurt(level, player, sound, share);
		}
	}

	protected static void hurt(ClientLevel level, LocalPlayer player) {
		hurt(level, player, SoundEvents.PLAYER_HURT);
	}

	protected static void particles(ClientLevel level, ParticleOptions particle, Vec3 pos, int count, double spread, double speed) {
		RandomSource random = level.getRandom();

		for (int i = 0; i < count; i++) {
			level.addParticle(particle,
				pos.x + (random.nextDouble() - 0.5) * 2 * spread, pos.y + (random.nextDouble() - 0.5) * 2 * spread, pos.z + (random.nextDouble() - 0.5) * 2 * spread,
				(random.nextDouble() - 0.5) * speed, (random.nextDouble() - 0.5) * speed, (random.nextDouble() - 0.5) * speed);
		}
	}

	protected static void line(ClientLevel level, ParticleOptions particle, Vec3 from, Vec3 to, double step) {
		Vec3 delta = to.subtract(from);
		int steps = Math.max(1, (int) (delta.length() / step));

		for (int i = 0; i <= steps; i++) {
			Vec3 point = from.add(delta.scale((double) i / steps));
			level.addParticle(particle, point.x, point.y, point.z, 0.0, 0.0, 0.0);
		}
	}

	/** Moves only the camera away from the player's eyes, for example pulled by a tongue. The player stays where they are. */
	protected static void moveCamera(Vec3 fromEyes) {
		CameraDirector.moveTo(fromEyes);
	}

	/** True while the player looks at themselves from outside (F5), some scenes then pick a different view. */
	protected static boolean thirdPerson() {
		return !Minecraft.getInstance().options.getCameraType().isFirstPerson();
	}

	/** Pans the camera towards a point. Only the camera, the player's real rotation is left alone. */
	protected static void turnTowards(LocalPlayer player, Vec3 target, float speed) {
		CameraDirector.turnTowards(player, target, speed);
	}

	/**
	 * Moves a client-only mob to a spot and lets its legs move along, like it really walks or runs there. Only
	 * setting the position would make it slide, the walking animation follows how far it got this tick.
	 */
	protected static void walk(net.minecraft.world.entity.LivingEntity mob, Vec3 to) {
		double distance = Math.sqrt(Mth.square(to.x - mob.getX()) + Mth.square(to.z - mob.getZ()));
		mob.setPos(to.x, to.y, to.z);
		mob.walkAnimation.update(Math.min((float) distance * 4.0F, 1.0F), 0.4F, mob.isBaby() ? 3.0F : 1.0F);
	}

	/** Makes a client-only mob look at a point, head and body together. */
	protected static void face(Entity entity, Vec3 target) {
		Vec3 delta = target.subtract(entity.getEyePosition());
		float yaw = (float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90.0F;
		float pitch = (float) -(Mth.atan2(delta.y, delta.horizontalDistance()) * Mth.RAD_TO_DEG);

		entity.setYRot(yaw);
		entity.setXRot(pitch);
		entity.setYHeadRot(yaw);

		if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
			living.setYBodyRot(yaw);
		}
	}

	protected static Vec3 horizontalLook(LocalPlayer player) {
		Vec3 look = player.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0.0, look.z);
		return flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
	}

	/** A spot at the given distance and angle from where the player looks, standing on solid ground if one is close. */
	protected static Vec3 groundSpot(ClientLevel level, LocalPlayer player, double distance, double angleDegrees) {
		Vec3 dir = horizontalLook(player).yRot((float) Math.toRadians(angleDegrees));
		Vec3 spot = player.position().add(dir.scale(distance));
		BlockPos base = BlockPos.containing(spot);

		for (int dy : new int[] {0, 1, -1, 2, -2, 3, -3}) {
			BlockPos feet = base.above(dy);

			if (isFree(level, feet) && isFree(level, feet.above()) && !isFree(level, feet.below()) && !onTree(level, feet)) {
				return new Vec3(spot.x, feet.getY(), spot.z);
			}
		}

		return spot;
	}

	/** The floor height near a spot, so walking actors stand on the ground and not in the air or in the hill. */
	protected static double floor(ClientLevel level, double x, double z, double around) {
		BlockPos base = BlockPos.containing(x, around, z);

		for (int dy : new int[] {0, 1, -1, 2, -2, 3, -3}) {
			BlockPos feet = base.above(dy);

			if (isFree(level, feet) && !isFree(level, feet.below()) && !onTree(level, feet)) {
				return feet.getY();
			}
		}

		return around;
	}

	/**
	 * True if a spot is on top of a tree, on its leaves or trunk. Nothing of a scene should stand or grow up there, a
	 * villager on a treetop just looks wrong.
	 */
	protected static boolean onTree(ClientLevel level, BlockPos feet) {
		BlockState below = level.getBlockState(feet.below());
		return below.is(net.minecraft.tags.BlockTags.LEAVES) || below.is(net.minecraft.tags.BlockTags.LOGS);
	}

	private static boolean isFree(ClientLevel level, BlockPos pos) {
		return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}
}
