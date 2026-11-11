package de.derpvpclient.fakedeath.scene;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

/** Finds the real things a scene is tied to: the goat, the fox or the painting right next to the player. */
public final class Nearby {
	private Nearby() {
	}

	/** The closest living, visible entity of a type within reach of the player, or null. */
	public static Entity entity(ClientLevel level, LocalPlayer player, Object type, double reach) {
		Entity best = null;
		for (Entity entity : level.entitiesForRendering()) {
			if (entity.getType() == type && entity.isAlive() && !entity.isInvisible() && entity != player
				&& entity.distanceToSqr(player) <= reach * reach && Math.abs(entity.getY() - player.getY()) < 4.0
				&& (best == null || entity.distanceToSqr(player) < best.distanceToSqr(player))) {
				best = entity;
			}
		}
		return best;
	}

	/** A block by its id, for blocks whose constant has another name or shape in other versions of the game. */
	public static net.minecraft.world.level.block.Block block(String id) {
		for (net.minecraft.world.level.block.Block block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
			if (block.getDescriptionId().equals("block.minecraft." + id)) {
				return block;
			}
		}
		return net.minecraft.world.level.block.Blocks.AIR;
	}
}
