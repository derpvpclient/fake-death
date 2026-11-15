package de.derpvpclient.fakedeath.scene;

import de.derpvpclient.fakedeath.mixin.DisplayAccessor;
import de.derpvpclient.fakedeath.mixin.TextDisplayAccessor;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The grand opening of a building that is nowhere near finished, like a certain airport: a tall front of concrete and
 * glass in scaffolding, a red ribbon across the way, a sign "Opening planned: 2011". Angela Merkel in a hard hat walks
 * up with the site manager: "Today we finally open this building!" He begs her not to: the fire safety, the
 * structure. She folds her hands into the famous diamond: "Wir schaffen das!" and cuts the ribbon. A creak, dust
 * trickles down, everyone runs, and the whole front tips over onto the player.
 */
public class MerkelScene extends CastScene {
	private static final int OPEN = ARRIVE + 8;
	private static final int NOT_READY = ARRIVE + 66;
	private static final int SCHAFFEN = ARRIVE + 120;
	private static final int CUT = ARRIVE + 165;
	private static final int CREAK = CUT + 14;
	private static final int RUN = CREAK + 10;
	private static final int FALL = CREAK + 36;
	private static final int FALL_TICKS = 18;
	private static final int HIT = FALL + FALL_TICKS;
	/** How far away the bottom of the front stands, and how high it is: high enough to reach past the player. */
	private static final double FRONT = 7.0;
	private static final int HIGH = 8;
	private static final ChatFormatting BUND = ChatFormatting.GOLD;
	private static final ChatFormatting BAU = ChatFormatting.GRAY;

	private final List<Piece> front = new ArrayList<>();
	private RemotePlayer merkel;
	private RemotePlayer manager;
	private Display.BlockDisplay ribbon;
	private Display.BlockDisplay ribbonLeft;
	private Display.BlockDisplay ribbonRight;
	private Vec3 pivot;
	private Vec3 look;
	private Vec3 side;
	private Vec3 postLeft;
	private Vec3 postRight;
	private float yaw;

	@Override
	public int length() {
		return HIT + 2;
	}

	@Override
	public int photoTick() {
		return SCHAFFEN + 12;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.merkel", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (merkel == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();
		Vec3 top = pivot.add(0.0, HIGH * 0.7, 0.0);
		if (tick < CREAK) {
			face(merkel, tick < CUT - 12 ? eyes : postLeft.lerp(postRight, 0.5).add(0.0, 1.0, 0.0));
			face(manager, tick > NOT_READY - 5 && tick < SCHAFFEN ? merkel.getEyePosition() : eyes);
		} else if (tick < RUN) {
			face(merkel, top);
			face(manager, top);
		} else if (tick < HIT) {
			// Everybody runs off to the sides.
			stepTowards(level, player, merkel, merkel.position().add(side.scale(2.0)).add(look.scale(-0.5)), 0.2);
			stepTowards(level, player, manager, manager.position().add(side.scale(-2.0)).add(look.scale(-0.5)), 0.24);
			face(merkel, merkel.position().add(side.scale(2.0)).add(0.0, 1.6, 0.0));
			face(manager, manager.position().add(side.scale(-2.0)).add(0.0, 1.6, 0.0));
		}

		if (tick == OPEN) {
			speak(level, merkel, "fakedeath.merkel.open", BUND, 55);
		}
		if (tick == NOT_READY) {
			speak(level, manager, "fakedeath.merkel.notready", BAU, 50);
			pose(manager, Skins.Pose.POINT);
		}
		if (tick == SCHAFFEN - 8) {
			pose(manager, null);
			pose(merkel, Skins.Pose.RAUTE);
		}
		if (tick == SCHAFFEN) {
			speak(level, merkel, "fakedeath.merkel.schaffen", BUND, 55);
		}
		if (tick == CUT - 12) {
			pose(merkel, null);
			hold(merkel, Looks.prop(Items.SHEARS, "barber_scissors"));
		}
		if (tick == CUT) {
			// Snip. The ribbon falls apart, everybody claps.
			Looks.swing(merkel);
			sound(level, merkel.position(), SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 1.2F, 1.0F);
			despawn(level, ribbon);
			ribbonLeft = blockDisplay(level, Looks.RED_WOOL, postLeft, 4.0F);
			ribbonRight = blockDisplay(level, Looks.RED_WOOL, postRight, 4.0F);
			for (int i = 0; i < 4; i++) {
				sound(level, eyes.add(side.scale(i - 1.5)), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.NEUTRAL, 0.6F, 1.5F + i * 0.1F);
			}
		}
		if (ribbonLeft != null) {
			// The two halves swing down from their posts.
			float drop = Math.min(1.0F, (tick - CUT) / 10.0F) * (Mth.HALF_PI - 0.1F);
			hangRibbon(ribbonLeft, postLeft, side, drop);
			hangRibbon(ribbonRight, postRight, side.scale(-1.0), drop);
		}
		if (tick >= CREAK && tick < FALL && (tick - CREAK) % 7 == 0) {
			// The front creaks and dust trickles down.
			sound(level, top, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.5F, 0.5F);
			sound(level, top, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 0.4F);
			particles(level, new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.GRAVEL.defaultBlockState()), top.add(look.scale(-0.6)), 12, 2.0, 0.0);
		}
		if (tick == RUN) {
			speak(level, manager, "fakedeath.merkel.run", BAU, 30);
		}
		float angle = 0.0F;
		if (tick >= FALL) {
			float p = Math.min(1.0F, (tick - FALL) / (float) FALL_TICKS);
			angle = p * p * Mth.HALF_PI;
			if (tick == FALL) {
				sound(level, top, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 2.0F, 0.5F);
			}
			if (tick % 4 == 0) {
				sound(level, top, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.5F, 0.6F);
			}
		}
		if (tick >= FALL && tick <= HIT) {
			// The whole front tips over its bottom edge, towards the player.
			for (Piece piece : front) {
				double f = piece.depth * Math.cos(angle) - piece.height * Math.sin(angle);
				double up = piece.depth * Math.sin(angle) + piece.height * Math.cos(angle);
				Vec3 at = pivot.add(side.scale(piece.across)).add(look.scale(f)).add(0.0, up, 0.0);
				place(piece.display, at, piece.size, new Quaternionf().rotateY(yaw).rotateX(-angle));
			}
		}

		if (tick < CUT - 12) {
			turnTowards(player, tick < NOT_READY ? merkel.getEyePosition() : tick < SCHAFFEN - 10 ? manager.getEyePosition() : merkel.getEyePosition(), 0.08F);
		} else if (tick < CREAK) {
			turnTowards(player, postLeft.lerp(postRight, 0.5).add(0.0, 1.0, 0.0), 0.1F);
		} else {
			// Up at the front, as it comes down.
			double reach = HIGH * Math.sin(angle);
			Vec3 edge = pivot.add(look.scale(-reach)).add(0.0, HIGH * Math.cos(angle) + 0.5, 0.0);
			turnTowards(player, edge, 0.12F);
			cameraShake(tick >= FALL ? 0.1F + (tick - FALL) * 0.02F : 0.03F);
		}
		if (tick == HIT) {
			sound(level, eyes, Looks.EXPLODE, SoundSource.BLOCKS, 2.0F, 0.6F);
			sound(level, eyes, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 2.0F, 0.5F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Looks.WHITE_CONCRETE), eyes, 50, 1.2, 0.3);
			particles(level, ParticleTypes.CLOUD, eyes, 30, 2.0, 0.1);
			cameraShake(0.5F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void hangRibbon(Display.BlockDisplay half, Vec3 post, Vec3 inward, float drop) {
		// A strip one and a half blocks long, fixed at the post, the loose end swinging down.
		Vec3 end = post.add(inward.scale(Math.cos(drop) * 1.4)).add(0.0, -Math.sin(drop) * 1.4, 0.0);
		Vec3 mid = post.lerp(end, 0.5);
		float turn = (float) Math.atan2(inward.x, inward.z);
		place(half, mid, new Vector3f(0.04F, 0.15F, 1.4F), new Quaternionf().rotateY(turn).rotateX(drop));
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		look = horizontalLook(player);
		side = look.yRot(Mth.HALF_PI);
		yaw = (float) Math.atan2(look.x, look.z);
		Vec3 feet = player.position();
		merkel = actor(level, "fakedeath.merkel.name", "merkel_helmet", false, onGround(level, player, feet.add(look.scale(4.0)).add(side.scale(-0.9))));
		manager = actor(level, "fakedeath.merkel.manager", "builder", false, onGround(level, player, feet.add(look.scale(4.4)).add(side.scale(1.2))));
		arrive(level, player, merkel, 0);
		arrive(level, player, manager, 5);

		// The red ribbon across the way, on two posts.
		postLeft = onGround(level, player, feet.add(look.scale(2.8)).add(side.scale(1.6))).add(0.0, 1.0, 0.0);
		postRight = onGround(level, player, feet.add(look.scale(2.8)).add(side.scale(-1.6))).add(0.0, 1.0, 0.0);
		for (Vec3 post : new Vec3[] {postLeft, postRight}) {
			place(blockDisplay(level, Looks.WHITE_CONCRETE, post, 3.0F), post.add(0.0, -0.45, 0.0), new Vector3f(0.1F, 1.1F, 0.1F), new Quaternionf().rotateY(yaw));
		}
		ribbon = blockDisplay(level, Looks.RED_WOOL, postLeft, 4.0F);
		place(ribbon, postLeft.lerp(postRight, 0.5), new Vector3f(3.2F, 0.15F, 0.04F), new Quaternionf().rotateY(yaw));

		// The sign: "Opening planned: 2011".
		Vec3 board = onGround(level, player, feet.add(look.scale(4.8)).add(side.scale(-3.0)));
		place(blockDisplay(level, Blocks.DARK_OAK_FENCE.defaultBlockState(), board, 3.0F), board.add(0.0, 0.8, 0.0), new Vector3f(0.2F, 1.6F, 0.2F), new Quaternionf());
		Display.TextDisplay sign = spawn(level, EntityType.TEXT_DISPLAY, board.add(0.0, 1.55, 0.0));
		((DisplayAccessor) sign).fakedeath$setBillboard(Display.BillboardConstraints.VERTICAL);
		((TextDisplayAccessor) sign).fakedeath$setText(Component.translatable("fakedeath.merkel.sign").withStyle(ChatFormatting.BOLD));
		((TextDisplayAccessor) sign).fakedeath$setBackgroundColor(0xFF2A4A8A);
		place(sign, board.add(0.0, 1.55, 0.0), 1.0F);
		standFlag(level, player, feet.add(look.scale(5.2)).add(side.scale(3.0)), "flag_germany", 1.2F);

		// The front of the building: concrete and glass, in scaffolding up to the middle.
		pivot = onGround(level, player, feet.add(look.scale(FRONT)));
		for (int across = -3; across <= 3; across++) {
			for (int y = 0; y < HIGH; y++) {
				boolean window = Math.abs(across) == 1 || Math.abs(across) == 2 ? y % 3 != 0 : false;
				BlockState state = window ? Blocks.GLASS.defaultBlockState() : y % 3 == 0 ? Looks.GRAY_CONCRETE : Looks.WHITE_CONCRETE;
				front.add(piece(level, state, across, y + 0.5, 0.5, new Vector3f(1.0F)));
				if (y < 4) {
					front.add(piece(level, Blocks.SCAFFOLDING.defaultBlockState(), across, y + 0.5, -0.5, new Vector3f(1.0F)));
				}
			}
		}
		for (Piece piece : front) {
			Vec3 at = pivot.add(side.scale(piece.across)).add(look.scale(piece.depth)).add(0.0, piece.height, 0.0);
			place(piece.display, at, piece.size, new Quaternionf().rotateY(yaw));
		}
		// A crane in the background.
		BlockState yellow = Blocks.YELLOW_CONCRETE.defaultBlockState();
		Vec3 tower = onGround(level, player, feet.add(look.scale(11.0)).add(side.scale(6.0)));
		place(blockDisplay(level, yellow, tower, 16.0F), tower.add(0.0, 6.0, 0.0), new Vector3f(0.7F, 12.0F, 0.7F), new Quaternionf().rotateY(yaw));
		place(blockDisplay(level, yellow, tower, 24.0F), tower.add(side.scale(-3.5)).add(0.0, 12.2, 0.0), new Vector3f(9.0F, 0.5F, 0.5F), new Quaternionf().rotateY(yaw));
	}

	private Piece piece(ClientLevel level, BlockState state, double across, double height, double depth, Vector3f size) {
		return new Piece(blockDisplay(level, state, pivot, 12.0F), across, height, depth, size);
	}

	private record Piece(Display.BlockDisplay display, double across, double height, double depth, Vector3f size) {
	}
}
