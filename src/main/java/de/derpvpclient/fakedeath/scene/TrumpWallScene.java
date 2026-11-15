package de.derpvpclient.fakedeath.scene;

import de.derpvpclient.fakedeath.mixin.DisplayAccessor;
import de.derpvpclient.fakedeath.mixin.TextDisplayAccessor;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
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
 * At the border to Mexico. The border line, white posts and a sign "USA | MÉXICO", runs straight away from the player,
 * right through where they stand. Donald Trump and his bodyguards walk up on the American side, the president of
 * Mexico on his. "We need to build a wall, and Mexico must pay for it!" Mexico will not hear of it. Then Trump sees
 * the player: standing right on his border. "Fine. HE pays!" An invoice for twenty-five billion dollars, made out to
 * the player. And the wall goes up along the border, brick by brick, closer and closer, and right around the player,
 * up over the head. "A big, beautiful wall!"
 */
public class TrumpWallScene extends CastScene {
	private static final int SPEECH = ARRIVE + 8;
	private static final int NO = ARRIVE + 58;
	private static final int NO_2 = ARRIVE + 98;
	private static final int ARGUE = ARRIVE + 138;
	private static final int BORDER = ARRIVE + 172;
	private static final int POINT = ARRIVE + 215;
	private static final int BUILD = POINT + 45;
	private static final ChatFormatting US = ChatFormatting.RED;
	private static final ChatFormatting MX = ChatFormatting.DARK_GREEN;

	private final List<BlockPos> line = new ArrayList<>();
	private final List<BlockPos> cell = new ArrayList<>();
	private final List<RemotePlayer> guards = new ArrayList<>();
	private RemotePlayer trump;
	private RemotePlayer mexico;
	private Display.TextDisplay invoice;
	private Vec3 far;
	private int builtLine;
	private int builtCell;
	private int beautiful = Integer.MAX_VALUE;
	private int death = Integer.MAX_VALUE;
	private float dark;

	@Override
	public int length() {
		return BUILD + 140;
	}

	@Override
	public int photoTick() {
		return BUILD + 14;
	}

	@Override
	public int screenTint() {
		return dark > 0.0F ? ((int) (dark * 200.0F) << 24) | 0x100804 : 0;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.trumpmauer", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (trump == null) {
			return;
		}
		Vec3 eyes = player.getEyePosition();
		bubbles();

		if (tick < BORDER) {
			// The two face each other while they argue, the guards watch the player.
			face(trump, tick < SPEECH + 40 ? eyes : mexico.getEyePosition());
			face(mexico, trump.getEyePosition());
			turnTowards(player, trump.getEyePosition().lerp(mexico.getEyePosition(), tick < NO ? 0.25 : 0.5), 0.07F);
		} else {
			face(trump, eyes);
			face(mexico, tick < POINT + 20 ? trump.getEyePosition() : eyes);
			if (tick < BUILD) {
				turnTowards(player, trump.getEyePosition(), 0.1F);
			} else if (builtLine < line.size()) {
				// Watching the wall come closer along the border.
				turnTowards(player, far.add(0.0, 1.0, 0.0), 0.06F);
			} else {
				turnTowards(player, eyes.add(horizontalLook(player).scale(3.0)).add(0.0, builtCell > 24 ? 3.0 : 0.5, 0.0), 0.05F);
			}
		}
		for (RemotePlayer guard : guards) {
			face(guard, eyes);
		}

		if (tick == SPEECH) {
			speak(level, trump, "fakedeath.trumpmauer.wall", US, 60);
		}
		if (tick == NO) {
			speak(level, mexico, "fakedeath.trumpmauer.no", MX, 50);
		}
		if (tick == NO_2) {
			speak(level, mexico, "fakedeath.trumpmauer.no2", MX, 45);
		}
		if (tick == ARGUE) {
			speak(level, trump, "fakedeath.trumpmauer.argue", US, 40);
		}
		if (tick == BORDER) {
			speak(level, trump, "fakedeath.trumpmauer.border", US, 45);
			pose(trump, Skins.Pose.POINT);
		}
		if (tick == POINT) {
			speak(level, trump, "fakedeath.trumpmauer.he", US, 50);
			Looks.swing(trump);
			// The invoice, made out to the player.
			Vec3 at = eyes.add(horizontalLook(player).scale(1.7)).add(0.0, -0.25, 0.0);
			invoice = spawn(level, EntityType.TEXT_DISPLAY, at);
			((DisplayAccessor) invoice).fakedeath$setBillboard(Display.BillboardConstraints.CENTER);
			((TextDisplayAccessor) invoice).fakedeath$setLineWidth(160);
			((TextDisplayAccessor) invoice).fakedeath$setBackgroundColor(0xF2F2EAD2);
			((TextDisplayAccessor) invoice).fakedeath$setText(Component.translatable("fakedeath.trumpmauer.invoice", player.getName()).withStyle(ChatFormatting.BLACK));
			Looks.glow(invoice);
			place(invoice, at, 0.5F);
			sound(level, at, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 1.0F, 2.0F);
			sound(level, at, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 1.0F, 0.8F);
		}
		if (tick == POINT + 30) {
			pose(trump, null);
		}
		if (tick == BUILD + 6 && invoice != null) {
			despawn(level, invoice);
			invoice = null;
		}

		if (tick >= BUILD) {
			if (builtLine < line.size()) {
				// The wall comes along the border towards the player, a column of three at a time.
				for (int i = 0; i < 3 && builtLine < line.size(); i++) {
					lay(level, line.get(builtLine++));
				}
				far = Vec3.atCenterOf(line.get(builtLine - 1));
			} else if (builtCell < cell.size() && tick % 2 == 0) {
				// It reaches the player and goes right around, up over the head.
				lay(level, cell.get(builtCell++));
				dark = thirdPerson() ? 0.0F : Math.min(0.85F, builtCell / (float) cell.size() * 0.9F);
				if (builtCell == 1) {
					beautiful = tick + 10;
				}
				if (builtCell == 10 || builtCell == 20) {
					hurt(level, player, SoundEvents.PLAYER_HURT, 0.3F);
				}
				if (builtCell == cell.size()) {
					death = tick + 14;
				}
			}
		}
		if (tick == beautiful) {
			speak(level, trump, "fakedeath.trumpmauer.beautiful", US, 60);
		}
		if (tick == death) {
			sound(level, player.position(), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.5F, 0.5F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void lay(ClientLevel level, BlockPos brick) {
		BlockState state = Blocks.BRICKS.defaultBlockState();
		if (placeBlock(level, brick, state)) {
			sound(level, Vec3.atCenterOf(brick), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F + random.nextFloat() * 0.3F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, state), Vec3.atCenterOf(brick), 5, 0.4, 0.05);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		Vec3 look = horizontalLook(player);
		Vec3 left = look.yRot(Mth.HALF_PI);
		float yaw = (float) Math.atan2(look.x, look.z);
		Vec3 front = player.position().add(look.scale(6.5));
		// The United States on the left of the border, Mexico on the right.
		Vec3 trumpAt = onGround(level, player, front.add(left.scale(2.2)));
		Vec3 mexicoAt = onGround(level, player, front.add(left.scale(-2.2)));
		trump = actor(level, "fakedeath.trumpmauer.trump", "trump", false, trumpAt);
		mexico = actor(level, "fakedeath.trumpmauer.mexico", "mexico", false, mexicoAt);
		hold(trump, Looks.prop(Items.SPYGLASS, "phone"));
		arrive(level, player, trump, 4);
		arrive(level, player, mexico, 0);
		for (int s : new int[] {-1, 1}) {
			RemotePlayer guard = extra(level, "bodyguard", onGround(level, player, trumpAt.add(look.scale(1.3)).add(left.scale(s * 0.9 + 0.6))));
			arrive(level, player, guard, s < 0 ? 0 : 6);
			guards.add(guard);
		}
		// The flags of both sides.
		standFlag(level, player, trumpAt.add(look.scale(1.2)).add(left.scale(1.6)), "flag_usa", 2.0F);
		standFlag(level, player, mexicoAt.add(look.scale(1.2)).add(left.scale(-1.6)), "flag_mexico", 2.0F);

		// The border: a white line on the ground, posts along it, the sign in the middle.
		Vec3 lineMid = onGround(level, player, player.position().add(look.scale(7.0)));
		place(blockDisplay(level, Looks.WHITE_CONCRETE, lineMid, 16.0F), lineMid.add(0.0, 0.02, 0.0), new Vector3f(0.14F, 0.03F, 12.0F), new Quaternionf().rotateY(yaw));
		for (int d = 3; d <= 13; d += 2) {
			Vec3 post = onGround(level, player, player.position().add(look.scale(d)));
			place(blockDisplay(level, Looks.WHITE_CONCRETE, post, 3.0F), post.add(0.0, 0.55, 0.0), new Vector3f(0.14F, 1.1F, 0.14F), new Quaternionf().rotateY(yaw));
			place(blockDisplay(level, Blocks.RED_CONCRETE.defaultBlockState(), post, 3.0F), post.add(0.0, 1.15, 0.0), new Vector3f(0.16F, 0.12F, 0.16F), new Quaternionf().rotateY(yaw));
		}
		Vec3 signAt = onGround(level, player, player.position().add(look.scale(5.0)));
		place(blockDisplay(level, Blocks.DARK_OAK_FENCE.defaultBlockState(), signAt, 3.0F), signAt.add(0.0, 0.75, 0.0), new Vector3f(0.25F, 1.5F, 0.25F), new Quaternionf());
		Display.TextDisplay sign = spawn(level, EntityType.TEXT_DISPLAY, signAt.add(0.0, 1.6, 0.0));
		((DisplayAccessor) sign).fakedeath$setBillboard(Display.BillboardConstraints.VERTICAL);
		((TextDisplayAccessor) sign).fakedeath$setText(Component.literal("USA  |  MÉXICO").withStyle(ChatFormatting.BOLD));
		((TextDisplayAccessor) sign).fakedeath$setBackgroundColor(0xFF6A4A2A);
		place(sign, signAt.add(0.0, 1.6, 0.0), 1.0F);

		// The wall along the border, from far away towards the player, three bricks high.
		Set<BlockPos> columns = new LinkedHashSet<>();
		for (double d = 14.0; d >= 2.0; d -= 0.5) {
			Vec3 at = onGround(level, player, player.position().add(look.scale(d)));
			columns.add(BlockPos.containing(at.x, at.y + 0.01, at.z));
		}
		for (BlockPos column : columns) {
			for (int y = 0; y < 3; y++) {
				line.add(column.above(y));
			}
		}
		far = Vec3.atCenterOf(columns.iterator().next());
		// Around the player: three rows of eight, then the roof.
		BlockPos feet = player.blockPosition();
		for (int y = 0; y < 3; y++) {
			for (int[] o : new int[][] {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}}) {
				cell.add(feet.offset(o[0], y, o[1]));
			}
		}
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				cell.add(feet.offset(dx, 3, dz));
			}
		}
		sound(level, front, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 1.0F, 1.2F);
	}
}
