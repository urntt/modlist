package com.urntt.modlist.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.urntt.modlist.session.ConnectionSession;
import com.urntt.modlist.session.PacketRewriter;
import com.urntt.modlist.session.SessionManager;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The single place where the mod sees what the server announces and changes what the client reports. Only the
 * connection of the current session is affected; every other connection, including singleplayer and server list
 * pings, stays vanilla.
 */
@Mixin(Connection.class)
abstract class ConnectionMixin {
	@Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
			at = @At("HEAD"))
	private void modlist$recordInbound(final ChannelHandlerContext context, final Packet<?> packet,
			final CallbackInfo ci) {
		ConnectionSession session = SessionManager.of((Connection) (Object) this);
		if (session != null) {
			PacketRewriter.recordInbound(session, packet);
		}
	}

	@WrapMethod(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V")
	private void modlist$rewriteOutbound(final Packet<?> packet, final @Nullable ChannelFutureListener listener,
			final boolean flush, final Operation<Void> original) {
		ConnectionSession session = SessionManager.of((Connection) (Object) this);
		Packet<?> rewritten = session != null ? PacketRewriter.rewriteOutbound(session, packet) : packet;
		if (rewritten != null) {
			original.call(rewritten, listener, flush);
		}
	}
}
