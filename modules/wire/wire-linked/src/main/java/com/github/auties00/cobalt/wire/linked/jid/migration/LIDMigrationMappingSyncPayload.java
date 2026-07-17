package com.github.auties00.cobalt.wire.linked.jid.migration;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The decoded body of a {@link LIDMigrationMappingSyncMessage}, carrying
 * the list of phone-number-to-LID mappings the primary device has
 * produced together with the timestamp at which the primary migrated its
 * own chat database.
 *
 * <p>WhatsApp is progressively moving 1:1 chats from phone-number-based
 * addressing (for example {@code 1234567890@s.whatsapp.net}) to LID-based
 * addressing (for example {@code 123456@lid}). When the primary device
 * performs this migration, it gzip-compresses and protobuf-encodes an
 * instance of this class and ships the resulting bytes inside a
 * {@link LIDMigrationMappingSyncMessage}. The companion device then reverses
 * those steps to obtain the structured payload documented here.
 *
 * <p>A typical companion-side consumer of this payload will:
 * <ol>
 *   <li>Iterate {@link #pnToLidMappings()} and, for each entry, rewrite
 *       its local chat key from the phone number JID to the corresponding
 *       assigned LID JID.</li>
 *   <li>Use {@link #chatDbMigrationTimestamp()} as a cut-off to decide
 *       whether locally observed mappings or primary-provided mappings
 *       should win when a conflict is detected, based on whether the local
 *       knowledge predates the primary's migration.</li>
 * </ol>
 *
 * <p>An empty {@link #pnToLidMappings() mappings} list is treated as a
 * malformed payload by WhatsApp Web: when this happens the companion logs
 * itself out because it cannot safely migrate without a mapping table.
 */
@ProtobufMessage(name = "LIDMigrationMappingSyncPayload")
public final class LIDMigrationMappingSyncPayload {
    /**
     * The list of phone-number-to-LID mapping entries produced by the
     * primary device, one per 1:1 chat affected by the migration.
     *
     * <p>Each entry associates a phone number with the LID that now
     * represents the same user. The list is repeated on the wire and may
     * be empty when the primary has no chats to migrate; when decoded by a
     * companion device, an empty list is interpreted as a malformed
     * payload.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<LIDMigrationMapping> pnToLidMappings;

    /**
     * The Unix epoch second at which the primary device migrated its local
     * chat database, or {@code null} when not provided.
     *
     * <p>Companion devices use this timestamp to break ties when their own
     * locally observed mappings disagree with the primary's mappings: the
     * side with the more recent knowledge wins. The value is stored as a
     * raw {@code Long} for compact wire representation and is exposed as
     * an {@link Instant} through {@link #chatDbMigrationTimestamp()}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    Long chatDbMigrationTimestamp;


    /**
     * Constructs a new {@code LIDMigrationMappingSyncPayload} with the
     * supplied values.
     *
     * <p>This constructor is package-private and is invoked by the generated
     * protobuf builder or by the decoder when an incoming migration payload
     * is parsed. Use the generated
     * {@code LIDMigrationMappingSyncPayloadBuilder} to create instances
     * from application code.
     *
     * @param pnToLidMappings          the list of phone-number-to-LID
     *                                 mapping entries, or {@code null} if
     *                                 the field was absent on the wire
     * @param chatDbMigrationTimestamp the Unix epoch second at which the
     *                                 primary device migrated its chat
     *                                 database, or {@code null} if the
     *                                 field was absent on the wire
     */
    LIDMigrationMappingSyncPayload(List<LIDMigrationMapping> pnToLidMappings, Long chatDbMigrationTimestamp) {
        this.pnToLidMappings = pnToLidMappings;
        this.chatDbMigrationTimestamp = chatDbMigrationTimestamp;
    }

    /**
     * Returns an unmodifiable view of the phone-number-to-LID mapping
     * entries produced by the primary device.
     *
     * <p>When the field was absent on the wire or the list was {@code null},
     * this method returns {@link List#of()} rather than {@code null}, so
     * callers can iterate the result without a null check. An empty list
     * is a signal that the payload is malformed and the companion should
     * refuse to migrate.
     *
     * @return an unmodifiable list of mapping entries, or an empty list
     *         when no mappings were provided
     */
    public List<LIDMigrationMapping> pnToLidMappings() {
        return pnToLidMappings == null ? List.of() : Collections.unmodifiableList(pnToLidMappings);
    }

    /**
     * Returns the timestamp at which the primary device migrated its own
     * chat database, if one was provided.
     *
     * <p>The raw Unix epoch second is converted to an {@link Instant} for
     * convenience. Callers use this timestamp as a tie-breaker against
     * their own locally observed mappings when resolving conflicts during
     * migration.
     *
     * @return an {@link Optional} containing the migration timestamp as an
     *         {@code Instant}, or {@link Optional#empty()} when the field
     *         was absent on the wire
     */
    public Optional<Instant> chatDbMigrationTimestamp() {
        return chatDbMigrationTimestamp == null
                ? Optional.empty()
                : Optional.of(Instant.ofEpochSecond(chatDbMigrationTimestamp));
    }

    /**
     * Replaces the list of phone-number-to-LID mapping entries, or clears
     * it when {@code null} is supplied.
     *
     * <p>This setter exists for the mutable builder pathway and for tests
     * that need to mutate a decoded instance. Production code should prefer
     * constructing a new payload through the generated builder.
     *
     * @param pnToLidMappings the new list of mapping entries, or
     *                        {@code null} to clear the field
     */
    public void setPnToLidMappings(List<LIDMigrationMapping> pnToLidMappings) {
        this.pnToLidMappings = pnToLidMappings;
    }

    /**
     * Replaces the Unix epoch second at which the primary device migrated
     * its chat database, or clears it when {@code null} is supplied.
     *
     * <p>This setter exists for the mutable builder pathway and for tests
     * that need to mutate a decoded instance. Production code should prefer
     * constructing a new payload through the generated builder.
     *
     * @param chatDbMigrationTimestamp the new migration timestamp as
     *                                 seconds since the Unix epoch, or
     *                                 {@code null} to clear the field
     */
    public void setChatDbMigrationTimestamp(Long chatDbMigrationTimestamp) {
        this.chatDbMigrationTimestamp = chatDbMigrationTimestamp;
    }
}
