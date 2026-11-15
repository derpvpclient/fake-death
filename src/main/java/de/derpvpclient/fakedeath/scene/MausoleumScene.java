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
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The mausoleum on Red Square: a hall of dark red granite with a black band and "ЛЕНИН" over it, and in a lit glass
 * coffin lies Lenin, eyes closed, as he has for a hundred years. The guard of honour marches in and salutes. Total
 * silence. A breath. His eyes open. The glass cracks, bursts, he gets up, climbs out of the coffin and comes for the
 * player: "Who disturbs my rest?" The guards back away.
 */
public class MausoleumScene extends CastScene {
	private static final int BREATH = ARRIVE + 35;
	private static final int OPEN = ARRIVE + 52;
	private static final int CRACK = ARRIVE + 72;
	private static final int SIT_UP = ARRIVE + 98;
	private static final int SPEAK = SIT_UP + 12;
	private static final int REACH = SPEAK + 40;
	private static final int GRAB = REACH + 18;
	private static final int DEATH = GRAB + 12;
	private static final ChatFormatting RED = ChatFormatting.DARK_RED;

	private final List<Display.BlockDisplay> glass = new ArrayList<>();
	private final List<RemotePlayer> guards = new ArrayList<>();
	private RemotePlayer lenin;
	private BlockPos bed;
	private Vec3 coffin;
	private Vec3 lying;

	@Override
	public int length() {
		return DEATH + 1;
	}

	@Override
	public int photoTick() {
		return SPEAK + 10;
	}

	@Override
	public int screenTint() {
		// Dim red light, as in the hall of the mausoleum.
		return 0x58300000;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.mausoleum", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (lenin == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();
		Vec3 look = horizontalLook(player);

		if (tick < SIT_UP) {
			// Lying in state, on his back, hands still.
			lenin.setPos(lying.x, lying.y, lying.z);
			lenin.setPose(Pose.SLEEPING);
		}
		for (int i = 0; i < guards.size(); i++) {
			RemotePlayer guard = guards.get(i);
			if (tick == ARRIVE) {
				pose(guard, Skins.Pose.SALUTE);
				sound(level, guard.position(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 0.8F, 0.8F);
			}
			if (tick < CRACK) {
				// Standing guard at the coffin, looking straight across the hall.
				face(guard, guard.getEyePosition().add(look.scale(-6.0)));
			} else if (tick < SIT_UP) {
				face(guard, lenin.getEyePosition());
			} else {
				// Backing away from what climbs out of the coffin.
				Vec3 away = guard.position().subtract(coffin).multiply(1.0, 0.0, 1.0).normalize();
				if (tick < REACH + 10) {
					stepTowards(level, player, guard, guard.position().add(away.scale(0.5)), 0.08);
				}
				face(guard, lenin.getEyePosition());
			}
		}
		if (tick == ARRIVE + 8) {
			sound(level, coffin, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.5F);
		}
		if (tick == BREATH) {
			sound(level, coffin, SoundEvents.PLAYER_BREATH, SoundSource.HOSTILE, 1.2F, 0.6F);
		}
		if (tick == OPEN) {
			// The eyes open.
			Skins.give(lenin.getId(), "lenin", false, false);
			sound(level, coffin, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.4F, 0.6F);
		}
		if (tick == OPEN + 12) {
			sound(level, coffin, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.4F, 0.6F);
		}
		if (tick == CRACK || tick == CRACK + 12) {
			sound(level, coffin, SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 1.5F, 0.6F);
			level.destroyBlockProgress(-7_350_000, bed, tick == CRACK ? 4 : 8);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GLASS.defaultBlockState()), coffin.add(0.0, 1.1, 0.0), 8, 0.4, 0.05);
			for (RemotePlayer guard : guards) {
				pose(guard, null);
			}
		}
		if (tick == SIT_UP) {
			// He gets up, the glass bursts.
			level.destroyBlockProgress(-7_350_000, bed, -1);
			lenin.stopSleeping();
			lenin.setPose(Pose.STANDING);
			for (Display.BlockDisplay pane : glass) {
				despawn(level, pane);
			}
			glass.clear();
			sound(level, coffin, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 2.0F, 0.6F);
			sound(level, coffin, SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 1.5F, 0.5F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GLASS.defaultBlockState()), coffin.add(0.0, 0.9, 0.0), 50, 0.9, 0.35);
		}
		if (tick >= SIT_UP) {
			// Standing on the coffin, then down and step by step towards the player.
			if (tick < REACH) {
				Vec3 top = Vec3.atBottomCenterOf(bed).add(0.0, 0.5625, 0.0);
				lenin.setPos(top.x, top.y, top.z);
			} else {
				stepTowards(level, player, lenin, onGround(level, player, player.position().add(look.scale(0.9))), 0.14);
			}
			face(lenin, eyes);
		}
		if (tick == SPEAK) {
			showName(lenin, true);
			speak(level, lenin, "fakedeath.mausoleum.who", RED, 45);
		}
		if (tick == REACH) {
			pose(lenin, Skins.Pose.POINT);
			sound(level, lenin.position(), SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 1.5F, 0.4F);
		}
		turnTowards(player, tick < ARRIVE ? coffin.add(look.scale(-1.0)).add(0.0, 1.2, 0.0) : tick < SIT_UP ? coffin.add(0.0, 0.7, 0.0) : lenin.getEyePosition(), 0.08F);
		if (tick >= REACH) {
			cameraShake(0.03F + (tick - REACH) * 0.004F);
		}
		if (tick == GRAB) {
			sound(level, eyes, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 1.5F, 0.6F);
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.5F);
		}
		if (tick == DEATH) {
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		Vec3 look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		Direction across = Direction.getNearest(side.x, 0.0, side.z);
		float yaw = (float) Math.atan2(look.x, look.z);
		// A red bed across the view three steps ahead: the velvet he lies on.
		BlockPos head = BlockPos.containing(player.position().add(look.scale(3.2)).add(side.scale(0.5)));
		BlockPos foot = head.relative(across.getOpposite());
		BlockState bedHead = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, across).setValue(BedBlock.PART, BedPart.HEAD);
		BlockState bedFoot = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, across).setValue(BedBlock.PART, BedPart.FOOT);
		if (!placeBlock(level, head, bedHead) || !placeBlock(level, foot, bedFoot)) {
			head = player.blockPosition().relative(Direction.getNearest(look.x, 0.0, look.z), 3);
			foot = head.relative(across.getOpposite());
		}
		bed = head;
		coffin = Vec3.atBottomCenterOf(head).add(Vec3.atBottomCenterOf(foot)).scale(0.5);
		// The floor of the hall at the height of the player's feet, not on top of the bed.
		Vec3 ground = new Vec3(coffin.x, Vec3.atBottomCenterOf(head).y, coffin.z);

		// The hall: dark red granite, a black band, black floor, the name over the coffin.
		Quaternionf turn = new Quaternionf().rotateY(yaw);
		BlockState granite = Blocks.RED_NETHER_BRICKS.defaultBlockState();
		BlockState black = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
		place(blockDisplay(level, granite, ground, 12.0F), ground.add(look.scale(2.8)).add(0.0, 2.25, 0.0), new Vector3f(7.6F, 4.5F, 0.5F), turn);
		place(blockDisplay(level, black, ground, 12.0F), ground.add(look.scale(2.5)).add(0.0, 3.6, 0.0), new Vector3f(7.6F, 0.8F, 0.12F), turn);
		for (int s : new int[] {-1, 1}) {
			place(blockDisplay(level, granite, ground, 12.0F), ground.add(side.scale(s * 3.8)).add(look.scale(0.9)).add(0.0, 2.25, 0.0), new Vector3f(0.5F, 4.5F, 4.2F), turn);
			// A column of black stone at each corner of the coffin.
			place(blockDisplay(level, black, ground, 12.0F), ground.add(side.scale(s * 1.9)).add(look.scale(1.6)).add(0.0, 1.6, 0.0), new Vector3f(0.45F, 3.2F, 0.45F), turn);
		}
		place(blockDisplay(level, black, ground, 12.0F), ground.add(look.scale(0.6)).add(0.0, 0.01, 0.0), new Vector3f(7.2F, 0.02F, 4.8F), turn);
		Display.TextDisplay name = spawn(level, EntityType.TEXT_DISPLAY, ground.add(look.scale(2.5)).add(0.0, 3.6, 0.0));
		((TextDisplayAccessor) name).fakedeath$setText(Component.literal("ЛЕНИН").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
		((TextDisplayAccessor) name).fakedeath$setBackgroundColor(0);
		((DisplayAccessor) name).fakedeath$setInterpolationDuration(1);
		Looks.glow(name);
		place(name, ground.add(look.scale(2.4)).add(0.0, 3.24, 0.0), new Vector3f(2.6F), new Quaternionf().rotateY(yaw + Mth.PI));
		standFlag(level, player, ground.add(look.scale(2.3)).add(side.scale(-2.9)), "flag_soviet", 1.3F);
		standFlag(level, player, ground.add(look.scale(2.3)).add(side.scale(2.9)), "flag_soviet", 1.3F);

		// Lenin on his back, eyes closed, as the sleeping pose lies him into the bed.
		lying = Vec3.atBottomCenterOf(head).add(0.0, 0.6875, 0.0);
		lenin = actor(level, "fakedeath.mausoleum.lenin", "lenin_sleep", false, lying);
		lenin.setSleepingPos(head);
		lenin.setPose(Pose.SLEEPING);
		showName(lenin, false);
		// The glass coffin over him, lit from inside, the pedestal of black stone below.
		Display.BlockDisplay lid = blockDisplay(level, Blocks.GLASS.defaultBlockState(), coffin, 6.0F);
		place(lid, coffin.add(0.0, 0.85, 0.0), new Vector3f(1.3F, 0.7F, 2.3F), new Quaternionf().rotateY(yaw + Mth.HALF_PI));
		Looks.glow(lid);
		glass.add(lid);
		Display.BlockDisplay pedestal = blockDisplay(level, black, coffin, 6.0F);
		place(pedestal, coffin.add(0.0, -0.05, 0.0), new Vector3f(1.6F, 0.1F, 2.6F), new Quaternionf().rotateY(yaw + Mth.HALF_PI));

		// The guard of honour marches in past the player and takes its places at both ends of the coffin.
		for (int s : new int[] {-1, 1}) {
			Vec3 mark = onGround(level, player, coffin.add(side.scale(s * 1.9)).add(look.scale(0.2)));
			RemotePlayer guard = extra(level, "general", mark);
			arrive(level, player, guard, s < 0 ? 0 : 3, player.position().add(look.scale(-6.0)).add(side.scale(s * 1.9)));
			guards.add(guard);
		}
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		if (bed != null) {
			level.destroyBlockProgress(-7_350_000, bed, -1);
		}
		super.end(level, player);
	}
}
