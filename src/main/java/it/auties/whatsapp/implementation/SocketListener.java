package it.auties.whatsapp.implementation;

public interface SocketListener {
    void onOpen(SocketSession session);

    void onMessage(byte[] message, int length);

    void onClose();

    void onError(Throwable throwable);
}