package com.urntt.modlist.config;

import com.google.gson.annotations.SerializedName;
import net.minecraft.network.chat.Component;

/**
 * Decides what a profile reports to the server about the client's mods. The one-click buttons on the profile screen
 * switch between the first three; editing the profile's own brand, hidden namespaces or fake channels selects
 * {@link #CUSTOM}.
 */
public enum ReportMode {
	/** Report the real brand and channels. */
	@SerializedName("actual")
	ACTUAL("actual"),
	/** Look like an unmodded client: brand {@code vanilla}, every mod hidden. */
	@SerializedName("vanilla")
	VANILLA("vanilla"),
	/** Claim every channel the server announces and hide the mods the server does not have. */
	@SerializedName("server")
	SERVER("server"),
	/** Use the profile's own brand, hidden namespaces and fake channels. */
	@SerializedName("custom")
	CUSTOM("custom");

	private final String name;

	ReportMode(final String name) {
		this.name = name;
	}

	public Component label() {
		return Component.translatable("options.modlist.report_mode." + this.name);
	}

	public Component description() {
		return Component.translatable("options.modlist.report_mode." + this.name + ".description");
	}
}
