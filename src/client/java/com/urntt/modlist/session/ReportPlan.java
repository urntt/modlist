package com.urntt.modlist.session;

import com.urntt.modlist.config.Profile;
import com.urntt.modlist.config.ReportMode;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * What a profile reports to the server: the brand, the channel namespaces to hide and the channels to fake. Hidden
 * namespaces and fake channels can depend on the channels the server has announced so far, which callers pass in.
 */
public final class ReportPlan {
	/** The brand vanilla clients report. */
	public static final String VANILLA_BRAND = "vanilla";

	private final ReportMode mode;
	private final String customBrand;
	private final Set<String> customHidden;
	private final List<String> customFakes;

	private ReportPlan(final ReportMode mode, final String customBrand, final Set<String> customHidden,
			final List<String> customFakes) {
		this.mode = mode;
		this.customBrand = customBrand;
		this.customHidden = customHidden;
		this.customFakes = customFakes;
	}

	public static ReportPlan of(final Profile profile) {
		return new ReportPlan(profile.reportMode(), profile.brand(), Set.copyOf(profile.hiddenNamespaces()),
				profile.fakeChannels());
	}

	public ReportMode mode() {
		return this.mode;
	}

	/** The brand to report instead of the real one, or {@code null} to keep the real brand. */
	public @Nullable String brand() {
		return switch (this.mode) {
			case VANILLA -> VANILLA_BRAND;
			case CUSTOM -> this.customBrand.isEmpty() ? null : this.customBrand;
			case ACTUAL, SERVER -> null;
		};
	}

	/** Whether channels, payloads and login answers in {@code namespace} are hidden from the server. */
	public boolean isHidden(final String namespace, final Collection<Identifier> serverChannels) {
		if (!ChannelEntries.isHideable(namespace)) {
			return false;
		}
		return switch (this.mode) {
			case ACTUAL -> false;
			case VANILLA -> true;
			case SERVER -> serverChannels.stream().noneMatch(channel -> channel.getNamespace().equals(namespace));
			case CUSTOM -> this.customHidden.contains(namespace);
		};
	}

	/** The namespaces among {@code namespaces} that are hidden, sorted. */
	public Set<String> hiddenAmong(final Collection<String> namespaces, final Collection<Identifier> serverChannels) {
		Set<String> hidden = new TreeSet<>();
		for (String namespace : namespaces) {
			if (this.isHidden(namespace, serverChannels)) {
				hidden.add(namespace);
			}
		}
		return hidden;
	}

	/** The channels to claim in addition to the real ones. */
	public Set<Identifier> fakeChannels(final Collection<Identifier> serverChannels) {
		Set<Identifier> fakes = new LinkedHashSet<>();
		switch (this.mode) {
			case SERVER -> fakes.addAll(serverChannels);
			case CUSTOM -> {
				for (String entry : this.customFakes) {
					if (entry.indexOf(Identifier.NAMESPACE_SEPARATOR) >= 0) {
						fakes.add(Identifier.parse(entry));
					} else {
						serverChannels.stream().filter(channel -> channel.getNamespace().equals(entry)).forEach(fakes::add);
					}
				}
			}
			case ACTUAL, VANILLA -> {
			}
		}
		return fakes;
	}
}
