package com.github.auties00.cobalt.bot;

/**
 * The outcome of a certificate revocation query.
 */
public enum BotRevocationStatus {
    /**
     * The certificate is not revoked and the CRL is fresh.
     */
    VALID,
    /**
     * The certificate's serial number is listed in the CRL.
     */
    REVOKED,
    /**
     * No CRL has been fetched yet.
     */
    CRL_UNAVAILABLE,
    /**
     * The CRL has aged past its {@code next_update} watermark.
     */
    CRL_STALE
}
