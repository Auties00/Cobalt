package it.auties.whatsapp4j.api;

import it.auties.whatsapp4j.event.WhatsappListener;
import it.auties.whatsapp4j.model.WhatsappChat;
import it.auties.whatsapp4j.model.WhatsappKeys;
import it.auties.whatsapp4j.model.WhatsappManager;
import it.auties.whatsapp4j.socket.WhatsappWebSocket;
import it.auties.whatsapp4j.utils.Validate;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Accessors(fluent = true)
public class WhatsappAPI extends WhatsappListener {
    private final @Getter @NotNull WhatsappManager manager;
    private final @Getter @NotNull WhatsappKeys keys;
    private final @NotNull WhatsappWebSocket socket;
    private final @NotNull List<WhatsappListener> listeners;
    public WhatsappAPI(){
        this(WhatsappConfiguration.defaultOptions());
    }

    public WhatsappAPI(@NotNull WhatsappConfiguration configuration){
        this.listeners = new ArrayList<>();
        this.manager = WhatsappManager.buildInstance();
        this.keys = WhatsappKeys.buildInstance();
        this.socket = new WhatsappWebSocket(this, configuration, manager, keys);
    }

    public void connect(){
        Validate.isTrue(socket.state() == WhatsappState.NOTHING, "WhatsappAPI: Cannot establish a connection with whatsapp as one already exists");
        socket.connect();
    }

    public void disconnect(){
        Validate.isTrue(socket.state() != WhatsappState.NOTHING, "WhatsappAPI: Cannot terminate the connection with whatsapp as it doesn't exist");
        socket.disconnect(null, false, false);
    }

    public void disconnectAndLogout(){
        Validate.isTrue(socket.state() != WhatsappState.NOTHING, "WhatsappAPI: Cannot terminate the connection with whatsapp as it doesn't exist");
        socket.disconnect(null, true, false);
    }

    public void reconnect(){
        Validate.isTrue(socket.state() != WhatsappState.NOTHING, "WhatsappAPI: Cannot terminate the connection with whatsapp as it doesn't exist");
        socket.disconnect(null, false, true);
    }

    public void registerListener(WhatsappListener listener){
        listeners.add(listener);
    }

    public void removeListener(WhatsappListener listener){
        listeners.remove(listener);
    }

    @Override
    public void onConnecting() {
        listeners.forEach(WhatsappListener::onConnecting);
    }

    @Override
    public void onOpen() {
        listeners.forEach(WhatsappListener::onOpen);
    }

    @Override
    public void onClose() {
        listeners.forEach(WhatsappListener::onClose);
    }

    @Override
    public void onPhoneStatusUpdateReceived() {
        listeners.forEach(WhatsappListener::onPhoneStatusUpdateReceived);
    }

    @Override
    public void onContactsReceived() {
        listeners.forEach(WhatsappListener::onContactsReceived);
    }

    @Override
    public void onContactUpdate() {
        listeners.forEach(WhatsappListener::onContactUpdate);
    }

    @Override
    public void onChatReceived(WhatsappChat chat) {
       listeners.forEach(listener -> listener.onChatReceived(chat));
    }

    @Override
    public void onChatsReceived() {
        listeners.forEach(WhatsappListener::onChatsReceived);
    }

    @Override
    public void onChatUpdate() {
        listeners.forEach(WhatsappListener::onChatUpdate);
    }

    @Override
    public void onBlacklistReceived() {
        listeners.forEach(WhatsappListener::onBlacklistReceived);
    }

    @Override
    public void onBlacklistUpdate() {
        listeners.forEach(WhatsappListener::onBlacklistUpdate);
    }
}
