package io.github.sirmustfailalot.projectash.fabric.mixin;

import io.github.sirmustfailalot.projectash.showcase.ChatPreviewManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderChatScreen(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        Style styleUnderMouse = minecraft.gui.getChat().getClickedComponentStyleAt((double)mouseX, (double)mouseY);

        // 1. Hover Logic: Track when the cursor is resting on a text line link
        if (styleUnderMouse != null && styleUnderMouse.getHoverEvent() != null) {
            HoverEvent event = styleUnderMouse.getHoverEvent();
            if (event.getAction() == HoverEvent.Action.SHOW_ITEM) {
                HoverEvent.ItemStackInfo itemInfo = event.getValue(HoverEvent.Action.SHOW_ITEM);
                if (itemInfo != null) {
                    ItemStack hoveredStack = itemInfo.getItemStack();
                    String namespace = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(hoveredStack.getItem()).getNamespace();
                    if (namespace.equals("cobbletcg") || namespace.equals(("cobblemon"))) {
                        ChatPreviewManager.INSTANCE.setHoveredPreviewStack(hoveredStack);
                    }
                }
            }
        } else {
            // Clears instantly when the mouse leaves the text link boundary
            ChatPreviewManager.INSTANCE.clear();
        }

        ItemStack activePreview = ChatPreviewManager.INSTANCE.getHoveredPreviewStack();
        if (activePreview != null && !activePreview.isEmpty()) {

            ItemStack singleItemPreview = activePreview.copy();
            singleItemPreview.setCount(1);

            float renderX = (float) mouseX + 15f;
            float renderY = (float) mouseY - 160f;

            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();

            poseStack.translate(renderX, renderY, 0f);

            float scaleFactor = 9.0f;
            poseStack.scale(scaleFactor, scaleFactor, 1f);

            guiGraphics.renderItem(singleItemPreview, 0, 0);
            guiGraphics.renderItemDecorations(minecraft.font, singleItemPreview, 0, 0);

            poseStack.popPose();
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onChatClosed(CallbackInfo ci) {
        ChatPreviewManager.INSTANCE.clear();
    }
}