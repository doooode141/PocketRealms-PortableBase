package com.doodly141.pocketrealms.client;

import com.doodly141.pocketrealms.menu.PocketNavigatorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class PocketNavigatorScreen extends AbstractContainerScreen<PocketNavigatorMenu> {
    private static final Component HEADER = Component.literal("POCKET REALMS");
    private static final Component SUBHEADER = Component.literal("DIMENSIONAL NAVIGATOR");

    public PocketNavigatorScreen(PocketNavigatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 220;
        this.imageHeight = 142;
        this.titleLabelX = 10;
        this.titleLabelY = 10;
        this.inventoryLabelY = 200;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x - 2, y - 2, x + imageWidth + 2, y + imageHeight + 2, 0xFF07111D);
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF142638);
        graphics.fill(x + 4, y + 4, x + imageWidth - 4, y + imageHeight - 4, 0xFF0B1827);
        graphics.fill(x + 6, y + 6, x + imageWidth - 6, y + 7, 0xFF4BB8C7);
        graphics.fill(x + 6, y + imageHeight - 7, x + imageWidth - 6, y + imageHeight - 6, 0xFF344E68);

        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.getSlot(i);
            int sx = x + slot.x;
            int sy = y + slot.y;
            graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF263E52);
            graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF0A1421);
            graphics.fill(sx, sy, sx + 16, sy + 1, 0xFF678B9A);
        }
        graphics.fill(x + 8, y + 31, x + imageWidth - 8, y + 32, 0xFF314E66);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, HEADER, (imageWidth - font.width(HEADER)) / 2, 10, 0xEAFBFF, false);
        graphics.drawString(font, SUBHEADER, (imageWidth - font.width(SUBHEADER)) / 2, 21, 0x69D4DF, false);
        graphics.drawString(font, "Select a destination", 10, 31, 0xA9BACB, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
