package com.urntt.modlist.config;

/**
 * Translation keys of the settings, shared by the configuration screens, the OneConfig integration and the
 * serverlist integration. They are compile-time constants so that OneConfig's option annotations can use them.
 */
public final class SettingKeys {
	private static final String PREFIX = "options.modlist.";

	/** Suffix of the key that explains a setting, shown as its tooltip. */
	public static final String TOOLTIP = ".tooltip";
	/** Suffix of the key that explains one value of a setting with several values. */
	public static final String DESCRIPTION = ".description";
	/** Suffix of the key of the hint shown in an empty text box. */
	public static final String HINT = ".hint";

	public static final String TITLE = PREFIX + "title";

	public static final String MODE = PREFIX + "mode";
	/** Prefix of the mode value keys; the rest of each key is the mode's id in the config file. */
	public static final String MODE_PREFIX = MODE + ".";
	/** Explains that the file names OneConfig mode while OneConfig is not installed. */
	public static final String MODE_ONECONFIG_MISSING = MODE_PREFIX + "oneconfig.missing";
	public static final String OPEN_ONECONFIG = PREFIX + "open_oneconfig";
	/** Explains why a setting cannot be changed in OneConfig mode. */
	public static final String SET_IN_ONECONFIG = PREFIX + "set_in_oneconfig";
	/** Explains why a setting cannot be changed while the mod is off. */
	public static final String INACTIVE = PREFIX + "inactive";

	public static final String SECTION_GENERAL = PREFIX + "section.general";
	public static final String SECTION_PROFILES = PREFIX + "section.profiles";
	public static final String SECTION_LAST_CONNECTION = PREFIX + "section.last_connection";
	public static final String SECTION_SERVER = PREFIX + "section.server";
	public static final String SECTION_JOINING = PREFIX + "section.joining";
	public static final String SECTION_REPORTING = PREFIX + "section.reporting";

	public static final String JOIN_NOTICES = PREFIX + "join_notices";
	public static final String DEFAULT_PROFILE = PREFIX + "default_profile";
	public static final String SERVER_PROFILES = PREFIX + "server_profiles";
	public static final String SERVER_PROFILES_TITLE = SERVER_PROFILES + ".title";
	public static final String SERVER_PROFILES_EDIT = SERVER_PROFILES + ".edit";
	public static final String SERVER_REPORT = PREFIX + "server_report";

	public static final String IGNORE_UNKNOWN_REGISTRY_ENTRIES = PREFIX + "ignore_unknown_registry_entries";
	public static final String PLACEHOLDERS = PREFIX + "placeholders";
	public static final String PLACEHOLDERS_TITLE = PLACEHOLDERS + ".title";
	/** Prefix of the placeholder keys; the rest of each key is the registry's path, such as {@code block}. */
	public static final String PLACEHOLDER_PREFIX = PLACEHOLDERS + ".";
	public static final String PLACEHOLDER_BLOCK = PLACEHOLDER_PREFIX + "block";
	public static final String PLACEHOLDER_ITEM = PLACEHOLDER_PREFIX + "item";
	public static final String PLACEHOLDER_ENTITY_TYPE = PLACEHOLDER_PREFIX + "entity_type";

	public static final String REPORT_MODE = PREFIX + "report_mode";
	/** Prefix of the report mode value keys; the rest of each key is the mode's id in the config file. */
	public static final String REPORT_MODE_PREFIX = REPORT_MODE + ".";
	public static final String REPORT_MODE_ACTUAL = REPORT_MODE_PREFIX + "actual";
	public static final String REPORT_MODE_VANILLA = REPORT_MODE_PREFIX + "vanilla";
	public static final String REPORT_MODE_SERVER = REPORT_MODE_PREFIX + "server";
	public static final String REPORT_MODE_CUSTOM = REPORT_MODE_PREFIX + "custom";
	public static final String BRAND = PREFIX + "brand";
	public static final String HIDDEN_MODS = PREFIX + "hidden_mods";
	public static final String HIDDEN_MODS_TITLE = HIDDEN_MODS + ".title";
	public static final String FAKE_MODS = PREFIX + "fake_mods";
	public static final String FAKE_MODS_TITLE = FAKE_MODS + ".title";
	public static final String PROTECT_HIDDEN_TRANSLATIONS = PREFIX + "protect_hidden_translations";
	public static final String LIST_ADD = PREFIX + "list.add";

	/** The button that the serverlist integration adds to the screen that adds or edits a server. */
	public static final String SERVERLIST_BUTTON = PREFIX + "serverlist.button";
	/** Explains why that button is inactive while the server has no address yet. */
	public static final String SERVERLIST_NO_ADDRESS = PREFIX + "serverlist.no_address";

	private SettingKeys() {
	}
}
