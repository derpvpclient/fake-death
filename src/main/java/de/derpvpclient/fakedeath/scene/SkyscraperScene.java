package de.derpvpclient.fakedeath.scene;

import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Stalin on a little stand: "By tonight there will be a skyscraper here!" And it rises: one of his wedding-cake towers,
 * tier on tier out of the ground, rows of windows, a golden spire, and on top a great red star that glows. The
 * workers cheer. The star wobbles in the wind, breaks off, and comes down the whole height of the tower, spinning.
 */
public class SkyscraperScene extends CastScene {
	/** The tiers from the bottom: width, height, depth. */
	private static final float[][] TIERS = {{9.0F, 6.0F, 7.0F}, {7.0F, 6.0F, 5.0F}, {5.0F, 5.0F, 3.6F}, {3.2F, 5.0F, 2.6F}, {1.0F, 7.0F, 1.0F}};
	private static final int ORDER = 12;
	private static final int FIRST = 30;
	private static final int TIER = 26;
	private static final int DONE = FIRST + TIERS.length * TIER;
	private static final int WOBBLE = DONE + 20;
	private static final int BREAK = WOBBLE + 30;
	private static final int FALL = 30;
	private static final int HIT = BREAK + FALL;
	private static final ChatFormatting RED = ChatFormatting.RED;

	private final List<Display.BlockDisplay> tiers = new ArrayList<>();
	private final float[] heights = new float[TIERS.length];
	private final List<Display.BlockDisplay> windows = new ArrayList<>();
	private final List<RemotePlayer> workers = new ArrayList<>();
	private RemotePlayer stalin;
	private Display.ItemDisplay star;
	private Vec3 base;
	private float yaw;
	private float topY;
	private int flash;

	@Override
	public int length() {
		return HIT + 2;
	}

	@Override
	public int photoTick() {
		return DONE + 10;
	}

	@Override
	public int screenTint() {
		return flash > 0 ? (Math.min(200, flash * 40) << 24) | 0xC01010 : 0;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.hochhaus", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (stalin == null) {
			return;
		}
		bubbles();
		if (flash > 0) {
			flash--;
		}
		Vec3 eyes = player.getEyePosition();
		face(stalin, tick < DONE ? base.add(0.0, 8.0, 0.0) : eyes);
		if (tick == ORDER) {
			speak(level, stalin, "fakedeath.hochhaus.order", RED, 50);
			pose(stalin, Skins.Pose.POINT);
		}
		if (tick == ORDER + 40) {
			pose(stalin, null);
		}

		// Tier after tier rising out of the ground.
		float y = 0.0F;
		Quaternionf turn = new Quaternionf().rotateY(yaw);
		for (int i = 0; i < TIERS.length; i++) {
			float rise = Mth.clamp((tick - FIRST - i * TIER) / (float) TIER, 0.0F, 1.0F);
			float[] size = TIERS[i];
			float height = size[1] * rise;
			heights[i] = height;
			Vec3 at = base.add(0.0, y, 0.0);
			tiers.get(i).setPos(at.x, at.y, at.z);
			transform(tiers.get(i), new Transformation(new Vector3f(-size[0] / 2, 0.0F, -size[2] / 2).rotate(turn), new Quaternionf(turn), new Vector3f(size[0], height, size[2]), new Quaternionf()));
			if (rise > 0.0F && rise < 1.0F && tick % 3 == 0) {
				particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SMOOTH_SANDSTONE.defaultBlockState()), at.add(0.0, height, 0.0), 8, size[0] / 2, 0.1);
				sound(level, at.add(0.0, height, 0.0), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 2.0F, 0.6F + random.nextFloat() * 0.3F);
			}
			// Each tier stands on the one below as it is right now, so nothing floats while the lower ones still grow.
			y += height;
		}
		placeWindows();
		topY = y;
		for (RemotePlayer worker : workers) {
			face(worker, base.add(0.0, 4.0, 0.0));
			if (tick < DONE && tick % 10 == workers.indexOf(worker) * 3) {
				Looks.swing(worker);
				sound(level, worker.position(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.3F, 1.6F);
			}
		}
		if (tick == DONE) {
			for (RemotePlayer worker : workers) {
				pose(worker, Skins.Pose.CHEER);
			}
			sound(level, eyes, SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 2.0F, 0.8F);
			speak(level, stalin, "fakedeath.hochhaus.done", RED, 40);
		}
		if (tick >= DONE - 20 && tick < BREAK) {
			// Searchlights sweeping up the tower.
			for (int s : new int[] {-1, 1}) {
				Vec3 lamp = base.add(horizontalLook(player).yRot(Mth.HALF_PI).scale(s * 6.0)).add(horizontalLook(player).scale(-3.0));
				Vec3 sky = base.add(Math.sin(tick * 0.05 + s) * 6.0, topY + 6.0, Math.cos(tick * 0.05 + s) * 6.0);
				line(level, ParticleTypes.END_ROD, lamp, sky, 3.0);
			}
		}

		// The star on top: glowing, wobbling, then falling.
		Vec3 top = base.add(0.0, topY + 2.6, 0.0);
		Vec3 starAt = top;
		float spin = 0.0F;
		if (tick >= FIRST + (TIERS.length - 1) * TIER && tick < BREAK) {
			float wobble = tick >= WOBBLE ? Mth.sin(tick * 0.6F) * 0.15F * (tick - WOBBLE) / (BREAK - WOBBLE) : 0.0F;
			starAt = top.add(wobble, 0.0, 0.0);
			spin = wobble;
			if (tick >= WOBBLE && tick % 8 == 0) {
				sound(level, top, SoundEvents.CHAIN_STEP, SoundSource.BLOCKS, 3.0F, 0.5F);
			}
		}
		if (tick == BREAK) {
			sound(level, top, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 4.0F, 0.5F);
			sound(level, top, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 3.0F, 0.6F);
			for (RemotePlayer worker : workers) {
				pose(worker, null);
			}
		}
		if (tick >= BREAK) {
			float p = Math.min(1.0F, (tick - BREAK) / (float) FALL);
			starAt = top.lerp(eyes.add(0.0, 0.4, 0.0), p * p);
			spin = (tick - BREAK) * 0.35F;
			if (tick % 5 == 0) {
				sound(level, starAt, SoundEvents.ELYTRA_FLYING, SoundSource.BLOCKS, 1.5F, 1.2F);
			}
			level.addParticle(ParticleTypes.FLAME, starAt.x, starAt.y, starAt.z, 0.0, 0.05, 0.0);
		}
		if (tick >= FIRST + (TIERS.length - 1) * TIER) {
			Vec3 toPlayer = player.position().subtract(base);
			float face = (float) Math.atan2(toPlayer.x, toPlayer.z);
			place(star, starAt, new Vector3f(5.0F), new Quaternionf().rotateY(face).rotateZ(spin));
		}
		turnTowards(player, tick < FIRST ? stalin.getEyePosition() : tick < BREAK ? base.add(0.0, Math.min(topY, 8.0 + (tick - FIRST) * 0.15), 0.0) : starAt, tick < BREAK ? 0.06F : 0.3F);
		if (tick == HIT) {
			flash = 6;
			sound(level, eyes, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 2.0F, 0.5F);
			sound(level, eyes, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 2.0F, 0.8F);
			cameraShake(0.4F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		Vec3 look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		base = onGround(level, player, player.position().add(look.scale(11.0)));
		yaw = (float) Math.atan2(look.x, look.z);
		BlockState[] looks = {Blocks.SMOOTH_SANDSTONE.defaultBlockState(), Blocks.SMOOTH_SANDSTONE.defaultBlockState(), Blocks.SMOOTH_SANDSTONE.defaultBlockState(),
			Blocks.SMOOTH_SANDSTONE.defaultBlockState(), Blocks.GOLD_BLOCK.defaultBlockState()};
		for (int i = 0; i < TIERS.length; i++) {
			tiers.add(blockDisplay(level, looks[i], base, 64.0F));
		}
		for (int i = 0; i < 4 * 4; i++) {
			windows.add(blockDisplay(level, Blocks.STAINED_GLASS.black().defaultBlockState(), base, 64.0F));
		}
		star = itemDisplay(level, Looks.prop(Items.IRON_NUGGET, "red_star"), base);
		Looks.glow(star);
		((de.derpvpclient.fakedeath.mixin.DisplayAccessor) star).fakedeath$setViewRange(4.0F);
		// Stalin on his little stand off to the side, the workers between him and the tower.
		Vec3 stand = onGround(level, player, player.position().add(look.scale(4.0)).add(side.scale(-3.0)));
		place(blockDisplay(level, Blocks.CONCRETE.red().defaultBlockState(), stand, 3.0F), stand.add(0.0, 0.3, 0.0), new Vector3f(1.4F, 0.6F, 1.4F), new Quaternionf().rotateY(yaw));
		stalin = actor(level, "fakedeath.hochhaus.stalin", "stalin", false, stand.add(0.0, 0.6, 0.0));
		for (int i = 0; i < 3; i++) {
			workers.add(extra(level, i == 1 ? "builder" : "worker", onGround(level, player, player.position().add(look.scale(6.5)).add(side.scale((i - 1) * 1.8 + 1.5)))));
		}
		standFlag(level, player, stand.add(side.scale(-1.3)).add(look.scale(0.6)), "flag_soviet", 1.3F);
	}

	/** Rows of dark windows on the face towards the player, four on each of the lower four tiers. */
	private void placeWindows() {
		Quaternionf turn = new Quaternionf().rotateY(yaw);
		Vec3 front = new Vec3(Math.sin(yaw), 0.0, Math.cos(yaw)).reverse();
		float y = 0.0F;
		int w = 0;
		for (int i = 0; i < 4; i++) {
			float[] size = TIERS[i];
			for (int row = 0; row < 4; row++) {
				float rowY = y + 0.8F + row * (size[1] - 1.2F) / 4.0F;
				Display.BlockDisplay window = windows.get(w++);
				Vec3 at = base.add(front.scale(size[2] / 2 + 0.02)).add(0.0, rowY, 0.0);
				boolean built = heights[i] > rowY - y + 0.5F;
				window.setPos(at.x, at.y, at.z);
				transform(window, new Transformation(new Vector3f(-size[0] * 0.4F, 0.0F, -0.02F).rotate(turn), new Quaternionf(turn),
					new Vector3f(size[0] * 0.8F, built ? 0.5F : 0.0F, 0.04F), new Quaternionf()));
			}
			y += heights[i];
		}
	}

}
