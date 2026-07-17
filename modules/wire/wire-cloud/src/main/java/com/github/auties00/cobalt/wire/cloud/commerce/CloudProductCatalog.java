package com.github.auties00.cobalt.wire.cloud.commerce;

import java.util.Objects;
import java.util.Optional;

/**
 * A WhatsApp Cloud API product catalog reference.
 *
 * <p>Identifies the Commerce Manager catalog connected to a WhatsApp Business Account. A WABA permits
 * at most one connected catalog, addressed by its server-assigned id and carrying an optional display
 * name.
 */
public final class CloudProductCatalog {
    /**
     * The server-assigned catalog id.
     */
    private final String id;

    /**
     * The catalog display name, or {@code null} when absent.
     */
    private final String name;

    /**
     * Constructs a new product catalog reference.
     *
     * @param id   the server-assigned catalog id
     * @param name the catalog display name, or {@code null} when absent
     * @throws NullPointerException if {@code id} is {@code null}
     */
    public CloudProductCatalog(String id, String name) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = name;
    }

    /**
     * Returns the server-assigned catalog id.
     *
     * @return the catalog id
     */
    public String id() {
        return id;
    }

    /**
     * Returns the catalog display name.
     *
     * @return an {@link Optional} carrying the name, or empty when absent
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }
}
