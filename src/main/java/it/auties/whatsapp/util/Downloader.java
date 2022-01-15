package it.auties.whatsapp.util;

import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.crypto.AesCbc;
import it.auties.whatsapp.crypto.Hmac;
import it.auties.whatsapp.manager.WhatsappStore;
import it.auties.whatsapp.protobuf.media.AttachmentProvider;
import it.auties.whatsapp.protobuf.media.MediaKeys;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@UtilityClass
public class Downloader {
    public byte[] download(AttachmentProvider provider, WhatsappStore store) {
        for(var url : collectDownloadUrls(provider, store)){
            var decrypted = download(provider, url);
            if(decrypted.isPresent()){
                return decrypted.get();
            }
        }

        throw new IllegalStateException("Cannot download encrypted media: no suitable host found");
    }

    private Optional<byte[]> download(AttachmentProvider provider, String url) {
        try {
            var stream = BinaryArray.of(new URL(url)
                    .openStream()
                    .readAllBytes());

            var digest = MessageDigest.getInstance("SHA-256");
            digest.update(stream.data());
            var sha256 = digest.digest();
            Validate.isTrue(Arrays.equals(sha256, provider.fileEncSha256()),
                    "Cannot decode media: Invalid sha256 signature",
                    SecurityException.class);

            var encryptedMedia = stream.cut(-10)
                    .data();
            var mediaMac = stream.slice(-10)
                    .data();

            var keys = MediaKeys.ofProvider(provider);
            var hmacInput = BinaryArray.of(keys.iv())
                    .append(encryptedMedia)
                    .data();
            var hmac = Hmac.calculate(hmacInput, keys.macKey())
                    .cut(10)
                    .data();
            Validate.isTrue(Arrays.equals(hmac, mediaMac),
                    "Cannot decode media: Hmac validation failed",
                    SecurityException.class);

            var decrypted = AesCbc.decrypt(keys.iv(), encryptedMedia, keys.cipherKey());
            Validate.isTrue(provider.fileLength() == decrypted.length,
                    "Cannot decode media: invalid size");

            return Optional.of(decrypted);
        } catch (IOException | NoSuchAlgorithmException e) {
            return Optional.empty();
        }
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
