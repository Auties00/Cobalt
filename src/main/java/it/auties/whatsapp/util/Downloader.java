package it.auties.whatsapp.util;

import it.auties.whatsapp.crypto.AesCbc;
import it.auties.whatsapp.manager.WhatsappStore;
import it.auties.whatsapp.protobuf.media.AttachmentProvider;
import it.auties.whatsapp.protobuf.media.MediaKeys;
import lombok.experimental.UtilityClass;

import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@UtilityClass
public class Downloader {
    public byte[] download(AttachmentProvider provider, WhatsappStore store) {
        for(var url : collectDownloadUrls(provider, store)){
            try {
                var encryptedMedia = new URL(url)
                        .openStream()
                        .readAllBytes();

                var digest = MessageDigest.getInstance("SHA-256");
                digest.update(encryptedMedia);
                var sha256 = digest.digest();

                Validate.isTrue(Arrays.equals(sha256, provider.fileEncSha256()),
                        "Cannot decode media: Invalid sha256 signature",
                        SecurityException.class);

                var keys = MediaKeys.ofProvider(provider);
                var decrypted = AesCbc.decrypt(keys.iv(), encryptedMedia, keys.cipherKey());
                Validate.isTrue(provider.fileLength() == decrypted.length,
                        "Cannot decode media: invalid size");

                return decrypted;
            } catch (Exception ignored) {

            }
        }

        throw new IllegalStateException("Cannot download encrypted media: no suitable host found");
    }

    private List<String> collectDownloadUrls(AttachmentProvider provider, WhatsappStore store){
        if(provider.url() != null){
            return List.of(provider.url());
        }

        var fileEncSha256 = Base64.getEncoder().encode(provider.fileEncSha256());
        return store.mediaConnection()
                .hosts()
                .stream()
                .map(host -> "https://%s%s&hash=%s&mms-type=%s&__wa-mms=".formatted(host, provider.directPath(), fileEncSha256, provider.name()))
                .toList();
    }
}
