package com.lyzer.lyzerrecime.recipe;

import com.lyzer.lyzerrecime.recipe.dto.CreateRecipeRequest;
import com.lyzer.lyzerrecime.recipe.dto.IngredientRequest;
import com.lyzer.lyzerrecime.recipe.dto.UpdateRecipeRequest;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /** PUT is a full replacement, so update mirrors {@code from} exactly. */
    public void applyUpdate(UpdateRecipeRequest request) {
        this.title = request.title().trim();
        this.description = request.description();
        this.servings = request.servings();
        this.vegetarian = request.vegetarian();
        this.instructions = request.instructions();
        replaceIngredients(request.ingredients());
    }

    /**
     * Mutates the managed collection in place. Assigning a brand-new List to
     * the field instead — {@code this.ingredients = newList} — detaches the
     * Hibernate-managed collection and throws "A collection with
     * cascade=all-delete-orphan was no longer referenced".
     *
     * <p>Rows are matched to the request by normalized name and reused, rather
     * than dropped and reinserted wholesale. With IDENTITY ids Hibernate cannot
     * defer a child INSERT — it needs the generated key — so the insert of a
     * name that survives the update reaches PostgreSQL before the orphan DELETE
     * of its old row, and {@code uq_recipe_ingredient_name} rejects it.
     * Matching by name also means the deleted and inserted names are disjoint,
     * so the flush order stops mattering at all.
     */
    private void replaceIngredients(List<IngredientRequest> requestedIngredients) {
        Map<String, RecipeIngredient> existingByName = new HashMap<>();
        for (RecipeIngredient ingredient : this.ingredients) {
            existingByName.put(ingredient.getName(), ingredient);
        }

        List<RecipeIngredient> reconciled = new ArrayList<>(requestedIngredients.size());
        for (int i = 0; i < requestedIngredients.size(); i++) {
            IngredientRequest requested = requestedIngredients.get(i);
            RecipeIngredient existing =
                    existingByName.get(IngredientNames.normalize(requested.name()));
            reconciled.add(existing == null
                    ? RecipeIngredient.of(this, requested, i)
                    : existing.applyUpdate(requested, i));
        }

        // Survivors are re-added, so they are not orphans; whatever is left out
        // is. The list keeps the requested order, which is what the response
        // built from this entity serializes.
        this.ingredients.clear();
        this.ingredients.addAll(reconciled);
    }

    /** Defensive: nothing outside the entity may structurally edit the list. */
    public List<RecipeIngredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }
}
