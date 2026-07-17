package com.github.auties00.cobalt.wire.linked.jid.migration;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a single mapping entry that associates a phone number with its
 * corresponding LID identifiers during a 1:1 chat LID migration.
 *
 * <p>WhatsApp is gradually moving 1:1 chats away from phone-number-based
 * addressing (for example {@code 1234567890@s.whatsapp.net}) toward
 * LID-based addressing (for example {@code 123456@lid}) so that a user's
 * phone number is no longer exposed inside the protocol. When the primary
 * device performs this migration, it needs to tell every companion device
 * (such as WhatsApp Web/Desktop) which phone number now corresponds to which
 * LID so that each companion can migrate its local chat threads accordingly.
 *
 * <p>Each instance of this class represents exactly one such correspondence
 * and is transported as an element of the
 * {@link LIDMigrationMappingSyncPayload#pnToLidMappings()} list inside a
 * {@link LIDMigrationMappingSyncMessage}.
 *
 * <p>Three identifiers are carried:
 * <ul>
 *   <li>{@link #pn() pn}: the phone number JID being migrated.</li>
 *   <li>{@link #assignedLid() assignedLid}: the LID that the primary
 *       device assigned to the phone number at the time the migration
 *       record was created.</li>
 *   <li>{@link #latestLid() latestLid}: the most recent LID the primary
 *       device currently holds for the same user. When this differs from
 *       {@code assignedLid}, the user's LID has changed since the migration
 *       record was created, and companions use this to detect and resolve
 *       mapping conflicts when migrating their local threads.</li>
 * </ul>
 *
 * <p>Instances are created by the protobuf layer when decoding an incoming
 * migration sync message. Applications built on top of Cobalt usually do
 * not construct this type directly; they consume the decoded mappings to
 * rewrite chat keys from phone-number addressing to LID addressing.
 */
@ProtobufMessage(name = "LIDMigrationMapping")
public final class LIDMigrationMapping {
    /**
     * The numeric user part of the phone number JID being migrated.
     *
     * <p>This is the user portion of a standard WhatsApp JID, for example
     * {@code 1234567890} from {@code 1234567890@s.whatsapp.net}. The value
     * is stored as a raw {@code long} for compact wire representation and
     * is exposed as a fully formed {@link Jid} through {@link #pn()}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT64)
    long pn;

    /**
     * The numeric user part of the LID assigned to the phone number by the
     * primary device at the moment this migration record was produced.
     *
     * <p>This is the user portion of a LID JID, for example {@code 123456}
     * from {@code 123456@lid}. It is exposed as a fully formed {@link Jid}
     * through {@link #assignedLid()}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    long assignedLid;

    /**
     * The numeric user part of the most recent LID known to the primary
     * device, or {@code null} when not provided.
     *
     * <p>When this value is present and differs from {@link #assignedLid},
     * it indicates that the user's LID changed after the migration record
     * was created. Companion devices use this information to detect
     * conflicting mappings and decide which LID to adopt. The value is
     * exposed as a fully formed {@link Jid} through {@link #latestLid()}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
    Long latestLid;


    /**
     * Constructs a new {@code LIDMigrationMapping} with the supplied values.
     *
     * <p>This constructor is package-private and is invoked by the generated
     * protobuf builder or by the decoder when an incoming migration message
     * is parsed. Use the generated {@code LIDMigrationMappingBuilder} to
     * create instances from application code.
     *
     * @param pn          the numeric user part of the phone number JID; must
     *                    not be {@code null}
     * @param assignedLid the numeric user part of the LID assigned by the
     *                    primary device; must not be {@code null}
     * @param latestLid   the numeric user part of the most recent LID, or
     *                    {@code null} if the primary device did not provide
     *                    one
     * @throws NullPointerException if {@code pn} or {@code assignedLid} is
     *                              {@code null}
     */
    LIDMigrationMapping(Long pn, Long assignedLid, Long latestLid) {
        this.pn = Objects.requireNonNull(pn);
        this.assignedLid = Objects.requireNonNull(assignedLid);
        this.latestLid = latestLid;
    }

    /**
     * Returns the phone number JID being migrated as a fully formed
     * {@link Jid}.
     *
     * <p>The raw numeric user part carried by this entry is combined with
     * the default WhatsApp user server to produce a JID such as
     * {@code 1234567890@s.whatsapp.net}.
     *
     * @return the phone number JID being migrated
     */
    public Jid pn() {
        return Jid.of(pn);
    }

    /**
     * Returns the LID that the primary device assigned to the phone number
     * when this migration record was produced.
     *
     * <p>The raw numeric user part is combined with the LID server to
     * produce a JID such as {@code 123456@lid}.
     *
     * @return the assigned LID JID for the phone number in this entry
     */
    public Jid assignedLid() {
        return Jid.of(String.valueOf(assignedLid), JidServer.lid());
    }

    /**
     * Returns the most recent LID known to the primary device for this
     * phone number, if one was provided.
     *
     * <p>When present, this value supersedes {@link #assignedLid()} and
     * indicates that the user's LID changed after this migration record
     * was created. Callers migrating their local state should compare the
     * two values to detect conflicts and decide which LID to adopt.
     *
     * @return an {@link Optional} containing the latest LID JID, or
     *         {@link Optional#empty()} if the primary device did not
     *         provide one
     */
    public Optional<Jid> latestLid() {
        return latestLid == null
                ? Optional.empty()
                : Optional.of(Jid.of(String.valueOf(latestLid), JidServer.lid()));
    }

    /**
     * Replaces the numeric user part of the phone number being migrated.
     *
     * <p>This setter exists for the mutable builder pathway and for tests
     * that need to mutate a decoded instance. Production code should prefer
     * constructing a new mapping through the generated builder.
     *
     * @param pn the new numeric user part of the phone number JID
     */
    public void setPn(long pn) {
        this.pn = pn;
    }

    /**
     * Replaces the numeric user part of the LID assigned by the primary
     * device.
     *
     * <p>This setter exists for the mutable builder pathway and for tests
     * that need to mutate a decoded instance. Production code should prefer
     * constructing a new mapping through the generated builder.
     *
     * @param assignedLid the new numeric user part of the assigned LID
     */
    public void setAssignedLid(long assignedLid) {
        this.assignedLid = assignedLid;
    }

    /**
     * Replaces the most recent LID known to the primary device, or clears
     * it when {@code null} is supplied.
     *
     * <p>This setter exists for the mutable builder pathway and for tests
     * that need to mutate a decoded instance. Production code should prefer
     * constructing a new mapping through the generated builder.
     *
     * @param latestLid the new numeric user part of the most recent LID,
     *                  or {@code null} to clear the field
     */
    public void setLatestLid(Long latestLid) {
        this.latestLid = latestLid;
    }
}
