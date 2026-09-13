package com.lyzer.lyzerrecime.recipe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyzer.lyzerrecime.common.error.RecipeNotFoundException;
import com.lyzer.lyzerrecime.recipe.dto.IngredientResponse;
import com.lyzer.lyzerrecime.recipe.dto.RecipeResponse;
import com.lyzer.lyzerrecime.recipe.dto.UpdateRecipeRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
class RecipeControllerUpdateDeleteTest {

    private static final Instant TIMESTAMP = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RecipeService recipeService;

    @Test
    void returns200WithReplacedRecipe() throws Exception {
        given(recipeService.update(eq(1L), any(UpdateRecipeRequest.class))).willReturn(response());

        mockMvc.perform(put("/api/recipes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Leek Soup"))
                .andExpect(jsonPath("$.servings").value(6))
                .andExpect(jsonPath("$.vegetarian").value(false))
                .andExpect(jsonPath("$.ingredients.length()").value(1))
                .andExpect(jsonPath("$.ingredients[0].name").value("leek"));
    }

    @Test
    void returns404ProblemDetailWhenReplacingUnknownRecipe() throws Exception {
        willThrow(new RecipeNotFoundException(99L))
                .given(recipeService).update(eq(99L), any(UpdateRecipeRequest.class));

        mockMvc.perform(put("/api/recipes/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Recipe 99 not found"));
    }

    @Test
    void returns400WithPerFieldErrorsWhenReplacementIsInvalid() throws Exception {
        String body = """
                {"title":"  ","servings":0,"instructions":"","ingredients":[]}
                """;

        mockMvc.perform(put("/api/recipes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.servings").exists())
                .andExpect(jsonPath("$.errors.vegetarian").exists())
                .andExpect(jsonPath("$.errors.instructions").exists())
                .andExpect(jsonPath("$.errors.ingredients").exists());

        then(recipeService).should(never()).update(any(), any());
    }

    @Test
    void returns400WhenReplacementRepeatsAnIngredientName() throws Exception {
        UpdateRecipeRequest duplicates = RecipeFixtures.updateRequest("Onion Soup",
                RecipeFixtures.ingredient("Onion"),
                RecipeFixtures.ingredient(" onion "));

        mockMvc.perform(put("/api/recipes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicates)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.ingredients").exists());
    }

    @Test
    void returns204WithEmptyBodyOnDelete() throws Exception {
        var result = mockMvc.perform(delete("/api/recipes/1"))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEmpty();
        then(recipeService).should().delete(1L);
    }

    @Test
    void returns404ProblemDetailWhenDeletingUnknownRecipe() throws Exception {
        willThrow(new RecipeNotFoundException(99L)).given(recipeService).delete(99L);

        mockMvc.perform(delete("/api/recipes/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Recipe 99 not found"));
    }

    private static UpdateRecipeRequest updateRequest() {
        return RecipeFixtures.updateRequest("Leek Soup", 6, false,
                RecipeFixtures.DEFAULT_INSTRUCTIONS, RecipeFixtures.ingredient("leek"));
    }

    private static RecipeResponse response() {
        var request = updateRequest();
        return new RecipeResponse(
                1L,
                request.title(),
                request.description(),
                request.servings(),
                request.vegetarian(),
                request.instructions(),
                List.of(new IngredientResponse("leek", BigDecimal.valueOf(1), "piece")),
                TIMESTAMP,
                TIMESTAMP);
    }
}
