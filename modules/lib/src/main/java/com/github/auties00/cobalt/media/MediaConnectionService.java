package com.github.auties00.cobalt.media;

import com.github.auties00.cobalt.exception.linked.WhatsAppMediaException;
import com.github.auties00.cobalt.wire.linked.media.MediaPath;
import com.github.auties00.cobalt.wire.linked.media.MediaProvider;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.io.InputStream;
import java.util.SequencedCollection;

/**
 * Service that obtains a WhatsApp {@code media_conn} handshake and uses it
 * to upload, download, probe, and delete attachments through WhatsApp's
 * CDN.
 *
 * <p>A media connection is the handshake the client must obtain before
 * transferring any attachment: it carries the authentication token, the
 * list of candidate CDN hosts with their accepted media types and download
 * buckets, the time-to-live values for the credentials, and the retry
 * budgets the server expects. {@link #update(Stanza)} installs a fresh
 * handshake; the transfer methods consume the most recently installed one.
 * This service is the entry point whenever Cobalt ships or materialises a
 * media-bearing message: {@link #upload(MediaProvider, MediaPayload)} for
 * outbound attachments, {@link #download(MediaProvider)} for inbound ones. {@link LiveMediaConnectionService}
 * is the production implementation that talks to the live CDN.
 *
 * @implSpec
 * Implementations are expected to be thread-safe; the success-stream
 * handler may call {@link #update(Stanza)} concurrently with sender and
 * receiver pipelines invoking the transfer methods. The transfer methods
 * and the credential accessors require at least one {@link #update(Stanza)}
 * to have landed first; {@link LiveMediaConnectionService} blocks the
 * transfer methods on a first-refresh latch and throws
 * {@link IllegalStateException} from the accessors until then.
 */
public interface MediaConnectionService {
    /**
     * Atomically replaces this service's snapshot with the credentials and
     * host list parsed from {@code response} and releases any upload or
     * download callers blocked on the first refresh.
     *
     * <p>Called by the success-stream handler each time the periodic
     * {@code media_conn} IQ reply lands.
     *
     * @implSpec
     * Implementations must be safe to call from any thread; a concurrent
     * transfer that captured the previous snapshot keeps using it for the
     * duration of the operation, while new callers see the fresh snapshot.
     *
     * @param response the {@code media_conn} IQ response stanza
     * @throws java.util.NoSuchElementException if {@code response} is
     *         missing the {@code media_conn} child or one of the mandatory
     *         attributes
     * @throws IllegalArgumentException         if a mandatory integer
     *         attribute cannot be parsed as an {@code int}
     */
    void update(Stanza response);

    /**
     * Uploads a media payload to WhatsApp's CDN on behalf of the given
     * provider.
     *
     * <p>High-level entry point for any code that ships a media-bearing
     * message: choose the appropriate {@link MediaProvider} subtype, then
     * call this method with a transcoded {@link MediaPayload}. On success
     * the provider's media metadata (plaintext and encrypted SHA-256
     * hashes, media key, direct path, URL, byte size, and key timestamp) is
     * written back through the provider setters so the caller can build the
     * outgoing message protobuf. The {@code payload} is not closed by this
     * method; the caller owns it.
     *
     * @param provider the media provider describing the media type and
     *                 receiving the upload metadata
     * @param payload  the transcoded payload
     * @return {@code true} if the upload succeeded
     * @throws WhatsAppMediaException if no host could service the upload, a
     *         non-retryable HTTP error occurred, or an I/O error occurred
     * @throws InterruptedException   if the calling thread is interrupted
     *         while waiting for the first media connection or between
     *         retries
     */
    boolean upload(MediaProvider provider, MediaPayload payload) throws WhatsAppMediaException, InterruptedException;

    /**
     * Downloads a media payload from WhatsApp's CDN for the given provider.
     *
     * <p>High-level entry point for any code that materialises an inbound
     * attachment. Tries the provider's cached static media URL first; on a
     * retryable failure it resolves a fresh host and rotates across
     * candidate hosts on each retry. Non-retryable errors propagate
     * immediately.
     *
     * @param provider the media provider
     * @return an {@link InputStream} delivering the decrypted media content
     * @throws WhatsAppMediaException if no host could service the download,
     *         the direct path is missing, or a non-retryable HTTP error
     *         occurred
     * @throws InterruptedException   if the calling thread is interrupted
     *         while waiting for the first media connection or between
     *         retries
     */
    InputStream download(MediaProvider provider) throws WhatsAppMediaException, InterruptedException;

    /**
     * Performs one HTTP GET download against a fully-formed CDN URL.
     *
     * <p>Lower-level counterpart of {@link #download(MediaProvider)} for
     * direct re-downloads against a known URL. The returned stream delivers
     * decrypted, integrity-checked bytes and owns the underlying HTTP
     * client, which it closes when consumed or closed by the caller.
     *
     * @param provider    the media provider holding the decryption metadata
     * @param downloadUrl the full URL to download from
     * @return an {@link InputStream} delivering the decrypted media content
     * @throws WhatsAppMediaException.Download if the server returns a
     *         non-{@code 200} status code, the {@code Content-Length}
     *         header is missing, or a network error occurs
     */
    InputStream tryDownload(MediaProvider provider, String downloadUrl) throws WhatsAppMediaException.Download;

    /**
     * Probes the WhatsApp CDN to verify that a media file exists and is
     * still available for download.
     *
     * <p>Useful before kicking off a full download for an attachment that
     * was referenced from elsewhere (a quoted message, a forwarded sticker)
     * to avoid wasting bandwidth on a known-missing payload. Sends an HTTP
     * HEAD request; a {@code 200} response signals availability.
     *
     * @param hostname    the CDN hostname
     * @param mediaType   the media path type
     * @param directPath  the CDN direct path, or {@code null}
     * @param encFileHash the base64-encoded encrypted file hash, or
     *                    {@code null}
     * @throws WhatsAppMediaException.Download if the media does not exist or
     *         a network error occurs
     */
    void checkExistence(String hostname, MediaPath mediaType, String directPath, String encFileHash) throws WhatsAppMediaException.Download;

    /**
     * Retrieves the size in bytes of the encrypted media payload stored on
     * the WhatsApp CDN.
     *
     * <p>Sends an HTTP HEAD request and reads the {@code Content-Length}
     * header so the caller can pre-allocate buffers or estimate bandwidth
     * before invoking a full download.
     *
     * @param hostname    the CDN hostname
     * @param mediaType   the media path type
     * @param directPath  the CDN direct path, or {@code null}
     * @param encFileHash the base64-encoded encrypted file hash, or
     *                    {@code null}
     * @return the encrypted media file size in bytes
     * @throws WhatsAppMediaException.Download if the {@code Content-Length}
     *         header is missing, the server returns a non-OK status, or a
     *         network error occurs
     */
    long getEncryptedMediaSize(String hostname, MediaPath mediaType, String directPath, String encFileHash) throws WhatsAppMediaException.Download;

    /**
     * Returns the authentication token presented to the CDN on every upload
     * and download request.
     *
     * @return the authentication token
     * @throws IllegalStateException if no media connection has been
     *         installed yet
     */
    String auth();

    /**
     * Returns the routes time-to-live in seconds.
     *
     * @return the TTL in seconds
     * @throws IllegalStateException if no media connection has been
     *         installed yet
     */
    int ttl();

    /**
     * Returns the authentication token time-to-live in seconds.
     *
     * @return the auth TTL in seconds
     * @throws IllegalStateException if no media connection has been
     *         installed yet
     */
    int authTtl();

    /**
     * Returns the maximum number of deterministic download buckets.
     *
     * @return the maximum bucket count
     * @throws IllegalStateException if no media connection has been
     *         installed yet
     */
    int maxBuckets();

    /**
     * Returns the server-advertised budget for manual media download
     * retries.
     *
     * @return the maximum manual retry count
     * @throws IllegalStateException if no media connection has been
     *         installed yet
     */
    int maxManualRetry();

    /**
     * Returns the server-advertised budget for automatic media download
     * retries.
     *
     * @return the maximum auto-download retry count
     * @throws IllegalStateException if no media connection has been
     *         installed yet
     */
    int maxAutoDownloadRetry();

    /**
     * Returns the epoch-second timestamp at which the current credentials
     * were parsed.
     *
     * @return the creation timestamp in epoch seconds
     * @throws IllegalStateException if no media connection has been
     *         installed yet
     */
    long timestamp();

    /**
     * Returns the list of CDN host entries available for uploads and
     * downloads.
     *
     * @return an unmodifiable collection of hosts
     * @throws IllegalStateException if no media connection has been
     *         installed yet
     */
    SequencedCollection<? extends MediaHost> hosts();

    /**
     * Tests whether the current authentication token has expired.
     *
     * <p>Returns {@code true} when no media connection has been installed
     * yet, or when the current clock is at or past
     * {@code timestamp + authTtl}. Callers that observe {@code true} must
     * request a fresh {@code media_conn} via {@link #update(Stanza)} before
     * issuing new CDN requests.
     *
     * @return {@code true} if no credentials are published or the auth
     *         token has expired
     */
    boolean isExpired();

    /**
     * Tests whether the current credentials should be proactively
     * refreshed.
     *
     * <p>Returns {@code true} when no media connection has been installed
     * yet, or when either the routes TTL has elapsed, or 80% of the
     * authentication TTL has elapsed, whichever happens first.
     *
     * @return {@code true} if no credentials are published or refresh is
     *         due
     */
    boolean needsRefresh();
}
