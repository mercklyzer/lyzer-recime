package com.lyzer.lyzerrecime.recipe;

import java.util.Locale;

/**
 * The canonical form of an ingredient name. Names are normalized on write, so
 * this is the single definition that write mapping, the uniqueness constraint
 * and search filtering all have to agree on — if they ever disagree, validation
 * accepts a request the database then rejects.
 */
public final class IngredientNames {

    private IngredientNames() {
    }

    /**
     * Locale.ROOT, not the default locale: in a Turkish locale
     * {@code "I".toLowerCase()} is {@code "ı"}, which would make matching
     * machine-dependent.
     */
    public static String normalize(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
