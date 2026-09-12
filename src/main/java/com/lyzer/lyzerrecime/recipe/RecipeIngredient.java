package com.lyzer.lyzerrecime.recipe;

import com.lyzer.lyzerrecime.recipe.dto.IngredientRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "recipe_ingredients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // EAGER is the @ManyToOne default and would load the parent on every
    // ingredient read. Overridden, per the house rule that all associations
    // are LAZY.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(nullable = false, length = 120)
    private String name;

    // BigDecimal, never double: 0.1 + 0.2 != 0.3 in binary floating point, and
    // quantities are decimal quantities a human typed.
    @Column(precision = 10, scale = 3)
    private BigDecimal quantity;

    @Column(length = 50)
    private String unit;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    static RecipeIngredient of(Recipe recipe, IngredientRequest requestedIngredient, int displayOrder) {
        RecipeIngredient ingredient = new RecipeIngredient();
        ingredient.recipe = recipe;
        ingredient.name = IngredientNames.normalize(requestedIngredient.name());
        ingredient.quantity = requestedIngredient.quantity();
        ingredient.unit = requestedIngredient.unit() == null ? null : requestedIngredient.unit().trim();
        ingredient.displayOrder = displayOrder;
        return ingredient;
    }
}
