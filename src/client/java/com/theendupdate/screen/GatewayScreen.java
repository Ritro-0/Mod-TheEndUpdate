package com.theendupdate.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class GatewayScreen extends AbstractContainerScreen<GatewayScreenHandler> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("theendupdate",
        "textures/gui/container/quantum_gateway.png");

    public GatewayScreen(GatewayScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176, 166);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256,
            256);
        super.extractContents(extractor, mouseX, mouseY, partialTick);
    }
}
