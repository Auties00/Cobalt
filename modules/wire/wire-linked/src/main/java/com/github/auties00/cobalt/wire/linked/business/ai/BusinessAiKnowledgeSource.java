package com.github.auties00.cobalt.wire.linked.business.ai;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;

/**
 * One source of material a WhatsApp Business AI agent learns from.
 *
 * <p>The merchant's auto-reply assistant can be fed from three kinds of
 * source: the account's past chat history, an ingested website, or an
 * uploaded file (such as a price list or a brochure). This model is a single
 * configured source of any of those kinds, flattened into one shape so a
 * caller can list every source uniformly.
 *
 * <p>{@link #sourceKind()} discriminates the kind. The shared fields
 * ({@link #id()}, {@link #label()}, {@link #lastUpdated()}) are present for
 * every kind. The file-specific fields ({@link #fileName()},
 * {@link #downloadUrl()}, {@link #thumbnailUrl()}, {@link #fileType()},
 * {@link #mimeType()}) are populated only for uploaded-file sources, and
 * {@link #created()} is populated only for chat-history and website sources.
 */
@ProtobufMessage(name = "BusinessAiKnowledgeSource")
public final class BusinessAiKnowledgeSource {
    /**
     * Server-defined marker discriminating the source kind (chat history,
     * website, or uploaded file). The full value set is not recoverable from
     * the WhatsApp client, so the raw marker is exposed as a string. Empty
     * when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String sourceKind;

    /**
     * Server-issued identifier of this source. This is the handle used to
     * remove the source; it is not a WhatsApp address. Empty when the server
     * omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String id;

    /**
     * Human-readable label for the source as shown to the merchant. Empty
     * when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String label;

    /**
     * Instant the source was last updated. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    final Instant lastUpdated;

    /**
     * Instant the source was created. Populated only for chat-history and
     * website sources. Empty otherwise.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    final Instant created;

    /**
     * File name the merchant supplied when uploading the file. Populated only
     * for uploaded-file sources. Empty otherwise.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String fileName;

    /**
     * CDN URL at which the uploaded file can be downloaded. Populated only
     * for uploaded-file sources. Empty otherwise.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String downloadUrl;

    /**
     * URL of a thumbnail rendered from the uploaded file. Populated only for
     * uploaded-file sources. Empty otherwise.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    final String thumbnailUrl;

    /**
     * Server-defined marker classifying the uploaded file's kind. The full
     * value set is not recoverable from the WhatsApp client, so the raw
     * marker is exposed as a string. Populated only for uploaded-file
     * sources. Empty otherwise.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    final String fileType;

    /**
     * MIME type of the uploaded file (for example {@code "application/pdf"}).
     * Populated only for uploaded-file sources. Empty otherwise.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    final String mimeType;

    /**
     * Constructs a new {@code BusinessAiKnowledgeSource}. Every argument may
     * be {@code null} when the server omitted the corresponding field; the
     * kind-specific arguments are {@code null} for sources of a different
     * kind.
     *
     * @param sourceKind   the source-kind marker, or {@code null}
     * @param id           the source identifier, or {@code null}
     * @param label        the display label, or {@code null}
     * @param lastUpdated  the last-update instant, or {@code null}
     * @param created      the creation instant, or {@code null}
     * @param fileName     the uploaded file name, or {@code null}
     * @param downloadUrl  the uploaded-file download URL, or {@code null}
     * @param thumbnailUrl the uploaded-file thumbnail URL, or {@code null}
     * @param fileType     the uploaded-file kind marker, or {@code null}
     * @param mimeType     the uploaded-file MIME type, or {@code null}
     */
    BusinessAiKnowledgeSource(String sourceKind, String id, String label, Instant lastUpdated, Instant created, String fileName, String downloadUrl, String thumbnailUrl, String fileType, String mimeType) {
        this.sourceKind = sourceKind;
        this.id = id;
        this.label = label;
        this.lastUpdated = lastUpdated;
        this.created = created;
        this.fileName = fileName;
        this.downloadUrl = downloadUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.fileType = fileType;
        this.mimeType = mimeType;
    }

    /**
     * Returns the marker discriminating the source kind.
     *
     * @return the source-kind marker, or empty when the server omitted it
     */
    public Optional<String> sourceKind() {
        return Optional.ofNullable(sourceKind);
    }

    /**
     * Returns the server-issued identifier of this source.
     *
     * @return the source id, or empty when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the human-readable label for the source.
     *
     * @return the label, or empty when the server omitted it
     */
    public Optional<String> label() {
        return Optional.ofNullable(label);
    }

    /**
     * Returns the instant the source was last updated.
     *
     * @return the last-update instant, or empty when the server omitted it
     */
    public Optional<Instant> lastUpdated() {
        return Optional.ofNullable(lastUpdated);
    }

    /**
     * Returns the instant the source was created.
     *
     * @return the creation instant, or empty when the server omitted it
     *         (including for uploaded-file sources, which carry none)
     */
    public Optional<Instant> created() {
        return Optional.ofNullable(created);
    }

    /**
     * Returns the file name the merchant supplied when uploading the file.
     *
     * @return the file name, or empty when the source is not an uploaded
     *         file or the server omitted it
     */
    public Optional<String> fileName() {
        return Optional.ofNullable(fileName);
    }

    /**
     * Returns the CDN URL at which the uploaded file can be downloaded.
     *
     * @return the download URL, or empty when the source is not an uploaded
     *         file or the server omitted it
     */
    public Optional<String> downloadUrl() {
        return Optional.ofNullable(downloadUrl);
    }

    /**
     * Returns the URL of a thumbnail rendered from the uploaded file.
     *
     * @return the thumbnail URL, or empty when the source is not an uploaded
     *         file or the server omitted it
     */
    public Optional<String> thumbnailUrl() {
        return Optional.ofNullable(thumbnailUrl);
    }

    /**
     * Returns the marker classifying the uploaded file's kind.
     *
     * @return the file-kind marker, or empty when the source is not an
     *         uploaded file or the server omitted it
     */
    public Optional<String> fileType() {
        return Optional.ofNullable(fileType);
    }

    /**
     * Returns the MIME type of the uploaded file.
     *
     * @return the MIME type, or empty when the source is not an uploaded
     *         file or the server omitted it
     */
    public Optional<String> mimeType() {
        return Optional.ofNullable(mimeType);
    }
}
