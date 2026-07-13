package io.github.sirmustfailalot.projectash.fabric.mixin;

import io.github.sirmustfailalot.projectash.client.CommonClientHandler;
import io.github.sirmustfailalot.projectash.fabric.ProjectAshFabricClient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public class ContainerScreenMixin {

    @Shadow protected Slot hoveredSlot;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onScreenKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (ProjectAshFabricClient.INSTANCE.getShowcaseKeyBind().matches(keyCode, scanCode)) {
            if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
                CommonClientHandler.INSTANCE.attemptShowcase(this.hoveredSlot.getItem());
                cir.setReturnValue(true);
            }
        }
    }
}