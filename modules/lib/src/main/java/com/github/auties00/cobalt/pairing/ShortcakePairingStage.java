package com.github.auties00.cobalt.pairing;

/**
 * The stages of the Shortcake passkey linking ceremony, in the order the companion advances through
 * them.
 *
 * <p>Consulted by {@link LiveShortcakePairingService} to validate that each inbound step arrives in
 * the expected order and to reject out-of-sequence notifications.
 */
enum ShortcakePairingStage {
    /**
     * No ceremony is in progress.
     */
    NOT_STARTED,
    /**
     * The prologue has been sent and the companion is awaiting the primary's ephemeral identity.
     */
    WAITING_FOR_PRIMARY_IDENTITY,
    /**
     * The encrypted pairing request has been sent and the companion is awaiting pairing completion.
     */
    WAITING_FOR_PAIRING_COMPLETION
}
