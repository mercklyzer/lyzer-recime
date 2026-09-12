package com.lyzer.lyzerrecime.recipe;

import com.lyzer.lyzerrecime.support.AbstractPostgresTest;
import com.lyzer.lyzerrecime.support.RecipeFixtures;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the entity mapping and the Flyway migration agree, that cascade and
 * orphanRemoval reach the database, and that auditing populates the timestamps
 * without a setter in sight.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RecipePersistenceTest extends AbstractPostgresTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void roundTripsRecipeWithItsIngredients() {
        var request = RecipeFixtures.createRequest(
                "  Garlic Onion Soup  ", 4, true, RecipeFixtures.DEFAULT_INSTRUCTIONS,
                RecipeFixtures.ingredient("Onion", BigDecimal.valueOf(2), " grams "),
                RecipeFixtures.ingredient("GARLIC", new BigDecimal("0.500"), "clove"));

        var id = entityManager.persistAndGetId(Recipe.from(request), Long.class);
        entityManager.flush();
        entityManager.clear();

        var found = entityManager.find(Recipe.class, id);

        assertThat(found.getTitle()).isEqualTo("Garlic Onion Soup");
        assertThat(found.getServings()).isEqualTo(4);
        assertThat(found.isVegetarian()).isTrue();
        assertThat(found.getIngredients())
                .extracting(RecipeIngredient::getName, RecipeIngredient::getDisplayOrder)
                .containsExactly(tuple("onion", 0), tuple("garlic", 1));
        assertThat(found.getIngredients().get(0).getUnit()).isEqualTo("grams");
        assertThat(found.getIngredients().get(0).getQuantity()).isEqualByComparingTo("2");
    }

    @Test
    void populatesAuditTimestampsOnInsert() {
        var recipe = entityManager.persistFlushFind(RecipeFixtures.recipe());

        assertThat(recipe.getCreatedAt()).isNotNull();
        assertThat(recipe.getUpdatedAt()).isNotNull();
    }

    @Test
    void deletesIngredientRowsWithTheirRecipe() {
        var recipe = entityManager.persistFlushFind(RecipeFixtures.recipe());

        entityManager.remove(recipe);
        entityManager.flush();

        assertThat(countIngredientRows()).isZero();
    }

    @Test
    void rejectsTwoIngredientsThatNormalizeToTheSameName() {
        var request = RecipeFixtures.createRequest("Onion Soup",
                RecipeFixtures.ingredient("Onion"),
                RecipeFixtures.ingredient(" onion "));

        // IDENTITY generation makes persist() emit the INSERTs immediately, so the
        // constraint fires here rather than at flush.
        var recipe = Recipe.from(request);

        assertThatThrownBy(() -> entityManager.persistAndFlush(recipe))
                .isInstanceOf(ConstraintViolationException.class)
                .hasStackTraceContaining("uq_recipe_ingredient_name");
    }

    private long countIngredientRows() {
        return entityManager.getEntityManager()
                .createQuery("select count(i) from RecipeIngredient i", Long.class)
                .getSingleResult();
    }
}
