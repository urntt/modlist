package com.urntt.modlist.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * Decides what a profile reports to the server about the client's mods. The profile's own brand, hidden namespaces and
 * fake channels only apply in {@link #CUSTOM} mode, and switching modes never clears them.
 */
public enum ReportMode implements StringRepresentable {
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

	public static final Codec<ReportMode> CODEC = StringRepresentable.fromEnum(ReportMode::values);

	private final String name;

	ReportMode(final String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	public Component label() {
		return Component.translatable("options.modlist.report_mode." + this.name);
	}

	public Component description() {
		return Component.translatable("options.modlist.report_mode." + this.name + ".description");
	}
}
