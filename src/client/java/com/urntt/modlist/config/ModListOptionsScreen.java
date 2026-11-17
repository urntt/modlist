package com.urntt.modlist.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Base of the mod's option screens: vanilla's options screen with plain section titles and options that can be
 * rebuilt in place.
 */
abstract class ModListOptionsScreen extends OptionsSubScreen {
	private @Nullable ModListOptionsList optionsList;

	ModListOptionsScreen(final @Nullable Screen parent, final Component title) {
		super(parent, Minecraft.getInstance().options, title);
	}

	@Override
	protected void addContents() {
		this.optionsList = this.layout.addToContents(new ModListOptionsList(this.minecraft, this.width, this));
		this.list = this.optionsList;
		this.addOptions();
	}

	/** Adds a section title that cannot take focus. */
	protected final void section(final Component title) {
		if (this.optionsList != null) {
			this.optionsList.addSection(title);
		}
	}

	/** Rebuilds the options, for example after an option that decides which others are shown has changed. */
	protected final void refreshOptions() {
		if (this.optionsList != null) {
			this.optionsList.rebuild(this::addOptions);
		}
	}
}
