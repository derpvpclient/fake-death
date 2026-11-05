package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * A panda waddles over chewing its bamboo, sits down in front of the player and finishes it. Then it looks at the
 * player. Long. Picks them up in both paws, like the next stick of bamboo, and munches.
 */
public class PandaScene extends Scene {
	private static final int ARRIVE = 55;
	private static final int SIT = 60;
	private static final int DONE = 115;
	private static final int GRAB = 145;
	private static final int UP = 22;
	private static final int[] BITES = {GRAB + 40, GRAB + 62, GRAB + 84, GRAB + 106};
	private static final int BITE = 10;

	private Panda panda;
	private Vec3 start;
	private Vec3 stand;
	private Vec3 held;

	@Override
	public int length() {
		return BITES[BITES.length - 1] + 40;
	}

	@Override
	public int photoTick() {
		return BITES[1] + 3;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death.panda", player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			start = groundSpot(level, player, 10.0, (random.nextDouble() - 0.5) * 60.0);
			Vec3 flat = player.position().subtract(start);
			flat = new Vec3(flat.x, 0.0, flat.z).normalize();
			stand = player.position().subtract(flat.scale(1.9));
			stand = new Vec3(stand.x, floor(level, stand.x, stand.z, player.getY()), stand.z);
			panda = spawn(level, EntityType.PANDA, start);
			panda.noPhysics = true;
			panda.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BAMBOO));
		}
		if (panda == null) {
			return;
		}

		Vec3 toPlayer = player.position().subtract(stand);
		toPlayer = new Vec3(toPlayer.x, 0.0, toPlayer.z).normalize();

		if (tick <= ARRIVE) {
			// Waddling over, a little side to side.
			Vec3 at = start.lerp(stand, tick / (double) ARRIVE).add(toPlayer.yRot(Mth.HALF_PI).scale(Mth.sin(tick * 0.3F) * 0.15));
			walk(panda, new Vec3(at.x, floor(level, at.x, at.z, player.getY()), at.z));
			face(panda, player.getEyePosition());
			if (tick % 25 == 0) {
				sound(level, at, SoundEvents.PANDA_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.0F);
			}
		} else {
			walk(panda, stand);
		}

		if (tick == SIT) {
			panda.sit(true);
			panda.eat(true);
		}
		if (tick > SIT && tick < DONE) {
			// Finishing its bamboo, not paying any attention.
			face(panda, panda.getEyePosition().add(toPlayer.yRot(0.6F).scale(3.0)).add(0.0, -1.0, 0.0));
			if (tick % 12 == 0) {
				sound(level, panda.position(), SoundEvents.PANDA_EAT, SoundSource.NEUTRAL, 1.0F, 1.0F);
				particles(level, Looks.bambooCrumbs(), panda.getEyePosition().add(toPlayer.scale(0.5)), 5, 0.2, 0.05);
			}
		}
		if (tick == DONE) {
			panda.eat(false);
			panda.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			sound(level, panda.position(), SoundEvents.PLAYER_BURP, SoundSource.NEUTRAL, 0.8F, 0.7F);
		}
		if (tick >= DONE && tick < GRAB) {
			// Then it looks at the player. Long. A little tilt of the head.
			face(panda, player.getEyePosition().add(toPlayer.yRot(Mth.HALF_PI).scale(Mth.sin((tick - DONE) * 0.08F) * 0.6)));
			if (tick == DONE + 20) {
				sound(level, panda.position(), SoundEvents.PANDA_AMBIENT, SoundSource.NEUTRAL, 1.0F, 0.6F);
			}
		}

		if (tick < GRAB) {
			turnTowards(player, panda.getEyePosition(), 0.12F);
			return;
		}

		face(panda, panda.getEyePosition().add(toPlayer.scale(3.0)));
		// A sitting panda holds its head high and a little forward, the camera stays in front of it.
		Vec3 head = panda.position().add(toPlayer.scale(0.5)).add(0.0, 1.75, 0.0);
		Vec3 mouth = head.add(toPlayer.scale(0.9)).add(0.0, -0.15, 0.0);
		// Held in both paws right in front of its face, like a stick of bamboo.
		Vec3 paws = head.add(toPlayer.scale(1.3)).add(0.0, -0.15, 0.0);
		int t = tick - GRAB;
		if (t == 0) {
			sound(level, panda.position(), SoundEvents.PANDA_BITE, SoundSource.NEUTRAL, 1.0F, 0.8F);
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.15F);
		}
		if (t <= UP) {
			float p = t / (float) UP;
			held = player.getEyePosition().lerp(paws, p * p * (3 - 2 * p)).add(0.0, Mth.sin(p * Mth.PI) * 0.5, 0.0);
		} else {
			held = paws.add(0.0, Mth.sin(t * 0.15F) * 0.04, 0.0);
		}
		if (t == UP + 5) {
			panda.eat(true);
		}

		for (int i = 0; i < BITES.length; i++) {
			int b = tick - BITES[i];
			if (b >= 0 && b < BITE) {
				// Pulled to the mouth, a chomp, and back.
				held = paws.lerp(mouth, Mth.sin(b / (float) BITE * Mth.PI));
			}
			if (b == BITE / 2) {
				sound(level, panda.position(), SoundEvents.PANDA_EAT, SoundSource.NEUTRAL, 1.5F, 0.9F);
				particles(level, Looks.bambooCrumbs(), mouth, 12, 0.25, 0.1);
				particles(level, ParticleTypes.CRIT, mouth, 6, 0.2, 0.1);
				hit(level, player, SoundEvents.PLAYER_HURT, 0.2F, i == BITES.length - 1);
			}
		}
		if (tick == BITES[BITES.length - 1] + 20) {
			sound(level, panda.position(), SoundEvents.PLAYER_BURP, SoundSource.NEUTRAL, 1.0F, 0.6F);
		}

		moveCamera(held.subtract(player.getEyePosition()));
		turnTowards(player, head, 0.3F);
	}
}
