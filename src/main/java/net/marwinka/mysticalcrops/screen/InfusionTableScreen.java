package net.marwinka.mysticalcrops.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.marwinka.mysticalcrops.MysticalCrops;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class InfusionTableScreen extends HandledScreen<InfusionTableScreenHandler> {
    private static final Identifier TEXTURE =
            new Identifier(MysticalCrops.MOD_ID, "textures/gui/infusion_table_gui.png");
    public InfusionTableScreen(InfusionTableScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
    }
    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        this.textRenderer.draw(matrices, title, (float) (this.backgroundWidth / 2 - this.textRenderer.getWidth(title) / 2), -5, 4210752);
    }
    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - 175) / 2;
        int y = (height - 217) / 2;
        drawTexture(matrices, x, y + 14 , 0, 0, 175, 217);

        renderProgressArrow(matrices, x, y);
    }

    private void renderProgressArrow(MatrixStack matrices, int x, int y) {
        if(handler.isCrafting()) {
            drawTexture(matrices, x + 47, y + 31, 176, 0, handler.getScaledProgress(), 82);
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        drawMouseoverTooltip(matrices, mouseX, mouseY);
    }
}