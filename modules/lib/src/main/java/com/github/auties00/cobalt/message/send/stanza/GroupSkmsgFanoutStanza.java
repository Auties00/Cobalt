package com.github.auties00.cobalt.message.send.stanza;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Builds the outer {@code <message>} stanza for group sender-key (SKMSG) fanout.
 * <p>
 * Used for every group send except per-device group-direct fanout (see {@link ChatFanoutStanza}). In SKMSG mode the
 * body is encrypted once with the sender key and shipped as a single {@code <enc type="skmsg">}; new group members and
 * members whose sender key has not yet propagated receive their copies via a pre-built {@code <participants>} child
 * carrying the per-device SKMSG distribution messages (built by
 * {@link ParticipantsStanza#buildSenderKeyDistribution(java.util.List, java.util.Map, String)}). Auxiliary children
 * ({@code <biz>}, {@code <meta>}, {@code <bot>}, {@code <reporting>}, {@code <sender_content_binding>}) are composed
 * exactly like {@link ChatFanoutStanza}.
 *
 * @implNote This implementation drops the {@code <enc>} child entirely when {@code skmsgCiphertext} is {@code null};
 * that branch is taken for bot-feedback sends where the {@code <bot>} child carries the only encrypted body and the
 * parent stanza's outer {@code phash} is also dropped.
 */
@WhatsAppWebModule(moduleName = "WAWebSendGroupSkmsgJob")
public final class GroupSkmsgFanoutStanza {
    /**
     * The logger for {@link GroupSkmsgFanoutStanza}.
     */
    private static final System.Logger LOGGER = Log.get(GroupSkmsgFanoutStanza.class);

    /**
     * Prevents instantiation; this is a static composer.
     */
    private GroupSkmsgFanoutStanza() {
        throw new AssertionError();
    }

    /**
     * Builds the outer {@code <message>} stanza for group SKMSG fanout.
     * <p>
     * The bot-feedback path drops both {@code phash} and the {@code <enc type="skmsg">} sibling, because delivery then
     * happens only via the {@code <bot>} child.
     *
     * @implNote This implementation defers to the caller to supply {@code skDistributionStanza}, {@code identityStanza},
     * {@code metaStanza}, {@code bizStanza}, {@code botStanza}, {@code reportingStanza}, and {@code senderContentBinding}; null
     * children are elided by {@link StanzaBuilder#content(Stanza...)}.
     *
     * @param messageId            the stanza id
     * @param groupJid             the group {@link Jid}
     * @param type                 the stanza {@code type} attribute
     * @param phash                the participant hash (V2); {@code null} for bot-feedback sends where the attribute is
     *                             dropped
     * @param skmsgCiphertext      the SKMSG-encrypted ciphertext; {@code null} for bot-feedback sends where the
     *                             {@code <enc>} child is omitted entirely
     * @param mediaType            the {@code mediatype} attribute on the {@code <enc>}, or {@code null}
     * @param decryptFail          the {@code decrypt-fail} attribute on the {@code <enc>}, or {@code null}
     * @param editAttribute        the {@code edit} attribute on the outer {@code <message>}, or {@code null}
     * @param addressingMode       {@code "pn"} or {@code "lid"}
     * @param skDistributionStanza   the {@code <participants>} carrying SK distribution payloads for new members, or
     *                             {@code null}
     * @param identityStanza         the {@code <device-identity>} child, or {@code null}
     * @param metaStanza             the {@code <meta>} child, or {@code null}
     * @param bizStanza              the {@code <biz>} child, or {@code null}
     * @param botStanza              the {@code <bot>} child, or {@code null}
     * @param reportingStanza        the {@code <reporting>} child, or {@code null}
     * @param senderContentBinding the {@code <sender_content_binding>} child, or {@code null}
     * @return the {@link StanzaBuilder} for the outer {@code <message>}
     * @throws NullPointerException if {@code messageId}, {@code groupJid}, {@code type}, or {@code addressingMode} is
     *                              {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendGroupSkmsgJob", exports = "encryptAndSendSenderKeyMsg",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static StanzaBuilder build(
            String messageId,
            Jid groupJid,
            String type,
            String phash,
            byte[] skmsgCiphertext,
            String mediaType,
            String decryptFail,
            String editAttribute,
            String addressingMode,
            Stanza skDistributionStanza,
            Stanza identityStanza,
            Stanza metaStanza,
            Stanza bizStanza,
            Stanza botStanza,
            Stanza reportingStanza,
            Stanza senderContentBinding
    ) {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(groupJid, "groupJid");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(addressingMode, "addressingMode");

        var skmsgEncNode = skmsgCiphertext != null
                ? new StanzaBuilder()
                        .description("enc")
                        .attribute("v", String.valueOf(MessageEncryption.CIPHERTEXT_VERSION))
                        .attribute("type", MessageEncryptionType.SKMSG.protocolValue())
                        .attribute("mediatype", mediaType)
                        .attribute("decrypt-fail", decryptFail)
                        .content(skmsgCiphertext)
                        .build()
                : null;

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "building skmsg fanout stanza id={0} group={1} type={2} hasEnc={3}",
                    messageId, groupJid, type, skmsgEncNode != null);
        }

        return new StanzaBuilder()
                .description("message")
                .attribute("id", messageId)
                .attribute("to", groupJid)
                .attribute("type", type)
                .attribute("phash", phash)
                .attribute("edit", editAttribute)
                .attribute("addressing_mode", addressingMode)
                .content(
                        skDistributionStanza,
                        skmsgEncNode,
                        identityStanza,
                        bizStanza,
                        metaStanza,
                        botStanza,
                        senderContentBinding,
                        reportingStanza
                )
                ;
    }
}
