package de.derpvpclient.fakedeath.scene;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.PlayerSkin;

/**
 * Skins of the mod's own for the people in the scenes, like Herobrine with his white eyes or a president in a suit.
 * They come from the mod's textures, so no real player's skin is ever downloaded for them.
 */
public final class Skins {
	private static final Map<Integer, PlayerSkin> SKINS = new HashMap<>();
	private static final Set<Integer> NAMELESS = new HashSet<>();
	private static final Map<Integer, Pose> POSES = new HashMap<>();
	private static final Map<Object, Pose> POSED = new java.util.WeakHashMap<>();

	/** How a person in a scene holds the arms. */
	public enum Pose {
		/** Both hands together in front of the belly, fingertips touching. */
		RAUTE,
		/** The right arm stretched out, pointing ahead. */
		POINT,
		/** Both arms up in the air. */
		CHEER,
		/** The right arm raised, holding something up high, like a flag. */
		RAISE,
		/** The right hand at the forehead. */
		SALUTE
	}

	/** Sets the arms of an actor, or back to normal with null. */
	static void pose(int entityId, Pose pose) {
		if (pose == null) {
			POSES.remove(entityId);
		} else {
			POSES.put(entityId, pose);
		}
	}

	public static Pose poseOf(int entityId) {
		return POSES.get(entityId);
	}

	/** Remembers which render state belongs to a posed actor, for the model to read. */
	public static void posed(Object state, Pose pose) {
		if (pose == null) {
			POSED.remove(state);
		} else {
			POSED.put(state, pose);
		}
	}

	public static Pose posedState(Object state) {
		return POSED.get(state);
	}

	private Skins() {
	}

	/** Gives the actor with this id the skin {@code textures/entity/skin/<name>.png}, slim arms or wide ones. */
	static void give(int entityId, String name, boolean slim, boolean showName) {
		ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("fakedeath", "textures/entity/skin/" + name + ".png");
		SKINS.put(entityId, new PlayerSkin(texture, null, null, null, slim ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE, true));
		if (!showName) {
			NAMELESS.add(entityId);
		}
	}

	static void forget(int entityId) {
		POSES.remove(entityId);
		SKINS.remove(entityId);
		NAMELESS.remove(entityId);
	}

	/** The skin for an actor, or null for everyone else. */
	public static PlayerSkin of(int entityId) {
		return SKINS.get(entityId);
	}

	public static boolean hidesName(int entityId) {
		return NAMELESS.contains(entityId);
	}
}
