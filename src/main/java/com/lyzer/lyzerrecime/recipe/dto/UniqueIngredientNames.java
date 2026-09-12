package com.lyzer.lyzerrecime.recipe.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code ["onion", "Onion"]} is a malformed request, not a storage conflict, so
 * it is rejected at the boundary as a 400 naming the offending field rather
 * than surfacing later as an opaque {@code uq_recipe_ingredient_name}
 * violation. The database constraint remains the backstop for writes that never
 * pass through this DTO.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueIngredientNamesValidator.class)
public @interface UniqueIngredientNames {

    String message() default "ingredient names must be unique within a recipe";

    // groups() and payload() are required by the Bean Validation spec even when
    // unused — a constraint annotation missing them fails at bootstrap.
    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
