package com.github.auties00.cobalt.ack;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * {@link AckResult} variant returned for {@code <ack class="message">} stanzas.
 *
 * <p>Carries the message-send pipeline's fanout slots in addition to the common envelope: the
 * {@link #sync() sync priority class}, the {@link #phash() participant-hash} for group and status
 * fanouts, the {@link #refreshLid() LID-refresh hint}, the server's expected
 * {@link #addressingMode() addressing mode}, and the {@link #count() recipient count}.
 */
public final class MessageAck implements AckResult {
    private final String id;
    private final Instant timestamp;
    private final String type;
    private final Jid from;
    private final Jid participant;
    private final Jid recipient;
    private final Integer error;
    private final String sync;
    private final String phash;
    private final boolean refreshLid;
    private final String addressingMode;
    private final Integer count;

    /**
     * Constructs a message ack snapshot. Package-private; the only caller is {@link AckParser}.
     */
    MessageAck(String id, Instant timestamp, String type, Jid from, Jid participant, Jid recipient,
               Integer error, String sync, String phash, boolean refreshLid,
               String addressingMode, Integer count) {
        this.id = id;
        this.timestamp = timestamp;
        this.type = type;
        this.from = from;
        this.participant = participant;
        this.recipient = recipient;
        this.error = error;
        this.sync = sync;
        this.phash = phash;
        this.refreshLid = refreshLid;
        this.addressingMode = addressingMode;
        this.count = count;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public AckClass ackClass() {
        return AckClass.MESSAGE;
    }

    @Override
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }

    @Override
    public Optional<String> type() {
        return Optional.ofNullable(type);
    }

    @Override
    public Optional<Jid> from() {
        return Optional.ofNullable(from);
    }

    @Override
    public Optional<Jid> participant() {
        return Optional.ofNullable(participant);
    }

    @Override
    public Optional<Jid> recipient() {
        return Optional.ofNullable(recipient);
    }

    @Override
    public OptionalInt error() {
        return error != null ? OptionalInt.of(error) : OptionalInt.empty();
    }

    /**
     * Returns the priority {@code sync} marker carried on the ack.
     *
     * <p>Reflects the priority class the server picked for this stanza; the receiver pipeline
     * aligns the local notification-priority shelf against this value.
     *
     * @return the {@code sync} value, or {@link Optional#empty()} when absent
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCommonApi", exports = "sendMsgAckSyncParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<String> sync() {
        return Optional.ofNullable(sync);
    }

    /**
     * Returns the participant-hash echoed by the server for a group or status fanout.
     *
     * <p>A non-empty value signals that the server's participant view differs from the local one;
     * the group and 1:1 senders resolve the delta devices and re-encrypt via the per-device
     * group-direct path.
     *
     * @return the {@code phash} value, or {@link Optional#empty()} when the views matched
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCommonApi", exports = "sendMsgAckSyncParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<String> phash() {
        return Optional.ofNullable(phash);
    }

    /**
     * Returns whether the server asked for a LID refresh for the recipient.
     *
     * <p>When {@code true}, the user-message send path issues a follow-up device-list sync so the
     * local LID-to-PN mapping catches up with the server's.
     *
     * @return {@code true} when {@code refresh_lid="true"} was carried on the ack
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCommonApi", exports = "sendMsgAckSyncParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    public boolean refreshLid() {
        return refreshLid;
    }

    /**
     * Returns the addressing mode the server expects for this chat or group.
     *
     * <p>A value that differs from the mode the client used signals an addressing-mode mismatch;
     * the group-message send path migrates participant JIDs and clears sender-key distribution
     * state in response.
     *
     * @return {@code "lid"} or {@code "pn"}, or {@link Optional#empty()} when absent
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCommonApi", exports = "sendMsgAckSyncParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<String> addressingMode() {
        return Optional.ofNullable(addressingMode);
    }

    /**
     * Returns the recipient count reported on group and CAG fanout acks.
     *
     * <p>Populates the {@code count} metric slot of the {@code WebcMessageSend} WAM event emitted
     * by the send pipeline.
     *
     * @return the {@code count} value, or {@link OptionalInt#empty()} when the server did not
     *         report one
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCommonApi", exports = "sendMsgAckSyncParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    public OptionalInt count() {
        return count != null ? OptionalInt.of(count) : OptionalInt.empty();
    }

    /**
     * Returns whether the ack carried a non-empty {@code phash}.
     *
     * <p>Equivalent to {@code phash().isPresent()}. The group and 1:1 senders branch on this to
     * drive the per-device group-direct or chat-resend fanout.
     *
     * @return {@code true} when a {@code phash} attribute was present
     */
    public boolean hasPhashMismatch() {
        return phash != null;
    }

    @Override
    public String toString() {
        return "MessageAck[id=" + id + ", error=" + error + ", phash=" + phash
                + ", count=" + count + ']';
    }
}
