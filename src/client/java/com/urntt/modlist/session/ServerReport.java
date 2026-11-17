package com.urntt.modlist.session;

import com.urntt.modlist.registry.RegistrySyncFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Describes a session for the server report screen: what the server announced and what the client reported.
 */
public final class ServerReport {
	private ServerReport() {
	}

	/** The report's paragraphs, in display order. */
	public static List<Component> describe(final ConnectionSession session) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("options.modlist.report.address", orUnknown(session.address())));
		lines.add(Component.translatable("options.modlist.report.profile", Component.translatable(session.usesServerProfile()
				? "options.modlist.profile.server_title" : "options.modlist.profile.default_title")));
		lines.add(Component.translatable("options.modlist.report.report_mode", session.plan().mode().label()));
		lines.add(Component.translatable("options.modlist.report.server_brand", orUnknown(session.serverBrand())));
		lines.add(Component.translatable("options.modlist.report.brand", orUnknown(session.reportedBrand()),
				orUnknown(session.realBrand())));

		lines.add(heading("options.modlist.report.server_channels", session.serverChannels().size()));
		lines.addAll(byNamespace(session.serverChannels()));
		Set<String> registryNamespaces = new TreeSet<>(session.serverRegistryNamespaces());
		lines.add(heading("options.modlist.report.registry_namespaces", registryNamespaces.size()));
		if (!registryNamespaces.isEmpty()) {
			lines.add(Component.literal(String.join(", ", registryNamespaces)));
		}
		lines.add(heading("options.modlist.report.hidden_channels", session.hiddenChannels().size()));
		lines.addAll(byNamespace(session.hiddenChannels()));
		lines.add(heading("options.modlist.report.faked_channels", session.fakedChannels().size()));
		lines.addAll(byNamespace(session.fakedChannels()));
		lines.add(Component.translatable("options.modlist.report.dropped", session.droppedPayloads(),
				session.refusedLoginQueries()));

		RegistrySyncFilter.Result ignored = session.ignoredRegistryContent();
		if (ignored == null) {
			lines.add(Component.translatable("options.modlist.report.ignored.none").withStyle(ChatFormatting.YELLOW));
		} else {
			lines.add(heading("options.modlist.report.ignored",
					ignored.droppedEntryCount() + ignored.droppedRegistries().size()));
			for (Map.Entry<Identifier, List<Identifier>> registry : ignored.droppedEntries().entrySet()) {
				lines.add(entry(registry.getKey().toString(),
						registry.getValue().stream().map(Identifier::toString).toList()));
			}
			for (Identifier registry : ignored.droppedRegistries()) {
				lines.add(Component.translatable("message.modlist.unknown_registry", registry.toString()));
			}
		}
		return lines;
	}

	private static Component heading(final String key, final int count) {
		return Component.translatable(key, count).withStyle(ChatFormatting.YELLOW);
	}

	/** One line per namespace, listing the paths of its channels. */
	private static List<Component> byNamespace(final Collection<Identifier> channels) {
		Map<String, TreeSet<String>> paths = new TreeMap<>();
		for (Identifier channel : channels) {
			paths.computeIfAbsent(channel.getNamespace(), namespace -> new TreeSet<>()).add(channel.getPath());
		}
		List<Component> lines = new ArrayList<>();
		paths.forEach((namespace, namespacePaths) -> lines.add(entry(namespace, namespacePaths)));
		return lines;
	}

	private static Component entry(final String name, final Collection<String> values) {
		return Component.translatable("message.modlist.entry",
				Component.literal(name).withStyle(ChatFormatting.GRAY), String.join(", ", values));
	}

	private static Component orUnknown(final @Nullable String value) {
		return value != null ? Component.literal(value) : Component.translatable("options.modlist.report.unknown");
	}
}
