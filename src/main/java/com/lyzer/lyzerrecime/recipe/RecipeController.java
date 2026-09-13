package com.lyzer.lyzerrecime.recipe;

import com.lyzer.lyzerrecime.common.error.InvalidSortPropertyException;
import com.lyzer.lyzerrecime.recipe.dto.CreateRecipeRequest;
import com.lyzer.lyzerrecime.recipe.dto.PagedResponse;
import com.lyzer.lyzerrecime.recipe.dto.RecipeResponse;
import com.lyzer.lyzerrecime.recipe.dto.RecipeSearchCriteria;
import com.lyzer.lyzerrecime.recipe.dto.RecipeSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private static final Set<String> SORTABLE = Set.of("title", "servings", "createdAt", "id");

    private static final int DEFAULT_PAGE_SIZE = 20;

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

    @GetMapping
    public PagedResponse<RecipeSummaryResponse> all(
            @ParameterObject @Valid RecipeSearchCriteria criteria,
            @ParameterObject
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "title", direction = Sort.Direction.ASC)
            Pageable pageable) {

        return PagedResponse.from(recipeService.search(criteria, sanitize(pageable)));
    }

    private Pageable sanitize(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new InvalidSortPropertyException(order.getProperty(), SORTABLE);
            }
        }
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().and(Sort.by("id")));
    }
}
