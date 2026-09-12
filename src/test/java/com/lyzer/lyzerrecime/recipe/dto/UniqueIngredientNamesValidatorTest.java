package com.lyzer.lyzerrecime.recipe.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.lyzer.lyzerrecime.support.RecipeFixtures.ingredient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class UniqueIngredientNamesValidatorTest {

    private static final String MESSAGE = "ingredient names must be unique within a recipe";

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void startValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void rejectsDuplicateNamesDifferingOnlyByCase() {
        assertThat(uniquenessViolations(List.of(ingredient("Onion"), ingredient("onion"))))
                .singleElement()
                .satisfies(violation ->
                        assertThat(violation.getPropertyPath()).hasToString("ingredients"));
    }

    @Test
    void rejectsDuplicateNamesDifferingOnlyBySurroundingWhitespace() {
        assertThat(uniquenessViolations(List.of(ingredient("onion"), ingredient("  onion  "))))
                .hasSize(1);
    }

    @Test
    void acceptsDistinctNames() {
        assertThat(uniquenessViolations(List.of(ingredient("onion"), ingredient("garlic"))))
                .isEmpty();
    }

    @Test
    void acceptsEmptyList() {
        assertThat(uniquenessViolations(List.of())).isEmpty();
    }

    @Test
    void acceptsNullList() {
        assertThat(uniquenessViolations(null)).isEmpty();
    }

    @Test
    void acceptsBlankNamesSinceNotBlankOwnsThatCase() {
        assertThat(uniquenessViolations(Arrays.asList(ingredient("onion"), ingredient(null))))
                .isEmpty();
    }

    @Test
    void doesNotThrowOnNullElement() {
        assertThatCode(() -> uniquenessViolations(Arrays.asList(ingredient("onion"), null)))
                .doesNotThrowAnyException();
    }

    @Test
    void reportsOnTheIngredientsFieldOfAWholeRequest() {
        CreateRecipeRequest request = new CreateRecipeRequest(
                "Onion Soup", "A duplicate-laden recipe.", 4, true, "Simmer.",
                List.of(ingredient("Onion"), ingredient("onion")));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessage)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("ingredients", MESSAGE));
    }

    /** Only the uniqueness violations — @NotEmpty owns the empty and null cases. */
    private List<ConstraintViolation<CreateRecipeRequest>> uniquenessViolations(List<IngredientRequest> ingredients) {
        return validator.validateValue(CreateRecipeRequest.class, "ingredients", ingredients).stream()
                .filter(violation -> MESSAGE.equals(violation.getMessage()))
                .toList();
    }
}
