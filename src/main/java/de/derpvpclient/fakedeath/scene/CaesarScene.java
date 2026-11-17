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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Rome, the Ides of March. Trumpets, marble columns, Caesar in his toga and laurel wreath among the senators. "The Ides
 * of March are come." A senator: "Ay, Caesar, but not gone." They draw their daggers and turn to him. Caesar smiles and
 * points at the player: "Not me. HIM." The senators close in, one stab after another, and the last one is Brutus.
 * "Et tu, Brute?"
 */
public class CaesarScene extends CastScene {
	private static final int IDES = ARRIVE + 8;
	private static final int NOT_GONE = ARRIVE + 58;
	private static final int DRAW = ARRIVE + 100;
	private static final int HIM = DRAW + 20;
	private static final int TURN = HIM + 22;
	private static final int[] STABS = {TURN + 26, TURN + 36, TURN + 46};
	private static final int ET_TU = TURN + 54;
	private static final int BRUTUS = ET_TU + 18;
	private static final ChatFormatting ROME = ChatFormatting.GOLD;
	/** A fanfare of the trumpets of Rome, the scene's own: F major, up and up. */
	private static final int[][] FANFARE = {{60, 2}, {65, 2}, {69, 2}, {72, 6}, {69, 2}, {72, 2}, {77, 8}, {0, 4},
		{72, 2}, {72, 2}, {74, 2}, {72, 2}, {69, 2}, {65, 2}, {69, 4}, {72, 4}, {65, 12}};
	private static final float STEP = 2.5F;

	private final List<RemotePlayer> senators = new ArrayList<>();
	private RemotePlayer caesar;
	private RemotePlayer brutus;
	private Vec3 forum;
	private int now;

	@Override
	public int length() {
		return BRUTUS + 2;
	}

	@Override
	public int photoTick() {
		return HIM + 6;
	}

	@Override
	public int screenTint() {
		// Warm Roman light, and red at the edges with the stabs.
		int stabbed = 0;
		for (int stab : STABS) {
			stabbed += now >= stab ? 1 : 0;
		}
		return stabbed == 0 ? 0x18FFC060 : (stabbed * 28) << 24 | 0x8A0A0A;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.caesar", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		now = tick;
		if (tick == 0) {
			setUp(level, player);
		}
		if (caesar == null) {
			return;
		}
		bubbles();
		Vec3 eyes = player.getEyePosition();
		Band.play(level, forum, FANFARE, tick - 4, STEP, 1.2F, Band.TRUMPET, Band.FLUTE);
		if (tick >= DRAW && tick < BRUTUS && (tick - DRAW) % 12 == 0) {
			// The drum beats slower and heavier.
			Scene.sound(level, forum, Band.DRUM, SoundSource.RECORDS, 1.2F, 0.6F);
		}

		face(caesar, tick < HIM ? eyes : tick < TURN ? eyes.add(0.0, -0.3, 0.0) : eyes);
		if (tick == IDES) {
			speak(level, caesar, "fakedeath.caesar.ides", ROME, 45);
		}
		if (tick == NOT_GONE) {
			speak(level, senators.get(1), "fakedeath.caesar.notgone", ChatFormatting.GRAY, 40);
		}
		if (tick == DRAW) {
			// Out come the daggers.
			for (RemotePlayer senator : senators) {
				hold(senator, new ItemStack(Items.IRON_SWORD));
			}
			hold(brutus, new ItemStack(Items.IRON_SWORD));
			Scene.sound(level, forum, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 1.5F, 1.2F);
			Scene.sound(level, forum, SoundEvents.ARMOR_EQUIP_CHAIN.value(), SoundSource.NEUTRAL, 1.5F, 0.9F);
		}
		if (tick == HIM) {
			pose(caesar, Skins.Pose.POINT);
			speak(level, caesar, "fakedeath.caesar.him", ROME, 40);
		}

		List<RemotePlayer> all = new ArrayList<>(senators);
		all.add(brutus);
		for (int i = 0; i < all.size(); i++) {
			RemotePlayer senator = all.get(i);
			if (tick < DRAW) {
				face(senator, caesar.getEyePosition());
			} else if (tick < TURN) {
				// Daggers up, at Caesar at first.
				face(senator, caesar.getEyePosition());
			} else {
				// Then at the player, closing in from all round.
				boolean isBrutus = senator == brutus;
				float angle = (i - (all.size() - 1) / 2.0F) * 0.75F;
				Vec3 spot = player.position().add(horizontalLook(player).yRot(angle).scale(isBrutus ? (tick < ET_TU ? 2.0 : 0.9) : 1.0));
				stepTowards(level, player, senator, spot, isBrutus ? 0.12 : 0.1);
				face(senator, eyes);
			}
		}
		for (int i = 0; i < STABS.length; i++) {
			if (tick == STABS[i]) {
				RemotePlayer senator = senators.get(i % senators.size());
				Looks.swing(senator);
				Scene.sound(level, eyes, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.2F, 0.9F);
				particles(level, ParticleTypes.CRIT, eyes.add(0.0, -0.5, 0.0), 10, 0.3, 0.2);
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.25F);
				cameraShake(0.12F);
			}
			if (tick == STABS[i] + 4) {
				cameraShake(0.0F);
			}
		}
		if (tick == ET_TU) {
			// The player's own last words, in chat.
			say(minecraft, player.getDisplayName(), Component.translatable("fakedeath.caesar.ettu"));
			speak(level, brutus, "fakedeath.caesar.brutus", ChatFormatting.GRAY, 30);
		}
		if (tick == BRUTUS) {
			Looks.swing(brutus);
			Scene.sound(level, eyes, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.5F, 0.7F);
			particles(level, ParticleTypes.CRIT, eyes.add(0.0, -0.5, 0.0), 20, 0.3, 0.3);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}

		if (tick < TURN) {
			turnTowards(player, caesar.getEyePosition(), 0.08F);
		} else if (tick < ET_TU - 6) {
			RemotePlayer next = senators.get(Math.min(senators.size() - 1, Math.max(0, (tick - TURN) / 12)));
			turnTowards(player, next.getEyePosition(), 0.15F);
		} else {
			turnTowards(player, brutus.getEyePosition(), 0.25F);
		}
	}

	private void setUp(ClientLevel level, LocalPlayer player) {
		Vec3 look = horizontalLook(player);
		Vec3 side = look.yRot(Mth.HALF_PI);
		forum = onGround(level, player, player.position().add(look.scale(4.0)));
		// A half circle of marble columns behind them, with their capitals, a marble floor.
		BlockState marble = Blocks.QUARTZ_PILLAR.defaultBlockState();
		BlockState capital = Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
		for (int i = 0; i < 7; i++) {
			float angle = (i - 3) * 0.42F;
			Vec3 foot = onGround(level, player, forum.add(look.yRot(angle).scale(4.2)));
			place(blockDisplay(level, marble, foot, 8.0F), foot.add(0.0, 2.2, 0.0), new Vector3f(0.7F, 4.4F, 0.7F), new Quaternionf());
			place(blockDisplay(level, capital, foot, 3.0F), foot.add(0.0, 4.55, 0.0), new Vector3f(1.0F, 0.3F, 1.0F), new Quaternionf());
		}
		place(blockDisplay(level, Blocks.SMOOTH_QUARTZ.defaultBlockState(), forum, 12.0F), forum.add(look.scale(1.0)).add(0.0, 0.02, 0.0),
			new Vector3f(9.0F, 0.04F, 7.0F), new Quaternionf().rotateY((float) Math.atan2(look.x, look.z)));

		caesar = actor(level, "fakedeath.caesar.name", "caesar", false, onGround(level, player, forum.add(look.scale(0.4))));
		wear(caesar, Looks.prop(Items.IRON_NUGGET, "laurel"), 0.05);
		arrive(level, player, caesar, 0);
		double[] across = {-2.4, -1.4, 1.4, 2.4};
		for (int i = 0; i < across.length; i++) {
			Vec3 spot = onGround(level, player, forum.add(side.scale(across[i])).add(look.scale(0.9 - Math.abs(across[i]) * 0.3)));
			RemotePlayer senator = actor(level, "fakedeath.caesar.senator", "senator", false, spot);
			arrive(level, player, senator, 3 + i * 3);
			senators.add(senator);
		}
		brutus = actor(level, "fakedeath.caesar.brutusname", "brutus", false, onGround(level, player, forum.add(look.scale(1.6)).add(side.scale(0.6))));
		arrive(level, player, brutus, 14);
	}
}
