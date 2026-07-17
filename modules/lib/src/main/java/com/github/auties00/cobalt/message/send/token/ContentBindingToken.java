package com.github.auties00.cobalt.message.send.token;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import javax.crypto.KDF;
import javax.crypto.Mac;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;

/**
 * Generates per-recipient content-binding tags (RCAT) for outgoing URL messages.
 *
 * <p>The content-binding tag lets the server verify that a URL preview's
 * underlying link matches what the sender claims without learning the plaintext
 * URL. Each recipient gets a unique tag of {@value #TAG_LENGTH} bytes that the
 * fanout writer threads into the {@code content_binding} attribute of that
 * recipient's {@code <enc>} child whenever {@link #generate} returns a value for
 * the recipient.
 */
@WhatsAppWebModule(moduleName = "WAWebMsgRcatUtils")
@WhatsAppWebModule(moduleName = "WAWebUtilsYoutubeUrlParser")
public final class ContentBindingToken {
    /**
     * The logger for {@link ContentBindingToken}.
     */
    private static final System.Logger LOGGER = Log.get(ContentBindingToken.class);

    /**
     * The info suffix appended to the HKDF-Expand info parameter when deriving
     * the per-recipient nonce.
     */
    private static final String NONCE_INFO_SUFFIX = "Rcat";

    /**
     * The output length, in bytes, of the HKDF-derived nonce.
     */
    private static final int NONCE_LENGTH = 32;

    /**
     * The number of leading HMAC bytes kept as the content-binding tag.
     */
    private static final int TAG_LENGTH = 8;

    /**
     * The HKDF algorithm used for nonce derivation.
     */
    private static final String HKDF_ALGORITHM = "HKDF-SHA256";

    /**
     * The HMAC algorithm used for tag computation.
     */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * The fixed length of a YouTube video id.
     */
    private static final int YT_VIDEO_ID_LENGTH = 11;

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private ContentBindingToken() {
        throw new AssertionError();
    }

    /**
     * Resolves the content-id string from a matched URL text.
     *
     * <p>When the URL is a recognised YouTube link the {@value #YT_VIDEO_ID_LENGTH}-character
     * video id is returned; otherwise the full matched text is returned unless
     * {@code youtubeOnly} forces a {@code null}. Passing {@code youtubeOnly} as
     * {@code true} restricts the result to YouTube links and yields {@code null}
     * for every other URL shape.
     *
     * @param matchedText the matched URL text from the URL-message body
     * @param youtubeOnly when {@code true}, restricts the result to YouTube
     *                    video ids (returns {@code null} otherwise)
     * @return the content-id string, or {@code null} when {@code youtubeOnly}
     *         is {@code true} and the URL is not a recognised YouTube link
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgRcatUtils", exports = "getContentIdString",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static String getContentIdString(String matchedText, boolean youtubeOnly) {
        if (matchedText == null || matchedText.isEmpty()) {
            return null;
        }
        var videoId = parseYoutubeVideoId(matchedText);
        return (youtubeOnly || videoId != null) ? videoId : matchedText;
    }

    /**
     * Resolves the content-id from a matched URL text as UTF-8 bytes.
     *
     * <p>Delegates to {@link #getContentIdString(String, boolean)} with
     * {@code youtubeOnly = false} and encodes the result as UTF-8; the returned
     * bytes are the {@code contentId} input to {@link #generate}.
     *
     * @param matchedText the matched URL text from the URL-message body
     * @return the content-id bytes, or {@code null} when no content-id could
     *         be derived
     * @throws NullPointerException if {@code matchedText} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgRcatUtils", exports = "getContentIdString",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static byte[] resolveContentId(String matchedText) {
        Objects.requireNonNull(matchedText, "matchedText");
        var contentId = getContentIdString(matchedText, false);
        if (contentId == null) {
            return null;
        }
        return contentId.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Extracts the YouTube video id from a URL, or returns {@code null} when the
     * URL is not a recognised YouTube format.
     *
     * <p>Recognised input shapes (with optional {@code www.} and, for
     * {@code youtube.com}, {@code m.} subdomains):
     * {@snippet :
     *     http(s)://youtu.be/XXXXXXXXXXX
     *     http(s)://youtube.com/watch?v=XXXXXXXXXXX
     *     http(s)://youtube.com/shorts/XXXXXXXXXXX
     * }
     *
     * @implNote
     * This implementation walks the string directly instead of running the
     * regex set WA Web ships in {@code WAWebPipConst.URL_PATTERNS.ONLINE_VIDEO_URL.YOUTUBE},
     * because the YouTube id length and the path-prefix offsets are fixed; the
     * string-scan version is faster and allocation-free.
     *
     * @param url the URL to parse
     * @return the {@value #YT_VIDEO_ID_LENGTH}-character video id, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebUtilsYoutubeUrlParser", exports = "parseYoutubeVideoId",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static String parseYoutubeVideoId(String url) {
        if (url == null) {
            return null;
        }

        var stripped = url.contains("www.") ? url.replace("www.", "") : url;

        int hostStart;
        if (stripped.startsWith("https://")) {
            hostStart = 8;
        } else if (stripped.startsWith("http://")) {
            hostStart = 7;
        } else {
            return null;
        }

        if (stripped.startsWith("youtu.be/", hostStart)) {
            return extractVideoId(stripped, hostStart + 9);
        }

        if (stripped.startsWith("m.", hostStart)) {
            hostStart += 2;
        }

        if (!stripped.startsWith("youtube.com/", hostStart)) {
            return null;
        }
        var pathStart = hostStart + 12;

        if (stripped.startsWith("watch?v=", pathStart)) {
            return extractVideoId(stripped, pathStart + 8);
        }

        if (stripped.startsWith("shorts/", pathStart)) {
            return extractVideoId(stripped, pathStart + 7);
        }

        return null;
    }

    /**
     * Extracts exactly {@value #YT_VIDEO_ID_LENGTH} characters starting at
     * {@code offset}.
     *
     * <p>Returns {@code null} when {@code url} is too short to carry a complete
     * YouTube id from {@code offset}.
     *
     * @param url    the full URL string
     * @param offset the character offset where the video id starts
     * @return the video id substring, or {@code null}
     */
    private static String extractVideoId(String url, int offset) {
        if (offset + YT_VIDEO_ID_LENGTH > url.length()) {
            return null;
        }
        return url.substring(offset, offset + YT_VIDEO_ID_LENGTH);
    }

    /**
     * Generates the per-recipient content-binding tags for an outgoing URL
     * message.
     *
     * <p>Mints one tag per recipient and returns them keyed by recipient
     * {@link Jid}; each tag is folded into the recipient's
     * {@code <enc content_binding="..."/>} attribute. The result is empty when
     * {@code recipientJids} is empty and is otherwise unmodifiable. For each
     * recipient the per-recipient nonce from {@link #deriveNonce} is the HMAC
     * key over {@code contentId}, of which the leading {@value #TAG_LENGTH}
     * bytes form the tag.
     *
     * @param messageId     the outgoing message's stanza id (the {@code id}
     *                      attribute on {@code <message>})
     * @param messageSecret the 32-byte message secret
     * @param senderJid     the sender's user {@link Jid}
     * @param recipientJids the recipient user {@link Jid}s
     * @param contentId     the URL content-id (matched URL text or YouTube
     *                      video id) encoded as UTF-8
     * @return an unmodifiable map from recipient {@link Jid} to its
     *         {@value #TAG_LENGTH}-byte tag
     * @throws NullPointerException     if any argument is {@code null}
     * @throws GeneralSecurityException if a cryptographic primitive is
     *                                  unavailable
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgRcatUtils", exports = "genContentBindingForMsg",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static Map<Jid, byte[]> generate(
            String messageId,
            byte[] messageSecret,
            Jid senderJid,
            Collection<Jid> recipientJids,
            byte[] contentId
    ) throws GeneralSecurityException {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(messageSecret, "messageSecret");
        Objects.requireNonNull(senderJid, "senderJid");
        Objects.requireNonNull(recipientJids, "recipientJids");
        Objects.requireNonNull(contentId, "contentId");

        var result = new LinkedHashMap<Jid, byte[]>(recipientJids.size());
        for (var recipientJid : recipientJids) {
            var nonce = deriveNonce(messageId, messageSecret, senderJid, recipientJid);
            var tag = hmacTruncated(nonce, contentId);
            result.put(recipientJid, tag);
        }

        if (Log.TRACE) LOGGER.log(Level.TRACE, "content binding tags generated count={0}", result.size());
        return Collections.unmodifiableMap(result);
    }

    /**
     * Derives the per-recipient {@value #NONCE_LENGTH}-byte nonce via
     * HKDF-SHA-256 extract-and-expand.
     *
     * <p>The {@code messageSecret} is the input keying material, the extract
     * salt is implicit-zero, and the expand info parameter is the UTF-8 encoding
     * of {@code messageId || senderJid || recipientJid || "Rcat"}. The nonce is
     * the HMAC key in {@link #generate} and the seed for the URL-safe Base64
     * variant returned by {@link #deriveNonceString}.
     *
     * @param messageId     the message stanza id
     * @param messageSecret the 32-byte message secret used as input keying
     *                      material
     * @param senderJid     the sender's user {@link Jid}
     * @param recipientJid  the recipient's user {@link Jid}
     * @return a {@value #NONCE_LENGTH}-byte nonce
     * @throws GeneralSecurityException if HKDF-SHA-256 is unavailable
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgRcatUtils", exports = "deriveNonce",
            adaptation = WhatsAppAdaptation.DIRECT)
    static byte[] deriveNonce(
            String messageId,
            byte[] messageSecret,
            Jid senderJid,
            Jid recipientJid
    ) throws GeneralSecurityException {
        var info = (messageId + senderJid + recipientJid + NONCE_INFO_SUFFIX)
                .getBytes(StandardCharsets.UTF_8);

        var kdf = KDF.getInstance(HKDF_ALGORITHM);
        var params = HKDFParameterSpec.ofExtract()
                .addIKM(messageSecret)
                .thenExpand(info, NONCE_LENGTH);
        return kdf.deriveData(params);
    }

    /**
     * Derives the per-recipient nonce and returns it as a URL-safe Base64 string
     * with padding.
     *
     * <p>Used when the nonce from {@link #deriveNonce} must cross a string-only
     * boundary, such as logging or a debug surface, rather than be consumed as
     * raw bytes by an HMAC.
     *
     * @param messageId     the message stanza id
     * @param messageSecret the 32-byte message secret used as input keying
     *                      material
     * @param senderJid     the sender's user {@link Jid}
     * @param recipientJid  the recipient's user {@link Jid}
     * @return the nonce as a URL-safe Base64 string with padding
     * @throws GeneralSecurityException if HKDF-SHA-256 is unavailable
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgRcatUtils", exports = "deriveNonceString",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static String deriveNonceString(
            String messageId,
            byte[] messageSecret,
            Jid senderJid,
            Jid recipientJid
    ) throws GeneralSecurityException {
        var nonce = deriveNonce(messageId, messageSecret, senderJid, recipientJid);
        return Base64.getUrlEncoder().encodeToString(nonce);
    }

    /**
     * Computes HMAC-SHA-256 of {@code data} keyed by {@code key}, truncated to
     * the first {@value #TAG_LENGTH} bytes.
     *
     * @param key  the HMAC key (the derived nonce)
     * @param data the data to authenticate (the content id)
     * @return the first {@value #TAG_LENGTH} bytes of the HMAC
     * @throws GeneralSecurityException if the HMAC algorithm is unavailable
     */
    private static byte[] hmacTruncated(byte[] key, byte[] data) throws GeneralSecurityException {
        var mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
        var full = mac.doFinal(data);
        return Arrays.copyOf(full, TAG_LENGTH);
    }
}
