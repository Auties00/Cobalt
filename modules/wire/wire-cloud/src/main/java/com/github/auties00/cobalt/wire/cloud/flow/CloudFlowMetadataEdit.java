package com.github.auties00.cobalt.wire.cloud.flow;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A partial metadata update for a WhatsApp Cloud API Flow.
 *
 * <p>A Flow's editable metadata covers its display name, its categories, the endpoint URI its data
 * exchange calls, and the Meta application bound to it. This model carries the id of the Flow to edit and
 * whichever of those fields the caller wants to change; an absent optional field is left untouched, while
 * an empty {@link #categories() categories} list means no change is requested rather than clearing the
 * categories. This model is the input to {@code CloudWhatsAppClient.editFlowMetadata}; the Flow id is
 * required.
 */
@ProtobufMessage
public final class CloudFlowMetadataEdit {
    /**
     * The id of the Flow to edit.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String flowId;

    /**
     * The new display name, or {@code null} to leave it unchanged.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String name;

    /**
     * The new categories, empty when no change is requested.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final List<String> categories;

    /**
     * The new endpoint URI, or {@code null} to leave it unchanged.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String endpointUri;

    /**
     * The new bound application id, or {@code null} to leave it unchanged.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String applicationId;

    /**
     * Constructs a new Flow-metadata edit.
     *
     * @param flowId        the id of the Flow to edit
     * @param name          the new display name, or {@code null} to leave it unchanged
     * @param categories    the new categories, or {@code null} when no change is requested
     * @param endpointUri   the new endpoint URI, or {@code null} to leave it unchanged
     * @param applicationId the new bound application id, or {@code null} to leave it unchanged
     * @throws NullPointerException if {@code flowId} is {@code null}
     */
    CloudFlowMetadataEdit(String flowId, String name, List<String> categories, String endpointUri,
                          String applicationId) {
        this.flowId = Objects.requireNonNull(flowId, "flowId must not be null");
        this.name = name;
        this.categories = categories == null ? List.of() : List.copyOf(categories);
        this.endpointUri = endpointUri;
        this.applicationId = applicationId;
    }

    /**
     * Returns the id of the Flow to edit.
     *
     * @return the Flow id
     */
    public String flowId() {
        return flowId;
    }

    /**
     * Returns the new display name.
     *
     * @return an {@link Optional} carrying the name, or empty to leave it unchanged
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the new categories.
     *
     * @return an unmodifiable list of categories, empty when no change is requested
     */
    public List<String> categories() {
        return categories;
    }

    /**
     * Returns the new endpoint URI.
     *
     * @return an {@link Optional} carrying the endpoint URI, or empty to leave it unchanged
     */
    public Optional<String> endpointUri() {
        return Optional.ofNullable(endpointUri);
    }

    /**
     * Returns the new bound application id.
     *
     * @return an {@link Optional} carrying the application id, or empty to leave it unchanged
     */
    public Optional<String> applicationId() {
        return Optional.ofNullable(applicationId);
    }
}
