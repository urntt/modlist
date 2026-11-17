package com.urntt.modlist.config;

import com.urntt.modlist.ServerAddresses;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Lists the server profiles. Adding one copies the default profile into a new entry; editing opens it; removing deletes
 * it at once. A connection uses the first profile whose address matches, so a profile that an earlier one always
 * matches first is marked as unused.
 *
 * <p>Vanilla initializes a screen instance only once and merely repositions it when it is shown again, so the list is
 * refreshed in place: after adding or removing, and in {@link #added()} when returning from a profile.
 */
public final class ServerProfilesScreen extends Screen {
	private static final Component TITLE = Component.translatable("options.modlist.server_profiles.title");
	private static final Component DESCRIPTION = Component.translatable("options.modlist.server_profiles.description");
	private static final int MAX_ADDRESS_LENGTH = 261;
	private static final int ROW_WIDTH = 310;
	private static final int ROW_HEIGHT = 24;
	private static final int BUTTON_WIDTH = 50;
	private static final int SPACING = 4;

	private final Screen parent;
	private final ModListConfig config;
	private final HeaderAndFooterLayout layout;
	private @Nullable ProfileList profileList;
	private @Nullable EditBox addressBox;
	private @Nullable Button addButton;
	/** Set when a row asks to refresh the list while the list is handling that row's click. */
	private boolean pendingRefresh;

	public ServerProfilesScreen(final Screen parent, final ModListConfig config) {
		super(TITLE);
		this.parent = parent;
		this.config = config;
		this.layout = new HeaderAndFooterLayout(this, ScreenWidgets.HEADER_HEIGHT, ScreenWidgets.FOOTER_HEIGHT);
	}

	@Override
	protected void init() {
		ScreenWidgets.addHeader(this.layout, TITLE, DESCRIPTION, this.font);

		this.profileList = this.layout.addToContents(new ProfileList());
		this.profileList.refresh();

		LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		this.addressBox = footer.addChild(new EditBox(this.font, 150, 20,
				Component.translatable("options.modlist.profile.address")));
		this.addressBox.setMaxLength(MAX_ADDRESS_LENGTH);
		this.addressBox.setHint(Component.translatable("options.modlist.profile.address.hint")
				.withStyle(EditBox.SEARCH_HINT_STYLE));
		this.addressBox.setResponder(text -> this.updateAddButton());
		this.addButton = footer.addChild(Button.builder(Component.translatable("options.modlist.list.add"),
				button -> this.addProfile()).width(70).build());
		footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).width(70).build());

		this.layout.visitWidgets(this::addRenderableWidget);
		this.repositionElements();
		this.updateAddButton();
	}

	@Override
	protected void repositionElements() {
		this.layout.arrangeElements();
		if (this.profileList != null) {
			this.profileList.updateSize(this.width, this.layout);
		}
	}

	@Override
	public void added() {
		// Called whenever this screen is shown, including on return from a profile whose address may have changed.
		if (this.profileList != null) {
			this.profileList.refresh();
		}
	}

	@Override
	public void tick() {
		if (this.pendingRefresh && this.profileList != null) {
			this.pendingRefresh = false;
			this.profileList.refresh();
		}
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	private void updateAddButton() {
		if (this.addButton != null && this.addressBox != null) {
			boolean valid = ServerAddresses.isValid(this.addressBox.getValue());
			this.addButton.active = valid;
			this.addressBox.setTextColor(valid || this.addressBox.getValue().isEmpty() ? EditBox.DEFAULT_TEXT_COLOR
					: ScreenWidgets.INVALID_TEXT_COLOR);
		}
	}

	private void addProfile() {
		if (this.addressBox != null && this.profileList != null && ServerAddresses.isValid(this.addressBox.getValue())) {
			this.config.addServerProfile(this.addressBox.getValue());
			this.addressBox.setValue("");
			this.profileList.refresh();
			this.profileList.scrollToLast();
		}
	}

	/** Whether a profile before {@code index} matches every connection that {@code profiles.get(index)} matches. */
	private static boolean isShadowed(final List<ServerProfile> profiles, final int index) {
		String address = profiles.get(index).address();
		for (int i = 0; i < index; i++) {
			if (ServerAddresses.matches(profiles.get(i).address(), address)) {
				return true;
			}
		}
		return false;
	}

	private final class ProfileList extends ContainerObjectSelectionList<Row> {
		private ProfileList() {
			super(Minecraft.getInstance(), ServerProfilesScreen.this.width,
					ServerProfilesScreen.this.layout.getContentHeight(), ServerProfilesScreen.this.layout.getHeaderHeight(),
					ROW_HEIGHT);
		}

		/** Replaces the rows with the configuration's current server profiles, keeping the scroll position. */
		private void refresh() {
			double scroll = this.scrollAmount();
			this.setFocused(null);
			this.clearEntries();
			List<ServerProfile> profiles = ServerProfilesScreen.this.config.serverProfiles();
			for (int i = 0; i < profiles.size(); i++) {
				this.addEntry(new Row(profiles.get(i), isShadowed(profiles, i)));
			}
			this.setScrollAmount(scroll);
		}

		private void scrollToLast() {
			if (!this.children().isEmpty()) {
				this.scrollToEntry(this.children().getLast());
			}
		}

		@Override
		public int getRowWidth() {
			return ROW_WIDTH;
		}
	}

	private final class Row extends ContainerObjectSelectionList.Entry<Row> {
		private final StringWidget label;
		private final Button editButton;
		private final Button removeButton;

		private Row(final ServerProfile server, final boolean shadowed) {
			Component address = Component.literal(server.address());
			this.label = new StringWidget(ROW_WIDTH - 2 * (BUTTON_WIDTH + SPACING), 20,
					shadowed ? address.copy().withStyle(ChatFormatting.RED) : address, ServerProfilesScreen.this.font);
			if (shadowed) {
				this.label.setTooltip(Tooltip.create(Component.translatable("options.modlist.server_profiles.shadowed")));
			}
			this.editButton = Button.builder(Component.translatable("options.modlist.server_profiles.edit"),
							button -> ServerProfilesScreen.this.minecraft.gui.setScreen(
									new ProfileScreen(ServerProfilesScreen.this, server.profile(), server)))
					.width(BUTTON_WIDTH).build();
			this.removeButton = Button.builder(Component.translatable("options.modlist.list.remove"), button -> {
				ServerProfilesScreen.this.config.removeServerProfile(server);
				// The list is still handling this row's click, so it is refreshed on the next tick.
				ServerProfilesScreen.this.pendingRefresh = true;
			}).width(BUTTON_WIDTH).build();
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(this.editButton, this.removeButton);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(this.editButton, this.removeButton);
		}

		@Override
		public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
				final boolean hovered, final float a) {
			this.label.setPosition(this.getContentX(), this.getContentY());
			this.editButton.setPosition(this.getContentRight() - 2 * BUTTON_WIDTH - SPACING, this.getContentY());
			this.removeButton.setPosition(this.getContentRight() - BUTTON_WIDTH, this.getContentY());
			this.label.extractRenderState(graphics, mouseX, mouseY, a);
			this.editButton.extractRenderState(graphics, mouseX, mouseY, a);
			this.removeButton.extractRenderState(graphics, mouseX, mouseY, a);
		}
	}
}
