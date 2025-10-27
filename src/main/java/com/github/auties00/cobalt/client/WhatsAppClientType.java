package com.github.auties00.cobalt.client;

import it.auties.protobuf.annotation.ProtobufEnum;

/**
 * Represents the different types of WhatsApp clients that can be initialized in this API.
 * <p>
 * This enumeration defines the various platforms where a WhatsApp client can operate.
 * Each client type might have specific capabilities, limitations, and connection protocols
 * that affect how messages are sent, received, and processed within the system.
 * <p>
 */
@ProtobufEnum
public enum WhatsAppClientType {
    /**
     * Represents a web-based WhatsApp client that connects through <a href="https://web.whatsapp.com">web.whatsapp.com</a>.
     * <p>
     * This client type emulates the official WhatsApp Web application.
     */
    WEB,

    /**
     * Represents a mobile application client for WhatsApp.
     * <p>
     * This client type emulates the behavior of the official WhatsApp mobile application.
     */
    MOBILE
}