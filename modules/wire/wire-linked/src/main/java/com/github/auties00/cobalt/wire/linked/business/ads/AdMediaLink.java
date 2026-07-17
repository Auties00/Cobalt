package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Reference to an already-uploaded WhatsApp medium being linked to a
 * native ad on the status surface.
 *
 * <p>Before a WhatsApp native ad can run on the status surface, each of
 * its already-uploaded creatives must be linked to the ad. The linking
 * payload takes a list of these references: each pairs the medium's
 * advertising-platform identifier with the {@link AdMediaType media kind}
 * the server should treat it as.
 */
@ProtobufMessage(name = "AdMediaLink")
public final class AdMediaLink {
    /**
     * The advertising-platform identifier of the uploaded medium being
     * linked. Always populated for a constructed link, may be {@code null}
     * for a deserialized one when the server omitted the field.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String id;

    /**
     * The {@link AdMediaType media kind} the server should treat the
     * linked medium as. May be {@code null} for a deserialized link when
     * the server omitted the field.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    AdMediaType type;

    /**
     * Constructs a new {@code AdMediaLink} from the advertising-platform
     * identifier and the media kind.
     *
     * @param id   the advertising-platform identifier of the uploaded
     *             medium, or {@code null} to omit the field
     * @param type the media kind the server should treat the medium as,
     *             or {@code null} to omit the field
     */
    AdMediaLink(String id, AdMediaType type) {
        this.id = id;
        this.type = type;
    }

    /**
     * Returns the advertising-platform identifier of the uploaded medium.
     *
     * @return the identifier, or empty when none was supplied
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the media kind the server should treat the linked medium as.
     *
     * @return the media kind, or empty when none was supplied
     */
    public Optional<AdMediaType> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Sets the advertising-platform identifier of the uploaded medium.
     *
     * @param id the identifier to set, or {@code null} to clear
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the media kind the server should treat the linked medium as.
     *
     * @param type the media kind to set, or {@code null} to clear
     */
    public void setType(AdMediaType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AdMediaLink) obj;
        return Objects.equals(this.id, that.id)
                && Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }

    @Override
    public String toString() {
        return "AdMediaLink[" +
                "id=" + id + ", " +
                "type=" + type + ']';
    }
}
