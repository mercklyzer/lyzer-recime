package com.lyzer.lyzerrecime.recipe.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RecipeSearchCriteria(
        Boolean vegetarian,
        @Positive Integer servings,
        @Positive Integer minServings,
        @Positive Integer maxServings,
        // Arbitrary number. I believe it is unlikely to pass more than 20 ingredients for search
        // This is something to be revisited once users start complaining about this limit.
        @Size(max = 20) List<String> includeIngredients,
        @Size(max = 20) List<String> excludeIngredients,
        String instructions) {

    /** Null collections become empty lists once, here, so nothing downstream needs a null check. */
    public RecipeSearchCriteria {
        includeIngredients = includeIngredients == null ? List.of() : includeIngredients;
        excludeIngredients = excludeIngredients == null ? List.of() : excludeIngredients;
    }

    @AssertTrue(message = "minServings must not exceed maxServings")
    public boolean isServingsRangeOrdered() {
        return minServings == null || maxServings == null || minServings <= maxServings;
    }

    @AssertTrue(message = "servings cannot be combined with minServings or maxServings")
    public boolean isServingsFilterUnambiguous() {
        return servings == null || (minServings == null && maxServings == null);
    }
}
