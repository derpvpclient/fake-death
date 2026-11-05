package de.derpvpclient.fakedeath.scene;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * A character scene: someone with a job walks up, says a few lines in chat (about four seconds each, so they can be
 * read), and then does their thing, which ends with the killing blow. Each character has its own job and its own way.
 */
public abstract class NpcScene extends Scene {
	/** Ticks between two chat lines. */
	protected static final int LINE = 80;
	private static final int ARRIVE = 40;

	protected final String id;
	protected final Component name;
	protected Villager npc;
	protected Vec3 start;
	protected Vec3 stand;
	// Where the walk in ends, a little further away than stand, the rest it comes closer while talking.
	private Vec3 approach;

	protected NpcScene(String id, ChatFormatting color) {
		this.id = id;
		this.name = Component.translatable("fakedeath." + id + ".name").withStyle(color);
	}

	/** Chat line keys, said one after the other from the arrival on. */
	protected abstract int lineCount();

	/** Ticks of the action after the last line, the killing blow has to fall inside them. */
	protected abstract int actLength();

	/** The action, {@code t} counts from 0 after the last line. */
	protected abstract void act(Minecraft minecraft, ClientLevel level, LocalPlayer player, int t);

	protected ResourceKey<VillagerProfession> profession() {
		return VillagerProfession.NONE;
	}

	protected ItemStack held() {
		return ItemStack.EMPTY;
	}

	/** How close the character comes. */
	protected double standDistance() {
		return 2.6;
	}

	/** Where the camera looks during the action, the character by default. */
	protected Vec3 focus(LocalPlayer player) {
		return npc.getEyePosition();
	}

	/** The character only shows up when this is true, for one that falls from a roof for example. */
	protected boolean walksIn() {
		return true;
	}

	protected final int actStart() {
		return ARRIVE + 2 + lineCount() * LINE;
	}

	@Override
	public int length() {
		return actStart() + actLength();
	}

	@Override
	public int photoTick() {
		return actStart() + photoOffset();
	}

	/** Ticks after the last line for the demo picture, it has to come before the killing blow. */
	protected int photoOffset() {
		return 8;
	}

	@Override
	public Component deathMessage(LocalPlayer player) {
		return Component.translatable("fakedeath.death." + id, player.getDisplayName());
	}

	@Override
	public void tick(Minecraft minecraft, ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			start = groundSpot(level, player, 9.0, (random.nextDouble() - 0.5) * 70.0);
			Vec3 toward = player.position().subtract(start);
			Vec3 flat = new Vec3(toward.x, 0.0, toward.z).normalize();
			stand = player.position().subtract(flat.scale(standDistance()));
			stand = new Vec3(stand.x, floor(level, stand.x, stand.z, player.getY()), stand.z);
			approach = player.position().subtract(flat.scale(standDistance() + 0.9));
			npc = spawn(level, EntityType.VILLAGER, walksIn() ? start : stand);
			npc.setCustomName(name);
			npc.setCustomNameVisible(true);
			npc.noPhysics = true;
			npc.setVillagerData(npc.getVillagerData().withProfession(level.registryAccess(), profession()));
			npc.setItemSlot(EquipmentSlot.MAINHAND, held());
			setup(minecraft, level, player);
		}

		if (npc == null) {
			return;
		}

		if (tick <= ARRIVE && walksIn()) {
			Vec3 pos = start.lerp(approach, tick / (double) ARRIVE);
			walk(npc, new Vec3(pos.x, floor(level, pos.x, pos.z, player.getY()), pos.z));
			if (tick % 6 == 0) {
				sound(level, pos, SoundEvents.GRASS_STEP, SoundSource.NEUTRAL, 0.7F, 1.1F);
			}
		}

		if (tick < actStart()) {
			if (tick > ARRIVE && walksIn()) {
				live(level, player, tick);
			} else {
				face(npc, player.getEyePosition());
			}
			turnTowards(player, npc.getEyePosition(), 0.12F);
		} else {
			turnTowards(player, focus(player), 0.25F);
		}

		int line = tick - ARRIVE - 2;
		if (line >= 0 && line % LINE == 0 && line / LINE < lineCount()) {
			speak(minecraft, level, "fakedeath." + id + "." + (line / LINE + 1));
		}

		if (tick >= actStart()) {
			act(minecraft, level, player, tick - actStart());
		}
	}

	/**
	 * While talking the character is never a statue: it nods along with each line, bobs a little, looks around between
	 * the lines and comes a step closer every time it starts a new one.
	 */
	private void live(ClientLevel level, LocalPlayer player, int tick) {
		int sinceLine = (tick - ARRIVE - 2) % LINE;
		int line = (tick - ARRIVE - 2) / LINE;
		Vec3 eyes = player.getEyePosition();
		Vec3 look;
		if (sinceLine < 35) {
			look = eyes.add(0.0, Math.sin(sinceLine * 0.55) * 0.35, 0.0);
		} else if (sinceLine < 55 && line % 2 == 0) {
			look = eyes.add(towardPlayer(player).yRot(1.2F).scale(3.0)).add(0.0, -0.8, 0.0);
		} else {
			look = eyes;
		}
		face(npc, look);

		// A step closer with every line, never closer than where it does its thing.
		double closer = Math.min(1.0, (tick - ARRIVE) / (double) (actStart() - ARRIVE));
		Vec3 at = approach.lerp(stand, closer);
		double bob = sinceLine < 8 ? Math.sin(sinceLine / 8.0 * Math.PI) * 0.12 : 0.0;
		walk(npc, new Vec3(at.x, floor(level, at.x, at.z, player.getY()) + bob, at.z));
		if (sinceLine == 0) {
			Looks.swing(npc);
		}
	}

	/** Anything the scene needs to build at the start, like a prop. */
	protected void setup(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
	}

	protected void speak(Minecraft minecraft, ClientLevel level, String key) {
		sound(level, npc.position(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 1.0F, 0.9F + random.nextFloat() * 0.3F);
		say(minecraft, name, Component.translatable(key));
	}

	/** From the character towards the player, flat. */
	protected Vec3 towardPlayer(LocalPlayer player) {
		Vec3 delta = player.position().subtract(npc.position());
		return new Vec3(delta.x, 0.0, delta.z).normalize();
	}
}
