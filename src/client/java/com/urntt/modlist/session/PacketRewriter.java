package com.urntt.modlist.session;

import java.util.HashSet;
import net.fabricmc.fabric.impl.networking.CommonRegisterPayload;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import org.jspecify.annotations.Nullable;

/**
 * Records what the server announces and rewrites what the client reports, packet by packet.
 *
 * <p>Channel registrations use Fabric API's payload types ({@code minecraft:register}, {@code minecraft:unregister}
 * and the common {@code c:register}), which are internal classes of Fabric API.
 */
public final class PacketRewriter {
	private PacketRewriter() {
	}

	/** Records what an incoming packet tells about the server. Called on the network thread before it is handled. */
	public static void recordInbound(final ConnectionSession session, final Packet<?> packet) {
		if (packet instanceof ClientboundCustomPayloadPacket(CustomPacketPayload payload)) {
			switch (payload) {
				case BrandPayload brand -> session.setServerBrand(brand.brand());
				case RegistrationPayload registration when RegistrationPayload.REGISTER.equals(registration.type()) ->
						session.addServerChannels(registration.channels());
				case RegistrationPayload registration -> session.removeServerChannels(registration.channels());
				case CommonRegisterPayload register -> session.addServerChannels(register.channels());
				default -> {
				}
			}
		} else if (packet instanceof ClientboundCustomQueryPacket(int transactionId, var payload)) {
			session.recordLoginQuery(transactionId, payload.id());
		}
	}

	/**
	 * Returns the packet to send instead of {@code packet}, which may be {@code packet} itself, or {@code null} to send
	 * nothing.
	 */
	public static @Nullable Packet<?> rewriteOutbound(final ConnectionSession session, final Packet<?> packet) {
		if (packet instanceof ServerboundCustomPayloadPacket(CustomPacketPayload payload)) {
			CustomPacketPayload rewritten = rewritePayload(session, payload);
			if (rewritten == null) {
				return null;
			}
			return rewritten == payload ? packet : new ServerboundCustomPayloadPacket(rewritten);
		}
		if (packet instanceof ServerboundCustomQueryAnswerPacket(int transactionId, var answer) && answer != null) {
			var channel = session.loginQueryChannel(transactionId);
			if (channel != null && session.plan().isHidden(channel.getNamespace(), session.serverChannels())) {
				session.recordRefusedLoginQuery();
				return new ServerboundCustomQueryAnswerPacket(transactionId, null);
			}
		}
		return packet;
	}

	private static @Nullable CustomPacketPayload rewritePayload(final ConnectionSession session,
			final CustomPacketPayload payload) {
		ReportPlan plan = session.plan();
		switch (payload) {
			case BrandPayload brand -> {
				String reported = plan.brand();
				session.recordBrand(brand.brand(), reported != null ? reported : brand.brand());
				return reported != null ? new BrandPayload(reported) : payload;
			}
			case RegistrationPayload registration when RegistrationPayload.REGISTER.equals(registration.type()) -> {
				ChannelRewriter.Result result = ChannelRewriter.rewrite(registration.channels(), plan,
						session.serverChannels());
				session.recordRewrite(result);
				return result.channels().isEmpty() ? null
						: new RegistrationPayload(RegistrationPayload.REGISTER, result.channels());
			}
			case RegistrationPayload unregistration -> {
				// Unregistering a hidden channel would reveal it; fake channels are never unregistered.
				var channels = unregistration.channels().stream()
						.filter(channel -> !plan.isHidden(channel.getNamespace(), session.serverChannels()))
						.toList();
				return channels.isEmpty() ? null : new RegistrationPayload(RegistrationPayload.UNREGISTER, channels);
			}
			default -> {
			}
		}

		if (plan.isHidden(payload.type().id().getNamespace(), session.serverChannels())) {
			session.recordDroppedPayload();
			return null;
		}

		if (payload instanceof CommonRegisterPayload register) {
			// The server waits for this answer, so it is sent even when every channel is hidden.
			ChannelRewriter.Result result = ChannelRewriter.rewrite(register.channels(), plan, session.serverChannels());
			session.recordRewrite(result);
			return new CommonRegisterPayload(register.version(), register.protocol(), new HashSet<>(result.channels()));
		}
		return payload;
	}
}
