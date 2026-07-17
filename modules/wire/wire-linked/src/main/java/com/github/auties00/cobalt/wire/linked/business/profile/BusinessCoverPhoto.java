package com.github.auties00.cobalt.wire.linked.business.profile;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Represents the single cover photo (banner) attached to a WhatsApp Business profile.
 *
 * <p>A business profile carries at most one cover photo. The {@link #id()} is the upload
 * identifier of the currently attached photo; it keys the cover-photo lifecycle (replace or
 * delete) so the relay can match the mutation to the photo that is currently attached. The
 * {@link #url()} is the signed download URL rendered as the profile banner.
 */
@ProtobufMessage
public final class BusinessCoverPhoto {
    /**
     * The upload identifier of the currently attached cover photo, used to key replace and delete
     * mutations.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String id;

    /**
     * The signed download URL of the cover photo, or {@code null} when only the id is known.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String url;

    /**
     * Constructs a new cover photo descriptor.
     *
     * @param id  the upload identifier
     * @param url the signed download URL, or {@code null} when unknown
     */
    BusinessCoverPhoto(String id, String url) {
        this.id = id;
        this.url = url;
    }

    /**
     * Returns the upload identifier of the currently attached cover photo.
     *
     * <p>The value keys the cover-photo lifecycle: it is echoed back on a replace or delete so the
     * relay detaches the matching photo.
     *
     * @return the upload identifier
     */
    public String id() {
        return id;
    }

    /**
     * Sets the upload identifier of the currently attached cover photo.
     *
     * @param id the upload identifier
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the signed download URL of the cover photo, if known.
     *
     * @return an {@link Optional} containing the URL, or an empty {@code Optional} when only the id
     *         is known
     */
    public Optional<String> url() {
        return Optional.ofNullable(url);
    }

    /**
     * Sets the signed download URL of the cover photo.
     *
     * @param url the download URL, or {@code null} to clear
     */
    public void setUrl(String url) {
        this.url = url;
    }
}
