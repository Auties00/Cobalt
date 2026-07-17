package com.github.auties00.cobalt.wam.privatestats;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.wam.TestWamService;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link WamPrivateStatsUploader}'s request assembly and status-code mapping against a
 * recording HTTP-client stub.
 *
 * <p>The suite drives a {@code RecordingHttpClient} that captures the request without performing
 * any network I/O, and the uploader's token-acquisition path uses the same scripted
 * {@link SecureRandom} as {@code WamPrivateStatsTokenIssuerTest} so the captured known-answer bytes
 * propagate through. It pins the URL, the {@code multipart/form-data} part order, the boundary
 * shape, and the HTTP-status to {@link WamPrivateStatsUploadResult.Type} mapping so any regression
 * in the wire-level upload contract surfaces immediately.
 */
@DisplayName("WamPrivateStatsUploader behavioural")
class WamPrivateStatsUploaderTest {
    private static final String ENDPOINT = "https://dit.whatsapp.net/deidentified_telemetry";

    // Hard-coded access_token the WhatsApp Web bundle sends; Cobalt must send the same value.
    private static final String ACCESS_TOKEN = "245118376424571|3e7d275052f1522bf3200afcf53841a7";

    @Test
    @DisplayName("request URL and Content-Type match the documented contract")
    void requestUrlAndContentType() {
        var captured = uploadCapture(new byte[]{1, 2, 3, 4}, 200);
        assertEquals(URI.create(ENDPOINT), captured.request().uri(),
                "must POST to dit.whatsapp.net/deidentified_telemetry");
        assertEquals("POST", captured.request().method());

        var contentType = captured.request().headers().firstValue("Content-Type").orElseThrow();
        assertTrue(contentType.startsWith("multipart/form-data; boundary="),
                "Content-Type must be multipart/form-data with explicit boundary, was: " + contentType);
        var boundary = contentType.substring("multipart/form-data; boundary=".length());
        assertTrue(boundary.startsWith("----WebKitFormBoundary"),
                "boundary must follow the WebKit convention: " + boundary);
    }

    @Test
    @DisplayName("multipart body has the four documented parts in the documented order")
    void multipartBodyHasFourParts() {
        var captured = uploadCapture(new byte[]{1, 2, 3, 4, 5}, 200);
        var body = new String(captured.body(), StandardCharsets.ISO_8859_1);

        var accessTokenIdx = body.indexOf("name=\"access_token\"");
        var credentialIdx = body.indexOf("name=\"credential\"");
        var messageIdx = body.indexOf("name=\"message\"");
        var metaDataIdx = body.indexOf("name=\"meta_data\"");

        assertTrue(accessTokenIdx >= 0, "access_token part present");
        assertTrue(credentialIdx > accessTokenIdx, "credential part appears after access_token");
        assertTrue(messageIdx > credentialIdx, "message part appears after credential");
        assertTrue(metaDataIdx > messageIdx, "meta_data part appears after message");

        assertTrue(body.contains(ACCESS_TOKEN),
                "access_token value must be the documented Facebook app credential");

        assertTrue(body.contains("filename=\"WAMEventBuffer.dat\""),
                "message attachment must use the WAMEventBuffer.dat filename");
        assertTrue(body.contains("Content-Type: application/octet-stream"),
                "message attachment must declare application/octet-stream");

        var metaDataValueStart = body.indexOf("{\"t\":", metaDataIdx);
        assertTrue(metaDataValueStart > 0, "meta_data must include a JSON {t,p} value");
        var metaDataValueEnd = body.indexOf("}", metaDataValueStart) + 1;
        var metaDataJson = body.substring(metaDataValueStart, metaDataValueEnd);
        var parsed = JSON.parseObject(metaDataJson);
        assertNotNull(parsed.get("t"), "meta_data must carry 't' (Unix seconds)");
        assertEquals(0, parsed.getIntValue("p"),
                "meta_data 'p' is hardcoded to 0 in the WhatsApp Web bundle");
    }

    @Test
    @DisplayName("buffer bytes appear verbatim in the multipart body")
    void bufferAppearsVerbatim() {
        var buffer = new byte[]{0x42, (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE};
        var captured = uploadCapture(buffer, 200);

        var body = captured.body();
        var matched = false;
        outer:
        for (var i = 0; i <= body.length - buffer.length; i++) {
            for (var j = 0; j < buffer.length; j++) {
                if (body[i + j] != buffer[j]) {
                    continue outer;
                }
            }
            matched = true;
            break;
        }
        assertTrue(matched, "buffer bytes must appear verbatim inside the multipart body");
    }

    @Test
    @DisplayName("status codes map to documented result types")
    void statusCodeMapping() {
        assertEquals(WamPrivateStatsUploadResult.Type.SUCCESS,
                uploadCapture(new byte[]{1}, 200).result().result());
        assertEquals(WamPrivateStatsUploadResult.Type.ERROR_PARSING,
                uploadCapture(new byte[]{1}, 400).result().result());
        assertEquals(WamPrivateStatsUploadResult.Type.ERROR_ACCESS_TOKEN,
                uploadCapture(new byte[]{1}, 401).result().result());
        assertEquals(WamPrivateStatsUploadResult.Type.ERROR_SERVER_OTHER,
                uploadCapture(new byte[]{1}, 429).result().result());
        assertEquals(WamPrivateStatsUploadResult.Type.ERROR_SERVER_OTHER,
                uploadCapture(new byte[]{1}, 500).result().result());
        assertEquals(WamPrivateStatsUploadResult.Type.ERROR_OTHER,
                uploadCapture(new byte[]{1}, 404).result().result());
        assertEquals(WamPrivateStatsUploadResult.Type.ERROR_OTHER,
                uploadCapture(new byte[]{1}, 503).result().result());
    }

    @Test
    @DisplayName("token issuer failure surfaces as ERROR_CREDENTIAL")
    void issuerFailureSurfacesAsErrorCredential() {
        var hitHttp = new AtomicReference<Boolean>(false);
        var client = TestWhatsAppClient.create()
                .withSendNodeHandler(builder -> new StanzaBuilder()
                        .description("iq")
                        .attribute("type", "error")
                        .build());
        var issuer = new WamPrivateStatsTokenIssuer(client, scriptedRandom(new byte[32], new byte[32]));
        var http = new RecordingHttpClient(200, _ -> hitHttp.set(true));
        var uploader = new WamPrivateStatsUploader(issuer, http, TestWamService.create(client));

        var result = uploader.upload(new byte[]{1, 2, 3});
        assertEquals(WamPrivateStatsUploadResult.Type.ERROR_CREDENTIAL, result.result());
        assertFalse(hitHttp.get(),
                "HTTP client must not be invoked when the issuer fails");
    }

    // Runs one upload against a scripted issuer plus fake HTTP client, returning the captured tuple.
    private static CapturedUpload uploadCapture(byte[] buffer, int httpStatus) {
        var vector = loadFirstVector();
        var msg = HexFormat.of().parseHex(vector.msg);
        var scalar = HexFormat.of().parseHex(vector.scalar);
        var signed = HexFormat.of().parseHex(vector.signed);
        var pk = HexFormat.of().parseHex(vector.pk);
        var client = TestWhatsAppClient.create()
                .withSendNodeHandler(builder -> issuerResponse(signed, pk));
        var issuer = new WamPrivateStatsTokenIssuer(client, scriptedRandom(msg, scalar));
        var capturedReq = new AtomicReference<HttpRequest>();
        var capturedBody = new AtomicReference<byte[]>();
        var http = new RecordingHttpClient(httpStatus, request -> {
            capturedReq.set(request);
            capturedBody.set(extractBody(request));
        });
        var uploader = new WamPrivateStatsUploader(issuer, http, TestWamService.create(client));
        var result = uploader.upload(buffer);
        return new CapturedUpload(capturedReq.get(), capturedBody.get(), result);
    }

    // A success sign-credential IQ so the issuer sees a canned-correct response.
    private static Stanza issuerResponse(byte[] signed, byte[] pk) {
        return new StanzaBuilder()
                .description("iq")
                .attribute("type", "result")
                .content(new StanzaBuilder()
                        .description("sign_credential")
                        .content(List.of(
                                new StanzaBuilder()
                                        .description("signed_credential")
                                        .content(signed)
                                        .build(),
                                new StanzaBuilder()
                                        .description("acs_public_key")
                                        .content(pk)
                                        .build()))
                        .build())
                .build();
    }

    // issue() draws from nextBytes once for the token and once for the blinding factor.
    private static SecureRandom scriptedRandom(byte[] first, byte[] second) {
        return new SecureRandom() {
            private int call;

            @Override
            public void nextBytes(byte[] target) {
                var source = switch (call++) {
                    case 0 -> first;
                    case 1 -> second;
                    default -> throw new AssertionError("scripted SecureRandom exhausted");
                };
                System.arraycopy(source, 0, target, 0, source.length);
            }
        };
    }

    // HttpRequest.BodyPublisher does not expose its bytes directly, so subscribe synchronously and
    // collect the ByteBuffer payloads.
    private static byte[] extractBody(HttpRequest request) {
        var publisher = request.bodyPublisher().orElseThrow();
        var collected = new ByteArrayOutputStream();
        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                var arr = new byte[item.remaining()];
                item.get(arr);
                collected.writeBytes(arr);
            }

            @Override
            public void onError(Throwable throwable) {
                throw new AssertionError("unexpected publisher error", throwable);
            }

            @Override
            public void onComplete() {
            }
        });
        return collected.toByteArray();
    }

    // Reuses the same vector WamPrivateStatsTokenIssuerTest pins, so any divergence between the
    // upload and issuance paths surfaces.
    private static Vector loadFirstVector() {
        var resource = "/fixtures/wam/ed25519-live-bundle-vectors.json";
        try (var stream = WamPrivateStatsUploaderTest.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new AssertionError("ed25519 vectors fixture missing on classpath: " + resource);
            }
            var json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            var v = JSON.parseObject(json).getJSONArray("vectors").getJSONObject(0);
            return new Vector(v.getString("msg"), v.getString("scalar"),
                    v.getString("blinded"), v.getString("pk"),
                    v.getString("signed"), v.getString("unblinded"));
        } catch (IOException error) {
            throw new UncheckedIOException("failed to load " + resource, error);
        }
    }

    // An upload's captured request, body, and the uploader's classified result.
    private record CapturedUpload(HttpRequest request, byte[] body, WamPrivateStatsUploadResult result) {
    }

    // Per-vector hex strings from the KAT fixture.
    private record Vector(String msg, String scalar, String blinded, String pk, String signed, String unblinded) {
    }

    // A minimal HttpClient stub: records the request and returns a canned status. Sufficient for the
    // uploader's single send() call; unrelated abstract methods throw to surface unintended use.
    private static final class RecordingHttpClient extends HttpClient {
        private final int status;
        private final Consumer<HttpRequest> onSend;

        RecordingHttpClient(int status, Consumer<HttpRequest> onSend) {
            this.status = status;
            this.onSend = onSend;
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            throw new UnsupportedOperationException("RecordingHttpClient: sslContext() is not stubbed");
        }

        @Override
        public SSLParameters sslParameters() {
            throw new UnsupportedOperationException("RecordingHttpClient: sslParameters() is not stubbed");
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_2;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            onSend.accept(request);
            return new CannedResponse<>(request, status);
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            return CompletableFuture.completedFuture(send(request, responseBodyHandler));
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            return sendAsync(request, responseBodyHandler);
        }
    }

    // A synthetic HttpResponse carrying only the status code, since the uploader only consults
    // statusCode().
    private static final class CannedResponse<T> implements HttpResponse<T> {
        private final HttpRequest request;
        private final int status;

        CannedResponse(HttpRequest request, int status) {
            this.request = request;
            this.status = status;
        }

        @Override
        public int statusCode() {
            return status;
        }

        @Override
        public HttpRequest request() {
            return request;
        }

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (_, _) -> true);
        }

        @Override
        public T body() {
            return null;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_2;
        }
    }
}
