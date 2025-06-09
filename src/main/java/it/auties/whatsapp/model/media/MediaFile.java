package it.auties.whatsapp.model.media;

import java.nio.ByteBuffer;

public record MediaFile(ByteBuffer encryptedFile, byte[] fileSha256, byte[] fileEncSha256, byte[] mediaKey, long fileLength,
                        String directPath, String url, String handle, Long timestamp) {

}
