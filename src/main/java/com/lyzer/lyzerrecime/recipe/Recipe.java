package com.lyzer.lyzerrecime.recipe;

import com.lyzer.lyzerrecime.recipe.dto.CreateRecipeRequest;
import com.lyzer.lyzerrecime.recipe.dto.IngredientRequest;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "recipes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private int servings;

    @Column(nullable = false)
    private boolean vegetarian;

    @Column(nullable = false, columnDefinition = "text")
    private String instructions;

    @OneToMany(
            mappedBy = "recipe",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * The single place a request becomes an entity — the invariants
     * (normalized names, contiguous ordering) live next to the fields they
     * constrain, and there is exactly one construction path.
     */
    public static Recipe from(CreateRecipeRequest request) {
        Recipe recipe = new Recipe();
        recipe.title = request.title().trim();
        recipe.description = request.description();
        recipe.servings = request.servings();
        recipe.vegetarian = request.vegetarian();
        recipe.instructions = request.instructions();
        recipe.replaceIngredients(request.ingredients());
        return recipe;
    }

    /**
     * Mutates the managed collection in place. Assigning a brand-new List to
     * the field instead — {@code this.ingredients = newList} — detaches the
     * Hibernate-managed collection and throws "A collection with
     * cascade=all-delete-orphan was no longer referenced". clear() marks the
     * old rows as orphans (DELETE); add() inserts the new.
     */
    private void replaceIngredients(List<IngredientRequest> requestedIngredients) {
        this.ingredients.clear();
        for (int i = 0; i < requestedIngredients.size(); i++) {
            this.ingredients.add(RecipeIngredient.of(this, requestedIngredients.get(i), i));
        }
    }

    /** Defensive: nothing outside the entity may structurally edit the list. */
    public List<RecipeIngredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }
}
