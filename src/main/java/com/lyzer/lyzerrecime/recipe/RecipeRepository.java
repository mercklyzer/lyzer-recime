package com.lyzer.lyzerrecime.recipe;

import org.springframework.data.jpa.repository.JpaRepository;

// Package-private: the compiler enforces that nothing outside the recipe
// package touches persistence directly.
interface RecipeRepository extends JpaRepository<Recipe, Long> {
}
