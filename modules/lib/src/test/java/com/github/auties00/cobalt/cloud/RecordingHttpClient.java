package com.github.auties00.cobalt.cloud;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A test {@link HttpClient} that records the last request (path, query, and decoded JSON body) and
 * returns a canned JSON response. Lets the Cloud client be exercised without a real network.
 */
final class RecordingHttpClient extends HttpClient {
    private final AtomicReference<URI> lastUri = new AtomicReference<>();
    private final AtomicReference<String> lastMethod = new AtomicReference<>();
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<java.net.http.HttpHeaders> lastHeaders = new AtomicReference<>();
    private final AtomicReference<byte[]> lastBodyBytes = new AtomicReference<>();
    private volatile String responseBody = "{}";
    private volatile int responseStatus = 200;

    void respondWith(String body) {
        this.responseBody = body;
    }

    URI lastUri() {
        return lastUri.get();
    }

    String lastMethod() {
        return lastMethod.get();
    }

    String lastBody() {
        return lastBody.get();
    }

    java.net.http.HttpHeaders lastHeaders() {
        return lastHeaders.get();
    }

    byte[] lastBodyBytes() {
        return lastBodyBytes.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
        lastUri.set(request.uri());
        lastMethod.set(request.method());
        lastHeaders.set(request.headers());
        var bytes = request.bodyPublisher().map(RecordingHttpClient::drainBytes).orElse(null);
        lastBodyBytes.set(bytes);
        lastBody.set(bytes == null ? null : new String(bytes, StandardCharsets.UTF_8));
        return (HttpResponse<T>) new StubResponse(request.uri(), responseStatus, responseBody);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                            HttpResponse.BodyHandler<T> responseBodyHandler) {
        return CompletableFuture.completedFuture(send(request, responseBodyHandler));
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                            HttpResponse.BodyHandler<T> responseBodyHandler,
                                                            HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
        return sendAsync(request, responseBodyHandler);
    }

    private static byte[] drainBytes(HttpRequest.BodyPublisher publisher) {
        var collector = new java.io.ByteArrayOutputStream();
        var done = new CompletableFuture<Void>();
        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                var bytes = new byte[item.remaining()];
                item.get(bytes);
                collector.writeBytes(bytes);
            }

            @Override
            public void onError(Throwable throwable) {
                done.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                done.complete(null);
            }
        });
        try {
            done.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException exception) {
            throw new IllegalStateException(exception.getCause());
        }
        return collector.toByteArray();
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
        try {
            return SSLContext.getDefault();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override
    public SSLParameters sslParameters() {
        return new SSLParameters();
    }

    @Override
    public Optional<Authenticator> authenticator() {
        return Optional.empty();
    }

    @Override
    public Version version() {
        return Version.HTTP_1_1;
    }

    @Override
    public Optional<Executor> executor() {
        return Optional.empty();
    }

    @Override
    public WebSocket.Builder newWebSocketBuilder() {
        throw new UnsupportedOperationException();
    }

    /**
     * A minimal {@link HttpResponse} carrying a fixed status and string body.
     */
    private record StubResponse(URI uri, int statusCode, String body) implements HttpResponse<Object> {
        @Override
        public HttpRequest request() {
            return null;
        }

        @Override
        public Optional<HttpResponse<Object>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public java.net.http.HttpHeaders headers() {
            return java.net.http.HttpHeaders.of(Map.of(), (a, b) -> true);
        }

        @Override
        public Optional<javax.net.ssl.SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }
    }
}
