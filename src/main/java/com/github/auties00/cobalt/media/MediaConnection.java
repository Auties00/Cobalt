package com.github.auties00.cobalt.media;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
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
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.*;

public final class MediaConnection {
    private final String auth;
    private final int ttl;
    private final int maxBuckets;
    private final long timestamp;
    private final SequencedCollection<? extends MediaHost> hosts;

    public MediaConnection(String auth, int ttl, int maxBuckets, long timestamp, SequencedCollection<? extends MediaHost> hosts) {
        this.auth = auth;
        this.ttl = ttl;
        this.maxBuckets = maxBuckets;
        this.timestamp = timestamp;
        this.hosts = hosts;
    }

    public boolean upload(MediaProvider provider, InputStream inputStream) {
        Objects.requireNonNull(provider, "provider cannot be null");
        Objects.requireNonNull(inputStream, "inputStream cannot be null");

        var path = provider.mediaPath()
                .path();
        if (path.isEmpty()) {
            return false;
        }

        try(var client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()) {
            var uploadStream = provider.mediaPath()
                    .keyName()
                    .map(keyName ->  MediaUploadInputStream.ofCiphertext(inputStream, keyName))
                    .orElseGet(() -> MediaUploadInputStream.ofPlaintext(inputStream));
            var tempFile = Files.createTempFile("upload", ".tmp");
            try (uploadStream; var outputStream = Files.newOutputStream(tempFile)) {
                uploadStream.transferTo(outputStream);
            }
            var timestamp = Clock.nowSeconds();
            var fileSha256 = uploadStream.fileSha256();
            var fileEncSha256 = uploadStream.fileEncSha256()
                    .orElse(null);
            var mediaKey = uploadStream.fileKey()
                    .orElse(null);
            var fileLength = uploadStream.fileLength();

            for (var host : hosts) {
                if(!host.canUpload(provider)) {
                    continue;
                }

                var uploadResult = tryUpload(client, host.hostname(), path.get(), fileEncSha256, fileSha256, tempFile)
                        .or(() -> host.fallbackHostname().flatMap(fallbackHostname -> tryUpload(client, fallbackHostname, path.get(), fileEncSha256, fileSha256, tempFile)));
                if(uploadResult.isPresent()) {
                    var directPath = uploadResult.get()
                            .getString("direct_path");
                    var url = uploadResult.get()
                            .getString("url");
                    // var handle = jsonObject.getString("handle");

                    provider.setMediaSha256(fileSha256);
                    provider.setMediaEncryptedSha256(fileEncSha256);
                    provider.setMediaKey(mediaKey);
                    provider.setMediaSize(fileLength);
                    provider.setMediaDirectPath(directPath);
                    provider.setMediaUrl(url);
                    provider.setMediaKeyTimestamp(timestamp);

                    return true;
                }
            }

            throw new MediaUploadException("Cannot upload media: no hosts available");
        }catch (IOException exception) {
            throw new MediaUploadException("Cannot upload media", exception);
        }
    }

    private Optional<JSONObject> tryUpload(HttpClient client, String hostname, String path, byte[] fileEncSha256, byte[] fileSha256, Path body) {
        try {
            var auth = URLEncoder.encode(this.auth, StandardCharsets.UTF_8);
            var token = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(Objects.requireNonNullElse(fileEncSha256, fileSha256));
            var uri = URI.create("https://%s/%s/%s?auth=%s&token=%s".formatted(hostname, path, token, auth, token));
            var requestBuilder = HttpRequest.newBuilder()
                    .uri(uri)
                    .POST(HttpRequest.BodyPublishers.ofFile(body));
            var request = requestBuilder.header("Content-Type", "application/octet-stream")
                    .header("Accept", "application/json")
                    .headers("Origin", "https://web.whatsapp.com")
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new MediaUploadException("Cannot upload media: status code " + response.statusCode());
            }

            var jsonObject = JSON.parseObject(response.body());
            return Optional.ofNullable(jsonObject);
        }catch (Throwable _) {
            return Optional.empty();
        }
    }

    public InputStream download(MediaProvider provider) throws GeneralSecurityException {
        Objects.requireNonNull(provider, "provider cannot be null");

        var defaultUploadUrl = provider.mediaUrl();
        if(defaultUploadUrl.isPresent()) {
            var result = MediaDownloadInputStream.of(provider, defaultUploadUrl.get());
            if(result.isPresent()) {
                return result.get();
            }
        }

        var defaultDirectPath = provider.mediaDirectPath()
                .orElseThrow(() -> new MediaDownloadException("Missing direct path from media"));
        for(var host : hosts) {
            if(!host.canDownload(provider)) {
                continue;
            }

            var uploadUrl = "https://" + host.hostname() + defaultDirectPath;
            var result = MediaDownloadInputStream.of(provider, uploadUrl);
            if(result.isPresent()) {
                return result.get();
            }
        }

        throw new MediaDownloadException("Cannot download media: no hosts available");
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

    public SequencedCollection<? extends MediaHost> hosts() {
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
