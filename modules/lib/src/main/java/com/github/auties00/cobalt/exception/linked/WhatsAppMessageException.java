package com.github.auties00.cobalt.exception.linked;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Sealed root for failures while sending or receiving an individual
 * WhatsApp message.
 *
 * <p>The hierarchy splits into two halves. {@link Receive} subtypes cover
 * everything that can go wrong while decrypting and validating an incoming
 * message: missing Signal sessions, invalid keys, replayed counters, MAC
 * failures, sender-key issues for group messages, multi-device validation
 * problems, device-verification failures, protobuf validation, and a small
 * number of WhatsApp-specific protocol errors. {@link Send} subtypes cover
 * the equivalent problems on the outgoing side: session and key bring-up
 * failures, stale device lists, server rejections with their numeric error
 * code, recipient-level problems, oversize payloads, expired messages,
 * monthly send caps, timeouts, duplicate sends, and authorization
 * failures.
 *
 * @apiNote
 * Raised for a single message; {@link #toErrorResult()} returns
 * {@link WhatsAppLinkedClientErrorResult#DISCARD} for every subtype, so a
 * configured {@code WhatsAppClientErrorHandler} can leave the session
 * running while it sends a retry receipt (for receives), retries the send,
 * or surfaces the failure to the caller.
 *
 * @implNote
 * This implementation returns {@link WhatsAppLinkedClientErrorResult#DISCARD} for
 * every message failure: a per-message failure emits a retry receipt or a
 * NACK without tearing the session down.
 *
 * @see Receive
 * @see Send
 */
public sealed class WhatsAppMessageException extends WhatsAppLinkedException
        permits WhatsAppMessageException.Receive, WhatsAppMessageException.Send {

    /**
     * Constructs a new message exception with the specified detail message.
     *
     * @param message the detail message describing the error
     */
    public WhatsAppMessageException(String message) {
        super(message);
    }

    /**
     * Constructs a new message exception with the specified detail message and cause.
     *
     * @param message the detail message describing the error
     * @param cause   the underlying cause of this exception
     */
    public WhatsAppMessageException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new message exception wrapping the specified cause.
     *
     * @param cause the underlying cause of this exception
     */
    public WhatsAppMessageException(Throwable cause) {
        super(cause);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns
     * {@link WhatsAppLinkedClientErrorResult#DISCARD}: a per-message failure emits
     * a retry receipt or a NACK without tearing the session down.
     */
    @Override
    public WhatsAppLinkedClientErrorResult toErrorResult() {
        return WhatsAppLinkedClientErrorResult.DISCARD;
    }

    /**
     * Sealed root for failures during the decryption and validation of
     * an incoming WhatsApp message.
     *
     * <p>Most of these failures cause Cobalt to send a retry receipt back
     * to the sender so the message can be retransmitted with fresh
     * cryptographic material. The retry reason that accompanies the
     * receipt is exposed through {@link #retryReason()};
     * {@link #shouldSendRetryReceipt()} reports whether a receipt should
     * be sent at all (duplicates do not warrant a retry); and
     * {@link #errorCode()} carries the optional NACK error code used for
     * validation-style failures.
     *
     * @apiNote
     * Raised on the receive path; a configured
     * {@code WhatsAppClientErrorHandler} reads {@link #retryReason()} and
     * {@link #shouldSendRetryReceipt()} to decide whether to ask the
     * sender to retransmit.
     *
     * @see RetryReason
     */
    public sealed static abstract class Receive extends WhatsAppMessageException
            permits Receive.NoSession, Receive.InvalidKey, Receive.InvalidKeyId,
                    Receive.InvalidOneTimeKey, Receive.InvalidSignedPreKey,
                    Receive.InvalidMessage, Receive.InvalidSignature,
                    Receive.DuplicateMessage, Receive.FutureMessage,
                    Receive.BadMac, Receive.NoSenderKey, Receive.InvalidSenderKey,
                    Receive.UnknownDevice, Receive.InvalidDeviceSentMessage,
                    Receive.AdvFailure, Receive.InvalidProtobuf,
                    Receive.BroadcastEphemeralSettings, Receive.HsmMismatch,
                    Receive.Unknown {

        /**
         * Constructs a new decryption exception with the specified detail message.
         *
         * @param message the detail message describing the decryption failure
         */
        protected Receive(String message) {
            super(message);
        }

        /**
         * Constructs a new decryption exception with the specified detail message and cause.
         *
         * @param message the detail message describing the decryption failure
         * @param cause   the underlying cause of this exception
         */
        protected Receive(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Returns the retry reason that should accompany a retry receipt
         * for this failure.
         *
         * @implSpec
         * Each concrete subtype returns the {@link RetryReason} value that
         * corresponds to its failure condition.
         *
         * @return the retry reason, never {@code null}
         */
        public abstract RetryReason retryReason();

        /**
         * Returns whether a retry receipt should be sent to the sender
         * for this failure.
         *
         * @apiNote
         * Most failures warrant a retry receipt so the sender
         * retransmits the message; the exception is duplicate messages,
         * which are silently dropped.
         *
         * @return {@code true} when a retry receipt should be sent
         */
        public boolean shouldSendRetryReceipt() {
            return true;
        }

        /**
         * Returns the NACK error code for this failure, when one applies.
         *
         * @apiNote
         * Validation-style failures (such as a malformed protobuf or a
         * bad device-sent message) trigger a NACK receipt instead of, or
         * in addition to, a retry receipt.
         *
         * @return the NACK error code, or empty when none applies
         */
        public Optional<String> errorCode() {
            return Optional.empty();
        }

        /**
         * The retry reason codes WhatsApp defines for retry receipts.
         *
         * <p>The numeric value carried in the receipt tells the sender what
         * kind of failure happened so it can pick the right corrective
         * action (resend the message, redeliver the key bundle,
         * redistribute the sender key, and so on). The constants and their
         * {@link #protocolValue()} integers match the wire protocol
         * one-for-one.
         */
        public enum RetryReason {
            /**
             * Generic failure that does not match any specific reason.
             */
            UNKNOWN_ERROR(0),

            /**
             * No Signal session exists with the sender device, so the
             * message cannot be decrypted as a regular Signal message.
             */
            SIGNAL_ERROR_NO_SESSION(1),

            /**
             * A public key in the message failed validation.
             */
            SIGNAL_ERROR_INVALID_KEY(2),

            /**
             * A key identifier in the message does not match any stored key.
             */
            SIGNAL_ERROR_INVALID_KEY_ID(3),

            /**
             * The Signal message body does not conform to the expected layout.
             */
            SIGNAL_ERROR_INVALID_MESSAGE(4),

            /**
             * A cryptographic signature on the message or key bundle did
             * not verify.
             */
            SIGNAL_ERROR_INVALID_SIGNATURE(5),

            /**
             * The message counter is too far ahead of the local counter
             * to be accepted.
             */
            SIGNAL_ERROR_FUTURE_MESSAGE(6),

            /**
             * The HMAC over the message did not validate.
             */
            SIGNAL_ERROR_BAD_MAC(7),

            /**
             * The Signal session exists but is in an inconsistent state.
             */
            SIGNAL_ERROR_INVALID_SESSION(8),

            /**
             * The per-message key derived from the session is unusable.
             */
            SIGNAL_ERROR_INVALID_MSG_KEY(9),

            /**
             * The ephemeral-message settings published for a broadcast
             * list could not be validated.
             */
            BAD_BROADCAST_EPH_SETTINGS(10),

            /**
             * A message arrived from a companion device that has not yet
             * uploaded a prekey bundle.
             */
            UNKNOWN_COMPANION_NO_PREKEY(11),

            /**
             * The sender's Advanced Device Verification chain did not
             * validate.
             */
            ADV_FAILURE(12),

            /**
             * A status revoke arrived too soon after the original
             * status, so the operation has to wait.
             */
            STATUS_REVOKE_DELAY(13);

            /**
             * The integer value carried in retry receipt stanzas.
             */
            private final int protocolValue;

            /**
             * Constructs a retry reason with the specified protocol value.
             *
             * @param protocolValue the integer value used in the protocol
             */
            RetryReason(int protocolValue) {
                this.protocolValue = protocolValue;
            }

            /**
             * Returns the integer value carried in retry receipt stanzas.
             *
             * @return the protocol integer value
             */
            public int protocolValue() {
                return protocolValue;
            }

            /**
             * Returns whether a retry receipt should be sent for this reason.
             *
             * @return {@code true} when a retry receipt should be sent
             */
            public boolean shouldSendRetryReceipt() {
                return true;
            }

            /**
             * Returns the retry reason that matches the given protocol
             * integer value.
             *
             * @param value the protocol integer value
             * @return the matching retry reason
             * @throws IllegalArgumentException when {@code value} does not match any known reason
             */
            public static RetryReason fromProtocolValue(int value) {
                for (var reason : values()) {
                    if (reason.protocolValue == value) {
                        return reason;
                    }
                }
                throw new IllegalArgumentException("Unknown retry reason code: " + value);
            }
        }

        /**
         * Thrown when no Signal session exists with the sender of an
         * incoming message.
         *
         * <p>Happens when the contact is new, when the local session was
         * evicted, or when the sender just registered a new device.
         *
         * @apiNote
         * Raised when an inbound message cannot be decrypted for lack of a
         * session; the retry receipt asks the sender to retransmit as a
         * prekey message that carries the material needed to start one.
         */
        public static final class NoSession extends Receive {
            /**
             * Whether this is a group sender-key session error rather
             * than a one-to-one Signal session error.
             */
            private final boolean isGroupSession;

            /**
             * Constructs a new no-session exception for a 1:1 session.
             */
            public NoSession() {
                super("No Signal session exists with sender device");
                this.isGroupSession = false;
            }

            /**
             * Constructs a new no-session exception with the specified message.
             *
             * @param message        the detail message
             * @param isGroupSession {@code true} when the missing session is a group sender-key session
             */
            public NoSession(String message, boolean isGroupSession) {
                super(message);
                this.isGroupSession = isGroupSession;
            }

            /**
             * Constructs a new no-session exception with a cause.
             *
             * @param message        the detail message
             * @param isGroupSession {@code true} when the missing session is a group sender-key session
             * @param cause          the underlying cause
             */
            public NoSession(String message, boolean isGroupSession, Throwable cause) {
                super(message, cause);
                this.isGroupSession = isGroupSession;
            }

            /**
             * Returns whether this is a group sender-key session error.
             *
             * @return {@code true} for a group session error,
             *         {@code false} for a 1:1 session error
             */
            public boolean isGroupSession() {
                return isGroupSession;
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation returns
             * {@link RetryReason#SIGNAL_ERROR_NO_SESSION}.
             */
            @Override
            public RetryReason retryReason() {
                return RetryReason.SIGNAL_ERROR_NO_SESSION;
            }
        }

        /**
         * Thrown when a public key carried by an incoming message fails
         * validation.
         *
         * @apiNote
         * Typically raised when the bytes do not encode a valid
         * Curve25519 point.
         */
        public static final class InvalidKey extends Receive {
            /**
             * Constructs a new invalid key exception.
             */
            public InvalidKey() {
                super("Invalid cryptographic key in Signal message");
            }

            /**
             * Constructs a new invalid key exception with the specified message.
             *
             * @param message the detail message
             */
            public InvalidKey(String message) {
                super(message);
            }

            /**
             * Constructs a new invalid key exception with a cause.
             *
             * @param message the detail message
             * @param cause   the underlying cause
             */
            public InvalidKey(String message, Throwable cause) {
                super(message, cause);
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation returns
             * {@link RetryReason#SIGNAL_ERROR_INVALID_KEY}.
             */
            @Override
            public RetryReason retryReason() {
                return RetryReason.SIGNAL_ERROR_INVALID_KEY;
            }
        }

        /**
         * Thrown when a key identifier in an incoming message does not
         * match any stored key.
         *
         * @apiNote
         * Most commonly caused by key rotation that happened on one
         * side but not the other.
         */
        public static final class InvalidKeyId extends Receive {
            /**
             * The unrecognized key identifier, when known.
             */
            private final Integer keyId;

            /**
             * Constructs a new invalid key id exception.
             */
            public InvalidKeyId() {
                super("Invalid key identifier in Signal message");
                this.keyId = null;
            }

            /**
             * Constructs a new invalid key id exception with the offending key id.
             *
             * @param keyId the unrecognized key identifier
             */
            public InvalidKeyId(int keyId) {
                super("Invalid key identifier in Signal message: " + keyId);
                this.keyId = keyId;
            }

            /**
             * Constructs a new invalid key id exception with a cause.
             *
             * @param message the detail message
             * @param cause   the underlying cause
             */
            public InvalidKeyId(String message, Throwable cause) {
                super(message, cause);
                this.keyId = null;
            }

            /**
             * Returns the unrecognized key identifier, when known.
             *
             * @return the key id, or empty when not available
             */
            public Optional<Integer> keyId() {
                return Optional.ofNullable(keyId);
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation returns
             * {@link RetryReason#SIGNAL_ERROR_INVALID_KEY_ID}.
             */
            @Override
            public RetryReason retryReason() {
                return RetryReason.SIGNAL_ERROR_INVALID_KEY_ID;
            }
        }

        /**
         * Thrown when a one-time prekey referenced by an incoming
         * message is not usable.
         *
         * @apiNote
         * Typically the prekey was already consumed or no longer exists
         * locally.
         */
        public static final class InvalidOneTimeKey extends Receive {
            /**
             * The offending prekey identifier, when known.
             */
            private final Integer prekeyId;

            /**
             * Constructs a new invalid one-time key exception.
             */
            public InvalidOneTimeKey() {
                super("Invalid one-time prekey in Signal message");
                this.prekeyId = null;
            }

            /**
             * Constructs a new invalid one-time key exception with the offending key id.
             *
             * @param prekeyId the prekey identifier
             */
            public InvalidOneTimeKey(int prekeyId) {
                super("Invalid one-time prekey in Signal message: " + prekeyId);
                this.prekeyId = prekeyId;
            }

            /**
             * Constructs a new invalid one-time key exception with a cause.
             *
             * @param message the detail message
             * @param cause   the underlying cause
             */
            public InvalidOneTimeKey(String message, Throwable cause) {
                super(message, cause);
                this.prekeyId = null;
            }

            /**
             * Returns the prekey identifier, when known.
             *
             * @return the prekey id, or empty when not available
             */
            public Optional<Integer> prekeyId() {
                return Optional.ofNullable(prekeyId);
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation returns
             * {@link RetryReason#SIGNAL_ERROR_INVALID_KEY}.
             */
            @Override
            public RetryReason retryReason() {
                return RetryReason.SIGNAL_ERROR_INVALID_KEY;
            }
        }

        /**
         * Thrown when the signed prekey referenced by an incoming
         * message is not usable.
         *
         * @apiNote
         * Triggered when the signature does not verify, the key has
         * expired, or the identifier is not known locally.
         */
        public static final class InvalidSignedPreKey extends Receive {
            /**
             * The offending signed prekey identifier, when known.
             */
            private final Integer signedPrekeyId;

            /**
             * Constructs a new invalid signed prekey exception.
             */
            public InvalidSignedPreKey() {
                super("Invalid signed prekey in Signal message");
                this.signedPrekeyId = null;
            }

            /**
             * Constructs a new invalid signed prekey exception with the offending key id.
             *
             * @param signedPrekeyId the signed prekey identifier
             */
            public InvalidSignedPreKey(int signedPrekeyId) {
                super("Invalid signed prekey in Signal message: " + signedPrekeyId);
                this.signedPrekeyId = signedPrekeyId;
            }

            /**
             * Constructs a new invalid signed prekey exception with a cause.
             *
             * @param message the detail message
             * @param cause   the underlying cause
             */
            public InvalidSignedPreKey(String message, Throwable cause) {
                super(message, cause);
                this.signedPrekeyId = null;
            }

            /**
             * Returns the signed prekey identifier, when known.
             *
             * @return the signed prekey id, or empty when not available
             */
            public Optional<Integer> signedPrekeyId() {
                return Optional.ofNullable(signedPrekeyId);
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation returns
             * {@link RetryReason#SIGNAL_ERROR_INVALID_KEY}.
             */
            @Override
            public RetryReason retryReason() {
                return RetryReason.SIGNAL_ERROR_INVALID_KEY;
            }
        }

        /**
         * Thrown when an incoming Signal message has a structurally
         * invalid body.
         *
         * @apiNote
         * Triggered by a truncated body, wrong version, missing field,
         * or bad ciphertext length.
         */
        public static final class InvalidMessage extends Receive {
            /**
             * Constructs a new invalid message exception.
             */
            public InvalidMessage() {
                super("Invalid Signal message structure");
            }

            /**
             * Constructs a new invalid message exception with the specified message.
             *
             * @param message the detail message
             */
            public InvalidMessage(String message) {
                super(message);
            }

            /**
             * Constructs a new invalid message exception with a cause.
             *
             * @param message the detail message
             * @param cause   the underlying cause
             */
            public InvalidMessage(String message, Throwable cause) {
                super(message, cause);
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation returns
             * {@link RetryReason#SIGNAL_ERROR_INVALID_MESSAGE}.
             */
            @Override
            public RetryReason retryReason() {
                return RetryReason.SIGNAL_ERROR_INVALID_MESSAGE;
            }
        }

        /**
         * Thrown when an Ed25519 signature on an incoming message or
         * key bundle does not verify.
         *
         * @apiNote
         * Signature failures may indicate tampering and are therefore
         * worth surfacing even when the same retry receipt is sent as
         * for other key-related failures.
         */
        public static final class InvalidSignature extends Receive {
            /**
             * Constructs a new invalid signature exception.
             */
            public InvalidSignature() {
                super("Invalid cryptographic signature in Signal message");
            }

            /**
             * Constructs a new invalid signature exception with the specified message.
             *
             * @param message the detail message
             */
            public InvalidSignature(String message) {
                super(message);
            }

            /**
             * Constructs a new invalid signature exception with a cause.
             *
             * @param message the detail message
             * @param cause   the underlying cause
             */
            public InvalidSignature(String message, Throwable cause) {
                super(message, cause);
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation returns
             * {@link RetryReason#SIGNAL_ERROR_INVALID_SIGNATURE}.
             */
            @Override
            public RetryReason retryReason() {
                return RetryReason.SIGNAL_ERROR_INVALID_SIGNATURE;
            }
        }

        /**
         * Thrown when an incoming message reuses a counter value that
         * has already been processed.
         *
         * @apiNote
         * Duplicates are silently dropped because they are usually
         * harmless retransmissions. {@link #shouldSendRetryReceipt()}
         * returns {@code false} so the sender is not asked to retry.
         */
        public static final class DuplicateMessage extends Receive {
            /**
             * The duplicated message counter value, when known.
             */
            private final Long counter;

            /**
             * Constructs a new duplicate message exception.
             */
            public DuplicateMessage() {
                super("Duplicate message counter detected");
                this.counter = null;
            }

            /**
             * Constructs a new duplicate message exception with the counter value.
             *
             * @param counter the duplicated message counter
             */
            public DuplicateMessage(long counter) {
                super("Duplicate message counter detected: " + counter);
                this.counter = counter;
            }

            /**
             * Constructs a new duplicate message exception with a cause.
             *
             * @param message the detail message
             * @param cause   the underlying cause
             */
            public DuplicateMessage(String message, Throwable cause) {
                super(message, cause);
                this.counter = null;
            }

            /**
             * Returns the duplicated counter value, when known.
             *
             * @return the counter, or empty when not available
             */
            public Optional<Long> counter() {
                return Optional.ofNullable(counter);
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation returns
             * {@link RetryReason#SIGNAL_ERROR_FUTURE_MESSAGE}.
             */
            @Override
            public RetryReason retryReason() {
                return RetryReason.SIGNAL_ERROR_FUTURE_MESSAGE;
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation returns {@code false}: duplicates are
             * silently dropped and the sender should not retransmit.
             */
            @Override
            public boolean shouldSendRetryReceipt() {
                return false;
            }
        }

        /**
         * Thrown when an incoming message carries a counter that is too
         * far ahead of the next expected counter to be accepted.
         *
         * @apiNote
         * Typically follows extended message loss or counter
         * desynchronization after a backup restore.
         */
        public static final class FutureMessage extends Receive {
            /**
             * The unexpected counter value, when known.
             */
            private final Long counter;

            /**
             * Whether the offending message was a group message.
             */
            private final boolean isGroupMessage;

            /**
             * Constructs a new future message exception.
             */
            public FutureMessage() {
                super("Message counter too far in future");
                this.counter = null;
                this.isGroupMessage = false;
            }

            /**
             * Constructs a new future message exception with the counter value.
             *
             * @param counter        the future message counter
             * @param isGroupMessage {@code true} when the offending message was a group message
             */
            public FutureMessage(long counter, boolean isGroupMessage) {
                super("Message counter too far in future: " + counter + (isGroupMessage ? " (group)" : ""));
                this.counter = counter;
                this.isGroupMessage = isGroupMessage;
            }

            /**
             * Constructs a new future message exception with a cause.
             *
             * @param message the detail message
             * @param cause   the underlying cause
             */
            public FutureMessage(String message, Throwable cause) {
                super(message, cause);
                this.counter = null;
                this.isGroupMessage = false;
            }

            /**
             * Returns the unexpected counter value, when known.
             *
             * @return the counter, or empty when not available
             */
            public Optional<Long> counter() {
                return Optional.ofNullable(counter);
            }

            /**
             * Returns whether the offending message was a group message.
             *
             * @return {@code true} for group messages
             */
            public boolean isGroupMessage() {
                return isGroupMessage;
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation returns
             * {@link RetryReason#SIGNAL_ERROR_FUTURE_MESSAGE}.
             */
            @Override
            public RetryReason retryReason() {
                return RetryReason.SIGNAL_ERROR_FUTURE_MESSAGE;
            }
        }

        /**
         * Thrown when the HMAC over an incoming Signal message does not
         * validate.
         *
         * <p>The {@link MacErrorType} narrows down the failure mode:
         * whether decryption produced plaintext anyway, whether the cipher
         * key derivation failed, or whether the failure happened on a
         * freshly created ratchet chain. All three carry the same
         * {@link RetryReason#SIGNAL_ERROR_BAD_MAC} on the retry receipt.
         */
        public static final class BadMac extends Receive {
            /**
             * The specific kind of MAC failure.
             */
            private final MacErrorType errorType;

            /**
             * Constructs a new bad MAC exception.
             */
            public BadMac() {
                super("Message authentication code verification failed");
                this.errorType = MacErrorType.UNKNOWN;
            }

            /**
             * Constructs a new bad MAC exception with the specified error type.
             *
             * @param errorType the kind of MAC failure
             * @throws NullPointerException if {@code errorType} is {@code null}
             */
            public BadMac(MacErrorType errorType) {
                super("Message authentication code verification failed: " + errorType);
                this.errorType = Objects.requireNonNull(errorType, "errorType cannot be null");
            }

            /**
             * Constructs a new bad MAC exception with a cause.
             *
             * @param message the detail message
             * @param cause   the underlying cause
             */
            public BadMac(String message, Throwable cause) {
                super(message, cause);
                this.errorType = MacErrorType.UNKNOWN;
            }

            /**
             * Returns the specific kind of MAC failure.
             *
             * @return the MAC error type
             */
            public MacErrorType errorType() {
                return errorType;
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation returns
             * {@link RetryReason#SIGNAL_ERROR_BAD_MAC}.
             */
            @Override
            public RetryReason retryReason() {
                return RetryReason.SIGNAL_ERROR_BAD_MAC;
            }

            /**
             * The kinds of MAC failure that can be reported by
             * {@link BadMac}.
             */
            public enum MacErrorType {
                /**
                 * The specific failure mode is not known.
                 */
                UNKNOWN,

                /**
                 * The MAC did not verify, yet decryption still produced
                 * plaintext. Worth investigating because it can
                 * indicate a key compromise.
                 */
                WITH_DECRYPTED_PLAINTEXT,

                /**
                 * The cipher key derived for this message could not be
                 * used to verify the MAC.
                 */
                INVALID_CIPHER_KEY,

                /**
                 * The cipher key derived on a freshly created ratchet
                 * chain could not be used to verify the MAC.
                 */
                INVALID_CIPHER_KEY_NEW_CHAIN
            }
        }

        /**
         * Thrown when no sender key exists for decrypting an incoming
         * group message.
         *
         * @apiNote
         * The sender has not yet distributed its sender key for the
         * group, the local sender key was evicted, or the recipient
         * just joined the group.
         */
        public static final class NoSenderKey extends Receive {
            /**
             * Constructs a new no sender key exception.
             */
            public NoSenderKey() {
                super("No sender key exists for group message decryption");
            }

            /**
             * Constructs a new no sender key exception with the specified message.
             *
             * @param message the detail message
             */
            public NoSenderKey(String message) {
                super(message);
            }

            /**
             * Constructs a new no sender key exception with a cause.
             *
             * @param message the detail message
             * @param cause   the underlying cause
             */
            public NoSenderKey(String message, Throwable cause) {
                super(message, cause);
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation returns
             * {@link RetryReason#SIGNAL_ERROR_NO_SESSION}.
             */
            @Override
            public RetryReason retryReason() {
                return RetryReason.SIGNAL_ERROR_NO_SESSION;
            }
        }

        /**
         * Thrown when the sender key state for a group is corrupt or
         * otherwise unusable.
         */
        public static final class InvalidSenderKey extends Receive {
            /**
             * Constructs a new invalid sender key exception.
             */
            public InvalidSenderKey() {
                super("Invalid sender key for group message decryption");
            }

            /**
             * Constructs a new invalid sender key exception with the specified message.
             *
             * @param message the detail message
             */
            public InvalidSenderKey(String message) {
                super(message);
            }

            /**
             * Constructs a new invalid sender key exception with a cause.
             *
             * @param message the detail message
             * @param cause   the underlying cause
             */
            public InvalidSenderKey(String message, Throwable cause) {
                super(message, cause);
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation returns
             * {@link RetryReason#SIGNAL_ERROR_NO_SESSION}.
             */
            @Override
            public RetryReason retryReason() {
                return RetryReason.SIGNAL_ERROR_NO_SESSION;
            }
        }

        /**
         * Thrown when an incoming message comes from a companion device
         * that is not on the local device list for the sender.
         *
         * @apiNote
         * The retry receipt asks the unknown device to publish its
         * prekey bundle so the recipient can establish a session with
         * it.
         */
        public static final class UnknownDevice extends Receive {
            /**
             * Constructs a new unknown device exception.
             */
            public UnknownDevice() {
                super("Message received from unknown companion device");
            }

            /**
             * Constructs a new unknown device exception with the specified message.
             *
             * @param message the detail message
             */
            public UnknownDevice(String message) {
                super(message);
            }

            /**
             * Constructs a new unknown device exception with a cause.
             *
             * @param message the detail message
             * @param cause   the underlying cause
             */
            public UnknownDevice(String message, Throwable cause) {
                super(message, cause);
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation returns
             * {@link RetryReason#UNKNOWN_COMPANION_NO_PREKEY}.
             */
            @Override
            public RetryReason retryReason() {
                return RetryReason.UNKNOWN_COMPANION_NO_PREKEY;
            }
        }

        /**
         * Thrown when a Device Sent Message (DSM, the multi-device
         * synchronization envelope) does not pass validation.
         *
         * <p>The {@link DsmErrorType} disambiguates whether the incoming
         * envelope should not have been a DSM, should have been one, or had
         * a malformed structure.
         */
        public static final class InvalidDeviceSentMessage extends Receive {
            /**
             * The specific kind of DSM failure.
             */
            private final DsmErrorType errorType;

            /**
             * Constructs a new invalid DSM exception.
             */
            public InvalidDeviceSentMessage() {
                super("Device sent message validation failed");
                this.errorType = DsmErrorType.INVALID_DSM;
            }

            /**
             * Constructs a new invalid DSM exception with the specified error type.
             *
             * @param errorType the kind of DSM failure
             * @throws NullPointerException if {@code errorType} is {@code null}
             */
            public InvalidDeviceSentMessage(DsmErrorType errorType) {
                super("Device sent message validation failed: " + errorType);
                this.errorType = Objects.requireNonNull(errorType, "errorType cannot be null");
            }

            /**
             * Constructs a new invalid DSM exception with a cause.
             *
             * @param message the detail message
             * @param cause   the underlying cause
             */
            public InvalidDeviceSentMessage(String message, Throwable cause) {
                super(message, cause);
                this.errorType = DsmErrorType.INVALID_DSM;
            }

            /**
             * Returns the specific kind of DSM failure.
             *
             * @return the DSM error type
             */
            public DsmErrorType errorType() {
                return errorType;
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation returns
             * {@link RetryReason#UNKNOWN_ERROR}: a DSM validation
             * failure is a NACK-only condition (see
             * {@link #errorCode()}), not a candidate for a Signal-level
             * retry.
             */
            @Override
            public RetryReason retryReason() {
                return RetryReason.UNKNOWN_ERROR;
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation always returns {@code "400"}.
             */
            @Override
            public Optional<String> errorCode() {
                return Optional.of("400");
            }

            /**
             * The kinds of Device Sent Message validation failure.
             */
            public enum DsmErrorType {
                /**
                 * The message was tagged as a DSM but should not have been.
                 */
                INVALID_SENDER(1),

                /**
                 * The message should have been a DSM but is not.
                 */
                MISSING_DSM(2),

                /**
                 * The DSM envelope structure is malformed.
                 */
                INVALID_DSM(3);

                /**
                 * The integer value used in the protocol.
                 */
                private final int protocolValue;

                /**
                 * Constructs a DSM error type with the specified protocol value.
                 *
                 * @param protocolValue the integer value used in the protocol
                 */
                DsmErrorType(int protocolValue) {
                    this.protocolValue = protocolValue;
                }

                /**
                 * Returns the integer value used in the protocol.
                 *
                 * @return the protocol value
                 */
                public int protocolValue() {
                    return protocolValue;
                }

                /**
                 * Returns the DSM error type that matches the given
                 * protocol integer value.
                 *
                 * @param value the protocol integer value
                 * @return the matching error type
                 * @throws IllegalArgumentException when {@code value} is unknown
                 */
                public static DsmErrorType fromProtocolValue(int value) {
                    for (var type : values()) {
                        if (type.protocolValue == value) {
                            return type;
                        }
                    }
                    throw new IllegalArgumentException("Unknown DSM error code: " + value);
                }
            }
        }

        /**
         * Thrown when the sender's Advanced Device Verification chain does
         * not validate during decryption of an incoming message.
         *
         * <p>The decryption pipeline turns an underlying
         * {@link WhatsAppAdvValidationException} into this receive-side
         * exception, preserving the original cause while carrying the
         * {@link RetryReason#ADV_FAILURE} retry reason.
         *
         * @see WhatsAppAdvValidationException
         */
        public static final class AdvFailure extends Receive {
            /**
             * Constructs a new ADV failure exception.
             */
            public AdvFailure() {
                super("Account device verification failed during message decryption");
            }

            /**
             * Constructs a new ADV failure exception with the specified message.
             *
             * @param message the detail message
             */
            public AdvFailure(String message) {
                super(message);
            }

            /**
             * Constructs a new ADV failure exception with a cause.
             *
             * @param message the detail message
             * @param cause   the underlying cause
             */
            public AdvFailure(String message, Throwable cause) {
                super(message, cause);
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation returns
             * {@link RetryReason#ADV_FAILURE}.
             */
            @Override
            public RetryReason retryReason() {
                return RetryReason.ADV_FAILURE;
            }
        }

        /**
         * Thrown when the protobuf body of a successfully decrypted
         * message cannot be deserialized or fails validation.
         *
         * <p>The {@link ProtobufErrorReason} pinpoints which check failed:
         * a generic parse error, a mismatch between the advertised stanza
         * type and the protobuf body, or the presence of multiple message
         * bodies where one is expected.
         *
         * @apiNote
         * Raised on the receive path as a NACK-only condition;
         * {@link #errorCode()} returns the code captured at construction,
         * defaulting to {@code "400"}, and {@link #retryReason()} reports
         * {@link RetryReason#UNKNOWN_ERROR} since no Signal-level retry
         * applies.
         */
        public static final class InvalidProtobuf extends Receive {
            /**
             * The NACK error code to send.
             */
            private final String errorCode;

            /**
             * The specific protobuf failure reason.
             */
            private final ProtobufErrorReason errorReason;

            /**
             * Constructs a new invalid protobuf exception.
             */
            public InvalidProtobuf() {
                super("Protobuf message validation failed");
                this.errorCode = "400";
                this.errorReason = ProtobufErrorReason.INVALID_MESSAGE;
            }

            /**
             * Constructs a new invalid protobuf exception with the specified message.
             *
             * @param message the detail message
             */
            public InvalidProtobuf(String message) {
                super(message);
                this.errorCode = "400";
                this.errorReason = ProtobufErrorReason.INVALID_MESSAGE;
            }

            /**
             * Constructs a new invalid protobuf exception with detailed information.
             *
             * @param errorCode   the NACK error code
             * @param message     the detail message
             * @param errorReason the specific protobuf failure reason
             * @throws NullPointerException if {@code errorCode} or {@code errorReason} is {@code null}
             */
            public InvalidProtobuf(String errorCode, String message, ProtobufErrorReason errorReason) {
                super(message);
                this.errorCode = Objects.requireNonNull(errorCode, "errorCode cannot be null");
                this.errorReason = Objects.requireNonNull(errorReason, "errorReason cannot be null");
            }

            /**
             * Constructs a new invalid protobuf exception with a cause.
             *
             * @param message the detail message
             * @param cause   the underlying cause
             */
            public InvalidProtobuf(String message, Throwable cause) {
                super(message, cause);
                this.errorCode = "400";
                this.errorReason = ProtobufErrorReason.INVALID_MESSAGE;
            }

            /**
             * Constructs a new invalid protobuf exception with full details and a cause.
             *
             * @param errorCode   the NACK error code
             * @param message     the detail message
             * @param errorReason the specific protobuf failure reason
             * @param cause       the underlying cause
             * @throws NullPointerException if {@code errorCode} or {@code errorReason} is {@code null}
             */
            public InvalidProtobuf(String errorCode, String message, ProtobufErrorReason errorReason, Throwable cause) {
                super(message, cause);
                this.errorCode = Objects.requireNonNull(errorCode, "errorCode cannot be null");
                this.errorReason = Objects.requireNonNull(errorReason, "errorReason cannot be null");
            }

            /**
             * Returns the specific protobuf failure reason.
             *
             * @return the failure reason
             */
            public ProtobufErrorReason errorReason() {
                return errorReason;
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation returns
             * {@link RetryReason#UNKNOWN_ERROR}: protobuf failures
             * surface as NACKs rather than Signal-level retries.
             */
            @Override
            public RetryReason retryReason() {
                return RetryReason.UNKNOWN_ERROR;
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation returns the NACK error code captured
             * at construction time, defaulting to {@code "400"}.
             */
            @Override
            public Optional<String> errorCode() {
                return Optional.of(errorCode);
            }

            /**
             * The kinds of protobuf validation failure.
             */
            public enum ProtobufErrorReason {
                /**
                 * The protobuf could not be parsed or failed generic
                 * validation.
                 */
                INVALID_MESSAGE,

                /**
                 * The message body inside the protobuf does not match
                 * the stanza type that wrapped it.
                 */
                MESSAGE_TYPE_MISMATCH,

                /**
                 * The protobuf carries more than one message body where
                 * exactly one is expected.
                 */
                INVALID_NUMBER_OF_MESSAGE_TYPES
            }
        }

        /**
         * Thrown when the encrypted ephemeral-message settings of a
         * broadcast list cannot be decrypted or validated.
         */
        public static final class BroadcastEphemeralSettings extends Receive {
            /**
             * Constructs a new broadcast ephemeral settings exception.
             */
            public BroadcastEphemeralSettings() {
                super("Failed to decrypt broadcast ephemeral settings");
            }

            /**
             * Constructs a new broadcast ephemeral settings exception with the specified message.
             *
             * @param message the detail message
             */
            public BroadcastEphemeralSettings(String message) {
                super(message);
            }

            /**
             * Constructs a new broadcast ephemeral settings exception with a cause.
             *
             * @param message the detail message
             * @param cause   the underlying cause
             */
            public BroadcastEphemeralSettings(String message, Throwable cause) {
                super(message, cause);
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation returns
             * {@link RetryReason#BAD_BROADCAST_EPH_SETTINGS}.
             */
            @Override
            public RetryReason retryReason() {
                return RetryReason.BAD_BROADCAST_EPH_SETTINGS;
            }
        }

        /**
         * Thrown when an incoming business HSM (Highly Structured
         * Message) template does not match the template the sender's
         * account has registered with WhatsApp.
         */
        public static final class HsmMismatch extends Receive {
            /**
             * Constructs a new HSM mismatch exception.
             */
            public HsmMismatch() {
                super("HSM template mismatch");
            }

            /**
             * Constructs a new HSM mismatch exception with the specified message.
             *
             * @param message the detail message
             */
            public HsmMismatch(String message) {
                super(message);
            }

            /**
             * Constructs a new HSM mismatch exception with a cause.
             *
             * @param message the detail message
             * @param cause   the underlying cause
             */
            public HsmMismatch(String message, Throwable cause) {
                super(message, cause);
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation returns
             * {@link RetryReason#UNKNOWN_ERROR}: HSM mismatches surface
             * as NACKs rather than Signal-level retries.
             */
            @Override
            public RetryReason retryReason() {
                return RetryReason.UNKNOWN_ERROR;
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation always returns {@code "400"}.
             */
            @Override
            public Optional<String> errorCode() {
                return Optional.of("400");
            }
        }

        /**
         * Thrown for decryption failures that do not match any of the
         * more specific subtypes.
         */
        public static final class Unknown extends Receive {
            /**
             * Constructs a new unknown decryption exception.
             */
            public Unknown() {
                super("Unknown message decryption failure");
            }

            /**
             * Constructs a new unknown decryption exception with the specified message.
             *
             * @param message the detail message
             */
            public Unknown(String message) {
                super(message);
            }

            /**
             * Constructs a new unknown decryption exception with a cause.
             *
             * @param message the detail message
             * @param cause   the underlying cause
             */
            public Unknown(String message, Throwable cause) {
                super(message, cause);
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation returns
             * {@link RetryReason#UNKNOWN_ERROR}.
             */
            @Override
            public RetryReason retryReason() {
                return RetryReason.UNKNOWN_ERROR;
            }
        }
    }

    /**
     * Sealed root for failures while sending a WhatsApp message.
     *
     * <p>Each subtype describes a different stage of the send pipeline:
     * missing or invalid Signal sessions, missing or expired sender keys
     * for groups, stale device lists detected by the participant-hash
     * check ({@link PhashMismatch}), identity key changes that need user
     * confirmation, server rejections with their numeric error code,
     * invalid recipients, oversize payloads, expired messages, monthly
     * caps, timeouts, duplicate sends, and authorization errors.
     *
     * @apiNote
     * Raised on the send path; {@link #isRetryable()} reports whether the
     * caller can retry the send after applying the corrective action
     * implied by the subtype.
     */
    public sealed static abstract class Send extends WhatsAppMessageException
            permits Send.NoSession, Send.InvalidKey, Send.NoSenderKey, Send.SenderKeyExpired,
                    Send.PhashMismatch, Send.MissingPreKeys, Send.IdentityChanged,
                    Send.ServerNack, Send.InvalidRecipient, Send.PayloadTooLarge, Send.MessageExpired,
                    Send.MessageCapped, Send.Timeout, Send.Unknown, Send.Duplicate, Send.Unauthorized {

        /**
         * Constructs a new send exception with the specified detail message.
         *
         * @param message the detail message describing the send failure
         */
        protected Send(String message) {
            super(message);
        }

        /**
         * Constructs a new send exception with the specified detail message and cause.
         *
         * @param message the detail message describing the send failure
         * @param cause   the underlying cause of this exception
         */
        protected Send(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Returns whether the failure can be recovered from by retrying
         * the send (possibly after the implied corrective action).
         *
         * @implSpec
         * Each concrete subtype returns a fixed answer, except
         * {@link ServerNack} which dispatches on the numeric code
         * captured at construction.
         *
         * @return {@code true} when the send can be retried
         */
        public abstract boolean isRetryable();

        /**
         * Thrown when the local device has no Signal session with a
         * device that should receive the message.
         *
         * @apiNote
         * Recovery is to fetch the device's prekey bundle and
         * establish a session before retrying the send.
         */
        public static final class NoSession extends Send {
            /**
             * The device that has no session.
             */
            private final Jid deviceJid;

            /**
             * Constructs a new no-session exception.
             *
             * @param deviceJid the device that has no session
             */
            public NoSession(Jid deviceJid) {
                super("No Signal session exists with device: " + deviceJid);
                this.deviceJid = deviceJid;
            }

            /**
             * Constructs a new no-session exception with a cause.
             *
             * @param deviceJid the device that has no session
             * @param cause     the underlying cause
             */
            public NoSession(Jid deviceJid, Throwable cause) {
                super("No Signal session exists with device: " + deviceJid, cause);
                this.deviceJid = deviceJid;
            }

            /**
             * Returns the device that has no session.
             *
             * @return the device JID
             */
            public Jid deviceJid() {
                return deviceJid;
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation always returns {@code true}: after a
             * prekey fetch the send can succeed.
             */
            @Override
            public boolean isRetryable() {
                return true;
            }
        }

        /**
         * Thrown when message encryption fails because of an invalid
         * key, either on the local ratchet or in the recipient's
         * published key bundle.
         */
        public static final class InvalidKey extends Send {
            /**
             * The device with the invalid key.
             */
            private final Jid deviceJid;

            /**
             * Constructs a new invalid key exception.
             *
             * @param deviceJid the device with the invalid key
             */
            public InvalidKey(Jid deviceJid) {
                super("Invalid cryptographic key for device: " + deviceJid);
                this.deviceJid = deviceJid;
            }

            /**
             * Constructs a new invalid key exception with a cause.
             *
             * @param deviceJid the device with the invalid key
             * @param cause     the underlying cause
             */
            public InvalidKey(Jid deviceJid, Throwable cause) {
                super("Invalid cryptographic key for device: " + deviceJid, cause);
                this.deviceJid = deviceJid;
            }

            /**
             * Returns the device with the invalid key.
             *
             * @return the device JID
             */
            public Jid deviceJid() {
                return deviceJid;
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation always returns {@code true}: a fresh
             * key fetch can resolve the mismatch.
             */
            @Override
            public boolean isRetryable() {
                return true;
            }
        }

        /**
         * Thrown when sending a group message but no sender key has
         * been generated yet for the group.
         *
         * @apiNote
         * Recovery is to generate a sender key, distribute it to the
         * participants, and retry the send.
         */
        public static final class NoSenderKey extends Send {
            /**
             * The group missing the sender key.
             */
            private final Jid groupJid;

            /**
             * Constructs a new no-sender-key exception.
             *
             * @param groupJid the group missing the sender key
             */
            public NoSenderKey(Jid groupJid) {
                super("No sender key exists for group: " + groupJid);
                this.groupJid = groupJid;
            }

            /**
             * Constructs a new no-sender-key exception with a cause.
             *
             * @param groupJid the group missing the sender key
             * @param cause    the underlying cause
             */
            public NoSenderKey(Jid groupJid, Throwable cause) {
                super("No sender key exists for group: " + groupJid, cause);
                this.groupJid = groupJid;
            }

            /**
             * Returns the group missing the sender key.
             *
             * @return the group JID
             */
            public Jid groupJid() {
                return groupJid;
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation always returns {@code true}: after
             * sender-key distribution the send can succeed.
             */
            @Override
            public boolean isRetryable() {
                return true;
            }
        }

        /**
         * Thrown when the local sender key for a group has expired and
         * must be rotated and redistributed before sending again.
         */
        public static final class SenderKeyExpired extends Send {
            /**
             * The group whose sender key expired.
             */
            private final Jid groupJid;

            /**
             * Constructs a new sender-key-expired exception.
             *
             * @param groupJid the group whose sender key expired
             */
            public SenderKeyExpired(Jid groupJid) {
                super("Sender key expired for group: " + groupJid);
                this.groupJid = groupJid;
            }

            /**
             * Returns the group whose sender key expired.
             *
             * @return the group JID
             */
            public Jid groupJid() {
                return groupJid;
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation always returns {@code true}: after
             * rotation and redistribution the send can succeed.
             */
            @Override
            public boolean isRetryable() {
                return true;
            }
        }

        /**
         * Thrown when the participant hash (the hash of all the recipient
         * device JIDs) the client computed does not match the value the
         * server expects.
         *
         * <p>A mismatch means the local device list is out of date.
         *
         * @apiNote
         * Raised when a send is rejected for a stale device list;
         * refreshing the list produces a matching hash so the send can be
         * retried.
         */
        public static final class PhashMismatch extends Send {
            /**
             * The participant hash the server expected.
             */
            private final String expectedHash;

            /**
             * The participant hash the client sent.
             */
            private final String actualHash;

            /**
             * Constructs a new phash mismatch exception.
             *
             * @param expectedHash the participant hash the server expected
             * @param actualHash   the participant hash the client sent
             */
            public PhashMismatch(String expectedHash, String actualHash) {
                super("Phash mismatch: computed " + expectedHash);
                this.expectedHash = expectedHash;
                this.actualHash = actualHash;
            }

            /**
             * Returns the participant hash the server expected.
             *
             * @return the expected hash
             */
            public String expectedHash() {
                return expectedHash;
            }

            /**
             * Returns the participant hash the client sent.
             *
             * @return the actual hash
             */
            public String actualHash() {
                return actualHash;
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation always returns {@code true}: after a
             * device-list refresh the hash matches.
             */
            @Override
            public boolean isRetryable() {
                return true;
            }
        }

        /**
         * Thrown when one or more recipient devices need a Signal
         * session but no prekey bundle for them has been fetched yet.
         *
         * @apiNote
         * Recovery is to fetch the missing bundles, establish the
         * sessions, and retry the send.
         */
        public static final class MissingPreKeys extends Send {
            /**
             * The devices for which prekey bundles are missing.
             */
            private final List<Jid> devices;

            /**
             * Constructs a new missing prekeys exception.
             *
             * @param devices the devices for which prekey bundles are missing
             */
            public MissingPreKeys(List<Jid> devices) {
                super("Missing prekey bundles for " + devices.size() + " devices");
                this.devices = List.copyOf(devices);
            }

            /**
             * Returns the devices for which prekey bundles are missing.
             *
             * @return the device JIDs
             */
            public List<Jid> devices() {
                return devices;
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation always returns {@code true}: after
             * the bundles arrive the send can succeed.
             */
            @Override
            public boolean isRetryable() {
                return true;
            }
        }

        /**
         * Thrown when one or more recipient devices have a different
         * identity key than the one the local session was established
         * with.
         *
         * @apiNote
         * An identity key change means the recipient reinstalled the
         * app or paired a new primary device. WhatsApp surfaces this
         * as a "safety number changed" notice and asks the user to
         * confirm before sending again.
         */
        public static final class IdentityChanged extends Send {
            /**
             * The devices whose identity key changed.
             */
            private final List<Jid> devices;

            /**
             * Constructs a new identity changed exception.
             *
             * @param devices the devices whose identity key changed
             */
            public IdentityChanged(List<Jid> devices) {
                super("Identity key changed for " + devices.size() + " devices");
                this.devices = List.copyOf(devices);
            }

            /**
             * Returns the devices whose identity key changed.
             *
             * @return the device JIDs
             */
            public List<Jid> devices() {
                return devices;
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation always returns {@code true}: once
             * the user confirms the new safety number, the send can
             * proceed.
             */
            @Override
            public boolean isRetryable() {
                return true;
            }
        }

        /**
         * Thrown when the server explicitly rejects a message with a NACK
         * response.
         *
         * <p>The numeric {@link #errorCode()} narrows down the reason:
         * stale group addressing, monthly cap, parsing failure, invalid
         * protobuf, missing message secret, stale Signal counter, deletion
         * on the peer, generic unhandled error, unsupported admin revoke,
         * unsupported LID group, server-side database failure, and a few
         * others. {@link #isRetryable()} dispatches on that code.
         */
        public static final class ServerNack extends Send {
            /**
             * The group addressing mode the client used is stale.
             */
            public static final int STALE_GROUP_ADDRESSING_MODE = 421;

            /**
             * The account has hit the monthly new-chat-message cap.
             */
            public static final int NEW_CHAT_MESSAGES_CAPPED = 475;

            /**
             * The server failed to parse the message.
             */
            public static final int PARSING_ERROR = 487;

            /**
             * The stanza shape was not recognized.
             */
            public static final int UNRECOGNIZED_STANZA = 488;

            /**
             * The stanza class was not recognized.
             */
            public static final int UNRECOGNIZED_STANZA_CLASS = 489;

            /**
             * The stanza type was not recognized.
             */
            public static final int UNRECOGNIZED_STANZA_TYPE = 490;

            /**
             * The protobuf body did not validate on the server.
             */
            public static final int INVALID_PROTOBUF = 491;

            /**
             * The hosted-companion stanza was rejected.
             */
            public static final int INVALID_HOSTED_COMPANION_STANZA = 493;

            /**
             * The message did not include a required message secret.
             */
            public static final int MISSING_MESSAGE_SECRET = 495;

            /**
             * The Signal counter on the message was older than the
             * server's view.
             */
            public static final int SIGNAL_ERROR_OLD_COUNTER = 496;

            /**
             * The referenced message has already been deleted on the peer.
             */
            public static final int MESSAGE_DELETED_ON_PEER = 499;

            /**
             * The server hit a generic unhandled error.
             */
            public static final int UNHANDLED_ERROR = 500;

            /**
             * The admin-revoke operation is not supported in this context.
             */
            public static final int UNSUPPORTED_ADMIN_REVOKE = 550;

            /**
             * LID groups are not supported here.
             */
            public static final int UNSUPPORTED_LID_GROUP = 551;

            /**
             * A server-side database operation failed.
             */
            public static final int DB_OPERATION_FAILED = 552;

            /**
             * The numeric NACK error code returned by the server.
             */
            private final int errorCode;

            /**
             * The human-readable description carried by the NACK,
             * when one was provided.
             */
            private final String errorDescription;

            /**
             * Constructs a new server NACK exception.
             *
             * @param errorCode        the numeric NACK error code
             * @param errorDescription the human-readable description
             */
            public ServerNack(int errorCode, String errorDescription) {
                super("Server NACK: " + errorCode + " - " + errorDescription);
                this.errorCode = errorCode;
                this.errorDescription = errorDescription;
            }

            /**
             * Returns the numeric NACK error code returned by the server.
             *
             * @return the error code
             */
            public int errorCode() {
                return errorCode;
            }

            /**
             * Returns the human-readable description carried by the NACK.
             *
             * @return the description, or {@code null} when none was provided
             */
            public String errorDescription() {
                return errorDescription;
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation switches on the numeric code: only
             * {@link #STALE_GROUP_ADDRESSING_MODE},
             * {@link #SIGNAL_ERROR_OLD_COUNTER}, and the generic
             * {@link #UNHANDLED_ERROR} are retryable. Every other code
             * is a permanent rejection (parse failure, protobuf
             * invalid, monthly cap, message deleted, unsupported
             * stanza class, database error, and so on).
             */
            @Override
            public boolean isRetryable() {
                return switch (errorCode) {
                    case STALE_GROUP_ADDRESSING_MODE, SIGNAL_ERROR_OLD_COUNTER, UNHANDLED_ERROR -> true;
                    default -> false;
                };
            }
        }

        /**
         * Thrown when the recipient cannot be addressed by this device.
         *
         * @apiNote
         * Triggered when the JID is malformed, references an
         * unsupported entity kind, or the recipient has blocked the
         * sender.
         */
        public static final class InvalidRecipient extends Send {
            /**
             * The JID that was rejected.
             */
            private final Jid recipientJid;

            /**
             * The reason the recipient is invalid.
             */
            private final String reason;

            /**
             * Constructs a new invalid recipient exception.
             *
             * @param recipientJid the JID that was rejected
             * @param reason       the reason the recipient is invalid
             */
            public InvalidRecipient(Jid recipientJid, String reason) {
                super("Invalid recipient " + recipientJid + ": " + reason);
                this.recipientJid = recipientJid;
                this.reason = reason;
            }

            /**
             * Returns the JID that was rejected.
             *
             * @return the recipient JID
             */
            public Jid recipientJid() {
                return recipientJid;
            }

            /**
             * Returns the reason the recipient is invalid.
             *
             * @return the rejection reason
             */
            public String reason() {
                return reason;
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation always returns {@code false}: a
             * rejected recipient is a permanent failure for this send.
             */
            @Override
            public boolean isRetryable() {
                return false;
            }
        }

        /**
         * Thrown when the message payload exceeds the size limit the
         * server enforces.
         */
        public static final class PayloadTooLarge extends Send {
            /**
             * The actual payload size in bytes.
             */
            private final long actualSize;

            /**
             * The maximum payload size accepted by the server, in
             * bytes.
             */
            private final long maxSize;

            /**
             * Constructs a new payload too large exception.
             *
             * @param actualSize the actual payload size in bytes
             * @param maxSize    the maximum allowed payload size in bytes
             */
            public PayloadTooLarge(long actualSize, long maxSize) {
                super("Message payload too large: " + actualSize + " bytes (max: " + maxSize + ")");
                this.actualSize = actualSize;
                this.maxSize = maxSize;
            }

            /**
             * Returns the actual payload size in bytes.
             *
             * @return the actual size
             */
            public long actualSize() {
                return actualSize;
            }

            /**
             * Returns the maximum payload size accepted by the server.
             *
             * @return the maximum size in bytes
             */
            public long maxSize() {
                return maxSize;
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation always returns {@code false}: the
             * caller must trim the payload before any retry can
             * succeed.
             */
            @Override
            public boolean isRetryable() {
                return false;
            }
        }

        /**
         * Thrown when a queued message has expired before delivery.
         *
         * @apiNote
         * Typically applies to disappearing messages whose timer
         * elapsed while the message was queued offline.
         */
        public static final class MessageExpired extends Send {
            /**
             * Constructs a new message expired exception.
             */
            public MessageExpired() {
                super("Message expired before delivery");
            }

            /**
             * Constructs a new message expired exception with a message id.
             *
             * @param messageId the id of the expired message
             */
            public MessageExpired(String messageId) {
                super("Message " + messageId + " expired before delivery");
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation always returns {@code false}: an
             * expired message cannot be revived.
             */
            @Override
            public boolean isRetryable() {
                return false;
            }
        }

        /**
         * Thrown when the account has hit its monthly cap for new chat
         * messages.
         */
        public static final class MessageCapped extends Send {
            /**
             * Constructs a new message capped exception.
             */
            public MessageCapped() {
                super("Monthly new chat message limit reached");
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation always returns {@code false}: the cap
             * resets only at the start of the next billing window.
             */
            @Override
            public boolean isRetryable() {
                return false;
            }
        }

        /**
         * Thrown when the server does not acknowledge the send within
         * the expected time window.
         */
        public static final class Timeout extends Send {
            /**
             * Constructs a new timeout exception.
             */
            public Timeout() {
                super("Message send timed out");
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation always returns {@code true}: the
             * server may have lost the request and a retry can
             * succeed.
             */
            @Override
            public boolean isRetryable() {
                return true;
            }
        }

        /**
         * Thrown for send failures that do not match any of the more
         * specific subtypes.
         */
        public static final class Unknown extends Send {
            /**
             * Constructs a new unknown send exception.
             */
            public Unknown() {
                super("Unknown message send failure");
            }

            /**
             * Constructs a new unknown send exception with the specified message.
             *
             * @param message the detail message
             */
            public Unknown(String message) {
                super(message);
            }

            /**
             * Constructs a new unknown send exception with a cause.
             *
             * @param message the detail message
             * @param cause   the underlying cause
             */
            public Unknown(String message, Throwable cause) {
                super(message, cause);
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation always returns {@code false}: with
             * no diagnosis, blind retry would only resurface the same
             * failure.
             */
            @Override
            public boolean isRetryable() {
                return false;
            }
        }

        /**
         * Thrown when a send is attempted with a message id that is
         * already being processed by another in-flight send.
         */
        public static final class Duplicate extends Send {
            /**
             * The duplicated message id.
             */
            private final String messageId;

            /**
             * Constructs a new duplicate exception.
             *
             * @param messageId the duplicated message id
             */
            public Duplicate(String messageId) {
                super("Duplicate message send attempted: " + messageId);
                this.messageId = messageId;
            }

            /**
             * Returns the duplicated message id.
             *
             * @return the message id
             */
            public String messageId() {
                return messageId;
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation always returns {@code false}: the
             * in-flight send already owns the id.
             */
            @Override
            public boolean isRetryable() {
                return false;
            }
        }

        /**
         * Thrown when the sender is not authorized to publish a
         * message in the destination chat.
         *
         * @apiNote
         * The most common case is a non-admin trying to send to a
         * Community Announcement Group whose policy restricts sending
         * to admins.
         */
        public static final class Unauthorized extends Send {
            /**
             * Constructs a new unauthorized exception.
             *
             * @param message the detail message
             */
            public Unauthorized(String message) {
                super(message);
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation always returns {@code false}:
             * authorization changes are an out-of-band event the
             * caller cannot drive by retrying.
             */
            @Override
            public boolean isRetryable() {
                return false;
            }
        }
    }
}
