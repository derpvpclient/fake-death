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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * East Berlin, 1961, a press conference. Walter Ulbricht at a table with microphones, the flag of the GDR behind him:
 * "Nobody intends to build a wall!" While he speaks, grey concrete slabs rise out of the ground around the player,
 * one after another. He says it again, more nervously, sweating. Barbed wire comes on top. The last gap closes in
 * front, the slabs press in. "... to build a wall."
 */
public class UlbrichtScene extends CastScene {
	private static final int SAY_1 = 15;
	private static final int SAY_2 = 80;
	private static final int SAY_3 = 140;
	private static final int SAY_4 = 190;
	private static final int SLABS = 14;
	private static final int FIRST_SLAB = 30;
	private static final int SLAB_EVERY = 9;
	private static final int RISE = 12;
	private static final int WIRE = FIRST_SLAB + SLABS * SLAB_EVERY + 6;
	private static final int SQUEEZE = SAY_4 + 15;
	private static final int DEATH = SQUEEZE + 30;
	private static final double RADIUS = 1.6;
	private static final ChatFormatting GDR = ChatFormatting.GOLD;

	private final List<Slab> slabs = new ArrayList<>();
	private final List<Display.BlockDisplay> wire = new ArrayList<>();
	private RemotePlayer ulbricht;
	private Vec3 center;
	private float facing;
	private float dark;

	@Override
	public int length() {
		return DEATH + 1;
	}

	@Override
	public int photoTick() {
		return SAY_3 + 10;
	}

	@Override
	public int screenTint() {
		return dark > 0.0F ? ((int) (dark * 210.0F) << 24) | 0x101010 : 0;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.ulbricht", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			setUp(level, player);
		}
		if (ulbricht == null) {
			return;
		}
		bubbles();
		face(ulbricht, player.getEyePosition());

		if (tick == SAY_1) {
			speak(level, ulbricht, "fakedeath.ulbricht.say", GDR, 60);
		}
		if (tick == SAY_2) {
			speak(level, ulbricht, "fakedeath.ulbricht.again", GDR, 50);
		}
		if (tick == SAY_3) {
			speak(level, ulbricht, "fakedeath.ulbricht.nervous", GDR, 45);
		}
		if (tick == SAY_4) {
			speak(level, ulbricht, "fakedeath.ulbricht.quiet", GDR, 60);
		}
		if (tick > SAY_2 && tick % 6 == 0) {
			// He is sweating more and more.
			particles(level, ParticleTypes.FALLING_WATER, ulbricht.getEyePosition().add(0.0, 0.3, 0.0), 2, 0.25, 0.0);
		}

		int since = tick - FIRST_SLAB;
		if (since >= 0 && since % SLAB_EVERY == 0 && since / SLAB_EVERY < SLABS) {
			addSlab(level, since / SLAB_EVERY, tick);
		}
		double radius = tick < SQUEEZE ? RADIUS : RADIUS - (RADIUS - 0.75) * Math.min(1.0F, (tick - SQUEEZE) / (float) (DEATH - SQUEEZE));
		for (Slab slab : slabs) {
			moveSlab(level, slab, tick, radius);
		}
		if (tick == WIRE) {
			// Barbed wire along the top.
			for (int i = 0; i < SLABS; i++) {
				wire.add(blockDisplay(level, Blocks.IRON_BARS.defaultBlockState(), center, 6.0F));
			}
			sound(level, center, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.5F, 0.7F);
		}
		for (int i = 0; i < wire.size(); i++) {
			float angle = facing + i * Mth.TWO_PI / SLABS;
			Vec3 at = center.add(Mth.sin(angle) * radius, 3.0, Mth.cos(angle) * radius);
			place(wire.get(i), at.add(0.0, 0.4, 0.0), new Vector3f(0.95F, 0.8F, 0.95F), new Quaternionf().rotateY(angle));
		}

		dark = thirdPerson() ? 0.0F : Math.min(0.75F, slabs.size() / (float) SLABS * 0.6F + (tick > SQUEEZE ? 0.15F : 0.0F));
		// Looking round at the wall growing, back to him at every line.
		boolean speaking = Math.abs(tick - SAY_1) < 25 || Math.abs(tick - SAY_2) < 20 || Math.abs(tick - SAY_3) < 20 || tick >= SAY_4;
		if (speaking || slabs.isEmpty()) {
			turnTowards(player, ulbricht.getEyePosition(), 0.1F);
		} else {
			turnTowards(player, slabs.getLast().top(), 0.08F);
		}
		if (tick == SQUEEZE + 10 || tick == SQUEEZE + 20) {
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.3F);
			cameraShake(0.15F);
		}
		if (tick == DEATH) {
			sound(level, center, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 2.0F, 0.5F);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		Vec3 look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		center = player.position();
		// The first slab goes behind the player, the last one closes the gap towards him.
		facing = (float) Math.atan2(-look.x, -look.z);
		Vec3 table = onGround(level, player, player.position().add(look.scale(5.0)));
		float yaw = (float) Math.atan2(look.x, look.z);
		BlockState wood = Blocks.SPRUCE_PLANKS.defaultBlockState();
		place(blockDisplay(level, wood, table, 4.0F), table.add(0.0, 0.8, 0.0), new Vector3f(2.4F, 0.12F, 0.9F), new Quaternionf().rotateY(yaw));
		place(blockDisplay(level, Blocks.CONCRETE.red().defaultBlockState(), table, 4.0F), table.add(0.0, 0.45, 0.0), new Vector3f(2.3F, 0.7F, 0.8F), new Quaternionf().rotateY(yaw));
		// A bunch of microphones on the table.
		for (int i = -1; i <= 1; i++) {
			Vec3 mic = table.add(side.scale(i * 0.25)).add(look.scale(-0.2)).add(0.0, 1.0, 0.0);
			place(blockDisplay(level, Looks.BLACK_CONCRETE, mic, 1.0F), mic, new Vector3f(0.04F, 0.3F, 0.04F), new Quaternionf().rotateY(yaw).rotateX(-0.3F + i * 0.15F));
			place(blockDisplay(level, Looks.GRAY_CONCRETE, mic, 1.0F), mic.add(look.scale(-0.08)).add(0.0, 0.17, 0.0), new Vector3f(0.09F), new Quaternionf());
		}
		ulbricht = actor(level, "fakedeath.ulbricht.name", "ulbricht", false, onGround(level, player, table.add(look.scale(0.8))));
		extra(level, "bodyguard", onGround(level, player, table.add(look.scale(1.4)).add(side.scale(1.4))));
		extra(level, "nkvd", onGround(level, player, table.add(look.scale(1.4)).add(side.scale(-1.4))));
		standFlag(level, player, table.add(look.scale(1.8)).add(side.scale(-2.2)), "flag_gdr", 1.4F);
	}

	private void addSlab(ClientLevel level, int index, int tick) {
		Display.BlockDisplay slab = blockDisplay(level, Blocks.CONCRETE.lightGray().defaultBlockState(), center, 8.0F);
		// Alternating left and right of the back, so the gap closes in front last.
		int step = (index + 1) / 2;
		float around = facing + (index % 2 == 0 ? step : -step) * Mth.TWO_PI / SLABS;
		slabs.add(new Slab(slab, around, tick));
		Vec3 at = center.add(Mth.sin(around) * RADIUS, 0.0, Mth.cos(around) * RADIUS);
		sound(level, at, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.5F, 0.6F);
		sound(level, at, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.8F, 0.6F);
		particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CONCRETE.lightGray().defaultBlockState()), at.add(0.0, 0.2, 0.0), 12, 0.5, 0.1);
	}

	private void moveSlab(ClientLevel level, Slab slab, int tick, double radius) {
		float rise = Math.min(1.0F, (tick - slab.start()) / (float) RISE);
		Vec3 at = center.add(Mth.sin(slab.angle()) * radius, 0.0, Mth.cos(slab.angle()) * radius);
		float height = 3.0F * rise;
		// A slab of concrete, a little wider than the gap between two, standing on edge facing the player.
		slab.display().setPos(at.x, at.y, at.z);
		Quaternionf turn = new Quaternionf().rotateY(slab.angle());
		transform(slab.display(), new Transformation(new Vector3f(-0.42F, 0.0F, -0.12F).rotate(turn), turn, new Vector3f(0.84F, height, 0.24F), new Quaternionf()));
	}

	private record Slab(Display.BlockDisplay display, float angle, int start) {
		Vec3 top() {
			return display.position().add(0.0, 2.5, 0.0);
		}
	}
}
