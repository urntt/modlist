package com.urntt.modlist.config;

import com.google.gson.annotations.SerializedName;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * Where the settings come from.
 */
public enum ConfigMode implements StringRepresentable {
	/** The mod changes nothing: no session, no rewritten reports, no ignored registry entries, no join notices. */
	@SerializedName("disabled")
	DISABLED("disabled"),

	/** The mod's own settings apply. */
	@SerializedName("custom")
	CUSTOM("custom"),

	/**
	 * The settings registered with OneConfig apply, except for server profiles, which OneConfig cannot hold. Only
	 * available while OneConfig is installed.
	 */
	@SerializedName("oneconfig")
	ONECONFIG("oneconfig");

	private final String id;

	ConfigMode(final String id) {
		this.id = id;
	}

	@Override
	public String getSerializedName() {
		return this.id;
	}

	public Component label() {
		return Component.translatable(SettingKeys.MODE_PREFIX + this.id);
	}

	public Component description() {
		return Component.translatable(SettingKeys.MODE_PREFIX + this.id + SettingKeys.DESCRIPTION);
	}
}
