package de.derpvpclient.fakedeath.scene;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.Vec3;

/**
 * A real player standing close by "kills" you, they see nothing of it. With nobody around, for a test,
 * a stranger with a made-up name sprints in from behind with a sword instead.
 */
public class PlayerKillScene extends Scene {
	private static final List<String> FIRST = List.of("Shadow", "Night", "Dark", "Silent", "Frost", "Blaze", "Void", "Toxic");
	private static final List<String> SECOND = List.of("Blade", "Hunter", "Ninja", "Wolf", "Reaper", "Sniper", "Viper", "Ghost");
	private static final int RUN = 26;
	private static final int[] HITS = {14, 30, 46};

	private final AbstractClientPlayer killer;
	private Component killerName;
	private RemotePlayer stranger;
	private Vec3 runFrom;
	private Vec3 runTo;

	public PlayerKillScene(AbstractClientPlayer killer) {
		this.killer = killer;
		this.killerName = killer != null ? killer.getDisplayName() : Component.empty();
	}

	/** Nobody around: a made-up player does it. */
	public static PlayerKillScene stranger() {
		return new PlayerKillScene(null);
	}

	private int start() {
		return killer != null ? 0 : RUN;
	}

	@Override
	public int length() {
		return start() + HITS[HITS.length - 1] + 12;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("death.attack.player", player.getDisplayName(), killerName);
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (killer == null) {
			strangerTick(level, player, tick);
		} else if (!killer.isRemoved()) {
			turnTowards(player, killer.getEyePosition(), tick < 14 ? 0.25F : 0.1F);
		}

		// Three hits, the last one a critical one.
		for (int hit : HITS) {
			if (tick == start() + hit) {
				boolean last = hit == HITS[HITS.length - 1];
				if (stranger != null) {
					stranger.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
				}
				sound(level, player.position(), last ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 1.0F);
				particles(level, ParticleTypes.CRIT, player.getEyePosition().add(0, -0.4, 0), last ? 16 : 8, 0.3, 0.3);
				hit(level, player, SoundEvents.PLAYER_HURT, 0.35F, last);
			}
		}
	}

	private void strangerTick(ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			String name = FIRST.get(random.nextInt(FIRST.size())) + "_" + SECOND.get(random.nextInt(SECOND.size())) + (10 + random.nextInt(90));
			stranger = spawn(level, new RemotePlayer(level, new GameProfile(UUID.randomUUID(), name)), groundSpot(level, player, 8.0, 150.0 + random.nextDouble() * 60.0));
			stranger.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
			stranger.setSprinting(true);
			stranger.noPhysics = true;
			killerName = stranger.getDisplayName();
			runFrom = stranger.position();
			Vec3 toward = player.position().subtract(runFrom);
			runTo = player.position().subtract(new Vec3(toward.x, 0.0, toward.z).normalize().scale(1.3));
		}

		if (stranger == null) {
			return;
		}

		if (tick <= RUN) {
			// Sprinting in, the footsteps come closer.
			Vec3 at = runFrom.lerp(runTo, tick / (double) RUN);
			walk(stranger, new Vec3(at.x, floor(level, at.x, at.z, player.getY()), at.z));
			if (tick % 4 == 0) {
				sound(level, at, SoundEvents.GRASS_STEP, SoundSource.PLAYERS, 1.0F, 1.1F);
			}
		}
		if (tick == RUN) {
			stranger.setSprinting(false);
		}

		face(stranger, player.getEyePosition());
		turnTowards(player, stranger.getEyePosition(), tick < RUN - 10 ? 0.06F : 0.3F);
	}
}
