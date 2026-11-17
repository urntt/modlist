package com.urntt.modlist.session;

import net.minecraft.resources.Identifier;

/**
 * Rules for the namespaces and channel identifiers a profile may hide or fake.
 */
public final class ChannelEntries {
	private ChannelEntries() {
	}

	/**
	 * Whether {@code namespace} may be hidden. Vanilla's own {@code minecraft} namespace never is, since it carries the
	 * brand and the channel registrations themselves.
	 */
	public static boolean isHideable(final String namespace) {
		return !namespace.isEmpty() && Identifier.isValidNamespace(namespace)
				&& !Identifier.DEFAULT_NAMESPACE.equals(namespace);
	}

	/**
	 * Whether {@code entry} is a usable fake channel entry: a namespace, standing for every channel the server
	 * announces in it, or a full channel identifier such as {@code examplemod:handshake}.
	 */
	public static boolean isValidFakeEntry(final String entry) {
		if (entry.indexOf(Identifier.NAMESPACE_SEPARATOR) < 0) {
			return !entry.isEmpty() && Identifier.isValidNamespace(entry);
		}
		Identifier id = Identifier.tryParse(entry);
		return id != null && !id.getPath().isEmpty() && entry.startsWith(id.getNamespace() + Identifier.NAMESPACE_SEPARATOR);
	}
}
