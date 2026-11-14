package de.derpvpclient.fakedeath;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.UpdateChannel;
import com.terraformersmc.modmenu.api.UpdateChecker;
import com.terraformersmc.modmenu.api.UpdateInfo;
import net.minecraft.network.chat.Component;

public class FakeDeathModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return FakeDeathOptionsScreen::new;
	}

	/** Mod Menu marks the mod in its list when a newer version is out and names both sites it can be had from. */
	@Override
	public UpdateChecker getUpdateChecker() {
		return () -> {
			String newer = UpdateCheck.newer();

			return new UpdateInfo() {
				@Override
				public boolean isUpdateAvailable() {
					return newer != null;
				}

				@Override
				public Component getUpdateMessage() {
					return newer == null ? null : Component.translatable(UpdateCheck.curseForgeFirst ? "fakedeath.update.message.curseforge" : "fakedeath.update.message", newer);
				}

				@Override
				public String getDownloadLink() {
					return UpdateCheck.link;
				}

				@Override
				public UpdateChannel getUpdateChannel() {
					return UpdateChannel.RELEASE;
				}
			};
		};
	}
}
