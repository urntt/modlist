package com.urntt.modlist.registry;

import com.urntt.modlist.ModListClient;
import com.urntt.modlist.mixin.MappedRegistryAccessor;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.registry.RegistryIdRemapCallback;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fills the raw ID holes that ignored registry entries leave behind.
 *
 * <p>Vanilla assumes raw IDs have no holes: {@code MappedRegistry.byId(int)} and the registry iterator fail on them,
 * and Fabric API's own remap listeners (such as the block state tracker) iterate the registry while remapping. The
 * filler therefore runs as a remap listener in a phase before Fabric API's default phase and puts a placeholder into
 * every empty raw ID, up to the server's highest raw ID. The placeholder's own raw ID is unchanged, and Fabric API's
 * unmapping on disconnect restores the client's registries as usual.
 */
public final class RegistryHoleFiller {
	private static final Logger LOGGER = LoggerFactory.getLogger(ModListClient.MOD_ID);
	private static final Identifier PHASE = Identifier.fromNamespaceAndPath(ModListClient.MOD_ID, "fill_ignored_entries");

	/** Required sizes of the registries currently being remapped with ignored entries, keyed by registry. */
	private static final Map<Identifier, Integer> PENDING = new HashMap<>();
	private static Map<String, String> placeholders = Map.of();

	private RegistryHoleFiller() {
	}

	/** Registers the filler for every built-in registry. Call once during client initialization. */
	public static void register() {
		for (Registry<?> registry : BuiltInRegistries.REGISTRY) {
			registerFor(registry);
		}
	}

	private static <T> void registerFor(final Registry<T> registry) {
		if (!(registry instanceof MappedRegistry<T>)) {
			return;
		}
		Event<RegistryIdRemapCallback<T>> event = RegistryIdRemapCallback.event(registry);
		event.addPhaseOrdering(PHASE, Event.DEFAULT_PHASE);
		event.register(PHASE, state -> fill(registry));
	}

	/**
	 * Arms the filler for the registries in {@code requiredSizes} until {@link #disarm()}.
	 *
	 * @param requiredSizes per registry, the number of raw IDs that must resolve
	 * @param profilePlaceholders the profile's placeholder overrides
	 */
	public static void arm(final Map<Identifier, Integer> requiredSizes, final Map<String, String> profilePlaceholders) {
		PENDING.clear();
		PENDING.putAll(requiredSizes);
		placeholders = Map.copyOf(profilePlaceholders);
	}

	public static void disarm() {
		PENDING.clear();
		placeholders = Map.of();
	}

	@SuppressWarnings("unchecked")
	private static <T> void fill(final Registry<T> registry) {
		Integer requiredSize = PENDING.get(registry.key().identifier());
		if (requiredSize == null) {
			return;
		}

		ObjectList<Holder.Reference<T>> byId = ((MappedRegistryAccessor<T>) registry).modlist$byId();
		if (byId.size() < requiredSize) {
			byId.size(requiredSize);
		}

		Holder.Reference<T> placeholder = Placeholders.resolve(registry, placeholders);
		if (placeholder == null) {
			placeholder = byId.stream().filter(reference -> reference != null).findFirst().orElse(null);
		}
		if (placeholder == null) {
			return;
		}

		int filled = 0;
		for (int rawId = 0; rawId < byId.size(); rawId++) {
			if (byId.get(rawId) == null) {
				byId.set(rawId, placeholder);
				filled++;
			}
		}
		LOGGER.info("Filled {} unknown raw IDs of {} with {}", filled, registry.key().identifier(),
				placeholder.key().identifier());
	}
}
