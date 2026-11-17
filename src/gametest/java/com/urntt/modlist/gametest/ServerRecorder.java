package com.urntt.modlist.gametest;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.impl.networking.CommonRegisterPayload;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * What the local dedicated server received from the client, recorded on the server's side of the connection.
 */
public final class ServerRecorder {
	private static volatile @Nullable String brand;
	private static final AtomicInteger REGISTRATIONS = new AtomicInteger();
	private static final AtomicInteger PROBES = new AtomicInteger();
	private static final Set<Identifier> PAYLOAD_TYPES = ConcurrentHashMap.newKeySet();

	private ServerRecorder() {
	}

	public static void reset() {
		brand = null;
		REGISTRATIONS.set(0);
		PROBES.set(0);
		PAYLOAD_TYPES.clear();
	}

	public static void record(final CustomPacketPayload payload) {
		PAYLOAD_TYPES.add(payload.type().id());
		if (payload instanceof BrandPayload brandPayload) {
			brand = brandPayload.brand();
		} else if (payload instanceof RegistrationPayload || payload instanceof CommonRegisterPayload) {
			REGISTRATIONS.incrementAndGet();
		}
	}

	static void recordProbe() {
		PROBES.incrementAndGet();
	}

	/** The brand the server received, or {@code null} if none arrived. */
	public static @Nullable String brand() {
		return brand;
	}

	/** The number of channel registrations the server received. */
	public static int registrations() {
		return REGISTRATIONS.get();
	}

	/** The number of probe payloads the server handled. */
	public static int probes() {
		return PROBES.get();
	}

	/** The types of all custom payloads the server received. */
	public static Set<Identifier> payloadTypes() {
		return Set.copyOf(PAYLOAD_TYPES);
	}
}
