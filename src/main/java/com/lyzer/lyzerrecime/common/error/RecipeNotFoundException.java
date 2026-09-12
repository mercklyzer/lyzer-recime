package com.lyzer.lyzerrecime.common.error;

public class RecipeNotFoundException extends RuntimeException {

    // RuntimeException, not checked: a missing recipe is not recoverable
    // mid-stack, and a checked type would force try/catch through every layer
    // to reach the one handler that deals with it.
    public RecipeNotFoundException(Long id) {
        super("Recipe %d not found".formatted(id));
    }
}
