package com.urntt.modlist.config;

import com.urntt.modlist.ServerAddresses;
import com.urntt.modlist.session.ChannelEntries;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Edits one profile: the default profile, or a server profile together with its address. Every change is saved
 * immediately.
 */
public final class ProfileScreen extends OptionsSubScreen {
	/** Longest address the address box accepts; long enough for any host name with a port. */
	private static final int MAX_ADDRESS_LENGTH = 261;
	/** Longest brand the brand box accepts. */
	private static final int MAX_BRAND_LENGTH = 128;

	private final Profile profile;
	private final @Nullable ServerProfile server;
	private @Nullable StringWidget modeStatus;
	private @Nullable Button mimicVanilla;
	private @Nullable Button mimicServer;
	private @Nullable Button restoreCurrent;

	public ProfileScreen(final Screen parent, final Profile profile, final @Nullable ServerProfile server) {
		super(parent, Minecraft.getInstance().options, title(server));
		this.profile = profile;
		this.server = server;
	}

	private static Component title(final @Nullable ServerProfile server) {
		return server == null
				? Component.translatable("options.modlist.profile.default_title")
				: Component.translatable("options.modlist.profile.server_title");
	}

	@Override
	protected void addOptions() {
		if (this.list == null) {
			return;
		}

		if (this.server != null) {
			this.list.addHeader(Component.translatable("options.modlist.section.server"));
			this.list.addBig(this.addressBox(this.server));
		}

		this.list.addHeader(Component.translatable("options.modlist.section.joining"));
		this.list.addBig(toggle("options.modlist.ignore_unknown_registry_entries",
				this.profile.ignoreUnknownRegistryEntries(), this.profile::setIgnoreUnknownRegistryEntries));
		this.list.addBig(ScreenWidgets.button("options.modlist.placeholders",
				() -> this.minecraft.gui.setScreen(new PlaceholdersScreen(this, this.profile))));

		this.list.addHeader(Component.translatable("options.modlist.section.reporting"));
		this.modeStatus = new StringWidget(Component.empty(), this.font);
		this.list.addBig(this.modeStatus);
		this.mimicVanilla = ScreenWidgets.button("options.modlist.mimic_vanilla",
				() -> this.setReportMode(ReportMode.VANILLA));
		this.mimicServer = ScreenWidgets.button("options.modlist.mimic_server",
				() -> this.setReportMode(ReportMode.SERVER));
		this.list.addSmall(this.mimicVanilla, this.mimicServer);
		this.restoreCurrent = ScreenWidgets.button("options.modlist.restore_current",
				() -> this.setReportMode(ReportMode.ACTUAL));
		this.list.addBig(this.restoreCurrent);
		this.list.addBig(toggle("options.modlist.protect_hidden_translations",
				this.profile.protectHiddenTranslations(), this.profile::setProtectHiddenTranslations));

		this.list.addHeader(Component.translatable("options.modlist.section.custom"));
		this.list.addBig(this.brandBox());
		this.list.addBig(ScreenWidgets.button("options.modlist.hidden_mods",
				() -> this.minecraft.gui.setScreen(new HiddenModsScreen(this, this.profile))));
		this.list.addBig(ScreenWidgets.button("options.modlist.fake_mods",
				() -> this.minecraft.gui.setScreen(new StringListScreen(this,
						Component.translatable("options.modlist.fake_mods.title"),
						Component.translatable("options.modlist.fake_mods.description"),
						Component.translatable("options.modlist.fake_mods.hint"),
						Component.translatable("options.modlist.fake_mods.entry"),
						this.profile.fakeChannels(), ChannelEntries::isValidFakeEntry, this.profile::setFakeChannels))));

		this.updateReportMode();
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
		EditBox box = new EditBox(this.font, 310, 20, Component.translatable("options.modlist.brand"));
		box.setMaxLength(MAX_BRAND_LENGTH);
		box.setHint(Component.translatable("options.modlist.brand.hint").withStyle(EditBox.SEARCH_HINT_STYLE));
		box.setTooltip(Tooltip.create(Component.translatable("options.modlist.brand.tooltip")));
		box.setValue(this.profile.brand());
		box.setResponder(text -> {
			this.profile.setBrand(text);
			this.updateReportMode();
		});
		return box;
	}

	private void setReportMode(final ReportMode mode) {
		this.profile.setReportMode(mode);
		this.updateReportMode();
	}

	/** Shows the current report mode and disables the button that would select it again. */
	private void updateReportMode() {
		ReportMode mode = this.profile.reportMode();
		if (this.modeStatus != null) {
			this.modeStatus.setMessage(Component.translatable("options.modlist.report_mode.current", mode.label()));
			this.modeStatus.setTooltip(Tooltip.create(mode.description()));
		}
		setActive(this.mimicVanilla, mode != ReportMode.VANILLA);
		setActive(this.mimicServer, mode != ReportMode.SERVER);
		setActive(this.restoreCurrent, mode != ReportMode.ACTUAL);
	}

	private static void setActive(final @Nullable Button button, final boolean active) {
		if (button != null) {
			button.active = active;
		}
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
