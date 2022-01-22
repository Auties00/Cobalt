package it.auties.whatsapp.protobuf.media;

import it.auties.whatsapp.protobuf.message.model.MediaMessage;
import it.auties.whatsapp.protobuf.sync.HistorySyncNotification;

public sealed interface AttachmentProvider permits MediaMessage, it.auties.whatsapp.protobuf.sync.ExternalBlobReference, HistorySyncNotification {
    String name();
    String url();
    String directPath();
    byte[] key();
    String keyName();
    byte[] fileSha256();
    byte[] fileEncSha256();
    long fileLength();
}
