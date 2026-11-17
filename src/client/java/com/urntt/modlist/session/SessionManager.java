package com.urntt.modlist.session;

import com.urntt.modlist.config.ModListConfig;
import com.urntt.modlist.config.ServerProfile;
import com.urntt.modlist.registry.UnknownContentFallbacks;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.Connection;
import org.jspecify.annotations.Nullable;

/**
 * Tracks the client's game connection to a server. There is at most one at a time.
 *
 * <p>A session starts when the login to a server begins and ends when the connection is lost. Connections to a
 * world this client hosts (memory connections) and server list pings never get a session, so the mod leaves them
 * alone.
 */
public final class SessionManager {
	private static volatile @Nullable ConnectionSession current;
	private static volatile @Nullable ConnectionSession last;

	private SessionManager() {
	}

	/**
	 * Starts a session for a login to {@code serverData} over {@code connection}, unless it is a memory connection.
	 */
	public static void begin(final Connection connection, final @Nullable ServerData serverData,
			final ModListConfig config) {
		end();
		if (connection.isMemoryConnection()) {
			return;
		}
		String address = serverData != null ? serverData.ip : null;
		ServerProfile serverProfile = config.serverProfileFor(address);
		ConnectionSession session = new ConnectionSession(connection, address,
				serverProfile != null ? serverProfile.profile() : config.defaultProfile(), serverProfile != null);
		current = session;
		last = session;
	}

	/** Ends the session of {@code connection}, if it has one. */
	public static void end(final Connection connection) {
		ConnectionSession session = current;
		if (session != null && session.connection() == connection) {
			end();
		}
	}

	private static void end() {
		current = null;
		UnknownContentFallbacks.deactivate();
	}

	/** The session of the current game connection, or {@code null} if there is none. */
	public static @Nullable ConnectionSession current() {
		return current;
	}

	/** The session of {@code connection}, or {@code null} if it has none. */
	public static @Nullable ConnectionSession of(final Connection connection) {
		ConnectionSession session = current;
		return session != null && session.connection() == connection ? session : null;
	}

	/** The current session, or else the most recent one, for the server report. */
	public static @Nullable ConnectionSession latest() {
		return last;
	}
}
