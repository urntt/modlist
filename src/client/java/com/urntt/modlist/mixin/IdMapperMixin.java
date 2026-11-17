package com.urntt.modlist.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.urntt.modlist.registry.UnknownContentFallbacks;
import net.minecraft.core.IdMapper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * While unknown registry entries are ignored, block state IDs the client does not know (the states of the server's
 * extra blocks) resolve to the block placeholder instead of failing to decode. Every other lookup stays vanilla.
 */
@Mixin(IdMapper.class)
abstract class IdMapperMixin<T> {
	@SuppressWarnings("unchecked")
	@ModifyReturnValue(method = "byId", at = @At("RETURN"))
	private @Nullable T modlist$unknownBlockState(final @Nullable T original) {
		if (original == null && (Object) this == Block.BLOCK_STATE_REGISTRY) {
			BlockState placeholder = UnknownContentFallbacks.blockState();
			if (placeholder != null) {
				return (T) placeholder;
			}
		}
		return original;
	}
}
