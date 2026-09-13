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
        // Each name becomes its own correlated EXISTS subquery, so an uncapped
        // list is a cheap way to make the server plan 500 subqueries.
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
