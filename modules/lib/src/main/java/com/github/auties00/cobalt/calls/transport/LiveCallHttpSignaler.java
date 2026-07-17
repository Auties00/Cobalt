package com.github.auties00.cobalt.calls.transport;

import com.github.auties00.cobalt.calls.platform.VoipCryptoNative;
import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bootstraps a call's media session by posting a {@code start_session_request} over the injected client
 * transport and correlating its {@code start_session_response} by request id.
 *
 * <p>Before a call's media plane can come up, a small JSON bootstrap request must be posted and its matching
 * response awaited. The request body is the literal {@value #START_SESSION_REQUEST_BODY}. There is no
 * dedicated HTTP endpoint and no synthetic request id header on the wire: the request rides the same client
 * transport the rest of the call uses. The {@link CallSignalingTransport} seam the embedder supplies issues
 * the request over that live transport (the Linked encrypted socket as a call IQ, or the Cloud REST client)
 * and returns the correlated {@code start_session_response} bytes; the seam owns how the request id travels
 * the transport.
 *
 * <p>The signaler generates a {@value #REQUEST_ID_LENGTH} character request id, records it in a concurrent
 * open request table keyed by that id, dispatches the request through the transport seam, and blocks the
 * calling virtual thread on the response. An entry is removed once its response returns or its request
 * fails. A single signaler is safe to share across the threads that bootstrap concurrent calls, because the
 * open request table is concurrent and each request blocks on its own response.
 *
 * @implNote This implementation formats a {@value #REQUEST_ID_LENGTH} character request id, sets the JSON
 *           body {@value #START_SESSION_REQUEST_BODY}, invokes the {@link CallSignalingTransport} callback,
 *           and keeps an open request table keyed by request id. A transport failure surfaces as
 *           {@link WhatsAppCallException.DataChannel}, the non fatal media plane bring up failure.
 */
public final class LiveCallHttpSignaler {
    /**
     * The logger for {@link LiveCallHttpSignaler}.
     */
    private static final System.Logger LOGGER = Log.get(LiveCallHttpSignaler.class);

    /**
     * The fixed length, in characters, of a generated session bootstrap request id.
     */
    public static final int REQUEST_ID_LENGTH = 63;

    /**
     * The literal JSON body posted to bootstrap a call's media session.
     */
    public static final String START_SESSION_REQUEST_BODY = "{\"start_session_request\":{}}";

    /**
     * The number of random bytes drawn to derive a request id; the URL safe base64 of these bytes is
     * truncated to {@value #REQUEST_ID_LENGTH} characters.
     */
    private static final int REQUEST_ID_ENTROPY_BYTES = 48;

    /**
     * Holds the client transport seam the bootstrap request is issued over.
     */
    private final CallSignalingTransport transport;

    /**
     * Holds the open requests keyed by request id, each mapped to the {@link System#nanoTime()} at which the
     * request was dispatched.
     */
    private final Map<String, Long> openRequests = new ConcurrentHashMap<>();

    /**
     * Constructs a signaler routing the bootstrap request over the given client transport seam.
     *
     * @param transport the client transport seam the {@code start_session_request} is issued over
     * @throws NullPointerException if {@code transport} is {@code null}
     */
    public LiveCallHttpSignaler(CallSignalingTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport cannot be null");
    }

    /**
     * Posts the {@code start_session_request} and blocks until its {@code start_session_response}
     * returns.
     *
     * <p>This generates a unique request id, records it in the open request table, posts the bootstrap body
     * over its transport, and waits for the correlated response, returning it as a
     * {@link StartSessionResult}. The calling virtual thread blocks for the round trip. The request id is
     * removed from the open request table once the response returns or the request fails.
     *
     * @return the correlated bootstrap result
     * @throws WhatsAppCallException if the request cannot be sent or the response does not return
     */
    public StartSessionResult sendStartSessionRequest() {
        var requestId = newRequestId();
        openRequests.put(requestId, System.nanoTime());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "posting start_session_request {0}", requestId);
        try {
            byte[] responseBody;
            try {
                responseBody = transport.sendStartSessionRequest(
                        requestId, START_SESSION_REQUEST_BODY.getBytes(StandardCharsets.UTF_8));
            } catch (WhatsAppCallException exception) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "start_session_request failed for request " + requestId, exception);
                }
                throw exception;
            } catch (RuntimeException exception) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "start_session_request failed for request " + requestId, exception);
                }
                throw new WhatsAppCallException.DataChannel(
                        "start_session_request failed for request " + requestId, exception);
            }
            if (responseBody == null) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "start_session_request {0} returned no response", requestId);
                }
                throw new WhatsAppCallException.DataChannel(
                        "start_session_request returned no response for request " + requestId);
            }
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "received start_session_response {0}, {1} bytes", requestId,
                        responseBody.length);
            }
            return new StartSessionResult(requestId, responseBody);
        } finally {
            openRequests.remove(requestId);
        }
    }

    /**
     * Returns the number of requests currently awaiting a response.
     *
     * @return the size of the open request table
     */
    public int openRequestCount() {
        return openRequests.size();
    }

    /**
     * Generates a fresh {@value #REQUEST_ID_LENGTH} character request id.
     *
     * <p>The id is the URL safe, unpadded base64 of {@value #REQUEST_ID_ENTROPY_BYTES} cryptographically
     * strong random bytes, truncated to the fixed request id length.
     *
     * @return a new request id of exactly {@value #REQUEST_ID_LENGTH} characters
     */
    private static String newRequestId() {
        var entropy = VoipCryptoNative.randomBytes(REQUEST_ID_ENTROPY_BYTES);
        var encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(entropy);
        return encoded.substring(0, REQUEST_ID_LENGTH);
    }

    /**
     * Holds the outcome of a {@code start_session} bootstrap round trip.
     *
     * <p>It carries the request id that correlated the exchange and the raw response body the transport
     * returned, which the transport controller parses for any session parameters the response conveys.
     *
     * @param requestId    the {@value LiveCallHttpSignaler#REQUEST_ID_LENGTH} character request id that
     *                     correlated the request and response
     * @param responseBody the raw {@code start_session_response} body bytes, or an empty array when the
     *                     response carried no body
     */
    public record StartSessionResult(String requestId, byte[] responseBody) {
        /**
         * Canonicalizes the record components, defensively copying the response body.
         *
         * @throws NullPointerException if {@code requestId} or {@code responseBody} is {@code null}
         */
        public StartSessionResult {
            Objects.requireNonNull(requestId, "requestId cannot be null");
            Objects.requireNonNull(responseBody, "responseBody cannot be null");
            responseBody = responseBody.clone();
        }

        /**
         * Returns a defensive copy of the raw response body bytes.
         *
         * @return a copy of the response body; never {@code null}, possibly empty
         */
        @Override
        public byte[] responseBody() {
            return responseBody.clone();
        }
    }

    /**
     * The seam the call bootstrap request is issued over, routing it onto the live client transport.
     *
     * <p>The {@code start_session_request} is handed to this callback rather than posted to a dedicated
     * socket; the callback is supplied by the embedder and backed by the live client transport. An
     * implementation issues the request over the existing transport (the {@code LinkedWhatsAppClient}
     * encrypted socket as a call IQ, or the {@code CloudApiClient} REST call), carries the request id so the
     * asynchronous {@code start_session_response} can be correlated to it, and returns the response body
     * bytes. The call blocks the calling virtual thread for the round trip, in keeping with the project's
     * blocking on a virtual thread model.
     *
     * @apiNote This is the integration seam an embedder wires the call transport to: the media plane builds
     *          a {@link LiveCallHttpSignaler} over this seam, and the seam routes the bootstrap onto the
     *          client transport. It is not itself an embedder facing API surface.
     * @implSpec An implementation MUST issue the request body over its client transport, MUST correlate the
     *           response to the supplied request id, and MUST surface a transport failure by throwing
     *           (a {@link WhatsAppCallException} is propagated unchanged; any other runtime failure is
     *           wrapped as a {@link WhatsAppCallException.DataChannel}) rather than returning {@code null}
     *           on error; returning {@code null} is reserved for a transport that completed but yielded no
     *           response body and is itself treated as a bring up failure.
     */
    @FunctionalInterface
    public interface CallSignalingTransport {
        /**
         * Issues the {@code start_session_request} over the client transport and returns the correlated
         * response body.
         *
         * @param requestId   the {@value LiveCallHttpSignaler#REQUEST_ID_LENGTH} character request id to
         *                    correlate the exchange by; never {@code null}
         * @param requestBody the literal {@code start_session_request} body bytes to issue; never
         *                    {@code null}
         * @return the correlated {@code start_session_response} body bytes, or {@code null} when the
         *         transport completed without a response body
         * @throws WhatsAppCallException if the request cannot be issued or the response does not return
         */
        byte[] sendStartSessionRequest(String requestId, byte[] requestBody);
    }
}
