package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.QrHandler;
import it.auties.whatsapp.binary.Socket;

public interface OnQrCode extends QrDiscardingListener {
    /**
     * Called when {@link Socket} successfully establishes a connection with new secrets.
     * By default, the QR code is printed to the console.
     *
     * @return a non-null handler to process the qr code
     */
    QrHandler onQRCode();
}