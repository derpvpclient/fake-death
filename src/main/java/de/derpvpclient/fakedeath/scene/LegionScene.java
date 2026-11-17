package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A Roman legion marches up to the beat of the drum, the vexillum with SPQR held high behind it, the buccinae
 * sounding. The centurion: "Legio! Testudo!" The shields go up, in front and over the heads, the tortoise closes. It
 * halts. "Pila!" A rain of javelins comes down around the player. Then: "Ad victoriam!" and the tortoise charges.
 */
public class LegionScene extends CastScene {
	private static final int FILES = 3;
	private static final int ROWS = 3;
	private static final double START = 18.0;
	private static final double HALT = 5.5;
	private static final int MARCH = 10;
	private static final int TESTUDO = ARRIVE + 10;
	private static final int FORM = 15;
	private static final int PILA = ARRIVE + 100;
	private static final int VOLLEY = 40;
	private static final int CHARGE = PILA + VOLLEY + 20;
	private static final double CHARGE_PACE = 0.28;
	private static final int BEAT = 10;
	private static final ChatFormatting ROME = ChatFormatting.DARK_RED;
	/** The call of the buccinae, the scene's own: a horn call in C. */
	private static final int[][] CALL = {{60, 2}, {64, 2}, {67, 4}, {67, 2}, {72, 6}, {0, 2}, {67, 2}, {72, 2}, {76, 8}};

	private final List<RemotePlayer> legion = new ArrayList<>();
	private final List<Display.BlockDisplay> shields = new ArrayList<>();
	private final List<Display.BlockDisplay> bosses = new ArrayList<>();
	private final List<Pilum> pila = new ArrayList<>();
	private RemotePlayer centurion;
	private RemotePlayer bearer;
	private Display.ItemDisplay vexillum;
	private Vec3 look;
	private Vec3 side;
	private float yaw;
	private double distance = START;
	private int callAt = -100;
	private boolean hit;

	@Override
	public int length() {
		return CHARGE + 60;
	}

	@Override
	public int photoTick() {
		return PILA + 10;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.legion", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (centurion == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();

		// Marching up, halting for the javelins, then the charge.
		if (tick >= MARCH && tick < PILA) {
			distance = Math.max(HALT, START - (tick - MARCH) * (START - HALT) / (PILA - MARCH));
		} else if (tick >= CHARGE) {
			distance -= CHARGE_PACE;
		}
		boolean moving = tick >= MARCH && tick < PILA || tick >= CHARGE;
		if (moving && tick % BEAT == 0 && tick < CHARGE) {
			// The drum and the step of the legion.
			Scene.sound(level, eyes.add(look.scale(distance)), Band.DRUM, SoundSource.RECORDS, 1.2F, 0.7F);
			Scene.sound(level, eyes.add(look.scale(distance)), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 1.0F, 0.6F);
		}
		if (tick >= CHARGE && tick % 4 == 0) {
			Scene.sound(level, eyes.add(look.scale(distance)), Band.DRUM, SoundSource.RECORDS, 1.4F, 0.9F);
		}

		float form = Mth.clamp((tick - TESTUDO) / (float) FORM, 0.0F, 1.0F);
		for (int i = 0; i < legion.size(); i++) {
			RemotePlayer soldier = legion.get(i);
			int file = i % FILES - 1;
			int row = i / FILES;
			Vec3 at = onGround(level, player, player.position().add(look.scale(distance + row * 1.05)).add(side.scale(file * 1.05)));
			if (moving) {
				walk(soldier, at);
			}
			face(soldier, at.add(look.scale(-4.0)).add(0.0, 1.6, 0.0));
			placeShield(i, soldier.position(), row == 0, form);
		}
		// The standard behind them, the centurion at the side.
		Vec3 back = player.position().add(look.scale(distance + ROWS * 1.05 + 0.6));
		if (moving) {
			walk(bearer, onGround(level, player, back));
		}
		face(bearer, back.add(look.scale(-4.0)).add(0.0, 1.6, 0.0));
		place(vexillum, bearer.position().add(0.0, 2.0, 0.0), new Vector3f(1.0F), new Quaternionf().rotateY(yaw + Mth.PI));
		Vec3 aside = onGround(level, player, player.position().add(look.scale(distance + 0.5)).add(side.scale(2.4)));
		if (moving) {
			walk(centurion, aside);
		}
		face(centurion, tick < CHARGE ? eyes : aside.add(look.scale(-4.0)).add(0.0, 1.6, 0.0));

		if (tick == TESTUDO) {
			pose(centurion, Skins.Pose.RAISE);
			speak(level, centurion, "fakedeath.legion.testudo", ROME, 40);
			callAt = tick;
			Scene.sound(level, eyes.add(look.scale(distance)), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 1.5F, 1.2F);
		}
		if (tick == TESTUDO + 30) {
			pose(centurion, null);
		}
		if (tick == PILA) {
			pose(centurion, Skins.Pose.POINT);
			speak(level, centurion, "fakedeath.legion.pila", ROME, 35);
			callAt = tick;
		}
		if (tick > PILA && tick < PILA + VOLLEY && tick % 3 == 0) {
			// The javelins come down all around the player, a few very close.
			double spread = 4.0 - (tick - PILA) * 0.07;
			Vec3 to = onGround(level, player, player.position().add(look.yRot((random.nextFloat() - 0.5F) * 2.6F).scale(0.8 + random.nextDouble() * spread)));
			Vec3 from = player.position().add(look.scale(distance + 2.0)).add(side.scale((random.nextDouble() - 0.5) * 3.0)).add(0.0, 2.0, 0.0);
			Display.ItemDisplay pilum = itemDisplay(level, new ItemStack(Items.TRIDENT), from);
			pila.add(new Pilum(pilum, from, to, tick));
			Scene.sound(level, from, SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 1.2F, 0.6F);
		}
		movePila(level, player, tick);
		if (tick == CHARGE) {
			pose(centurion, Skins.Pose.RAISE);
			speak(level, centurion, "fakedeath.legion.victory", ROME, 40);
			callAt = tick;
			Scene.sound(level, eyes, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.8F, 1.3F);
		}
		Band.play(level, eyes.add(look.scale(distance + 3.0)), CALL, tick - callAt, 1.5F, 1.4F, Band.TRUMPET, Band.FLUTE);

		if (tick >= CHARGE && !hit && distance <= 0.7) {
			// The wall of shields hits.
			hit = true;
			Scene.sound(level, eyes, SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 2.0F, 0.6F);
			Scene.sound(level, eyes, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5F, 0.8F);
			particles(level, ParticleTypes.CRIT, eyes, 30, 0.6, 0.3);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}

		if (tick < TESTUDO + 20) {
			turnTowards(player, tick < MARCH + 30 ? vexillum.position() : centurion.getEyePosition(), 0.06F);
		} else if (tick < PILA + 10) {
			turnTowards(player, eyes.add(look.scale(distance)).add(0.0, 0.3, 0.0), 0.08F);
		} else if (tick < CHARGE) {
			turnTowards(player, eyes.add(look.scale(distance * 0.5)).add(0.0, 2.5, 0.0), 0.1F);
		} else {
			turnTowards(player, eyes.add(look.scale(Math.max(1.5, distance))).add(0.0, -0.2, 0.0), 0.2F);
		}
	}

	/** A shield: carried at the side, then raised, in front of the front row, over the heads of the others. */
	private void placeShield(int i, Vec3 at, boolean front, float form) {
		Vec3 toward = look.reverse();
		Vec3 carried = at.add(toward.scale(0.35)).add(side.scale(-0.35)).add(0.0, 0.9, 0.0);
		Vec3 raised = front ? at.add(toward.scale(0.5)).add(0.0, 1.0, 0.0) : at.add(toward.scale(0.1)).add(0.0, 2.15, 0.0);
		Vec3 pos = carried.lerp(raised, form);
		Quaternionf turn = new Quaternionf().rotateY(yaw);
		if (!front) {
			turn.rotateX(-Mth.HALF_PI * form);
		} else {
			turn.rotateY(Mth.HALF_PI * (1.0F - form));
		}
		place(shields.get(i), pos, new Vector3f(1.05F, 1.3F, 0.08F), turn);
		place(bosses.get(i), front ? pos.add(toward.scale(0.06)) : pos.add(0.0, 0.06, 0.0), new Vector3f(form > 0.5F ? 0.22F : 0.0F), turn);
	}

	private void movePila(ClientLevel level, LocalPlayer player, int tick) {
		for (Iterator<Pilum> it = pila.iterator(); it.hasNext();) {
			Pilum pilum = it.next();
			float t = Math.min(1.0F, (tick - pilum.start()) / 14.0F);
			Vec3 at = pilum.from().lerp(pilum.to(), t).add(0.0, Math.sin(t * Math.PI) * 5.0, 0.0);
			Vec3 way = pilum.to().subtract(pilum.from());
			float heading = (float) Math.atan2(way.x, way.z);
			float pitch = (float) (Mth.HALF_PI * 0.6 - t * Mth.PI * 0.6);
			place(pilum.display(), at.add(0.0, t >= 1.0F ? 0.4 : 0.0, 0.0), new Vector3f(1.4F), new Quaternionf().rotateY(heading).rotateX(-pitch).rotateZ(Mth.PI / 4.0F));
			if (t >= 1.0F && tick == pilum.start() + 14) {
				Scene.sound(level, pilum.to(), SoundEvents.ARROW_HIT, SoundSource.HOSTILE, 1.2F, 0.6F);
				if (pilum.to().distanceTo(player.position()) < 1.1) {
					hurt(level, player, SoundEvents.PLAYER_HURT, 0.2F);
				}
			}
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		look = horizontalLook(player);
		side = look.yRot(Mth.HALF_PI);
		yaw = (float) Math.atan2(look.x, look.z);
		for (int i = 0; i < FILES * ROWS; i++) {
			int file = i % FILES - 1;
			int row = i / FILES;
			Vec3 at = onGround(level, player, player.position().add(look.scale(START + row * 1.05)).add(side.scale(file * 1.05)));
			RemotePlayer soldier = extra(level, "legionary", at);
			soldier.setItemSlot(EquipmentSlot.HEAD, Looks.prop(Items.IRON_NUGGET, "galea"));
			hold(soldier, new ItemStack(Items.IRON_SWORD));
			legion.add(soldier);
			shields.add(blockDisplay(level, Blocks.RED_CONCRETE.defaultBlockState(), at, 6.0F));
			bosses.add(blockDisplay(level, Blocks.GOLD_BLOCK.defaultBlockState(), at, 6.0F));
		}
		bearer = extra(level, "legionary", onGround(level, player, player.position().add(look.scale(START + ROWS * 1.05 + 0.6))));
		bearer.setItemSlot(EquipmentSlot.HEAD, Looks.prop(Items.IRON_NUGGET, "galea"));
		vexillum = itemDisplay(level, Looks.prop(Items.IRON_NUGGET, "flag_spqr"), bearer.position());
		Looks.farView(vexillum);
		centurion = actor(level, "fakedeath.legion.centurion", "legionary", false, onGround(level, player, player.position().add(look.scale(START + 0.5)).add(side.scale(2.4))));
		wear(centurion, Looks.prop(Items.IRON_NUGGET, "galea_centurion"), 0.45);
		hold(centurion, new ItemStack(Items.STICK));
		// The standards of Rome at both sides of the field.
		standFlag(level, player, player.position().add(look.scale(7.0)).add(side.scale(4.5)), "flag_spqr", 1.2F);
		standFlag(level, player, player.position().add(look.scale(7.0)).add(side.scale(-4.5)), "flag_spqr", 1.2F);
	}

	private record Pilum(Display.ItemDisplay display, Vec3 from, Vec3 to, int start) {
	}
}
