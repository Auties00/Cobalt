package com.github.auties00.cobalt.message.send.stanza;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryptedPayload;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds the {@code <participants>} stanza stanza for group SKMSG fanout and its content-binding-only variant.
 * <p>
 * Composed by {@link GroupSkmsgFanoutStanza} and {@link ChatFanoutStanza}. Two emission shapes are produced: a
 * sender-key distribution wrapper ({@link #buildSenderKeyDistribution(List, Map, String)}) carrying one
 * {@code <to><enc>} per new SK recipient, and a content-binding-only wrapper
 * ({@link #buildContentBindingOnly(List, Map)}) carrying RCAT tags for devices that already have the sender key.
 * {@link #requiresIdentityNode(List)} is the pre-key probe used to decide whether the outer stanza must include a
 * {@code <device-identity>} child.
 */
@WhatsAppWebModule(moduleName = "WAWebSendMsgCreateFanoutStanza")
@WhatsAppWebModule(moduleName = "WAWebSendGroupSkmsgJob")
public final class ParticipantsStanza {
    /**
     * The logger for {@link ParticipantsStanza}.
     */
    private static final System.Logger LOGGER = Log.get(ParticipantsStanza.class);

    /**
     * Prevents instantiation; this is a static composer.
     */
    private ParticipantsStanza() {
        throw new AssertionError();
    }

    /**
     * Builds a {@code <participants>} stanza for sender-key distribution.
     * <p>
     * Each device payload becomes a {@code <to jid="..."><enc decrypt-fail="hide" .../><content_binding .../></to>}
     * child. Distribution {@code <enc>}s carry {@code decrypt-fail="hide"} unless the caller passes an explicit
     * override, ensuring SK distribution messages never produce a visible decrypt-fail placeholder. Payloads with a
     * {@code null} {@link MessageEncryptedPayload#recipientJid()} are skipped (those represent the sender-key cipher).
     *
     * @param payloads        the per-device encrypted SK distribution payloads
     * @param contentBindings per-recipient RCAT tags keyed by user {@link Jid}, or {@code null} when RCAT does not apply
     * @param decryptFail     the explicit {@code decrypt-fail} attribute for the distribution {@code <enc>} nodes, or
     *                        {@code null} to use the default {@code "hide"}
     * @return the {@code <participants>} {@link Stanza}, or {@code null} when {@code payloads} is empty
     */
    @WhatsAppWebExport(moduleName = "WAWebSendGroupSkmsgJob", exports = "encryptAndSendSenderKeyMsg",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static Stanza buildSenderKeyDistribution(
            List<MessageEncryptedPayload> payloads,
            Map<Jid, byte[]> contentBindings,
            String decryptFail
    ) {
        if (payloads == null || payloads.isEmpty()) {
            return null;
        }

        var children = new ArrayList<Stanza>(payloads.size());
        for (var payload : payloads) {
            if (payload.recipientJid() == null) {
                continue;
            }

            var encNode = new StanzaBuilder()
                    .description("enc")
                    .attribute("v", String.valueOf(MessageEncryption.CIPHERTEXT_VERSION))
                    .attribute("type", payload.type().protocolValue())
                    .attribute("decrypt-fail", decryptFail != null ? decryptFail : "hide")
                    .content(payload.ciphertext())
                    .build();

            var contentBindingNode = resolveContentBinding(
                    payload.recipientJid(), contentBindings);

            var toNode = new StanzaBuilder()
                    .description("to")
                    .attribute("jid", payload.recipientJid())
                    .content(encNode, contentBindingNode)
                    .build();
            children.add(toNode);
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sk distribution participants built count={0}", children.size());
        return new StanzaBuilder()
                .description("participants")
                .content(children)
                .build();
    }

    /**
     * Builds a {@code <participants>} stanza carrying only content-binding tags for existing SK recipients.
     * <p>
     * Used when no sender-key distribution is needed but RCAT tags must still be delivered to existing SK recipients
     * (e.g. for an edit on an established group). Returns {@code null} when no binding matches any of the supplied device
     * JIDs, so the caller can suppress the empty wrapper.
     *
     * @implNote This implementation skips device JIDs whose corresponding user JID is not present in
     * {@code contentBindings}; only devices with a matching binding produce a {@code <to>} child.
     *
     * @param devices         the existing SK device {@link Jid}s
     * @param contentBindings per-recipient RCAT tags keyed by user {@link Jid}
     * @return the {@code <participants>} {@link Stanza}, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendGroupSkmsgJob", exports = "encryptAndSendSenderKeyMsg",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static Stanza buildContentBindingOnly(
            List<Jid> devices,
            Map<Jid, byte[]> contentBindings
    ) {
        if (contentBindings == null || contentBindings.isEmpty() || devices == null) {
            return null;
        }

        var children = new ArrayList<Stanza>();
        for (var device : devices) {
            var bindingNode = resolveContentBinding(device, contentBindings);
            if (bindingNode == null) {
                continue;
            }
            var toNode = new StanzaBuilder()
                    .description("to")
                    .attribute("jid", device)
                    .content(bindingNode)
                    .build();
            children.add(toNode);
        }

        if (children.isEmpty()) {
            return null;
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "content binding participants built count={0}", children.size());
        return new StanzaBuilder()
                .description("participants")
                .content(children)
                .build();
    }

    /**
     * Returns whether any payload is a pre-key message, signalling that the outer stanza must include a
     * {@code <device-identity>} child.
     * <p>
     * A PKMSG payload triggers the identity stanza because the recipient needs the sender's ADV identity to decrypt the
     * PreKey envelope.
     *
     * @param payloads the encrypted payloads, possibly {@code null}
     * @return {@code true} when at least one payload is a PreKey message
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCreateFanoutStanza", exports = "createFanoutMsgStanza",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static boolean requiresIdentityNode(List<MessageEncryptedPayload> payloads) {
        return payloads != null
                && payloads.stream()
                        .anyMatch(MessageEncryptedPayload::isPreKeyMessage);
    }

    /**
     * Resolves the {@code <content_binding>} child for a device by looking up the binding keyed on the device's user JID.
     * <p>
     * Returns {@code null} when the RCAT map is {@code null} or does not contain a binding for the device's user.
     *
     * @implNote This implementation hashes on {@link Jid#toUserJid()} rather than the device JID so that all of a user's
     * devices share the same RCAT tag.
     *
     * @param deviceJid       the device {@link Jid}
     * @param contentBindings per-recipient RCAT tags keyed by user {@link Jid}, or {@code null}
     * @return the {@code <content_binding>} {@link Stanza}, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendGroupSkmsgJob", exports = "encryptAndSendSenderKeyMsg",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static Stanza resolveContentBinding(Jid deviceJid, Map<Jid, byte[]> contentBindings) {
        if (contentBindings == null) {
            return null;
        }

        var userJid = deviceJid.toUserJid();
        var binding = contentBindings.get(userJid);
        if (binding == null) {
            return null;
        }

        return new StanzaBuilder()
                .description("content_binding")
                .content(binding)
                .build();
    }
}
