package com.lyzer.lyzerrecime.recipe;

import com.lyzer.lyzerrecime.recipe.dto.RecipeSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

final class RecipeSpecifications {

    private RecipeSpecifications() {
    }

    static Specification<Recipe> vegetarian(Boolean value) {
        return value == null
                ? null
                : (root, query, cb) -> cb.equal(root.get("vegetarian"), value);
    }

    static Specification<Recipe> servingsEquals(Integer value) {
        return value == null
                ? null
                : (root, query, cb) -> cb.equal(root.get("servings"), value);
    }

    static Specification<Recipe> servingsAtLeast(Integer value) {
        return value == null
                ? null
                : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("servings"), value);
    }

    static Specification<Recipe> servingsAtMost(Integer value) {
        return value == null
                ? null
                : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("servings"), value);
    }

    static Specification<Recipe> instructionsContain(String fragment) {
        if (fragment == null || fragment.isBlank()) {
            return null;
        }
        String pattern = "%" + escapeLike(fragment.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("instructions")), pattern, ESCAPE_CHAR);
    }

    static Specification<Recipe> includesAllIngredients(Collection<String> names) {
        List<String> normalized = normalizeAll(names);
        if (normalized.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> {
            Predicate[] existsPerName = normalized.stream()
                    .map(name -> {
                        Subquery<Long> sub = query.subquery(Long.class);
                        Root<RecipeIngredient> ingredient = sub.from(RecipeIngredient.class);
                        sub.select(ingredient.get("id"))
                           .where(cb.and(
                                   cb.equal(ingredient.get("recipe"), root),
                                   cb.equal(ingredient.get("name"), name)));
                        return cb.exists(sub);
                    })
                    .toArray(Predicate[]::new);
            return cb.and(existsPerName);
        };
    }

    static Specification<Recipe> excludesAllIngredients(Collection<String> names) {
        List<String> normalized = normalizeAll(names);
        if (normalized.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<RecipeIngredient> ingredient = sub.from(RecipeIngredient.class);
            sub.select(ingredient.get("id"))
               .where(cb.and(
                       cb.equal(ingredient.get("recipe"), root),
                       ingredient.get("name").in(normalized)));
            return cb.not(cb.exists(sub));
        };
    }

    static Specification<Recipe> matching(RecipeSearchCriteria criteria) {
        return Specification.allOf(
                vegetarian(criteria.vegetarian()),
                servingsEquals(criteria.servings()),
                servingsAtLeast(criteria.minServings()),
                servingsAtMost(criteria.maxServings()),
                instructionsContain(criteria.instructions()),
                includesAllIngredients(criteria.includeIngredients()),
                excludesAllIngredients(criteria.excludeIngredients()));
    }

    private static final char ESCAPE_CHAR = '\\';

    /**
     * Without this, a search for "50%" becomes LIKE '%50%%' and the trailing %
     * is a wildcard — the user's literal character silently changes the query.
     */
    private static String escapeLike(String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * The query side must apply exactly the function write mapping applied, or
     * ?includeIngredients=Onion never matches the stored "onion".
     */
    private static List<String> normalizeAll(Collection<String> names) {
        return names == null ? List.of() : names.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(IngredientNames::normalize)
                .distinct()
                .toList();
    }
}
