package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.QrHandler;

interface QrDiscardingListener extends Listener {
    @Override
    default QrHandler onQRCode() {
        return null;
    }
}
