package com.lyzer.lyzerrecime.recipe;

import com.lyzer.lyzerrecime.recipe.dto.IngredientResponse;
import com.lyzer.lyzerrecime.recipe.dto.RecipeSearchCriteria;
import com.lyzer.lyzerrecime.recipe.dto.RecipeSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
class RecipeControllerSearchTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeService recipeService;

    @BeforeEach
    void stubEmptySearch() {
        given(recipeService.search(any(), any())).willReturn(Page.empty());
    }

    @Test
    void returns200WithPagedSummaries() throws Exception {
        var summary = new RecipeSummaryResponse(
                1L, "Soup", "A fixture recipe.", 4, true,
                List.of(
                        new IngredientResponse("onion", BigDecimal.valueOf(2), "piece"),
                        new IngredientResponse("garlic", null, null)));
        given(recipeService.search(any(), any()))
                .willReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 25));

        mockMvc.perform(get("/api/recipes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Soup"))
                .andExpect(jsonPath("$.content[0].description").value("A fixture recipe."))
                .andExpect(jsonPath("$.content[0].servings").value(4))
                .andExpect(jsonPath("$.content[0].vegetarian").value(true))
                .andExpect(jsonPath("$.content[0].ingredients.length()").value(2))
                .andExpect(jsonPath("$.content[0].ingredients[0].name").value("onion"))
                .andExpect(jsonPath("$.content[0].ingredients[0].quantity").value(2))
                .andExpect(jsonPath("$.content[0].ingredients[0].unit").value("piece"))
                // Unquantified ingredients keep their nulls rather than a formatted string.
                .andExpect(jsonPath("$.content[0].ingredients[1].name").value("garlic"))
                .andExpect(jsonPath("$.content[0].ingredients[1].quantity").doesNotExist())
                // Instructions stay out of the summary.
                .andExpect(jsonPath("$.content[0].instructions").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void bindsQueryParametersToSearchCriteria() throws Exception {
        mockMvc.perform(get("/api/recipes")
                        .param("vegetarian", "true")
                        .param("minServings", "2")
                        .param("maxServings", "6")
                        .param("instructions", "simmer")
                        // Comma form and repeated form both bind to a List.
                        .param("includeIngredients", "onion,garlic")
                        .param("excludeIngredients", "pork")
                        .param("excludeIngredients", "beef"))
                .andExpect(status().isOk());

        assertThat(capturedCriteria()).satisfies(criteria -> {
            assertThat(criteria.vegetarian()).isTrue();
            assertThat(criteria.servings()).isNull();
            assertThat(criteria.minServings()).isEqualTo(2);
            assertThat(criteria.maxServings()).isEqualTo(6);
            assertThat(criteria.instructions()).isEqualTo("simmer");
            assertThat(criteria.includeIngredients()).containsExactly("onion", "garlic");
            assertThat(criteria.excludeIngredients()).containsExactly("pork", "beef");
        });
    }

    @Test
    void bindsEmptyListsWhenNoIngredientFilterSupplied() throws Exception {
        mockMvc.perform(get("/api/recipes")).andExpect(status().isOk());

        assertThat(capturedCriteria().includeIngredients()).isEmpty();
        assertThat(capturedCriteria().excludeIngredients()).isEmpty();
    }

    @Test
    void defaultsToPageSizeOfTwentySortedByTitleThenId() throws Exception {
        mockMvc.perform(get("/api/recipes")).andExpect(status().isOk());

        Pageable pageable = capturedPageable();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        // The id tiebreaker is what keeps paging stable across duplicate titles.
        assertThat(pageable.getSort()).containsExactly(
                Sort.Order.asc("title"), Sort.Order.asc("id"));
    }

    @Test
    void appendsIdTiebreakerToAnExplicitSort() throws Exception {
        mockMvc.perform(get("/api/recipes").param("sort", "servings,desc").param("size", "5"))
                .andExpect(status().isOk());

        Pageable pageable = capturedPageable();
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort()).containsExactly(
                Sort.Order.desc("servings"), Sort.Order.asc("id"));
    }

    @Test
    void returns400WhenSortPropertyIsNotAllowed() throws Exception {
        mockMvc.perform(get("/api/recipes").param("sort", "instructions"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid sort property"))
                .andExpect(jsonPath("$.detail").value(
                        startsWith("Cannot sort by 'instructions'")))
                .andExpect(jsonPath("$.timestamp").exists());

        then(recipeService).should(never()).search(any(), any());
    }

    @Test
    void returns400WhenServingsIsNotANumber() throws Exception {
        // Record constructor binding fails inside constructAttribute, so the
        // conversion error arrives as a MethodArgumentNotValidException field
        // error rather than a TypeMismatchException.
        mockMvc.perform(get("/api/recipes").param("servings", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.servings").exists());
    }

    @Test
    void returns400WhenServingsIsNotPositive() throws Exception {
        mockMvc.perform(get("/api/recipes").param("servings", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.servings").value("must be greater than 0"));
    }

    @Test
    void returns400WhenServingsIsCombinedWithARange() throws Exception {
        mockMvc.perform(get("/api/recipes").param("servings", "4").param("minServings", "6"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.servingsFilterUnambiguous").value(
                        "servings cannot be combined with minServings or maxServings"));
    }

    @Test
    void returns400WhenMinServingsExceedsMaxServings() throws Exception {
        mockMvc.perform(get("/api/recipes").param("minServings", "6").param("maxServings", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.servingsRangeOrdered").value(
                        "minServings must not exceed maxServings"));
    }

    private RecipeSearchCriteria capturedCriteria() {
        var captor = ArgumentCaptor.forClass(RecipeSearchCriteria.class);
        then(recipeService).should().search(captor.capture(), any());
        return captor.getValue();
    }

    private Pageable capturedPageable() {
        var captor = ArgumentCaptor.forClass(Pageable.class);
        then(recipeService).should().search(any(), captor.capture());
        return captor.getValue();
    }
}
