package com.lyzer.lyzerrecime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyzer.lyzerrecime.recipe.dto.CreateRecipeRequest;
import com.lyzer.lyzerrecime.recipe.dto.UpdateRecipeRequest;
import com.lyzer.lyzerrecime.support.AbstractPostgresTest;
import com.lyzer.lyzerrecime.support.RecipeFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The only place the real controller, real service, real repository and real
 * PostgreSQL meet. Every other test mocks one side or the other: the
 * {@code @WebMvcTest} slices mock the service away, the {@code @DataJpaTest}
 * classes never load the web layer, and both roll back. Here the writes commit,
 * so each step actually sees the previous step's state.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecipeApiIntegrationTest extends AbstractPostgresTest {

    // Distinctive enough that the search step matches this recipe and nothing
    // else another test may have committed to the shared container.
    private static final String INGREDIENT = "kohlrabi";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsFetchesSearchesUpdatesAndDeletesARecipe() throws Exception {
        CreateRecipeRequest create = RecipeFixtures.createRequest(
                "Kohlrabi Soup", 4, true, "Simmer for 20 minutes, then blend.",
                RecipeFixtures.ingredient(INGREDIENT, BigDecimal.valueOf(2), "piece"),
                RecipeFixtures.ingredient("garlic", BigDecimal.valueOf(1), "clove"));

        String createdBody = mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Kohlrabi Soup"))
                .andExpect(jsonPath("$.ingredients.length()").value(2))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(createdBody).get("id").asLong();

        // Fetch: the @EntityGraph path with open-in-view=false. A slice test
        // cannot reach this — only a real service call can prove the children
        // are initialized before the transaction closes.
        mockMvc.perform(get("/api/recipes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.ingredients[*].name")
                        .value(containsInAnyOrder(INGREDIENT, "garlic")));

        mockMvc.perform(get("/api/recipes")
                        .param("includeIngredients", INGREDIENT)
                        .param("vegetarian", "true")
                        .param("minServings", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));

        // Full replacement keeping one ingredient name: the orphan deletes must
        // be flushed before the reinserts or uq_recipe_ingredient_name rejects
        // them.
        UpdateRecipeRequest update = RecipeFixtures.updateRequest(
                "Kohlrabi Stew", 6, false, "Brown the beef, then simmer for an hour.",
                RecipeFixtures.ingredient(INGREDIENT, BigDecimal.valueOf(3), "piece"),
                RecipeFixtures.ingredient("beef", BigDecimal.valueOf(500), "g"));

        mockMvc.perform(put("/api/recipes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Kohlrabi Stew"))
                .andExpect(jsonPath("$.servings").value(6))
                .andExpect(jsonPath("$.vegetarian").value(false))
                .andExpect(jsonPath("$.ingredients.length()").value(2));

        // Re-fetch rather than trusting the update response: this is what
        // proves the change was committed, not merely mapped.
        mockMvc.perform(get("/api/recipes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Kohlrabi Stew"))
                .andExpect(jsonPath("$.ingredients[*].name")
                        .value(containsInAnyOrder(INGREDIENT, "beef")));

        mockMvc.perform(delete("/api/recipes/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/api/recipes/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }
}
