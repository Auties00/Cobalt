package it.auties.whatsapp.protobuf.signal.session;

public record UnacknowledgedPreKeyMessageItems(int preKeyId, int signedPreKeyId, byte[] baseKey) {

}