package com.urntt.modlist.config;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * Widgets shared by the configuration screens.
 */
final class ScreenWidgets {
	/** Text color of input that cannot be saved. */
	static final int INVALID_TEXT_COLOR = 0xFFFF0000;

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
}
