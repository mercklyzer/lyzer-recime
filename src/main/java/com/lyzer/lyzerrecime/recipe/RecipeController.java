package com.lyzer.lyzerrecime.recipe;

import com.lyzer.lyzerrecime.recipe.dto.CreateRecipeRequest;
import com.lyzer.lyzerrecime.recipe.dto.RecipeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @PostMapping
    public ResponseEntity<RecipeResponse> newRecipe(
            @Valid @RequestBody CreateRecipeRequest request,
            UriComponentsBuilder uriBuilder) {

        RecipeResponse created = recipeService.create(request);

        // Built from the injected builder rather than a hardcoded
        // "/api/recipes/" so the URI stays correct behind a context path or
        // proxy.
        URI location = uriBuilder.path("/api/recipes/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public RecipeResponse one(@PathVariable Long id) {
        // Returning the body directly means 200. ResponseEntity is only needed
        // where the status or headers vary.
        return recipeService.findById(id);
    }
}
