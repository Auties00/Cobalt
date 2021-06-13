package it.auties.whatsapp4j.media;

import it.auties.whatsapp4j.binary.BinaryArray;
import it.auties.whatsapp4j.protobuf.message.model.MediaMessageType;
import lombok.NonNull;

/**
 * An immutable model class that represents an upload request
 *
 * @param url the non null upload url
 * @param directPath the non null direct upload path
 * @param mediaKey the non null media key
 * @param file the uploaded file
 * @param fileSha256 the sha256 of the uploaded file
 * @param fileEncSha256 the sha256 of the encoded file
 * @param sidecar the sidecar of the uploaded file
 * @param mediaType the type of media
 */
public record MediaUpload(@NonNull String url, @NonNull String directPath,
                          @NonNull BinaryArray mediaKey, byte[] file,
                          byte[] fileSha256, byte[] fileEncSha256, byte[] sidecar,
                          @NonNull MediaMessageType mediaType) {
}
