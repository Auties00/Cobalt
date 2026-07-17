package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * One detailed-targeting interest, behaviour, or demographic entry for a Click-to-WhatsApp ad.
 *
 * <p>Detailed targeting narrows an ad's audience by interests, behaviours, and demographics. Each such
 * entry names one targetable item by its server-issued {@link #id() identifier}, its
 * {@link #name() display name}, and its {@link #type() kind}.
 */
@ProtobufMessage(name = "DetailedTargetingItem")
public final class DetailedTargetingItem {
    /**
     * Server-issued identifier of the targetable item. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * Display name of the targetable item. Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String name;

    /**
     * Kind of the targetable item (for example interest, behaviour, demographic). Empty when unset.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String type;

    /**
     * Constructs a new {@code DetailedTargetingItem}. Every argument may be {@code null} to leave the
     * corresponding field unset.
     *
     * @param id   the item identifier, or {@code null}
     * @param name the display name, or {@code null}
     * @param type the item kind, or {@code null}
     */
    DetailedTargetingItem(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    /**
     * Returns the server-issued identifier of the targetable item.
     *
     * @return an {@link Optional} carrying the identifier, or empty when unset
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the display name of the targetable item.
     *
     * @return an {@link Optional} carrying the name, or empty when unset
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the kind of the targetable item.
     *
     * @return an {@link Optional} carrying the kind, or empty when unset
     */
    public Optional<String> type() {
        return Optional.ofNullable(type);
    }
}
