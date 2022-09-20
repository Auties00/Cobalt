package it.auties.whatsapp.model.media;

import it.auties.whatsapp.model.message.model.MediaMessage;
import it.auties.whatsapp.model.sync.ExternalBlobReference;
import it.auties.whatsapp.model.sync.HistorySyncNotification;

public sealed interface AttachmentProvider permits MediaMessage, ExternalBlobReference, HistorySyncNotification {
    String mediaUrl();

    AttachmentProvider mediaUrl(String mediaUrl);

    String mediaDirectPath();

    AttachmentProvider mediaDirectPath(String mediaDirectPath);

    byte[] mediaKey();

    String mediaName();

    byte[] mediaSha256();

    byte[] mediaEncryptedSha256();

    long mediaSize();
}
