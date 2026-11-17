package com.urntt.modlist.registry;

import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lenient handling of content the client does not know, active while connected to a server whose unknown registry
 * entries were ignored.
 */
public final class UnknownContentFallbacks {
	private static volatile @Nullable BlockState blockState;
	private static volatile boolean active;

	private UnknownContentFallbacks() {
	}

	/**
	 * Starts resolving unknown block state IDs to {@code placeholder} and skipping incompatible entity data.
	 */
	public static void activate(final BlockState placeholder) {
		blockState = placeholder;
		active = true;
	}

	public static void deactivate() {
		active = false;
		blockState = null;
	}

	/** The state that unknown block state IDs resolve to, or {@code null} when inactive. */
	public static @Nullable BlockState blockState() {
		return blockState;
	}

	/** Whether entity data values that do not fit the entity are skipped instead of disconnecting. */
	public static boolean lenientEntityData() {
		return active;
	}
}
