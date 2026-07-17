package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Input model for uploading Click-to-WhatsApp ad media into Meta's
 * advertising-platform media store.
 *
 * <p>A Click-to-WhatsApp ad is a paid promotion that opens a chat with the
 * business when tapped. The ad-creation flow first uploads the chosen
 * media descriptors into the Meta advertising-platform media store and
 * then attaches the resulting descriptors to the ad creative. This input
 * carries every parameter the upload endpoint consumes.
 *
 * <p>{@link #adAccountId()} names the advertising-platform ad account
 * funding the campaign. {@link #pageId()} names the linked advertising-platform
 * page the media is registered against. {@link #mediaIds()} lists the
 * advertising-platform media identifiers to upload.
 * {@link #facebookAccessToken()} carries the Facebook access token the
 * advertising-platform endpoint requires.
 */
@ProtobufMessage(name = "AdMediaUploadOptions")
public final class AdMediaUploadOptions {
    /**
     * Advertising-platform ad-account identifier funding the campaign.
     * Unset omits the variable.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String adAccountId;

    /**
     * Linked advertising-platform page identifier the media is registered
     * against. Unset omits the variable.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String pageId;

    /**
     * Advertising-platform media identifiers to upload. Defaults to
     * {@link List#of()} when unset.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final List<String> mediaIds;

    /**
     * Facebook access token the advertising-platform endpoint requires.
     * Unset omits the variable.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String facebookAccessToken;

    /**
     * Constructs a new {@code AdMediaUploadOptions}. The list argument may
     * be {@code null} to default to {@link List#of()}; the scalar
     * arguments may be {@code null} to omit the corresponding variable
     * from the request.
     *
     * @param adAccountId         the advertising-platform ad-account
     *                            identifier, or {@code null}
     * @param pageId              the advertising-platform page identifier,
     *                            or {@code null}
     * @param mediaIds            the advertising-platform media
     *                            identifiers, or {@code null} to default
     *                            to empty
     * @param facebookAccessToken the Facebook access token, or
     *                            {@code null}
     */
    public AdMediaUploadOptions(String adAccountId, String pageId, List<String> mediaIds,
                                String facebookAccessToken) {
        this.adAccountId = adAccountId;
        this.pageId = pageId;
        this.mediaIds = mediaIds == null ? List.of() : List.copyOf(mediaIds);
        this.facebookAccessToken = facebookAccessToken;
    }

    /**
     * Returns the advertising-platform ad-account identifier.
     *
     * @return an {@link Optional} carrying the identifier, or empty when
     *         unset
     */
    public Optional<String> adAccountId() {
        return Optional.ofNullable(adAccountId);
    }

    /**
     * Returns the advertising-platform page identifier.
     *
     * @return an {@link Optional} carrying the identifier, or empty when
     *         unset
     */
    public Optional<String> pageId() {
        return Optional.ofNullable(pageId);
    }

    /**
     * Returns the advertising-platform media identifiers.
     *
     * @return an unmodifiable view of the identifiers; never {@code null},
     *         possibly empty
     */
    public List<String> mediaIds() {
        return mediaIds;
    }

    /**
     * Returns the Facebook access token.
     *
     * @return an {@link Optional} carrying the token, or empty when unset
     */
    public Optional<String> facebookAccessToken() {
        return Optional.ofNullable(facebookAccessToken);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AdMediaUploadOptions) obj;
        return Objects.equals(adAccountId, that.adAccountId)
                && Objects.equals(pageId, that.pageId)
                && Objects.equals(mediaIds, that.mediaIds)
                && Objects.equals(facebookAccessToken, that.facebookAccessToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(adAccountId, pageId, mediaIds, facebookAccessToken);
    }

    @Override
    public String toString() {
        return "AdMediaUploadOptions[" +
                "adAccountId=" + adAccountId + ", " +
                "pageId=" + pageId + ", " +
                "mediaIds=" + mediaIds + ", " +
                "facebookAccessToken=" + facebookAccessToken + ']';
    }
}
