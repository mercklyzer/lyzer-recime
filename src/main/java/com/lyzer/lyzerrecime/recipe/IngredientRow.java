package com.lyzer.lyzerrecime.recipe;

import com.lyzer.lyzerrecime.recipe.dto.IngredientResponse;

import java.math.BigDecimal;

/**
 * Query projection, not an API type: flat columns carrying the owning recipe id
 * so a page of rows can be grouped back into per-recipe lists.
 */
record IngredientRow(Long recipeId, String name, BigDecimal quantity, String unit) {

    IngredientResponse toResponse() {
        return new IngredientResponse(name, quantity, unit);
    }
}
