package com.urntt.modlist.gametest.mixin;

import com.urntt.modlist.gametest.ServerRecorder;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records the custom payloads the local dedicated server receives.
 */
@Mixin(Connection.class)
abstract class ServerConnectionMixin {
	@Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
			at = @At("HEAD"))
	private void modlistGametest$record(final ChannelHandlerContext context, final Packet<?> packet,
			final CallbackInfo ci) {
		Connection connection = (Connection) (Object) this;
		if (connection.getReceiving() == PacketFlow.SERVERBOUND && !connection.isMemoryConnection()
				&& packet instanceof ServerboundCustomPayloadPacket(var payload)) {
			ServerRecorder.record(payload);
		}
	}
}
