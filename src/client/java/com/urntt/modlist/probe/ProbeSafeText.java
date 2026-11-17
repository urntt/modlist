package com.urntt.modlist.probe;

import java.util.Set;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Turns server-supplied text into the string a client without the hidden mods would produce.
 *
 * <p>Servers can detect mods by putting a mod's translation key or key binding name into text the client turns into a
 * string and sends back, such as the lines of a sign being edited or an item name in an anvil. Translation keys and
 * key binding names in {@code hiddenKeys} are therefore treated as unknown: a translation shows its fallback or its
 * key, and a key binding shows its name, as vanilla does for names it does not know.
 */
public final class ProbeSafeText {
	private ProbeSafeText() {
	}

	/** Returns {@code component} as a string, treating {@code hiddenKeys} as unknown. */
	public static String getString(final Component component, final Set<String> hiddenKeys) {
		return getString(component, hiddenKeys, Component::getString);
	}

	/**
	 * Returns {@code component} as a string through {@code stringifier}, treating {@code hiddenKeys} as unknown.
	 */
	public static String getString(final Component component, final Set<String> hiddenKeys,
			final Function<Component, String> stringifier) {
		return stringifier.apply(hiddenKeys.isEmpty() ? component : sanitize(component, hiddenKeys));
	}

	/** Returns a copy of {@code component} in which {@code hiddenKeys} no longer resolve. */
	public static Component sanitize(final Component component, final Set<String> hiddenKeys) {
		MutableComponent result = MutableComponent.create(sanitizeContents(component.getContents(), hiddenKeys));
		result.setStyle(component.getStyle());
		for (Component sibling : component.getSiblings()) {
			result.append(sanitize(sibling, hiddenKeys));
		}
		return result;
	}

	private static ComponentContents sanitizeContents(final ComponentContents contents, final Set<String> hiddenKeys) {
		if (contents instanceof TranslatableContents translatable) {
			if (hiddenKeys.contains(translatable.getKey())) {
				String fallback = translatable.getFallback();
				return Component.literal(fallback != null ? fallback : translatable.getKey()).getContents();
			}
			Object[] args = translatable.getArgs().clone();
			for (int i = 0; i < args.length; i++) {
				if (args[i] instanceof Component argument) {
					args[i] = sanitize(argument, hiddenKeys);
				}
			}
			return new TranslatableContents(translatable.getKey(), translatable.getFallback(), args);
		}
		if (contents instanceof KeybindContents keybind && hiddenKeys.contains(keybind.getName())) {
			return Component.literal(keybind.getName()).getContents();
		}
		return contents;
	}
}
