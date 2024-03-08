package it.auties.whatsapp.registration;

import it.auties.whatsapp.util.ProxyAuthenticator;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

class HttpClient {
    private final ProxyAuthenticator authenticator;
    private volatile SSLFactoryWithParams factoryWithParams;
    HttpClient() {
        this.authenticator = new ProxyAuthenticator();
    }

    public CompletableFuture<byte[]> get(URI uri, Proxy proxy, Map<String, ?> headers) {
        return sendRequest("GET", uri, proxy, headers);
    }

    public CompletableFuture<byte[]> post(URI uri, Proxy proxy, Map<String, ?> headers) {
        return sendRequest("POST", uri, proxy, headers);
    }

    private CompletableFuture<byte[]> sendRequest(String method, URI uri, Proxy proxy, Map<String, ?> headers) {
        var future = new CompletableFuture<byte[]>();
        Thread.startVirtualThread(() -> {
            try {
                var url = uri.toURL();
                var connection = (HttpsURLConnection) createConnection(proxy, url);
                connection.setRequestMethod(method);
                headers.forEach((key, value) -> connection.setRequestProperty(key, String.valueOf(value)));
                connection.setAuthenticator(authenticator);
                connection.setSSLSocketFactory(getOrCreateParams());
                connection.setInstanceFollowRedirects(true);
                connection.connect();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IllegalStateException("Invalid status code: " + connection.getResponseCode());
                }

                try (var inputStream = connection.getInputStream()) {
                    future.complete(inputStream.readAllBytes());
                }
            } catch (IOException exception) {
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    private SSLSocketFactory getOrCreateParams() {
        try {
            if(factoryWithParams != null) {
                return factoryWithParams;
            }

            var sslContext = SSLContext.getInstance("TLSv1." + (ThreadLocalRandom.current().nextBoolean() ? 2 : 3));
            sslContext.init(null, null, new SecureRandom());
            var sslParameters = sslContext.getDefaultSSLParameters();
            var supportedCiphers = Arrays.stream(sslParameters.getCipherSuites())
                    .filter(entry -> ThreadLocalRandom.current().nextBoolean())
                    .collect(Collectors.collectingAndThen(Collectors.toList(), result -> {
                        Collections.shuffle(result);
                        return result;
                    }))
                    .toArray(String[]::new);
            sslParameters.setCipherSuites(supportedCiphers);
            var supportedNamedGroups = Arrays.stream(sslParameters.getNamedGroups())
                    .filter(entry -> ThreadLocalRandom.current().nextBoolean())
                    .collect(Collectors.collectingAndThen(Collectors.toList(), result -> {
                        Collections.shuffle(result);
                        return result;
                    }))
                    .toArray(String[]::new);
            sslParameters.setNamedGroups(supportedNamedGroups);
            return factoryWithParams = new SSLFactoryWithParams(sslContext.getSocketFactory(), sslParameters);
        } catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    private URLConnection createConnection(Proxy proxy, URL url) throws IOException {
        return proxy == null ? url.openConnection() : url.openConnection(proxy);
    }

    private static class SSLFactoryWithParams extends SSLSocketFactory {
        private final SSLSocketFactory sslSocketFactory;
        private final SSLParameters sslParameters;
        private SSLFactoryWithParams(SSLSocketFactory sslSocketFactory, SSLParameters sslParameters) {
            this.sslSocketFactory = sslSocketFactory;
            this.sslParameters = sslParameters;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return sslParameters.getCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return sslParameters.getCipherSuites();
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            var socket = (SSLSocket) sslSocketFactory.createSocket(s, host, port, autoClose);
            socket.setSSLParameters(sslParameters);
            return socket;
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            var socket = (SSLSocket) sslSocketFactory.createSocket(host, port);
            socket.setSSLParameters(sslParameters);
            return socket;
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
            var socket = (SSLSocket) sslSocketFactory.createSocket(host, port, localHost, localPort);
            socket.setSSLParameters(sslParameters);
            return socket;
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            var socket = (SSLSocket) sslSocketFactory.createSocket(host, port);
            socket.setSSLParameters(sslParameters);
            return socket;
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            var socket = (SSLSocket) sslSocketFactory.createSocket(address, port, localAddress, localPort);
            socket.setSSLParameters(sslParameters);
            return socket;
        }

        @Override
        public Socket createSocket(Socket s, InputStream consumed, boolean autoClose) throws IOException {
            var socket = (SSLSocket) sslSocketFactory.createSocket(s, consumed, autoClose);
            socket.setSSLParameters(sslParameters);
            return socket;
        }

        @Override
        public Socket createSocket() throws IOException {
            var socket = (SSLSocket) sslSocketFactory.createSocket();
            socket.setSSLParameters(sslParameters);
            return socket;
        }
    }
}
