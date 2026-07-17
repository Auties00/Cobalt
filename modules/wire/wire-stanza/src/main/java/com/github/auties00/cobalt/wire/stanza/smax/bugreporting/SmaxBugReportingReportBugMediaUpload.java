package com.github.auties00.cobalt.wire.stanza.smax.bugreporting;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Models a single attachment carried by a {@link SmaxBugReportingReportBugRequest}.
 *
 * <p>Each instance is one entry of the repeated {@code media} child the bug-report
 * stanza folds in, rendered as {@code <media iv cipherKey type? fileName?>{bytes}</media>}
 * by {@link #toNode()}. The payload bytes are expected to be already encrypted under
 * {@link #mediaCipherKey} and {@link #mediaIv}; this type does no encryption itself and
 * carries the ciphertext and key material verbatim onto the wire. Embedders attach
 * screenshots, log dumps, and other binary artefacts to a report by populating the
 * uploads list of {@link SmaxBugReportingReportBugRequest}, which caps the list at ten
 * entries.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutBugReportingReportBugRequest")
public final class SmaxBugReportingReportBugMediaUpload {
    /**
     * Holds the initialization vector that, paired with {@link #mediaCipherKey}, was used
     * to encrypt {@link #mediaElementValue}.
     *
     * <p>Routed verbatim into the {@code iv} attribute by {@link #toNode()}. The value is
     * treated as an opaque string on the wire, so any binary-safe encoding the caller and
     * decoder agree on is preserved end to end.
     */
    private final String mediaIv;

    /**
     * Holds the cipher key paired with {@link #mediaIv}.
     *
     * <p>Routed verbatim into the {@code cipherKey} attribute by {@link #toNode()} and
     * treated as an opaque string on the wire.
     */
    private final String mediaCipherKey;

    /**
     * Holds the optional MIME type of the upload, or {@code null} when the embedder omitted
     * it.
     *
     * <p>Stamped on the {@code type} attribute by {@link #toNode()} only when non-{@code null}.
     */
    private final String mediaType;

    /**
     * Holds the optional original file name of the upload, or {@code null} when the embedder
     * omitted it.
     *
     * <p>Stamped on the {@code fileName} attribute by {@link #toNode()} only when
     * non-{@code null}.
     */
    private final String mediaFileName;

    /**
     * Holds the encrypted blob carried as the {@code <media>} content bytes.
     *
     * <p>Stored and serialised as a raw byte array because the wire layer transmits it
     * verbatim. Callers are expected to have encrypted the payload under
     * {@link #mediaCipherKey} and {@link #mediaIv} before constructing this projection.
     */
    private final byte[] mediaElementValue;

    /**
     * Constructs a media upload from its IV, cipher key, optional metadata, and encrypted
     * payload.
     *
     * <p>The required arguments are null-checked eagerly. Cardinality is not validated here;
     * {@link SmaxBugReportingReportBugRequest} enforces the ten-entry ceiling on the uploads
     * list, so callers do not need to bound the count at this layer.
     *
     * @param mediaIv           the encryption IV; never {@code null}
     * @param mediaCipherKey    the cipher key; never {@code null}
     * @param mediaType         the optional MIME type; may be {@code null}
     * @param mediaFileName     the optional file name; may be {@code null}
     * @param mediaElementValue the encrypted blob; never {@code null}
     * @throws NullPointerException if {@code mediaIv}, {@code mediaCipherKey}, or
     *                              {@code mediaElementValue} is {@code null}
     */
    public SmaxBugReportingReportBugMediaUpload(String mediaIv, String mediaCipherKey, String mediaType,
                       String mediaFileName, byte[] mediaElementValue) {
        this.mediaIv = Objects.requireNonNull(mediaIv, "mediaIv cannot be null");
        this.mediaCipherKey = Objects.requireNonNull(mediaCipherKey, "mediaCipherKey cannot be null");
        this.mediaType = mediaType;
        this.mediaFileName = mediaFileName;
        this.mediaElementValue = Objects.requireNonNull(mediaElementValue, "mediaElementValue cannot be null");
    }

    /**
     * Returns the encryption IV.
     *
     * <p>The value populates the {@code iv} attribute in {@link #toNode()} and lets a parsed
     * request decrypt {@link #mediaElementValue}.
     *
     * @return the IV; never {@code null}
     */
    public String mediaIv() {
        return mediaIv;
    }

    /**
     * Returns the cipher key.
     *
     * <p>The value populates the {@code cipherKey} attribute in {@link #toNode()}.
     *
     * @return the cipher key; never {@code null}
     */
    public String mediaCipherKey() {
        return mediaCipherKey;
    }

    /**
     * Returns the optional MIME type.
     *
     * <p>Empty when the embedder omitted it, in which case {@link #toNode()} leaves the
     * {@code type} attribute off the rendered child.
     *
     * @return an {@link Optional} carrying the MIME type
     */
    public Optional<String> mediaType() {
        return Optional.ofNullable(mediaType);
    }

    /**
     * Returns the optional file name.
     *
     * <p>Empty when the embedder omitted it, in which case {@link #toNode()} leaves the
     * {@code fileName} attribute off the rendered child.
     *
     * @return an {@link Optional} carrying the file name
     */
    public Optional<String> mediaFileName() {
        return Optional.ofNullable(mediaFileName);
    }

    /**
     * Returns the encrypted blob.
     *
     * @implNote
     * This implementation returns the backing array directly rather than a defensive copy,
     * so {@link #toNode()} can serialise it without an intermediate allocation; callers must
     * not mutate the returned buffer.
     *
     * @return the bytes; never {@code null}
     */
    public byte[] mediaElementValue() {
        return mediaElementValue;
    }

    /**
     * Builds the {@code <media iv cipherKey type? fileName?>{bytes}</media>} child stanza.
     *
     * <p>The {@code iv} and {@code cipherKey} attributes and the content bytes are always
     * present; {@code type} and {@code fileName} are added only when their backing fields are
     * non-{@code null}. {@link SmaxBugReportingReportBugRequest#toStanza()} appends the result as
     * one entry of the repeated {@code media} slot.
     *
     * @return the rendered {@link Stanza}
     */
    @WhatsAppWebExport(moduleName = "WASmaxOutBugReportingReportBugRequest",
            exports = "makeReportBugRequestMedia",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Stanza toStanza() {
        var builder = new StanzaBuilder()
                .description("media")
                .attribute("iv", mediaIv)
                .attribute("cipherKey", mediaCipherKey)
                .content(mediaElementValue);
        if (mediaType != null) {
            builder.attribute("type", mediaType);
        }
        if (mediaFileName != null) {
            builder.attribute("fileName", mediaFileName);
        }
        return builder.build();
    }

    /**
     * Indicates whether the given object is a media upload equal to this one.
     *
     * <p>Two uploads are equal when their IV, cipher key, MIME type, file name, and the
     * contents of their payload arrays all match.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is an equal upload; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxBugReportingReportBugMediaUpload) obj;
        return Objects.equals(this.mediaIv, that.mediaIv)
                && Objects.equals(this.mediaCipherKey, that.mediaCipherKey)
                && Objects.equals(this.mediaType, that.mediaType)
                && Objects.equals(this.mediaFileName, that.mediaFileName)
                && Arrays.equals(this.mediaElementValue, that.mediaElementValue);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * <p>The payload array contributes through {@link Arrays#hashCode(byte[])} so equal
     * uploads produce equal codes.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        var result = Objects.hash(mediaIv, mediaCipherKey, mediaType, mediaFileName);
        result = 31 * result + Arrays.hashCode(mediaElementValue);
        return result;
    }

    /**
     * Returns a debug representation listing every field, including the decoded payload bytes.
     *
     * @return the string form
     */
    @Override
    public String toString() {
        return "SmaxBugReportingReportBugMediaUpload[mediaIv=" + mediaIv
                + ", mediaCipherKey=" + mediaCipherKey
                + ", mediaType=" + mediaType
                + ", mediaFileName=" + mediaFileName
                + ", mediaElementValue=" + Arrays.toString(mediaElementValue) + ']';
    }
}
