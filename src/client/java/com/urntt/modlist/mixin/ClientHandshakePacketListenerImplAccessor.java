package com.urntt.modlist.mixin;

import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.Connection;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Gives access to the connection and the server of a login, which start a session.
 */
@Mixin(ClientHandshakePacketListenerImpl.class)
public interface ClientHandshakePacketListenerImplAccessor {
	@Accessor("connection")
	Connection modlist$connection();

	@Accessor("serverData")
	@Nullable ServerData modlist$serverData();
}
