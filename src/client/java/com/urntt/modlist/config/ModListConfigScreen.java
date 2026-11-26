package com.urntt.modlist.config;

import com.urntt.modlist.ModListClient;
import com.urntt.modlist.compat.OneConfigBridge;
import com.urntt.modlist.session.SessionManager;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * The main configuration screen, built from vanilla widgets. Every change is saved immediately.
 *
 * <p>The mode button comes first. The settings below it can only be edited in custom mode; otherwise they are greyed
 * out and show the values that currently apply. Server profiles apply in custom and OneConfig mode, so they stay
 * editable in both.
 */
public final class ModListConfigScreen extends ModListOptionsScreen {
	private static final Component TITLE = Component.translatable(SettingKeys.TITLE);

	public ModListConfigScreen(final @Nullable Screen parent) {
		super(parent, TITLE);
	}

	@Override
	protected void addOptions() {
		if (this.list == null) {
			return;
		}
		ModListConfig config = ModListClient.config();
		ConfigMode mode = config.effectiveMode();
		boolean active = config.isActive();
		// Why the settings below the mode button cannot be changed, or null if they can.
		Component lockedReason = !active ? Component.translatable(SettingKeys.INACTIVE)
				: mode == ConfigMode.ONECONFIG ? Component.translatable(SettingKeys.SET_IN_ONECONFIG)
				: null;

		this.list.addBig(this.modeButton(config, mode));
		if (mode == ConfigMode.ONECONFIG) {
			this.list.addBig(ScreenWidgets.button(SettingKeys.OPEN_ONECONFIG, OneConfigBridge::openOneConfig));
		}

		this.section(Component.translatable(SettingKeys.SECTION_GENERAL));
		CycleButton<Boolean> joinNotices = CycleButton.onOffBuilder(config.effectiveJoinNotices())
				.withTooltip(value -> Tooltip.create(withReason(SettingKeys.JOIN_NOTICES, lockedReason)))
				.create(Component.translatable(SettingKeys.JOIN_NOTICES), (button, value) -> config.setJoinNotices(value));
		joinNotices.active = lockedReason == null;
		this.list.addBig(joinNotices);

		this.section(Component.translatable(SettingKeys.SECTION_PROFILES));
		Button defaultProfile = ScreenWidgets.button(SettingKeys.DEFAULT_PROFILE, () -> {
			ModListSettings effective = config.effective();
			if (effective != null && mode == ConfigMode.ONECONFIG) {
				// OneConfig holds the default profile, so show its values without letting them be changed here.
				this.minecraft.gui.setScreen(
						new ProfileScreen(this, Profile.copyOf(effective.defaultProfile()), null, false));
			} else {
				this.minecraft.gui.setScreen(new ProfileScreen(this, config.defaultProfile(), null));
			}
		});
		this.list.addBig(lock(defaultProfile, SettingKeys.DEFAULT_PROFILE, active,
				mode == ConfigMode.ONECONFIG ? Component.translatable(SettingKeys.SET_IN_ONECONFIG) : null));
		Button serverProfiles = ScreenWidgets.button(SettingKeys.SERVER_PROFILES,
				() -> this.minecraft.gui.setScreen(new ServerProfilesScreen(this, config)));
		this.list.addBig(lock(serverProfiles, SettingKeys.SERVER_PROFILES, active, null));

		this.section(Component.translatable(SettingKeys.SECTION_LAST_CONNECTION));
		Button report = ScreenWidgets.button(SettingKeys.SERVER_REPORT,
				() -> this.minecraft.gui.setScreen(new ServerReportScreen(this, config)));
		if (SessionManager.latest() == null) {
			report.active = false;
			report.setTooltip(Tooltip.create(Component.translatable(SettingKeys.SERVER_REPORT + ".none")));
		}
		this.list.addBig(report);
	}

	/** Shows the values that apply now, which OneConfig may have changed while this screen was hidden. */
	@Override
	public void added() {
		super.added();
		this.refreshOptions();
	}

	private CycleButton<ConfigMode> modeButton(final ModListConfig config, final ConfigMode mode) {
		List<ConfigMode> modes = OneConfigBridge.isAvailable()
				? List.of(ConfigMode.values())
				: List.of(ConfigMode.DISABLED, ConfigMode.CUSTOM);
		boolean oneConfigMissing = config.storedMode() == ConfigMode.ONECONFIG && !OneConfigBridge.isAvailable();
		return CycleButton.builder(ConfigMode::label, mode)
				.withValues(modes)
				.withTooltip(value -> Tooltip.create(oneConfigMissing && value == mode
						? Component.translatable(SettingKeys.MODE_ONECONFIG_MISSING)
						: value.description()))
				.create(Component.translatable(SettingKeys.MODE), (button, value) -> {
					config.setMode(value);
					// The settings below change their values and whether they can be edited. The list is still
					// handling this click, so it is rebuilt on the next tick.
					this.refreshOptionsLater();
				});
	}

	/**
	 * Greys out a button that opens a settings screen while the mod is off. A button that still opens a read-only view
	 * keeps working but explains why its settings cannot be changed.
	 */
	private static Button lock(final Button button, final String key, final boolean active,
			final @Nullable Component readOnlyReason) {
		if (!active) {
			button.active = false;
			button.setTooltip(Tooltip.create(withReason(key, Component.translatable(SettingKeys.INACTIVE))));
		} else if (readOnlyReason != null) {
			button.setTooltip(Tooltip.create(withReason(key, readOnlyReason)));
		}
		return button;
	}

	/** The tooltip of the setting {@code key}, followed by {@code reason} if there is one. */
	private static Component withReason(final String key, final @Nullable Component reason) {
		Component tooltip = Component.translatable(key + SettingKeys.TOOLTIP);
		return reason == null ? tooltip : Component.empty().append(tooltip).append(CommonComponents.NEW_LINE)
				.append(CommonComponents.NEW_LINE).append(reason);
	}
}
