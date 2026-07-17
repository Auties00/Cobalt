package com.github.auties00.cobalt.wire.cloud.flow;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A WhatsApp Cloud API Flow.
 *
 * <p>Flows are interactive forms a business publishes under its WhatsApp Business Account and then
 * sends as interactive flow messages. This model carries the management view: the server-assigned id,
 * the name, the {@link CloudFlowStatus lifecycle status}, and the declared categories.
 */
public final class CloudFlow {
    /**
     * The server-assigned flow id, or {@code null} before creation.
     */
    private final String id;

    /**
     * The flow name.
     */
    private final String name;

    /**
     * The lifecycle status, or {@code null} before creation.
     */
    private final CloudFlowStatus status;

    /**
     * The declared flow categories.
     */
    private final List<String> categories;

    /**
     * Constructs a new flow.
     *
     * @param id         the server-assigned id, or {@code null} before creation
     * @param name       the flow name
     * @param status     the lifecycle status, or {@code null} before creation
     * @param categories the declared categories, or {@code null} for none
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public CloudFlow(String id, String name, CloudFlowStatus status, List<String> categories) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.status = status;
        this.categories = categories == null ? List.of() : List.copyOf(categories);
    }

    /**
     * Returns the server-assigned flow id.
     *
     * @return an {@link Optional} carrying the id, or empty before creation
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the flow name.
     *
     * @return the name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the lifecycle status.
     *
     * @return an {@link Optional} carrying the {@link CloudFlowStatus}, or empty before creation
     */
    public Optional<CloudFlowStatus> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns the declared flow categories.
     *
     * @return an unmodifiable list of categories, empty when none were declared
     */
    public List<String> categories() {
        return categories;
    }
}
