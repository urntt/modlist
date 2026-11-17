package com.urntt.modlist.config;

import com.urntt.modlist.session.ChannelEntries;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * One set of connection settings. The configuration holds a default profile and any number of server profiles; a
 * connection uses exactly one of them.
 *
 * <p>Instances are serialized by Gson, so field initializers are the defaults, which also apply to keys missing from
 * the file. Every setter notifies the owning {@link ModListConfig}, which saves the file.
 */
public final class Profile {
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
		Profile copy = new Profile();
		copy.ignoreUnknownRegistryEntries = this.ignoreUnknownRegistryEntries;
		copy.placeholders = new LinkedHashMap<>(this.placeholders);
		copy.reportMode = this.reportMode;
		copy.brand = this.brand;
		copy.hiddenNamespaces = new ArrayList<>(this.hiddenNamespaces);
		copy.fakeChannels = new ArrayList<>(this.fakeChannels);
		copy.protectHiddenTranslations = this.protectHiddenTranslations;
		return copy;
	}

	void attach(final Runnable onChange) {
		this.onChange = onChange;
	}

	public boolean ignoreUnknownRegistryEntries() {
		return this.ignoreUnknownRegistryEntries;
	}

	public void setIgnoreUnknownRegistryEntries(final boolean ignoreUnknownRegistryEntries) {
		this.ignoreUnknownRegistryEntries = ignoreUnknownRegistryEntries;
		this.changed();
	}

	/** Placeholder overrides, from registry identifier to entry identifier. */
	public Map<String, String> placeholders() {
		return Map.copyOf(this.placeholders);
	}

	public void setPlaceholders(final Map<String, String> placeholders) {
		this.placeholders = new LinkedHashMap<>(placeholders);
		this.normalize();
		this.changed();
	}

	public ReportMode reportMode() {
		return this.reportMode;
	}

	public void setReportMode(final ReportMode reportMode) {
		this.reportMode = reportMode;
		this.changed();
	}

	public String brand() {
		return this.brand;
	}

	/** Sets the brand reported in {@link ReportMode#CUSTOM} mode. */
	public void setBrand(final String brand) {
		this.brand = brand.trim();
		this.changed();
	}

	public List<String> hiddenNamespaces() {
		return List.copyOf(this.hiddenNamespaces);
	}

	/** Sets the namespaces hidden in {@link ReportMode#CUSTOM} mode. */
	public void setHiddenNamespaces(final List<String> hiddenNamespaces) {
		this.hiddenNamespaces = new ArrayList<>(hiddenNamespaces);
		this.normalize();
		this.changed();
	}

	public List<String> fakeChannels() {
		return List.copyOf(this.fakeChannels);
	}

	/** Sets the fake channel entries used in {@link ReportMode#CUSTOM} mode. */
	public void setFakeChannels(final List<String> fakeChannels) {
		this.fakeChannels = new ArrayList<>(fakeChannels);
		this.normalize();
		this.changed();
	}

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
		this.brand = this.brand == null ? "" : this.brand.trim();
		this.hiddenNamespaces = clean(this.hiddenNamespaces, ChannelEntries::isHideable);
		this.fakeChannels = clean(this.fakeChannels, ChannelEntries::isValidFakeEntry);

		Map<String, String> cleaned = new LinkedHashMap<>();
		if (this.placeholders != null) {
			this.placeholders.forEach((registry, entry) -> {
				if (registry != null && entry != null && Identifier.tryParse(registry.trim()) != null
						&& Identifier.tryParse(entry.trim()) != null) {
					cleaned.put(registry.trim(), entry.trim());
				}
			});
		}
		this.placeholders = cleaned;
	}

	private static List<String> clean(final @Nullable List<String> entries,
			final Predicate<String> valid) {
		LinkedHashSet<String> result = new LinkedHashSet<>();
		if (entries != null) {
			for (String entry : entries) {
				if (entry != null && valid.test(entry.trim())) {
					result.add(entry.trim());
				}
			}
		}
		return new ArrayList<>(result);
	}
}
