package com.urntt.modlist.config;

/**
 * The settings that do not belong to a server profile. Implemented by the mod's own {@link ModListConfig} and by the
 * OneConfig integration.
 */
public interface ModListSettings {
	/** Whether chat messages summarize the differences between client and server after joining. */
	boolean joinNotices();

	/** The profile for servers that no server profile matches. */
	ProfileSettings defaultProfile();

	/** Whether the mod is on. Only OneConfig's card toggle turns it off; the mod's own settings use the mode. */
	default boolean enabled() {
		return true;
	}
}
