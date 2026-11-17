package com.urntt.modlist.config;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;

/**
 * Widgets shared by the configuration screens.
 */
final class ScreenWidgets {
	/** Text color of input that cannot be saved. */
	static final int INVALID_TEXT_COLOR = 0xFFFF0000;
	/** Width of list rows and of header descriptions, as in vanilla's option lists. */
	static final int CONTENT_WIDTH = 310;
	/** Spacing between the title and the description. */
	static final int HEADER_SPACING = 4;
	/** Height of a header with a title and a description of up to two lines. */
	static final int HEADER_HEIGHT = 12 + 9 + HEADER_SPACING + 2 * 9;
	/** Height of a footer with one row of buttons. */
	static final int FOOTER_HEIGHT = 33;

	private ScreenWidgets() {
	}

	/**
	 * Creates a button labeled with the translation of {@code key} and with the translation of
	 * {@code key + ".tooltip"} as its tooltip.
	 */
	static Button button(final String key, final Runnable onPress) {
		return Button.builder(Component.translatable(key), button -> onPress.run())
				.tooltip(Tooltip.create(Component.translatable(key + ".tooltip")))
				.build();
	}

	/** Adds the title and a gray description, wrapped to two lines if needed, to the header of {@code layout}. */
	static void addHeader(final HeaderAndFooterLayout layout, final Component title, final Component description,
			final Font font) {
		LinearLayout header = layout.addToHeader(LinearLayout.vertical().spacing(HEADER_SPACING));
		header.defaultCellSetting().alignHorizontallyCenter();
		header.addChild(new StringWidget(title, font));
		header.addChild(new MultiLineTextWidget(description.copy().withStyle(ChatFormatting.GRAY), font)
				.setMaxWidth(CONTENT_WIDTH)
				.setCentered(true));
	}
}
