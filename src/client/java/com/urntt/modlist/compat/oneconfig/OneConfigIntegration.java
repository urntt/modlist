package com.urntt.modlist.compat.oneconfig;

import com.urntt.modlist.compat.OneConfigBridge;
import com.urntt.modlist.config.ModListConfigScreen;
import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.api.config.v1.ConfigEntrypoint;
import org.polyfrost.oneconfig.api.ui.v1.OneConfigUI;
import org.polyfrost.oneconfig.api.ui.v1.page.ModConfigTarget;
import org.polyfrost.oneconfig.api.ui.v1.page.OneConfigDestination;

/**
 * OneConfig's {@code oneconfig:config} entrypoint. OneConfig only calls it when it is installed, so this package is
 * the only place that may reference OneConfig classes.
 */
public final class OneConfigIntegration implements ConfigEntrypoint {
	@Override
	public void registerConfigs(final Context context) {
		ModListOneConfig config = context.register(new ModListOneConfig());
		// The mod's own screen holds the mode button and the server profiles, so OneConfig links to it.
		context.originalScreen(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.gui.setScreen(new ModListConfigScreen(minecraft.gui.screen()));
		});

		// OneConfig identifies a mod's page by its config tree, whose id comes from the config file name.
		OneConfigBridge.register(() -> config, () -> OneConfigUI.open(
				new OneConfigDestination.ModConfig(new ModConfigTarget(config.getTree().getID(), null))));
	}
}
