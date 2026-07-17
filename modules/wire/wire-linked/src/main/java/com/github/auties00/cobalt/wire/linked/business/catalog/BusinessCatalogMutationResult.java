package com.github.auties00.cobalt.wire.linked.business.catalog;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Outcome of a status-only mutation against a WhatsApp Business catalog.
 *
 * <p>Several catalog edits report only whether the change took effect,
 * without echoing the full entity that changed: deleting products or
 * collections, creating a catalog, reordering collections, appealing a
 * moderation verdict, and toggling per-product visibility all fall into
 * this group. This model collapses those outcomes into one shape so a
 * caller checks {@link #success()} regardless of which mutation it ran.
 *
 * <p>When the server reports which entities the mutation touched (for
 * example the ids it actually removed), they are surfaced through
 * {@link #affectedIds()}; otherwise that list is empty. A human-readable
 * failure message, when the server provides one, is carried by
 * {@link #errorMessage()}.
 */
@ProtobufMessage(name = "BusinessCatalogMutationResult")
public final class BusinessCatalogMutationResult {
    /**
     * Whether the mutation took effect. The server reports this as the
     * sole outcome of a status-only catalog edit; {@code false} both when
     * the server explicitly reported failure and when it omitted the
     * success marker entirely.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean success;

    /**
     * Identifiers of the catalog entities the mutation affected, when the
     * server reports them (for example the products it actually removed).
     * Never {@code null}, possibly empty when the server reports no ids.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final List<String> affectedIds;

    /**
     * Human-readable reason the mutation failed. Empty when the mutation
     * succeeded or the server did not attach a message.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String errorMessage;

    /**
     * Constructs a new {@code BusinessCatalogMutationResult}. A
     * {@code null} {@code affectedIds} is coerced to an empty list, and
     * {@code errorMessage} may be {@code null} when the server attached
     * no failure message.
     *
     * @param success      whether the mutation took effect
     * @param affectedIds  the affected entity ids; {@code null} treated as empty
     * @param errorMessage the failure message, or {@code null}
     */
    BusinessCatalogMutationResult(boolean success, List<String> affectedIds, String errorMessage) {
        this.success = success;
        this.affectedIds = affectedIds == null ? List.of() : affectedIds;
        this.errorMessage = errorMessage;
    }

    /**
     * Returns whether the mutation took effect.
     *
     * @return {@code true} when the server reported the change applied
     */
    public boolean success() {
        return success;
    }

    /**
     * Returns the identifiers of the catalog entities the mutation
     * affected.
     *
     * @return an unmodifiable view of the affected ids; never
     *         {@code null}, possibly empty
     */
    public List<String> affectedIds() {
        return Collections.unmodifiableList(affectedIds);
    }

    /**
     * Returns the human-readable reason the mutation failed.
     *
     * @return an {@code Optional} carrying the failure message, or empty
     *         when the mutation succeeded or carried no message
     */
    public Optional<String> errorMessage() {
        return Optional.ofNullable(errorMessage);
    }
}
