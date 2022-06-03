package it.auties.whatsapp.model.media;

public record MediaFile(byte[] fileSha256, byte[] fileEncSha256, byte[] mediaKey, long fileLength, String directPath,
                        String url) {
}
