package com.urntt.modlist.compat.serverlist;

import com.urntt.modlist.ModListClient;
import com.urntt.modlist.ServerAddresses;
import com.urntt.modlist.config.ModListConfig;
import com.urntt.modlist.config.ProfileScreen;
import com.urntt.modlist.config.ServerProfile;
import com.urntt.modlist.config.SettingKeys;
import com.urntt.serverlist.api.ServerInfoScreenContext;
import com.urntt.serverlist.api.ServerInfoScreenExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * serverlist's {@code serverlist} entrypoint, which adds a button to the Add Server and Edit Server Info screens that
 * opens the server profile of the server being edited. serverlist only calls it when it is installed, so this package
 * is the only place that may reference serverlist classes.
 */
public final class ServerListIntegration implements ServerInfoScreenExtension {
	@Override
	public void init(final ServerInfoScreenContext context) {
		ModListConfig config = ModListClient.config();
		// Vanilla copies the address box into the server data only when the player presses Done, so this is the
		// saved address. A server that is being added has none yet.
		String address = context.serverData().ip.trim();
		Button button = context.addRow(Button.builder(Component.translatable(SettingKeys.SERVERLIST_BUTTON), ignored -> {
			ServerProfile server = config.serverProfileToEdit(address);
			Minecraft.getInstance().gui.setScreen(new ProfileScreen(context.screen(), server.profile(), server));
		}).build());

		if (!config.isActive()) {
			button.active = false;
			button.setTooltip(Tooltip.create(Component.translatable(SettingKeys.INACTIVE)));
		} else if (!ServerAddresses.isValid(address)) {
			button.active = false;
			button.setTooltip(Tooltip.create(Component.translatable(SettingKeys.SERVERLIST_NO_ADDRESS)));
		} else {
			button.setTooltip(Tooltip.create(Component.translatable(SettingKeys.SERVERLIST_BUTTON + SettingKeys.TOOLTIP,
					address)));
		}
	}
}
