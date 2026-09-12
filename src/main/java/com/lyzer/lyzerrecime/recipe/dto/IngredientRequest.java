package com.lyzer.lyzerrecime.recipe.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * {@code quantity} is nullable for the unquantified ingredient — "salt, to
 * taste" has no number. {@code unit} is nullable for the countable one —
 * "2 eggs" has no unit. {@code @Positive} and {@code @Digits} only fire on a
 * non-null value, so optionality and validity compose.
 */
public record IngredientRequest(

        @NotBlank @Size(max = 120)
        String name,

        @Positive @Digits(integer = 7, fraction = 3)
        BigDecimal quantity,

        @Size(max = 50)
        String unit) {
}
