package com.lyzer.lyzerrecime.recipe;

import com.lyzer.lyzerrecime.common.error.RecipeNotFoundException;
import com.lyzer.lyzerrecime.recipe.dto.CreateRecipeRequest;
import com.lyzer.lyzerrecime.recipe.dto.IngredientResponse;
import com.lyzer.lyzerrecime.recipe.dto.RecipeResponse;
import com.lyzer.lyzerrecime.recipe.dto.RecipeSearchCriteria;
import com.lyzer.lyzerrecime.recipe.dto.RecipeSummaryResponse;
import com.lyzer.lyzerrecime.recipe.dto.UpdateRecipeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;

    /**
     *  No @Transactional: SimpleJpaRepository.save is itself  transactional, so a
     *  single save — children included — is already atomic. Add one if create ever
     *  performs a second write or reads something it then writes based on.
     */
    public RecipeResponse create(CreateRecipeRequest request) {
        Recipe saved = recipeRepository.save(Recipe.from(request));
        return RecipeResponse.from(saved);
    }

    public RecipeResponse findById(Long id) {
        // Optional stops here: the controller gets a response or an exception,
        // which is what maps cleanly onto HTTP.
        return recipeRepository.findWithIngredientsById(id)
                .map(RecipeResponse::from)
                .orElseThrow(() -> new RecipeNotFoundException(id));
    }

    /**
     * Two statements, deliberately: the paged query, then one IN lookup for the
     * ingredients of the rows it returned. A collection join would make
     * Hibernate paginate the page in memory; a lazy load per row is the N+1.
     */
    public Page<RecipeSummaryResponse> search(RecipeSearchCriteria criteria, Pageable pageable) {
        Page<Recipe> page =
                recipeRepository.findAll(RecipeSpecifications.matching(criteria), pageable);
        Map<Long, List<IngredientResponse>> ingredientsByRecipeId =
                ingredientsFor(page.getContent());

        return page.map(recipe -> RecipeSummaryResponse.from(
                recipe, ingredientsByRecipeId.getOrDefault(recipe.getId(), List.of())));
    }

    @Transactional
    public RecipeResponse update(Long id, UpdateRecipeRequest request) {
        Recipe recipe = recipeRepository.findWithIngredientsById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
        recipe.applyUpdate(request);
        return RecipeResponse.from(recipe);
    }

    @Transactional
    public void delete(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
        recipeRepository.delete(recipe);
    }

    private Map<Long, List<IngredientResponse>> ingredientsFor(List<Recipe> recipes) {
        if (recipes.isEmpty()) {
            return Map.of();  // nothing to look up — skip the round-trip
        }

        List<Long> recipeIds = recipes.stream().map(Recipe::getId).toList();

        // The query orders by displayOrder and groupingBy preserves encounter
        // order, so each list keeps the order the recipe defines.
        return recipeRepository.findIngredientsByRecipeIds(recipeIds).stream()
                .collect(groupingBy(
                        IngredientRow::recipeId,
                        mapping(IngredientRow::toResponse, toList())));
    }
}
