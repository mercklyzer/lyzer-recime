package com.lyzer.lyzerrecime.recipe;

import com.lyzer.lyzerrecime.common.config.JpaAuditingConfig;
import com.lyzer.lyzerrecime.support.AbstractPostgresTest;
import com.lyzer.lyzerrecime.support.RecipeFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class RecipeRepositorySearchTest extends AbstractPostgresTest {

    private static final PageRequest FIRST_PAGE = PageRequest.of(0, 10);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RecipeRepository recipeRepository;

    @Test
    void returnsOnlyVegetarianRecipesWhenVegetarianIsTrue() {
        persist("Onion Soup", 4, true);
        persist("Pork Belly", 4, false);

        var page = recipeRepository.findAll(RecipeSpecifications.vegetarian(true), FIRST_PAGE);

        assertThat(page.getContent())
                .extracting(Recipe::getTitle)
                .containsExactly("Onion Soup");
    }

    @Test
    void matchesExactServings() {
        persistServingsSpread();

        var page = recipeRepository.findAll(RecipeSpecifications.servingsEquals(4), FIRST_PAGE);

        assertThat(page.getContent())
                .extracting(Recipe::getTitle)
                .containsExactly("Family Dinner");
    }

    @Test
    void matchesMinimumServings() {
        persistServingsSpread();

        var page = recipeRepository.findAll(RecipeSpecifications.servingsAtLeast(4), FIRST_PAGE);

        assertThat(page.getContent())
                .extracting(Recipe::getTitle)
                .containsExactlyInAnyOrder("Family Dinner", "Party Platter");
    }

    @Test
    void matchesMaximumServings() {
        persistServingsSpread();

        var page = recipeRepository.findAll(RecipeSpecifications.servingsAtMost(4), FIRST_PAGE);

        assertThat(page.getContent())
                .extracting(Recipe::getTitle)
                .containsExactlyInAnyOrder("Dinner For Two", "Family Dinner");
    }

    @Test
    void combinesMinAndMaxServings() {
        persistServingsSpread();

        var spec = RecipeSpecifications.servingsAtLeast(4)
                .and(RecipeSpecifications.servingsAtMost(6));
        var page = recipeRepository.findAll(spec, FIRST_PAGE);

        assertThat(page.getContent())
                .extracting(Recipe::getTitle)
                .containsExactly("Family Dinner");
    }

    @Test
    void matchesInstructionsCaseInsensitively() {
        persistWithInstructions("Roast Vegetables", "Preheat the oven to 200C.");
        persistWithInstructions("Garden Salad", "Toss everything in a bowl.");

        var page = recipeRepository.findAll(
                RecipeSpecifications.instructionsContain("OVEN"), FIRST_PAGE);

        assertThat(page.getContent())
                .extracting(Recipe::getTitle)
                .containsExactly("Roast Vegetables");
    }

    @Test
    void treatsPercentInInstructionSearchLiterally() {
        persistWithInstructions("Red Wine Reduction", "Reduce by 50% then serve.");
        persistWithInstructions("Slow Braise", "Cook for 50 minutes then serve.");

        var page = recipeRepository.findAll(
                RecipeSpecifications.instructionsContain("50%"), FIRST_PAGE);

        assertThat(page.getContent())
                .extracting(Recipe::getTitle)
                .containsExactly("Red Wine Reduction");
    }

    @Test
    void requiresAllIncludedIngredients() {
        persistWithIngredients("Onion Only", "onion");
        persistWithIngredients("Onion And Garlic", "onion", "garlic");

        var page = recipeRepository.findAll(
                RecipeSpecifications.includesAllIngredients(List.of("onion", "garlic")), FIRST_PAGE);

        assertThat(page.getContent())
                .extracting(Recipe::getTitle)
                .containsExactly("Onion And Garlic");
    }

    @Test
    void matchesIncludedIngredientIgnoringCase() {
        persistWithIngredients("Onion Soup", "onion");
        persistWithIngredients("Pork Belly", "pork");

        var page = recipeRepository.findAll(
                RecipeSpecifications.includesAllIngredients(List.of("ONION")), FIRST_PAGE);

        assertThat(page.getContent())
                .extracting(Recipe::getTitle)
                .containsExactly("Onion Soup");
    }

    @Test
    void doesNotMatchIngredientByPartialName() {
        persistWithIngredients("Spring Onion Salad", "spring onion");

        var page = recipeRepository.findAll(
                RecipeSpecifications.includesAllIngredients(List.of("onion")), FIRST_PAGE);

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void excludesRecipesContainingAnyExcludedIngredient() {
        persistWithIngredients("Pork Stew", "pork", "onion", "garlic");
        persistWithIngredients("Onion Soup", "onion", "garlic");

        var page = recipeRepository.findAll(
                RecipeSpecifications.excludesAllIngredients(List.of("pork")), FIRST_PAGE);

        assertThat(page.getContent())
                .extracting(Recipe::getTitle)
                .containsExactly("Onion Soup");
    }

    @Test
    void excludeAndIncludeCombine() {
        persistWithIngredients("Pork Stew", "pork", "onion", "garlic");
        persistWithIngredients("Onion Soup", "onion", "garlic");
        persistWithIngredients("Plain Rice", "rice");

        var spec = RecipeSpecifications.includesAllIngredients(List.of("onion"))
                .and(RecipeSpecifications.excludesAllIngredients(List.of("pork")));
        var page = recipeRepository.findAll(spec, FIRST_PAGE);

        assertThat(page.getContent())
                .extracting(Recipe::getTitle)
                .containsExactly("Onion Soup");
    }

    private void persistWithIngredients(String title, String... ingredientNames) {
        var ingredients = java.util.Arrays.stream(ingredientNames)
                .map(RecipeFixtures::ingredient)
                .toArray(com.lyzer.lyzerrecime.recipe.dto.IngredientRequest[]::new);
        entityManager.persist(RecipeFixtures.recipe(title, ingredients));
    }

    private void persistWithInstructions(String title, String instructions) {
        entityManager.persist(RecipeFixtures.recipe(
                title, 4, true, instructions, RecipeFixtures.ingredient("onion")));
    }

    private void persistServingsSpread() {
        persist("Dinner For Two", 2, true);
        persist("Family Dinner", 4, true);
        persist("Party Platter", 8, true);
    }

    private void persist(String title, int servings, boolean vegetarian) {
        entityManager.persist(RecipeFixtures.recipe(
                title, servings, vegetarian, RecipeFixtures.DEFAULT_INSTRUCTIONS,
                RecipeFixtures.ingredient("onion")));
    }
}
