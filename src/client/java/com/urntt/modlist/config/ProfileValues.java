package com.urntt.modlist.config;

import com.urntt.modlist.session.ChannelEntries;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * The defaults of a profile and the rules for its values, shared by {@link Profile} and the OneConfig integration so
 * both accept the same values.
 */
public final class ProfileValues {
	/** A profile with every setting at its default. It belongs to no configuration, so it is never saved. */
	public static final ProfileSettings DEFAULTS = new Profile();

	private ProfileValues() {
	}

	/** The brand without surrounding whitespace; {@code null} becomes blank, which keeps the real brand. */
	public static String brand(final @Nullable String brand) {
		return brand == null ? "" : brand.trim();
	}

	/** The namespaces that can be hidden, trimmed, without blanks and duplicates, in their original order. */
	public static List<String> hiddenNamespaces(final @Nullable Collection<String> namespaces) {
		return clean(namespaces, ChannelEntries::isHideable);
	}

	/** The valid fake channel entries, trimmed, without blanks and duplicates, in their original order. */
	public static List<String> fakeChannels(final @Nullable Collection<String> entries) {
		return clean(entries, ChannelEntries::isValidFakeEntry);
	}

	/**
	 * The placeholder overrides whose registry and entry are both identifiers, trimmed. A blank entry means the built-in
	 * placeholder, so it is dropped, although an empty string parses as an identifier.
	 */
	public static Map<String, String> placeholders(final @Nullable Map<String, String> placeholders) {
		Map<String, String> cleaned = new LinkedHashMap<>();
		if (placeholders != null) {
			placeholders.forEach((registry, entry) -> {
				if (isIdentifier(registry) && isIdentifier(entry)) {
					cleaned.put(registry.trim(), entry.trim());
				}
			});
		}
		return cleaned;
	}

	private static boolean isIdentifier(final @Nullable String text) {
		return text != null && !text.isBlank() && Identifier.tryParse(text.trim()) != null;
	}

	private static List<String> clean(final @Nullable Collection<String> entries, final Predicate<String> valid) {
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
