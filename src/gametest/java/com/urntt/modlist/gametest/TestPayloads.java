package com.urntt.modlist.gametest;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Channels for the multiplayer tests. The local dedicated server runs in the same JVM, so the server receivers
 * registered here are the ones it announces to the client.
 */
public final class TestPayloads implements ClientModInitializer {
	/** The namespace of this test mod, which the tests hide. */
	public static final String NAMESPACE = "modlist-gametest";
	/** A namespace only the server has channels in. */
	public static final String SERVER_NAMESPACE = "mirrored";
	/** A namespace only the client has channels in. */
	public static final String CLIENT_NAMESPACE = "modlist-clientonly";

	@Override
	public void onInitializeClient() {
		PayloadTypeRegistry.clientboundPlay().register(ClientChannel.TYPE, ClientChannel.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ClientOnly.TYPE, ClientOnly.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Probe.TYPE, Probe.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Mirrored.TYPE, Mirrored.CODEC);

		ClientPlayNetworking.registerGlobalReceiver(ClientChannel.TYPE, (payload, context) -> {
		});
		ClientPlayNetworking.registerGlobalReceiver(ClientOnly.TYPE, (payload, context) -> {
		});
		ServerPlayNetworking.registerGlobalReceiver(Probe.TYPE, (payload, context) -> ServerRecorder.recordProbe());
		ServerPlayNetworking.registerGlobalReceiver(Mirrored.TYPE, (payload, context) -> {
		});
	}

	/** Received by the client in this test mod's namespace. */
	public record ClientChannel() implements CustomPacketPayload {
		public static final Type<ClientChannel> TYPE = new Type<>(Identifier.fromNamespaceAndPath(NAMESPACE, "client_channel"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ClientChannel> CODEC = StreamCodec.unit(new ClientChannel());

		@Override
		public Type<ClientChannel> type() {
			return TYPE;
		}
	}

	/** Received by the client in a namespace the server has no channel in. */
	public record ClientOnly() implements CustomPacketPayload {
		public static final Type<ClientOnly> TYPE = new Type<>(Identifier.fromNamespaceAndPath(CLIENT_NAMESPACE, "hello"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ClientOnly> CODEC = StreamCodec.unit(new ClientOnly());

		@Override
		public Type<ClientOnly> type() {
			return TYPE;
		}
	}

	/** Sent by the client in this test mod's namespace; the server counts it. */
	public record Probe() implements CustomPacketPayload {
		public static final Type<Probe> TYPE = new Type<>(Identifier.fromNamespaceAndPath(NAMESPACE, "probe"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Probe> CODEC = StreamCodec.unit(new Probe());

		@Override
		public Type<Probe> type() {
			return TYPE;
		}
	}

	/** Received by the server in a namespace the client has no mod for. */
	public record Mirrored() implements CustomPacketPayload {
		public static final Type<Mirrored> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SERVER_NAMESPACE, "hello"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Mirrored> CODEC = StreamCodec.unit(new Mirrored());

		@Override
		public Type<Mirrored> type() {
			return TYPE;
		}
	}
}
