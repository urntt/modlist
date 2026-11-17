package com.urntt.modlist.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Edits a list of strings, modeled after vanilla's game rule screen: "Done" saves and is disabled while an entry is
 * invalid, and "Cancel" or Escape discards the changes. Blank entries are dropped when saving.
 */
public final class StringListScreen extends Screen {
	private static final Component ADD = Component.translatable("options.modlist.list.add");
	private static final Component REMOVE = Component.translatable("options.modlist.list.remove");
	private static final int MAX_ENTRY_LENGTH = 256;
	private static final int ROW_WIDTH = 310;
	private static final int ROW_HEIGHT = 24;
	private static final int REMOVE_BUTTON_WIDTH = 50;
	private static final int SPACING = 4;

	private final Screen parent;
	private final Component description;
	private final Component hint;
	private final Component entryNarration;
	private final List<String> initialEntries;
	private final Predicate<String> validator;
	private final Consumer<List<String>> onSave;
	private final HeaderAndFooterLayout layout;
	private final Set<ValueEntry> invalidEntries = new HashSet<>();
	private @Nullable EntryList entryList;
	private @Nullable Button doneButton;

	/**
	 * @param description a line shown under the title
	 * @param hint the hint shown in empty entries
	 * @param entryNarration the narration of an entry's text box
	 * @param validator whether a non-blank entry may be saved
	 * @param onSave receives the trimmed, non-blank entries when "Done" is pressed
	 */
	public StringListScreen(final Screen parent, final Component title, final Component description,
			final Component hint, final Component entryNarration, final List<String> entries,
			final Predicate<String> validator, final Consumer<List<String>> onSave) {
		super(title);
		this.parent = parent;
		this.description = description;
		this.hint = hint.copy().withStyle(EditBox.SEARCH_HINT_STYLE);
		this.entryNarration = entryNarration;
		this.initialEntries = List.copyOf(entries);
		this.validator = validator;
		this.onSave = onSave;
		this.layout = new HeaderAndFooterLayout(this, ScreenWidgets.HEADER_HEIGHT, ScreenWidgets.FOOTER_HEIGHT);
	}

	@Override
	protected void init() {
		ScreenWidgets.addHeader(this.layout, this.title, this.description, this.font);

		this.invalidEntries.clear();
		this.entryList = this.layout.addToContents(new EntryList(this.initialEntries));

		LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		footer.addChild(Button.builder(ADD, button -> this.addEntry()).width(100).build());
		this.doneButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.saveAndClose())
				.width(100).build());
		footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).width(100).build());

		this.layout.visitWidgets(this::addRenderableWidget);
		this.repositionElements();
		this.updateDoneButton();
	}

	@Override
	protected void repositionElements() {
		this.layout.arrangeElements();
		if (this.entryList != null) {
			this.entryList.updateSize(this.width, this.layout);
		}
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	private void saveAndClose() {
		if (this.entryList != null) {
			this.onSave.accept(this.entryList.children().stream()
					.map(ValueEntry::value)
					.filter(value -> !value.isEmpty())
					.toList());
		}
		this.onClose();
	}

	private void addEntry() {
		if (this.entryList == null) {
			return;
		}
		ValueEntry entry = this.entryList.add("");
		this.setFocused(this.entryList);
		this.entryList.setFocused(entry);
		entry.setFocused(entry.textBox);
	}

	private void updateValidity(final ValueEntry entry, final boolean valid) {
		if (valid) {
			this.invalidEntries.remove(entry);
		} else {
			this.invalidEntries.add(entry);
		}
		this.updateDoneButton();
	}

	private void updateDoneButton() {
		if (this.doneButton != null) {
			this.doneButton.active = this.invalidEntries.isEmpty();
		}
	}

	private final class EntryList extends ContainerObjectSelectionList<ValueEntry> {
		private EntryList(final List<String> values) {
			super(Minecraft.getInstance(), StringListScreen.this.width, StringListScreen.this.layout.getContentHeight(),
					StringListScreen.this.layout.getHeaderHeight(), ROW_HEIGHT);
			values.forEach(this::add);
		}

		private ValueEntry add(final String value) {
			ValueEntry entry = new ValueEntry(value);
			this.addEntry(entry);
			this.scrollToEntry(entry);
			return entry;
		}

		private void remove(final ValueEntry entry) {
			StringListScreen.this.updateValidity(entry, true);
			this.removeEntry(entry);
		}

		@Override
		public int getRowWidth() {
			return ROW_WIDTH;
		}
	}

	private final class ValueEntry extends ContainerObjectSelectionList.Entry<ValueEntry> {
		private final EditBox textBox;
		private final Button removeButton;

		private ValueEntry(final String value) {
			this.textBox = new EditBox(StringListScreen.this.font, ROW_WIDTH - REMOVE_BUTTON_WIDTH - SPACING, 20,
					StringListScreen.this.entryNarration);
			this.textBox.setMaxLength(MAX_ENTRY_LENGTH);
			this.textBox.setHint(StringListScreen.this.hint);
			this.textBox.setValue(value);
			this.textBox.setResponder(this::validate);
			this.validate(value);
			this.removeButton = Button.builder(REMOVE, button -> {
				if (StringListScreen.this.entryList != null) {
					StringListScreen.this.entryList.remove(this);
				}
			}).width(REMOVE_BUTTON_WIDTH).build();
		}

		/** The trimmed value; blank entries are dropped when saving. */
		private String value() {
			return this.textBox.getValue().trim();
		}

		private void validate(final String text) {
			boolean valid = text.isBlank() || StringListScreen.this.validator.test(text.trim());
			this.textBox.setTextColor(valid ? EditBox.DEFAULT_TEXT_COLOR : ScreenWidgets.INVALID_TEXT_COLOR);
			StringListScreen.this.updateValidity(this, valid);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(this.textBox, this.removeButton);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(this.textBox, this.removeButton);
		}

		@Override
		public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
				final boolean hovered, final float a) {
			this.textBox.setPosition(this.getContentX(), this.getContentY());
			this.removeButton.setPosition(this.getContentRight() - REMOVE_BUTTON_WIDTH, this.getContentY());
			this.textBox.extractRenderState(graphics, mouseX, mouseY, a);
			this.removeButton.extractRenderState(graphics, mouseX, mouseY, a);
		}
	}
}
