package com.urntt.modlist.compat.oneconfig;

import com.urntt.modlist.ModListClient;
import com.urntt.modlist.config.ModListSettings;
import com.urntt.modlist.config.ProfileSettings;
import com.urntt.modlist.config.ProfileValues;
import com.urntt.modlist.config.ReportMode;
import com.urntt.modlist.config.ServerProfilesScreen;
import com.urntt.modlist.config.SettingKeys;
import com.urntt.modlist.registry.Placeholders;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.annotations.Button;
import org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown;
import org.polyfrost.oneconfig.api.config.v1.annotations.MasterToggle;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;
import org.polyfrost.oneconfig.api.config.v1.annotations.Text;
import org.polyfrost.oneconfig.api.config.v1.annotations.TextList;

/**
 * The settings OneConfig manages in OneConfig mode: the general settings and the default profile, with the same
 * defaults as the mod's own settings. OneConfig cannot hold a list of server profiles, so those stay in the mod's own
 * settings, which a button opens.
 *
 * <p>OneConfig writes the fields directly, so the getters drop values that the mod's own profiles do not accept.
 * Titles use the mod's translation keys; the keys double as fallback titles.
 */
public final class ModListOneConfig extends Config implements ModListSettings, ProfileSettings {
	/**
	 * OneConfig keeps its files in the same config directory as the mod's own {@code modlist.json} and watches them
	 * for changes, so its file needs a name of its own.
	 */
	private static final String FILE_NAME = ModListClient.MOD_ID + "-oneconfig.json";

	/** The mod icon, as named in {@code fabric.mod.json}, as a resource path for OneConfig's mod card. */
	private static final String ICON_PATH = "/assets/" + ModListClient.MOD_ID + "/icon.png";

	/** An identifier, with or without a namespace, or nothing to use the built-in default. */
	private static final String IDENTIFIER_REGEX = "^(?:[a-z0-9_.-]+:)?[a-z0-9_./-]*$";
	private static final String NAMESPACE_REGEX = "^[a-z0-9_.-]+$";
	private static final String FAKE_CHANNEL_REGEX = "^[a-z0-9_.-]+(?::[a-z0-9_./-]+)?$";

	/** The toggle on the mod's OneConfig card. While it is off, the mod changes nothing, as in disabled mode. */
	@MasterToggle
	public boolean enabled = true;

	@Switch(
			title = SettingKeys.JOIN_NOTICES,
			description = SettingKeys.JOIN_NOTICES + SettingKeys.TOOLTIP,
			subcategory = SettingKeys.SECTION_GENERAL)
	public boolean joinNotices = true;

	@Button(
			title = SettingKeys.SERVER_PROFILES_TITLE,
			description = SettingKeys.SERVER_PROFILES + SettingKeys.TOOLTIP,
			text = SettingKeys.SERVER_PROFILES_EDIT,
			subcategory = SettingKeys.SECTION_PROFILES)
	public void openServerProfiles() {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.gui.setScreen(new ServerProfilesScreen(minecraft.gui.screen(), ModListClient.config()));
	}

	@Switch(
			title = SettingKeys.IGNORE_UNKNOWN_REGISTRY_ENTRIES,
			description = SettingKeys.IGNORE_UNKNOWN_REGISTRY_ENTRIES + SettingKeys.TOOLTIP,
			subcategory = SettingKeys.SECTION_JOINING)
	public boolean ignoreUnknownRegistryEntries = ProfileValues.DEFAULTS.ignoreUnknownRegistryEntries();

	@Text(
			title = SettingKeys.PLACEHOLDER_BLOCK,
			description = SettingKeys.PLACEHOLDER_BLOCK + SettingKeys.TOOLTIP,
			placeholder = Placeholders.DEFAULT_BLOCK,
			regex = IDENTIFIER_REGEX,
			subcategory = SettingKeys.PLACEHOLDERS_TITLE)
	public String blockPlaceholder = "";

	@Text(
			title = SettingKeys.PLACEHOLDER_ITEM,
			description = SettingKeys.PLACEHOLDER_ITEM + SettingKeys.TOOLTIP,
			placeholder = Placeholders.DEFAULT_ITEM,
			regex = IDENTIFIER_REGEX,
			subcategory = SettingKeys.PLACEHOLDERS_TITLE)
	public String itemPlaceholder = "";

	@Text(
			title = SettingKeys.PLACEHOLDER_ENTITY_TYPE,
			description = SettingKeys.PLACEHOLDER_ENTITY_TYPE + SettingKeys.TOOLTIP,
			placeholder = Placeholders.DEFAULT_ENTITY_TYPE,
			regex = IDENTIFIER_REGEX,
			subcategory = SettingKeys.PLACEHOLDERS_TITLE)
	public String entityTypePlaceholder = "";

	/** The index of the report mode in {@link ReportMode#values()}, the order of the options below. */
	@Dropdown(
			title = SettingKeys.REPORT_MODE,
			description = SettingKeys.REPORT_MODE + SettingKeys.TOOLTIP,
			options = {SettingKeys.REPORT_MODE_ACTUAL, SettingKeys.REPORT_MODE_VANILLA, SettingKeys.REPORT_MODE_SERVER,
					SettingKeys.REPORT_MODE_CUSTOM},
			subcategory = SettingKeys.SECTION_REPORTING)
	public int reportMode = ProfileValues.DEFAULTS.reportMode().ordinal();

	@Text(
			title = SettingKeys.BRAND,
			description = SettingKeys.BRAND + SettingKeys.TOOLTIP,
			placeholder = SettingKeys.BRAND + SettingKeys.HINT,
			subcategory = SettingKeys.SECTION_REPORTING)
	public String brand = ProfileValues.DEFAULTS.brand();

	@TextList(
			title = SettingKeys.HIDDEN_MODS_TITLE,
			description = SettingKeys.HIDDEN_MODS + SettingKeys.TOOLTIP,
			placeholder = SettingKeys.HIDDEN_MODS + SettingKeys.HINT,
			addText = SettingKeys.LIST_ADD,
			regex = NAMESPACE_REGEX,
			subcategory = SettingKeys.SECTION_REPORTING)
	public List<String> hiddenNamespaces = new ArrayList<>(ProfileValues.DEFAULTS.hiddenNamespaces());

	@TextList(
			title = SettingKeys.FAKE_MODS_TITLE,
			description = SettingKeys.FAKE_MODS + SettingKeys.TOOLTIP,
			placeholder = SettingKeys.FAKE_MODS + SettingKeys.HINT,
			addText = SettingKeys.LIST_ADD,
			regex = FAKE_CHANNEL_REGEX,
			subcategory = SettingKeys.SECTION_REPORTING)
	public List<String> fakeChannels = new ArrayList<>(ProfileValues.DEFAULTS.fakeChannels());

	@Switch(
			title = SettingKeys.PROTECT_HIDDEN_TRANSLATIONS,
			description = SettingKeys.PROTECT_HIDDEN_TRANSLATIONS + SettingKeys.TOOLTIP,
			subcategory = SettingKeys.SECTION_REPORTING)
	public boolean protectHiddenTranslations = ProfileValues.DEFAULTS.protectHiddenTranslations();

	public ModListOneConfig() {
		super(FILE_NAME, ICON_PATH, ModListClient.MOD_ID, Category.QOL);
	}

	@Override
	public boolean enabled() {
		return this.enabled;
	}

	@Override
	public boolean joinNotices() {
		return this.joinNotices;
	}

	/** OneConfig holds one profile, which applies to every server without a server profile. */
	@Override
	public ProfileSettings defaultProfile() {
		return this;
	}

	@Override
	public boolean ignoreUnknownRegistryEntries() {
		return this.ignoreUnknownRegistryEntries;
	}

	@Override
	public Map<String, String> placeholders() {
		Map<String, String> placeholders = new LinkedHashMap<>();
		placeholders.put(Registries.BLOCK.identifier().toString(), this.blockPlaceholder);
		placeholders.put(Registries.ITEM.identifier().toString(), this.itemPlaceholder);
		placeholders.put(Registries.ENTITY_TYPE.identifier().toString(), this.entityTypePlaceholder);
		return Map.copyOf(ProfileValues.placeholders(placeholders));
	}

	/** An index OneConfig's file holds out of range falls back to the default report mode. */
	@Override
	public ReportMode reportMode() {
		ReportMode[] modes = ReportMode.values();
		return this.reportMode >= 0 && this.reportMode < modes.length
				? modes[this.reportMode]
				: ProfileValues.DEFAULTS.reportMode();
	}

	@Override
	public String brand() {
		return ProfileValues.brand(this.brand);
	}

	@Override
	public List<String> hiddenNamespaces() {
		return List.copyOf(ProfileValues.hiddenNamespaces(this.hiddenNamespaces));
	}

	@Override
	public List<String> fakeChannels() {
		return List.copyOf(ProfileValues.fakeChannels(this.fakeChannels));
	}

	@Override
	public boolean protectHiddenTranslations() {
		return this.protectHiddenTranslations;
	}
}
