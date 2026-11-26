package com.urntt.modlist.config;

import com.urntt.modlist.ServerAddresses;
import com.urntt.modlist.session.ChannelEntries;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Edits one profile: the default profile, or a server profile together with its address. Every change is saved
 * immediately. The custom report settings are only shown in custom mode, and translation protection only when the
 * report mode hides something. A read-only screen shows a profile that is edited elsewhere, such as OneConfig's, with
 * every control greyed out.
 */
public final class ProfileScreen extends ModListOptionsScreen {
	/** Longest address the address box accepts; long enough for any host name with a port. */
	private static final int MAX_ADDRESS_LENGTH = 261;
	/** Longest brand the brand box accepts. */
	private static final int MAX_BRAND_LENGTH = 128;
	/** Width of one of the two widgets in a row, as in vanilla's option lists. */
	private static final int HALF_WIDTH = 150;

	private final Profile profile;
	private final @Nullable ServerProfile server;
	private final boolean editable;

	public ProfileScreen(final Screen parent, final Profile profile, final @Nullable ServerProfile server) {
		this(parent, profile, server, true);
	}

	/**
	 * @param editable whether the controls can change {@code profile}; otherwise they only show its values
	 */
	public ProfileScreen(final Screen parent, final Profile profile, final @Nullable ServerProfile server,
			final boolean editable) {
		super(parent, Component.translatable(server == null
				? "options.modlist.profile.default_title" : "options.modlist.profile.server_title"));
		this.profile = profile;
		this.server = server;
		this.editable = editable;
	}

	@Override
	protected void addOptions() {
		if (this.list == null) {
			return;
		}

		if (this.server != null) {
			this.section(Component.translatable(SettingKeys.SECTION_SERVER));
			this.list.addBig(this.addressBox(this.server));
		}

		this.section(Component.translatable(SettingKeys.SECTION_JOINING));
		this.list.addBig(this.toggle(SettingKeys.IGNORE_UNKNOWN_REGISTRY_ENTRIES,
				this.profile.ignoreUnknownRegistryEntries(), this.profile::setIgnoreUnknownRegistryEntries));
		this.list.addBig(this.editable(ScreenWidgets.button(SettingKeys.PLACEHOLDERS,
				() -> this.minecraft.gui.setScreen(new PlaceholdersScreen(this, this.profile)))));

		this.section(Component.translatable(SettingKeys.SECTION_REPORTING));
		CycleButton<ReportMode> reportMode = CycleButton.builder(ReportMode::label, this.profile.reportMode())
				.withValues(ReportMode.values())
				.withTooltip(mode -> Tooltip.create(mode.description()))
				.create(Component.translatable(SettingKeys.REPORT_MODE), (button, mode) -> {
					this.profile.setReportMode(mode);
					// Which settings follow depends on the mode. The list is still handling this click, so it is
					// rebuilt on the next tick.
					this.refreshOptionsLater();
				});
		this.list.addBig(this.editable(reportMode));

		ReportMode mode = this.profile.reportMode();
		if (mode == ReportMode.CUSTOM) {
			this.list.addSmall(new StringWidget(HALF_WIDTH, 20, Component.translatable(SettingKeys.BRAND), this.font),
					this.brandBox());
			this.list.addBig(this.editable(ScreenWidgets.button(SettingKeys.HIDDEN_MODS,
					() -> this.minecraft.gui.setScreen(new HiddenModsScreen(this, this.profile)))));
			this.list.addBig(this.editable(ScreenWidgets.button(SettingKeys.FAKE_MODS,
					() -> this.minecraft.gui.setScreen(new StringListScreen(this,
							Component.translatable(SettingKeys.FAKE_MODS_TITLE),
							Component.translatable(SettingKeys.FAKE_MODS + SettingKeys.DESCRIPTION),
							Component.translatable(SettingKeys.FAKE_MODS + SettingKeys.HINT),
							Component.translatable(SettingKeys.FAKE_MODS + ".entry"),
							this.profile.fakeChannels(), ChannelEntries::isValidFakeEntry,
							this.profile::setFakeChannels)))));
		}
		if (mode != ReportMode.ACTUAL) {
			this.list.addBig(this.toggle(SettingKeys.PROTECT_HIDDEN_TRANSLATIONS,
					this.profile.protectHiddenTranslations(), this.profile::setProtectHiddenTranslations));
		}
	}

	/** Greys out {@code widget} on a read-only screen. */
	private <T extends AbstractWidget> T editable(final T widget) {
		widget.active = this.editable;
		return widget;
	}

	private EditBox addressBox(final ServerProfile server) {
		EditBox box = new EditBox(this.font, 310, 20, Component.translatable("options.modlist.profile.address"));
		box.setMaxLength(MAX_ADDRESS_LENGTH);
		box.setHint(Component.translatable("options.modlist.profile.address.hint").withStyle(EditBox.SEARCH_HINT_STYLE));
		box.setTooltip(Tooltip.create(Component.translatable("options.modlist.profile.address.tooltip")));
		box.setValue(server.address());
		box.setEditable(this.editable);
		box.setResponder(text -> {
			boolean valid = ServerAddresses.isValid(text);
			box.setTextColor(valid ? EditBox.DEFAULT_TEXT_COLOR : ScreenWidgets.INVALID_TEXT_COLOR);
			if (valid) {
				server.setAddress(text);
			}
		});
		box.setTextColor(ServerAddresses.isValid(server.address()) ? EditBox.DEFAULT_TEXT_COLOR
				: ScreenWidgets.INVALID_TEXT_COLOR);
		return box;
	}

	private EditBox brandBox() {
		EditBox box = new EditBox(this.font, HALF_WIDTH, 20, Component.translatable("options.modlist.brand"));
		box.setMaxLength(MAX_BRAND_LENGTH);
		box.setHint(Component.translatable("options.modlist.brand.hint").withStyle(EditBox.SEARCH_HINT_STYLE));
		box.setTooltip(Tooltip.create(Component.translatable("options.modlist.brand.tooltip")));
		box.setValue(this.profile.brand());
		box.setEditable(this.editable);
		box.setResponder(this.profile::setBrand);
		return box;
	}

	/**
	 * Creates an on/off button whose tooltip is the translation of {@code key + ".tooltip"}, greyed out on a
	 * read-only screen.
	 */
	private CycleButton<Boolean> toggle(final String key, final boolean value, final Consumer<Boolean> onChange) {
		Tooltip tooltip = Tooltip.create(Component.translatable(key + SettingKeys.TOOLTIP));
		return this.editable(CycleButton.onOffBuilder(value)
				.withTooltip(ignored -> tooltip)
				.create(Component.translatable(key), (button, newValue) -> onChange.accept(newValue)));
	}
}
