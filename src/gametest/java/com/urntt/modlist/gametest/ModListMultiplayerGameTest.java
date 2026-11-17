package com.urntt.modlist.gametest;

import static com.urntt.modlist.gametest.GameTestSupport.LOGGER;
import static com.urntt.modlist.gametest.GameTestSupport.check;
import static com.urntt.modlist.gametest.GameTestSupport.checkEquals;
import static com.urntt.modlist.gametest.GameTestSupport.configure;
import static com.urntt.modlist.gametest.GameTestSupport.resetConfig;
import static com.urntt.modlist.gametest.GameTestSupport.resetProfile;

import com.urntt.modlist.ModListClient;
import com.urntt.modlist.config.ModListConfigScreen;
import com.urntt.modlist.config.Profile;
import com.urntt.modlist.config.ReportMode;
import com.urntt.modlist.config.ServerReportScreen;
import com.urntt.modlist.gametest.mixin.DisconnectedScreenAccessor;
import com.urntt.modlist.registry.RegistrySyncFilter;
import com.urntt.modlist.session.ConnectionSession;
import com.urntt.modlist.session.LocalChannels;
import com.urntt.modlist.session.SessionManager;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;

/**
 * Connects to a local dedicated server, which runs in the same JVM, and checks on the server's side what the client
 * reports in each report mode, and on the client's side how it handles registry entries it does not have.
 */
@SuppressWarnings("UnstableApiUsage")
public final class ModListMultiplayerGameTest implements FabricClientGameTest {
	/** The address the test server profile matches; the client connects to {@code localhost:<port>}. */
	private static final String ADDRESS = "localhost";
	private static final Identifier FAKE_HANDSHAKE = Identifier.fromNamespaceAndPath("fakemod", "handshake");
	private static final Identifier BRAND = Identifier.withDefaultNamespace("brand");
	private static final String SECRET = TestPayloads.NAMESPACE + ".secret";
	private static final String CUSTOM_BRAND = "lunarclient";

	@Override
	public void runTest(final ClientGameTestContext context) {
		resetConfig(context);
		try (TestDedicatedServerContext server = context.worldBuilder().createServer()) {
			checkCustomServerProfile(context, server);
			checkDefaultProfile(context, server);
			checkMimicVanilla(context, server);
			checkMimicServer(context, server);
			checkRefusedWithHint(context, server);
		}
		GhostEntries.setEnabled(false);
		resetConfig(context);
	}

	/**
	 * A server profile in custom mode: a custom brand, a hidden namespace, fake channels, and ignored registry entries
	 * shown as configured placeholders.
	 */
	private static void checkCustomServerProfile(final ClientGameTestContext context,
			final TestDedicatedServerContext server) {
		configure(context, config -> {
			Profile profile = config.addServerProfile(ADDRESS).profile();
			profile.setIgnoreUnknownRegistryEntries(true);
			profile.setPlaceholders(Map.of("minecraft:block", "minecraft:barrier", "minecraft:item", "minecraft:barrier"));
			profile.setReportMode(ReportMode.CUSTOM);
			profile.setBrand(CUSTOM_BRAND);
			profile.setHiddenNamespaces(List.of(TestPayloads.NAMESPACE));
			profile.setFakeChannels(List.of(TestPayloads.SERVER_NAMESPACE, FAKE_HANDSHAKE.toString()));
		});
		GhostEntries.setEnabled(true);
		ServerRecorder.reset();

		GhostEntries.Layout layout;
		Item lastItem;
		try (TestDedicatedServerConnection connection = server.connect()) {
			connection.waitForChunksRender();
			layout = Objects.requireNonNull(GhostEntries.layout(), "the server should have sent the ghost entries");
			lastItem = BuiltInRegistries.ITEM.getValue(layout.lastItem());

			checkEquals(CUSTOM_BRAND, ServerRecorder.brand(), "brand the server received");
			check(!canSend(server, TestPayloads.ClientChannel.TYPE.id()), "a hidden channel must not reach the server");
			check(canSend(server, TestPayloads.Mirrored.TYPE.id()), "a fake namespace should mirror the server's channel");
			check(canSend(server, FAKE_HANDSHAKE), "a fake channel identifier should be claimed");
			check(canSend(server, TestPayloads.ClientOnly.TYPE.id()), "channels that are not hidden should be reported");
			checkProbe(context, 0, "with the probe's namespace hidden");

			context.runOnClient(client -> {
				int lastId = layout.lastId();
				checkEquals(Items.BARRIER, BuiltInRegistries.ITEM.byId(lastId), "item at the raw ID of an unknown entry");
				checkEquals(lastId + 1, BuiltInRegistries.ITEM.getId(lastItem), "raw ID of the item the server moved");
				checkEquals(Items.BARRIER, BuiltInRegistries.ITEM.byId(lastId + 2), "item at the server's last raw ID");
				check(BuiltInRegistries.ITEM.stream().count() > lastId, "the item registry should still iterate");
				checkEquals(Blocks.BARRIER.defaultBlockState(), Block.BLOCK_STATE_REGISTRY.byId(Block.BLOCK_STATE_REGISTRY.size() + 10),
						"block state at an unknown ID");

				ConnectionSession session = Objects.requireNonNull(SessionManager.current(), "session");
				RegistrySyncFilter.Result ignored = Objects.requireNonNull(session.ignoredRegistryContent(), "ignored content");
				checkEquals(List.of(GhostEntries.MIDDLE_ITEM, GhostEntries.TAIL_ITEM),
						ignored.droppedEntries().get(BuiltInRegistries.ITEM.key().identifier()), "ignored items");
				checkEquals(List.of(GhostEntries.UNKNOWN_REGISTRY), ignored.droppedRegistries(), "ignored registries");
				LOGGER.info("Joined with {} ignored registry entries and {} ignored registries; hidden {}, faked {}",
						ignored.droppedEntryCount(), ignored.droppedRegistries().size(), session.hiddenChannels(),
						session.fakedChannels());
			});

			List<String> lines = signRoundTrip(context, server);
			LOGGER.info("Sign lines the server received with the test namespace hidden: {}", lines);
			checkEquals(SECRET, lines.getFirst(), "a hidden mod's translation key sent back from a sign");
			checkEquals("Stone", lines.get(1), "a vanilla translation sent back from a sign");

			context.getInput().setCursorPos(0, 0);
			context.takeScreenshot("modlist-join-notices");
			context.setScreen(() -> new ServerReportScreen(new ModListConfigScreen(null), ModListClient.config()));
			context.takeScreenshot("modlist-server-report");
			context.setScreen(() -> null);
		}

		waitForDisconnect(context, server);
		context.runOnClient(client -> {
			checkEquals(layout.lastId(), BuiltInRegistries.ITEM.getId(lastItem), "the item's raw ID after disconnecting");
			checkEquals(lastItem, BuiltInRegistries.ITEM.byId(layout.lastId()), "the item at its raw ID after disconnecting");
			check(Block.BLOCK_STATE_REGISTRY.byId(Block.BLOCK_STATE_REGISTRY.size() + 10) == null,
					"unknown block state IDs resolve to nothing after disconnecting");
		});
		GhostEntries.setEnabled(false);
		LOGGER.info("Custom server profile checked");
	}

	/** Without a matching server profile, the default profile reports the real mods. */
	private static void checkDefaultProfile(final ClientGameTestContext context, final TestDedicatedServerContext server) {
		configure(context, config -> config.serverProfiles().forEach(config::removeServerProfile));
		ServerRecorder.reset();
		try (TestDedicatedServerConnection connection = server.connect()) {
			connection.waitForChunksRender();
			checkEquals("fabric", ServerRecorder.brand(), "brand the server received");
			check(canSend(server, TestPayloads.ClientChannel.TYPE.id()), "the real channel should be reported");
			check(!canSend(server, TestPayloads.Mirrored.TYPE.id()) && !canSend(server, FAKE_HANDSHAKE),
					"nothing should be faked");
			checkProbe(context, 1, "with nothing hidden");
			check(context.computeOnClient(client -> Objects.requireNonNull(SessionManager.current()).ignoredRegistryContent() == null),
					"nothing should be ignored");
			List<String> lines = signRoundTrip(context, server);
			checkEquals("Secret Translation", lines.getFirst(), "a translation of a mod that is not hidden");
		}
		waitForDisconnect(context, server);
		LOGGER.info("Default profile checked");
	}

	/** "Mimic Vanilla" reports the vanilla brand and nothing else. */
	private static void checkMimicVanilla(final ClientGameTestContext context, final TestDedicatedServerContext server) {
		configure(context, config -> config.defaultProfile().setReportMode(ReportMode.VANILLA));
		ServerRecorder.reset();
		try (TestDedicatedServerConnection connection = server.connect()) {
			connection.waitForChunksRender();
			checkEquals("vanilla", ServerRecorder.brand(), "brand the server received");
			checkEquals(0, ServerRecorder.registrations(), "channel registrations the server received");
			Set<Identifier> visible = context.computeOnClient(client -> LocalChannels.all()).stream()
					.filter(channel -> canSend(server, channel))
					.collect(Collectors.toSet());
			checkEquals(Set.of(), visible, "client channels the server can send to");
			checkProbe(context, 0, "when mimicking vanilla");
			checkEquals(Set.of(BRAND), ServerRecorder.payloadTypes(), "custom payloads the server received");
		}
		waitForDisconnect(context, server);
		LOGGER.info("Mimic Vanilla checked");
	}

	/** "Mimic Server" claims the server's channels and hides the client's other mods. */
	private static void checkMimicServer(final ClientGameTestContext context, final TestDedicatedServerContext server) {
		configure(context, config -> config.defaultProfile().setReportMode(ReportMode.SERVER));
		ServerRecorder.reset();
		try (TestDedicatedServerConnection connection = server.connect()) {
			connection.waitForChunksRender();
			checkEquals("fabric", ServerRecorder.brand(), "brand the server received");
			check(canSend(server, TestPayloads.Mirrored.TYPE.id()), "the server's own channel should be claimed");
			check(!canSend(server, TestPayloads.ClientOnly.TYPE.id()), "a namespace the server lacks should be hidden");
			check(canSend(server, TestPayloads.ClientChannel.TYPE.id()), "a namespace the server has should be reported");
		}
		waitForDisconnect(context, server);
		LOGGER.info("Mimic Server checked");
	}

	/** Without ignoring unknown registry entries, Fabric API refuses the server, and the reason mentions the option. */
	private static void checkRefusedWithHint(final ClientGameTestContext context, final TestDedicatedServerContext server) {
		configure(context, config -> resetProfile(config.defaultProfile()));
		GhostEntries.setEnabled(true);
		int port = server.computeOnServer(MinecraftServer::getPort);
		context.runOnClient(client -> ConnectScreen.startConnecting(new TitleScreen(), client,
				ServerAddress.parseString(ADDRESS + ":" + port),
				new ServerData(ADDRESS, ADDRESS + ":" + port, ServerData.Type.OTHER), false, null));
		context.waitForScreen(DisconnectedScreen.class);
		String reason = context.computeOnClient(client ->
				((DisconnectedScreenAccessor) Objects.requireNonNull(client.gui.screen())).modlistGametest$details().reason().getString());
		LOGGER.info("Disconnect reason without ignoring: {}", reason.replace('\n', ' '));
		String hint = context.computeOnClient(client -> Component.translatable("message.modlist.ignore_hint").getString());
		check(reason.contains(hint), "the disconnect reason should mention the option to ignore unknown entries");
		context.getInput().setCursorPos(0, 0);
		context.takeScreenshot("modlist-refused-with-hint");
		context.setScreen(() -> null);
		GhostEntries.setEnabled(false);
		server.waitFor(s -> s.getPlayerList().getPlayers().isEmpty());
		LOGGER.info("Refusal hint checked");
	}

	/** Sends a probe payload and waits until the server has handled {@code expected} of them. */
	private static void checkProbe(final ClientGameTestContext context, final int expected, final String situation) {
		context.runOnClient(client -> ClientPlayNetworking.send(new TestPayloads.Probe()));
		context.waitTicks(10);
		if (expected > 0) {
			context.waitFor(client -> ServerRecorder.probes() >= expected);
		}
		checkEquals(expected, ServerRecorder.probes(), "probe payloads the server handled " + situation);
	}

	private static boolean canSend(final TestDedicatedServerContext server, final Identifier channel) {
		return server.computeOnServer(s -> ServerPlayNetworking.canSend(onlyPlayer(s), channel));
	}

	private static ServerPlayer onlyPlayer(final MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	/**
	 * Places a sign whose first line is the test mod's translation and whose second line is a vanilla translation, lets
	 * the client open and close its editor, and returns the lines the server received back.
	 */
	private static List<String> signRoundTrip(final ClientGameTestContext context, final TestDedicatedServerContext server) {
		BlockPos pos = context.computeOnClient(client -> client.player.blockPosition().offset(2, 0, 0));
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			level.setBlockAndUpdate(pos, Blocks.OAK_SIGN.defaultBlockState());
			List<Component> messages = List.of(Component.translatable(SECRET), Component.translatable("block.minecraft.stone"),
					Component.empty(), Component.empty());
			((SignBlockEntity) Objects.requireNonNull(level.getBlockEntity(pos)))
					.setText(new SignText(messages, messages, DyeColor.BLACK, false), SignTextSlot.FRONT);
		});
		context.waitFor(client -> client.level.getBlockEntity(pos) instanceof SignBlockEntity sign
				&& !sign.getText(SignTextSlot.FRONT).getMessages(false).getFirst().getString().isEmpty());

		server.runOnServer(s -> ((SignBlock) Blocks.OAK_SIGN).openTextEdit(onlyPlayer(s),
				(SignBlockEntity) Objects.requireNonNull(s.overworld().getBlockEntity(pos)), SignTextSlot.FRONT));
		context.waitForScreen(SignEditScreen.class);
		context.setScreen(() -> null);

		server.waitFor(s -> !(firstLine(s, pos).getContents() instanceof TranslatableContents));
		List<String> lines = server.computeOnServer(s -> ((SignBlockEntity) Objects.requireNonNull(s.overworld().getBlockEntity(pos)))
				.getText(SignTextSlot.FRONT).getMessages(false).stream().map(Component::getString).toList());
		server.runOnServer(s -> s.overworld().removeBlock(pos, false));
		return lines;
	}

	private static Component firstLine(final MinecraftServer server, final BlockPos pos) {
		return ((SignBlockEntity) Objects.requireNonNull(server.overworld().getBlockEntity(pos)))
				.getText(SignTextSlot.FRONT).getMessages(false).getFirst();
	}

	private static void waitForDisconnect(final ClientGameTestContext context, final TestDedicatedServerContext server) {
		context.waitFor(client -> SessionManager.current() == null);
		server.waitFor(s -> s.getPlayerList().getPlayers().isEmpty());
	}
}
