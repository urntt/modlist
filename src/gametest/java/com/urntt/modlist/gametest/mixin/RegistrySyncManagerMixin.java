package com.urntt.modlist.gametest.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.urntt.modlist.gametest.GhostEntries;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Map;
import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Adds the ghost entries to the registry sync map the local dedicated server sends.
 */
@Mixin(RegistrySyncManager.class)
abstract class RegistrySyncManagerMixin {
	@ModifyReturnValue(method = "createAndPopulateRegistryMap", at = @At("RETURN"))
	private static @Nullable Map<Identifier, Object2IntMap<Identifier>> modlistGametest$addGhosts(
			final @Nullable Map<Identifier, Object2IntMap<Identifier>> original) {
		return GhostEntries.inject(original);
	}
}
