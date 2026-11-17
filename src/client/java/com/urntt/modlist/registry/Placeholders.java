package com.urntt.modlist.registry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

/**
 * Chooses the entries that stand in for registry entries the client does not have.
 *
 * <p>A profile can override the placeholder of any registry. Otherwise the built-in defaults apply: air for blocks
 * and items, and the marker entity, which is invisible and has no hitbox, for entity types. Other registries use their
 * default entry, if they have one, or else their lowest raw ID.
 */
public final class Placeholders {
	/** The registries the placeholder screen offers, in display order. */
	public static final List<ResourceKey<? extends Registry<?>>> EDITABLE = List.of(
			Registries.BLOCK, Registries.ITEM, Registries.ENTITY_TYPE);

	/** Built-in placeholders, keyed by registry. */
	public static final Map<Identifier, Identifier> BUILT_IN = Map.of(
			Registries.BLOCK.identifier(), Identifier.withDefaultNamespace("air"),
			Registries.ITEM.identifier(), Identifier.withDefaultNamespace("air"),
			Registries.ENTITY_TYPE.identifier(), Identifier.withDefaultNamespace("marker"));

	private Placeholders() {
	}

	/**
	 * Returns the configured or built-in placeholder of {@code registry}, or its default entry, or {@code null} if it
	 * has none of these.
	 *
	 * @param overrides the profile's placeholders, from registry identifier to entry identifier
	 */
	public static <T> Holder.@Nullable Reference<T> resolve(final Registry<T> registry,
			final Map<String, String> overrides) {
		Identifier registryId = registry.key().identifier();
		Optional<Holder.Reference<T>> configured = lookup(registry, overrides.get(registryId.toString()));
		if (configured.isPresent()) {
			return configured.get();
		}
		Identifier builtIn = BUILT_IN.get(registryId);
		if (builtIn != null && registry.get(builtIn).isPresent()) {
			return registry.get(builtIn).get();
		}
		if (registry instanceof DefaultedRegistry<T> defaulted) {
			return registry.get(defaulted.getDefaultKey()).orElse(null);
		}
		return null;
	}

	/** Whether {@code entry} names an entry of the registry {@code registryKey}. */
	public static boolean isValid(final ResourceKey<? extends Registry<?>> registryKey, final String entry) {
		Registry<?> registry = BuiltInRegistries.REGISTRY.getValue(registryKey.identifier());
		return registry != null && lookup(registry, entry).isPresent();
	}

	private static <T> Optional<Holder.Reference<T>> lookup(final Registry<T> registry, final @Nullable String entry) {
		if (entry == null) {
			return Optional.empty();
		}
		Identifier id = Identifier.tryParse(entry.trim());
		return id == null ? Optional.empty() : registry.get(id);
	}
}
