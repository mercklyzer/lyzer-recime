package com.lyzer.lyzerrecime.recipe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyzer.lyzerrecime.recipe.dto.CreateRecipeRequest;
import com.lyzer.lyzerrecime.recipe.dto.IngredientResponse;
import com.lyzer.lyzerrecime.recipe.dto.RecipeResponse;
import com.lyzer.lyzerrecime.support.RecipeFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
class RecipeControllerCreateTest {

    private static final Instant TIMESTAMP = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // @MockitoBean, not the deprecated @MockBean.
    @MockitoBean
    private RecipeService recipeService;

    @BeforeEach
    void stubCreate() {
        given(recipeService.create(any(CreateRecipeRequest.class))).willReturn(response());
    }

    @Test
    void returns201WithLocationHeaderOnCreate() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RecipeFixtures.createRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/recipes/1")));
    }

    @Test
    void returnsCreatedRecipeBody() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RecipeFixtures.createRequest())))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value(RecipeFixtures.DEFAULT_TITLE))
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
    void returns400WithPerFieldErrorsWhenRequestIsInvalid() throws Exception {
        CreateRecipeRequest invalid = new CreateRecipeRequest(
                "  ", null, 0, null, "", List.of(RecipeFixtures.ingredient("  ")));

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors.length()").value(5))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.servings").exists())
                .andExpect(jsonPath("$.errors.vegetarian").exists())
                .andExpect(jsonPath("$.errors.instructions").exists())
                .andExpect(jsonPath("$.errors['ingredients[0].name']").exists());
    }

    @Test
    void returns400WhenIngredientNamesAreDuplicatedIgnoringCase() throws Exception {
        CreateRecipeRequest duplicates = RecipeFixtures.createRequest(
                RecipeFixtures.DEFAULT_TITLE,
                RecipeFixtures.ingredient("onion"),
                RecipeFixtures.ingredient("Onion"));

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicates)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.errors.ingredients")
                        .value("ingredient names must be unique within a recipe"));
    }

    @Test
    void returns400WhenBodyIsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    /** The persisted shape the service would return — id and timestamps assigned. */
    private static RecipeResponse response() {
        CreateRecipeRequest request = RecipeFixtures.createRequest();
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
