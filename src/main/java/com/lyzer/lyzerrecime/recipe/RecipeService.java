package com.lyzer.lyzerrecime.recipe;

import com.lyzer.lyzerrecime.common.error.RecipeNotFoundException;
import com.lyzer.lyzerrecime.recipe.dto.CreateRecipeRequest;
import com.lyzer.lyzerrecime.recipe.dto.RecipeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
