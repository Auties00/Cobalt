package com.github.auties00.cobalt.media;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.exception.MediaDownloadException;
import com.github.auties00.cobalt.exception.MediaUploadException;
import com.github.auties00.cobalt.model.media.MediaProvider;
import com.github.auties00.cobalt.util.Clock;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public final class MediaConnection {
    private final String auth;
    private final int ttl;
    private final int maxBuckets;
    private final long timestamp;
    private final List<String> hosts;

    public MediaConnection(String auth, int ttl, int maxBuckets, long timestamp, List<String> hosts) {
        this.auth = auth;
        this.ttl = ttl;
        this.maxBuckets = maxBuckets;
        this.timestamp = timestamp;
        this.hosts = hosts;
    }

    public boolean upload(MediaProvider provider, InputStream inputStream) {
        Objects.requireNonNull(provider, "provider cannot be null");
        Objects.requireNonNull(inputStream, "inputStream cannot be null");

        var type = provider.mediaPath();
        var path = type.path();
        if (path.isEmpty()) {
            return false;
        }

        try(var client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()) {
            var uploadStream = type.keyName()
                    .map(keyName ->  MediaUploadInputStream.ofCiphertext(inputStream, keyName))
                    .orElseGet(() -> MediaUploadInputStream.ofPlaintext(inputStream));
            var tempFile = Files.createTempFile("upload", ".tmp");
            try (uploadStream; var outputStream = Files.newOutputStream(tempFile)) {
                uploadStream.transferTo(outputStream);
            }
            var fileSha256 = uploadStream.fileSha256();
            var fileEncSha256 = uploadStream.fileEncSha256()
                    .orElse(null);
            var mediaKey = uploadStream.fileKey()
                    .orElse(null);
            var fileLength = uploadStream.fileLength();

            var auth = URLEncoder.encode(this.auth, StandardCharsets.UTF_8);
            for (var host : hosts) {
                try {
                    var timestamp = Clock.nowSeconds();
                    var token = Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(Objects.requireNonNullElse(fileEncSha256, fileSha256));
                    var uri = URI.create("https://%s/%s/%s?auth=%s&token=%s".formatted(host, path, token, auth, token));
                    var requestBuilder = HttpRequest.newBuilder()
                            .uri(uri)
                            .POST(HttpRequest.BodyPublishers.ofFile(tempFile));
                    var request = requestBuilder.header("Content-Type", "application/octet-stream")
                            .header("Accept", "application/json")
                            .headers("Origin", "https://web.whatsapp.com")
                            .build();
                    var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                    if (response.statusCode() != 200) {
                        throw new MediaUploadException("Cannot upload media: status code " + response.statusCode());
                    }

                    var jsonObject = JSON.parseObject(response.body());
                    if (jsonObject == null) {
                        throw new MediaUploadException("Cannot parse upload response: " + new String(response.body()));
                    }

                    var directPath = jsonObject.getString("direct_path");
                    var url = jsonObject.getString("url");
                    // var handle = jsonObject.getString("handle");

                    provider.setMediaSha256(fileSha256);
                    provider.setMediaEncryptedSha256(fileEncSha256);
                    provider.setMediaKey(mediaKey);
                    provider.setMediaSize(fileLength);
                    provider.setMediaDirectPath(directPath);
                    provider.setMediaUrl(url);
                    provider.setMediaKeyTimestamp(timestamp);

                    return true;
                }catch (Throwable _) {

                }
            }

            throw new MediaUploadException("Cannot upload media: no hosts available");
        }catch (IOException exception) {
            throw new MediaUploadException("Cannot upload media", exception);
        }
    }

    public InputStream download(MediaProvider provider) {
        Objects.requireNonNull(provider, "provider cannot be null");

        var url = provider.mediaUrl()
                .or(() -> provider.mediaDirectPath().map(directPath -> "https://mmg.whatsapp.net" + directPath))
                .orElseThrow(() -> new MediaDownloadException("Missing url or direct path from media"));

        try(var client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()) {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            var payloadLength = (int) response.headers()
                    .firstValueAsLong("Content-Length")
                    .orElseThrow(() -> new MediaDownloadException("Unknown content length"));

            var rawInputStream = response.body();

            var hasKeyName = provider.mediaPath()
                    .keyName()
                    .isPresent();
            var hasMediaKey = provider.mediaKey()
                    .isPresent();
            if (hasKeyName != hasMediaKey) {
                throw new MediaDownloadException("Media key and key name must both be present or both be absent");
            }else if (hasKeyName) {
                return MediaDownloadInputStream.ofCiphertext(
                        rawInputStream,
                        payloadLength,
                        provider
                );
            } else {
                return MediaDownloadInputStream.ofPlaintext(
                        rawInputStream,
                        payloadLength,
                        provider
                );
            }
        } catch (Throwable throwable) {
            throw new MediaDownloadException(throwable);
        }
    }

    public String auth() {
        return auth;
    }

    public int ttl() {
        return ttl;
    }

    public int maxBuckets() {
        return maxBuckets;
    }

    public long timestamp() {
        return timestamp;
    }

    public List<String> hosts() {
        return hosts;
    }

    @Override
    public String toString() {
        return "MediaConnection[" +
               "auth=" + auth + ", " +
               "ttl=" + ttl + ", " +
               "maxBuckets=" + maxBuckets + ", " +
               "timestamp=" + timestamp + ", " +
               "hosts=" + hosts + ']';
    }
}
