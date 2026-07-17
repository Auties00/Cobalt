package com.github.auties00.cobalt.device.icdc;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Carries the outcome of inspecting an inbound message's ICDC metadata for
 * hosted business-coexistence transitions.
 *
 * <p>This record is returned by
 * {@link com.github.auties00.cobalt.device.DeviceService#handleHostedIcdcMetadataInline}
 * during inbound-message processing. Its two booleans drive two distinct
 * downstream effects. {@link #hostedBizEncMismatch()} reports that the cached
 * account type disagrees with the incoming HOSTED type, which requires the
 * device list to be refreshed via USync. {@link #senderOrRecipientAccountTypeHosted()}
 * reports that either side of the chat is a hosted business account, which may
 * require the coexistence system message to be inserted into the chat.
 *
 * @param hostedBizEncMismatch               {@code true} when the local ADV
 *                                           account type disagrees with the
 *                                           incoming HOSTED type and a device
 *                                           list refresh is required
 * @param senderOrRecipientAccountTypeHosted {@code true} when either side of
 *                                           the chat in the inbound metadata
 *                                           is a hosted account
 */
@WhatsAppWebModule(moduleName = "WAWebIcdcHandlerApi")
public record HostedIcdcResult(
        boolean hostedBizEncMismatch,
        boolean senderOrRecipientAccountTypeHosted
) {
    /**
     * Holds the no-op outcome returned on every code path that short-circuits
     * before any hosted-coexistence work happens.
     *
     * <p>This instance carries {@code false} for both fields, meaning "nothing
     * to do; do not flag the UI and do not refresh the device list". It is
     * returned when the {@code adv_accept_hosted_devices} AB prop is off, when
     * the chat is with self, when the chat is not a user chat, when the inbound
     * message carries no {@code deviceListMetadata}, or when the relevant
     * party's account type is missing or already E2EE.
     */
    @WhatsAppWebExport(moduleName = "WAWebIcdcHandlerApi",
            exports = "handleHostedIcdcMetadataInline",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static final HostedIcdcResult DEFAULT = new HostedIcdcResult(false, false);
}
