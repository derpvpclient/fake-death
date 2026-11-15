package de.derpvpclient.fakedeath.scene;

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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * War drums from the steppe. On the horizon a line of riders appears: the Mongol horde, Genghis Khan in front under
 * his standard of white horse tails. They ride closer and stop. He raises his sword: "Khurai!" A rain of arrows, and
 * then the whole horde gallops straight over the player.
 */
public class GenghisScene extends CastScene {
	private static final int RIDERS = 7;
	private static final double START = 38.0;
	private static final double HALT = 13.0;
	private static final double PACE = 0.24;
	private static final int STOP = (int) ((START - HALT) / PACE);
	private static final int SHOUT = STOP + 12;
	private static final int ARROWS = SHOUT + 18;
	private static final int ARROW_FLIGHT = 22;
	private static final int CHARGE = ARROWS + ARROW_FLIGHT + 6;
	private static final double CHARGE_PACE = 0.55;
	private static final ChatFormatting KHAN = ChatFormatting.GOLD;

	private final List<RemotePlayer> riders = new ArrayList<>();
	private final List<Horse> horses = new ArrayList<>();
	private final List<Vec3> lanes = new ArrayList<>();
	private final List<Display.ItemDisplay> arrows = new ArrayList<>();
	private final List<Vec3> arrowFrom = new ArrayList<>();
	private final List<Vec3> arrowTo = new ArrayList<>();
	private Display.BlockDisplay pole;
	private Display.BlockDisplay tails;
	private Vec3 look;
	private int death = Integer.MAX_VALUE;

	@Override
	public int length() {
		return CHARGE + (int) (HALT / CHARGE_PACE) + 20;
	}

	@Override
	public int photoTick() {
		return SHOUT + 4;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.genghis", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (riders.isEmpty()) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();

		// The war drums, faster as they come closer.
		int beat = tick < STOP ? 12 : tick < CHARGE ? 8 : 5;
		if (tick % beat == 0 && tick < death) {
			sound(level, eyes, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.RECORDS, 1.2F, 0.5F);
			if (tick % (beat * 2) == 0) {
				sound(level, eyes, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.RECORDS, 0.5F, 0.6F);
			}
		}
		if (tick == 2) {
			sound(level, eyes.add(look.scale(10.0)), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.2F, 0.7F);
		}

		double distance;
		if (tick < STOP) {
			distance = START - tick * PACE;
		} else if (tick < CHARGE) {
			distance = HALT;
		} else {
			distance = HALT - (tick - CHARGE) * CHARGE_PACE;
		}
		boolean moving = tick < STOP || tick >= CHARGE;
		for (int i = 0; i < riders.size(); i++) {
			Horse horse = horses.get(i);
			RemotePlayer rider = riders.get(i);
			// The Khan rides a little ahead of the line.
			double ahead = i == 0 ? 1.5 : 0.0;
			Vec3 at = onGround(level, player, player.position().add(lanes.get(i)).add(look.scale(distance - ahead)));
			if (moving) {
				walk(horse, at);
				if ((tick + i * 3) % 7 == 0) {
					sound(level, at, SoundEvents.HORSE_GALLOP, SoundSource.NEUTRAL, 1.4F, 0.9F);
				}
			}
			face(horse, at.add(look.scale(-4.0)).add(0.0, 1.0, 0.0));
			horse.positionRider(rider);
			face(rider, tick < CHARGE ? eyes : at.add(look.scale(-4.0)).add(0.0, 1.8, 0.0));
		}
		RemotePlayer khan = riders.getFirst();
		// The standard of white horse tails, carried by the rider next to him.
		Vec3 bearer = riders.get(1).position();
		place(pole, bearer.add(0.0, 2.6, 0.0), new Vector3f(0.12F, 3.2F, 0.12F), new Quaternionf());
		place(tails, bearer.add(0.0, 3.9, 0.0), new Vector3f(0.55F, 0.9F, 0.55F), new Quaternionf().rotateY(tick * 0.05F));

		if (tick == STOP) {
			for (Horse horse : horses) {
				horse.setStanding(20);
			}
			sound(level, khan.position(), SoundEvents.HORSE_ANGRY, SoundSource.NEUTRAL, 1.5F, 0.9F);
		}
		if (tick == SHOUT) {
			pose(khan, Skins.Pose.RAISE);
			speak(level, khan, "fakedeath.genghis.khurai", KHAN, 40);
			sound(level, khan.position(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.0F, 1.3F);
		}
		if (tick == ARROWS) {
			// A rain of arrows, high up and down around the player.
			pose(khan, null);
			for (int i = 0; i < 14; i++) {
				RemotePlayer archer = riders.get(1 + i % (riders.size() - 1));
				Vec3 from = archer.getEyePosition();
				Vec3 to = player.position().add((random.nextDouble() - 0.5) * 3.0, 0.1, (random.nextDouble() - 0.5) * 3.0);
				if (i % 5 == 0) {
					to = eyes;
				}
				Display.ItemDisplay arrow = itemDisplay(level, new ItemStack(Items.ARROW), from);
				arrows.add(arrow);
				arrowFrom.add(from);
				arrowTo.add(to);
				Looks.swing(archer);
			}
			sound(level, eyes.add(look.scale(8.0)), SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 2.0F, 0.8F);
			sound(level, eyes.add(look.scale(8.0)), SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 2.0F, 1.0F);
		}
		if (tick > ARROWS && tick <= ARROWS + ARROW_FLIGHT) {
			float p = (tick - ARROWS) / (float) ARROW_FLIGHT;
			for (int i = 0; i < arrows.size(); i++) {
				Vec3 a = arrowFrom.get(i).lerp(arrowTo.get(i), p).add(0.0, Math.sin(p * Math.PI) * 6.0, 0.0);
				Vec3 b = arrowFrom.get(i).lerp(arrowTo.get(i), Math.min(1.0F, p + 0.05F)).add(0.0, Math.sin(Math.min(1.0F, p + 0.05F) * Math.PI) * 6.0, 0.0);
				Vec3 dir = b.subtract(a);
				Quaternionf along = new Quaternionf().rotationTo(new Vector3f(1.0F, 1.0F, 0.0F).normalize(), new Vector3f((float) dir.x, (float) dir.y, (float) dir.z).normalize());
				place(arrows.get(i), a, new Vector3f(0.9F), along);
			}
			if (tick == ARROWS + ARROW_FLIGHT) {
				sound(level, eyes, SoundEvents.ARROW_HIT, SoundSource.HOSTILE, 1.5F, 1.0F);
				sound(level, eyes, SoundEvents.ARROW_HIT_PLAYER, SoundSource.HOSTILE, 1.0F, 1.0F);
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.35F);
			}
		}
		if (tick == CHARGE) {
			sound(level, eyes.add(look.scale(6.0)), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.5F, 1.0F);
			for (RemotePlayer rider : riders) {
				pose(rider, Skins.Pose.RAISE);
			}
		}
		if (tick >= CHARGE && distance < 2.5 && death == Integer.MAX_VALUE) {
			// Trampled.
			cameraShake(0.6F);
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.5F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(player.blockPosition().below())), player.position(), 30, 1.0, 0.2);
			death = tick + 6;
		}
		if (tick >= CHARGE) {
			cameraShake(Math.min(0.4F, 0.08F + (tick - CHARGE) * 0.01F));
		}
		turnTowards(player, khan.getEyePosition(), tick < CHARGE ? 0.06F : 0.15F);
		if (tick == death) {
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		for (int i = 0; i < RIDERS; i++) {
			// The Khan in the middle, his riders spread out to both sides.
			double across = i == 0 ? 0.0 : ((i + 1) / 2) * 2.6 * (i % 2 == 0 ? -1.0 : 1.0);
			Vec3 lane = side.scale(across);
			lanes.add(lane);
			Vec3 at = onGround(level, player, player.position().add(lane).add(look.scale(START)));
			Horse horse = spawn(level, EntityTypes.HORSE, at);
			horse.noPhysics = true;
			RemotePlayer rider = i == 0
				? actor(level, "fakedeath.genghis.name", "genghis", false, at.add(0.0, 1.0, 0.0))
				: extra(level, "mongol", at.add(0.0, 1.0, 0.0));
			rider.startRiding(horse);
			hold(rider, new ItemStack(i == 0 ? Items.IRON_SWORD : Items.BOW));
			horses.add(horse);
			riders.add(rider);
		}
		pole = blockDisplay(level, Blocks.DARK_OAK_PLANKS.defaultBlockState(), riders.get(1).position(), 32.0F);
		tails = blockDisplay(level, Blocks.WOOL.white().defaultBlockState(), riders.get(1).position(), 32.0F);
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		for (RemotePlayer rider : riders) {
			rider.stopRiding();
		}
		super.end(level, player);
	}
}
