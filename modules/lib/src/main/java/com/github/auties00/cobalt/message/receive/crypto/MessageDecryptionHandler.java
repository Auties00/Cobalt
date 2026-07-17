package com.github.auties00.cobalt.message.receive.crypto;

import com.github.auties00.cobalt.exception.linked.WhatsAppMessageException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.message.receive.stanza.MessageReceiveEncryptedPayload;
import com.github.auties00.cobalt.message.receive.stanza.MessageReceiveStanza;
import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.E2eMessageRecvEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.AddressingMode;
import com.github.auties00.cobalt.wire.wam.type.E2eCiphertextType;
import com.github.auties00.cobalt.wire.wam.type.E2eDestination;
import com.github.auties00.cobalt.wire.wam.type.E2eDeviceType;
import com.github.auties00.cobalt.wire.wam.type.E2eFailureReason;
import com.github.auties00.cobalt.wire.wam.type.EncryptionTypeCode;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.SessionScopeType;
import com.github.auties00.cobalt.wire.wam.type.StanzaType;
import com.github.auties00.cobalt.wire.wam.type.TypeOfGroupEnum;

import java.lang.System.Logger.Level;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Tracks the decryption outcome of every {@code <enc>} child in a single message
 * stanza and produces the aggregated {@link MessageDecryptionResult} for receipt
 * selection.
 *
 * <p>One handler is constructed per incoming message stanza by
 * {@code ChatMessageReceiver}. Each enc is
 * offered first to {@link #canDecryptNext(MessageReceiveEncryptedPayload)} and, on
 * success, the receiver invokes the appropriate cipher and reports any failure back via
 * {@link #handleError(MessageReceiveEncryptedPayload, WhatsAppMessageException.Receive)}.
 * A stanza carries at most two {@code <enc>} children: a sender-key payload
 * ({@link MessageEncryptionType#SKMSG}) and a per-device Signal payload
 * ({@link MessageEncryptionType#PKMSG} or {@link MessageEncryptionType#MSG}). Each slot
 * tracks its own failure; the non-{@link MessageEncryptionType#SKMSG} slot
 * short-circuits the {@link MessageEncryptionType#SKMSG} attempt when it fails with a
 * retryable error.
 *
 * <p>Once the receiver has finished iterating the stanza's encs it calls
 * {@link #commitReceiveMetric(MessageReceiveStanza, boolean)} exactly once, which folds
 * the recorded slot state together with the stanza addressing context into an
 * {@code E2eMessageRecv} telemetry event and hands it to {@link WamService}. The metric
 * is emitted on both the success and the all-failed path so the aggregate
 * decrypt-success rate is observable.
 *
 * @implNote
 * This implementation mirrors WhatsApp Web's
 * {@code WAWebMsgProcessingDecryptionHandler.createDecryptionHandler} closure but lifts
 * the state into instance fields so the handler can be reused in tests. The
 * short-circuit blocker set matches the WhatsApp Web {@code SignalRetryable}-only
 * blocker set.
 */
@WhatsAppWebModule(moduleName = "WAWebMsgProcessingDecryptionHandler")
public final class MessageDecryptionHandler {
    /**
     * The logger for {@link MessageDecryptionHandler}.
     */
    private static final System.Logger LOGGER = Log.get(MessageDecryptionHandler.class);

    /**
     * Holds the Signal ciphertext wire version reported on the per-message
     * {@code E2eMessageRecv} metric.
     *
     * @implNote
     * This implementation reports the constant {@code 3}, the current Signal double
     * ratchet message version Cobalt encodes and decodes; Cobalt does not negotiate a
     * per-message ciphertext version, so there is no runtime value to read.
     */
    private static final long SIGNAL_CIPHERTEXT_VERSION = 3;

    /**
     * Holds the error types that block further decryption attempts once observed on the
     * non-{@link MessageEncryptionType#SKMSG} slot.
     *
     * @implNote
     * This implementation contains only {@link DecryptionErrorType#SIGNAL_RETRYABLE},
     * mirroring WhatsApp Web's local {@code b} set inside
     * {@code WAWebMsgProcessingDecryptionHandler}; other error types still allow the
     * {@link MessageEncryptionType#SKMSG} slot to be attempted.
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptionHandler", exports = "createDecryptionHandler",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final Set<DecryptionErrorType> RETRYABLE_BLOCKERS =
            EnumSet.of(DecryptionErrorType.SIGNAL_RETRYABLE);

    /**
     * Holds the encryption types whose enc nodes have been offered so far.
     *
     * <p>Lets {@link #getResult()} promote a {@link MessageEncryptionType#SKMSG} failure
     * to {@link MessageDecryptionResult#SUCCESS} when the
     * {@link MessageEncryptionType#SKMSG} slot was actually attempted and only the
     * per-device slot failed.
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptionHandler", exports = "createDecryptionHandler",
            adaptation = WhatsAppAdaptation.DIRECT)
    private final Set<MessageEncryptionType> accessedEncs =
            EnumSet.noneOf(MessageEncryptionType.class);

    /**
     * Holds the failure recorded for the per-device Signal slot
     * ({@link MessageEncryptionType#PKMSG} or {@link MessageEncryptionType#MSG}), or
     * {@code null} when that slot was not attempted or succeeded.
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptionHandler", exports = "createDecryptionHandler",
            adaptation = WhatsAppAdaptation.DIRECT)
    private EncFailure pkOrMsgFailure;

    /**
     * Holds the failure recorded for the sender-key slot
     * ({@link MessageEncryptionType#SKMSG}), or {@code null} when that slot was not
     * attempted or succeeded.
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptionHandler", exports = "createDecryptionHandler",
            adaptation = WhatsAppAdaptation.DIRECT)
    private EncFailure skMsgFailure;

    /**
     * Holds the telemetry sink the per-message {@code E2eMessageRecv} metric is committed
     * to once the stanza's encs have all been offered.
     */
    private final WamService wamService;

    /**
     * Constructs a handler bound to the telemetry sink that receives its per-message
     * decryption metric.
     *
     * <p>A fresh handler is created for every incoming message stanza; the injected
     * {@link WamService} is shared with the surrounding receive pipeline and is the same
     * sink used for the other receive-side metrics.
     *
     * @param wamService the telemetry sink the {@code E2eMessageRecv} metric is committed
     *                   to
     */
    public MessageDecryptionHandler(WamService wamService) {
        this.wamService = wamService;
    }

    /**
     * Returns whether the given encrypted payload should be attempted next and records
     * the attempt when it is.
     *
     * <p>Returning {@code false} aborts the remaining payloads after a retryable
     * per-device Signal failure so the stanza's overall result is settled by the first
     * blocker.
     *
     * @implNote
     * This implementation also records the offered enc type in {@link #accessedEncs} so
     * {@link #getResult()} can distinguish "{@link MessageEncryptionType#SKMSG}
     * attempted and succeeded" from "{@link MessageEncryptionType#SKMSG} never tried"
     * when the per-device slot also failed.
     *
     * @param enc the next encrypted payload to consider
     * @return {@code true} if decryption should be attempted; {@code false} when a prior
     *         retryable failure blocks further work
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptionHandler", exports = "createDecryptionHandler",
            adaptation = WhatsAppAdaptation.DIRECT)
    public boolean canDecryptNext(MessageReceiveEncryptedPayload enc) {
        if (pkOrMsgFailure != null && RETRYABLE_BLOCKERS.contains(pkOrMsgFailure.errorType)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "decryption of {0} blocked by prior retryable failure", enc.e2eType());
            return false;
        }

        accessedEncs.add(enc.e2eType());
        return true;
    }

    /**
     * Records a decryption failure for the given encrypted payload.
     *
     * <p>The resulting classification drives both the
     * {@link MessageEncryptionType#SKMSG} short-circuit and the final receipt
     * selection.
     *
     * @implNote
     * This implementation routes {@link MessageEncryptionType#SKMSG} failures into
     * {@link #skMsgFailure} and per-device failures into {@link #pkOrMsgFailure}; if
     * both fail the {@link MessageEncryptionType#SKMSG} slot is preferred as the
     * dominant failure in {@link #getResult()}.
     *
     * @param enc   the encrypted payload that failed
     * @param error the decryption exception
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptionHandler", exports = "createDecryptionHandler",
            adaptation = WhatsAppAdaptation.DIRECT)
    public void handleError(
            MessageReceiveEncryptedPayload enc,
            WhatsAppMessageException.Receive error
    ) {
        var errorType = classifyError(error);
        var failure = new EncFailure(enc, error, errorType);

        if (enc.e2eType().isSenderKeyMessage()) {
            skMsgFailure = failure;
        } else {
            pkOrMsgFailure = failure;
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "decryption error for enc type {0}: {1}", enc.e2eType(), errorType);
    }

    /**
     * Returns the aggregated decryption result computed from the recorded failures.
     *
     * <p>The returned {@link MessageDecryptionResult} drives the follow-up receipt
     * selection (delivery, retry, NACK, plain ack) inside the orchestrator.
     *
     * @implNote
     * This implementation mirrors WhatsApp Web's local {@code E} function: if no failure
     * was recorded the result is {@link MessageDecryptionResult#SUCCESS}; a
     * {@link MessageEncryptionType#SKMSG}-only failure on a stanza that also carried an
     * attempted {@link MessageEncryptionType#SKMSG} slot also resolves to
     * {@link MessageDecryptionResult#SUCCESS} (the WhatsApp Web {@code l}
     * short-circuit); otherwise the dominant failure
     * ({@link MessageEncryptionType#SKMSG} preferred over the per-device slot) is mapped
     * through {@link #mapErrorToResult(EncFailure)}.
     *
     * @return the aggregated decryption result
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptionHandler", exports = "createDecryptionHandler",
            adaptation = WhatsAppAdaptation.DIRECT)
    public MessageDecryptionResult getResult() {
        var dominant = skMsgFailure != null ? skMsgFailure : pkOrMsgFailure;

        if (dominant == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "decryption result {0}", MessageDecryptionResult.SUCCESS);
            return MessageDecryptionResult.SUCCESS;
        }

        var skMsgAccessed = accessedEncs.contains(MessageEncryptionType.SKMSG);
        if (skMsgAccessed && skMsgFailure == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "decryption result {0}", MessageDecryptionResult.SUCCESS);
            return MessageDecryptionResult.SUCCESS;
        }

        var result = mapErrorToResult(dominant);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "decryption result {0}", result);
        return result;
    }

    /**
     * Builds and commits the per-message {@code E2eMessageRecv} telemetry event for the
     * stanza this handler tracked.
     *
     * <p>Invoked once by the receiver after the enc-iteration loop settles, on both the
     * success and the all-failed path. The recorded slot state supplies the ciphertext
     * type and, on failure, the mapped failure reason, while {@code stanza} supplies the
     * addressing context (destination, sender device type, addressing modes, offline
     * flag, retry count, group type). Fields Cobalt does not compute at decrypt time
     * (post-quantum session flag, simple-Signal flag, processing-deferred flag) are
     * reported with their steady-state values because Cobalt neither negotiates a
     * post-quantum session nor defers orphan-bot processing.
     *
     * @implNote
     * This implementation derives {@code successful} from the caller rather than from
     * {@link #getResult()} so the no-enc stanza (which
     * {@link #getResult()} reports as {@link MessageDecryptionResult#SUCCESS} because no
     * failure was recorded) is still classified as a failure when no plaintext was
     * produced, matching WhatsApp Web's split between {@code postSuccessE2eMessageRecvMetric}
     * and {@code postFailureE2eMessageRecvMetric}.
     *
     * @param stanza     the stanza whose encs were offered to this handler
     * @param successful whether a plaintext was ultimately recovered from any enc
     */
    @WhatsAppWebExport(moduleName = "WAWebPostE2eMessageRecvMetric",
            exports = "postSuccessE2eMessageRecvMetric", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebPostE2eMessageRecvMetric",
            exports = "postFailureE2eMessageRecvMetric", adaptation = WhatsAppAdaptation.ADAPTED)
    public void commitReceiveMetric(MessageReceiveStanza stanza, boolean successful) {
        var chatJid = stanza.chatJid();
        var senderJid = stanza.senderJid();
        var addressingMode = resolveAddressingMode(stanza);
        var builder = new E2eMessageRecvEventBuilder()
                .e2eSuccessful(successful)
                .e2eCiphertextType(resolveCiphertextType())
                .e2eCiphertextVersion(SIGNAL_CIPHERTEXT_VERSION)
                .e2eDestination(resolveDestination(chatJid))
                .e2eSenderType(resolveSenderType(senderJid))
                .encryptionType(resolveEncryptionType())
                .messageAddressingMode(addressingMode)
                .serverAddressingMode(addressingMode)
                .localAddressingMode(AddressingMode.LID)
                .isLid(isLidAddressed(stanza))
                .isHostedChat(chatJid.hasHostedServer() || chatJid.hasHostedLidServer())
                .isPq(false)
                .isSimpleSignal(false)
                .offline(stanza.isOffline())
                .processingDeferred(false)
                .retryCount(stanza.retryCount().orElse(0))
                .sessionScope(chatJid.isStatusBroadcastAccount()
                        ? SessionScopeType.STATUS
                        : SessionScopeType.DEFAULT)
                .stanzaType(StanzaType.MESSAGE);

        if (chatJid.hasGroupOrCommunityServer()) {
            builder.typeOfGroup(TypeOfGroupEnum.GROUP);
        }

        resolveMediaType(stanza).ifPresent(builder::messageMediaType);

        if (!successful) {
            var failureReason = resolveFailureReason();
            if (Log.WARNING) LOGGER.log(Level.WARNING, "decryption failed for chat {0} sender {1} reason {2}", chatJid, senderJid, failureReason);
            builder.e2eFailureReason(failureReason);
        } else if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "decryption succeeded for chat {0} sender {1}", chatJid, senderJid);
        }

        wamService.commit(builder.build());
    }

    /**
     * Returns the representative Signal ciphertext type for the metric, chosen from the
     * enc types this handler was offered.
     *
     * <p>Prefers the sender-key group envelope over the per-device envelopes so a group
     * stanza carrying both a sender-key and a retry envelope reports the group envelope;
     * returns {@code null} when no enc was offered.
     *
     * @return the ciphertext type, or {@code null} when the stanza carried no enc
     */
    private E2eCiphertextType resolveCiphertextType() {
        if (accessedEncs.contains(MessageEncryptionType.SKMSG)) {
            return E2eCiphertextType.SENDER_KEY_MESSAGE;
        }
        if (accessedEncs.contains(MessageEncryptionType.PKMSG)) {
            return E2eCiphertextType.PREKEY_MESSAGE;
        }
        if (accessedEncs.contains(MessageEncryptionType.MSMSG)) {
            return E2eCiphertextType.MESSAGE_SECRET_MESSAGE;
        }
        if (accessedEncs.contains(MessageEncryptionType.MSG)) {
            return E2eCiphertextType.MESSAGE;
        }
        return null;
    }

    /**
     * Returns the encryption scheme code for the metric.
     *
     * <p>Reports {@link EncryptionTypeCode#BOT} when a bot multi-side envelope
     * ({@link MessageEncryptionType#MSMSG}) was offered and standard end-to-end
     * encryption otherwise.
     *
     * @return the encryption type code
     */
    private EncryptionTypeCode resolveEncryptionType() {
        return accessedEncs.contains(MessageEncryptionType.MSMSG)
                ? EncryptionTypeCode.BOT
                : EncryptionTypeCode.E2EE;
    }

    /**
     * Maps the dominant recorded failure onto the metric's failure reason.
     *
     * <p>Reads the same dominant-slot preference as {@link #getResult()}
     * ({@link MessageEncryptionType#SKMSG} over the per-device slot) and collapses the
     * classified {@link DecryptionErrorType} onto the closest
     * {@link E2eFailureReason}; falls back to {@link E2eFailureReason#ERROR_UNKNOWN}
     * when the failure path was reached without a recorded slot failure.
     *
     * @return the mapped failure reason
     */
    private E2eFailureReason resolveFailureReason() {
        var dominant = skMsgFailure != null ? skMsgFailure : pkOrMsgFailure;
        if (dominant == null) {
            return E2eFailureReason.ERROR_UNKNOWN;
        }
        return switch (dominant.errorType) {
            case SIGNAL_RETRYABLE -> E2eFailureReason.NO_SESSION_AVAILABLE;
            case SIGNAL_DUPLICATE_MESSAGE -> E2eFailureReason.DUPLICATE_MESSAGE;
            case UNKNOWN_DEVICE -> E2eFailureReason.NOT_IN_PENDING_DEVICES;
            case DEVICE_SENT_MESSAGE -> E2eFailureReason.INVALID_DSM;
            case INVALID_PROTOBUF -> E2eFailureReason.INVALID_PROTOCOL_BUFFER;
            case HSM_MISMATCH -> E2eFailureReason.INVALID_HSM_ELEMENT;
            case BROADCAST_EPH_SETTINGS -> E2eFailureReason.INVALID_BROADCAST_STANZA_ATTRIBUTE;
            case UNKNOWN -> E2eFailureReason.DECRYPTION_FAILED;
        };
    }

    /**
     * Returns the metric's destination classification for the stanza's chat JID.
     *
     * <p>Distinguishes the status feed, groups and communities, channels, broadcast
     * lists, and interop bridges; falls back to a one-to-one individual chat.
     *
     * @param chatJid the chat JID the message arrived in
     * @return the destination classification
     */
    private static E2eDestination resolveDestination(Jid chatJid) {
        if (chatJid.isStatusBroadcastAccount()) {
            return E2eDestination.STATUS;
        }
        if (chatJid.hasGroupOrCommunityServer()) {
            return E2eDestination.GROUP;
        }
        if (chatJid.hasNewsletterServer()) {
            return E2eDestination.CHANNEL;
        }
        if (chatJid.hasBroadcastServer()) {
            return E2eDestination.LIST;
        }
        if (chatJid.hasInteropServer()) {
            return E2eDestination.INTEROP;
        }
        return E2eDestination.INDIVIDUAL;
    }

    /**
     * Returns the metric's sender device classification for the stanza's sender JID.
     *
     * <p>Inbound messages are attributed to the peer account; the primary/companion
     * split follows the sender JID device component and the hosted classification
     * follows a hosted server. The metric therefore never reports one of the
     * {@code MY_*} device types on the receive path.
     *
     * @param senderJid the sender JID of the message
     * @return the sender device classification
     */
    private static E2eDeviceType resolveSenderType(Jid senderJid) {
        if (senderJid.hasHostedServer() || senderJid.hasHostedLidServer()) {
            return E2eDeviceType.OTHER_HOSTED_COMPANION;
        }
        return senderJid.isPrimaryDevice()
                ? E2eDeviceType.OTHER_PRIMARY
                : E2eDeviceType.OTHER_COMPANION;
    }

    /**
     * Returns the addressing mode carried on the stanza's {@code addressing_mode}
     * attribute.
     *
     * <p>Maps the {@code "lid"} wire value to {@link AddressingMode#LID} and every other
     * value, including an absent attribute, to {@link AddressingMode#PN}.
     *
     * @param stanza the stanza whose addressing mode is read
     * @return the addressing mode
     */
    private static AddressingMode resolveAddressingMode(MessageReceiveStanza stanza) {
        return stanza.addressingMode()
                .filter("lid"::equalsIgnoreCase)
                .map(_ -> AddressingMode.LID)
                .orElse(AddressingMode.PN);
    }

    /**
     * Returns whether the stanza is LID-addressed for the metric's {@code isLid} flag.
     *
     * <p>Treats the message as LID-addressed when the stanza's addressing mode is
     * {@link AddressingMode#LID} or when either the sender or chat JID sits on a LID
     * server.
     *
     * @param stanza the stanza to classify
     * @return {@code true} when the message is LID-addressed
     */
    private static boolean isLidAddressed(MessageReceiveStanza stanza) {
        return resolveAddressingMode(stanza) == AddressingMode.LID
                || stanza.senderJid().hasLidServer()
                || stanza.chatJid().hasLidServer();
    }

    /**
     * Returns the media class of the first enc that carried a {@code mediatype} tag, for
     * the metric's {@code messageMediaType} field.
     *
     * <p>Returns {@link Optional#empty()} when no enc carried a media-type tag, in which
     * case the field is left unset rather than reported as {@link MediaType#NONE}.
     *
     * @param stanza the stanza whose encs are scanned
     * @return an {@link Optional} wrapping the mapped media type
     */
    private static Optional<MediaType> resolveMediaType(MessageReceiveStanza stanza) {
        return stanza.encs()
                .stream()
                .map(MessageReceiveEncryptedPayload::encMediaType)
                .flatMap(Optional::stream)
                .findFirst()
                .map(MessageDecryptionHandler::mapMediaType);
    }

    /**
     * Maps an {@code <enc mediatype>} wire tag onto the metric's {@link MediaType}.
     *
     * <p>Recognises the common media tags carried on encrypted payloads and folds every
     * unrecognised tag onto {@link MediaType#NONE}.
     *
     * @param mediaType the {@code mediatype} attribute value
     * @return the corresponding media type
     */
    private static MediaType mapMediaType(String mediaType) {
        return switch (mediaType) {
            case "image" -> MediaType.PHOTO;
            case "video" -> MediaType.VIDEO;
            case "audio" -> MediaType.AUDIO;
            case "ptt" -> MediaType.PTT;
            case "document" -> MediaType.DOCUMENT;
            case "sticker" -> MediaType.STICKER;
            case "gif" -> MediaType.GIF;
            case "url" -> MediaType.URL;
            case "contact" -> MediaType.CONTACT;
            case "GroupHistory" -> MediaType.GROUP_HISTORY;
            default -> MediaType.NONE;
        };
    }

    /**
     * Returns the failed encrypted payload that the receipt path references when
     * surfacing the failure to the server.
     *
     * <p>Returns {@link Optional#empty()} when no failure was recorded.
     *
     * @return an {@link Optional} wrapping the failed enc payload
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptionHandler", exports = "createDecryptionHandler",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<MessageReceiveEncryptedPayload> failedEnc() {
        var dominant = skMsgFailure != null ? skMsgFailure : pkOrMsgFailure;
        return dominant != null ? Optional.of(dominant.enc) : Optional.empty();
    }

    /**
     * Returns the dominant failure exception so the caller can rethrow it when no enc
     * could be decrypted.
     *
     * <p>Consumed by {@code ChatMessageReceiver}
     * after the enc-iteration loop completes without a successful payload, so the
     * original Signal exception (rather than a synthesised one) bubbles up.
     *
     * @return an {@link Optional} wrapping the dominant exception
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptionHandler", exports = "createDecryptionHandler",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<WhatsAppMessageException.Receive> failedError() {
        var dominant = skMsgFailure != null ? skMsgFailure : pkOrMsgFailure;
        return dominant != null ? Optional.of(dominant.error) : Optional.empty();
    }

    /**
     * Classifies a decryption exception into a {@link DecryptionErrorType} for
     * downstream slot tracking and result mapping.
     *
     * @implNote
     * This implementation switches on the sealed {@link WhatsAppMessageException.Receive}
     * hierarchy and collapses every Signal-protocol error subtype
     * ({@link WhatsAppMessageException.Receive.NoSession},
     * {@link WhatsAppMessageException.Receive.BadMac},
     * {@link WhatsAppMessageException.Receive.InvalidKey}, and others) into
     * {@link DecryptionErrorType#SIGNAL_RETRYABLE}, because WhatsApp Web's local
     * {@code S} classifier also folds them into its {@code SignalRetryable} bucket via
     * the shared {@code SignalDecryptionError} class.
     *
     * @param error the exception to classify
     * @return the corresponding error type
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptionHandler", exports = "createDecryptionHandler",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static DecryptionErrorType classifyError(WhatsAppMessageException.Receive error) {
        return switch (error) {
            case WhatsAppMessageException.Receive.UnknownDevice _ ->
                    DecryptionErrorType.UNKNOWN_DEVICE;
            case WhatsAppMessageException.Receive.DuplicateMessage _ ->
                    DecryptionErrorType.SIGNAL_DUPLICATE_MESSAGE;
            case WhatsAppMessageException.Receive.InvalidDeviceSentMessage _ ->
                    DecryptionErrorType.DEVICE_SENT_MESSAGE;
            case WhatsAppMessageException.Receive.InvalidProtobuf _ ->
                    DecryptionErrorType.INVALID_PROTOBUF;
            case WhatsAppMessageException.Receive.HsmMismatch _ ->
                    DecryptionErrorType.HSM_MISMATCH;
            case WhatsAppMessageException.Receive.BroadcastEphemeralSettings _ ->
                    DecryptionErrorType.BROADCAST_EPH_SETTINGS;
            case WhatsAppMessageException.Receive.NoSession _,
                 WhatsAppMessageException.Receive.InvalidKey _,
                 WhatsAppMessageException.Receive.InvalidKeyId _,
                 WhatsAppMessageException.Receive.InvalidOneTimeKey _,
                 WhatsAppMessageException.Receive.InvalidSignedPreKey _,
                 WhatsAppMessageException.Receive.InvalidMessage _,
                 WhatsAppMessageException.Receive.InvalidSignature _,
                 WhatsAppMessageException.Receive.FutureMessage _,
                 WhatsAppMessageException.Receive.BadMac _,
                 WhatsAppMessageException.Receive.NoSenderKey _,
                 WhatsAppMessageException.Receive.InvalidSenderKey _,
                 WhatsAppMessageException.Receive.AdvFailure _ ->
                    DecryptionErrorType.SIGNAL_RETRYABLE;
            default -> DecryptionErrorType.UNKNOWN;
        };
    }

    /**
     * Maps the dominant failure's error type onto the corresponding
     * {@link MessageDecryptionResult} that drives receipt selection.
     *
     * @implNote
     * This implementation does not surface WhatsApp Web's {@code DEFERRED} variant (used
     * for orphan bot messages); the omission is intentional because Cobalt does not
     * model the orphan-bot-message buffer.
     *
     * @param failure the dominant failure to map
     * @return the corresponding decryption result
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptionHandler", exports = "createDecryptionHandler",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static MessageDecryptionResult mapErrorToResult(EncFailure failure) {
        return switch (failure.errorType) {
            case SIGNAL_RETRYABLE, UNKNOWN_DEVICE, BROADCAST_EPH_SETTINGS ->
                    MessageDecryptionResult.RETRY;
            case SIGNAL_DUPLICATE_MESSAGE ->
                    MessageDecryptionResult.SIGNAL_OLD_COUNTER_ERROR;
            case DEVICE_SENT_MESSAGE, INVALID_PROTOBUF ->
                    MessageDecryptionResult.PARSE_VALIDATION_ERROR;
            case HSM_MISMATCH ->
                    MessageDecryptionResult.HSM_MISMATCH;
            case UNKNOWN ->
                    MessageDecryptionResult.PARSE_ERROR;
        };
    }

    /**
     * Classifies decryption failures so the handler can drive the
     * {@link MessageEncryptionType#SKMSG}-blocker short-circuit and the
     * {@link MessageDecryptionResult} mapping.
     *
     * <p>This type is not exposed outside the package;
     * {@link MessageDecryptionHandler#classifyError} is the only producer and
     * {@link MessageDecryptionHandler#mapErrorToResult} the only consumer.
     */
    @WhatsAppWebModule(moduleName = "WAWebMsgProcessingDecryptionHandler")
    private enum DecryptionErrorType {
        /**
         * Tags Signal-protocol errors that the sender can resolve by re-encrypting and
         * re-sending the payload.
         *
         * <p>Covers {@link WhatsAppMessageException.Receive.NoSession},
         * {@link WhatsAppMessageException.Receive.InvalidKey},
         * {@link WhatsAppMessageException.Receive.InvalidMessage},
         * {@link WhatsAppMessageException.Receive.BadMac},
         * {@link WhatsAppMessageException.Receive.NoSenderKey}, and similar; resolves to
         * {@link MessageDecryptionResult#RETRY}.
         */
        SIGNAL_RETRYABLE,

        /**
         * Tags a Signal-protocol duplicate or already-seen counter.
         *
         * <p>Resolves to {@link MessageDecryptionResult#SIGNAL_OLD_COUNTER_ERROR}; the
         * orchestrator either reuses the cached delivery outcome or treats the message as
         * a normal delivery based on the dedup gate.
         */
        SIGNAL_DUPLICATE_MESSAGE,

        /**
         * Tags a message that came from a device not present in the local device list.
         *
         * <p>Resolves to {@link MessageDecryptionResult#RETRY}; WhatsApp Web also
         * triggers a device-list sync, which Cobalt drives separately from the sync
         * handlers.
         */
        UNKNOWN_DEVICE,

        /**
         * Tags a {@code DeviceSentMessage} envelope that was missing, invalid, or present
         * when it should not be.
         *
         * <p>Resolves to {@link MessageDecryptionResult#PARSE_VALIDATION_ERROR}.
         */
        DEVICE_SENT_MESSAGE,

        /**
         * Tags a decrypted protobuf that failed structural validation (multiple message
         * keys, type mismatch, and similar).
         *
         * <p>Resolves to {@link MessageDecryptionResult#PARSE_VALIDATION_ERROR}.
         */
        INVALID_PROTOBUF,

        /**
         * Tags a stanza that marked the message as HSM but whose protobuf did not match,
         * or vice versa.
         *
         * <p>Resolves to {@link MessageDecryptionResult#HSM_MISMATCH}.
         */
        HSM_MISMATCH,

        /**
         * Tags a failure to decode broadcast ephemeral settings from the shared secret.
         *
         * <p>Resolves to {@link MessageDecryptionResult#RETRY}; WhatsApp Web additionally
         * tags the retry receipt with the {@code INVALID_BROADCAST_STANZA_ATTRIBUTE}
         * failure reason, which Cobalt does not currently surface.
         */
        BROADCAST_EPH_SETTINGS,

        /**
         * Tags an unclassified error that does not match any known category.
         *
         * <p>Resolves to {@link MessageDecryptionResult#PARSE_ERROR}.
         */
        UNKNOWN
    }

    /**
     * Bundles a single slot's failure: the payload, the exception, and the classified
     * error type.
     *
     * <p>Stored in {@link MessageDecryptionHandler#pkOrMsgFailure} or
     * {@link MessageDecryptionHandler#skMsgFailure} so
     * {@link MessageDecryptionHandler#getResult()} can both pick the dominant failure
     * and surface the original exception via {@link MessageDecryptionHandler#failedError()}.
     */
    @WhatsAppWebModule(moduleName = "WAWebMsgProcessingDecryptionHandler")
    private static final class EncFailure {
        /**
         * Holds the encrypted payload whose decryption failed.
         */
        @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptionHandler", exports = "createDecryptionHandler",
                adaptation = WhatsAppAdaptation.DIRECT)
        private final MessageReceiveEncryptedPayload enc;

        /**
         * Holds the exception that caused the failure.
         */
        @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptionHandler", exports = "createDecryptionHandler",
                adaptation = WhatsAppAdaptation.DIRECT)
        private final WhatsAppMessageException.Receive error;

        /**
         * Holds the classified error type produced by
         * {@link MessageDecryptionHandler#classifyError}.
         */
        @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptionHandler", exports = "createDecryptionHandler",
                adaptation = WhatsAppAdaptation.DIRECT)
        private final DecryptionErrorType errorType;

        /**
         * Constructs a failure record from its three components.
         *
         * @param enc       the encrypted payload that failed
         * @param error     the exception that caused the failure
         * @param errorType the classified error type
         */
        @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptionHandler", exports = "createDecryptionHandler",
                adaptation = WhatsAppAdaptation.DIRECT)
        private EncFailure(
                MessageReceiveEncryptedPayload enc,
                WhatsAppMessageException.Receive error,
                DecryptionErrorType errorType
        ) {
            this.enc = enc;
            this.error = error;
            this.errorType = errorType;
        }
    }
}
