package it.auties.whatsapp.model.media;

import it.auties.whatsapp.model.message.model.MediaMessage;
import it.auties.whatsapp.model.sync.ExternalBlobReference;
import it.auties.whatsapp.model.sync.HistorySyncNotification;

public sealed interface AttachmentProvider permits MediaMessage, ExternalBlobReference, HistorySyncNotification {
    String name();
    String url();
    String directPath();
    byte[] key();
    String keyName();
    byte[] fileSha256();
    byte[] fileEncSha256();
    Long fileLength();
}
