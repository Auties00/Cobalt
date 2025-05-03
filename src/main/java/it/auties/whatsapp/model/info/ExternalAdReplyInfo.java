package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that holds the information related to an advertisement.
 */
@ProtobufMessage(name = "ContextInfo.ExternalAdReplyInfo")
public final class ExternalAdReplyInfo implements Info {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String title;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String body;

    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    final MediaType mediaType;

    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String thumbnailUrl;

    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String mediaUrl;

    @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
    final byte[] thumbnail;

    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String sourceType;

    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    final String sourceId;

    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    final String sourceUrl;

    @ProtobufProperty(index = 10, type = ProtobufType.BOOL)
    final boolean containsAutoReply;

    @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
    final boolean renderLargerThumbnail;

    @ProtobufProperty(index = 12, type = ProtobufType.BOOL)
    final boolean showAdAttribution;

    @ProtobufProperty(index = 13, type = ProtobufType.STRING)
    final String ctwaClid;

    ExternalAdReplyInfo(String title, String body, MediaType mediaType, String thumbnailUrl, String mediaUrl, byte[] thumbnail, String sourceType, String sourceId, String sourceUrl, boolean containsAutoReply, boolean renderLargerThumbnail, boolean showAdAttribution, String ctwaClid) {
        this.title = title;
        this.body = body;
        this.mediaType = mediaType;
        this.thumbnailUrl = thumbnailUrl;
        this.mediaUrl = mediaUrl;
        this.thumbnail = thumbnail;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.sourceUrl = sourceUrl;
        this.containsAutoReply = containsAutoReply;
        this.renderLargerThumbnail = renderLargerThumbnail;
        this.showAdAttribution = showAdAttribution;
        this.ctwaClid = ctwaClid;
    }

    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    public Optional<String> body() {
        return Optional.ofNullable(body);
    }

    public Optional<MediaType> mediaType() {
        return Optional.ofNullable(mediaType);
    }

    public Optional<String> thumbnailUrl() {
        return Optional.ofNullable(thumbnailUrl);
    }

    public Optional<String> mediaUrl() {
        return Optional.ofNullable(mediaUrl);
    }

    public Optional<byte[]> thumbnail() {
        return Optional.ofNullable(thumbnail);
    }

    public Optional<String> sourceType() {
        return Optional.ofNullable(sourceType);
    }

    public Optional<String> sourceId() {
        return Optional.ofNullable(sourceId);
    }

    public Optional<String> sourceUrl() {
        return Optional.ofNullable(sourceUrl);
    }

    public boolean containsAutoReply() {
        return containsAutoReply;
    }

    public boolean renderLargerThumbnail() {
        return renderLargerThumbnail;
    }

    public boolean showAdAttribution() {
        return showAdAttribution;
    }

    public Optional<String> ctwaClid() {
        return Optional.ofNullable(ctwaClid);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ExternalAdReplyInfo that
                && Objects.equals(title, that.title)
                && Objects.equals(body, that.body)
                && Objects.equals(mediaType, that.mediaType)
                && Objects.equals(thumbnailUrl, that.thumbnailUrl)
                && Objects.equals(mediaUrl, that.mediaUrl)
                && Arrays.equals(thumbnail, that.thumbnail)
                && Objects.equals(sourceType, that.sourceType)
                && Objects.equals(sourceId, that.sourceId)
                && Objects.equals(sourceUrl, that.sourceUrl)
                && containsAutoReply == that.containsAutoReply
                && renderLargerThumbnail == that.renderLargerThumbnail
                && showAdAttribution == that.showAdAttribution
                && Objects.equals(ctwaClid, that.ctwaClid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, body, mediaType, thumbnailUrl, mediaUrl, Arrays.hashCode(thumbnail), sourceType, sourceId, sourceUrl, containsAutoReply, renderLargerThumbnail, showAdAttribution, ctwaClid);
    }

    @Override
    public String toString() {
        return "ExternalAdReplyInfo[" +
                "title=" + title +
                ", body=" + body +
                ", mediaType=" + mediaType +
                ", thumbnailUrl=" + thumbnailUrl +
                ", mediaUrl=" + mediaUrl +
                ", thumbnail=" + Arrays.toString(thumbnail) +
                ", sourceType=" + sourceType +
                ", sourceId=" + sourceId +
                ", sourceUrl=" + sourceUrl +
                ", containsAutoReply=" + containsAutoReply +
                ", renderLargerThumbnail=" + renderLargerThumbnail +
                ", showAdAttribution=" + showAdAttribution +
                ", ctwaClid=" + ctwaClid +
                ']';
    }

    /**
     * The constants of this enumerated type describe the various types of media that an ad can wrap
     */
    @ProtobufEnum(name = "ChatRowOpaqueData.DraftMessage.CtwaContextData.ContextInfoExternalAdReplyInfoMediaType")
    public enum MediaType {
        /**
         * No media
         */
        NONE(0),

        /**
         * Image
         */
        IMAGE(1),

        /**
         * Video
         */
        VIDEO(2);

        final int index;

        MediaType(@ProtobufEnumIndex int index) {
            this.index = index;
        }
    }
}