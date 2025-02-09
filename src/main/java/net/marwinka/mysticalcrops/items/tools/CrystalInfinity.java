package net.marwinka.mysticalcrops.items.tools;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.text.Text;

import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class CrystalInfinity extends Item {
    public CrystalInfinity(Settings settings) {
        super(settings);
    }
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.mysticalcrops.infinity.tooltip") );
        if (Screen.hasShiftDown()) {
            tooltip.add(Text.translatable("item.mysticalcrops.crystal.tooltip.shift"));
        } else {
            tooltip.add(Text.translatable("item.mysticalcrops.crystal.tooltip"));
        }
        super.appendTooltip(stack, world, tooltip, context);
    }

}
