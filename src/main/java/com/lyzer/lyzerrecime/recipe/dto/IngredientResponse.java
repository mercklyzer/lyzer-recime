package com.lyzer.lyzerrecime.recipe.dto;

import com.lyzer.lyzerrecime.recipe.RecipeIngredient;

import java.math.BigDecimal;

public record IngredientResponse(String name, BigDecimal quantity, String unit) {

    static IngredientResponse from(RecipeIngredient ingredient) {
        return new IngredientResponse(
                ingredient.getName(), ingredient.getQuantity(), ingredient.getUnit());
    }
}
