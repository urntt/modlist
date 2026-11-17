package com.urntt.modlist.session;

import com.urntt.modlist.registry.RegistrySyncFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Chat messages shown after joining a server that summarize how the client and the server differ. They are only
 * informational; nothing here blocks joining.
 */
public final class JoinNotices {
	/** Namespaces every Fabric client and server share, which say nothing about mods. */
	private static final Set<String> SHARED_NAMESPACES = Set.of(Identifier.DEFAULT_NAMESPACE, "c");
	/** Number of namespaces listed in a message before the rest are only counted. */
	private static final int LISTED = 6;
	/** Number of ignored entries listed per registry in the hover text. */
	private static final int LISTED_ENTRIES = 8;

	private JoinNotices() {
	}

	/**
	 * Namespaces the server uses that this client lacks: channel namespaces that are neither a local channel namespace
	 * nor an installed mod, and the namespaces of ignored registry content.
	 */
	public static Set<String> serverOnly(final Collection<Identifier> serverChannels,
			final Collection<String> localNamespaces, final Collection<String> installedMods,
			final RegistrySyncFilter.@Nullable Result ignored) {
		Set<String> result = new TreeSet<>();
		for (Identifier channel : serverChannels) {
			result.add(channel.getNamespace());
		}
		result.removeAll(localNamespaces);
		result.removeAll(installedMods);
		if (ignored != null) {
			ignored.droppedEntries().values().forEach(entries -> entries.forEach(entry -> result.add(entry.getNamespace())));
			ignored.droppedRegistries().forEach(registry -> result.add(registry.getNamespace()));
		}
		result.removeAll(SHARED_NAMESPACES);
		return result;
	}

	/** Local channel namespaces the server has announced no channel in. */
	public static Set<String> clientOnly(final Collection<String> localNamespaces,
			final Collection<Identifier> serverChannels) {
		Set<String> result = new TreeSet<>(localNamespaces);
		for (Identifier channel : serverChannels) {
			result.remove(channel.getNamespace());
		}
		result.removeAll(SHARED_NAMESPACES);
		return result;
	}

	/** Builds the messages for {@code session}; empty when client and server match. */
	public static List<Component> messages(final ConnectionSession session, final Collection<String> localNamespaces,
			final Collection<String> installedMods) {
		List<Component> messages = new ArrayList<>();
		RegistrySyncFilter.Result ignored = session.ignoredRegistryContent();
		if (ignored != null) {
			Set<String> namespaces = new TreeSet<>();
			ignored.droppedEntries().values().forEach(entries -> entries.forEach(entry -> namespaces.add(entry.getNamespace())));
			ignored.droppedRegistries().forEach(registry -> namespaces.add(registry.getNamespace()));
			messages.add(Component.translatable("message.modlist.ignored_entries",
					ignored.droppedEntryCount() + ignored.droppedRegistries().size(), list(namespaces))
					.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(ignoredDetails(ignored)))));
		}

		Set<String> serverOnly = serverOnly(session.serverChannels(), localNamespaces, installedMods, ignored);
		if (!serverOnly.isEmpty()) {
			messages.add(Component.translatable("message.modlist.server_only", list(serverOnly))
					.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.literal(String.join("\n", serverOnly))))));
		}

		Set<String> clientOnly = clientOnly(localNamespaces, session.serverChannels());
		if (!clientOnly.isEmpty()) {
			messages.add(Component.translatable("message.modlist.client_only", list(clientOnly))
					.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.literal(String.join("\n", clientOnly))))));
		}
		return messages;
	}

	/** Adds the messages for {@code session} to the chat. */
	public static void show(final ConnectionSession session, final Minecraft minecraft,
			final Collection<String> installedMods) {
		for (Component message : messages(session, LocalChannels.namespaces(), installedMods)) {
			minecraft.gui.hud.getChat().addClientSystemMessage(Component.empty()
					.append(Component.translatable("message.modlist.prefix").withStyle(ChatFormatting.GRAY))
					.append(CommonComponents.SPACE)
					.append(message));
		}
	}

	/** Lists namespaces, naming the first few and counting the rest. */
	private static Component list(final Collection<String> namespaces) {
		List<String> named = namespaces.stream().limit(LISTED).toList();
		MutableComponent result = Component.literal(String.join(", ", named)).withStyle(ChatFormatting.YELLOW);
		if (namespaces.size() > named.size()) {
			result = Component.translatable("message.modlist.and_more", result, namespaces.size() - named.size());
		}
		return result;
	}

	private static Component ignoredDetails(final RegistrySyncFilter.Result ignored) {
		MutableComponent details = Component.empty();
		boolean first = true;
		for (Map.Entry<Identifier, List<Identifier>> registry : ignored.droppedEntries().entrySet()) {
			if (!first) {
				details.append(CommonComponents.NEW_LINE);
			}
			first = false;
			List<Identifier> entries = registry.getValue();
			List<String> named = entries.stream().limit(LISTED_ENTRIES).map(Identifier::toString).toList();
			details.append(Component.literal(registry.getKey() + ": ").withStyle(ChatFormatting.YELLOW))
					.append(String.join(", ", named));
			if (entries.size() > named.size()) {
				details.append(CommonComponents.SPACE)
						.append(Component.translatable("message.modlist.more", entries.size() - named.size()));
			}
		}
		for (Identifier registry : ignored.droppedRegistries()) {
			if (!first) {
				details.append(CommonComponents.NEW_LINE);
			}
			first = false;
			details.append(Component.translatable("message.modlist.unknown_registry", registry.toString())
					.withStyle(ChatFormatting.YELLOW));
		}
		return details;
	}
}
