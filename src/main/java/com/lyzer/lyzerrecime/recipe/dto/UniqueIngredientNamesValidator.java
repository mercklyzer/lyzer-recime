package com.lyzer.lyzerrecime.recipe.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.lyzer.lyzerrecime.recipe.IngredientNames;

import java.util.List;
import java.util.Objects;

public class UniqueIngredientNamesValidator
        implements ConstraintValidator<UniqueIngredientNames, List<IngredientRequest>> {

    @Override
    public boolean isValid(List<IngredientRequest> ingredients, ConstraintValidatorContext context) {
        // null is valid here by convention: @NotEmpty owns the null and empty
        // case, and a constraint that reports someone else's failure
        // double-reports. The same reasoning skips null elements and blank
        // names below — those belong to @NotNull and @NotBlank.
        if (ingredients == null) {
            return true;
        }
        List<String> names = ingredients.stream()
                .filter(Objects::nonNull)
                .map(IngredientRequest::name)
                .filter(name -> name != null && !name.isBlank())
                // Normalized the same way the entity is on write, so "Onion"
                // and " onion " collide here exactly as they would in the
                // database.
                .map(IngredientNames::normalize)
                .toList();

        return names.stream().distinct().count() == names.size();
    }
}
