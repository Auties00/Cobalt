package com.github.auties00.cobalt.wire.linked.business.profile;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A single match returned by the WhatsApp business-category typeahead query, used by
 * the profile-edit flow when the user is choosing a vertical for their business
 * (such as "Restaurant", "Retail", or "Medical Services").
 *
 * <p>Each hit pairs a stable category identifier with its already-decoded localized
 * display name. The {@linkplain #isNotABiz() not-a-business} flag is {@code true}
 * for the synthetic placeholder hit that the server returns alongside the actual
 * matches; clients render that placeholder as a "this is not a business" option in
 * the picker UI.
 *
 * <p>This is a different type from the catalog-side {@link BusinessCategory} model
 * carried on a {@link BusinessProfile}: typeahead hits surface only the id and the
 * already-decoded localized name (no URL decoding required), while the catalog-side
 * category model is a richer protobuf message used when displaying or persisting a
 * profile's chosen verticals.
 */
@ProtobufMessage
public final class BusinessCategoryHit {
    /**
     * The stable category identifier assigned by WhatsApp, such as a slug or numeric
     * id. The identifier is locale-independent and uniquely distinguishes this
     * category from every other entry in the catalog.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String id;

    /**
     * The localized human-readable display label for this category, already decoded
     * from any URL-escape sequences used on the wire and rendered in the locale of
     * the user performing the typeahead query.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String localizedName;

    /**
     * Flag indicating whether this hit is the synthetic "not a business" placeholder
     * returned by the server. When {@code true}, the client renders it as a special
     * "this is not a business" option rather than a real category match. When
     * {@code false}, this is an ordinary category hit.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    boolean notABiz;

    /**
     * Constructs a new business-category typeahead hit with the specified stable
     * identifier, already-decoded localized label, and not-a-business placeholder
     * flag.
     *
     * @param id            the stable category identifier
     * @param localizedName the localized display label
     * @param notABiz       the not-a-business placeholder flag
     */
    BusinessCategoryHit(String id, String localizedName, boolean notABiz) {
        this.id = id;
        this.localizedName = localizedName;
        this.notABiz = notABiz;
    }

    /**
     * Returns the stable category identifier assigned by WhatsApp.
     *
     * @return the identifier
     */
    public String id() {
        return id;
    }

    /**
     * Sets the stable category identifier assigned by WhatsApp.
     *
     * @param id the category identifier
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the localized human-readable display label for this category.
     *
     * @return the label
     */
    public String localizedName() {
        return localizedName;
    }

    /**
     * Sets the localized human-readable display label for this category.
     *
     * @param localizedName the localized display label
     */
    public void setLocalizedName(String localizedName) {
        this.localizedName = localizedName;
    }

    /**
     * Returns whether this hit is the synthetic "not a business" placeholder
     * returned by the server alongside the real category matches.
     *
     * @return {@code true} when this hit is the placeholder, {@code false} for
     *         regular category hits
     */
    public boolean isNotABiz() {
        return notABiz;
    }

    /**
     * Sets whether this hit is the synthetic "not a business" placeholder.
     *
     * @param notABiz {@code true} to mark this hit as the placeholder, {@code false}
     *                for regular category hits
     */
    public void setNotABiz(boolean notABiz) {
        this.notABiz = notABiz;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessCategoryHit) obj;
        return this.notABiz == that.notABiz &&
               Objects.equals(this.id, that.id) &&
               Objects.equals(this.localizedName, that.localizedName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, localizedName, notABiz);
    }

    @Override
    public String toString() {
        return "BusinessCategoryHit[" +
               "id=" + id + ", " +
               "localizedName=" + localizedName + ", " +
               "notABiz=" + notABiz + ']';
    }
}
