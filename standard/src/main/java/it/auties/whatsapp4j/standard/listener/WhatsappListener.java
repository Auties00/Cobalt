package it.auties.whatsapp4j.standard.listener;

import it.auties.whatsapp4j.standard.api.WhatsappAPI;
import it.auties.whatsapp4j.common.listener.IWhatsappListener;
import it.auties.whatsapp4j.common.listener.RegisterListener;
import it.auties.whatsapp4j.standard.response.BlocklistResponse;
import it.auties.whatsapp4j.standard.response.PhoneBatteryResponse;
import it.auties.whatsapp4j.standard.response.PropsResponse;
import it.auties.whatsapp4j.standard.response.UserInformationResponse;
import lombok.NonNull;

/**
 * This interface can be used to listen for events fired when new information is sent by WhatsappWeb's socket.
 * A WhatsappListener can be registered manually using {@link WhatsappAPI#registerListener(IWhatsappListener)}.
 * Otherwise, it can be registered by annotating it with the {@link RegisterListener} annotation.
 * If the latter option is used, auto-detection of listeners by calling {@link WhatsappAPI#autodetectListeners()}.
 */
public interface WhatsappListener extends IWhatsappListener {
    @Override
    default void onLoggedIn(@NonNull Object info) {
        if (!(info instanceof UserInformationResponse response)) {
            return;
        }

        onLoggedIn(response);
    }

    /**
     * Called when {@link it.auties.whatsapp4j.standard.socket.WhatsappSocket} successfully establishes a connection and logs in into an account.
     * When this event is called, any data, including chats and contact, is not guaranteed to be already in memory.
     * Instead, {@link WhatsappListener#onChats()} and {@link WhatsappListener#onContacts()} should be used.
     *
     * @param info the information sent by WhatsappWeb's WebSocket about this session
     */
    default void onLoggedIn(@NonNull UserInformationResponse info) {
    }

    @Override
    default void onBlocklistUpdate(@NonNull Object blocklist) {
        if (!(blocklist instanceof BlocklistResponse response)) {
            return;
        }

        onBlocklistUpdate(response);
    }

    /**
     * Called when an updated blocklist is received.
     * This method is called both when a connection is established with WhatsappWeb and when a contact is added or removed from the blocklist.
     *
     * @param blocklist the updated blocklist
     */
    default void onBlocklistUpdate(@NonNull BlocklistResponse blocklist) {
    }

    @Override
    default void onPropsUpdate(@NonNull Object props) {
        if (!(props instanceof PropsResponse response)) {
            return;
        }

        onPropsUpdate(response);
    }

    /**
     * Called when an updated list of properties is received.
     * This method is called both when a connection is established with WhatsappWeb and when new props are available.
     * In the latter case though, this object should be considered as partial and is guaranteed to contain only updated entries.
     *
     * @param props the updated list of properties
     */
    default void onPropsUpdate(@NonNull PropsResponse props) {
    }

    @Override
    default void onPhoneBatteryStatusUpdate(@NonNull Object battery) {
        if (!(battery instanceof PhoneBatteryResponse response)) {
            return;
        }

        onPhoneBatteryStatusUpdate(response);
    }


    /**
     * Called when an updated object describing the status of the phone's associated with this session battery status changes
     *
     * @param battery the new battery status
     */
    default void onPhoneBatteryStatusUpdate(@NonNull PhoneBatteryResponse battery) {
    }
}
