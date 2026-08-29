package org.allaymc.api.entity.data;

import java.util.Objects;

/**
 * Describes how an entity name tag is presented to a viewer.
 *
 * @param text the name tag text; an empty value hides the text without removing the viewer override
 * @param alwaysShow whether the client should keep the name tag visible without targeting the entity
 */
public record EntityNameTag(String text, boolean alwaysShow) {

    /**
     * Creates a name tag presentation intent.
     *
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public EntityNameTag {
        Objects.requireNonNull(text, "text");
    }
}
