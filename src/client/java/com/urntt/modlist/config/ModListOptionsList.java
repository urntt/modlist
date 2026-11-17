package com.urntt.modlist.config;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

/**
 * Vanilla's option list with section titles that cannot take focus, and with a way to rebuild its entries when the
 * options shown depend on another option's value.
 */
final class ModListOptionsList extends OptionsList {
	private final Screen screen;

	ModListOptionsList(final Minecraft minecraft, final int width, final OptionsSubScreen screen) {
		super(minecraft, width, screen);
		this.screen = screen;
	}

	/**
	 * Adds a section title. Unlike vanilla's {@link #addHeader(Component)}, whose focusable text shows a border when
	 * clicked, the title is plain text.
	 */
	void addSection(final Component title) {
		int lineHeight = this.minecraft.font.lineHeight;
		int paddingTop = this.children().isEmpty() ? 0 : lineHeight * 2;
		this.addEntry(new SectionEntry(this.screen, title, paddingTop), paddingTop + lineHeight * 2);
	}

	/** Replaces every entry with those {@code addOptions} adds, keeping the scroll position where possible. */
	void rebuild(final Runnable addOptions) {
		double scroll = this.scrollAmount();
		this.setFocused(null);
		this.clearEntries();
		addOptions.run();
		this.setScrollAmount(scroll);
	}

	private static final class SectionEntry extends OptionsList.AbstractEntry {
		private final Screen screen;
		private final Component title;
		private final int paddingTop;

		private SectionEntry(final Screen screen, final Component title, final int paddingTop) {
			this.screen = screen;
			this.title = title;
			this.paddingTop = paddingTop;
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of();
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of();
		}

		@Override
		public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
				final boolean hovered, final float a) {
			graphics.centeredText(this.screen.getFont(), this.title, this.screen.width / 2,
					this.getContentY() + this.paddingTop, CommonColors.WHITE);
		}
	}
}
