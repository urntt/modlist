package com.urntt.modlist.config;

import com.urntt.modlist.registry.Placeholders;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

/**
 * Chooses the entries that stand in for the server's blocks, items and entity types the client does not have. A blank
 * box uses the built-in placeholder. "Done" saves and is disabled while an entry does not exist; "Cancel" or Escape
 * discards the changes.
 */
public final class PlaceholdersScreen extends Screen {
	private static final Component TITLE = Component.translatable("options.modlist.placeholders.title");
	private static final Component DESCRIPTION = Component.translatable("options.modlist.placeholders.description");
	private static final int LABEL_WIDTH = 100;
	private static final int BOX_WIDTH = 206;
	private static final int MAX_ENTRY_LENGTH = 256;
	private static final int SPACING = 4;

	private final Screen parent;
	private final Profile profile;
	private final HeaderAndFooterLayout layout;
	private final Map<ResourceKey<? extends Registry<?>>, EditBox> boxes = new LinkedHashMap<>();
	private final Set<EditBox> invalidBoxes = new HashSet<>();
	private @Nullable Button doneButton;

	public PlaceholdersScreen(final Screen parent, final Profile profile) {
		super(TITLE);
		this.parent = parent;
		this.profile = profile;
		this.layout = new HeaderAndFooterLayout(this, 12 + 9 + SPACING + 9, 33);
	}

	@Override
	protected void init() {
		LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(SPACING));
		header.defaultCellSetting().alignHorizontallyCenter();
		header.addChild(new StringWidget(TITLE, this.font));
		header.addChild(new StringWidget(DESCRIPTION.copy().withStyle(ChatFormatting.GRAY), this.font));

		this.boxes.clear();
		this.invalidBoxes.clear();
		Map<String, String> configured = this.profile.placeholders();
		LinearLayout rows = this.layout.addToContents(LinearLayout.vertical().spacing(SPACING));
		for (ResourceKey<? extends Registry<?>> registry : Placeholders.EDITABLE) {
			String registryKey = "options.modlist.placeholders." + registry.identifier().getPath();
			LinearLayout row = rows.addChild(LinearLayout.horizontal().spacing(SPACING));
			row.defaultCellSetting().alignVerticallyMiddle();
			row.addChild(new StringWidget(LABEL_WIDTH, 20, Component.translatable(registryKey), this.font));

			EditBox box = new EditBox(this.font, BOX_WIDTH, 20, Component.translatable(registryKey));
			box.setMaxLength(MAX_ENTRY_LENGTH);
			box.setHint(Component.literal(String.valueOf(Placeholders.BUILT_IN.get(registry.identifier())))
					.withStyle(EditBox.SEARCH_HINT_STYLE));
			box.setTooltip(Tooltip.create(Component.translatable(registryKey + ".tooltip")));
			box.setValue(configured.getOrDefault(registry.identifier().toString(), ""));
			box.setResponder(text -> this.validate(registry, box, text));
			this.validate(registry, box, box.getValue());
			row.addChild(box);
			this.boxes.put(registry, box);
		}

		LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		this.doneButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.saveAndClose())
				.width(150).build());
		footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).width(150).build());

		this.layout.visitWidgets(this::addRenderableWidget);
		this.repositionElements();
		this.updateDoneButton();
	}

	@Override
	protected void repositionElements() {
		this.layout.arrangeElements();
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	private void validate(final ResourceKey<? extends Registry<?>> registry, final EditBox box, final String text) {
		boolean valid = text.isBlank() || Placeholders.isValid(registry, text);
		box.setTextColor(valid ? EditBox.DEFAULT_TEXT_COLOR : ScreenWidgets.INVALID_TEXT_COLOR);
		if (valid) {
			this.invalidBoxes.remove(box);
		} else {
			this.invalidBoxes.add(box);
		}
		this.updateDoneButton();
	}

	private void updateDoneButton() {
		if (this.doneButton != null) {
			this.doneButton.active = this.invalidBoxes.isEmpty();
		}
	}

	private void saveAndClose() {
		// Placeholders of registries this screen does not show are kept as they are.
		Map<String, String> placeholders = new LinkedHashMap<>(this.profile.placeholders());
		this.boxes.forEach((registry, box) -> {
			String value = box.getValue().trim();
			if (value.isEmpty()) {
				placeholders.remove(registry.identifier().toString());
			} else {
				placeholders.put(registry.identifier().toString(), value);
			}
		});
		this.profile.setPlaceholders(placeholders);
		this.onClose();
	}
}
