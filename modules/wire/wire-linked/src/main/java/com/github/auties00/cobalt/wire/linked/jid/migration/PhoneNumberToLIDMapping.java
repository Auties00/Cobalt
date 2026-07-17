package com.github.auties00.cobalt.wire.linked.jid.migration;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents a single phone-number-to-LID correspondence embedded inside
 * a history sync conversation, pairing the phone number JID and the LID
 * JID that identify the same user.
 *
 * <p>History sync is the mechanism WhatsApp Web/Desktop uses to rebuild a
 * companion device's chat database when it first pairs with a primary
 * device (or when recent chats must be reloaded). For each conversation
 * included in the sync, the primary may attach a list of entries of this
 * type to inform the companion that a given phone number should from now
 * on be addressed as a LID. The companion walks the list and, for every
 * entry where both JIDs are present, writes a local phone-number-to-LID
 * mapping so that subsequent lookups can translate between the two
 * representations.
 *
 * <p>This class plays the same conceptual role as {@link LIDMigrationMapping}
 * but is used on a different code path: {@code LIDMigrationMapping} carries
 * the mapping as raw numeric user parts inside a real-time migration
 * notification, whereas {@code PhoneNumberToLIDMapping} carries the mapping
 * as fully formed {@link Jid} strings inside a history sync chunk. Entries
 * where either {@link #pnJid()} or {@link #lidJid()} is missing are
 * silently ignored by the companion and must not be used to update the
 * local mapping table.
 */
@ProtobufMessage(name = "PhoneNumberToLIDMapping")
public final class PhoneNumberToLIDMapping {
    /**
     * The phone number JID side of the mapping, for example
     * {@code 1234567890@s.whatsapp.net}.
     *
     * <p>When absent on the wire this field is {@code null} and the entry
     * is considered incomplete; companion devices skip such entries when
     * building their local mapping table.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid pnJid;

    /**
     * The LID JID that corresponds to the phone number, for example
     * {@code 123456@lid}.
     *
     * <p>When absent on the wire this field is {@code null} and the entry
     * is considered incomplete; companion devices skip such entries when
     * building their local mapping table.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    Jid lidJid;


    /**
     * Constructs a new {@code PhoneNumberToLIDMapping} with the supplied
     * JIDs.
     *
     * <p>This constructor is package-private and is invoked by the generated
     * protobuf builder or by the decoder when an incoming history sync
     * chunk is parsed. Use the generated
     * {@code PhoneNumberToLIDMappingBuilder} to create instances from
     * application code.
     *
     * @param pnJid  the phone number JID, or {@code null} when the field
     *               was absent on the wire
     * @param lidJid the corresponding LID JID, or {@code null} when the
     *               field was absent on the wire
     */
    PhoneNumberToLIDMapping(Jid pnJid, Jid lidJid) {
        this.pnJid = pnJid;
        this.lidJid = lidJid;
    }

    /**
     * Returns the phone number JID side of the mapping, if one was
     * provided.
     *
     * <p>Entries where this value is empty are considered incomplete and
     * must be skipped by consumers building a local mapping table.
     *
     * @return an {@link Optional} containing the phone number JID, or
     *         {@link Optional#empty()} when the field was absent on the
     *         wire
     */
    public Optional<Jid> pnJid() {
        return Optional.ofNullable(pnJid);
    }

    /**
     * Returns the LID JID side of the mapping, if one was provided.
     *
     * <p>Entries where this value is empty are considered incomplete and
     * must be skipped by consumers building a local mapping table.
     *
     * @return an {@link Optional} containing the LID JID, or
     *         {@link Optional#empty()} when the field was absent on the
     *         wire
     */
    public Optional<Jid> lidJid() {
        return Optional.ofNullable(lidJid);
    }

    /**
     * Replaces the phone number JID side of the mapping, or clears it
     * when {@code null} is supplied.
     *
     * <p>This setter exists for the mutable builder pathway and for tests
     * that need to mutate a decoded instance. Production code should prefer
     * constructing a new mapping through the generated builder.
     *
     * @param pnJid the new phone number JID, or {@code null} to clear the
     *              field
     */
    public void setPnJid(Jid pnJid) {
        this.pnJid = pnJid;
    }

    /**
     * Replaces the LID JID side of the mapping, or clears it when
     * {@code null} is supplied.
     *
     * <p>This setter exists for the mutable builder pathway and for tests
     * that need to mutate a decoded instance. Production code should prefer
     * constructing a new mapping through the generated builder.
     *
     * @param lidJid the new LID JID, or {@code null} to clear the field
     */
    public void setLidJid(Jid lidJid) {
        this.lidJid = lidJid;
    }
}
