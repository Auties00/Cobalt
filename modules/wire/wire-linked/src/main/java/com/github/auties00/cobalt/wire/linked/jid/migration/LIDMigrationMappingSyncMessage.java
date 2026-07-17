package com.github.auties00.cobalt.wire.linked.jid.migration;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Carries the compressed, protobuf-encoded payload used to synchronise
 * phone-number-to-LID mappings from the primary device to a companion
 * device during a 1:1 chat LID migration.
 *
 * <p>WhatsApp is progressively moving 1:1 chats from phone-number-based
 * addressing (for example {@code 1234567890@s.whatsapp.net}) to LID-based
 * addressing (for example {@code 123456@lid}). When the primary device
 * performs this migration, it must inform every companion device which
 * phone number now corresponds to which LID so that each companion can
 * migrate its local chat threads. This message is the envelope used for
 * that notification: it travels as a nested field of a protocol message
 * inside a regular WhatsApp message stanza sent from the primary to the
 * companion.
 *
 * <p>The payload is deliberately carried as opaque bytes for two reasons:
 * <ul>
 *   <li>It is gzip-compressed, because the mapping list can be large for
 *       users with many 1:1 chats.</li>
 *   <li>It is a self-contained protobuf message
 *       ({@link LIDMigrationMappingSyncPayload}), which allows it to be
 *       versioned independently of the outer envelope.</li>
 * </ul>
 *
 * <p>To access the mappings, the companion device reads
 * {@link #encodedMappingPayload()}, gzip-decompresses the bytes and decodes
 * the result as a {@link LIDMigrationMappingSyncPayload}. If any step of
 * this process fails the companion aborts the migration: WhatsApp Web
 * treats such failures as fatal and logs the device out so that a clean
 * re-pairing can be performed.
 */
@ProtobufMessage(name = "LIDMigrationMappingSyncMessage")
public final class LIDMigrationMappingSyncMessage {
    /**
     * The gzip-compressed, protobuf-encoded bytes of the underlying
     * {@link LIDMigrationMappingSyncPayload}.
     *
     * <p>The field is an opaque byte blob on the wire. To obtain the
     * structured mappings the bytes must first be inflated with gzip and
     * the result must then be decoded as a
     * {@code LIDMigrationMappingSyncPayload} protobuf message.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] encodedMappingPayload;


    /**
     * Constructs a new {@code LIDMigrationMappingSyncMessage} with the
     * supplied payload bytes.
     *
     * <p>This constructor is package-private and is invoked by the generated
     * protobuf builder or by the decoder when an incoming migration message
     * is parsed. Use the generated
     * {@code LIDMigrationMappingSyncMessageBuilder} to create instances
     * from application code.
     *
     * @param encodedMappingPayload the gzip-compressed, protobuf-encoded
     *        payload bytes, or {@code null} when the field is not set
     */
    LIDMigrationMappingSyncMessage(byte[] encodedMappingPayload) {
        this.encodedMappingPayload = encodedMappingPayload;
    }

    /**
     * Returns the gzip-compressed, protobuf-encoded payload bytes, if one
     * was supplied.
     *
     * <p>To obtain the structured mappings, inflate the returned bytes with
     * gzip and decode the result as a {@link LIDMigrationMappingSyncPayload}.
     *
     * @return an {@link Optional} containing the payload bytes, or
     *         {@link Optional#empty()} if the field was absent on the wire
     */
    public Optional<byte[]> encodedMappingPayload() {
        return Optional.ofNullable(encodedMappingPayload);
    }

    /**
     * Replaces the gzip-compressed, protobuf-encoded payload bytes, or
     * clears the field when {@code null} is supplied.
     *
     * <p>This setter exists for the mutable builder pathway and for tests
     * that need to mutate a decoded instance. Production code should prefer
     * constructing a new message through the generated builder.
     *
     * @param encodedMappingPayload the new payload bytes, or {@code null}
     *        to clear the field
     */
    public void setEncodedMappingPayload(byte[] encodedMappingPayload) {
        this.encodedMappingPayload = encodedMappingPayload;
    }
}
