package com.urntt.modlist.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.urntt.modlist.config.ProfileSettings;
import com.urntt.modlist.registry.Placeholders;
import com.urntt.modlist.registry.RegistryHoleFiller;
import com.urntt.modlist.registry.RegistrySyncFilter;
import com.urntt.modlist.registry.UnknownContentFallbacks;
import com.urntt.modlist.session.ConnectionSession;
import com.urntt.modlist.session.SessionManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.fabricmc.fabric.impl.client.registry.sync.ClientRegistrySyncHandler;
import net.fabricmc.fabric.impl.registry.sync.RemapException;
import net.fabricmc.fabric.impl.registry.sync.packet.RegistrySyncPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Fabric API's client side of registry sync, which disconnects when the server sends registry entries the client
 * does not have.
 *
 * <p>When the session's profile ignores unknown registry entries, the server's map is filtered before Fabric API
 * applies it, and the resulting raw ID holes are filled with placeholders. Otherwise Fabric API's behavior is kept,
 * and its disconnect message gains a hint about this option. Fabric API's own checks and remapping run unchanged on
 * whatever map they are given.
 */
@Mixin(ClientRegistrySyncHandler.class)
abstract class ClientRegistrySyncHandlerMixin {
	@WrapMethod(method = "apply")
	private static void modlist$ignoreUnknownEntries(final RegistrySyncPayload payload, final Operation<Void> original) {
		ConnectionSession session = SessionManager.current();
		if (session == null) {
			original.call(payload);
			return;
		}
		session.addServerRegistryNamespaces(modlist$namespaces(payload));

		ProfileSettings profile = session.profile();
		if (!profile.ignoreUnknownRegistryEntries()) {
			original.call(payload);
			return;
		}

		RegistrySyncFilter.Result result = RegistrySyncFilter.filter(payload.registryMap(), payload.registryAttributes(),
				ClientRegistrySyncHandlerMixin::modlist$localEntries);
		if (!result.droppedAnything()) {
			original.call(payload);
			return;
		}

		session.setIgnoredRegistryContent(result);
		RegistryHoleFiller.arm(result.requiredSizes(), profile.placeholders());
		try {
			original.call(new RegistrySyncPayload(result.registryMap(), result.attributes()));
		} finally {
			RegistryHoleFiller.disarm();
		}

		Holder.Reference<Block> block = Placeholders.resolve(BuiltInRegistries.BLOCK, profile.placeholders());
		UnknownContentFallbacks.activate((block != null ? block.value() : Blocks.AIR).defaultBlockState());
	}

	@WrapMethod(method = "checkRemoteRemap")
	private static void modlist$hintAtIgnoring(final RegistrySyncPayload payload, final Operation<Void> original)
			throws RemapException {
		try {
			original.call(payload);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			RemapException remapException = (RemapException) e;
			Component message = remapException.getComponent();
			if (message == null || SessionManager.current() == null) {
				throw remapException;
			}
			throw new RemapException(message.copy()
					.append(CommonComponents.NEW_LINE)
					.append(Component.translatable("message.modlist.ignore_hint").withStyle(ChatFormatting.AQUA)));
		}
	}

	@Unique
	private static @Nullable List<Identifier> modlist$localEntries(final Identifier registryId) {
		Registry<?> registry = BuiltInRegistries.REGISTRY.getValue(registryId);
		return registry != null ? modlist$keysInRawIdOrder(registry) : null;
	}

	@Unique
	private static <T> List<Identifier> modlist$keysInRawIdOrder(final Registry<T> registry) {
		List<Identifier> keys = new ArrayList<>(registry.size());
		for (T entry : registry) {
			keys.add(registry.getKey(entry));
		}
		return keys;
	}

	@Unique
	private static Set<String> modlist$namespaces(final RegistrySyncPayload payload) {
		Set<String> namespaces = new TreeSet<>();
		payload.registryMap().values().forEach(entries -> entries.keySet().forEach(id -> namespaces.add(id.getNamespace())));
		return namespaces;
	}
}
