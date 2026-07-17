package com.github.auties00.cobalt.cloud;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.exception.cloud.WhatsAppCloudException;
import com.github.auties00.cobalt.exception.cloud.WhatsAppCloudAuthException;
import com.github.auties00.cobalt.exception.cloud.WhatsAppCloudApiException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.wire.core.util.DataUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP/JSON transport for Meta's WhatsApp Cloud API.
 *
 * <p>Every request targets the versioned graph base {@code https://graph.facebook.com/<version>/},
 * authenticates with the system-user access token as a {@code Bearer} credential, optionally appends
 * an {@code appsecret_proof} (an {@code HmacSHA256} of the access token keyed by the app secret) when
 * an app secret is configured, and parses the response with fastjson2. A non-2xx status with a Graph
 * {@code error} envelope is mapped to {@link WhatsAppCloudException}: an OAuth failure (HTTP 401 or
 * Graph error code 190) becomes a {@link WhatsAppCloudAuthException}, every other
 * rejection a {@link WhatsAppCloudApiException} carrying the code, subcode, and
 * {@code fbtrace_id}.
 *
 * <p>The transport is stateless beyond its credentials and is safe to share across threads, mirroring
 * the {@code FacebookGraphQlClient} design.
 */
public final class CloudApiClient {
    /**
     * The logger for {@link CloudApiClient}.
     */
    private static final System.Logger LOGGER = Log.get(CloudApiClient.class);

    /**
     * The HTTP client used for every request, reused across dispatches for connection pooling.
     */
    private final HttpClient httpClient;

    /**
     * The system-user access token sent as the {@code Bearer} credential.
     */
    private final String accessToken;

    /**
     * The app secret used to derive {@code appsecret_proof}, or {@code null} when proofs are disabled.
     */
    private final String appSecret;

    /**
     * The versioned graph base, for example {@code https://graph.facebook.com/v23.0/}.
     */
    private final URI baseUri;

    /**
     * The literal {@code "--"} prefix that opens the first multipart boundary delimiter.
     */
    private static final byte[] MP_DASH = "--".getBytes(StandardCharsets.UTF_8);

    /**
     * The literal {@code "\r\n--"} that closes a part and opens the next boundary delimiter.
     */
    private static final byte[] MP_CRLF_DASH = "\r\n--".getBytes(StandardCharsets.UTF_8);

    /**
     * The fixed bytes that follow the first boundary: the {@code messaging_product} part carrying
     * {@code whatsapp} and the opening {@code "--"} of the next boundary delimiter.
     */
    private static final byte[] MP_PRODUCT_PART =
            "\r\nContent-Disposition: form-data; name=\"messaging_product\"\r\n\r\nwhatsapp\r\n--"
                    .getBytes(StandardCharsets.UTF_8);

    /**
     * The fixed bytes that introduce the {@code type} part value.
     */
    private static final byte[] MP_TYPE_PART =
            "\r\nContent-Disposition: form-data; name=\"type\"\r\n\r\n".getBytes(StandardCharsets.UTF_8);

    /**
     * The fixed bytes that introduce the {@code file} part up to the opening quote of its filename.
     */
    private static final byte[] MP_FILE_PART =
            "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"".getBytes(StandardCharsets.UTF_8);

    /**
     * The fixed bytes that close the filename quote and introduce the {@code Content-Type} value.
     */
    private static final byte[] MP_FILE_CONTENT_TYPE = "\"\r\nContent-Type: ".getBytes(StandardCharsets.UTF_8);

    /**
     * The blank line that terminates the {@code file} part headers before the raw media bytes.
     */
    private static final byte[] MP_HEADER_END = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);

    /**
     * The literal {@code "--\r\n"} that closes the final boundary delimiter.
     */
    private static final byte[] MP_CLOSE = "--\r\n".getBytes(StandardCharsets.UTF_8);

    /**
     * Constructs a Cloud API client.
     *
     * @param httpClient  the HTTP client to use
     * @param accessToken the system-user access token
     * @param apiVersion  the graph API version segment, for example {@code v23.0}
     * @param appSecret   the app secret used to derive {@code appsecret_proof}, or {@code null} to
     *                    omit the proof
     * @throws NullPointerException if {@code httpClient}, {@code accessToken}, or {@code apiVersion}
     *                              is {@code null}
     */
    public CloudApiClient(HttpClient httpClient, String accessToken, String apiVersion, String appSecret) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.accessToken = Objects.requireNonNull(accessToken, "accessToken must not be null");
        Objects.requireNonNull(apiVersion, "apiVersion must not be null");
        this.appSecret = appSecret;
        this.baseUri = URI.create("https://graph.facebook.com/" + apiVersion + "/");
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "cloud api client initialized, base {0}, appsecret proof enabled {1}",
                    baseUri, appSecret != null);
        }
    }

    /**
     * Issues a {@code GET} against the given edge.
     *
     * @param path  the edge path relative to the versioned base, for example
     *              {@code "<PHONE_NUMBER_ID>/whatsapp_business_profile"}
     * @param query the query parameters, or an empty map for none
     * @return the parsed JSON response
     * @throws WhatsAppCloudException if the request fails or the endpoint reports an error
     */
    public JSONObject get(String path, Map<String, String> query) {
        var request = HttpRequest.newBuilder(buildUri(path, query))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        return send(request, "GET " + path);
    }

    /**
     * Issues a {@code GET} carrying its credentials in the query string, without the {@code Bearer}
     * header or the {@code appsecret_proof}.
     *
     * <p>Used by the Facebook Login endpoints ({@code oauth/access_token}, {@code debug_token}) whose
     * authentication (app id, app secret, app access token) is passed as query parameters rather than in
     * the {@code Authorization} header; {@link #get(String, Map)} cannot serve them because it always
     * attaches the bearer credential and the proof.
     *
     * @param path  the edge path relative to the versioned base, for example {@code "oauth/access_token"}
     * @param query the query parameters carrying the credentials
     * @return the parsed JSON response
     * @throws WhatsAppCloudException if the request fails or the endpoint reports an error
     */
    public JSONObject getUnauthenticated(String path, Map<String, String> query) {
        var encoded = new StringBuilder();
        for (var entry : query.entrySet()) {
            appendParam(encoded, entry.getKey(), entry.getValue());
        }
        var resolved = baseUri.resolve(path);
        var uri = encoded.isEmpty() ? resolved : URI.create(resolved + "?" + encoded);
        var request = HttpRequest.newBuilder(uri)
                .GET()
                .build();
        return send(request, "GET " + path);
    }

    /**
     * Issues a {@code POST} with a JSON body against the given edge.
     *
     * @param path the edge path relative to the versioned base, for example
     *             {@code "<PHONE_NUMBER_ID>/messages"}
     * @param body the JSON request body
     * @return the parsed JSON response
     * @throws WhatsAppCloudException if the request fails or the endpoint reports an error
     */
    public JSONObject post(String path, JSONObject body) {
        var request = HttpRequest.newBuilder(buildUri(path, Map.of()))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        return send(request, "POST " + path);
    }

    /**
     * Issues a {@code POST} with a url-encoded form body against the given edge.
     *
     * <p>Used by the registration and verification edges, which take {@code application/x-www-form-urlencoded}
     * fields rather than a JSON document.
     *
     * @param path the edge path relative to the versioned base
     * @param form the form fields
     * @return the parsed JSON response
     * @throws WhatsAppCloudException if the request fails or the endpoint reports an error
     */
    public JSONObject postForm(String path, Map<String, String> form) {
        var request = HttpRequest.newBuilder(buildUri(path, Map.of()))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encodeForm(form), StandardCharsets.UTF_8))
                .build();
        return send(request, "POST " + path);
    }

    /**
     * Issues a {@code DELETE} against the given edge.
     *
     * @param path  the edge path relative to the versioned base
     * @param query the query parameters, or an empty map for none
     * @return the parsed JSON response
     * @throws WhatsAppCloudException if the request fails or the endpoint reports an error
     */
    public JSONObject delete(String path, Map<String, String> query) {
        var request = HttpRequest.newBuilder(buildUri(path, query))
                .header("Authorization", "Bearer " + accessToken)
                .DELETE()
                .build();
        return send(request, "DELETE " + path);
    }

    /**
     * Issues a {@code DELETE} with a JSON body against the given edge.
     *
     * <p>Used by edges whose delete semantics take a request body, such as the block-users edge's
     * unblock operation.
     *
     * @param path the edge path relative to the versioned base
     * @param body the JSON request body
     * @return the parsed JSON response
     * @throws WhatsAppCloudException if the request fails or the endpoint reports an error
     */
    public JSONObject delete(String path, JSONObject body) {
        var request = HttpRequest.newBuilder(buildUri(path, Map.of()))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .method("DELETE", HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        return send(request, "DELETE " + path);
    }

    /**
     * Uploads a media asset and returns the resulting media object.
     *
     * <p>Posts a {@code multipart/form-data} body to the phone number's {@code media} edge with the
     * {@code messaging_product=whatsapp} field, the {@code type} field carrying the MIME type, and the
     * binary {@code file} part. The response carries the media id under {@code id}.
     *
     * @param phoneNumberId the phone number id whose media edge receives the upload
     * @param data          the raw media bytes
     * @param mimeType      the MIME type of the media, for example {@code image/jpeg}
     * @param filename      the file name advertised in the multipart part
     * @return the parsed JSON response carrying the media {@code id}
     * @throws WhatsAppCloudException if the request fails or the endpoint reports an error
     */
    public JSONObject uploadMedia(String phoneNumberId, byte[] data, String mimeType, String filename) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "uploading media, size {0}, mime type {1}", data.length, mimeType);
        var boundary = "cobalt" + Long.toHexString(data.length) + "x" + Integer.toHexString(mimeType.hashCode());
        var body = multipartBody(boundary, mimeType, filename, data);
        var request = HttpRequest.newBuilder(buildUri(phoneNumberId + "/media", Map.of()))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        return send(request, "POST " + phoneNumberId + "/media");
    }

    /**
     * Uploads a single named file part with arbitrary text fields as {@code multipart/form-data}.
     *
     * <p>Generalises {@link #uploadMedia} for edges that take their own set of text fields and a
     * single binary part under a caller-chosen field name, such as the Flow assets edge. The text
     * fields precede the file part, and the file part advertises the given filename and content type.
     *
     * @param path          the edge path relative to the versioned base
     * @param fields        the text form fields, in iteration order
     * @param fileFieldName the form field name of the binary part
     * @param filename      the file name advertised in the binary part
     * @param contentType   the content type advertised in the binary part
     * @param data          the raw file bytes
     * @return the parsed JSON response
     * @throws WhatsAppCloudException if the request fails or the endpoint reports an error
     */
    public JSONObject uploadFormFile(String path, Map<String, String> fields, String fileFieldName,
                                     String filename, String contentType, byte[] data) {
        var boundary = "cobalt" + Long.toHexString(data.length) + "x" + Integer.toHexString(filename.hashCode());
        var body = genericMultipartBody(boundary, fields, fileFieldName, filename, contentType, data);
        var request = HttpRequest.newBuilder(buildUri(path, Map.of()))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        return send(request, "POST " + path);
    }

    /**
     * Downloads the binary content of a media asset from its resolved CDN URL.
     *
     * <p>The URL is the one returned by {@code GET /<MEDIA_ID>}; it is short-lived and still requires
     * the {@code Bearer} credential.
     *
     * @param mediaUrl the resolved media URL
     * @return the raw media bytes
     * @throws WhatsAppCloudException if the request fails
     */
    public byte[] download(URI mediaUrl) {
        var request = HttpRequest.newBuilder(mediaUrl)
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            var status = response.statusCode();
            if (status < 200 || status >= 300) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "media download failed, status {0}", status);
                throw new WhatsAppCloudApiException(status, 0, 0,
                        "media download failed", null);
            }
            var body = response.body();
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "media download finished, size {0}", body.length);
            return body;
        } catch (IOException exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "media download failed", exception);
            throw new WhatsAppCloudApiException(
                    "media download failed: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            if (Log.WARNING) LOGGER.log(Level.WARNING, "media download interrupted", exception);
            throw new WhatsAppCloudApiException("media download interrupted");
        }
    }

    /**
     * Creates a resumable upload session and returns its session locator.
     *
     * <p>Posts to the application's {@code /{APP_ID}/uploads} edge with the total byte length, MIME
     * type, and optional advertised file name as query parameters and no body. The Resumable Upload
     * API is keyed by the Meta app id rather than a phone number or WABA, and it does not accept the
     * {@code appsecret_proof}, so the URI is assembled directly off the versioned base without the
     * proof. The response carries the session locator under {@code id} as the literal string
     * {@code upload:<UPLOAD_SESSION_ID>}; that whole value, prefix included, is reused verbatim as the
     * path of {@link #uploadToSession(String, long, byte[])} and {@link #queryUploadStatus(String)}.
     *
     * @param appId      the Meta app id whose uploads edge creates the session
     * @param fileLength the total byte length of the file to upload
     * @param fileType   the MIME type of the file, for example {@code image/jpeg}
     * @param fileName   the advertised file name, or {@code null} to omit it
     * @return the session locator, the literal {@code upload:<UPLOAD_SESSION_ID>} string
     * @throws WhatsAppCloudException if the request fails or the endpoint reports an error
     */
    public String createUploadSession(String appId, long fileLength, String fileType, String fileName) {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "creating resumable upload session, file length {0}, type {1}", fileLength, fileType);
        }
        var encoded = new StringBuilder();
        appendParam(encoded, "file_length", Long.toString(fileLength));
        appendParam(encoded, "file_type", fileType);
        if (fileName != null) {
            appendParam(encoded, "file_name", fileName);
        }
        var uri = URI.create(baseUri.resolve(appId + "/uploads") + "?" + encoded);
        var request = HttpRequest.newBuilder(uri)
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        var json = send(request, "POST " + appId + "/uploads");
        var sessionId = json.getString("id");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "created resumable upload session {0}", new LogRedactable.Token(sessionId));
        return sessionId;
    }

    /**
     * Uploads the file bytes to a resumable session and returns the resulting media handle.
     *
     * <p>Posts the raw binary bytes to the session locator with no multipart wrapper and no
     * {@code Content-Type} form header. The Resumable Upload API authenticates this step with the
     * literal {@code OAuth} scheme rather than {@code Bearer}, carries the byte offset in the
     * {@code file_offset} header ({@code 0} for a fresh upload, or the value returned by
     * {@link #queryUploadStatus(String)} when resuming), and does not accept the
     * {@code appsecret_proof}. The response carries the handle under {@code h}; that handle is the
     * value fed to a template HEADER component as {@code components[].example.header_handle}.
     *
     * @param uploadSessionId the session locator returned by
     *                        {@link #createUploadSession(String, long, String, String)}, the full
     *                        {@code upload:<UPLOAD_SESSION_ID>} string
     * @param fileOffset      the byte offset to upload from, {@code 0} for a fresh upload
     * @param data            the raw file bytes
     * @return the media handle usable as a template {@code header_handle}
     * @throws WhatsAppCloudException if the request fails or the endpoint reports an error
     */
    public String uploadToSession(String uploadSessionId, long fileOffset, byte[] data) {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "uploading to resumable session {0}, offset {1}, size {2}",
                    new LogRedactable.Token(uploadSessionId), fileOffset, data.length);
        }
        var uri = sessionUri(uploadSessionId);
        var request = HttpRequest.newBuilder(uri)
                .header("Authorization", "OAuth " + accessToken)
                .header("file_offset", Long.toString(fileOffset))
                .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                .build();
        var json = send(request, "POST " + uploadSessionId);
        return json.getString("h");
    }

    /**
     * Queries the byte offset already received by a resumable upload session.
     *
     * <p>Issues a {@code GET} on the session locator with no body and no query, authenticating with
     * the literal {@code OAuth} scheme and without the {@code appsecret_proof}. The response carries
     * the received byte count under {@code file_offset}; resuming an interrupted upload re-posts
     * {@link #uploadToSession(String, long, byte[])} with the returned value. Graph may serialise
     * {@code file_offset} as a JSON string, so it is read with a coercing accessor.
     *
     * @param uploadSessionId the session locator, the full {@code upload:<UPLOAD_SESSION_ID>} string
     * @return the byte offset already received by the session
     * @throws WhatsAppCloudException if the request fails or the endpoint reports an error
     */
    public long queryUploadStatus(String uploadSessionId) {
        var uri = sessionUri(uploadSessionId);
        var request = HttpRequest.newBuilder(uri)
                .header("Authorization", "OAuth " + accessToken)
                .GET()
                .build();
        var json = send(request, "GET " + uploadSessionId);
        var offset = json.getLongValue("file_offset");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "resumable session {0} offset {1}", new LogRedactable.Token(uploadSessionId), offset);
        return offset;
    }

    /**
     * Builds the absolute request URI for a resumable upload session locator.
     *
     * <p>The locator is the literal {@code upload:<UPLOAD_SESSION_ID>} string, whose leading
     * {@code upload:} would be parsed as a URI scheme by {@link URI#resolve(String)}. The locator is
     * therefore appended to the absolute base string rather than resolved, so the colon stays inside
     * the path segment.
     *
     * @param uploadSessionId the session locator
     * @return the absolute request URI
     */
    private URI sessionUri(String uploadSessionId) {
        return URI.create(baseUri + uploadSessionId);
    }

    /**
     * Builds the absolute request URI for an edge path, merging the query parameters and the
     * optional {@code appsecret_proof}.
     *
     * @param path  the edge path relative to the versioned base
     * @param query the query parameters
     * @return the absolute request URI
     */
    private URI buildUri(String path, Map<String, String> query) {
        var encoded = new StringBuilder();
        for (var entry : query.entrySet()) {
            appendParam(encoded, entry.getKey(), entry.getValue());
        }
        var proof = appSecretProof();
        if (proof != null) {
            appendParam(encoded, "appsecret_proof", proof);
        }
        var resolved = baseUri.resolve(path);
        if (encoded.isEmpty()) {
            return resolved;
        }
        return URI.create(resolved + "?" + encoded);
    }

    /**
     * Appends a single url-encoded {@code key=value} pair to the running query string.
     *
     * @param target the buffer accumulating the query string
     * @param key    the parameter name
     * @param value  the parameter value
     */
    private static void appendParam(StringBuilder target, String key, String value) {
        if (!target.isEmpty()) {
            target.append('&');
        }
        target.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                .append('=')
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    /**
     * Encodes a form map as an {@code application/x-www-form-urlencoded} body.
     *
     * @param form the form fields
     * @return the encoded body string
     */
    private static String encodeForm(Map<String, String> form) {
        var body = new StringBuilder();
        for (var entry : form.entrySet()) {
            appendParam(body, entry.getKey(), entry.getValue());
        }
        return body.toString();
    }

    /**
     * Computes the {@code appsecret_proof} for the configured access token.
     *
     * @return the hex-encoded {@code HmacSHA256} of the access token keyed by the app secret, or
     *         {@code null} when no app secret is configured
     */
    private String appSecretProof() {
        if (appSecret == null) {
            return null;
        }
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            var raw = mac.doFinal(accessToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (GeneralSecurityException exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to compute appsecret_proof", exception);
            throw new WhatsAppCloudAuthException("failed to compute appsecret_proof", exception);
        }
    }

    /**
     * Builds a {@code multipart/form-data} body for a media upload.
     *
     * @param boundary the multipart boundary token
     * @param mimeType the MIME type of the media
     * @param filename the file name advertised in the binary part
     * @param data     the raw media bytes
     * @return the assembled multipart body
     */
    private static byte[] multipartBody(String boundary, String mimeType, String filename, byte[] data) {
        var boundaryBytes = boundary.getBytes(StandardCharsets.UTF_8);
        var mimeTypeBytes = mimeType.getBytes(StandardCharsets.UTF_8);
        var filenameBytes = filename.getBytes(StandardCharsets.UTF_8);
        var size = MP_DASH.length + boundaryBytes.length
                + MP_PRODUCT_PART.length + boundaryBytes.length
                + MP_TYPE_PART.length + mimeTypeBytes.length
                + MP_CRLF_DASH.length + boundaryBytes.length
                + MP_FILE_PART.length + filenameBytes.length
                + MP_FILE_CONTENT_TYPE.length + mimeTypeBytes.length
                + MP_HEADER_END.length
                + data.length
                + MP_CRLF_DASH.length + boundaryBytes.length
                + MP_CLOSE.length;
        var out = new byte[size];
        var pos = 0;
        pos = DataUtils.append(out, pos, MP_DASH);
        pos = DataUtils.append(out, pos, boundaryBytes);
        pos = DataUtils.append(out, pos, MP_PRODUCT_PART);
        pos = DataUtils.append(out, pos, boundaryBytes);
        pos = DataUtils.append(out, pos, MP_TYPE_PART);
        pos = DataUtils.append(out, pos, mimeTypeBytes);
        pos = DataUtils.append(out, pos, MP_CRLF_DASH);
        pos = DataUtils.append(out, pos, boundaryBytes);
        pos = DataUtils.append(out, pos, MP_FILE_PART);
        pos = DataUtils.append(out, pos, filenameBytes);
        pos = DataUtils.append(out, pos, MP_FILE_CONTENT_TYPE);
        pos = DataUtils.append(out, pos, mimeTypeBytes);
        pos = DataUtils.append(out, pos, MP_HEADER_END);
        pos = DataUtils.append(out, pos, data);
        pos = DataUtils.append(out, pos, MP_CRLF_DASH);
        pos = DataUtils.append(out, pos, boundaryBytes);
        pos = DataUtils.append(out, pos, MP_CLOSE);
        return out;
    }

    /**
     * Builds a {@code multipart/form-data} body for arbitrary text fields and a single named file
     * part.
     *
     * <p>Emits one part per text field in iteration order, then a final part carrying the binary
     * file under the given field name with the supplied filename and content type, and closes with
     * the terminating boundary delimiter.
     *
     * @param boundary      the multipart boundary token
     * @param fields        the text form fields, in iteration order
     * @param fileFieldName the form field name of the binary part
     * @param filename      the file name advertised in the binary part
     * @param contentType   the content type advertised in the binary part
     * @param data          the raw file bytes
     * @return the assembled multipart body
     */
    private static byte[] genericMultipartBody(String boundary, Map<String, String> fields,
                                               String fileFieldName, String filename,
                                               String contentType, byte[] data) {
        var head = new StringBuilder();
        for (var entry : fields.entrySet()) {
            head.append("--").append(boundary).append("\r\n")
                    .append("Content-Disposition: form-data; name=\"").append(entry.getKey()).append("\"\r\n\r\n")
                    .append(entry.getValue()).append("\r\n");
        }
        head.append("--").append(boundary).append("\r\n")
                .append("Content-Disposition: form-data; name=\"").append(fileFieldName)
                .append("\"; filename=\"").append(filename).append("\"\r\n")
                .append("Content-Type: ").append(contentType).append("\r\n\r\n");
        var headBytes = head.toString().getBytes(StandardCharsets.UTF_8);
        var tailBytes = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        var out = new byte[headBytes.length + data.length + tailBytes.length];
        var pos = 0;
        pos = DataUtils.append(out, pos, headBytes);
        pos = DataUtils.append(out, pos, data);
        DataUtils.append(out, pos, tailBytes);
        return out;
    }

    /**
     * Sends a prepared request and parses the response, mapping failures to the Cloud exception
     * hierarchy.
     *
     * @param request the prepared HTTP request
     * @param opName  a short operation name used in diagnostics
     * @return the parsed JSON response
     * @throws WhatsAppCloudException if the request fails or the endpoint reports an error
     */
    private JSONObject send(HttpRequest request, String opName) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sending cloud api request, method {0}", request.method());
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "cloud api request failed, method " + request.method(), exception);
            }
            throw new WhatsAppCloudApiException(
                    opName + " failed: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "cloud api request interrupted, method " + request.method(), exception);
            }
            throw new WhatsAppCloudApiException(opName + " interrupted");
        }
        return parse(response, opName);
    }

    /**
     * Parses a Cloud API HTTP response, raising the matching Cloud exception on failure.
     *
     * @param response the HTTP response
     * @param opName   a short operation name used in diagnostics
     * @return the parsed JSON response, or an empty object when the body was blank but the status
     *         was successful
     * @throws WhatsAppCloudException if the body is unparsable, the status is non-2xx, or the
     *                                response carries an error envelope
     */
    private JSONObject parse(HttpResponse<String> response, String opName) {
        var status = response.statusCode();
        JSONObject json;
        try {
            var body = response.body();
            json = body == null || body.isBlank() ? new JSONObject() : JSON.parseObject(body);
        } catch (RuntimeException exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to parse cloud api response, status " + status, exception);
            throw new WhatsAppCloudApiException(status, 0, 0,
                    "failed to parse " + opName + " response", null);
        }

        var error = json.getJSONObject("error");
        if (status >= 200 && status < 300 && error == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "cloud api response received, status {0}", status);
            return json;
        }

        var code = error != null ? error.getIntValue("code") : 0;
        var subcode = error != null ? error.getIntValue("error_subcode") : 0;
        var message = error != null ? error.getString("message") : status + " response for " + opName;
        var fbtraceId = error != null ? error.getString("fbtrace_id") : null;
        if (status == 401 || code == 190) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "cloud api unauthorized, status {0}", status);
            throw new WhatsAppCloudAuthException(opName + " unauthorized: " + message);
        }
        if (Log.WARNING) {
            LOGGER.log(Level.WARNING, "cloud api error, status {0}, code {1}, subcode {2}, fbtrace {3}",
                    status, code, subcode, fbtraceId);
        }
        throw new WhatsAppCloudApiException(status, code, subcode, message, fbtraceId);
    }
}
