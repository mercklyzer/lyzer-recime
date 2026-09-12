package com.lyzer.lyzerrecime.recipe.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateRecipeRequest(

        @NotBlank @Size(max = 200)
        String title,

        @Size(max = 2000)
        String description,

        // Wrapper Integer, not int: a missing JSON field binds to null and
        // @NotNull reports "servings is required". An int would silently
        // default to 0 and then fail @Positive with a confusing message.
        @NotNull @Positive
        Integer servings,

        @NotNull
        Boolean vegetarian,

        @NotBlank
        String instructions,

        // @Valid cascades validation into each element; without it the
        // annotations on IngredientRequest are never evaluated.
        @NotEmpty @Valid
        List<IngredientRequest> ingredients) {
}
