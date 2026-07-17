package com.github.auties00.cobalt.wire.linked.message.system;

import com.github.auties00.cobalt.wire.linked.message.Message;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A system message used by the sticker "Re-upload on Missing Reference" (RMR)
 * synchronisation flow.
 *
 * <p>When a client references a sticker by its file hash but cannot locate
 * the underlying media on the server, it issues this request so that peer
 * devices re-upload the missing stickers. Each request carries the list of
 * file hashes that the initiator is looking for, a string identifying the
 * source of the request, and the timestamp at which the request was issued
 * so that responders can deduplicate repeated queries.
 */
@ProtobufMessage(name = "Message.StickerSyncRMRMessage")
public final class StickerSyncRMRMessage implements Message {
    /**
     * The file hashes of the stickers that are missing and need to be
     * re-uploaded.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    List<String> filehash;

    /**
     * An opaque string identifying the originator of the RMR request.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String rmrSource;

    /**
     * The timestamp, in seconds, at which the RMR request was issued.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant requestTimestamp;


    /**
     * Constructs a new sticker sync RMR request.
     *
     * @param filehash         the missing sticker file hashes, may be {@code null}
     * @param rmrSource        the originator identifier, may be {@code null}
     * @param requestTimestamp the request timestamp, may be {@code null}
     */
    StickerSyncRMRMessage(List<String> filehash, String rmrSource, Instant requestTimestamp) {
        this.filehash = filehash;
        this.rmrSource = rmrSource;
        this.requestTimestamp = requestTimestamp;
    }

    /**
     * Returns the file hashes of the stickers that need to be re-uploaded.
     *
     * @return an unmodifiable list of file hashes, or an empty list if none
     *         have been set
     */
    public List<String> filehash() {
        return filehash == null ? List.of() : Collections.unmodifiableList(filehash);
    }

    /**
     * Returns the opaque string identifying the originator of the RMR request.
     *
     * @return an {@link Optional} containing the source identifier, or
     *         {@link Optional#empty()} if no source is set
     */
    public Optional<String> rmrSource() {
        return Optional.ofNullable(rmrSource);
    }

    /**
     * Returns the timestamp at which the RMR request was issued.
     *
     * @return an {@link Optional} containing the timestamp, or
     *         {@link Optional#empty()} if no timestamp is set
     */
    public Optional<Instant> requestTimestamp() {
        return Optional.ofNullable(requestTimestamp);
    }

    /**
     * Sets the file hashes of the stickers that need to be re-uploaded.
     *
     * @param filehash the new list of file hashes, or {@code null} to clear it
     */
    public void setFilehash(List<String> filehash) {
        this.filehash = filehash;
    }

    /**
     * Sets the opaque string identifying the originator of the RMR request.
     *
     * @param rmrSource the new source identifier, or {@code null} to clear it
     */
    public void setRmrSource(String rmrSource) {
        this.rmrSource = rmrSource;
    }

    /**
     * Sets the timestamp at which the RMR request was issued.
     *
     * @param requestTimestamp the new timestamp, or {@code null} to clear it
     */
    public void setRequestTimestamp(Instant requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
    }
}
