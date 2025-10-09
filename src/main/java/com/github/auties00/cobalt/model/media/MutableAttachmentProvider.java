package com.github.auties00.cobalt.model.media;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.exception.MediaDownloadException;
import com.github.auties00.cobalt.exception.MediaUploadException;
import com.github.auties00.cobalt.io.media.download.MediaDownloadCiphertextInputStream;
import com.github.auties00.cobalt.io.media.download.MediaDownloadPlaintextInputStream;
import com.github.auties00.cobalt.io.media.upload.MediaUploadCiphertextInputStream;
import com.github.auties00.cobalt.io.media.upload.MediaUploadInputStream;
import com.github.auties00.cobalt.io.media.upload.MediaUploadPlaintextInputStream;
import com.github.auties00.cobalt.model.action.StickerAction;
import com.github.auties00.cobalt.model.message.model.MediaMessage;
import com.github.auties00.cobalt.model.sync.ExternalBlobReference;
import com.github.auties00.cobalt.model.sync.HistorySyncNotification;
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
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A sealed interface that represents a class that can provide data about a media
 */
public sealed interface MutableAttachmentProvider
        permits StickerAction, MediaMessage, ExternalBlobReference, HistorySyncNotification {
    default boolean upload(InputStream file, MediaConnection mediaConnection) {
        Objects.requireNonNull(file, "file cannot be null");
        Objects.requireNonNull(mediaConnection, "mediaConnection cannot be null");

        var type = attachmentType();
        var path = type.path();
        if (path.isEmpty()) {
            return false;
        }

        try(var client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()) {
            var uploadStream = type.keyName()
                    .map(keyName -> (MediaUploadInputStream) new MediaUploadCiphertextInputStream(file, keyName))
                    .orElseGet(() -> new MediaUploadPlaintextInputStream(file));
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

            var auth = URLEncoder.encode(mediaConnection.auth(), StandardCharsets.UTF_8);
            for (var host : mediaConnection.hosts()) {
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

                    setMediaSha256(fileSha256);
                    setMediaEncryptedSha256(fileEncSha256);
                    setMediaKey(mediaKey);
                    setMediaSize(fileLength);
                    setMediaDirectPath(directPath);
                    setMediaUrl(url);
                    setMediaKeyTimestamp(timestamp);

                    return true;
                }catch (Throwable _) {

                }
            }

            throw new MediaUploadException("Cannot upload media: no hosts available");
        }catch (IOException exception) {
            throw new MediaUploadException("Cannot upload media", exception);
        }
    }

    default InputStream download(MediaConnection mediaConnection) {
        Objects.requireNonNull(mediaConnection, "mediaConnection cannot be null");

        var url = mediaUrl()
                .or(() -> mediaDirectPath().map(directPath -> "https://mmg.whatsapp.net" + directPath))
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

            var hasKeyName = attachmentType()
                    .keyName()
                    .isPresent();
            var hasMediaKey = mediaKey()
                    .isPresent();
            if (hasKeyName != hasMediaKey) {
                throw new MediaDownloadException("Media key and key name must both be present or both be absent");
            }else if (hasKeyName) {
                return new MediaDownloadCiphertextInputStream(
                        rawInputStream,
                        payloadLength,
                        this
                );
            } else {
                return new MediaDownloadPlaintextInputStream(
                        rawInputStream,
                        payloadLength,
                        this
                );
            }
        } catch (Throwable throwable) {
            throw new MediaDownloadException(throwable);
        }
    }

    /**
     * Returns the url to the media
     *
     * @return a nullable String
     */
    Optional<String> mediaUrl();

    /**
     * Sets the media url of this provider
     *
     */
    void setMediaUrl(String mediaUrl);

    /**
     * Returns the direct path to the media
     *
     * @return a nullable String
     */
    Optional<String> mediaDirectPath();

    /**
     * Sets the direct path of this provider
     *
     */
    void setMediaDirectPath(String mediaDirectPath);

    /**
     * Returns the key of this media
     *
     * @return a non-null array of bytes
     */
    Optional<byte[]> mediaKey();

    /**
     * Sets the media key of this provider
     *
     */
    void setMediaKey(byte[] bytes);

    /**
     * Sets the timestamp of the media key
     *
     */
    void setMediaKeyTimestamp(Long timestamp);

    /**
     * Returns the sha256 of this media
     *
     * @return a non-null array of bytes
     */
    Optional<byte[]> mediaSha256();

    /**
     * Sets the sha256 of the media in this provider
     *
     */
    void setMediaSha256(byte[] bytes);

    /**
     * Returns the sha256 of this encrypted media
     *
     * @return a non-null array of bytes
     */
    Optional<byte[]> mediaEncryptedSha256();

    /**
     * Sets the sha256 of the encrypted media in this provider
     *
     */
    void setMediaEncryptedSha256(byte[] bytes);

    /**
     * Returns the size of this media
     *
     * @return a long
     */
    OptionalLong mediaSize();

    /**
     * Sets the size of this media
     *
     */
    void setMediaSize(long mediaSize);


    /**
     * Returns the type of this attachment
     *
     * @return a non-null attachment
     */
    AttachmentType attachmentType();
}
