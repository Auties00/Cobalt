package com.github.auties00.cobalt.client.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;
import it.auties.protobuf.annotation.ProtobufEnum;

/**
 * Names the product flavour a {@link LinkedWhatsAppClient} impersonates on the
 * wire.
 *
 * @apiNote
 * Picked at builder time and persisted in the store so the same flavour
 * is used on every reconnect. The value drives transport selection,
 * handshake payload shape, registration code paths, history-sync
 * behaviour, and which {@link LinkedWhatsAppClientListener} callbacks fire.
 *
 * @see LinkedWhatsAppClientBuilder
 * @see LinkedWhatsAppClientDevice
 */
@ProtobufEnum
public enum LinkedWhatsAppClientType {
    /**
     * A companion that links to an existing primary mobile account.
     *
     * @apiNote
     * Selected by {@link LinkedWhatsAppClientBuilder#webClient()}. The
     * companion does not own a phone number; it goes through the linked-device
     * ceremony driven by {@link LinkedWhatsAppClientVerificationHandler.Web}
     * (QR code or pairing code) and inherits the primary device's
     * identity.
     */
    WEB,

    /**
     * A primary client that registers a phone number directly with the
     * WhatsApp servers.
     *
     * @apiNote
     * Selected by {@link LinkedWhatsAppClientBuilder#mobileClient()}. The
     * client owns the phone number and runs the full SMS, voice, or
     * in-app verification flow, surfacing
     * {@link LinkedWhatsAppClientListener#onRegistrationCode(LinkedWhatsAppClient, long)}
     * during registration.
     */
    MOBILE
}
