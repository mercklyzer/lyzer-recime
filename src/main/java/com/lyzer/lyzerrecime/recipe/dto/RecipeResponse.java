package com.lyzer.lyzerrecime.recipe.dto;

import com.lyzer.lyzerrecime.recipe.Recipe;

import java.time.Instant;
import java.util.List;

public record RecipeResponse(
        Long id,
        String title,
        String description,
        int servings,
        boolean vegetarian,
        String instructions,
        List<IngredientResponse> ingredients,
        Instant createdAt,
        Instant updatedAt) {

    public static RecipeResponse from(Recipe recipe) {
        return new RecipeResponse(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getDescription(),
                recipe.getServings(),
                recipe.isVegetarian(),
                recipe.getInstructions(),
                recipe.getIngredients().stream().map(IngredientResponse::from).toList(),
                recipe.getCreatedAt(),
                recipe.getUpdatedAt());
    }
}
