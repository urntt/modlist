package com.urntt.modlist.gametest;

import com.urntt.modlist.ModListClient;
import com.urntt.modlist.config.ModListConfig;
import com.urntt.modlist.config.Profile;
import com.urntt.modlist.config.ReportMode;
import com.urntt.modlist.config.ServerProfile;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared helpers for the client game tests.
 */
@SuppressWarnings("UnstableApiUsage")
final class GameTestSupport {
	static final Logger LOGGER = LoggerFactory.getLogger("modlist-gametest");

	private GameTestSupport() {
	}

	/** Changes the mod's configuration on the client thread. */
	static void configure(final ClientGameTestContext context, final Consumer<ModListConfig> change) {
		context.runOnClient(client -> change.accept(ModListClient.config()));
	}

	/** Restores the default profile's defaults and removes every server profile. */
	static void resetConfig(final ClientGameTestContext context) {
		configure(context, config -> {
			config.setJoinNotices(true);
			for (ServerProfile server : config.serverProfiles()) {
				config.removeServerProfile(server);
			}
			resetProfile(config.defaultProfile());
		});
	}

	/** Sets every setting of {@code profile} to its default. */
	static void resetProfile(final Profile profile) {
		profile.setBrand("");
		profile.setHiddenNamespaces(List.of());
		profile.setFakeChannels(List.of());
		profile.setReportMode(ReportMode.ACTUAL);
		profile.setIgnoreUnknownRegistryEntries(false);
		profile.setPlaceholders(Map.of());
		profile.setProtectHiddenTranslations(true);
	}

	static void switchLanguage(final ClientGameTestContext context, final String language) {
		CompletableFuture<Void> reload = context.computeOnClient(client -> {
			client.getLanguageManager().setSelected(language);
			client.options.languageCode = language;
			return client.reloadResourcePacks();
		});
		context.waitFor(client -> reload.isDone() && client.gui.overlay() == null);
	}

	static void check(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	static void checkEquals(final Object expected, final Object actual, final String what) {
		if (!Objects.equals(expected, actual)) {
			throw new AssertionError(what + ": expected " + expected + ", got " + actual);
		}
	}
}
