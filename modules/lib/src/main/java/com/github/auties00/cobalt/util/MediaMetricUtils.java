package com.github.auties00.cobalt.util;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.github.auties00.cobalt.wire.core.util.RandomIdUtils;

import com.github.auties00.cobalt.exception.linked.WhatsAppMediaException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.wam.event.WebcMediaErrorUnknownDetailsEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.BackendStoreType;
import com.github.auties00.cobalt.wire.wam.type.MediaDownloadModeType;
import com.github.auties00.cobalt.wire.wam.type.MediaDownloadResultType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MediaUploadModeType;
import com.github.auties00.cobalt.wire.wam.type.MediaUploadResultType;
import com.github.auties00.cobalt.wire.wam.type.WebcMediaOperationCode;
import com.github.auties00.cobalt.wam.WamService;

import java.lang.System.Logger.Level;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Classifies media-pipeline state into the WAM enums consumed by the
 * {@code MediaUpload2Event} and {@code MediaDownload2Event} WAM beacons.
 *
 * <p>The media-upload and media-download sites feed these classifiers so the
 * WAM events they emit carry the same overall-mode, overall-result, media-type,
 * backend-store, and event-id values that WhatsApp Web's own beacons carry. The
 * classifiers absorb the {@code instanceof} cascades in
 * {@code WAWebWamMediaMetricUtils.getMetricUploadErrorResultType} /
 * {@code getMetricDownloadErrorResultType} against the Cobalt exception
 * hierarchy.
 */
@WhatsAppWebModule(moduleName = "WAWebWamMediaMetricUtils")
public final class MediaMetricUtils {
    /**
     * The logger for {@link MediaMetricUtils}.
     */
    private static final System.Logger LOGGER = Log.get(MediaMetricUtils.class);

    /**
     * Holds the message prefix produced by Cobalt's
     * {@code MediaDownloadInputStream} when the downloaded ciphertext SHA-256
     * disagrees with the expected {@code encFilehash}.
     *
     * <p>{@link #getMetricDownloadErrorResultType(Throwable)} matches against
     * this prefix to recognise Cobalt's equivalent of WA Web's
     * {@code MmsDownloadFilehashMismatchError} and route the
     * {@link MediaDownloadResultType#ERROR_ENC_HASH_MISMATCH} bucket.
     */
    @WhatsAppWebExport(moduleName = "WAWebHttpErrors", exports = "MmsDownloadFilehashMismatchError",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static final String CIPHERTEXT_HASH_MISMATCH_MESSAGE =
            "Ciphertext SHA256 hash doesn't match the expected value";

    /**
     * Holds the message prefix produced by Cobalt's
     * {@code MediaDownloadInputStream} when the decrypted plaintext SHA-256
     * disagrees with the expected {@code fileSha256}.
     *
     * <p>{@link #getMetricDownloadErrorResultType(Throwable)} matches against
     * this prefix to recognise Cobalt's equivalent of WA Web's
     * {@code MediaDecryptionError} carrying {@code PLAINTEXT_HASH_MISMATCH_ERROR}
     * and route the {@link MediaDownloadResultType#ERROR_HASH_MISMATCH} bucket.
     */
    @WhatsAppWebExport(moduleName = "WAWebMiscErrors", exports = "MediaDecryptionError",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebMiscErrors", exports = "PLAINTEXT_HASH_MISMATCH_ERROR",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static final String PLAINTEXT_HASH_MISMATCH_MESSAGE =
            "Plaintext SHA256 hash doesn't match the expected value";

    /**
     * Prevents instantiation of this utility class.
     *
     * <p>The class is a pure helpers holder; instances carry no state.
     *
     * @throws AssertionError always
     */
    private MediaMetricUtils() {
        throw new AssertionError("MediaMetricUtils is a static-only utility");
    }

    /**
     * Returns whether the given web-media-type string represents a thumbnail
     * variant.
     *
     * <p>Short-circuits both
     * {@link #getMetricOverallDownloadModeType(String, String, boolean)} and
     * {@link #getMetricOverallUploadModeType(String)} to the thumbnail mode-type
     * bucket. The check is true for any of the five thumbnail variants.
     *
     * @param webMediaType the {@code WAWebMmsMediaTypes.MEDIA_TYPES} string
     *                     identifying the media variant
     * @return {@code true} if the value is one of the five thumbnail variants
     */
    private static boolean isThumbnailMediaType(String webMediaType) {
        return "thumbnail-document".equals(webMediaType)
                || "thumbnail-image".equals(webMediaType)
                || "thumbnail-video".equals(webMediaType)
                || "thumbnail-link".equals(webMediaType)
                || "newsletter-thumbnail-link".equals(webMediaType);
    }

    /**
     * Maps the triple {@code (webMediaType, downloadReason, isPrefetched)} to a
     * {@link MediaDownloadModeType}.
     *
     * <p>Drives the {@code overallDownloadMode} field of a media-download WAM
     * beacon, matching WA Web's classification: thumbnails always collapse to
     * {@link MediaDownloadModeType#THUMBNAIL}; an explicit {@code "manual"}
     * download reason wins next; then a truthy {@code isPrefetched} flag;
     * otherwise the call falls back to {@link MediaDownloadModeType#FULL}.
     *
     * @param webMediaType   the {@code WAWebMmsMediaTypes.MEDIA_TYPES} string
     *                       describing the media being downloaded
     * @param downloadReason the {@code "manual"} or non-{@code "manual"} reason
     *                       passed by the caller
     * @param isPrefetched   {@code true} when the download is part of an
     *                       autodownload prefetch cycle
     * @return the matching {@link MediaDownloadModeType}
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMediaMetricUtils",
            exports = "getMetricOverallDownloadModeType",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static MediaDownloadModeType getMetricOverallDownloadModeType(
            String webMediaType,
            String downloadReason,
            boolean isPrefetched
    ) {
        if (isThumbnailMediaType(webMediaType)) {
            return MediaDownloadModeType.THUMBNAIL;
        }
        if ("manual".equals(downloadReason)) {
            return MediaDownloadModeType.MANUAL;
        }
        if (isPrefetched) {
            return MediaDownloadModeType.PREFETCH;
        }
        return MediaDownloadModeType.FULL;
    }

    /**
     * Maps a web-media-type string to a {@link MediaUploadModeType}.
     *
     * <p>Drives the coarse {@code overallUploadMode} field of a media-upload WAM
     * beacon, matching WA Web's classification. The finer-grained
     * {@code FAST_FORWARD_EXIST_CHECK} / {@code WEB_REUPLOAD} values are decided
     * at the call site, not here.
     *
     * @param webMediaType the {@code WAWebMmsMediaTypes.MEDIA_TYPES} string
     *                     describing the media being uploaded
     * @return {@link MediaUploadModeType#THUMBNAIL} for thumbnail variants,
     *         {@link MediaUploadModeType#REGULAR} otherwise
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMediaMetricUtils",
            exports = "getMetricOverallUploadModeType",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static MediaUploadModeType getMetricOverallUploadModeType(String webMediaType) {
        if (isThumbnailMediaType(webMediaType)) {
            return MediaUploadModeType.THUMBNAIL;
        }
        return MediaUploadModeType.REGULAR;
    }

    /**
     * Maps a {@code WAWebMmsMediaTypes.MEDIA_TYPES} string to the matching
     * {@link MediaType} reported on WAM media events.
     *
     * <p>Projects a media-pipeline media-type slug into the {@code mediaType}
     * field of a media WAM beacon. The mapping is exhaustive over WA Web's MMS
     * media-type enum and mirrors the JS {@code switch} cascade verbatim,
     * including the cases where several input strings collapse to the same WAM
     * enum (for example {@code "image"}, {@code "waffle-image"},
     * {@code "thumbnail-image"} and {@code "newsletter-image"} all fold to
     * {@link MediaType#PHOTO}).
     *
     * @implNote
     * This implementation throws {@link IllegalArgumentException} with the same
     * {@code "webMediaType is invalid: <value>"} message format the JS
     * {@code err()} helper uses.
     *
     * @param webMediaType the {@code WAWebMmsMediaTypes.MEDIA_TYPES} string to
     *                     translate, must not be {@code null}
     * @return the matching {@link MediaType} value
     * @throws IllegalArgumentException when {@code webMediaType} does not
     *                                  correspond to any known
     *                                  {@code WAWebMmsMediaTypes.MEDIA_TYPES}
     *                                  value
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMediaMetricUtils",
            exports = "getMetricMediaType",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static MediaType getMetricMediaType(String webMediaType) {
        Objects.requireNonNull(webMediaType, "webMediaType cannot be null");
        return switch (webMediaType) {
            case "audio", "newsletter-audio" -> MediaType.AUDIO;
            case "document", "thumbnail-document" -> MediaType.DOCUMENT;
            case "gif", "newsletter-gif" -> MediaType.GIF;
            case "image", "waffle-image", "newsletter-image", "thumbnail-image" -> MediaType.PHOTO;
            case "ppic" -> MediaType.PROFILE_PIC;
            case "product", "product-catalog-image" -> MediaType.PRODUCT_IMAGE;
            case "ptt", "newsletter-ptt" -> MediaType.PTT;
            case "sticker", "newsletter-sticker" -> MediaType.STICKER;
            case "sticker-pack", "newsletter-sticker-pack", "thumbnail-sticker-pack" -> MediaType.STICKER_PACK;
            case "video", "newsletter-video", "waffle-video", "thumbnail-video" -> MediaType.VIDEO;
            case "ptv", "newsletter-ptv" -> MediaType.PUSH_TO_VIDEO;
            case "template" -> MediaType.TEMPLATE;
            case "md-msg-hist" -> MediaType.MD_HISTORY_SYNC;
            case "md-app-state" -> MediaType.MD_APP_STATE;
            case "thumbnail-link", "newsletter-thumbnail-link" -> MediaType.URL;
            case "music-artwork", "newsletter-music-artwork" -> MediaType.MUSIC_ARTWORK;
            case "payment-bg-image", "biz-cover-photo", "ads-image", "ads-video", "group-history" -> MediaType.NONE;
            default -> {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "unknown media type: {0}", webMediaType);
                throw new IllegalArgumentException("webMediaType is invalid: " + webMediaType);
            }
        };
    }

    /**
     * Classifies a thrown upload error into a {@link MediaUploadResultType}.
     *
     * <p>Drives the {@code overallUploadResult} field of a media-upload WAM
     * beacon, picking the same bucket WA Web would have picked. The lookup
     * mirrors WA Web's {@code instanceof} cascade against the Cobalt exception
     * hierarchy: {@link InterruptedException} folds to
     * {@link MediaUploadResultType#ERROR_CANCEL},
     * {@link WhatsAppMediaException.Connection} folds to
     * {@link MediaUploadResultType#ERROR_MEDIA_CONN}, and an
     * {@link WhatsAppMediaException.Upload} with a status code fans out by code:
     * 401 -> {@link MediaUploadResultType#ERROR_NO_PERMISSIONS},
     * 413 -> {@link MediaUploadResultType#ERROR_BAD_MEDIA},
     * 507 -> {@link MediaUploadResultType#ERROR_THROTTLE}, then
     * 5xx -> {@link MediaUploadResultType#ERROR_SERVER}.
     *
     * @implNote
     * This implementation reads the status code first because WA Web's error
     * classes are not disjoint ({@code MMSThrottleError} extends
     * {@code HttpStatusCodeError(507)} and {@code MediaTooLargeError} extends
     * {@code HttpStatusCodeError(413)}). Cobalt collapses the six
     * {@code WAWebMmsClientErrors} subclasses into a single
     * {@link WhatsAppMediaException.Upload} carrying an optional status code so
     * the same priority ordering is preserved by switching on the code first.
     *
     * @param throwable the error that aborted the upload, or {@code null}
     * @return the matching {@link MediaUploadResultType}, never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMediaMetricUtils",
            exports = "getMetricUploadErrorResultType",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static MediaUploadResultType getMetricUploadErrorResultType(Throwable throwable) {
        for (var current = throwable; current != null; current = nextCause(current)) {
            if (current instanceof InterruptedException) {
                return MediaUploadResultType.ERROR_CANCEL;
            }
            if (current instanceof WhatsAppMediaException.Connection) {
                return MediaUploadResultType.ERROR_MEDIA_CONN;
            }
            if (current instanceof WhatsAppMediaException.Upload upload) {
                var status = upload.httpStatusCode();
                if (status.isPresent()) {
                    var code = status.getAsInt();
                    return switch (code) {
                        case 401 -> MediaUploadResultType.ERROR_NO_PERMISSIONS;
                        case 413 -> MediaUploadResultType.ERROR_BAD_MEDIA;
                        case 507 -> MediaUploadResultType.ERROR_THROTTLE;
                        default -> {
                            if (code >= 500) {
                                yield MediaUploadResultType.ERROR_SERVER;
                            }
                            yield MediaUploadResultType.ERROR_UNKNOWN;
                        }
                    };
                }
                return MediaUploadResultType.ERROR_UPLOAD;
            }
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "media upload error classified as unknown, cause={0}",
                    throwable == null ? "none" : throwable.getClass().getSimpleName());
        }
        return MediaUploadResultType.ERROR_UNKNOWN;
    }

    /**
     * Classifies a thrown download error into a {@link MediaDownloadResultType}.
     *
     * <p>Drives the {@code overallDownloadResult} field of a media-download WAM
     * beacon, picking the same bucket WA Web would have picked. Throttle
     * detection wins over generic status-code handling because
     * {@code MMSThrottleError} extends {@code HttpStatusCodeError(507)};
     * media-host failures map to
     * {@link MediaDownloadResultType#ERROR_MEDIA_CONN}; generic network failures
     * (no status) fold to {@link MediaDownloadResultType#ERROR_NETWORK}; then the
     * explicit status switch covers 404, 410, 416, 401, 429, 507; and the cancel
     * plus hash-mismatch tail branches handle the Cobalt-side stream variants.
     *
     * @implNote
     * This implementation recognises the hash-mismatch flavours by message-prefix
     * match against {@link #CIPHERTEXT_HASH_MISMATCH_MESSAGE} and
     * {@link #PLAINTEXT_HASH_MISMATCH_MESSAGE}, both of which Cobalt's
     * {@code MediaDownloadInputStream} attaches to
     * {@link WhatsAppMediaException.Download}; a plaintext mismatch raised
     * outside the streaming download path surfaces as
     * {@link WhatsAppMediaException.Processing} and is matched in the same way.
     *
     * @param throwable the error raised by the download path, or {@code null}
     * @return the matching {@link MediaDownloadResultType}, never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMediaMetricUtils",
            exports = "getMetricDownloadErrorResultType",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static MediaDownloadResultType getMetricDownloadErrorResultType(Throwable throwable) {
        for (var current = throwable; current != null; current = nextCause(current)) {
            if (current instanceof InterruptedException) {
                return MediaDownloadResultType.ERROR_CANCEL;
            }
            if (current instanceof WhatsAppMediaException.Connection) {
                return MediaDownloadResultType.ERROR_MEDIA_CONN;
            }
            if (current instanceof WhatsAppMediaException.Download download) {
                var status = download.httpStatusCode();
                if (status.isPresent()) {
                    var code = status.getAsInt();
                    return switch (code) {
                        case 429, 507 -> MediaDownloadResultType.ERROR_THROTTLE;
                        case 404, 410 -> MediaDownloadResultType.ERROR_TOO_OLD;
                        case 416 -> MediaDownloadResultType.ERROR_CANNOT_RESUME;
                        case 401 -> MediaDownloadResultType.ERROR_INVALID_URL;
                        default -> MediaDownloadResultType.ERROR_UNKNOWN;
                    };
                }
                var message = download.getMessage();
                if (message != null) {
                    if (message.startsWith(CIPHERTEXT_HASH_MISMATCH_MESSAGE)) {
                        return MediaDownloadResultType.ERROR_ENC_HASH_MISMATCH;
                    }
                    if (message.startsWith(PLAINTEXT_HASH_MISMATCH_MESSAGE)) {
                        return MediaDownloadResultType.ERROR_HASH_MISMATCH;
                    }
                }
                return MediaDownloadResultType.ERROR_NETWORK;
            }
            if (current instanceof WhatsAppMediaException.Processing processing) {
                var message = processing.getMessage();
                if (message != null && message.startsWith(PLAINTEXT_HASH_MISMATCH_MESSAGE)) {
                    return MediaDownloadResultType.ERROR_HASH_MISMATCH;
                }
            }
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "media download error classified as unknown, cause={0}",
                    throwable == null ? "none" : throwable.getClass().getSimpleName());
        }
        return MediaDownloadResultType.ERROR_UNKNOWN;
    }

    /**
     * Returns the HTTP status code carried by the first
     * {@link WhatsAppMediaException} in the cause chain of {@code throwable} that
     * has one attached.
     *
     * <p>Fills the {@code httpCode} field of a media WAM beacon when the upload
     * or download leg fails. Returns {@code null} when no exception in the chain
     * has a status (the connection never made it that far).
     *
     * @param throwable the error to inspect, or {@code null}
     * @return the HTTP status code, or {@code null} if none of the exceptions in
     *         the chain carries one
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMediaMetricUtils",
            exports = "getStatusCode",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Integer getStatusCode(Throwable throwable) {
        for (var current = throwable; current != null; current = nextCause(current)) {
            if (current instanceof WhatsAppMediaException mediaException) {
                var status = mediaException.httpStatusCode();
                if (status.isPresent()) {
                    return status.getAsInt();
                }
            }
        }
        return null;
    }

    /**
     * Returns a random media-event identifier in the closed range
     * {@code [1, 2^53 - 1]}.
     *
     * <p>The value is the correlation identifier on {@code MediaUpload2Event},
     * {@code MediaDownload2Event} and {@code WebcMediaErrorUnknownDetailsEvent}
     * so the server can deduplicate retried attempts. The range matches
     * JavaScript's {@code Number.MAX_SAFE_INTEGER}
     * ({@code 2^53 - 1 = 9007199254740991}) exactly.
     *
     * @implNote
     * This implementation draws via {@link ThreadLocalRandom#nextLong(long)}
     * rather than {@link DataUtils#randomLong(long)} because the value is
     * non-cryptographic and is generated on hot paths.
     *
     * @return a random {@code long} in the closed range
     *         {@code [1, 9007199254740991]}
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMediaMetricUtils",
            exports = "generateMediaEventId",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static long generateMediaEventId() {
        return 1L + ThreadLocalRandom.current().nextLong(9007199254740991L);
    }

    /**
     * Maps a media direct-path string to the matching {@link BackendStoreType}
     * by inspecting the first two characters of the path.
     *
     * <p>Fills the {@code backendStore} field of a media WAM beacon. {@code /v},
     * {@code /o} and {@code /m} mark Everstore, OIL and Manifold blob stores
     * respectively; an empty or {@code null} path signals that the asset is not
     * using a direct-path scheme and folds to
     * {@link BackendStoreType#NON_DIRECT_PATH}; unknown prefixes return
     * {@code null}.
     *
     * @implNote
     * This implementation caps the slice length at {@code path.length()} before
     * lowercasing because Java's {@code substring} throws on out-of-range slices,
     * whereas the WA Web {@code t.slice(0,2)} silently accepts shorter inputs.
     *
     * @param directPath the {@code direct_path} string handed back by the
     *                   media-upload server, or {@code null}
     * @return the matching {@link BackendStoreType}, or {@code null} when the
     *         prefix is unrecognised
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMediaMetricUtils",
            exports = "getMetricBackendStore",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static BackendStoreType getMetricBackendStore(String directPath) {
        if (directPath == null || directPath.isEmpty()) {
            return BackendStoreType.NON_DIRECT_PATH;
        }
        var prefix = directPath.substring(0, Math.min(2, directPath.length()))
                .toLowerCase(Locale.ROOT);
        return switch (prefix) {
            case "/v" -> BackendStoreType.EVERSTORE;
            case "/o" -> BackendStoreType.OIL;
            case "/m" -> BackendStoreType.MANIFOLD;
            default -> null;
        };
    }

    /**
     * Commits a {@code WebcMediaErrorUnknownDetailsWamEvent} when the outer
     * {@link MediaUploadResultType} or {@link MediaDownloadResultType} is
     * {@code ERROR_UNKNOWN}.
     *
     * <p>Called from the same site that emits the outer media-upload or
     * media-download WAM beacon. The follow-up event lets the server correlate
     * the unclassified error bucket back to the throwable's class name and
     * message so the WA backend can build a dictionary of unknown failures.
     * Emission is skipped when {@code throwable} is {@code null} or neither
     * result field is {@code ERROR_UNKNOWN}.
     *
     * @implNote
     * This implementation prioritises {@code DOWNLOAD} over {@code UPLOAD} when
     * both result fields are unknown, matching the JS conditional that sets the
     * operation to {@code DOWNLOAD} first and only overrides to {@code UPLOAD}
     * when the download branch did not match.
     *
     * @param wamService     the WAM service used to commit the event when
     *                       triggered, must not be {@code null}
     * @param mediaId        the media-event identifier, propagated as
     *                       {@code mediaId}, or {@code null} to omit the field
     * @param downloadResult the overall download result, or {@code null} when
     *                       this call site is upload-only
     * @param uploadResult   the overall upload result, or {@code null} when this
     *                       call site is download-only
     * @param throwable      the original error that caused the unknown
     *                       classification, or {@code null} to skip emission
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMediaMetricUtils",
            exports = "logErrorUnknownDetails",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static void logErrorUnknownDetails(
            WamService wamService,
            Long mediaId,
            MediaDownloadResultType downloadResult,
            MediaUploadResultType uploadResult,
            Throwable throwable
    ) {
        Objects.requireNonNull(wamService, "wamService cannot be null");
        if (throwable == null) {
            return;
        }
        WebcMediaOperationCode operation = null;
        if (downloadResult == MediaDownloadResultType.ERROR_UNKNOWN) {
            operation = WebcMediaOperationCode.DOWNLOAD;
        } else if (uploadResult == MediaUploadResultType.ERROR_UNKNOWN) {
            operation = WebcMediaOperationCode.UPLOAD;
        }
        if (operation == null) {
            return;
        }
        var builder = new WebcMediaErrorUnknownDetailsEventBuilder()
                .webcMediaOperation(operation)
                .webcMediaErrorName(throwable.getClass().getSimpleName())
                .webcMediaErrorMessage(throwable.getMessage());
        if (mediaId != null) {
            // FIXME: mediaId is a JS double in [1, MAX_SAFE_INTEGER] but the WAM property is INTEGER; values above Integer.MAX_VALUE silently truncate.
            builder.mediaId((int) (long) mediaId);
        }
        if (Log.WARNING) {
            LOGGER.log(Level.WARNING, "committing media error unknown-details event, operation={0}, error={1}",
                    operation, throwable.getClass().getSimpleName());
        }
        wamService.commit(builder.build());
    }

    /**
     * Returns the next exception in the cause chain of {@code throwable},
     * breaking self-cycles.
     *
     * <p>Every classifier in this class that walks the cause chain advances
     * through this helper to stay robust against an exception that lists itself
     * as its own cause.
     *
     * @param throwable the current throwable, must not be {@code null}
     * @return the cause, or {@code null} when the chain ends or self-references
     */
    private static Throwable nextCause(Throwable throwable) {
        var cause = throwable.getCause();
        return cause == throwable ? null : cause;
    }
}
