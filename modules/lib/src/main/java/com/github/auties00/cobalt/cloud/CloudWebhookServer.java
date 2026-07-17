package com.github.auties00.cobalt.cloud;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.exception.cloud.WhatsAppCloudException;
import com.github.auties00.cobalt.exception.cloud.WhatsAppCloudWebhookException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * The built-in receiver for inbound WhatsApp Cloud API webhook deliveries.
 *
 * <p>This wraps a {@link HttpServer} bound to the configured address, port, and path. It answers the
 * two request shapes Meta sends:
 * <ul>
 *   <li>a {@code GET} subscription handshake ({@code hub.mode=subscribe} with a {@code hub.verify_token}
 *       that must match the configured token), to which it echoes the {@code hub.challenge};</li>
 *   <li>a {@code POST} delivery, whose raw body is verified against the {@code X-Hub-Signature-256}
 *       header (an {@code HmacSHA256} over the body keyed by the app secret) before the parsed
 *       {@code object=whatsapp_business_account} envelope is handed to the configured sink.</li>
 * </ul>
 *
 * <p>Requests run on a virtual-thread-per-task executor, matching Cobalt's Loom-based concurrency
 * model. A signature mismatch or a parse failure is reported through the error consumer and answered
 * with the appropriate {@code 4xx} status; it never tears the server down.
 */
public final class CloudWebhookServer {
    /**
     * The logger for {@link CloudWebhookServer}.
     */
    private static final System.Logger LOGGER = Log.get(CloudWebhookServer.class);

    /**
     * The bind address, or {@code null} to bind the wildcard address.
     */
    private final String bindAddress;

    /**
     * The TCP port the server binds.
     */
    private final int port;

    /**
     * The URL path the server listens on.
     */
    private final String path;

    /**
     * The verify token echoed during the subscription handshake.
     */
    private final String verifyToken;

    /**
     * The app secret used to verify {@code X-Hub-Signature-256}, or {@code null} to skip verification.
     */
    private final String appSecret;

    /**
     * The sink that receives each verified envelope.
     */
    private final Consumer<JSONObject> onEnvelope;

    /**
     * The sink that receives processing failures.
     */
    private final Consumer<Throwable> onError;

    /**
     * The running HTTP server, or {@code null} while stopped.
     */
    private volatile HttpServer server;

    /**
     * Constructs a new webhook server.
     *
     * @param bindAddress the bind address, or {@code null} to bind the wildcard address
     * @param port        the TCP port to bind
     * @param path        the URL path to listen on
     * @param verifyToken the verify token echoed during the subscription handshake
     * @param appSecret   the app secret used to verify signatures, or {@code null} to skip
     *                    verification
     * @param onEnvelope  the sink receiving each verified envelope
     * @param onError     the sink receiving processing failures
     * @throws NullPointerException if {@code path}, {@code verifyToken}, {@code onEnvelope}, or
     *                              {@code onError} is {@code null}
     */
    public CloudWebhookServer(String bindAddress, int port, String path, String verifyToken, String appSecret,
                              Consumer<JSONObject> onEnvelope, Consumer<Throwable> onError) {
        this.bindAddress = bindAddress;
        this.port = port;
        this.path = Objects.requireNonNull(path, "path must not be null");
        this.verifyToken = Objects.requireNonNull(verifyToken, "verifyToken must not be null");
        this.appSecret = appSecret;
        this.onEnvelope = Objects.requireNonNull(onEnvelope, "onEnvelope must not be null");
        this.onError = Objects.requireNonNull(onError, "onError must not be null");
    }

    /**
     * Starts the server and begins accepting deliveries.
     *
     * @throws IllegalStateException  if the server is already running
     * @throws WhatsAppCloudException if the server socket cannot be bound
     */
    public synchronized void start() {
        if (server != null) {
            throw new IllegalStateException("webhook server already running");
        }
        try {
            var address = bindAddress == null
                    ? new InetSocketAddress(port)
                    : new InetSocketAddress(bindAddress, port);
            var created = HttpServer.create(address, 0);
            created.createContext(path, this::handle);
            created.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            created.start();
            this.server = created;
            if (Log.INFO) LOGGER.log(Level.INFO, "webhook server started on port {0} path {1}", port, path);
        } catch (IOException exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "webhook server failed to bind on port " + port, exception);
            throw new WhatsAppCloudWebhookException("failed to bind webhook server", exception);
        }
    }

    /**
     * Stops the server, refusing further deliveries.
     *
     * <p>A stop on an already-stopped server is a no-op.
     */
    public synchronized void stop() {
        var running = server;
        if (running != null) {
            running.stop(0);
            this.server = null;
            if (Log.INFO) LOGGER.log(Level.INFO, "webhook server stopped");
        }
    }

    /**
     * Returns whether the server is currently running.
     *
     * @return {@code true} if the server is bound and accepting deliveries
     */
    public boolean isRunning() {
        return server != null;
    }

    /**
     * Routes one request to the handshake or delivery handler.
     *
     * @param exchange the HTTP exchange
     * @throws IOException if the response cannot be written
     */
    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleHandshake(exchange);
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleDelivery(exchange);
            } else {
                respond(exchange, 405, "method not allowed");
            }
        }
    }

    /**
     * Answers the {@code GET} subscription handshake.
     *
     * @param exchange the HTTP exchange
     * @throws IOException if the response cannot be written
     */
    private void handleHandshake(HttpExchange exchange) throws IOException {
        var query = parseQuery(exchange.getRequestURI().getRawQuery());
        var mode = query.get("hub.mode");
        var token = query.get("hub.verify_token");
        var challenge = query.get("hub.challenge");
        if ("subscribe".equals(mode) && verifyToken.equals(token) && challenge != null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "webhook handshake accepted");
            respond(exchange, 200, challenge);
        } else {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "webhook handshake rejected, mode={0}", mode);
            respond(exchange, 403, "forbidden");
        }
    }

    /**
     * Verifies and dispatches a {@code POST} delivery.
     *
     * @param exchange the HTTP exchange
     * @throws IOException if the response cannot be written
     */
    private void handleDelivery(HttpExchange exchange) throws IOException {
        var body = exchange.getRequestBody().readAllBytes();
        if (appSecret != null) {
            var signature = exchange.getRequestHeaders().getFirst("X-Hub-Signature-256");
            if (!verifySignature(body, signature)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "webhook delivery rejected, signature mismatch, bytes={0}", body.length);
                onError.accept(new WhatsAppCloudWebhookException(
                        "webhook signature verification failed"));
                respond(exchange, 401, "invalid signature");
                return;
            }
        }
        JSONObject envelope;
        try {
            envelope = JSON.parseObject(new String(body, StandardCharsets.UTF_8));
        } catch (RuntimeException exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "webhook delivery rejected, malformed payload", exception);
            onError.accept(new WhatsAppCloudWebhookException("malformed webhook payload", exception));
            respond(exchange, 400, "malformed payload");
            return;
        }
        respond(exchange, 200, "EVENT_RECEIVED");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "webhook delivery accepted, bytes={0}", body.length);
        try {
            onEnvelope.accept(envelope);
        } catch (RuntimeException exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "webhook envelope dispatch failed", exception);
            onError.accept(exception);
        }
    }

    /**
     * Verifies the {@code X-Hub-Signature-256} header against the raw body.
     *
     * @param body      the raw request body
     * @param signature the {@code X-Hub-Signature-256} header value, of the form {@code sha256=<hex>}
     * @return {@code true} if the signature matches the computed HMAC
     */
    private boolean verifySignature(byte[] body, String signature) {
        if (signature == null || !signature.startsWith("sha256=")) {
            return false;
        }
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            var expected = HexFormat.of().formatHex(mac.doFinal(body));
            var provided = signature.substring("sha256=".length());
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    provided.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            return false;
        }
    }

    /**
     * Writes a plain-text response with the given status and body.
     *
     * @param exchange the HTTP exchange
     * @param status   the HTTP status code
     * @param body     the response body
     * @throws IOException if the response cannot be written
     */
    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    /**
     * Parses a url-encoded query string into a flat map.
     *
     * @param rawQuery the raw query string, or {@code null}
     * @return a map of decoded parameter names to values
     */
    private static Map<String, String> parseQuery(String rawQuery) {
        var result = new HashMap<String, String>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return result;
        }
        for (var pair : rawQuery.split("&")) {
            var index = pair.indexOf('=');
            if (index != -1) {
                var key = URLDecoder.decode(pair.substring(0, index), StandardCharsets.UTF_8);
                var value = URLDecoder.decode(pair.substring(index + 1), StandardCharsets.UTF_8);
                result.put(key, value);
            }
        }
        return result;
    }
}
