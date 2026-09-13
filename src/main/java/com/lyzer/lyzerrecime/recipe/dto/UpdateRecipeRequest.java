package com.lyzer.lyzerrecime.recipe.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The same shape as {@link CreateRecipeRequest} today — PUT is a full
 * replacement, so it has to carry everything create does. Kept separate because
 * the two diverge the moment a field is create-only (a client-supplied slug) or
 * update-only (an {@code If-Match} version): a field addition then, rather than
 * a refactor of every caller.
 */
public record UpdateRecipeRequest(

        @NotBlank @Size(max = 200)
        String title,

        @Size(max = 2000)
        String description,

        @NotNull @Positive
        Integer servings,

        @NotNull
        Boolean vegetarian,

        @NotBlank
        String instructions,

        @NotEmpty @Valid @UniqueIngredientNames
        List<IngredientRequest> ingredients) {
}
