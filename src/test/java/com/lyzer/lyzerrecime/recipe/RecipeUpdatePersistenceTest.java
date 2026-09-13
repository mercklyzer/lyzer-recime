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
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class RecipeUpdatePersistenceTest extends AbstractPostgresTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void updatesRecipeKeepingOneIngredientNameUnchanged() {
        var recipe = entityManager.persistFlushFind(RecipeFixtures.recipe(
                "Garlic Onion Soup",
                RecipeFixtures.ingredient("onion"),
                RecipeFixtures.ingredient("garlic")));

        recipe.applyUpdate(RecipeFixtures.updateRequest("Leek Onion Soup",
                RecipeFixtures.ingredient("Onion"),
                RecipeFixtures.ingredient("leek")));

        // Without this the test rolls back having emitted no SQL: the unique
        // constraint is never evaluated and the ordering it exists to check is
        // never exercised. The orphan DELETE of the old "onion" row must reach
        // PostgreSQL before the INSERT of the new one.
        entityManager.flush();
        entityManager.clear();

        var reloaded = entityManager.find(Recipe.class, recipe.getId());

        assertThat(reloaded.getTitle()).isEqualTo("Leek Onion Soup");
        assertThat(reloaded.getIngredients())
                .extracting(RecipeIngredient::getName, RecipeIngredient::getDisplayOrder)
                .containsExactly(tuple("onion", 0), tuple("leek", 1));
    }

    @Test
    void replacesScalarFieldsAndRemovesDroppedIngredientRows() {
        var recipe = entityManager.persistFlushFind(RecipeFixtures.recipe());

        recipe.applyUpdate(RecipeFixtures.updateRequest(
                "  Leek Soup  ", 6, false, "Boil the leeks.",
                RecipeFixtures.ingredient("Leek")));

        entityManager.flush();
        entityManager.clear();

        var reloaded = entityManager.find(Recipe.class, recipe.getId());

        assertThat(reloaded.getTitle()).isEqualTo("Leek Soup");
        assertThat(reloaded.getServings()).isEqualTo(6);
        assertThat(reloaded.isVegetarian()).isFalse();
        assertThat(reloaded.getInstructions()).isEqualTo("Boil the leeks.");
        assertThat(reloaded.getIngredients())
                .extracting(RecipeIngredient::getName)
                .containsExactly("leek");
        assertThat(countIngredientRows()).isEqualTo(1);
    }

    private long countIngredientRows() {
        return entityManager.getEntityManager()
                .createQuery("select count(i) from RecipeIngredient i", Long.class)
                .getSingleResult();
    }
}
