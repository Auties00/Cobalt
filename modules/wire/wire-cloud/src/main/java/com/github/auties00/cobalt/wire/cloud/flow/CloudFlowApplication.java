package com.github.auties00.cobalt.wire.cloud.flow;

import java.util.Optional;

/**
 * The Meta application linked to a WhatsApp Cloud API Flow's endpoint.
 *
 * <p>Flows with a data-exchange endpoint declare the Meta app that signs the endpoint requests. This
 * model carries the management view of that app: its id, name, and link, all as returned by the
 * Flow's {@code application} field.
 */
public final class CloudFlowApplication {
    /**
     * The application id, or {@code null} when none was returned.
     */
    private final String id;

    /**
     * The application name, or {@code null} when none was returned.
     */
    private final String name;

    /**
     * The application link, or {@code null} when none was returned.
     */
    private final String link;

    /**
     * Constructs a new flow application.
     *
     * @param id   the application id, or {@code null} when none was returned
     * @param name the application name, or {@code null} when none was returned
     * @param link the application link, or {@code null} when none was returned
     */
    public CloudFlowApplication(String id, String name, String link) {
        this.id = id;
        this.name = name;
        this.link = link;
    }

    /**
     * Returns the application id.
     *
     * @return an {@link Optional} carrying the id, or empty when none was returned
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the application name.
     *
     * @return an {@link Optional} carrying the name, or empty when none was returned
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the application link.
     *
     * @return an {@link Optional} carrying the link, or empty when none was returned
     */
    public Optional<String> link() {
        return Optional.ofNullable(link);
    }
}
