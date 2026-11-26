package com.urntt.modlist.gametest;

import static com.urntt.modlist.gametest.GameTestSupport.LOGGER;
import static com.urntt.modlist.gametest.GameTestSupport.check;
import static com.urntt.modlist.gametest.GameTestSupport.checkEquals;
import static com.urntt.modlist.gametest.GameTestSupport.oneConfig;
import static com.urntt.modlist.gametest.GameTestSupport.resetConfig;
import static com.urntt.modlist.gametest.GameTestSupport.resetOneConfig;
import static com.urntt.modlist.gametest.GameTestSupport.setOneConfig;

import com.urntt.modlist.ModListClient;
import com.urntt.modlist.compat.OneConfigBridge;
import com.urntt.modlist.config.ConfigMode;
import com.urntt.modlist.config.ModListConfig;
import com.urntt.modlist.config.ModListSettings;
import com.urntt.modlist.config.Profile;
import com.urntt.modlist.config.ProfileSettings;
import com.urntt.modlist.config.SettingKeys;
import com.urntt.modlist.config.ReportMode;
import com.urntt.modlist.config.ServerProfile;
import com.urntt.modlist.probe.HiddenTranslations;
import com.urntt.modlist.probe.ProbeSafeText;
import com.urntt.modlist.registry.Placeholders;
import com.urntt.modlist.registry.RegistrySyncFilter;
import com.urntt.modlist.session.ChannelEntries;
import com.urntt.modlist.session.ChannelRewriter;
import com.urntt.modlist.session.JoinNotices;
import com.urntt.modlist.session.ReportPlan;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

/**
 * Checks the configuration and the decisions about what to report and what to drop, without a world.
 */
@SuppressWarnings("UnstableApiUsage")
public final class ModListLogicGameTest implements FabricClientGameTest {
	private static final Identifier MIRRORED = id("mirrored", "hello");
	private static final Identifier FAKE_HANDSHAKE = id("fakemod", "handshake");

	@Override
	public void runTest(final ClientGameTestContext context) {
		context.runOnClient(client -> {
			checkDefaults();
			checkSaveAndLoad();
			checkUnreadableFile();
			checkNormalization();
			checkProfileSelection();
			checkModes();
			if (OneConfigBridge.isAvailable()) {
				checkOneConfig();
			}
			checkReportPlans();
			checkChannelRewriter();
			checkRegistrySyncFilter();
			checkPlaceholders();
			checkProbeSafeText();
			checkJoinNotices();
		});
		resetConfig(context);
	}

	/** Loom deletes the run directory before each run, so the mod starts from a fresh configuration. */
	private static void checkDefaults() {
		ModListConfig config = ModListClient.config();
		check(Files.exists(ModListConfig.defaultPath()), "the config file should be created with the defaults");
		checkEquals(ConfigMode.CUSTOM, config.storedMode(), "default mode");
		check(config.joinNotices(), "join notices should be enabled by default");
		check(config.serverProfiles().isEmpty(), "there should be no server profiles by default");
		Profile profile = config.defaultProfile();
		check(!profile.ignoreUnknownRegistryEntries(), "ignoring unknown registry entries should be off by default");
		checkEquals(ReportMode.ACTUAL, profile.reportMode(), "default report mode");
		checkEquals("", profile.brand(), "default brand");
		check(profile.hiddenNamespaces().isEmpty() && profile.fakeChannels().isEmpty(), "nothing hidden or faked by default");
		check(profile.placeholders().isEmpty(), "no placeholder overrides by default");
		check(profile.protectHiddenTranslations(), "translation protection should be on by default");
		LOGGER.info("Default configuration checked");
	}

	private static void checkSaveAndLoad() {
		Path path = tempConfig("roundtrip", null);
		ModListConfig config = ModListConfig.load(path);
		config.setJoinNotices(false);
		config.defaultProfile().setIgnoreUnknownRegistryEntries(true);
		config.defaultProfile().setReportMode(ReportMode.VANILLA);

		ServerProfile server = config.addServerProfile("MC.Example.com");
		checkEquals(ReportMode.VANILLA, server.profile().reportMode(), "a new server profile copies the default report mode");
		check(server.profile().ignoreUnknownRegistryEntries(), "a new server profile copies the default registry option");
		server.profile().setReportMode(ReportMode.CUSTOM);
		server.profile().setBrand(" lunarclient ");
		server.profile().setPlaceholders(Map.of("minecraft:block", "minecraft:barrier"));
		config.defaultProfile().setReportMode(ReportMode.SERVER);
		checkEquals(ReportMode.CUSTOM, server.profile().reportMode(), "server profiles are independent of the default profile");

		ModListConfig reloaded = ModListConfig.load(path);
		check(!reloaded.joinNotices(), "join notices should be saved");
		checkEquals(ReportMode.SERVER, reloaded.defaultProfile().reportMode(), "saved default report mode");
		check(reloaded.defaultProfile().ignoreUnknownRegistryEntries(), "saved registry option");
		checkEquals(1, reloaded.serverProfiles().size(), "saved server profiles");
		ServerProfile reloadedServer = reloaded.serverProfiles().getFirst();
		checkEquals("MC.Example.com", reloadedServer.address(), "saved address");
		checkEquals("lunarclient", reloadedServer.profile().brand(), "saved, trimmed brand");
		checkEquals(ReportMode.CUSTOM, reloadedServer.profile().reportMode(), "saved server report mode");
		checkEquals(Map.of("minecraft:block", "minecraft:barrier"), reloadedServer.profile().placeholders(), "saved placeholders");

		reloaded.removeServerProfile(reloadedServer);
		checkEquals(0, ModListConfig.load(path).serverProfiles().size(), "removing a server profile should be saved");
		LOGGER.info("Saving and loading checked");
	}

	private static void checkUnreadableFile() {
		String broken = "{ not json";
		Path path = tempConfig("broken", broken);
		ModListConfig config = ModListConfig.load(path);
		checkEquals(ReportMode.ACTUAL, config.defaultProfile().reportMode(), "an unreadable file falls back to the defaults");
		checkEquals(broken, read(path), "an unreadable file must be left untouched");
	}

	private static void checkNormalization() {
		Path path = tempConfig("normalize", """
				{
					"defaultProfile": {
						"reportMode": "bogus",
						"brand": "  vanilla ",
						"hiddenNamespaces": ["minecraft", "Bad Namespace", "xaero", " xaero ", null],
						"fakeChannels": ["examplemod", "examplemod:handshake", "bad entry", ":x", "foo:"],
						"placeholders": {"minecraft:block": "minecraft:barrier", "minecraft:item": " ", "not an id!": "x"}
					},
					"servers": [{"address": "not an address!"}, {"address": "mc.example.com"}, null]
				}
				""");
		Profile profile = ModListConfig.load(path).defaultProfile();
		checkEquals(ReportMode.ACTUAL, profile.reportMode(), "an unknown report mode falls back to the default");
		checkEquals("vanilla", profile.brand(), "the brand is trimmed");
		checkEquals(List.of("xaero"), profile.hiddenNamespaces(), "hidden namespaces keep only hideable, distinct ones");
		checkEquals(List.of("examplemod", "examplemod:handshake"), profile.fakeChannels(), "fake channels keep only valid entries");
		checkEquals(Map.of("minecraft:block", "minecraft:barrier"), profile.placeholders(), "placeholders keep only identifiers, without blank entries");
		checkEquals(1, ModListConfig.load(path).serverProfiles().size(), "server profiles without a valid address are dropped");
		check(!ChannelEntries.isHideable("minecraft"), "the minecraft namespace must not be hideable");
		LOGGER.info("Normalization checked");
	}

	private static void checkProfileSelection() {
		ModListConfig config = ModListConfig.load(tempConfig("selection", null));
		Profile any = config.addServerProfile("mc.example.com").profile();
		config.addServerProfile("mc.example.com:25566");
		Profile ported = config.addServerProfile("play.test.net:25570").profile();
		Profile fallback = config.defaultProfile();

		check(config.effectiveProfileFor("MC.EXAMPLE.COM") == any, "addresses match ignoring case");
		check(config.effectiveProfileFor("mc.example.com:25566") == any, "the first matching profile wins");
		check(config.effectiveProfileFor("play.test.net:25570") == ported, "an address with a port matches that port");
		check(config.effectiveProfileFor("play.test.net") == fallback, "an address with a port does not match the default port");
		check(config.effectiveProfileFor("other.example.net") == fallback, "unmatched servers use the default profile");
		check(config.effectiveProfileFor(null) == fallback, "an unknown address uses the default profile");
		LOGGER.info("Profile selection checked");
	}

	private static void checkModes() {
		Path path = tempConfig("modes", null);
		ModListConfig config = ModListConfig.load(path);
		config.addServerProfile("mc.example.com");
		check(config.effective() == config, "custom mode applies the mod's own settings");
		check(config.isActive() && config.effectiveJoinNotices(), "custom mode shows join notices by default");

		config.setMode(ConfigMode.DISABLED);
		checkEquals(ConfigMode.DISABLED, ModListConfig.load(path).storedMode(), "saved mode");
		check(config.effective() == null && !config.isActive(), "disabled mode applies no settings");
		check(config.effectiveProfileFor("mc.example.com") == null && config.effectiveProfileFor("other.example.net") == null
				&& config.effectiveProfileFor(null) == null, "disabled mode leaves every connection alone");
		check(!config.effectiveJoinNotices(), "disabled mode shows no join notices");
		check(config.joinNotices(), "disabled mode keeps the saved join notices setting");

		config.setMode(ConfigMode.ONECONFIG);
		ModListConfig reloaded = ModListConfig.load(path);
		checkEquals(ConfigMode.ONECONFIG, reloaded.storedMode(), "OneConfig mode is saved");
		if (OneConfigBridge.isAvailable()) {
			checkEquals(ConfigMode.ONECONFIG, reloaded.effectiveMode(), "OneConfig mode applies while OneConfig is installed");
			check(reloaded.effectiveProfileFor("other.example.net") == oneConfig().defaultProfile(),
					"OneConfig's default profile applies to servers without a server profile");
		} else {
			checkEquals(ConfigMode.CUSTOM, reloaded.effectiveMode(), "OneConfig mode falls back to custom without OneConfig");
			check(reloaded.effective() == reloaded, "the fallback applies the mod's own settings");
			check(reloaded.effectiveProfileFor("other.example.net") == reloaded.defaultProfile(),
					"the fallback applies the mod's own default profile");
			checkEquals(ConfigMode.ONECONFIG, ModListConfig.load(path).storedMode(), "the fallback keeps the saved mode");
		}
		check(reloaded.effectiveProfileFor("mc.example.com") == reloaded.serverProfiles().getFirst().profile(),
				"server profiles apply in OneConfig mode, with or without OneConfig");

		ModListConfig misspelled = ModListConfig.load(tempConfig("bad-mode", "{\"mode\": \"bogus\"}"));
		checkEquals(ConfigMode.CUSTOM, misspelled.storedMode(), "an unknown mode falls back to custom");
		LOGGER.info("Configuration modes checked (OneConfig installed: {})", OneConfigBridge.isAvailable());
	}

	/** Only runs with OneConfig installed: OneConfig's values drive the default profile in OneConfig mode. */
	private static void checkOneConfig() {
		ModListSettings settings = oneConfig();
		ProfileSettings profile = settings.defaultProfile();
		try {
			checkEquals(ReportMode.ACTUAL, profile.reportMode(), "OneConfig's default report mode");
			check(settings.joinNotices() && settings.enabled() && profile.protectHiddenTranslations()
					&& !profile.ignoreUnknownRegistryEntries(), "OneConfig's defaults match the mod's own");

			setOneConfig("hiddenNamespaces", new ArrayList<>(List.of("minecraft", "xaero", " xaero ", "Bad Namespace")));
			setOneConfig("fakeChannels", new ArrayList<>(List.of("examplemod", "bad entry", "fakemod:handshake")));
			setOneConfig("blockPlaceholder", " minecraft:barrier ");
			setOneConfig("itemPlaceholder", "not an id!");
			setOneConfig("brand", "  vanilla ");
			setOneConfig("reportMode", 99);
			checkEquals(List.of("xaero"), profile.hiddenNamespaces(), "OneConfig's hidden namespaces are cleaned");
			checkEquals(List.of("examplemod", "fakemod:handshake"), profile.fakeChannels(), "OneConfig's fake channels are cleaned");
			checkEquals(Map.of("minecraft:block", "minecraft:barrier"), profile.placeholders(), "OneConfig's placeholders are cleaned");
			checkEquals("vanilla", profile.brand(), "OneConfig's brand is trimmed");
			checkEquals(ReportMode.ACTUAL, profile.reportMode(), "an out-of-range report mode falls back to the default");

			// The dropdown's options must follow ReportMode's order, which its index maps to.
			List<String> options = dropdownOptions(settings, "reportMode");
			checkEquals(Arrays.stream(ReportMode.values()).map(mode -> SettingKeys.REPORT_MODE_PREFIX + mode.getSerializedName()).toList(),
					options, "OneConfig's report mode options");
			for (ReportMode mode : ReportMode.values()) {
				setOneConfig("reportMode", mode.ordinal());
				checkEquals(mode, profile.reportMode(), "report mode at OneConfig's index " + mode.ordinal());
			}

			ModListConfig config = ModListConfig.load(tempConfig("oneconfig", null));
			config.setMode(ConfigMode.ONECONFIG);
			Profile copy = config.addServerProfile("copy.example.net").profile();
			checkEquals(ReportMode.CUSTOM, copy.reportMode(), "a new server profile copies OneConfig's report mode");
			checkEquals("vanilla", copy.brand(), "a new server profile copies OneConfig's brand");

			setOneConfig("enabled", false);
			check(config.effective() == null && !config.isActive(), "the toggle on OneConfig's card turns the mod off");
			check(config.effectiveProfileFor("copy.example.net") == null, "server profiles do not apply while it is off");
		} finally {
			resetOneConfig();
		}
		LOGGER.info("OneConfig settings checked");
	}

	/** Reads the options of a OneConfig dropdown by reflection, so the tests compile without OneConfig. */
	private static List<String> dropdownOptions(final Object settings, final String field) {
		try {
			for (Annotation annotation : settings.getClass().getField(field).getAnnotations()) {
				if (annotation.annotationType().getSimpleName().equals("Dropdown")) {
					return List.of((String[]) annotation.annotationType().getMethod("options").invoke(annotation));
				}
			}
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("cannot read the options of " + field, e);
		}
		throw new AssertionError(field + " is not a dropdown");
	}

	private static void checkReportPlans() {
		Profile profile = ModListConfig.load(tempConfig("plans", null)).defaultProfile();
		Set<Identifier> server = Set.of(id("fabric", "registry/sync/complete"), MIRRORED);

		ReportPlan actual = ReportPlan.of(profile);
		check(actual.brand() == null, "the actual mode keeps the brand");
		check(!actual.isHidden("xaero", server) && actual.fakeChannels(server).isEmpty(), "the actual mode changes nothing");

		profile.setReportMode(ReportMode.VANILLA);
		ReportPlan vanilla = ReportPlan.of(profile);
		checkEquals("vanilla", vanilla.brand(), "vanilla mode brand");
		check(vanilla.isHidden("fabric", server) && vanilla.isHidden("xaero", server), "vanilla mode hides every mod");
		check(!vanilla.isHidden("minecraft", server), "vanilla mode keeps the minecraft namespace");
		check(vanilla.fakeChannels(server).isEmpty(), "vanilla mode fakes nothing");

		profile.setReportMode(ReportMode.SERVER);
		ReportPlan mimicServer = ReportPlan.of(profile);
		check(mimicServer.brand() == null, "server mode keeps the brand");
		check(mimicServer.isHidden("xaero", server), "server mode hides mods the server does not have");
		check(!mimicServer.isHidden("fabric", server), "server mode keeps mods the server has");
		checkEquals(server, mimicServer.fakeChannels(server), "server mode claims every server channel");

		profile.setReportMode(ReportMode.ACTUAL);
		profile.setHiddenNamespaces(List.of("xaero"));
		profile.setFakeChannels(List.of("mirrored", FAKE_HANDSHAKE.toString()));
		profile.setBrand("custom");
		checkEquals(ReportMode.ACTUAL, profile.reportMode(), "custom settings do not change the report mode");
		check(ReportPlan.of(profile).brand() == null && !ReportPlan.of(profile).isHidden("xaero", server),
				"custom settings only apply in custom mode");
		profile.setReportMode(ReportMode.CUSTOM);
		ReportPlan custom = ReportPlan.of(profile);
		checkEquals("custom", custom.brand(), "custom brand");
		check(custom.isHidden("xaero", server) && !custom.isHidden("fabric", server), "custom mode hides its list");
		checkEquals(Set.of(MIRRORED, FAKE_HANDSHAKE), custom.fakeChannels(server), "custom fake channels");

		profile.setReportMode(ReportMode.VANILLA);
		profile.setReportMode(ReportMode.ACTUAL);
		checkEquals(List.of("xaero"), profile.hiddenNamespaces(), "switching modes keeps the custom lists");
		checkEquals("custom", profile.brand(), "switching modes keeps the custom brand");
		LOGGER.info("Report plans checked");
	}

	private static void checkChannelRewriter() {
		Profile profile = ModListConfig.load(tempConfig("rewriter", null)).defaultProfile();
		profile.setReportMode(ReportMode.CUSTOM);
		profile.setHiddenNamespaces(List.of("xaero"));
		profile.setFakeChannels(List.of("mirrored"));
		List<Identifier> channels = List.of(id("fabric", "registry/sync"), id("xaero", "a"), id("xaero", "b"), id("other", "c"));
		Set<Identifier> server = Set.of(MIRRORED);

		ChannelRewriter.Result result = ChannelRewriter.rewrite(channels, ReportPlan.of(profile), server);
		checkEquals(List.of(id("fabric", "registry/sync"), id("other", "c"), MIRRORED), result.channels(), "rewritten channels");
		checkEquals(Set.of(id("xaero", "a"), id("xaero", "b")), result.removed(), "removed channels");
		checkEquals(Set.of(MIRRORED), result.added(), "added channels");

		profile.setReportMode(ReportMode.VANILLA);
		check(ChannelRewriter.rewrite(channels, ReportPlan.of(profile), server).channels().isEmpty(),
				"vanilla mode registers no channel");
		LOGGER.info("Channel rewriting checked");
	}

	private static void checkRegistrySyncFilter() {
		Identifier things = id("test", "things");
		Identifier same = id("test", "same");
		Identifier missing = id("test", "missing");
		Identifier optional = id("test", "optional");
		Map<Identifier, Object2IntMap<Identifier>> remote = new LinkedHashMap<>();
		remote.put(things, ids(Map.of(id("test", "a"), 0, id("test", "ghost"), 1, id("test", "b"), 2,
				id("test", "c"), 3, id("test", "ghost2"), 4)));
		remote.put(same, ids(Map.of(id("test", "x"), 0, id("test", "y"), 1)));
		remote.put(missing, ids(Map.of(id("test", "z"), 0)));
		remote.put(optional, ids(Map.of(id("test", "o"), 0)));
		Map<Identifier, EnumSet<RegistryAttribute>> attributes = new LinkedHashMap<>();
		remote.keySet().forEach(registry -> attributes.put(registry, EnumSet.of(RegistryAttribute.SYNCED)));
		attributes.put(optional, EnumSet.of(RegistryAttribute.SYNCED, RegistryAttribute.OPTIONAL));
		Map<Identifier, List<Identifier>> local = Map.of(
				things, List.of(id("test", "a"), id("test", "b"), id("test", "c"), id("test", "client_only")),
				same, List.of(id("test", "x"), id("test", "y")));

		RegistrySyncFilter.Result result = RegistrySyncFilter.filter(remote, attributes, local::get);
		checkEquals(Map.of(things, List.of(id("test", "ghost"), id("test", "ghost2"))), result.droppedEntries(), "dropped entries");
		checkEquals(List.of(missing), result.droppedRegistries(), "dropped registries, except optional ones");
		checkEquals(Set.of(things, same), result.registryMap().keySet(), "kept registries");
		checkEquals(ids(Map.of(id("test", "a"), 0, id("test", "b"), 2, id("test", "c"), 3, id("test", "client_only"), 5)),
				result.registryMap().get(things), "known entries keep the server's IDs; client-only ones go above them");
		check(result.registryMap().get(same) == remote.get(same), "registries without unknown entries are passed on unchanged");
		checkEquals(Map.of(things, 5), result.requiredSizes(), "raw IDs to fill");
		checkEquals(3, result.droppedEntryCount() + result.droppedRegistries().size(), "ignored content count");
		LOGGER.info("Registry sync filtering checked");
	}

	private static void checkPlaceholders() {
		Holder.Reference<?> block = Placeholders.resolve(BuiltInRegistries.BLOCK, Map.of());
		check(block != null && block.value() == Blocks.AIR, "air is the built-in block placeholder");
		block = Placeholders.resolve(BuiltInRegistries.BLOCK, Map.of("minecraft:block", "minecraft:barrier"));
		check(block != null && block.value() == Blocks.BARRIER, "a configured block placeholder is used");
		block = Placeholders.resolve(BuiltInRegistries.BLOCK, Map.of("minecraft:block", "minecraft:nope"));
		check(block != null && block.value() == Blocks.AIR, "an unknown configured placeholder falls back to the built-in one");
		Holder.Reference<?> entity = Placeholders.resolve(BuiltInRegistries.ENTITY_TYPE, Map.of());
		check(entity != null && entity.value() == EntityTypes.MARKER, "the marker is the built-in entity placeholder");
		check(Placeholders.resolve(BuiltInRegistries.SOUND_EVENT, Map.of()) == null,
				"registries without a built-in or default entry have no resolved placeholder");
		check(Placeholders.isValid(Registries.ITEM, "minecraft:barrier"), "existing items are valid placeholders");
		check(!Placeholders.isValid(Registries.ITEM, "minecraft:nope") && !Placeholders.isValid(Registries.ITEM, "not an id!"),
				"unknown items are invalid placeholders");
		LOGGER.info("Placeholders checked");
	}

	private static void checkProbeSafeText() {
		String secret = TestPayloads.NAMESPACE + ".secret";
		Set<String> hidden = Set.of(secret, "key.modlist.open_settings");
		checkEquals("Secret Translation", Component.translatable(secret).getString(), "the test translation resolves");
		checkEquals(secret, ProbeSafeText.getString(Component.translatable(secret), hidden), "a hidden key stays unresolved");
		checkEquals("Fallback", ProbeSafeText.getString(Component.translatableWithFallback(secret, "Fallback"), hidden),
				"a hidden key shows its fallback");
		checkEquals("<" + secret + "> hi", ProbeSafeText.getString(
				Component.translatable("chat.type.text", Component.translatable(secret), "hi"), hidden),
				"hidden keys in arguments stay unresolved while vanilla keys resolve");
		checkEquals("a" + secret, ProbeSafeText.getString(Component.literal("a").append(Component.translatable(secret)), hidden),
				"hidden keys in siblings stay unresolved");
		checkEquals("key.modlist.open_settings", ProbeSafeText.getString(Component.keybind("key.modlist.open_settings"), hidden),
				"a hidden key binding shows its name");
		checkEquals("Secret Translation", ProbeSafeText.getString(Component.translatable(secret), Set.of()),
				"nothing changes without hidden keys");
		check(HiddenTranslations.currentKeys().isEmpty(), "nothing is hidden outside a server connection");
		LOGGER.info("Translation probing protection checked");
	}

	private static void checkJoinNotices() {
		Set<Identifier> server = Set.of(MIRRORED, id("fabric", "registry/sync/complete"), id("c", "register"));
		checkEquals(Set.of("mirrored"), JoinNotices.serverOnly(server, Set.of("fabric", "xaero"), Set.of("modlist"), null),
				"namespaces only the server has");
		checkEquals(Set.of("xaero"), JoinNotices.clientOnly(Set.of("fabric", "xaero", "minecraft"), server),
				"namespaces only the client has");
		LOGGER.info("Join notice differences checked");
	}

	private static Path tempConfig(final String name, final @Nullable String content) {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("modlist-test-" + name + ".json");
		try {
			Files.deleteIfExists(path);
			if (content != null) {
				Files.writeString(path, content);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return path;
	}

	private static String read(final Path path) {
		try {
			return Files.readString(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static Object2IntMap<Identifier> ids(final Map<Identifier, Integer> entries) {
		Object2IntMap<Identifier> map = new Object2IntLinkedOpenHashMap<>();
		entries.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(entry -> map.put(entry.getKey(), entry.getValue().intValue()));
		return map;
	}

	private static Identifier id(final String namespace, final String path) {
		return Identifier.fromNamespaceAndPath(namespace, path);
	}
}
