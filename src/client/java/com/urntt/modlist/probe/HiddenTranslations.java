package com.urntt.modlist.probe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.urntt.modlist.ModListClient;
import com.urntt.modlist.session.ConnectionSession;
import com.urntt.modlist.session.LocalChannels;
import com.urntt.modlist.session.SessionManager;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds the translation keys that belong to the namespaces the current session hides. The result is cached, since
 * the anvil screen asks on every keystroke.
 */
public final class HiddenTranslations {
	private static final Logger LOGGER = LoggerFactory.getLogger(ModListClient.MOD_ID);
	private static final String DEFAULT_LANGUAGE = "en_us";

	private static @Nullable Cache cache;

	private HiddenTranslations() {
	}

	/**
	 * The translation keys a server must not be able to resolve through the client right now: those defined by the
	 * hidden namespaces' language files and not by vanilla. Empty outside a session or when the profile does not
	 * protect translations.
	 */
	public static Set<String> currentKeys() {
		ConnectionSession session = SessionManager.current();
		if (session == null || !session.profile().protectHiddenTranslations()) {
			return Set.of();
		}

		Minecraft minecraft = Minecraft.getInstance();
		ResourceManager resources = minecraft.getResourceManager();
		Set<String> candidates = new TreeSet<>(resources.getNamespaces());
		candidates.addAll(LocalChannels.namespaces());
		candidates.remove(Identifier.DEFAULT_NAMESPACE);
		candidates.remove(Identifier.REALMS_NAMESPACE);
		Set<String> hidden = session.plan().hiddenAmong(candidates, session.serverChannels());
		if (hidden.isEmpty()) {
			return Set.of();
		}

		List<String> languages = List.of(DEFAULT_LANGUAGE, minecraft.getLanguageManager().getSelected());
		Cache cached = cache;
		if (cached != null && cached.session() == session && cached.namespaces().equals(hidden)
				&& cached.languages().equals(languages)) {
			return cached.keys();
		}

		Set<String> keys = new HashSet<>();
		for (String namespace : hidden) {
			keys.addAll(keysOf(resources, namespace, languages));
		}
		keys.removeAll(keysOf(resources, Identifier.DEFAULT_NAMESPACE, languages));
		Set<String> result = Set.copyOf(keys);
		cache = new Cache(session, Set.copyOf(hidden), languages, result);
		return result;
	}

	private static Set<String> keysOf(final ResourceManager resources, final String namespace,
			final List<String> languages) {
		Set<String> keys = new HashSet<>();
		for (String language : Set.copyOf(languages)) {
			Identifier file = Identifier.fromNamespaceAndPath(namespace, "lang/" + language + ".json");
			for (Resource resource : resources.getResourceStack(file)) {
				try (Reader reader = resource.openAsReader()) {
					JsonElement json = JsonParser.parseReader(reader);
					if (json instanceof JsonObject object) {
						keys.addAll(object.keySet());
					}
				} catch (IOException | JsonParseException e) {
					LOGGER.warn("Failed to read language file {}", file, e);
				}
			}
		}
		return keys;
	}

	/** The keys last computed, reused while the session, the hidden namespaces and the language stay the same. */
	private record Cache(ConnectionSession session, Set<String> namespaces, List<String> languages, Set<String> keys) {
	}
}
