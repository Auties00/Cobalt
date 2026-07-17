package com.github.auties00.cobalt.bot;

import com.github.auties00.cobalt.wire.stanza.mex.json.bot.FetchBotCertificateRevocationListMexRequest;

import java.math.BigInteger;

/**
 * Maintains the bot-feature certificate revocation list (CRL) and answers per-certificate
 * revocation queries for the {@link BotSignatureVerificationService}.
 *
 * <p>WhatsApp distributes the revoked serial numbers of the {@code whatsapp_simple_signal} PKI
 * through a MEX-fetched CRL rather than the certificates' own distribution points. An implementation
 * fetches that CRL through {@link FetchBotCertificateRevocationListMexRequest}, parses its revoked
 * serial numbers, records the {@code next_update} watermark, and refreshes it periodically while
 * verification is enabled. A revocation query returns {@link BotRevocationStatus#CRL_UNAVAILABLE} until
 * the first fetch succeeds and {@link BotRevocationStatus#CRL_STALE} once the CRL has aged past its
 * {@code next_update}; either is treated as a verification failure, matching WA Web's fail-closed
 * policy.
 */
public interface BotCertificateRevocationService {
    /**
     * Returns the revocation status of a certificate serial number at the given time.
     *
     * @implSpec
     * Implementations return {@link BotRevocationStatus#CRL_UNAVAILABLE} before any successful fetch,
     * {@link BotRevocationStatus#CRL_STALE} when {@code nowMs} is past the recorded {@code next_update}
     * watermark, {@link BotRevocationStatus#REVOKED} when {@code serial} is listed in the current CRL, and
     * {@link BotRevocationStatus#VALID} otherwise.
     *
     * @param serial the certificate serial number
     * @param nowMs  the reference time in epoch milliseconds
     * @return the revocation status
     */
    BotRevocationStatus checkRevocationStatus(BigInteger serial, long nowMs);

    /**
     * Starts the periodic CRL refresh if it is not already running.
     *
     * @implSpec
     * Implementations run the first refresh as soon as possible and then refresh on a fixed cadence
     * while running. Repeated calls are no-ops while a refresh schedule is active, and a refresh
     * failure leaves the previously published CRL state in place to be retried on the next tick.
     */
    void startPeriodicRefresh();

    /**
     * Stops the periodic CRL refresh, cancelling any scheduled tick.
     *
     * @implSpec
     * Implementations cancel any pending refresh and return the service to the not-started state so a
     * later {@link #startPeriodicRefresh()} re-arms it. Calling this when no refresh is scheduled is a
     * no-op, and the last published CRL state remains queryable.
     */
    void stopPeriodicRefresh();
}
