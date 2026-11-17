package com.urntt.modlist.mixin;

import com.urntt.modlist.ModListClient;
import com.urntt.modlist.registry.UnknownContentFallbacks;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.syncher.SynchedEntityData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * While unknown registry entries are ignored, entities of unknown types are created as the entity placeholder, so the
 * server's data values for them may not fit. Values with an unknown ID or another type are then skipped instead of
 * disconnecting. Otherwise data values are applied as in vanilla.
 */
@Mixin(SynchedEntityData.class)
abstract class SynchedEntityDataMixin {
	@Unique
	private static final Logger MODLIST$LOGGER = LoggerFactory.getLogger(ModListClient.MOD_ID);

	@Shadow
	@Final
	private SynchedEntityData.DataItem<?>[] itemsById;

	@ModifyVariable(method = "assignValues", at = @At("HEAD"), argsOnly = true)
	private List<SynchedEntityData.DataValue<?>> modlist$skipIncompatibleValues(
			final List<SynchedEntityData.DataValue<?>> values) {
		if (!UnknownContentFallbacks.lenientEntityData()) {
			return values;
		}
		List<SynchedEntityData.DataValue<?>> compatible = new ArrayList<>(values.size());
		for (SynchedEntityData.DataValue<?> value : values) {
			if (value.id() >= 0 && value.id() < this.itemsById.length
					&& Objects.equals(value.serializer(), this.itemsById[value.id()].getAccessor().serializer())) {
				compatible.add(value);
			}
		}
		if (compatible.size() != values.size()) {
			MODLIST$LOGGER.debug("Skipped {} entity data values that do not fit the entity",
					values.size() - compatible.size());
			return compatible;
		}
		return values;
	}
}
