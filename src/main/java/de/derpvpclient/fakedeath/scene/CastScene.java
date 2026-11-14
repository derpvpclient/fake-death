package de.derpvpclient.fakedeath.scene;

import de.derpvpclient.fakedeath.mixin.DisplayAccessor;
import de.derpvpclient.fakedeath.mixin.TextDisplayAccessor;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * A scene with a cast of people: player bodies with the mod's own skins, a speech bubble over each head (the same line
 * also in chat, so it can be read from any view), walking, looking, holding things.
 */
public abstract class CastScene extends Scene {
	private final Map<RemotePlayer, Bubble> bubbles = new HashMap<>();

	/** A person with a name over the head, a skin of the mod's own and a bubble ready for speaking. */
	protected RemotePlayer actor(ClientLevel level, String nameKey, String skin, boolean slim, Vec3 pos) {
		// The name over the head is a sign of its own: player names are limited to 16 letters, a title is not.
		RemotePlayer body = person(level, "Cast", skin, slim, false, pos);
		Display.TextDisplay tag = spawn(level, EntityTypes.TEXT_DISPLAY, pos);
		((DisplayAccessor) tag).fakedeath$setBillboard(Display.BillboardConstraints.CENTER);
		((DisplayAccessor) tag).fakedeath$setInterpolationDuration(1);
		((TextDisplayAccessor) tag).fakedeath$setBackgroundColor(0x40000000);
		((TextDisplayAccessor) tag).fakedeath$setText(Component.translatable(nameKey));
		Looks.glow(tag);
		Display.TextDisplay bubble = spawn(level, EntityTypes.TEXT_DISPLAY, pos);
		((DisplayAccessor) bubble).fakedeath$setBillboard(Display.BillboardConstraints.CENTER);
		((DisplayAccessor) bubble).fakedeath$setInterpolationDuration(1);
		Looks.glow(bubble);
		((TextDisplayAccessor) bubble).fakedeath$setLineWidth(180);
		((TextDisplayAccessor) bubble).fakedeath$setBackgroundColor(0xC0101418);
		((TextDisplayAccessor) bubble).fakedeath$setText(Component.empty());
		bubbles.put(body, new Bubble(bubble, tag, nameKey));
		return body;
	}

	/** A person without a bubble or a name, like a guard in the background. */
	protected RemotePlayer extra(ClientLevel level, String skin, Vec3 pos) {
		return person(level, "Extra", skin, false, false, pos);
	}

	/** The person says a line: over the head for a while, and in chat, with a murmur. */
	protected void speak(ClientLevel level, RemotePlayer actor, String key, ChatFormatting color, int ticks, Object... args) {
		Bubble bubble = bubbles.get(actor);
		Component text = Component.translatable(key, args);
		if (bubble != null) {
			((TextDisplayAccessor) bubble.display()).fakedeath$setText(text.copy().withStyle(color));
			bubble.left = ticks;
			say(Minecraft.getInstance(), Component.translatable(bubble.nameKey()).withStyle(color), text);
		}
		sound(level, actor.position(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 0.9F, 0.6F + random.nextFloat() * 0.2F);
		Looks.swing(actor);
	}

	/** Keeps every bubble over its head and takes it away when its time is up. Call once every tick. */
	protected void bubbles() {
		for (Map.Entry<RemotePlayer, Bubble> entry : bubbles.entrySet()) {
			Bubble bubble = entry.getValue();
			RemotePlayer actor = entry.getKey();
			place(bubble.tag, actor.position().add(0.0, 2.05, 0.0), 0.6F);
			place(bubble.display(), actor.position().add(0.0, 2.45, 0.0), 0.8F);
			if (bubble.left > 0 && --bubble.left == 0) {
				((TextDisplayAccessor) bubble.display()).fakedeath$setText(Component.empty());
			}
		}
	}

	protected static void pose(RemotePlayer actor, Skins.Pose pose) {
		Skins.pose(actor.getId(), pose);
	}

	protected static void hold(RemotePlayer actor, ItemStack stack) {
		actor.setItemSlot(EquipmentSlot.MAINHAND, stack);
	}

	protected static void holdLeft(RemotePlayer actor, ItemStack stack) {
		actor.setItemSlot(EquipmentSlot.OFFHAND, stack);
	}

	/** One step of walking from where the person is towards a spot, at a walking pace, feet on the ground. */
	protected void stepTowards(ClientLevel level, LocalPlayer player, RemotePlayer actor, Vec3 to, double speed) {
		Vec3 from = actor.position();
		Vec3 way = new Vec3(to.x - from.x, 0.0, to.z - from.z);
		Vec3 next = way.length() <= speed ? to : from.add(way.normalize().scale(speed));
		walk(actor, new Vec3(next.x, floor(level, next.x, next.z, player.getY()), next.z));
	}

	/**
	 * A flag on its pole standing on the ground, the printed side of the cloth towards the player. Size 1 is three
	 * blocks tall, the model reaches 24 model pixels under its middle.
	 */
	protected Display.ItemDisplay standFlag(ClientLevel level, LocalPlayer player, Vec3 at, String model, float size) {
		Vec3 ground = onGround(level, player, at);
		Display.ItemDisplay flag = itemDisplay(level, Looks.prop(net.minecraft.world.item.Items.IRON_NUGGET, model), ground);
		Vec3 toPlayer = player.position().subtract(ground);
		float yaw = (float) Math.atan2(toPlayer.x, toPlayer.z);
		place(flag, ground.add(0.0, 1.5 * size, 0.0), new org.joml.Vector3f(size), new org.joml.Quaternionf().rotateY(yaw));
		return flag;
	}

	protected static Vec3 onGround(ClientLevel level, LocalPlayer player, Vec3 spot) {
		return new Vec3(spot.x, floor(level, spot.x, spot.z, player.getY()), spot.z);
	}

	@Override
	public void end(ClientLevel level, LocalPlayer player) {
		bubbles.clear();
		super.end(level, player);
	}

	private static final class Bubble {
		private final Display.TextDisplay display;
		private final String nameKey;
		private final Display.TextDisplay tag;
		int left;

		Bubble(Display.TextDisplay display, Display.TextDisplay tag, String nameKey) {
			this.display = display;
			this.tag = tag;
			this.nameKey = nameKey;
		}

		Display.TextDisplay display() {
			return display;
		}

		String nameKey() {
			return nameKey;
		}
	}
}
