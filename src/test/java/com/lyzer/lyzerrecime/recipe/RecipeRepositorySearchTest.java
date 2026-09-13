package com.lyzer.lyzerrecime.recipe;

import com.lyzer.lyzerrecime.common.config.JpaAuditingConfig;
import com.lyzer.lyzerrecime.recipe.dto.IngredientRequest;
import com.lyzer.lyzerrecime.recipe.dto.RecipeSearchCriteria;
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

import java.util.Arrays;
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

    @Test
    void returnsAllRecipesWhenNoFilterSupplied() {
        persistServingsSpread();

        var criteria = new RecipeSearchCriteria(null, null, null, null, null, null, null);
        var page = recipeRepository.findAll(RecipeSpecifications.matching(criteria), FIRST_PAGE);

        assertThat(page.getContent())
                .extracting(Recipe::getTitle)
                .containsExactlyInAnyOrder("Dinner For Two", "Family Dinner", "Party Platter");
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void appliesAllFiltersTogether() {
        persistFull("Garlic Onion Soup", 4, true, RecipeFixtures.DEFAULT_INSTRUCTIONS, "onion", "garlic");
        persistFull("Steak Onion Soup", 4, false, RecipeFixtures.DEFAULT_INSTRUCTIONS, "onion", "garlic");
        persistFull("Onion Soup For Twelve", 12, true, RecipeFixtures.DEFAULT_INSTRUCTIONS, "onion", "garlic");
        persistFull("Plain Onion Soup", 4, true, RecipeFixtures.DEFAULT_INSTRUCTIONS, "onion");
        persistFull("Pork Onion Soup", 4, true, RecipeFixtures.DEFAULT_INSTRUCTIONS, "onion", "garlic", "pork");
        persistFull("No Bake Onion Salad", 4, true, "Toss everything in a bowl.", "onion", "garlic");

        var criteria = new RecipeSearchCriteria(
                true, null, 2, 6, List.of("Onion", "garlic"), List.of("pork"), "oven");
        var page = recipeRepository.findAll(RecipeSpecifications.matching(criteria), FIRST_PAGE);

        assertThat(page.getContent())
                .extracting(Recipe::getTitle)
                .containsExactly("Garlic Onion Soup");
    }

    // The guard against a join-based implementation: joins multiply rows, so
    // the count query would report the joined row count rather than 25.
    @Test
    void paginatesResultsAndReportsCorrectTotal() {
        for (int i = 1; i <= 25; i++) {
            persistFull("Onion Soup " + i, 4, true, RecipeFixtures.DEFAULT_INSTRUCTIONS, "onion", "garlic");
        }
        persistFull("Pork Stew", 4, true, RecipeFixtures.DEFAULT_INSTRUCTIONS, "onion", "garlic", "pork");

        var criteria = new RecipeSearchCriteria(
                null, null, null, null, List.of("onion", "garlic"), List.of("pork"), null);
        var page = recipeRepository.findAll(RecipeSpecifications.matching(criteria), FIRST_PAGE);

        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalElements()).isEqualTo(25);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    private void persistWithIngredients(String title, String... ingredientNames) {
        entityManager.persist(RecipeFixtures.recipe(title, ingredients(ingredientNames)));
    }

    private void persistFull(
            String title, int servings, boolean vegetarian, String instructions, String... ingredientNames) {

        entityManager.persist(RecipeFixtures.recipe(
                title, servings, vegetarian, instructions, ingredients(ingredientNames)));
    }

    private IngredientRequest[] ingredients(String... names) {
        return Arrays.stream(names)
                .map(RecipeFixtures::ingredient)
                .toArray(IngredientRequest[]::new);
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
