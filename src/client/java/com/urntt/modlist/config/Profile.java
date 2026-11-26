package com.urntt.modlist.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * One set of connection settings. The configuration holds a default profile and any number of server profiles; a
 * connection uses exactly one of them.
 *
 * <p>Instances are serialized by Gson, so field initializers are the defaults, which also apply to keys missing from
 * the file, and {@link ProfileValues#DEFAULTS} is an instance that belongs to no configuration. Every setter notifies
 * the owning {@link ModListConfig}, which saves the file.
 */
public final class Profile implements ProfileSettings {
	private boolean ignoreUnknownRegistryEntries = false;
	/** Placeholder entry per registry, as identifiers. Registries without an entry use the built-in defaults. */
	private Map<String, String> placeholders = new LinkedHashMap<>();
	private ReportMode reportMode = ReportMode.ACTUAL;
	/** Brand reported in {@link ReportMode#CUSTOM} mode; blank keeps the real brand. */
	private String brand = "";
	private List<String> hiddenNamespaces = new ArrayList<>();
	/** Channel namespaces (mirroring the server's channels in them) or full channel identifiers. */
	private List<String> fakeChannels = new ArrayList<>();
	private boolean protectHiddenTranslations = true;

	private transient @Nullable Runnable onChange;

	Profile() {
	}

	/** Returns an independent copy with the same settings and no owner. */
	public Profile copy() {
		return copyOf(this);
	}

	/**
	 * Returns a profile with the values of {@code settings} and no owner, so changing it saves nothing. Values that a
	 * profile does not accept are dropped.
	 */
	public static Profile copyOf(final ProfileSettings settings) {
		Profile copy = new Profile();
		copy.ignoreUnknownRegistryEntries = settings.ignoreUnknownRegistryEntries();
		copy.placeholders = new LinkedHashMap<>(settings.placeholders());
		copy.reportMode = settings.reportMode();
		copy.brand = settings.brand();
		copy.hiddenNamespaces = new ArrayList<>(settings.hiddenNamespaces());
		copy.fakeChannels = new ArrayList<>(settings.fakeChannels());
		copy.protectHiddenTranslations = settings.protectHiddenTranslations();
		copy.normalize();
		return copy;
	}

	void attach(final Runnable onChange) {
		this.onChange = onChange;
	}

	@Override
	public boolean ignoreUnknownRegistryEntries() {
		return this.ignoreUnknownRegistryEntries;
	}

	public void setIgnoreUnknownRegistryEntries(final boolean ignoreUnknownRegistryEntries) {
		this.ignoreUnknownRegistryEntries = ignoreUnknownRegistryEntries;
		this.changed();
	}

	@Override
	public Map<String, String> placeholders() {
		return Map.copyOf(this.placeholders);
	}

	public void setPlaceholders(final Map<String, String> placeholders) {
		this.placeholders = new LinkedHashMap<>(placeholders);
		this.normalize();
		this.changed();
	}

	@Override
	public ReportMode reportMode() {
		return this.reportMode;
	}

	public void setReportMode(final ReportMode reportMode) {
		this.reportMode = reportMode;
		this.changed();
	}

	@Override
	public String brand() {
		return this.brand;
	}

	/** Sets the brand reported in {@link ReportMode#CUSTOM} mode. */
	public void setBrand(final String brand) {
		this.brand = ProfileValues.brand(brand);
		this.changed();
	}

	@Override
	public List<String> hiddenNamespaces() {
		return List.copyOf(this.hiddenNamespaces);
	}

	/** Sets the namespaces hidden in {@link ReportMode#CUSTOM} mode. */
	public void setHiddenNamespaces(final List<String> hiddenNamespaces) {
		this.hiddenNamespaces = new ArrayList<>(hiddenNamespaces);
		this.normalize();
		this.changed();
	}

	@Override
	public List<String> fakeChannels() {
		return List.copyOf(this.fakeChannels);
	}

	/** Sets the fake channel entries used in {@link ReportMode#CUSTOM} mode. */
	public void setFakeChannels(final List<String> fakeChannels) {
		this.fakeChannels = new ArrayList<>(fakeChannels);
		this.normalize();
		this.changed();
	}

	@Override
	public boolean protectHiddenTranslations() {
		return this.protectHiddenTranslations;
	}

	public void setProtectHiddenTranslations(final boolean protectHiddenTranslations) {
		this.protectHiddenTranslations = protectHiddenTranslations;
		this.changed();
	}

	private void changed() {
		if (this.onChange != null) {
			this.onChange.run();
		}
	}

	/**
	 * Replaces values Gson could not map (unknown mode names, missing lists) and drops blank, duplicate or invalid
	 * entries.
	 */
	void normalize() {
		if (this.reportMode == null) {
			this.reportMode = ReportMode.ACTUAL;
		}
		this.brand = ProfileValues.brand(this.brand);
		this.hiddenNamespaces = ProfileValues.hiddenNamespaces(this.hiddenNamespaces);
		this.fakeChannels = ProfileValues.fakeChannels(this.fakeChannels);
		this.placeholders = ProfileValues.placeholders(this.placeholders);
	}
}
