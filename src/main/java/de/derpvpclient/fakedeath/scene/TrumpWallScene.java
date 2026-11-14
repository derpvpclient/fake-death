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
 * At the border to Mexico. Donald Trump with his bodyguards under the American flag, the president of Mexico under
 * his. "We need to build a wall, and Mexico must pay for it!" Mexico will not hear of it, they argue. Then Trump
 * points at the player: "Fine. HE pays!" A wall grows around the player brick by brick, up over the head, and closes.
 */
public class TrumpWallScene extends CastScene {
	private static final int SPEECH = 15;
	private static final int NO = 70;
	private static final int NO_2 = 115;
	private static final int ARGUE = 155;
	private static final int POINT = 190;
	private static final int BUILD = 210;
	private static final int BRICK_EVERY = 2;
	private static final int BEAUTIFUL = 250;
	private static final ChatFormatting US = ChatFormatting.RED;
	private static final ChatFormatting MX = ChatFormatting.DARK_GREEN;

	private final List<BlockPos> wall = new ArrayList<>();
	private RemotePlayer trump;
	private RemotePlayer mexico;
	private final List<RemotePlayer> guards = new ArrayList<>();
	private int built;
	private int death = Integer.MAX_VALUE;
	private float dark;

	@Override
	public int length() {
		return BUILD + 60 * BRICK_EVERY;
	}

	@Override
	public int photoTick() {
		return BUILD + 12 * BRICK_EVERY;
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

		if (tick < POINT) {
			// The two face each other while they argue, the guards watch the player.
			face(trump, tick < SPEECH + 40 ? eyes : mexico.getEyePosition());
			face(mexico, trump.getEyePosition());
			turnTowards(player, trump.getEyePosition().lerp(mexico.getEyePosition(), tick < NO ? 0.2 : 0.5), 0.08F);
		} else {
			face(trump, eyes);
			face(mexico, eyes);
			turnTowards(player, trump.getEyePosition(), 0.1F);
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
		if (tick == POINT) {
			speak(level, trump, "fakedeath.trumpmauer.he", US, 50);
			Looks.swing(trump);
		}
		if (tick == BEAUTIFUL) {
			speak(level, trump, "fakedeath.trumpmauer.beautiful", US, 60);
		}

		if (tick >= BUILD && (tick - BUILD) % BRICK_EVERY == 0 && built < wall.size()) {
			BlockPos brick = wall.get(built++);
			BlockState state = Blocks.BRICKS.defaultBlockState();
			if (placeBlock(level, brick, state)) {
				sound(level, Vec3.atCenterOf(brick), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F + random.nextFloat() * 0.3F);
				particles(level, new BlockParticleOption(ParticleTypes.BLOCK, state), Vec3.atCenterOf(brick), 6, 0.4, 0.05);
			}
			// Darker and darker inside, the last bricks close the roof.
			dark = thirdPerson() ? 0.0F : Math.min(0.85F, built / (float) wall.size() * 0.9F);
			if (built == 10 || built == 18) {
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.3F);
			}
			if (built == wall.size()) {
				death = tick + 12;
			}
		}
		if (tick == death) {
			sound(level, player.position(), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.5F, 0.5F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		Vec3 look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		Vec3 front = player.position().add(look.scale(6.0));
		Vec3 trumpAt = onGround(level, player, front.add(side.scale(-1.6)));
		Vec3 mexicoAt = onGround(level, player, front.add(side.scale(1.8)));
		trump = actor(level, "fakedeath.trumpmauer.trump", "trump", false, trumpAt);
		mexico = actor(level, "fakedeath.trumpmauer.mexico", "mexico", false, mexicoAt);
		for (int s : new int[] {-1, 1}) {
			guards.add(extra(level, "bodyguard", onGround(level, player, trumpAt.add(look.scale(1.4)).add(side.scale(s * 0.9)))));
		}
		// The flags of both sides, and the border sign between them.
		standFlag(level, player, trumpAt.add(look.scale(0.9)).add(side.scale(-1.0)), "flag_usa", 2.0F);
		standFlag(level, player, mexicoAt.add(look.scale(0.9)).add(side.scale(1.0)), "flag_mexico", 2.0F);
		Vec3 signAt = onGround(level, player, front.add(look.scale(0.6)));
		place(blockDisplay(level, Blocks.DARK_OAK_FENCE.defaultBlockState(), signAt, 3.0F), signAt.add(0.0, 0.75, 0.0), new Vector3f(0.25F, 1.5F, 0.25F), new Quaternionf());
		Display.TextDisplay sign = spawn(level, EntityType.TEXT_DISPLAY, signAt.add(0.0, 1.6, 0.0));
		((DisplayAccessor) sign).fakedeath$setBillboard(Display.BillboardConstraints.VERTICAL);
		((TextDisplayAccessor) sign).fakedeath$setText(Component.literal("USA  |  MÉXICO").withStyle(ChatFormatting.BOLD));
		((TextDisplayAccessor) sign).fakedeath$setBackgroundColor(0xFF6A4A2A);
		place(sign, signAt.add(0.0, 1.6, 0.0), 1.0F);
		hold(trump, Looks.prop(Items.SPYGLASS, "phone"));

		// The wall around the player: three rows of eight, then the roof.
		BlockPos feet = player.blockPosition();
		for (int y = 0; y < 3; y++) {
			for (int[] o : new int[][] {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}}) {
				wall.add(feet.offset(o[0], y, o[1]));
			}
		}
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				wall.add(feet.offset(dx, 2, dz).above());
			}
		}
		sound(level, front, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 1.0F, 1.2F);
	}

}
