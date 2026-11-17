package com.urntt.modlist.session;

import java.util.Set;
import java.util.TreeSet;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.Identifier;

/**
 * The channels the client really receives on, which is what Fabric API reports to servers.
 */
public final class LocalChannels {
	private LocalChannels() {
	}

	/** Every channel with a global receiver in the login, configuration or play phase, sorted. */
	public static Set<Identifier> all() {
		Set<Identifier> channels = new TreeSet<>();
		channels.addAll(ClientLoginNetworking.getGlobalReceivers());
		channels.addAll(ClientConfigurationNetworking.getGlobalReceivers());
		channels.addAll(ClientPlayNetworking.getGlobalReceivers());
		return channels;
	}

	/** The namespaces of {@link #all()}, sorted. */
	public static Set<String> namespaces() {
		Set<String> namespaces = new TreeSet<>();
		for (Identifier channel : all()) {
			namespaces.add(channel.getNamespace());
		}
		return namespaces;
	}
}
