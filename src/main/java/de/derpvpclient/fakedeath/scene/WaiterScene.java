package de.derpvpclient.fakedeath.scene;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A waiter hurries over with a big cake: "Here is your food!" He trips, the cake flies right into the player's face.
 * Everything goes white, and they choke on whipped cream.
 */
public class WaiterScene extends NpcScene {
	private static final int TRIP = 22;
	private static final int SPLAT = TRIP + 12;
	private static final int[] CHOKE = {SPLAT + 22, SPLAT + 42};
	private static final int DEATH = SPLAT + 62;
	private Display.BlockDisplay cake;
	private Vec3 from;
	private int cream = -1;

	public WaiterScene() {
		super("kellner", ChatFormatting.YELLOW);
	}

	@Override
	protected int lineCount() {
		return 2;
	}

	@Override
	protected int actLength() {
		return DEATH + 50;
	}

	@Override
	protected int photoOffset() {
		return TRIP + 6;
	}

	@Override
	protected double standDistance() {
		return 4.2;
	}

	@Override
	public int screenTint() {
		if (cream < 0) {
			return 0;
		}
		// Whipped cream all over the view, a bit wiped away now and then, never enough.
		int alpha = cream < 6 ? 250 : 215 + (int) (Mth.sin(cream * 0.3F) * 25);
		return (alpha << 24) | 0xFFF8E8;
	}

	@Override
	protected Vec3 focus(LocalPlayer player) {
		return cake != null ? cake.position() : npc.getEyePosition();
	}

	@Override
	protected void setup(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
		cake = blockDisplay(level, Blocks.CAKE.defaultBlockState(), npc.getEyePosition(), 3.0F);
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		super.tick(minecraft, level, player, tick);
		if (cake != null && tick < actStart()) {
			// While he talks the cake sits on his hand, wobbling a little with every step.
			float yaw = npc.getYRot() * Mth.DEG_TO_RAD;
			Vec3 front = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
			place(cake, npc.getEyePosition().add(front.scale(0.6)).add(0.0, -0.1 + Mth.sin(tick * 0.3F) * 0.03, 0.0), new Vector3f(0.8F), new Quaternionf().rotateY(-yaw));
		}
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		Vec3 toward = towardPlayer(player);
		if (t == 0) {
			from = npc.position();
		}
		if (cream >= 0) {
			cream++;
		}

		if (t < TRIP) {
			// Hurrying over, the cake balanced on one hand.
			Vec3 at = from.add(toward.scale(2.0 * t / TRIP));
			walk(npc, new Vec3(at.x, floor(level, at.x, at.z, player.getY()), at.z));
			if (t % 4 == 0) {
				sound(level, at, SoundEvents.WOOD_STEP, SoundSource.NEUTRAL, 0.8F, 1.3F);
			}
		}
		Vec3 tray = npc.getEyePosition().add(toward.scale(0.6)).add(0.0, -0.1, 0.0);
		Quaternionf facing = new Quaternionf().rotateY((float) Math.atan2(toward.x, toward.z));
		if (cake != null) {
			if (t < TRIP) {
				place(cake, tray.add(0.0, Mth.sin(t * 0.5F) * 0.05, 0.0), new Vector3f(0.8F), facing);
			} else if (t <= SPLAT) {
				// Flying, turning over on the way.
				float p = (t - TRIP) / (float) (SPLAT - TRIP);
				Vec3 at = tray.lerp(player.getEyePosition(), p).add(0.0, Mth.sin(p * Mth.PI) * 0.8, 0.0);
				place(cake, at, new Vector3f(0.8F), new Quaternionf(facing).rotateX(p * Mth.PI));
			}
		}
		if (t == TRIP) {
			// Tripping over his own feet, he nearly falls over.
			speak(minecraft, level, "fakedeath.kellner.oops");
			sound(level, npc.position(), SoundEvents.PLAYER_SMALL_FALL, SoundSource.NEUTRAL, 1.5F, 0.8F);
			Looks.swing(npc);
		}
		if (t > TRIP && t < TRIP + 15) {
			Vec3 at = npc.position();
			walk(npc, new Vec3(at.x, floor(level, at.x, at.z, player.getY()) - Mth.sin((t - TRIP) / 15.0F * Mth.PI) * 0.6, at.z));
			face(npc, player.position());
		}
		if (t == SPLAT) {
			despawn(level, cake);
			cake = null;
			cream = 0;
			sound(level, player.position(), SoundEvents.HONEY_BLOCK_FALL, SoundSource.NEUTRAL, 2.0F, 0.8F);
			sound(level, player.position(), SoundEvents.SLIME_BLOCK_FALL, SoundSource.NEUTRAL, 2.0F, 0.6F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CAKE.defaultBlockState()), player.getEyePosition(), 40, 0.5, 0.3);
			hurt(level, player, SoundEvents.PLAYER_HURT, 0.3F);
			moveCamera(toward.scale(0.25));
		}
		if (t > SPLAT && t < DEATH) {
			cameraShake(0.06F);
			if (t % 6 == 0) {
				particles(level, ParticleTypes.SNOWFLAKE, player.getEyePosition(), 3, 0.4, 0.05);
			}
		}
		for (int c : CHOKE) {
			if (t == c) {
				hurt(level, player, SoundEvents.PLAYER_HURT, 0.2F);
			}
		}
		if (t == DEATH) {
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
		if (t == DEATH + 25) {
			speak(minecraft, level, "fakedeath.kellner.dessert");
		}
	}
}
