package com.urntt.modlist.config;

import com.urntt.modlist.ServerAddresses;
import com.urntt.modlist.session.ConnectionSession;
import com.urntt.modlist.session.ServerReport;
import com.urntt.modlist.session.SessionManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Shows what the most recent server announced and what the client reported to it, and leads to that server's
 * settings.
 */
public final class ServerReportScreen extends Screen {
	private static final Component TITLE = Component.translatable("options.modlist.report.title");
	private static final int TEXT_WIDTH = 310;
	private static final int INITIAL_HEIGHT = 130;

	private final Screen parent;
	private final ModListConfig config;
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
	private @Nullable ScrollableLayout scrollArea;

	public ServerReportScreen(final Screen parent, final ModListConfig config) {
		super(TITLE);
		this.parent = parent;
		this.config = config;
	}

	@Override
	protected void init() {
		this.layout.addTitleHeader(TITLE, this.font);
		ConnectionSession session = SessionManager.latest();

		LinearLayout lines = LinearLayout.vertical().spacing(4);
		if (session != null) {
			for (Component line : ServerReport.describe(session)) {
				lines.addChild(new MultiLineTextWidget(line, this.font).setMaxWidth(TEXT_WIDTH));
			}
		}
		this.scrollArea = new ScrollableLayout(this.minecraft, lines, INITIAL_HEIGHT);
		this.scrollArea.setMinWidth(TEXT_WIDTH);
		this.layout.addToContents(this.scrollArea);

		LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		Button edit = footer.addChild(Button.builder(Component.translatable("options.modlist.report.edit"),
				button -> this.editServerProfile(session)).width(200).build());
		edit.setTooltip(Tooltip.create(Component.translatable("options.modlist.report.edit.tooltip")));
		edit.active = this.config.isActive() && session != null && session.address() != null
				&& ServerAddresses.isValid(session.address());
		footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).width(100).build());

		this.layout.visitWidgets(this::addRenderableWidget);
		this.repositionElements();
	}

	@Override
	protected void repositionElements() {
		if (this.scrollArea == null) {
			this.layout.arrangeElements();
			return;
		}
		this.scrollArea.setMaxHeight(INITIAL_HEIGHT);
		this.layout.arrangeElements();
		int availableExtraHeight = this.height - this.layout.getFooterHeight() - this.scrollArea.getRectangle().bottom();
		this.scrollArea.setMaxHeight(this.scrollArea.getHeight() + availableExtraHeight);
		this.layout.arrangeElements();
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	/** Opens the server profile the session's address uses, creating it from the default profile if needed. */
	private void editServerProfile(final @Nullable ConnectionSession session) {
		if (session == null || session.address() == null || !ServerAddresses.isValid(session.address())) {
			return;
		}
		ServerProfile server = this.config.serverProfileToEdit(session.address());
		this.minecraft.gui.setScreen(new ProfileScreen(this, server.profile(), server));
	}
}
