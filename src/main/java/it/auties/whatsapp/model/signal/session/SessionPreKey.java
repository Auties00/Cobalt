package it.auties.whatsapp.model.signal.session;


public record SessionPreKey(Integer preKeyId, byte[] baseKey, int signedKeyId) {

}