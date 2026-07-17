package com.github.auties00.cobalt.message.dedup;

import com.github.auties00.cobalt.message.receive.stanza.MessageReceiveEncryptedPayload;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.message.MessageKey;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Derives the composite dedup string {@link MessageDedup} uses to identify an
 * in-flight inbound message.
 *
 * <p>The key is deterministic in the message key, the message timestamp, and
 * the list of encrypted payloads carried on the incoming stanza. Two different
 * ciphertexts of the same logical message id (for example a {@code pkmsg}
 * replayed as a plain {@code msg} on retry) therefore produce different dedup
 * strings and do not collide.
 *
 * @implNote This implementation serialises the message key through
 * {@link #serializeKey(MessageKey)} and joins it with the timestamp's
 * Unix-seconds value and one {@code e2eType:retryCount} segment per encrypted
 * payload, using underscore and comma separators.
 */
@WhatsAppWebModule(moduleName = "WAWebPendingMessageKey")
public final class PendingMessageKey {
    /**
     * Prevents instantiation of this static helper.
     *
     * @throws AssertionError always
     */
    private PendingMessageKey() {
        throw new AssertionError("PendingMessageKey is a static helper and cannot be instantiated");
    }

    /**
     * Derives the composite dedup string for an incoming message.
     *
     * <p>The returned string has the shape
     * {@snippet :
     *     // <fromMe>_<remote>_<id>[_<participant>]_<unixSeconds>_<e2eType1>:<retry1>,<e2eType2>:<retry2>
     *     // example:
     *     //   false_12025550100@s.whatsapp.net_3EB0ABCD_1700000000_pkmsg:0,msg:1
     * }
     * where the {@code <fromMe>_<remote>_<id>[_<participant>]} prefix comes from
     * {@link #serializeKey(MessageKey)}, {@code unixSeconds} is the timestamp's
     * Unix-seconds value, and every {@link MessageReceiveEncryptedPayload}
     * contributes one {@code e2eType:retryCount} segment in iteration order.
     *
     * @implNote This implementation concatenates the encs in input order rather
     * than sorted, and an empty encs list yields a trailing underscore with no
     * per-enc segment.
     *
     * @param key       the logical message key
     * @param timestamp the message timestamp, used as Unix-seconds
     * @param encs      the encrypted payloads on the incoming stanza; may be
     *                  empty but must not be {@code null}
     * @return the composite dedup string, never {@code null}
     * @throws NullPointerException if {@code key}, {@code timestamp}, or
     *                              {@code encs} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebPendingMessageKey", exports = "createPendingMessageKey",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static String create(MessageKey key, Instant timestamp, List<MessageReceiveEncryptedPayload> encs) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        Objects.requireNonNull(encs, "encs cannot be null");

        var encsJoiner = new StringJoiner(",");
        for (var enc : encs) {
            encsJoiner.add(enc.e2eType().protocolValue() + ":" + enc.retryCount());
        }
        var encsSegment = encsJoiner.toString();

        return serializeKey(key) + "_" + timestamp.getEpochSecond() + "_" + encsSegment;
    }

    /**
     * Serialises a {@link MessageKey} into its underscore-joined string form.
     *
     * <p>Emits {@code fromMe_remote_id} for one-to-one keys and
     * {@code fromMe_remote_id_participant} for group keys; the participant
     * segment is suppressed when the sender JID equals the parent JID.
     *
     * @implNote This implementation omits the {@code selfDir} ("in" / "out")
     * field WA Web appends when a one-to-one conversation with the primary
     * device is detected; Cobalt does not track {@code selfDir} on the key, and
     * including it would never line up with the inbound replay loop that
     * consumes the dedup string.
     *
     * @param key the message key to serialise
     * @return the underscore-joined string form
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgKey", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static String serializeKey(MessageKey key) {
        var base = key.fromMe() + "_"
                + key.parentJid().map(Object::toString).orElse("") + "_"
                + key.id().orElse("");
        return key.senderJid()
                .filter(sender -> !key.parentJid().map(parent -> parent.equals(sender)).orElse(false))
                .map(sender -> base + "_" + sender)
                .orElse(base);
    }
}
