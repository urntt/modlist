package com.urntt.modlist.config;

import com.urntt.modlist.ServerAddresses;
import com.urntt.modlist.session.ChannelEntries;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Edits one profile: the default profile, or a server profile together with its address. Every change is saved
 * immediately. The custom report settings are only shown in custom mode, and translation protection only when the
 * report mode hides something.
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
	/** Set when the report mode changed, so the options are rebuilt on the next tick, outside the click handling. */
	private boolean pendingRefresh;

	public ProfileScreen(final Screen parent, final Profile profile, final @Nullable ServerProfile server) {
		super(parent, Component.translatable(server == null
				? "options.modlist.profile.default_title" : "options.modlist.profile.server_title"));
		this.profile = profile;
		this.server = server;
	}

	@Override
	protected void addOptions() {
		if (this.list == null) {
			return;
		}

		if (this.server != null) {
			this.section(Component.translatable("options.modlist.section.server"));
			this.list.addBig(this.addressBox(this.server));
		}

		this.section(Component.translatable("options.modlist.section.joining"));
		this.list.addBig(toggle("options.modlist.ignore_unknown_registry_entries",
				this.profile.ignoreUnknownRegistryEntries(), this.profile::setIgnoreUnknownRegistryEntries));
		this.list.addBig(ScreenWidgets.button("options.modlist.placeholders",
				() -> this.minecraft.gui.setScreen(new PlaceholdersScreen(this, this.profile))));

		this.section(Component.translatable("options.modlist.section.reporting"));
		this.list.addBig(new OptionInstance<>(
				"options.modlist.report_mode",
				mode -> Tooltip.create(mode.description()),
				// The button itself prepends the caption, so this only names the value.
				(caption, mode) -> mode.label(),
				new OptionInstance.Enum<>(List.of(ReportMode.values()), ReportMode.CODEC),
				this.profile.reportMode(),
				mode -> {
					this.profile.setReportMode(mode);
					this.pendingRefresh = true;
				}));

		ReportMode mode = this.profile.reportMode();
		if (mode == ReportMode.CUSTOM) {
			this.list.addSmall(new StringWidget(HALF_WIDTH, 20, Component.translatable("options.modlist.brand"), this.font),
					this.brandBox());
			this.list.addBig(ScreenWidgets.button("options.modlist.hidden_mods",
					() -> this.minecraft.gui.setScreen(new HiddenModsScreen(this, this.profile))));
			this.list.addBig(ScreenWidgets.button("options.modlist.fake_mods",
					() -> this.minecraft.gui.setScreen(new StringListScreen(this,
							Component.translatable("options.modlist.fake_mods.title"),
							Component.translatable("options.modlist.fake_mods.description"),
							Component.translatable("options.modlist.fake_mods.hint"),
							Component.translatable("options.modlist.fake_mods.entry"),
							this.profile.fakeChannels(), ChannelEntries::isValidFakeEntry, this.profile::setFakeChannels))));
		}
		if (mode != ReportMode.ACTUAL) {
			this.list.addBig(toggle("options.modlist.protect_hidden_translations",
					this.profile.protectHiddenTranslations(), this.profile::setProtectHiddenTranslations));
		}
	}

	@Override
	public void tick() {
		if (this.pendingRefresh) {
			this.pendingRefresh = false;
			this.refreshOptions();
		}
	}

	private EditBox addressBox(final ServerProfile server) {
		EditBox box = new EditBox(this.font, 310, 20, Component.translatable("options.modlist.profile.address"));
		box.setMaxLength(MAX_ADDRESS_LENGTH);
		box.setHint(Component.translatable("options.modlist.profile.address.hint").withStyle(EditBox.SEARCH_HINT_STYLE));
		box.setTooltip(Tooltip.create(Component.translatable("options.modlist.profile.address.tooltip")));
		box.setValue(server.address());
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
		box.setResponder(this.profile::setBrand);
		return box;
	}

	/**
	 * Creates an on/off option whose tooltip is the translation of {@code captionKey + ".tooltip"}.
	 */
	private static OptionInstance<Boolean> toggle(final String captionKey, final boolean value,
			final Consumer<Boolean> onChange) {
		return OptionInstance.createBoolean(
				captionKey,
				OptionInstance.cachedConstantTooltip(Component.translatable(captionKey + ".tooltip")),
				value,
				onChange::accept);
	}
}
