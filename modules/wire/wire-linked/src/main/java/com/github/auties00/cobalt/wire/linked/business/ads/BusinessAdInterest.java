package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A detailed-targeting interest a merchant can add to a WhatsApp Business
 * advertisement's audience.
 *
 * <p>When a merchant narrows who a "Click-to-WhatsApp" ad (a paid promotion that
 * opens a chat with the business when tapped) is shown to, they pick from a
 * taxonomy of interests: hobbies, topics, demographics, and behaviours people
 * are grouped by. This model is one such interest, whether it was browsed under
 * a category, found by searching, or suggested from interests already chosen.
 *
 * <p>{@link #id()} is the handle added to a targeting specification;
 * {@link #name()} is the label shown to the merchant; {@link #path()} is the
 * position of the interest within the targeting taxonomy; {@link #rawName()} is
 * the unlocalised name; and {@link #targetType()} is the server-defined kind of
 * interest.
 */
@ProtobufMessage(name = "BusinessAdInterest")
public final class BusinessAdInterest {
    /**
     * Server-issued identifier of the interest. This is the handle added to a
     * targeting specification; it is a numeric advertising identifier, not a
     * WhatsApp address. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * Label of the interest shown to the merchant. Empty when the server omitted
     * it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String name;

    /**
     * Position of the interest within the targeting taxonomy, as a
     * server-defined path. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String path;

    /**
     * Unlocalised name of the interest. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String rawName;

    /**
     * Server-defined kind of interest (for example an interest, a behaviour, or
     * a demographic). The full marker set is not recoverable from the WhatsApp
     * client, so the raw marker is exposed as a string. Empty when the server
     * omitted it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String targetType;

    /**
     * Constructs a new {@code BusinessAdInterest}. The reference arguments may be
     * {@code null} when the server omitted them.
     *
     * @param id         the interest identifier, or {@code null}
     * @param name       the display label, or {@code null}
     * @param path       the taxonomy path, or {@code null}
     * @param rawName    the unlocalised name, or {@code null}
     * @param targetType the interest kind marker, or {@code null}
     */
    BusinessAdInterest(String id, String name, String path, String rawName, String targetType) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.rawName = rawName;
        this.targetType = targetType;
    }

    /**
     * Returns the server-issued identifier of the interest.
     *
     * @return the interest id, or empty when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the label of the interest shown to the merchant.
     *
     * @return the display label, or empty when the server omitted it
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the position of the interest within the targeting taxonomy.
     *
     * @return the taxonomy path, or empty when the server omitted it
     */
    public Optional<String> path() {
        return Optional.ofNullable(path);
    }

    /**
     * Returns the unlocalised name of the interest.
     *
     * @return the unlocalised name, or empty when the server omitted it
     */
    public Optional<String> rawName() {
        return Optional.ofNullable(rawName);
    }

    /**
     * Returns the server-defined kind of interest.
     *
     * @return the interest kind marker, or empty when the server omitted it
     */
    public Optional<String> targetType() {
        return Optional.ofNullable(targetType);
    }
}
