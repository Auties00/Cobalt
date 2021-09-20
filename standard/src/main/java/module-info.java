open module it.auties.whatsapp4j.standard {
    requires jakarta.websocket;
    requires com.fasterxml.jackson.databind;
    requires com.google.zxing;
    requires java.desktop;
    requires it.auties.protoc.api;
    requires org.bouncycastle.provider;
    requires java.prefs;
    requires java.net.http;
    requires java.compiler;
    requires it.auties.whatsapp4j.shared;

    requires transitive java.logging;
    requires static lombok;
    requires static jdk.unsupported;
}