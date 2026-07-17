package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The website-backed material a WhatsApp Business AI agent answers from,
 * together with the products it features.
 *
 * <p>Beyond hand-authored frequently-asked-question entries, the merchant's
 * auto-reply assistant can be pointed at external material: one or more
 * websites whose contents it ingests, and a set of featured (bestseller)
 * catalog products it can recommend. This model groups that material.
 *
 * <p>{@link #websites()} lists the typed website sources. {@link #website()}
 * carries an older single-website address the server may still report
 * alongside the list. {@link #bestsellerProductIds()} lists the catalog ids
 * of the products the assistant treats as featured.
 */
@ProtobufMessage(name = "BusinessAiWebsiteKnowledge")
public final class BusinessAiWebsiteKnowledge {
    /**
     * Older single-website address the server may report in addition to
     * {@link #websites}. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String website;

    /**
     * Typed website sources the assistant ingests, in the order the server
     * returned them. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final List<BusinessAiWebsite> websites;

    /**
     * Catalog identifiers of the products the assistant treats as featured.
     * These are product-catalog identifiers, not WhatsApp addresses. Never
     * {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final List<String> bestsellerProductIds;

    /**
     * Constructs a new {@code BusinessAiWebsiteKnowledge}. A {@code null}
     * list argument is coerced to an empty list, and {@code website} may be
     * {@code null} when the server omitted it.
     *
     * @param website              the older single-website address, or {@code null}
     * @param websites             the typed website sources; {@code null} treated as empty
     * @param bestsellerProductIds the featured product ids; {@code null} treated as empty
     */
    BusinessAiWebsiteKnowledge(String website, List<BusinessAiWebsite> websites, List<String> bestsellerProductIds) {
        this.website = website;
        this.websites = websites == null ? List.of() : websites;
        this.bestsellerProductIds = bestsellerProductIds == null ? List.of() : bestsellerProductIds;
    }

    /**
     * Returns the older single-website address the server may report
     * alongside the typed list.
     *
     * @return the single-website address, or empty when the server omitted
     *         it
     */
    public Optional<String> website() {
        return Optional.ofNullable(website);
    }

    /**
     * Returns the typed website sources the assistant ingests.
     *
     * @return an unmodifiable view of the website sources; never
     *         {@code null}, possibly empty
     */
    public List<BusinessAiWebsite> websites() {
        return Collections.unmodifiableList(websites);
    }

    /**
     * Returns the catalog identifiers of the featured products.
     *
     * @return an unmodifiable view of the featured product ids; never
     *         {@code null}, possibly empty
     */
    public List<String> bestsellerProductIds() {
        return Collections.unmodifiableList(bestsellerProductIds);
    }
}
