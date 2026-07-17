package com.github.auties00.cobalt.calls.signaling.incall;

import com.github.auties00.cobalt.wire.linked.call.datachannel.E2eRekeyPayload;
import com.github.auties00.cobalt.wire.linked.call.datachannel.E2eRekeyPayloadSpec;
import com.github.auties00.cobalt.wire.linked.call.datachannel.RekeyKeyEntry;
import com.github.auties00.cobalt.wire.linked.call.datachannel.RekeyKeyType;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents an {@code <enc_rekey>} action, a group call end to end media key rotation.
 *
 * <p>A rekey rotates the per call media keys when the group membership changes. One is emitted on
 * participant join, leave, and other lifecycle events so every connected device converges on a fresh
 * key generation. This is the one signaling action whose payload is a protobuf rather than flat
 * attributes: an {@link E2eRekeyPayload} carrying one {@link RekeyKeyEntry} per encryption domain
 * ({@link RekeyKeyType#AUDIO}, {@link RekeyKeyType#VIDEO}, {@link RekeyKeyType#APPDATA}, each a
 * thirty two byte key). This record carries that payload alongside the universal call header, the
 * rotation transaction identifier, the key generation version, the retry counter, and an optional
 * key request flag.
 *
 * <p>The {@link #transactionId() transaction id} drives the receiver's ordering: a rekey whose
 * transaction id is older than the one already applied is dropped as stale, an exactly matching one is
 * idempotent, and a newer one is buffered until the call catches up. The transaction id is bounded
 * below {@value #MAX_TRANSACTION_ID}; a rekey carrying a key payload is ignored before the call is
 * accepted and in one to one calls, where the simpler offer/accept key fanout applies instead.
 *
 * <p>On the wire the element is a top level child of {@code <call>} with no enclosing wrapper:
 * {@snippet lang = xml:
 * <enc_rekey call-id="..." call-creator="..." transaction-id="N" ver="2" retry="R">PAYLOAD</enc_rekey>
 *}
 * where {@code PAYLOAD} is the serialized {@link E2eRekeyPayload} protobuf. A key request rekey
 * carries the {@code request_keys} attribute and no payload: it asks connected peers to resend their
 * current keys rather than installing new ones.
 *
 * @implNote This implementation caps the transaction id below {@value #MAX_TRANSACTION_ID}, mirroring
 * the sender guard that accepts a rotation only when its transaction id is strictly less than
 * {@value #MAX_TRANSACTION_ID}. The only accepted key generation version is {@value #SUPPORTED_VERSION}.
 * The repeated {@link RekeyKeyEntry} list serializes as a nanopb protobuf whose enum orders the
 * encryption domains as audio (index {@code 0}), video (index {@code 1}), and appdata (index
 * {@code 2}).
 *
 * @see E2eRekeyPayload
 * @see SignalingType#REKEY
 */
public final class RekeyStanza implements CallMessage {
    /**
     * The wire element tag for a rekey action.
     */
    public static final String ELEMENT = "enc_rekey";

    /**
     * The wire attribute naming the rotation transaction id on an {@code <enc_rekey>} element.
     */
    private static final String TRANSACTION_ID_ATTRIBUTE = "transaction-id";

    /**
     * The wire attribute naming the key generation version on an {@code <enc_rekey>} element.
     */
    private static final String VERSION_ATTRIBUTE = "ver";

    /**
     * The wire attribute naming the retry counter on an {@code <enc_rekey>} element.
     */
    private static final String RETRY_ATTRIBUTE = "retry";

    /**
     * The wire attribute marking a rekey as a key request probe.
     */
    private static final String REQUEST_KEYS_ATTRIBUTE = "request_keys";

    /**
     * The exclusive upper bound the sender enforces on a rekey's transaction id.
     */
    public static final int MAX_TRANSACTION_ID = 6;

    /**
     * The only key generation version the engine supports for a rekey.
     */
    public static final int SUPPORTED_VERSION = 2;

    /**
     * The call identifier this rekey's {@code call-id} header carries.
     */
    private final String callId;

    /**
     * The call creator device JID this rekey's {@code call-creator} header carries.
     */
    private final Jid callCreator;

    /**
     * The rotation transaction id used for stale versus fresh ordering, or {@code -1} when absent.
     */
    private final int transactionId;

    /**
     * The key generation version, or {@code -1} when absent.
     */
    private final int version;

    /**
     * The retry counter, or {@code -1} when absent.
     */
    private final int retry;

    /**
     * Whether this rekey is a {@code request_keys} probe asking peers to resend their current keys
     * rather than installing new ones.
     */
    private final boolean requestKeys;

    /**
     * The protobuf key bundle, or {@code null} for a key request rekey carrying no new keys.
     */
    private final E2eRekeyPayload payload;

    /**
     * Constructs a rekey action, validating the required header.
     *
     * @param callId        the call identifier; never {@code null}
     * @param callCreator   the call creator's device JID; never {@code null}
     * @param transactionId the rotation transaction id used for stale versus fresh ordering, or
     *                      {@code -1} when absent
     * @param version       the key generation version, or {@code -1} when absent
     * @param retry         the retry counter, or {@code -1} when absent
     * @param requestKeys   whether this rekey is a {@code request_keys} probe asking peers to resend
     *                      their current keys rather than installing new ones
     * @param payload       the protobuf key bundle, or {@code null} for a key request rekey carrying no
     *                      new keys
     * @throws NullPointerException if {@code callId} or {@code callCreator} is {@code null}
     */
    public RekeyStanza(String callId, Jid callCreator, int transactionId, int version, int retry,
                       boolean requestKeys, E2eRekeyPayload payload) {
        this.callId = Objects.requireNonNull(callId, "callId cannot be null");
        this.callCreator = Objects.requireNonNull(callCreator, "callCreator cannot be null");
        this.transactionId = transactionId;
        this.version = version;
        this.retry = retry;
        this.requestKeys = requestKeys;
        this.payload = payload;
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for a rekey
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for a rekey
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns the rotation transaction id used for stale versus fresh ordering, or {@code -1} when
     * absent.
     *
     * @return the transaction id, or {@code -1} when absent
     */
    public int transactionId() {
        return transactionId;
    }

    /**
     * Returns the key generation version, or {@code -1} when absent.
     *
     * @return the version, or {@code -1} when absent
     */
    public int version() {
        return version;
    }

    /**
     * Returns the retry counter, or {@code -1} when absent.
     *
     * @return the retry counter, or {@code -1} when absent
     */
    public int retry() {
        return retry;
    }

    /**
     * Returns whether this rekey is a {@code request_keys} probe.
     *
     * @return {@code true} when this rekey asks peers to resend their current keys
     */
    public boolean requestKeys() {
        return requestKeys;
    }

    /**
     * Returns the protobuf key bundle, or {@code null} for a key request rekey.
     *
     * @return the key bundle, or {@code null} when absent
     */
    public E2eRekeyPayload payload() {
        return payload;
    }

    /**
     * Returns a rekey carrying a fresh key bundle.
     *
     * @param callId        the call identifier
     * @param callCreator   the call creator's device JID
     * @param transactionId the rotation transaction id
     * @param version       the key generation version
     * @param retry         the retry counter
     * @param payload       the protobuf key bundle
     * @return the key bearing rekey
     * @throws NullPointerException if {@code callId}, {@code callCreator}, or {@code payload} is
     *                              {@code null}
     */
    public static RekeyStanza withKeys(String callId, Jid callCreator, int transactionId, int version, int retry, E2eRekeyPayload payload) {
        Objects.requireNonNull(payload, "payload cannot be null");
        return new RekeyStanza(callId, callCreator, transactionId, version, retry, false, payload);
    }

    /**
     * Returns a key request rekey carrying no new keys.
     *
     * <p>A key request rekey asks the connected peers to resend their current keys; it carries the
     * {@code request_keys} flag and no payload.
     *
     * @param callId        the call identifier
     * @param callCreator   the call creator's device JID
     * @param transactionId the rotation transaction id, or {@code -1} when not applicable
     * @return the key request rekey
     * @throws NullPointerException if {@code callId} or {@code callCreator} is {@code null}
     */
    public static RekeyStanza requestKeys(String callId, Jid callCreator, int transactionId) {
        return new RekeyStanza(callId, callCreator, transactionId, -1, -1, true, null);
    }

    /**
     * Returns the rotation transaction id, if present.
     *
     * @return an {@link OptionalInt} holding the transaction id, or empty when absent
     */
    public OptionalInt transactionIdValue() {
        return transactionId < 0 ? OptionalInt.empty() : OptionalInt.of(transactionId);
    }

    /**
     * Returns the key generation version, if present.
     *
     * @return an {@link OptionalInt} holding the version, or empty when absent
     */
    public OptionalInt versionValue() {
        return version < 0 ? OptionalInt.empty() : OptionalInt.of(version);
    }

    /**
     * Returns the retry counter, if present.
     *
     * @return an {@link OptionalInt} holding the retry counter, or empty when absent
     */
    public OptionalInt retryValue() {
        return retry < 0 ? OptionalInt.empty() : OptionalInt.of(retry);
    }

    /**
     * Returns the protobuf key bundle, if present.
     *
     * @return an {@link Optional} holding the {@link E2eRekeyPayload}, or empty for a key request rekey
     */
    public Optional<E2eRekeyPayload> payloadValue() {
        return Optional.ofNullable(payload);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SignalingType#REKEY}, the message type of a rekey
     */
    @Override
    public SignalingType type() {
        return SignalingType.REKEY;
    }

    /**
     * Builds the {@code <enc_rekey>} action stanza.
     *
     * <p>The common header is stamped first, then the transaction id, version, and retry attributes
     * when present, then the {@code request_keys} attribute when this is a key request rekey. The
     * serialized {@link E2eRekeyPayload} protobuf, when present, becomes the element's binary content.
     * Absent attributes are omitted rather than written as sentinels.
     *
     * @return the rekey action stanza
     */
    @Override
    public Stanza toStanza() {
        var builder = CallMessages.stampHeader(new StanzaBuilder().description(ELEMENT), callId, callCreator)
                .attribute(TRANSACTION_ID_ATTRIBUTE, transactionId, transactionId >= 0)
                .attribute(VERSION_ATTRIBUTE, version, version >= 0)
                .attribute(RETRY_ATTRIBUTE, retry, retry >= 0)
                .attribute(REQUEST_KEYS_ATTRIBUTE, "1", requestKeys);
        if (payload != null) {
            builder.content(E2eRekeyPayloadSpec.encode(payload));
        }
        return builder.build();
    }

    /**
     * Decodes an {@code <enc_rekey>} action stanza into a {@link RekeyStanza}.
     *
     * <p>The element's binary content, when present, is parsed as an {@link E2eRekeyPayload} protobuf;
     * an element with no content decodes to a rekey without a payload. The {@code request_keys} attribute,
     * the transaction id, the version, and the retry counter are read from the element's attributes.
     *
     * @param stanza the {@code <enc_rekey>} stanza
     * @return the decoded rekey
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent
     */
    public static RekeyStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var transactionId = stanza.getAttributeAsInt(TRANSACTION_ID_ATTRIBUTE, -1);
        var version = stanza.getAttributeAsInt(VERSION_ATTRIBUTE, -1);
        var retry = stanza.getAttributeAsInt(RETRY_ATTRIBUTE, -1);
        var requestKeys = stanza.getAttributeAsBool(REQUEST_KEYS_ATTRIBUTE, false);
        var payload = stanza.toContentBytes()
                .map(E2eRekeyPayloadSpec::decode)
                .orElse(null);
        return new RekeyStanza(callId, callCreator, transactionId, version, retry, requestKeys, payload);
    }

    /**
     * Returns whether {@code obj} is a {@link RekeyStanza} with equal components.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a value equal rekey
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof RekeyStanza that
                && callId.equals(that.callId)
                && callCreator.equals(that.callCreator)
                && transactionId == that.transactionId
                && version == that.version
                && retry == that.retry
                && requestKeys == that.requestKeys
                && Objects.equals(payload, that.payload));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this rekey
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, transactionId, version, retry, requestKeys, payload);
    }

    /**
     * Returns a debug oriented string for this rekey.
     *
     * @return the debug string
     */
    @Override
    public String toString() {
        return "RekeyStanza[callId=" + callId
                + ", callCreator=" + callCreator
                + ", transactionId=" + transactionId
                + ", version=" + version
                + ", retry=" + retry
                + ", requestKeys=" + requestKeys
                + ", payload=" + payload + ']';
    }
}
