package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.util.Locale;
import java.util.Optional;

/**
 * The high-level media kind carried by the {@code type} attribute on the CTWA native-ad upload-media stanzas.
 * Classifies the asset advertised by a {@link SmaxUploadAdMediaMediaEntry} on both the outbound
 * {@link SmaxUploadAdMediaRequest} {@code <media/>} and {@code <media_list/>} children and the inbound
 * {@link SmaxUploadAdMediaResponse.Success} echoes. Only the two values {@link #IMAGE} and {@link #VIDEO}
 * are defined; the link-in-Facebook debug action always uploads {@link #IMAGE} blobs.
 *
 * @implNote
 * This implementation maps Java's uppercase constant names to the lowercase wire literals via {@link #wire()}
 * and back via a case-sensitive {@code switch} in {@link #of(String)}, mirroring the JS dictionary lookup
 * against {@code ENUM_IMAGE_VIDEO}.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaNativeAdEnums")
@WhatsAppWebExport(
        moduleName = "WASmaxInBizCtwaNativeAdEnums",
        exports = "ENUM_IMAGE_VIDEO",
        adaptation = WhatsAppAdaptation.ADAPTED
)
public enum SmaxUploadAdMediaMediaType {
    /**
     * Denotes a still-image ad asset.
     * Carries the wire literal {@code "image"}.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizCtwaNativeAdEnums",
            exports = "ENUM_IMAGE_VIDEO",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    IMAGE,
    /**
     * Denotes a video ad asset.
     * Carries the wire literal {@code "video"}.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizCtwaNativeAdEnums",
            exports = "ENUM_IMAGE_VIDEO",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    VIDEO;

    /**
     * Returns the wire literal for this constant.
     * The returned value is stamped onto the {@code type} attribute of the outbound {@code <media/>} or
     * {@code <media_list/>} child built by {@link SmaxUploadAdMediaRequest#toStanza()}.
     *
     * @implNote
     * This implementation derives the literal by lowercasing {@link #name()} with {@link Locale#ROOT}, which
     * is exact for the two documented values.
     *
     * @return the lowercase enum name; never {@code null}
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizCtwaNativeAdEnums",
            exports = "ENUM_IMAGE_VIDEO",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    public String wire() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the constant whose {@link #wire()} value equals {@code value}.
     * Used when decoding the {@code <media/>} and {@code <media_list/>} echoes by
     * {@link SmaxUploadAdMediaResponse.Success}. The lookup is case-sensitive: any literal outside the
     * documented {@code {"image","video"}} dictionary returns {@link Optional#empty()} and fails the parse
     * upstream.
     *
     * @param value the candidate wire literal; may be {@code null}
     * @return an {@link Optional} carrying the matching constant, or {@link Optional#empty()} when
     *         {@code value} is {@code null} or unknown
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizCtwaNativeAdEnums",
            exports = "ENUM_IMAGE_VIDEO",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    public static Optional<SmaxUploadAdMediaMediaType> of(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return switch (value) {
            case "image" -> Optional.of(IMAGE);
            case "video" -> Optional.of(VIDEO);
            default -> Optional.empty();
        };
    }
}
