package com.urntt.modlist.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.urntt.modlist.probe.HiddenTranslations;
import com.urntt.modlist.probe.ProbeSafeText;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The anvil fills its name field from the item's name and sends the field back as the new name. Translation keys and
 * key binding names of hidden mods stay unresolved in both places, as on a client without those mods. The rest of the
 * screen is vanilla.
 */
@Mixin(AnvilScreen.class)
abstract class AnvilScreenMixin {
	@WrapOperation(method = {"slotChanged", "onNameChanged"}, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"))
	private String modlist$hideProbedTranslations(final Component name, final Operation<String> original) {
		return ProbeSafeText.getString(name, HiddenTranslations.currentKeys(), original::call);
	}
}
