package it.auties.whatsapp.registration.http;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
class ProxySSLFactory extends SSLSocketFactory {
    private final SSLSocketFactory sslSocketFactory;
    private final SSLParameters sslParameters;

    ProxySSLFactory(SSLSocketFactory sslSocketFactory, SSLParameters sslParameters) {
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