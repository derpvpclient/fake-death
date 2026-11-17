package de.derpvpclient.fakedeath.scene;

import java.util.ArrayList;
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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Colorado Springs, 1899. Two great Tesla coils, one at each side of the player. Nikola Tesla: "The present is yours.
 * The future, for which I really worked, is mine." He throws the switch. The coils hum, sparks crawl over them, and
 * "In the Hall of the Mountain King" starts, slow, then faster and faster, the arcs reaching further with it, between
 * the coils, right in front of the player's face. At the last chord the arc goes through the player.
 */
public class TeslaScene extends CastScene {
	private static final int FUTURE = ARRIVE + 8;
	private static final int SWITCH = ARRIVE + 70;
	private static final int ZAP = SWITCH + 170;
	private static final ChatFormatting SERBIA = ChatFormatting.AQUA;
	/** "In the Hall of the Mountain King" (Edvard Grieg, 1875) as in the score, the theme in B minor, one octave up. */
	private static final int[][] HALL = {{59, 2}, {61, 2}, {62, 2}, {64, 2}, {66, 2}, {62, 2}, {66, 4}, {65, 2}, {61, 2}, {65, 4}, {64, 2}, {60, 2}, {64, 4},
		{59, 2}, {61, 2}, {62, 2}, {64, 2}, {66, 2}, {62, 2}, {66, 2}, {71, 2}, {69, 2}, {66, 2}, {62, 2}, {66, 2}, {69, 4}, {0, 4}};

	private RemotePlayer tesla;
	private final List<Vec3> tops = new ArrayList<>();
	private Vec3 lever;
	private int hall;
	private int played;
	private float step = 3.0F;
	private int loopStart = -1;
	private float glow;

	@Override
	public int length() {
		return ZAP + 4;
	}

	@Override
	public int photoTick() {
		return ZAP - 30;
	}

	@Override
	public int screenTint() {
		// The lab lit by the arcs: a blue-white flicker that grows.
		return glow > 0.0F ? (int) (glow * 120.0F) << 24 | 0xA0C8FF : 0;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.tesla", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (tesla == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();
		face(tesla, eyes);
		if (tick == FUTURE) {
			speak(level, tesla, "fakedeath.tesla.future", SERBIA, 60);
		}
		if (tick == SWITCH) {
			Looks.swing(tesla);
			pose(tesla, Skins.Pose.RAISE);
			Scene.sound(level, lever, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.5F, 0.6F);
			Scene.sound(level, lever, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5F, 0.5F);
			loopStart = tick;
		}

		float charge = tick < SWITCH ? 0.0F : Math.min(1.0F, (tick - SWITCH) / (float) (ZAP - SWITCH));
		if (loopStart >= 0 && tick < ZAP) {
			// The Mountain King, faster with every round.
			int since = tick - loopStart;
			int loop = Band.length(HALL, step);
			if (since >= loop) {
				loopStart = tick;
				since = 0;
				step = Math.max(1.0F, step - 0.6F);
				played++;
			}
			int midi = Band.noteAt(HALL, since, step);
			if (midi > 0) {
				Band.note(level, eyes, Band.BASS, midi - 24, 0.9F + charge * 0.5F);
				Band.note(level, eyes, played >= 1 ? Band.BIT : Band.HARP, midi, 0.5F + charge * 0.6F);
				if (played >= 2) {
					// The flute sounds an octave above: the tune doubled, louder and louder.
					Band.note(level, eyes, Band.FLUTE, midi, 0.4F + charge * 0.6F);
				}
			}
			if (tick % 20 == 0) {
				Scene.sound(level, lever, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 1.0F + charge, 0.5F + charge);
			}
		}

		// Sparks crawl over the coils, arcs reach out further and further, then between them, then the player.
		if (tick >= SWITCH && tick < ZAP) {
			for (Vec3 top : tops) {
				particles(level, ParticleTypes.ELECTRIC_SPARK, top, 2 + (int) (charge * 8), 0.6, 0.2);
			}
			int every = Math.max(2, 10 - (int) (charge * 8));
			if (tick % every == 0) {
				Vec3 from = tops.get(random.nextInt(2));
				Vec3 to;
				if (charge < 0.4F) {
					to = from.add((random.nextDouble() - 0.5) * 3.0, -random.nextDouble() * 2.0, (random.nextDouble() - 0.5) * 3.0);
				} else if (charge < 0.8F) {
					to = tops.get(0) == from ? tops.get(1) : tops.get(0);
				} else {
					to = eyes.add((random.nextDouble() - 0.5) * 0.8, (random.nextDouble() - 0.5) * 0.6, (random.nextDouble() - 0.5) * 0.8);
				}
				arc(level, from, to);
				Scene.sound(level, from.lerp(to, 0.5), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.3F + charge * 0.5F, 1.6F);
				glow = 0.2F + charge * 0.5F;
			}
		}
		glow = Math.max(0.0F, glow - 0.06F);
		if (tick == ZAP) {
			LightningBolt bolt = spawn(level, EntityTypes.LIGHTNING_BOLT, player.position());
			bolt.setVisualOnly(true);
			level.setSkyFlashTime(2);
			arc(level, tops.get(0), eyes);
			arc(level, tops.get(1), eyes);
			glow = 1.0F;
			particles(level, ParticleTypes.ELECTRIC_SPARK, eyes, 60, 0.8, 1.0);
			Scene.sound(level, eyes, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.0F, 1.0F);
			kill(level, player, SoundEvents.PLAYER_HURT_ON_FIRE);
		}
		cameraShake(charge > 0.6F && tick < ZAP ? (charge - 0.6F) * 0.2F : 0.0F);

		if (tick < SWITCH + 10) {
			turnTowards(player, tesla.getEyePosition(), 0.08F);
		} else {
			// Back and forth between the coils.
			Vec3 top = tops.get(((tick - SWITCH) / 30) % 2);
			turnTowards(player, top.lerp(eyes.add(horizontalLook(player).scale(3.0)), charge * 0.8), 0.06F + charge * 0.1F);
		}
	}

	/** A jagged line of sparks from one point to another, like an arc of lightning. */
	private void arc(ClientLevel level, Vec3 from, Vec3 to) {
		Vec3 last = from;
		int pieces = 7;
		for (int i = 1; i <= pieces; i++) {
			Vec3 next = from.lerp(to, i / (double) pieces);
			if (i < pieces) {
				next = next.add((random.nextDouble() - 0.5) * 0.6, (random.nextDouble() - 0.5) * 0.6, (random.nextDouble() - 0.5) * 0.6);
			}
			line(level, ParticleTypes.END_ROD, last, next, 0.25);
			last = next;
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		Vec3 look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		float yaw = (float) Math.atan2(look.x, look.z);
		for (int s : new int[] {-1, 1}) {
			// A coil: a copper base, the tall wound column, the big round top.
			Vec3 foot = onGround(level, player, player.position().add(look.scale(3.0)).add(side.scale(s * 2.8)));
			place(blockDisplay(level, Blocks.CONCRETE.orange().defaultBlockState(), foot, 8.0F), foot.add(0.0, 0.3, 0.0), new Vector3f(1.0F, 0.6F, 1.0F), new Quaternionf().rotateY(yaw));
			for (int k = 0; k < 6; k++) {
				place(blockDisplay(level, k % 2 == 0 ? Blocks.WOOL.orange().defaultBlockState() : Blocks.WOOL.brown().defaultBlockState(), foot, 8.0F),
					foot.add(0.0, 0.85 + k * 0.5, 0.0), new Vector3f(0.45F, 0.5F, 0.45F), new Quaternionf().rotateY(yaw + k * 0.3F));
			}
			Vec3 top = foot.add(0.0, 4.0, 0.0);
			for (int k = 0; k < 2; k++) {
				place(blockDisplay(level, Blocks.IRON_BLOCK.defaultBlockState(), foot, 8.0F), top, new Vector3f(1.5F, 0.35F, 1.5F), new Quaternionf().rotateY(yaw + k * Mth.PI / 4.0F));
			}
			tops.add(top.add(0.0, 0.2, 0.0));
		}
		Vec3 desk = onGround(level, player, player.position().add(look.scale(5.5)));
		lever = desk.add(0.0, 1.0, 0.0);
		place(blockDisplay(level, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), desk, 2.0F), desk.add(0.0, 0.45, 0.0), new Vector3f(1.0F, 0.9F, 0.5F), new Quaternionf().rotateY(yaw));
		place(blockDisplay(level, Blocks.LEVER.defaultBlockState(), desk, 2.0F), desk.add(0.0, 1.05, 0.0), new Vector3f(0.6F), new Quaternionf().rotateY(yaw));
		tesla = actor(level, "fakedeath.tesla.name", "tesla", false, onGround(level, player, desk.add(look.scale(0.7))));
		arrive(level, player, tesla, 0);
	}
}
