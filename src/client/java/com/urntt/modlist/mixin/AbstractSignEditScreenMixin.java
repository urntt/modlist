package com.urntt.modlist.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.urntt.modlist.probe.HiddenTranslations;
import com.urntt.modlist.probe.ProbeSafeText;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The sign editor turns the server's sign text into the strings it sends back when the screen closes. Translation
 * keys and key binding names of hidden mods stay unresolved there, as on a client without those mods. The rest of the
 * screen is vanilla.
 */
@Mixin(AbstractSignEditScreen.class)
abstract class AbstractSignEditScreenMixin {
	@WrapOperation(method = "<init>(Lnet/minecraft/world/level/block/entity/SignBlockEntity;Lnet/minecraft/world/level/block/entity/SignTextSlot;ZLnet/minecraft/network/chat/Component;)V",
			at = @At(value = "INVOKE",
			target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"))
	private String modlist$hideProbedTranslations(final Component line, final Operation<String> original) {
		return ProbeSafeText.getString(line, HiddenTranslations.currentKeys(), original::call);
	}
}
