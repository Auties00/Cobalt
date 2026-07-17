package com.github.auties00.cobalt.wire.cloud.flow;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The rich read view of a WhatsApp Cloud API Flow.
 *
 * <p>This is the management projection returned when a single Flow is fetched with the full field
 * set, as opposed to the lightweight {@link CloudFlow} carried in list responses. It exposes the
 * id, name, lifecycle status, declared categories, any pending validation errors, the JSON and
 * data-API version strings, the configured data-exchange endpoint, the web preview link, the linked
 * Meta application, the endpoint data-exchange availability, and the owning WhatsApp Business Account
 * id.
 */
public final class CloudFlowDetails {
    /**
     * The server-assigned flow id.
     */
    private final String id;

    /**
     * The flow name, or {@code null} when none was returned.
     */
    private final String name;

    /**
     * The lifecycle status, or {@code null} when none was returned.
     */
    private final CloudFlowStatus status;

    /**
     * The declared flow categories.
     */
    private final List<String> categories;

    /**
     * The pending validation errors.
     */
    private final List<CloudFlowValidationError> validationErrors;

    /**
     * The Flow JSON version, or {@code null} when none was returned.
     */
    private final String jsonVersion;

    /**
     * The data-API version, or {@code null} when none was returned.
     */
    private final String dataApiVersion;

    /**
     * The data-exchange endpoint URI, or {@code null} when none was returned.
     */
    private final String endpointUri;

    /**
     * The web preview link, or {@code null} when none was returned.
     */
    private final CloudFlowPreview preview;

    /**
     * The linked Meta application, or {@code null} when none was returned.
     */
    private final CloudFlowApplication application;

    /**
     * The endpoint data-exchange availability, or {@code null} when none was returned.
     */
    private final CloudFlowEndpointAvailability healthStatus;

    /**
     * The owning WhatsApp Business Account id, or {@code null} when none was returned.
     */
    private final String whatsappBusinessAccountId;

    /**
     * The instant the flow metadata or JSON was last changed, or {@code null} when none was returned.
     */
    private final Instant updatedAt;

    /**
     * Constructs a new flow detail view.
     *
     * @param id                        the server-assigned flow id
     * @param name                      the flow name, or {@code null} when none was returned
     * @param status                    the lifecycle status, or {@code null} when none was returned
     * @param categories                the declared categories, or {@code null} for none
     * @param validationErrors          the pending validation errors, or {@code null} for none
     * @param jsonVersion               the Flow JSON version, or {@code null} when none was returned
     * @param dataApiVersion            the data-API version, or {@code null} when none was returned
     * @param endpointUri               the data-exchange endpoint URI, or {@code null} when absent
     * @param preview                   the web preview link, or {@code null} when none was returned
     * @param application               the linked Meta application, or {@code null} when absent
     * @param healthStatus              the endpoint data-exchange availability, or {@code null} when
     *                                  absent
     * @param whatsappBusinessAccountId the owning WABA id, or {@code null} when none was returned
     * @param updatedAt                 the last-changed instant, or {@code null} when none was returned
     * @throws NullPointerException if {@code id} is {@code null}
     */
    public CloudFlowDetails(String id, String name, CloudFlowStatus status, List<String> categories,
                            List<CloudFlowValidationError> validationErrors, String jsonVersion,
                            String dataApiVersion, String endpointUri, CloudFlowPreview preview,
                            CloudFlowApplication application, CloudFlowEndpointAvailability healthStatus,
                            String whatsappBusinessAccountId, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = name;
        this.status = status;
        this.categories = categories == null ? List.of() : List.copyOf(categories);
        this.validationErrors = validationErrors == null ? List.of() : List.copyOf(validationErrors);
        this.jsonVersion = jsonVersion;
        this.dataApiVersion = dataApiVersion;
        this.endpointUri = endpointUri;
        this.preview = preview;
        this.application = application;
        this.healthStatus = healthStatus;
        this.whatsappBusinessAccountId = whatsappBusinessAccountId;
        this.updatedAt = updatedAt;
    }

    /**
     * Returns the server-assigned flow id.
     *
     * @return the id
     */
    public String id() {
        return id;
    }

    /**
     * Returns the flow name.
     *
     * @return an {@link Optional} carrying the name, or empty when none was returned
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the lifecycle status.
     *
     * @return an {@link Optional} carrying the {@link CloudFlowStatus}, or empty when none was returned
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

    /**
     * Returns the pending validation errors.
     *
     * @return an unmodifiable list of validation errors, empty when the flow is valid
     */
    public List<CloudFlowValidationError> validationErrors() {
        return validationErrors;
    }

    /**
     * Returns the Flow JSON version.
     *
     * @return an {@link Optional} carrying the JSON version, or empty when none was returned
     */
    public Optional<String> jsonVersion() {
        return Optional.ofNullable(jsonVersion);
    }

    /**
     * Returns the data-API version.
     *
     * @return an {@link Optional} carrying the data-API version, or empty when none was returned
     */
    public Optional<String> dataApiVersion() {
        return Optional.ofNullable(dataApiVersion);
    }

    /**
     * Returns the data-exchange endpoint URI.
     *
     * @return an {@link Optional} carrying the endpoint URI, or empty when none was configured
     */
    public Optional<String> endpointUri() {
        return Optional.ofNullable(endpointUri);
    }

    /**
     * Returns the web preview link.
     *
     * @return an {@link Optional} carrying the preview, or empty when none was returned
     */
    public Optional<CloudFlowPreview> preview() {
        return Optional.ofNullable(preview);
    }

    /**
     * Returns the linked Meta application.
     *
     * @return an {@link Optional} carrying the application, or empty when none was linked
     */
    public Optional<CloudFlowApplication> application() {
        return Optional.ofNullable(application);
    }

    /**
     * Returns the endpoint data-exchange availability.
     *
     * @return an {@link Optional} carrying the {@link CloudFlowEndpointAvailability}, or empty when none
     *         was returned
     */
    public Optional<CloudFlowEndpointAvailability> healthStatus() {
        return Optional.ofNullable(healthStatus);
    }

    /**
     * Returns the owning WhatsApp Business Account id.
     *
     * @return an {@link Optional} carrying the WABA id, or empty when none was returned
     */
    public Optional<String> whatsappBusinessAccountId() {
        return Optional.ofNullable(whatsappBusinessAccountId);
    }

    /**
     * Returns the instant the flow metadata or JSON was last changed.
     *
     * @return an {@link Optional} carrying the last-changed instant, or empty when none was returned
     */
    public Optional<Instant> updatedAt() {
        return Optional.ofNullable(updatedAt);
    }
}
