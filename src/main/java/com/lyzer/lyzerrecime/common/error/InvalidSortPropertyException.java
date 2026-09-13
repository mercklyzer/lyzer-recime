package com.lyzer.lyzerrecime.common.error;

import java.util.Collection;

public class InvalidSortPropertyException extends RuntimeException {

    public InvalidSortPropertyException(String property, Collection<String> allowed) {
        super("Cannot sort by '%s'. Allowed: %s".formatted(property, allowed));
    }
}
