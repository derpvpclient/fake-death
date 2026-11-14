package de.derpvpclient.fakedeath;

import net.fabricmc.loader.api.FabricLoader;

/** Whether Sodium shows Fake Death's page in its video settings; then the extra options button is not needed. */
public final class SodiumCheck {
	private static Boolean hasPage;

	private SodiumCheck() {
	}

	public static boolean hasPage() {
		if (hasPage == null) {
			boolean api;
			try {
				Class.forName("net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint", false, SodiumCheck.class.getClassLoader());
				api = true;
			} catch (ClassNotFoundException | LinkageError e) {
				api = false;
			}
			hasPage = api && FabricLoader.getInstance().isModLoaded("sodium");
		}
		return hasPage;
	}
}
