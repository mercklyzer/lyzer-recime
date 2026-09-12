package com.lyzer.lyzerrecime.recipe;

import com.lyzer.lyzerrecime.common.error.RecipeNotFoundException;
import com.lyzer.lyzerrecime.recipe.dto.IngredientResponse;
import com.lyzer.lyzerrecime.recipe.dto.RecipeResponse;
import com.lyzer.lyzerrecime.support.RecipeFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
class RecipeControllerReadTest {

    private static final Instant TIMESTAMP = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeService recipeService;

    @Test
    void returns200WithRecipeAndItsIngredients() throws Exception {
        given(recipeService.findById(1L)).willReturn(response());

        mockMvc.perform(get("/api/recipes/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value(RecipeFixtures.DEFAULT_TITLE))
                .andExpect(jsonPath("$.description").value("A fixture recipe."))
                .andExpect(jsonPath("$.servings").value(4))
                .andExpect(jsonPath("$.vegetarian").value(true))
                .andExpect(jsonPath("$.instructions").value(RecipeFixtures.DEFAULT_INSTRUCTIONS))
                .andExpect(jsonPath("$.ingredients.length()").value(2))
                .andExpect(jsonPath("$.ingredients[0].name").value("onion"))
                .andExpect(jsonPath("$.ingredients[0].quantity").value(1))
                .andExpect(jsonPath("$.ingredients[0].unit").value("piece"))
                .andExpect(jsonPath("$.ingredients[1].name").value("garlic"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void returns404ProblemDetailWhenRecipeDoesNotExist() throws Exception {
        willThrow(new RecipeNotFoundException(99L)).given(recipeService).findById(99L);

        mockMvc.perform(get("/api/recipes/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recipe not found"))
                .andExpect(jsonPath("$.detail").value("Recipe 99 not found"))
                .andExpect(jsonPath("$.type")
                        .value("https://api.lyzer.dev/problems/recipe-not-found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void returns400WhenIdIsNotANumber() throws Exception {
        mockMvc.perform(get("/api/recipes/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private static RecipeResponse response() {
        var request = RecipeFixtures.createRequest();
        return new RecipeResponse(
                1L,
                request.title(),
                request.description(),
                request.servings(),
                request.vegetarian(),
                request.instructions(),
                List.of(
                        new IngredientResponse("onion", BigDecimal.valueOf(1), "piece"),
                        new IngredientResponse("garlic", BigDecimal.valueOf(1), "piece")),
                TIMESTAMP,
                TIMESTAMP);
    }
}
