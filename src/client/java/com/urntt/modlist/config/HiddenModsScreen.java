package com.urntt.modlist.config;

import com.urntt.modlist.session.ChannelEntries;
import com.urntt.modlist.session.LocalChannels;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Chooses the channel namespaces a profile hides in custom mode. Servers only see mods through the channels they
 * register, so the screen lists those namespaces rather than every installed mod. "Done" saves and "Cancel" or Escape
 * discards the changes.
 */
public final class HiddenModsScreen extends Screen {
	private static final Component TITLE = Component.translatable("options.modlist.hidden_mods.title");
	private static final Component DESCRIPTION = Component.translatable("options.modlist.hidden_mods.description");
	private static final Component HIDDEN = Component.translatable("options.modlist.hidden_mods.hidden");
	private static final Component REPORTED = Component.translatable("options.modlist.hidden_mods.reported");
	/** Fabric API's namespace, which servers need for registry sync. */
	private static final String FABRIC_NAMESPACE = "fabric";
	private static final int ROW_WIDTH = 310;
	private static final int ROW_HEIGHT = 24;
	private static final int TOGGLE_WIDTH = 80;
	private static final int SPACING = 4;

	private final Screen parent;
	private final Profile profile;
	private final HeaderAndFooterLayout layout;
	private @Nullable NamespaceList namespaceList;

	public HiddenModsScreen(final Screen parent, final Profile profile) {
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

		this.namespaceList = this.layout.addToContents(new NamespaceList(channelCounts(), this.profile.hiddenNamespaces()));

		LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.saveAndClose()).width(150).build());
		footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).width(150).build());

		this.layout.visitWidgets(this::addRenderableWidget);
		this.repositionElements();
	}

	@Override
	protected void repositionElements() {
		this.layout.arrangeElements();
		if (this.namespaceList != null) {
			this.namespaceList.updateSize(this.width, this.layout);
		}
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	private void saveAndClose() {
		if (this.namespaceList != null) {
			this.profile.setHiddenNamespaces(this.namespaceList.children().stream()
					.filter(row -> row.toggle.getValue())
					.map(row -> row.namespace)
					.toList());
		}
		this.onClose();
	}

	/** The number of real channels per hideable namespace, sorted by namespace. */
	private static Map<String, Integer> channelCounts() {
		Map<String, Integer> counts = new TreeMap<>();
		for (Identifier channel : LocalChannels.all()) {
			if (ChannelEntries.isHideable(channel.getNamespace())) {
				counts.merge(channel.getNamespace(), 1, Integer::sum);
			}
		}
		return counts;
	}

	private final class NamespaceList extends ContainerObjectSelectionList<Row> {
		private NamespaceList(final Map<String, Integer> counts, final List<String> hidden) {
			super(Minecraft.getInstance(), HiddenModsScreen.this.width, HiddenModsScreen.this.layout.getContentHeight(),
					HiddenModsScreen.this.layout.getHeaderHeight(), ROW_HEIGHT);
			Map<String, Integer> all = new TreeMap<>(counts);
			for (String namespace : hidden) {
				all.putIfAbsent(namespace, 0);
			}
			List<String> namespaces = new ArrayList<>(all.keySet());
			for (String namespace : namespaces) {
				this.addEntry(new Row(namespace, all.get(namespace), hidden.contains(namespace)));
			}
		}

		@Override
		public int getRowWidth() {
			return ROW_WIDTH;
		}
	}

	private final class Row extends ContainerObjectSelectionList.Entry<Row> {
		private final String namespace;
		private final StringWidget label;
		private final CycleButton<Boolean> toggle;

		private Row(final String namespace, final int channels, final boolean hidden) {
			this.namespace = namespace;
			Component name = FabricLoader.getInstance().getModContainer(namespace)
					.map(ModContainer::getMetadata)
					.<Component>map(metadata -> Component.translatable("options.modlist.hidden_mods.named",
							metadata.getName(), namespace))
					.orElse(Component.literal(namespace));
			this.label = new StringWidget(ROW_WIDTH - TOGGLE_WIDTH - SPACING, 20,
					Component.translatable("options.modlist.hidden_mods.row", name, channels), HiddenModsScreen.this.font);
			this.toggle = CycleButton.booleanBuilder(HIDDEN, REPORTED, hidden)
					.displayOnlyValue()
					.create(0, 0, TOGGLE_WIDTH, 20, Component.literal(namespace));
			if (FABRIC_NAMESPACE.equals(namespace)) {
				this.toggle.setTooltip(Tooltip.create(Component.translatable("options.modlist.hidden_mods.fabric_warning")));
			} else if (channels == 0) {
				this.toggle.setTooltip(Tooltip.create(Component.translatable("options.modlist.hidden_mods.not_installed")));
			}
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(this.toggle);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(this.toggle);
		}

		@Override
		public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
				final boolean hovered, final float a) {
			this.label.setPosition(this.getContentX(), this.getContentY());
			this.toggle.setPosition(this.getContentRight() - TOGGLE_WIDTH, this.getContentY());
			this.label.extractRenderState(graphics, mouseX, mouseY, a);
			this.toggle.extractRenderState(graphics, mouseX, mouseY, a);
		}
	}
}
