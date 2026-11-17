package com.urntt.modlist.gametest;

import static com.urntt.modlist.gametest.GameTestSupport.LOGGER;
import static com.urntt.modlist.gametest.GameTestSupport.check;
import static com.urntt.modlist.gametest.GameTestSupport.checkEquals;
import static com.urntt.modlist.gametest.GameTestSupport.configure;
import static com.urntt.modlist.gametest.GameTestSupport.resetConfig;
import static com.urntt.modlist.gametest.GameTestSupport.switchLanguage;

import com.mojang.blaze3d.platform.InputConstants;
import com.urntt.modlist.ModListClient;
import com.urntt.modlist.config.HiddenModsScreen;
import com.urntt.modlist.config.ModListConfig;
import com.urntt.modlist.config.ModListConfigScreen;
import com.urntt.modlist.config.PlaceholdersScreen;
import com.urntt.modlist.config.Profile;
import com.urntt.modlist.config.ProfileScreen;
import com.urntt.modlist.config.ReportMode;
import com.urntt.modlist.config.ServerProfile;
import com.urntt.modlist.config.ServerProfilesScreen;
import com.urntt.modlist.config.StringListScreen;
import com.urntt.modlist.session.ChannelEntries;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

/**
 * Drives the configuration screens: adding and removing server profiles, refreshing lists when a screen is shown
 * again, showing settings according to the report mode, and section titles that cannot take focus. Takes screenshots
 * of every screen in both languages.
 */
@SuppressWarnings("UnstableApiUsage")
public final class ModListScreenGameTest implements FabricClientGameTest {
	private static final MouseButtonInfo LEFT_CLICK = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0);

	@Override
	public void runTest(final ClientGameTestContext context) {
		resetConfig(context);
		checkServerProfilesScreen(context);
		resetConfig(context);
		checkProfileScreen(context);
		resetConfig(context);
		takeScreenshots(context);
		resetConfig(context);
	}

	private static void checkServerProfilesScreen(final ClientGameTestContext context) {
		ModListConfig config = context.computeOnClient(client -> ModListClient.config());
		ServerProfilesScreen screen = context.computeOnClient(client ->
				new ServerProfilesScreen(new ModListConfigScreen(null), config));
		context.setScreen(() -> screen);

		addAddress(context, "play.example.net");
		context.runOnClient(client -> {
			check(client.gui.screen() == screen, "adding a server profile should stay on the list");
			checkEquals("", only(screen, EditBox.class).getValue(), "address box after adding");
			checkEquals(List.of("play.example.net"), addresses(config), "server profiles after adding");
			checkEquals(1, rows(screen), "rows after adding");
		});
		addAddress(context, "PLAY.example.net:25566");
		context.runOnClient(client -> checkEquals(2, rows(screen), "rows after adding a second profile"));

		context.runOnClient(client -> press(rowWidgets(screen, 0), "options.modlist.list.remove"));
		context.waitTicks(2);
		context.runOnClient(client -> {
			checkEquals(List.of("PLAY.example.net:25566"), addresses(config), "server profiles after removing");
			checkEquals(1, rows(screen), "rows after removing");
			checkEquals(1, count(screen.children(), AbstractSelectionList.class), "lists on the screen after removing");
		});

		// Vanilla only repositions a screen that is shown again, so the list must refresh itself.
		ServerProfile added = context.computeOnClient(client -> config.addServerProfile("other.example.net"));
		context.setScreen(() -> new ProfileScreen(screen, added.profile(), added));
		context.setScreen(() -> screen);
		context.runOnClient(client -> {
			checkEquals(2, rows(screen), "rows after returning to the list");
			checkEquals(1, count(screen.children(), AbstractSelectionList.class), "lists on the screen after returning");
		});
		context.setScreen(() -> null);
		LOGGER.info("Server profile list checked");
	}

	private static void checkProfileScreen(final ClientGameTestContext context) {
		configure(context, config -> config.defaultProfile().setReportMode(ReportMode.SERVER));
		ModListConfig config = context.computeOnClient(client -> ModListClient.config());
		ProfileScreen screen = context.computeOnClient(client ->
				new ProfileScreen(new ModListConfigScreen(null), config.defaultProfile(), null));
		context.setScreen(() -> screen);

		context.runOnClient(client -> {
			List<AbstractWidget> widgets = listWidgets(screen);
			check(widgets.stream().noneMatch(widget -> widget instanceof FocusableTextWidget),
					"section titles must not be focusable");
			check(count(widgets, EditBox.class) == 0, "custom settings should be hidden in server mode");
			check(hasButton(widgets, "options.modlist.protect_hidden_translations"),
					"translation protection should be shown when the mode hides mods");
			pressReportMode(widgets);
		});
		context.waitTicks(2);
		context.runOnClient(client -> {
			checkEquals(ReportMode.CUSTOM, config.defaultProfile().reportMode(), "report mode after one click");
			List<AbstractWidget> widgets = listWidgets(screen);
			checkEquals(1, count(widgets, EditBox.class), "brand boxes in custom mode");
			check(hasButton(widgets, "options.modlist.hidden_mods") && hasButton(widgets, "options.modlist.fake_mods"),
					"custom mode should show the hidden and fake mod settings");
			checkEquals(1, count(screen.children(), AbstractSelectionList.class), "option lists on the screen");
			pressReportMode(widgets);
		});
		context.waitTicks(2);
		context.runOnClient(client -> {
			checkEquals(ReportMode.ACTUAL, config.defaultProfile().reportMode(), "report mode after another click");
			List<AbstractWidget> widgets = listWidgets(screen);
			check(count(widgets, EditBox.class) == 0 && !hasButton(widgets, "options.modlist.hidden_mods"),
					"custom settings should be hidden when reporting unchanged mods");
			check(!hasButton(widgets, "options.modlist.protect_hidden_translations"),
					"translation protection should be hidden when nothing is hidden");
		});
		context.setScreen(() -> null);
		LOGGER.info("Profile screen checked");
	}

	private static void takeScreenshots(final ClientGameTestContext context) {
		configure(context, config -> {
			Profile profile = config.defaultProfile();
			profile.setIgnoreUnknownRegistryEntries(true);
			profile.setPlaceholders(Map.of("minecraft:block", "minecraft:barrier"));
			ServerProfile server = config.addServerProfile("mc.example.com");
			server.profile().setReportMode(ReportMode.CUSTOM);
			server.profile().setBrand("vanilla");
			server.profile().setHiddenNamespaces(List.of(TestPayloads.NAMESPACE));
			server.profile().setFakeChannels(List.of(TestPayloads.SERVER_NAMESPACE, "fakemod:handshake"));
			config.addServerProfile("mc.example.com:25566");
		});
		// Keep the cursor away from the widgets so no tooltip covers them.
		context.getInput().setCursorPos(0, 0);
		takeScreenshots(context, "en_us");
		switchLanguage(context, "zh_cn");
		takeScreenshots(context, "zh_cn");
		switchLanguage(context, "en_us");
	}

	private static void takeScreenshots(final ClientGameTestContext context, final String language) {
		ModListConfig config = context.computeOnClient(client -> ModListClient.config());
		ServerProfile server = config.serverProfiles().getFirst();

		screenshot(context, "main-" + language, () -> new ModListConfigScreen(null));
		screenshot(context, "default-profile-" + language, () -> new ProfileScreen(new ModListConfigScreen(null),
				config.defaultProfile(), null));
		screenshot(context, "server-profile-" + language, () -> new ProfileScreen(new ModListConfigScreen(null),
				server.profile(), server));
		context.getInput().scroll(-20);
		context.waitTick();
		context.takeScreenshot("modlist-server-profile-bottom-" + language);
		screenshot(context, "server-profiles-" + language, () -> new ServerProfilesScreen(new ModListConfigScreen(null), config));
		screenshot(context, "placeholders-" + language, () -> new PlaceholdersScreen(new ModListConfigScreen(null),
				config.defaultProfile()));
		screenshot(context, "hidden-mods-" + language, () -> new HiddenModsScreen(new ModListConfigScreen(null),
				server.profile()));
		screenshot(context, "fake-mods-" + language, () -> new StringListScreen(new ModListConfigScreen(null),
				Component.translatable("options.modlist.fake_mods.title"),
				Component.translatable("options.modlist.fake_mods.description"),
				Component.translatable("options.modlist.fake_mods.hint"),
				Component.translatable("options.modlist.fake_mods.entry"),
				List.of("mirrored", "fakemod:handshake", "bad entry"), ChannelEntries::isValidFakeEntry, entries -> {
				}));
		context.setScreen(() -> null);
	}

	private static void screenshot(final ClientGameTestContext context, final String name, final Supplier<Screen> screen) {
		context.setScreen(screen::get);
		context.takeScreenshot("modlist-" + name);
	}

	/** Types {@code address} into the server list's address box and presses "Add". */
	private static void addAddress(final ClientGameTestContext context, final String address) {
		context.runOnClient(client -> only(client.gui.screen(), EditBox.class).setValue(address));
		context.clickScreenButton("options.modlist.list.add");
	}

	private static List<String> addresses(final ModListConfig config) {
		return config.serverProfiles().stream().map(ServerProfile::address).toList();
	}

	/** The rows of the screen's only list. */
	private static int rows(final Screen screen) {
		return only(screen, AbstractSelectionList.class).children().size();
	}

	/** The widgets of one row of the screen's only list. */
	private static List<AbstractWidget> rowWidgets(final Screen screen, final int row) {
		AbstractSelectionList<?> list = only(screen, AbstractSelectionList.class);
		return widgetsOf((ContainerEventHandler) list.children().get(row));
	}

	/** The widgets in all rows of every list on the screen. */
	private static List<AbstractWidget> listWidgets(final Screen screen) {
		List<AbstractWidget> widgets = new ArrayList<>();
		for (GuiEventListener child : screen.children()) {
			if (child instanceof AbstractSelectionList<?> list) {
				for (GuiEventListener entry : list.children()) {
					widgets.addAll(widgetsOf((ContainerEventHandler) entry));
				}
			}
		}
		return widgets;
	}

	private static List<AbstractWidget> widgetsOf(final ContainerEventHandler entry) {
		return entry.children().stream()
				.filter(AbstractWidget.class::isInstance)
				.map(AbstractWidget.class::cast)
				.toList();
	}

	/** Presses the button whose label starts with the translation of {@code key}. */
	private static void press(final List<AbstractWidget> widgets, final String key) {
		String label = Component.translatable(key).getString();
		AbstractButton button = widgets.stream()
				.filter(widget -> widget instanceof AbstractButton && widget.getMessage().getString().startsWith(label))
				.map(AbstractButton.class::cast)
				.findFirst()
				.orElseThrow(() -> new AssertionError("no button " + label));
		button.onPress(LEFT_CLICK);
	}

	private static void pressReportMode(final List<AbstractWidget> widgets) {
		widgets.stream()
				.filter(widget -> widget instanceof CycleButton<?> cycle && cycle.getValue() instanceof ReportMode)
				.map(CycleButton.class::cast)
				.findFirst()
				.orElseThrow(() -> new AssertionError("no report mode option"))
				.onPress(LEFT_CLICK);
	}

	private static boolean hasButton(final List<AbstractWidget> widgets, final String key) {
		String label = Component.translatable(key).getString();
		return widgets.stream().anyMatch(widget -> widget.getMessage().getString().startsWith(label));
	}

	private static <T> T only(final Screen screen, final Class<T> type) {
		List<T> matches = screen.children().stream().filter(type::isInstance).map(type::cast).toList();
		checkEquals(1, matches.size(), type.getSimpleName() + " widgets on " + screen.getClass().getSimpleName());
		return matches.getFirst();
	}

	private static int count(final List<? extends GuiEventListener> widgets, final Class<?> type) {
		return (int) widgets.stream().filter(type::isInstance).count();
	}
}
