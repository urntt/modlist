package com.urntt.modlist.mixin;

import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the raw ID list, so the holes left by ignored registry entries can be filled.
 */
@Mixin(MappedRegistry.class)
public interface MappedRegistryAccessor<T> {
	@Accessor("byId")
	ObjectList<Holder.Reference<T>> modlist$byId();
}
