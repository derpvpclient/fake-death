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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The United Nations, 1960. Nikita Khrushchev walks up to the desk with the sign "USSR". "Whether you like it or not,
 * history is on our side." He takes off his shoe and bangs it on the desk, three times. Then he points at the player:
 * "We will bury you!" Two workers with shovels come running, and they do, shovel by shovel, until it is dark.
 */
public class KhrushchevScene extends CastScene {
	private static final int HISTORY = ARRIVE + 10;
	private static final int SHOE = ARRIVE + 62;
	private static final int[] BANGS = {SHOE + 14, SHOE + 26, SHOE + 38};
	private static final int BURY = SHOE + 55;
	private static final int DIG = BURY + 40;
	private static final int SHOVEL_EVERY = 4;
	private static final ChatFormatting RED = ChatFormatting.RED;

	private final List<RemotePlayer> diggers = new ArrayList<>();
	private final List<BlockPos> dirt = new ArrayList<>();
	private RemotePlayer khrushchev;
	private Vec3 desk;
	private int filled;
	private int death = Integer.MAX_VALUE;
	private float dark;

	@Override
	public int length() {
		return DIG + 34 * SHOVEL_EVERY + 30;
	}

	@Override
	public int photoTick() {
		return BANGS[1];
	}

	@Override
	public int screenTint() {
		return dark > 0.0F ? ((int) (Math.min(dark, 1.0F) * 235.0F) << 24) | 0x2A1C10 : 0;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.chruschtschow", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (khrushchev == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();
		face(khrushchev, eyes);

		if (tick == HISTORY) {
			speak(level, khrushchev, "fakedeath.chruschtschow.history", RED, 55);
		}
		if (tick == SHOE) {
			// Off with the shoe.
			hold(khrushchev, new ItemStack(Items.LEATHER_BOOTS));
			sound(level, khrushchev.position(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.NEUTRAL, 1.2F, 0.8F);
		}
		for (int bang : BANGS) {
			if (tick == bang) {
				// Bang on the desk with it.
				Looks.swing(khrushchev);
				sound(level, desk, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.NEUTRAL, 1.6F, 0.6F);
				sound(level, desk, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.NEUTRAL, 0.8F, 1.2F);
				cameraShake(0.12F);
			}
		}
		if (tick == BURY) {
			hold(khrushchev, ItemStack.EMPTY);
			pose(khrushchev, Skins.Pose.POINT);
			speak(level, khrushchev, "fakedeath.chruschtschow.bury", RED, 60);
			sound(level, eyes, SoundEvents.RAID_HORN.value(), SoundSource.NEUTRAL, 0.6F, 0.7F);
		}
		for (int i = 0; i < diggers.size(); i++) {
			RemotePlayer digger = diggers.get(i);
			if (tick < BURY + 10) {
				face(digger, eyes);
			} else {
				// Running over with their shovels, then digging away.
				Vec3 spot = player.position().add(horizontalLook(player).yRot(i == 0 ? 1.1F : -1.1F).scale(1.9));
				stepTowards(level, player, digger, spot, 0.26);
				face(digger, player.position());
				if (tick >= DIG && (tick - DIG + i * 2) % SHOVEL_EVERY == 0) {
					Looks.swing(digger);
				}
			}
		}

		if (tick >= DIG && (tick - DIG) % SHOVEL_EVERY == 0 && filled < dirt.size()) {
			BlockPos spot = dirt.get(filled++);
			BlockState state = filled > dirt.size() - 9 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.DIRT.defaultBlockState();
			if (placeBlock(level, spot, state)) {
				sound(level, Vec3.atCenterOf(spot), SoundEvents.GRAVEL_PLACE, SoundSource.BLOCKS, 1.2F, 0.8F + random.nextFloat() * 0.3F);
				particles(level, new BlockParticleOption(ParticleTypes.BLOCK, state), Vec3.atCenterOf(spot).add(0.0, 0.6, 0.0), 10, 0.4, 0.1);
			}
			// Dirt over the head: a little darker with every shovel, as the last ones close the top.
			dark = thirdPerson() ? 0.0F : Math.min(0.9F, filled / (float) dirt.size());
			if (filled == 12 || filled == 24) {
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.3F);
			}
			if (filled == dirt.size()) {
				death = tick + 12;
			}
		}
		if (tick < BURY + 12) {
			turnTowards(player, khrushchev.getEyePosition(), 0.08F);
		} else if (tick < DIG) {
			turnTowards(player, diggers.getFirst().getEyePosition(), 0.1F);
		} else {
			turnTowards(player, eyes.add(horizontalLook(player).scale(3.0)).add(0.0, filled > 24 ? 3.0 : -0.4, 0.0), 0.06F);
		}
		if (tick == death) {
			sound(level, eyes, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 1.5F, 0.5F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		Vec3 look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		float yaw = (float) Math.atan2(look.x, look.z);
		Quaternionf turn = new Quaternionf().rotateY(yaw);
		desk = onGround(level, player, player.position().add(look.scale(4.0)));
		// The desk of the delegation with its sign, the blue wall of the assembly behind.
		place(blockDisplay(level, Blocks.BIRCH_PLANKS.defaultBlockState(), desk, 4.0F), desk.add(0.0, 0.9, 0.0), new Vector3f(2.4F, 0.1F, 0.9F), turn);
		place(blockDisplay(level, Blocks.BIRCH_PLANKS.defaultBlockState(), desk, 4.0F), desk.add(look.scale(-0.4)).add(0.0, 0.45, 0.0), new Vector3f(2.4F, 0.9F, 0.08F), turn);
		Vec3 wall = desk.add(look.scale(3.0));
		place(blockDisplay(level, Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(), wall, 12.0F), wall.add(0.0, 2.2, 0.0), new Vector3f(8.0F, 4.4F, 0.3F), turn);
		text(level, wall.add(look.scale(-0.2)).add(0.0, 3.2, 0.0), Component.literal("UNITED NATIONS").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), 1.6F, yaw);
		text(level, desk.add(look.scale(-0.5)).add(0.0, 0.62, 0.0), Component.literal("USSR").withStyle(ChatFormatting.BLACK, ChatFormatting.BOLD), 0.9F, yaw);
		standFlag(level, player, desk.add(side.scale(1.9)).add(look.scale(0.6)), "flag_soviet", 1.0F);

		khrushchev = actor(level, "fakedeath.chruschtschow.name", "khrushchev", false, onGround(level, player, desk.add(look.scale(0.8))));
		arrive(level, player, khrushchev, 0);
		for (int s : new int[] {-1, 1}) {
			RemotePlayer digger = extra(level, "worker", onGround(level, player, desk.add(side.scale(s * 3.2)).add(look.scale(-0.4))));
			hold(digger, new ItemStack(Items.IRON_SHOVEL));
			arrive(level, player, digger, s < 0 ? 6 : 10);
			diggers.add(digger);
		}

		// The earth around the player, three rings from the feet up, then the top.
		BlockPos feet = player.blockPosition();
		for (int y = 0; y < 3; y++) {
			for (int[] o : new int[][] {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}}) {
				dirt.add(feet.offset(o[0], y, o[1]));
			}
		}
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				dirt.add(feet.offset(dx, 3, dz));
			}
		}
	}

	private void text(ClientLevel level, Vec3 at, Component content, float size, float yaw) {
		Display.TextDisplay text = spawn(level, EntityType.TEXT_DISPLAY, at);
		((TextDisplayAccessor) text).fakedeath$setText(content);
		((TextDisplayAccessor) text).fakedeath$setBackgroundColor(0);
		((DisplayAccessor) text).fakedeath$setInterpolationDuration(1);
		Looks.glow(text);
		place(text, at, new Vector3f(size), new Quaternionf().rotateY(yaw + Mth.PI));
	}
}
