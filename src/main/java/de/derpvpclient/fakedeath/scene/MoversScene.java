package de.derpvpclient.fakedeath.scene;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Two movers carry a huge sofa past the player. "Left! No, YOUR left!" They stumble, and the sofa lands on the player. */
public class MoversScene extends NpcScene {
	private static final float LONG = 2.4F;
	private static final int TIP = 26;
	private Villager partner;
	private Display.BlockDisplay seat;
	private Display.BlockDisplay back;

	public MoversScene() {
		super("umzug", ChatFormatting.GOLD);
	}

	@Override
	protected int lineCount() {
		return 3;
	}

	@Override
	protected int actLength() {
		return TIP + 60;
	}

	@Override
	protected double standDistance() {
		return 2.2;
	}

	@Override
	protected ResourceKey<VillagerProfession> profession() {
		return VillagerProfession.MASON;
	}

	@Override
	protected Vec3 focus(LocalPlayer player) {
		return seat != null ? seat.position() : npc.getEyePosition();
	}

	@Override
	protected void setup(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
		partner = spawn(level, EntityType.VILLAGER, start);
		partner.setCustomName(Component.translatable("fakedeath.umzug.partner").withStyle(ChatFormatting.GOLD));
		partner.setCustomNameVisible(true);
		partner.noPhysics = true;
		partner.setVillagerData(partner.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.MASON));
		seat = blockDisplay(level, Looks.RED_WOOL, start, 4.0F);
		back = blockDisplay(level, Looks.RED_WOOL, start, 4.0F);
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		super.tick(minecraft, level, player, tick);
		if (npc == null || tick >= actStart()) {
			return;
		}
		// The sofa hangs between the two, side by side, the partner on the far end.
		Vec3 side = towardPlayer(player).yRot(Mth.HALF_PI);
		Vec3 other = npc.position().add(side.scale(LONG + 0.4));
		walk(partner, new Vec3(other.x, floor(level, other.x, other.z, npc.getY()), other.z));
		face(partner, player.getEyePosition());
		placeSofa(player, npc.position().add(side.scale(LONG / 2 + 0.2)).add(0.0, 0.8, 0.0), 0.0F);
	}

	@Override
	protected void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t) {
		Vec3 carried = npc.position().add(towardPlayer(player).yRot(Mth.HALF_PI).scale(LONG / 2 + 0.2)).add(0.0, 0.8, 0.0);
		if (t == 0) {
			sound(level, carried, SoundEvents.WOOL_BREAK, SoundSource.NEUTRAL, 1.0F, 0.6F);
			say(minecraft, Component.translatable("fakedeath.umzug.partner").withStyle(ChatFormatting.GOLD), Component.translatable("fakedeath.umzug.oops"));
		}
		if (t <= TIP) {
			// Up and over, end over end, onto the player.
			float p = t / (float) TIP;
			Vec3 at = carried.lerp(player.position().add(0.0, 0.6, 0.0), p).add(0.0, Mth.sin(p * Mth.PI) * 2.2, 0.0);
			placeSofa(player, at, p * Mth.PI);
		}
		if (t == TIP) {
			sound(level, player.position(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 1.2F, 0.6F);
			sound(level, player.position(), SoundEvents.WOOL_BREAK, SoundSource.NEUTRAL, 1.5F, 0.8F);
			particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Looks.RED_WOOL), player.getEyePosition(), 30, 0.8, 0.2);
			kill(level, player, SoundEvents.PLAYER_HURT);
		}
		if (t == TIP + 30) {
			speak(minecraft, level, "fakedeath.umzug.lift");
		}
	}

	private void placeSofa(LocalPlayer player, Vec3 center, float flip) {
		Vec3 dir = towardPlayer(player);
		Quaternionf rotation = new Quaternionf().rotateY((float) Math.atan2(dir.x, dir.z) + Mth.HALF_PI).rotateZ(flip);
		place(seat, center, new Vector3f(LONG, 0.45F, 0.9F), rotation);
		Vector3f up = new Vector3f(0.0F, 0.45F, -0.35F).rotate(rotation);
		place(back, center.add(up.x, up.y, up.z), new Vector3f(LONG, 0.6F, 0.25F), rotation);
	}
}
