package it.auties.whatsapp4j.common.socket;

import it.auties.whatsapp4j.common.api.AbstractWhatsappAPI;
import it.auties.whatsapp4j.common.manager.WhatsappKeysManager;
import lombok.NonNull;

/**
 * This class is an interface between this API and WhatsappWeb's WebClient.
 * These methods should not be used by any project, excluding obviously WhatsappWeb4j.
 * Instead, {@link AbstractWhatsappAPI} should be used.
 */
public interface IWhatsappSocket {
    void connect();
    @NonNull WhatsappKeysManager keys();
}