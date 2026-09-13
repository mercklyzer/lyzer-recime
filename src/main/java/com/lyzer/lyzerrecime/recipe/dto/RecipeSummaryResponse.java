package com.lyzer.lyzerrecime.recipe.dto;

import com.lyzer.lyzerrecime.recipe.Recipe;

import java.util.List;

public record RecipeSummaryResponse(
        Long id,
        String title,
        String description,
        int servings,
        boolean vegetarian,
        List<IngredientResponse> ingredients) {

    public static RecipeSummaryResponse from(
            Recipe recipe, List<IngredientResponse> ingredients) {
        return new RecipeSummaryResponse(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getDescription(),
                recipe.getServings(),
                recipe.isVegetarian(),
                List.copyOf(ingredients));
    }
}
