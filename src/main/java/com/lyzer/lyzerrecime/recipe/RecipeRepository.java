package com.lyzer.lyzerrecime.recipe;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Package-private: the compiler enforces that nothing outside the recipe
// package touches persistence directly.
interface RecipeRepository extends JpaRepository<Recipe, Long> {
    @EntityGraph(attributePaths = "ingredients")
    Optional<Recipe> findWithIngredientsById(Long id);
}
