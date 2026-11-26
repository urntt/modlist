package com.urntt.modlist.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.urntt.modlist.ModListClient;
import com.urntt.modlist.compat.OneConfigBridge;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The mod's own settings, the server profiles and the configuration mode, persisted as JSON. Every change is written
 * to disk immediately.
 */
public final class ModListConfig implements ModListSettings {
	private static final Logger LOGGER = LoggerFactory.getLogger(ModListClient.MOD_ID);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private final Path path;
	private final Settings settings;

	private ModListConfig(final Path path, final Settings settings) {
		this.path = path;
		this.settings = settings;
		this.settings.defaultProfile.attach(this::save);
		this.settings.servers.forEach(server -> server.attach(this::save));
	}

	/**
	 * Returns the location of the config file in the Fabric config directory.
	 */
	public static Path defaultPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(ModListClient.MOD_ID + ".json");
	}

	/**
	 * Loads the configuration from {@code path}. A missing file is created with the defaults. A readable file is
	 * rewritten with any settings it lacks and without entries it cannot use. An unreadable file falls back to the
	 * defaults and is left untouched until the next change is saved.
	 */
	public static ModListConfig load(final Path path) {
		if (Files.notExists(path)) {
			ModListConfig config = new ModListConfig(path, new Settings());
			config.save();
			return config;
		}

		Settings settings;
		try {
			settings = GSON.fromJson(Files.readString(path), Settings.class);
		} catch (IOException | JsonParseException e) {
			LOGGER.warn("Failed to read config file {}, using defaults", path, e);
			return new ModListConfig(path, new Settings());
		}

		if (settings == null) {
			settings = new Settings();
		}
		settings.normalize();
		ModListConfig config = new ModListConfig(path, settings);
		config.save();
		return config;
	}

	/** The mode stored in the file, which may name OneConfig even while OneConfig is not installed. */
	public ConfigMode storedMode() {
		return this.settings.mode;
	}

	public void setMode(final ConfigMode mode) {
		this.settings.mode = mode;
		this.save();
	}

	/** The mode that applies: OneConfig mode falls back to the custom settings while OneConfig is not available. */
	public ConfigMode effectiveMode() {
		ConfigMode mode = this.settings.mode;
		return mode == ConfigMode.ONECONFIG && !OneConfigBridge.isAvailable() ? ConfigMode.CUSTOM : mode;
	}

	/**
	 * The settings that apply in the {@linkplain #effectiveMode() effective mode}, or {@code null} while the mod is off:
	 * in disabled mode, or in OneConfig mode while the toggle on OneConfig's card is off.
	 */
	public @Nullable ModListSettings effective() {
		ModListSettings settings = switch (this.effectiveMode()) {
			case DISABLED -> null;
			case CUSTOM -> this;
			case ONECONFIG -> OneConfigBridge.settings().orElse(this);
		};
		return settings != null && settings.enabled() ? settings : null;
	}

	/**
	 * Whether the mod changes connections: not in disabled mode, nor in OneConfig mode while the toggle on OneConfig's
	 * card is off. While it does not, profiles cannot be edited from the screens, because they have no effect.
	 */
	public boolean isActive() {
		return this.effective() != null;
	}

	/** Whether join notices are shown, as the {@linkplain #effective() effective settings} decide. */
	public boolean effectiveJoinNotices() {
		ModListSettings settings = this.effective();
		return settings != null && settings.joinNotices();
	}

	/**
	 * Returns the profile a connection to {@code connectedAddress} uses: the first matching server profile, else the
	 * {@linkplain #effective() effective} default profile. Returns {@code null} while the mod is off, so the connection
	 * is left alone.
	 */
	public @Nullable ProfileSettings effectiveProfileFor(final @Nullable String connectedAddress) {
		ModListSettings settings = this.effective();
		if (settings == null) {
			return null;
		}
		ServerProfile server = this.serverProfileFor(connectedAddress);
		return server != null ? server.profile() : settings.defaultProfile();
	}

	/** Whether chat messages summarize the differences between client and server after joining. */
	@Override
	public boolean joinNotices() {
		return this.settings.joinNotices;
	}

	public void setJoinNotices(final boolean joinNotices) {
		this.settings.joinNotices = joinNotices;
		this.save();
	}

	/** The profile used for servers that no server profile matches. */
	@Override
	public Profile defaultProfile() {
		return this.settings.defaultProfile;
	}

	/** The server profiles, in the order they are matched. */
	public List<ServerProfile> serverProfiles() {
		return List.copyOf(this.settings.servers);
	}

	/**
	 * Adds a server profile for {@code address} that starts as a copy of the default profile that applies: OneConfig's
	 * in OneConfig mode, otherwise the mod's own.
	 */
	public ServerProfile addServerProfile(final String address) {
		ModListSettings effective = this.effective();
		ProfileSettings base = effective != null ? effective.defaultProfile() : this.settings.defaultProfile;
		ServerProfile server = new ServerProfile(address.trim(), Profile.copyOf(base));
		server.attach(this::save);
		this.settings.servers.add(server);
		this.save();
		return server;
	}

	public void removeServerProfile(final ServerProfile server) {
		if (this.settings.servers.remove(server)) {
			this.save();
		}
	}

	/**
	 * Returns the server profile a connection to {@code address} uses, adding one for {@code address} if none matches.
	 */
	public ServerProfile serverProfileToEdit(final String address) {
		ServerProfile server = this.serverProfileFor(address);
		return server != null ? server : this.addServerProfile(address);
	}

	/** Returns the first server profile whose address matches, or {@code null} if none does. */
	public @Nullable ServerProfile serverProfileFor(final @Nullable String connectedAddress) {
		if (connectedAddress == null) {
			return null;
		}
		for (ServerProfile server : this.settings.servers) {
			if (server.matches(connectedAddress)) {
				return server;
			}
		}
		return null;
	}

	private void save() {
		try {
			Files.createDirectories(this.path.getParent());
			Files.writeString(this.path, GSON.toJson(this.settings));
		} catch (IOException e) {
			LOGGER.error("Failed to write config file {}", this.path, e);
		}
	}

	/**
	 * The serialized form. Field initializers are the defaults, which also apply to keys missing from the file.
	 */
	private static final class Settings {
		private ConfigMode mode = ConfigMode.CUSTOM;
		private boolean joinNotices = true;
		private Profile defaultProfile = new Profile();
		private List<ServerProfile> servers = new ArrayList<>();

		private void normalize() {
			if (this.mode == null) {
				this.mode = ConfigMode.CUSTOM;
			}
			if (this.defaultProfile == null) {
				this.defaultProfile = new Profile();
			}
			this.defaultProfile.normalize();

			List<ServerProfile> usable = new ArrayList<>();
			if (this.servers != null) {
				for (ServerProfile server : this.servers) {
					if (server != null && server.normalize()) {
						usable.add(server);
					}
				}
			}
			this.servers = usable;
		}
	}
}
