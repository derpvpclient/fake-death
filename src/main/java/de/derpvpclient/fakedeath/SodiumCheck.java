package de.derpvpclient.fakedeath;

/** Sodium's settings page is only built for Minecraft 26.3; in this version the options button always shows. */
public final class SodiumCheck {
	private SodiumCheck() {
	}

	public static boolean hasPage() {
		return false;
	}
}
