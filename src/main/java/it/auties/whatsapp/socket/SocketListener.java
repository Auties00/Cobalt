package it.auties.whatsapp.socket;

public interface SocketListener {
    void onOpen(SocketSession session);

    void onMessage(byte[] message, int length);

    void onClose();

    void onError(Throwable throwable);
}
