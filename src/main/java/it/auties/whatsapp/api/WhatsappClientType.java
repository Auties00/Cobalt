package it.auties.whatsapp.api;

import it.auties.protobuf.annotation.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various types of Whatsapp client that can be initialized
 */
@ProtobufEnum
public enum WhatsappClientType {
    /**
     * Standalone <a href="https://web.whatsapp.com">Whatsapp Web Client</a>
     */
    WEB,

    /**
     * Standalone Mobile App Client
     */
    MOBILE
}
