package net.marwinka.mysticalcrops.compat.rei.ritual_table;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.marwinka.mysticalcrops.compat.rei.REICategories;
import net.marwinka.mysticalcrops.recipe.InfusionTableRecipe;
import net.marwinka.mysticalcrops.recipe.RitualTableRecipe;

import java.util.List;

@Environment(EnvType.CLIENT)
public record InfusionTableDisplay(InfusionTableRecipe recipe) implements Display {

    @Override
    public List<EntryIngredient> getInputEntries() {
        return EntryIngredients.ofIngredients(recipe.getIngredients());
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return List.of(EntryIngredients.of(recipe.getOutput()));
    }


    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return REICategories.INFUSION_TABLE_CATEGORY;
    }
}