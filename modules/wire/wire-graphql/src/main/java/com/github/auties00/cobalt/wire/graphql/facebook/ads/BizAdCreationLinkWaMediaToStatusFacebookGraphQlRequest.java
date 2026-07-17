package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds the Facebook GraphQL mutation that links uploaded WhatsApp media to a click-to-WhatsApp native ad,
 * registering it on the status surface.
 *
 * <p>The mutation takes a single {@code input} GraphQL variable. WhatsApp Web's
 * {@code WAWebBizAdCreationLinkWAMediaToStatus} builds it as {@code {media_list: [{id, type}, ...]}},
 * mapping each uploaded medium's Facebook id ({@code fbid}) and ad media kind to an entry; the kind
 * is rendered as the literal {@code "IMAGE"} or {@code "VIDEO"}. The Meta graph endpoint returns the registered
 * media under {@code xfb_ctwa_native_upload_ad_media}; the reply is consumed through
 * {@link BizAdCreationLinkWaMediaToStatusFacebookGraphQlResponse}.
 *
 * @see BizAdCreationLinkWaMediaToStatusFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationLinkWAMediaToStatusMutation")
public final class BizAdCreationLinkWaMediaToStatusFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationLinkWAMediaToStatusMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25164732633213778";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationLinkWAMediaToStatusMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAdCreationLinkWAMediaToStatusMutation";

    /**
     * The media entries emitted as the {@code media_list} array of the {@code input} object, or
     * {@code null} to omit the array.
     */
    private final List<MediaEntry> mediaList;

    /**
     * Constructs a link-WhatsApp-media-to-status mutation request.
     *
     * <p>The {@code mediaList} populates the {@code media_list} array of the {@code input} object; a
     * {@code null} value omits the array from the serialized object.
     *
     * @param mediaList the media entries to link, or {@code null} to omit the array
     */
    public BizAdCreationLinkWaMediaToStatusFacebookGraphQlRequest(List<MediaEntry> mediaList) {
        this.mediaList = mediaList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String docId() {
        return DOC_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation emits {@code {"input": {"media_list": [{"id": <id>, "type":
     * <type>}, ...]}}}, writing each entry's id and type only when non-null and emitting
     * {@code {"input": {}}} when {@code mediaList} is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationLinkWAMediaToStatus", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (mediaList != null) {
                writer.writeName("media_list");
                writer.writeColon();
                writer.startArray();
                for (var index = 0; index < mediaList.size(); index++) {
                    if (index > 0) {
                        writer.writeComma();
                    }
                    var entry = mediaList.get(index);
                    writer.startObject();
                    if (entry.id() != null) {
                        writer.writeName("id");
                        writer.writeColon();
                        writer.writeString(entry.id());
                    }

                    if (entry.type() != null) {
                        writer.writeName("type");
                        writer.writeColon();
                        writer.writeString(entry.type().wireValue());
                    }
                    writer.endObject();
                }
                writer.endArray();
            }
            writer.endObject();
            writer.endObject();
            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return output.toString();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * Models one entry of the {@code media_list} array: the Facebook id of an uploaded medium paired
     * with its ad media kind.
     *
     * <p>WhatsApp Web derives each entry from an uploaded medium's {@code fbid} and its
     * {@code MEDIA_TYPES} kind, narrowing the kind to {@link MediaType}.
     */
    public static final class MediaEntry {
        /**
         * Holds the Facebook id of the uploaded medium.
         */
        private final String id;

        /**
         * Holds the ad media kind.
         */
        private final MediaType type;

        /**
         * Constructs a media entry from the Facebook id and the ad media kind.
         *
         * @param id   the Facebook id of the uploaded medium, or {@code null} to omit the field
         * @param type the ad media kind, or {@code null} to omit the field
         */
        public MediaEntry(String id, MediaType type) {
            this.id = id;
            this.type = type;
        }

        /**
         * Returns the Facebook id of the uploaded medium.
         *
         * @return the Facebook id, or {@code null} when none was supplied
         */
        public String id() {
            return id;
        }

        /**
         * Returns the ad media kind.
         *
         * @return the ad media kind, or {@code null} when none was supplied
         */
        public MediaType type() {
            return type;
        }
    }

    /**
     * Enumerates the ad media kinds carried by a {@link MediaEntry}.
     *
     * <p>The two constants are the complete set WhatsApp Web's
     * {@code WAWebBizAdCreationLinkWAMediaToStatus} renders: it maps
     * {@code MEDIA_TYPES.NATIVE_AD_IMAGE} to {@code "IMAGE"} and {@code MEDIA_TYPES.NATIVE_AD_VIDEO}
     * to {@code "VIDEO"} and rejects every other kind, so no further constants are reachable on the
     * wire.
     */
    public enum MediaType {
        /**
         * The native-ad image kind, rendered on the wire as {@code "IMAGE"}.
         */
        IMAGE("IMAGE"),

        /**
         * The native-ad video kind, rendered on the wire as {@code "VIDEO"}.
         */
        VIDEO("VIDEO");

        /**
         * Holds the wire token emitted for this kind.
         */
        private final String wireValue;

        /**
         * Constructs an ad media kind bound to its wire token.
         *
         * @param wireValue the wire token emitted for this kind
         */
        MediaType(String wireValue) {
            this.wireValue = wireValue;
        }

        /**
         * Returns the wire token emitted for this kind.
         *
         * @return the wire token, never {@code null}
         */
        public String wireValue() {
            return wireValue;
        }

        /**
         * Resolves an ad media kind from its wire token.
         *
         * <p>The lookup is lenient: a token outside the closed {@code "IMAGE"}/{@code "VIDEO"} set,
         * or a {@code null} token, resolves to {@link Optional#empty()} rather than raising.
         *
         * @param wireValue the wire token to resolve, may be {@code null}
         * @return the matching kind, or empty when the token is {@code null} or unrecognised
         */
        public static Optional<MediaType> ofWireValue(String wireValue) {
            for (var value : values()) {
                if (Objects.equals(value.wireValue, wireValue)) {
                    return Optional.of(value);
                }
            }
            return Optional.empty();
        }
    }
}
