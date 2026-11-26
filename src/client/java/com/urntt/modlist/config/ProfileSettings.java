package com.urntt.modlist.config;

import java.util.List;
import java.util.Map;

/**
 * The values a connection reads from a profile. Implemented by the mod's own {@link Profile}s and by the OneConfig
 * integration.
 */
public interface ProfileSettings {
	/** Whether registry entries and registries the client does not have are dropped instead of disconnecting. */
	boolean ignoreUnknownRegistryEntries();

	/** Placeholder overrides, from registry identifier to entry identifier. */
	Map<String, String> placeholders();

	ReportMode reportMode();

	/** The brand reported in {@link ReportMode#CUSTOM} mode; blank keeps the real brand. */
	String brand();

	/** The channel namespaces hidden in {@link ReportMode#CUSTOM} mode. */
	List<String> hiddenNamespaces();

	/** The fake channel entries used in {@link ReportMode#CUSTOM} mode. */
	List<String> fakeChannels();

	/** Whether sign editing and anvil renaming treat hidden mods' translation keys as unknown. */
	boolean protectHiddenTranslations();
}
