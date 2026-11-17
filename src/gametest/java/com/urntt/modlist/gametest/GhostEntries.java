package com.urntt.modlist.gametest;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

/**
 * Makes the local dedicated server's registry sync contain entries and a registry the client does not have, the way
 * a server with extra content mods does.
 *
 * <p>The item map moves the client's last item one raw ID up and puts an unknown item into the freed raw ID, so the
 * client sees an unknown entry between known ones, plus another unknown item at the end. The server and the client
 * share the item registry in the test JVM, so the client's remapping moves the item for both.
 */
public final class GhostEntries {
	public static final Identifier MIDDLE_ITEM = Identifier.fromNamespaceAndPath("modlist-ghost", "middle_item");
	public static final Identifier TAIL_ITEM = Identifier.fromNamespaceAndPath("modlist-ghost", "tail_item");
	public static final Identifier UNKNOWN_REGISTRY = Identifier.fromNamespaceAndPath("modlist-ghost", "ghosts");

	private static volatile boolean enabled;
	private static volatile @Nullable Layout layout;

	private GhostEntries() {
	}

	public static void setEnabled(final boolean enabled) {
		GhostEntries.enabled = enabled;
	}

	/** The item IDs of the last registry sync with ghost entries, or {@code null} if there was none. */
	public static @Nullable Layout layout() {
		return layout;
	}

	/** Adds the ghost entries to {@code original}, the server's registry sync map, if enabled. */
	public static @Nullable Map<Identifier, Object2IntMap<Identifier>> inject(
			final @Nullable Map<Identifier, Object2IntMap<Identifier>> original) {
		if (!enabled) {
			return original;
		}
		Map<Identifier, Object2IntMap<Identifier>> map = original != null ? new LinkedHashMap<>(original) : new LinkedHashMap<>();

		int lastId = BuiltInRegistries.ITEM.size() - 1;
		Item lastItem = BuiltInRegistries.ITEM.byId(lastId);
		Object2IntMap<Identifier> items = new Object2IntLinkedOpenHashMap<>();
		for (Item item : BuiltInRegistries.ITEM) {
			items.put(BuiltInRegistries.ITEM.getKey(item), BuiltInRegistries.ITEM.getId(item));
		}
		Identifier lastKey = BuiltInRegistries.ITEM.getKey(lastItem);
		items.put(MIDDLE_ITEM, lastId);
		items.put(lastKey, lastId + 1);
		items.put(TAIL_ITEM, lastId + 2);
		map.put(Registries.ITEM.identifier(), items);

		Object2IntMap<Identifier> ghosts = new Object2IntLinkedOpenHashMap<>();
		ghosts.put(Identifier.fromNamespaceAndPath("modlist-ghost", "ghost"), 0);
		map.put(UNKNOWN_REGISTRY, ghosts);

		layout = new Layout(lastId, lastKey);
		return map;
	}

	/**
	 * @param lastId the client's raw ID of {@code lastItem} before remapping
	 * @param lastItem the item the server numbers {@code lastId + 1}
	 */
	public record Layout(int lastId, Identifier lastItem) {
	}
}
