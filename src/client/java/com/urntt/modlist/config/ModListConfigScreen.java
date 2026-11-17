package com.urntt.modlist.config;

import com.urntt.modlist.ModListClient;
import com.urntt.modlist.session.SessionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * The main configuration screen, built from vanilla widgets. Every change is saved immediately.
 */
public final class ModListConfigScreen extends OptionsSubScreen {
	private static final Component TITLE = Component.translatable("options.modlist.title");

	public ModListConfigScreen(final @Nullable Screen parent) {
		super(parent, Minecraft.getInstance().options, TITLE);
	}

	@Override
	protected void addOptions() {
		if (this.list == null) {
			return;
		}
		ModListConfig config = ModListClient.config();

		this.list.addHeader(Component.translatable("options.modlist.section.general"));
		this.list.addBig(OptionInstance.createBoolean("options.modlist.join_notices",
				OptionInstance.cachedConstantTooltip(Component.translatable("options.modlist.join_notices.tooltip")),
				config.joinNotices(), config::setJoinNotices));

		this.list.addHeader(Component.translatable("options.modlist.section.profiles"));
		this.list.addBig(ScreenWidgets.button("options.modlist.default_profile",
				() -> this.minecraft.gui.setScreen(new ProfileScreen(this, config.defaultProfile(), null))));
		this.list.addBig(ScreenWidgets.button("options.modlist.server_profiles",
				() -> this.minecraft.gui.setScreen(new ServerProfilesScreen(this, config))));

		this.list.addHeader(Component.translatable("options.modlist.section.last_connection"));
		Button report = ScreenWidgets.button("options.modlist.server_report",
				() -> this.minecraft.gui.setScreen(new ServerReportScreen(this, config)));
		if (SessionManager.latest() == null) {
			report.active = false;
			report.setTooltip(Tooltip.create(Component.translatable("options.modlist.server_report.none")));
		}
		this.list.addBig(report);
	}
}
