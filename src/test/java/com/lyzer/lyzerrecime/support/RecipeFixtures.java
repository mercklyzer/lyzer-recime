package com.lyzer.lyzerrecime.support;

import com.lyzer.lyzerrecime.recipe.Recipe;
import com.lyzer.lyzerrecime.recipe.dto.CreateRecipeRequest;
import com.lyzer.lyzerrecime.recipe.dto.IngredientRequest;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Valid requests and entities for every test to reuse, so a failing test points
 * at the code under test rather than at a hand-rolled fixture.
 */
public final class RecipeFixtures {

    public static final String DEFAULT_TITLE = "Garlic Onion Soup";
    public static final String DEFAULT_INSTRUCTIONS = "Preheat the oven, then simmer for 20 minutes.";

    private RecipeFixtures() {
    }

    public static IngredientRequest ingredient(String name) {
        return new IngredientRequest(name, BigDecimal.valueOf(1), "piece");
    }

    public static IngredientRequest ingredient(String name, BigDecimal quantity, String unit) {
        return new IngredientRequest(name, quantity, unit);
    }

    /** The canonical valid request: vegetarian, 4 servings, two ingredients. */
    public static CreateRecipeRequest createRequest() {
        return createRequest(DEFAULT_TITLE, 4, true, DEFAULT_INSTRUCTIONS,
                ingredient("onion"), ingredient("garlic"));
    }

    public static CreateRecipeRequest createRequest(String title, IngredientRequest... ingredients) {
        return createRequest(title, 4, true, DEFAULT_INSTRUCTIONS, ingredients);
    }

    public static CreateRecipeRequest createRequest(
            String title,
            int servings,
            boolean vegetarian,
            String instructions,
            IngredientRequest... ingredients) {

        return new CreateRecipeRequest(
                title,
                "A fixture recipe.",
                servings,
                vegetarian,
                instructions,
                List.copyOf(Arrays.asList(ingredients)));
    }

    public static Recipe recipe() {
        return Recipe.from(createRequest());
    }

    public static Recipe recipe(String title, IngredientRequest... ingredients) {
        return Recipe.from(createRequest(title, ingredients));
    }

    public static Recipe recipe(
            String title,
            int servings,
            boolean vegetarian,
            String instructions,
            IngredientRequest... ingredients) {

        return Recipe.from(createRequest(title, servings, vegetarian, instructions, ingredients));
    }
}
