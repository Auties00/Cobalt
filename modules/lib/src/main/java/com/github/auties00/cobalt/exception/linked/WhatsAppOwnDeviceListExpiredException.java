package com.github.auties00.cobalt.exception.linked;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;

/**
 * Thrown when the cached list of devices linked to this account has
 * become too stale to be used for routing messages.
 *
 * <p>Every device in a WhatsApp account keeps its own record of the full
 * set of devices linked to that account, refreshed periodically from the
 * server by the same device-verification maintenance pass that drives
 * {@link WhatsAppAdvValidationException.MaintenanceJobFailed}. This exception marks the point at
 * which that record has aged past the staleness threshold the server is
 * willing to accept, so sending a message would risk targeting a device
 * that is no longer authorized or omitting one that has just been added.
 *
 * @apiNote
 * Raised before a send when the local device list is too old to trust;
 * {@link #toErrorResult()} reports {@link WhatsAppLinkedClientErrorResult#LOG_OUT},
 * so a configured {@code WhatsAppClientErrorHandler} typically refreshes the
 * list and re-pairs the device, rather than discarding the event.
 *
 * @see WhatsAppAdvValidationException.MaintenanceJobFailed
 */
public final class WhatsAppOwnDeviceListExpiredException extends WhatsAppLinkedException {
    /**
     * Constructs a new device list expired exception.
     */
    public WhatsAppOwnDeviceListExpiredException() {
        super("Own device list has expired");
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns
     * {@link WhatsAppLinkedClientErrorResult#LOG_OUT}: an expired own-device list
     * forces a fresh pairing, matching WhatsApp Web's ADV check, which logs
     * the device out once the list crosses the server-enforced staleness
     * threshold.
     */
    @Override
    public WhatsAppLinkedClientErrorResult toErrorResult() {
        return WhatsAppLinkedClientErrorResult.LOG_OUT;
    }
}
