package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The eyes of ender leave the frame of the End portal one by one, hover, and stare at the player. Then they circle
 * closer and closer, peck, and at the end all of them strike at once.
 */
public class EndPortalScene extends Scene {
	private static final int MAX_EYES = 12;
	private static final int CIRCLE = 60;
	private static final int STRIKE = 150;
	private static final int[] PECKS = {CIRCLE + 35, CIRCLE + 58, CIRCLE + 76};
	private final BlockPos found;
	private final List<Vec3> homes = new ArrayList<>();
	private final List<BlockPos> frames = new ArrayList<>();
	private final List<Display.ItemDisplay> eyes = new ArrayList<>();
	private Vec3 center;

	public EndPortalScene(BlockPos portal) {
		this.found = portal;
	}

	@Override
	public int length() {
		return STRIKE + 25;
	}

	@Override
	public int photoTick() {
		return CIRCLE + 50;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.endportal", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			prepare(level);
		}

		// One eye after the other pops out of its frame.
		if (tick >= 8 && tick % 3 == 2 && eyes.size() < homes.size()) {
			int i = eyes.size();
			Vec3 home = homes.get(i);
			BlockPos frame = frames.get(i);

			if (frame != null) {
				BlockState state = level.getBlockState(frame);
				if (state.is(Blocks.END_PORTAL_FRAME) && state.getValue(EndPortalFrameBlock.HAS_EYE)) {
					replaceBlock(level, frame, state.setValue(EndPortalFrameBlock.HAS_EYE, false));
				}
			}

			eyes.add(itemDisplay(level, new ItemStack(Items.ENDER_EYE), home));
			sound(level, home, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 0.7F + i * 0.06F);
			particles(level, ParticleTypes.PORTAL, home, 12, 0.2, 0.6);
		}

		Vec3 head = player.getEyePosition();
		turnTowards(player, center.add(0.0, 1.6, 0.0), 0.12F);

		for (int i = 0; i < eyes.size(); i++) {
			Vec3 hover = homes.get(i).add(0.0, 1.3 + Math.sin((tick + i * 7) * 0.15) * 0.12, 0.0);
			Vec3 at = hover;

			if (tick >= CIRCLE) {
				float p = Math.min(1.0F, (tick - CIRCLE) / (float) (STRIKE - CIRCLE));
				double radius = tick >= STRIKE ? 1.1 : 3.4 - 2.4 * p;
				double angle = i * (Math.PI * 2.0 / homes.size()) + (tick - CIRCLE) * (0.06 + p * 0.1);
				Vec3 ring = orbit(head, radius, angle, Math.sin((tick + i * 5) * 0.2) * 0.35 - 0.1);
				// Smoothly from the place above the frame into the ring.
				float in = Math.min(1.0F, (tick - CIRCLE) / 20.0F);
				at = hover.lerp(ring, in * in * (3.0F - 2.0F * in));

				if (tick == STRIKE) {
					at = orbit(head, 0.35, angle, 0.0);
				}
			}

			// Always the flat front towards the player: they stare.
			float yaw = (float) Math.atan2(head.x - at.x, head.z - at.z);
			place(eyes.get(i), at, new Vector3f(0.55F), new Quaternionf().rotateY(yaw));

			if (tick % 4 == i % 4) {
				level.addParticle(ParticleTypes.PORTAL, at.x, at.y, at.z, (random.nextDouble() - 0.5) * 0.4, -0.3, (random.nextDouble() - 0.5) * 0.4);
			}
		}

		if (tick == CIRCLE) {
			sound(level, center, SoundEvents.ENDER_EYE_LAUNCH, SoundSource.HOSTILE, 1.5F, 0.6F);
		}
		for (int peck : PECKS) {
			if (tick == peck) {
				sound(level, head, SoundEvents.ENDER_EYE_DEATH, SoundSource.HOSTILE, 1.2F, 1.2F);
				particles(level, ParticleTypes.PORTAL, head, 20, 0.4, 0.8);
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.22F);
			}
		}
		if (tick == STRIKE) {
			sound(level, head, SoundEvents.ENDER_EYE_DEATH, SoundSource.HOSTILE, 2.0F, 0.7F);
			particles(level, ParticleTypes.REVERSE_PORTAL, head, 60, 0.5, 0.4);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	/** Every frame around gives one eye. Where there are too few, as at the portal in the End, the rest comes out of the portal itself. */
	private void prepare(ClientLevel level) {
		for (BlockPos pos : BlockPos.betweenClosed(found.offset(-5, -2, -5), found.offset(5, 2, 5))) {
			if (homes.size() < MAX_EYES && level.getBlockState(pos).is(Blocks.END_PORTAL_FRAME)) {
				frames.add(pos.immutable());
				homes.add(Vec3.atBottomCenterOf(pos).add(0.0, 0.95, 0.0));
			}
		}

		Vec3 sum = Vec3.ZERO;
		for (Vec3 home : homes) {
			sum = sum.add(home);
		}
		center = homes.isEmpty() ? Vec3.atCenterOf(found) : sum.scale(1.0 / homes.size());

		for (int i = homes.size(); i < 8; i++) {
			frames.add(null);
			homes.add(orbit(center, 1.2, i * Math.PI / 4.0, 0.2));
		}
	}
}
