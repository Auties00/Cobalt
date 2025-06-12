package it.auties.whatsapp.model.media;

public record MediaFile(byte[] encryptedFile, byte[] fileSha256, byte[] fileEncSha256, byte[] mediaKey, long fileLength,
                        String directPath, String url, String handle, Long timestamp) {

}
