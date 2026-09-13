package com.lyzer.lyzerrecime.recipe;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// Package-private: the compiler enforces that nothing outside the recipe
// package touches persistence directly.
interface RecipeRepository extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {
    @EntityGraph(attributePaths = "ingredients")
    Optional<Recipe> findWithIngredientsById(Long id);

    /**
     * The second half of the two-query search: one IN lookup covering the whole
     * page, rather than a lazy load per row (N+1) or a collection join, which
     * would force Hibernate to paginate the result in memory.
     */
    @Query("""
            select new com.lyzer.lyzerrecime.recipe.IngredientRow(
                ri.recipe.id, ri.name, ri.quantity, ri.unit)
            from RecipeIngredient ri
            where ri.recipe.id in :recipeIds
            order by ri.recipe.id, ri.displayOrder
            """)
    List<IngredientRow> findIngredientsByRecipeIds(
            @Param("recipeIds") Collection<Long> recipeIds);
}
