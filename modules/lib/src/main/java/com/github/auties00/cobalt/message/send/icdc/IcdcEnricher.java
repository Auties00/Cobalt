package com.github.auties00.cobalt.message.send.icdc;

import com.github.auties00.cobalt.device.icdc.IcdcResult;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageContextInfoBuilder;
import com.github.auties00.cobalt.wire.linked.device.DeviceListMetadataBuilder;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;

/**
 * Stamps Identity Change Detection Consistency device-list metadata onto
 * outgoing {@link LinkedMessageContainer} instances.
 *
 * <p>This utility lets a recipient detect any change to the sender's or
 * recipient's device list since the last key exchange by carrying a
 * {@link com.github.auties00.cobalt.wire.linked.device.DeviceListMetadata} inside the
 * container's {@link com.github.auties00.cobalt.wire.linked.chat.ChatMessageContextInfo}.
 * A message that omits this metadata loses the integrity check rather than being
 * rejected. The only entry point is {@link #enrich(LinkedMessageContainer, IcdcResult, IcdcResult)};
 * it is called from the per-recipient fanout path, including
 * {@link com.github.auties00.cobalt.message.send.senderkey.SenderKeyDistribution}.
 */
@WhatsAppWebModule(moduleName = "WAWebE2EProtoGenerator")
@WhatsAppWebModule(moduleName = "WAWebICDCMetaApi")
public final class IcdcEnricher {
    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private IcdcEnricher() {
        throw new AssertionError();
    }

    /**
     * Returns a copy of {@code container} with ICDC device-list metadata merged
     * into its {@link com.github.auties00.cobalt.wire.linked.chat.ChatMessageContextInfo}.
     *
     * <p>The supplied sender and recipient {@link IcdcResult} pair populates a
     * {@link com.github.auties00.cobalt.wire.linked.device.DeviceListMetadata} on the
     * container's context info, every other context-info field is preserved, and
     * {@code deviceListMetadataVersion} is set to {@code 2}. When both
     * {@link IcdcResult} inputs are {@code null} the method is a no-op and the
     * original container is returned by reference.
     *
     * @implNote
     * This implementation reproduces WA Web's spread merge by copying every
     * present field of the existing
     * {@link com.github.auties00.cobalt.wire.linked.chat.ChatMessageContextInfo} into a
     * fresh {@link ChatMessageContextInfoBuilder} before overwriting
     * {@code deviceListMetadata} and {@code deviceListMetadataVersion}; the
     * {@link LinkedMessageContainer} is then replaced via
     * {@link LinkedMessageContainer#withMessageContextInfo(com.github.auties00.cobalt.wire.linked.chat.ChatMessageContextInfo)}
     * so the original instance stays untouched. The version constant {@code 2}
     * matches the value WA Web pins for ICDC device-list metadata.
     *
     * @param container     the original {@link LinkedMessageContainer}
     * @param senderIcdc    the sender's {@link IcdcResult}, or {@code null}
     * @param recipientIcdc the recipient's {@link IcdcResult}, or {@code null}
     * @return the enriched container; the original instance when both ICDC
     *         inputs are {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebE2EProtoGenerator", exports = "populateMessageContextInfo",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebICDCMetaApi", exports = "populateICDCMeta",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static LinkedMessageContainer enrich(
            LinkedMessageContainer container,
            IcdcResult senderIcdc,
            IcdcResult recipientIcdc
    ) {
        if (senderIcdc == null && recipientIcdc == null) {
            return container;
        }

        var metadataBuilder = new DeviceListMetadataBuilder();
        if (senderIcdc != null) {
            senderIcdc.keyHash().ifPresent(metadataBuilder::senderKeyHash);
            senderIcdc.timestamp().ifPresent(metadataBuilder::senderTimestamp);
            metadataBuilder.senderKeyIndexes(senderIcdc.keyIndexes());
            senderIcdc.accountType().ifPresent(metadataBuilder::senderAccountType);
        }
        if (recipientIcdc != null) {
            recipientIcdc.keyHash().ifPresent(metadataBuilder::recipientKeyHash);
            recipientIcdc.timestamp().ifPresent(metadataBuilder::recipientTimestamp);
            metadataBuilder.recipientKeyIndexes(recipientIcdc.keyIndexes());
            recipientIcdc.accountType().ifPresent(metadataBuilder::receiverAccountType);
        }

        var existing = container.messageContextInfo().orElse(null);
        var infoBuilder = new ChatMessageContextInfoBuilder()
                .deviceListMetadata(metadataBuilder.build())
                .deviceListMetadataVersion(2);
        if (existing != null) {
            existing.messageSecret().ifPresent(infoBuilder::messageSecret);
            existing.paddingBytes().ifPresent(infoBuilder::paddingBytes);
            existing.botMessageSecret().ifPresent(infoBuilder::botMessageSecret);
            existing.botMetadata().ifPresent(infoBuilder::botMetadata);
            infoBuilder.capiCreatedGroup(existing.capiCreatedGroup());
            existing.supportPayload().ifPresent(infoBuilder::supportPayload);
            infoBuilder.threadId(existing.threadId());
            existing.messageAddOnExpiryType().ifPresent(infoBuilder::messageAddOnExpiryType);
            existing.messageAssociation().ifPresent(infoBuilder::messageAssociation);
            existing.limitSharing().ifPresent(infoBuilder::limitSharing);
            existing.limitSharingV2().ifPresent(infoBuilder::limitSharingV2);
            existing.weblinkRenderConfig().ifPresent(infoBuilder::weblinkRenderConfig);
            existing.reportingTokenVersion().ifPresent(version -> infoBuilder.reportingTokenVersion(version));
            existing.messageAddOnDurationInSecs().ifPresent(secs -> infoBuilder.messageAddOnDurationInSecs(secs));
        }

        return container.withMessageContextInfo(infoBuilder.build());
    }
}
