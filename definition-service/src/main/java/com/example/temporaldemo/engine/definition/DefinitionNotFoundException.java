package com.example.temporaldemo.engine.definition;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a workflow definition cannot be found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class DefinitionNotFoundException extends RuntimeException {

    public DefinitionNotFoundException(String message) {
        super(message);
    }
}
