package de.derpvpclient.fakedeath.scene;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * Skins of the mod's own for the people in the scenes, like Herobrine with his white eyes or a president in a suit.
 * They come from the mod's textures, so no real player's skin is ever downloaded for them.
 */
public final class Skins {
	private static final Map<Integer, PlayerSkin> SKINS = new HashMap<>();
	private static final Set<Integer> NAMELESS = new HashSet<>();

	private Skins() {
	}

	/** Gives the actor with this id the skin {@code textures/entity/skin/<name>.png}, slim arms or wide ones. */
	static void give(int entityId, String name, boolean slim, boolean showName) {
		Identifier texture = Identifier.fromNamespaceAndPath("fakedeath", "textures/entity/skin/" + name + ".png");
		ClientAsset.ResourceTexture asset = new ClientAsset.ResourceTexture(Identifier.fromNamespaceAndPath("fakedeath", "entity/skin/" + name), texture);
		SKINS.put(entityId, new PlayerSkin(asset, null, null, slim ? PlayerModelType.SLIM : PlayerModelType.WIDE, true));
		if (!showName) {
			NAMELESS.add(entityId);
		}
	}

	static void forget(int entityId) {
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
