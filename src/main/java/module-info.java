open module it.auties.whatsapp4j {
    requires static lombok;
    requires transitive java.logging;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires java.prefs;
    requires org.bouncycastle.provider;
    requires jakarta.websocket;
    requires it.auties.protoc.api;
    requires com.google.zxing;
    requires io.github.classgraph;
    requires java.net.http;
    requires jdk.crypto.ec;
    requires signal.protocol.java;
    requires curve25519.java;
    requires netty.buffer;
}