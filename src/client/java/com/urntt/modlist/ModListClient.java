package com.urntt.modlist;

import com.mojang.blaze3d.platform.InputConstants;
import com.urntt.modlist.config.ModListConfig;
import com.urntt.modlist.config.ModListConfigScreen;
import com.urntt.modlist.mixin.ClientCommonPacketListenerImplAccessor;
import com.urntt.modlist.mixin.ClientHandshakePacketListenerImplAccessor;
import com.urntt.modlist.registry.RegistryHoleFiller;
import com.urntt.modlist.session.ConnectionSession;
import com.urntt.modlist.session.JoinNotices;
import com.urntt.modlist.session.SessionManager;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class ModListClient implements ClientModInitializer {
	public static final String MOD_ID = "modlist";
	public static final String OPEN_SETTINGS_KEY_NAME = "key.modlist.open_settings";

	private static ModListConfig config;

	@Override
	public void onInitializeClient() {
		config = ModListConfig.load(ModListConfig.defaultPath());
		RegistryHoleFiller.register();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "general"));
		KeyMapping openSettingsKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping(OPEN_SETTINGS_KEY_NAME, InputConstants.UNKNOWN.getValue(), category));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openSettingsKey.consumeClick()) {
				client.gui.setScreen(new ModListConfigScreen(client.gui.screen()));
			}
		});

		ClientLoginConnectionEvents.INIT.register((handler, client) -> {
			ClientHandshakePacketListenerImplAccessor accessor = (ClientHandshakePacketListenerImplAccessor) handler;
			SessionManager.begin(accessor.modlist$connection(), accessor.modlist$serverData(), config);
		});
		ClientLoginConnectionEvents.DISCONNECT.register((handler, client) ->
				SessionManager.end(((ClientHandshakePacketListenerImplAccessor) handler).modlist$connection()));
		ClientConfigurationConnectionEvents.DISCONNECT.register((handler, client) ->
				SessionManager.end(((ClientCommonPacketListenerImplAccessor) handler).modlist$connection()));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SessionManager.end(handler.getConnection()));

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			ConnectionSession session = SessionManager.of(handler.getConnection());
			if (session != null && config.joinNotices() && session.claimJoinNotices()) {
				JoinNotices.show(session, client, installedMods());
			}
		});
	}

	public static ModListConfig config() {
		return config;
	}

	/** The IDs of all loaded mods. */
	public static Set<String> installedMods() {
		return FabricLoader.getInstance().getAllMods().stream()
				.map(mod -> mod.getMetadata().getId())
				.collect(Collectors.toUnmodifiableSet());
	}
}
