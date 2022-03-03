open module it.auties.whatsapp4j {
    requires static lombok;
    requires transitive java.logging;
    requires transitive java.desktop;

    requires jakarta.websocket;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jdk8;
    requires com.fasterxml.jackson.datatype.jsr310;

    requires it.auties.protoc.api;
    requires it.auties.map;

    requires com.google.zxing;
    requires com.google.zxing.javase;

    requires io.github.classgraph;

    requires java.net.http;
    requires jdk.crypto.ec;

    requires org.bouncycastle.provider;
    requires signal.protocol.java;
    requires curve25519.java;
    requires it.auties.bytebuffer;
}