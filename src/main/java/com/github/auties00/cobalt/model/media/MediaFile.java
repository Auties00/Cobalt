package com.github.auties00.cobalt.model.media;

public record MediaFile(byte[] encryptedFile, byte[] fileSha256, byte[] fileEncSha256, byte[] mediaKey, long fileLength,
                        String directPath, String url, String handle, Long timestamp) {

}
