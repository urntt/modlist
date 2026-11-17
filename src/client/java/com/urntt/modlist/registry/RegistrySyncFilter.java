package com.urntt.modlist.registry;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Removes what the client does not know from a Fabric API registry sync map, so the client can apply the rest.
 *
 * <p>Entries keep the server's raw IDs. The raw IDs of dropped entries become holes, which
 * {@link RegistryHoleFiller} fills with placeholders, so the server's IDs for them still resolve to something. Entries
 * only the client has get raw IDs above the server's highest one; Fabric API would otherwise number them from the
 * highest ID it is given, which can be the ID of a dropped entry.
 */
public final class RegistrySyncFilter {
	private RegistrySyncFilter() {
	}

	/**
	 * Filters a registry sync map.
	 *
	 * @param registryMap the server's map from registry to entry to raw ID
	 * @param attributes the server's registry attributes
	 * @param localEntries returns the client's entries of a registry in raw ID order, or {@code null} if the client
	 *        does not have the registry
	 */
	public static Result filter(final Map<Identifier, Object2IntMap<Identifier>> registryMap,
			final Map<Identifier, EnumSet<RegistryAttribute>> attributes,
			final Function<Identifier, @Nullable List<Identifier>> localEntries) {
		Map<Identifier, Object2IntMap<Identifier>> keptMap = new LinkedHashMap<>();
		Map<Identifier, EnumSet<RegistryAttribute>> keptAttributes = new LinkedHashMap<>();
		Map<Identifier, List<Identifier>> droppedEntries = new LinkedHashMap<>();
		List<Identifier> droppedRegistries = new ArrayList<>();
		Map<Identifier, Integer> requiredSizes = new LinkedHashMap<>();

		for (Map.Entry<Identifier, Object2IntMap<Identifier>> registry : registryMap.entrySet()) {
			Identifier registryId = registry.getKey();
			Object2IntMap<Identifier> remote = registry.getValue();
			List<Identifier> local = localEntries.apply(registryId);
			if (local == null) {
				EnumSet<RegistryAttribute> registryAttributes = attributes.get(registryId);
				if (registryAttributes == null || !registryAttributes.contains(RegistryAttribute.OPTIONAL)) {
					droppedRegistries.add(registryId);
				}
				continue;
			}

			keptAttributes.put(registryId, attributes.getOrDefault(registryId, EnumSet.noneOf(RegistryAttribute.class)));
			Set<Identifier> localSet = new HashSet<>(local);
			List<Identifier> dropped = new ArrayList<>();
			for (Identifier entry : remote.keySet()) {
				if (!localSet.contains(entry)) {
					dropped.add(entry);
				}
			}
			if (dropped.isEmpty()) {
				keptMap.put(registryId, remote);
				continue;
			}

			int highestRemoteId = remote.values().intStream().max().orElse(-1);
			Object2IntMap<Identifier> kept = new Object2IntLinkedOpenHashMap<>();
			for (Object2IntMap.Entry<Identifier> entry : remote.object2IntEntrySet()) {
				if (localSet.contains(entry.getKey())) {
					kept.put(entry.getKey(), entry.getIntValue());
				}
			}
			int nextId = highestRemoteId;
			for (Identifier entry : local) {
				if (!remote.containsKey(entry)) {
					kept.put(entry, ++nextId);
				}
			}

			dropped.sort(null);
			keptMap.put(registryId, kept);
			droppedEntries.put(registryId, List.copyOf(dropped));
			requiredSizes.put(registryId, highestRemoteId + 1);
		}

		droppedRegistries.sort(null);
		return new Result(keptMap, keptAttributes, droppedEntries, List.copyOf(droppedRegistries), requiredSizes);
	}

	/**
	 * @param registryMap the map to apply
	 * @param attributes the attributes of the registries in {@code registryMap}
	 * @param droppedEntries the dropped entries per registry, sorted
	 * @param droppedRegistries the registries the client does not have and that the server does not mark optional
	 * @param requiredSizes per registry with dropped entries, the number of raw IDs that must resolve after applying
	 */
	public record Result(Map<Identifier, Object2IntMap<Identifier>> registryMap,
			Map<Identifier, EnumSet<RegistryAttribute>> attributes,
			Map<Identifier, List<Identifier>> droppedEntries,
			List<Identifier> droppedRegistries,
			Map<Identifier, Integer> requiredSizes) {
		public boolean droppedAnything() {
			return !this.droppedEntries.isEmpty() || !this.droppedRegistries.isEmpty();
		}

		public int droppedEntryCount() {
			return this.droppedEntries.values().stream().mapToInt(List::size).sum();
		}
	}
}
