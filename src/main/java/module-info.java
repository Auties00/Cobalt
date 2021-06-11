open module whatsapp4j {
    exports it.auties.whatsapp4j.api;
    exports it.auties.whatsapp4j.listener;
    exports it.auties.whatsapp4j.utils;
    exports it.auties.whatsapp4j.manager;
    exports it.auties.whatsapp4j.response.impl.json;
    exports it.auties.whatsapp4j.response.impl.binary;
    exports it.auties.whatsapp4j.response.model.common;
    exports it.auties.whatsapp4j.protobuf.chat;
    exports it.auties.whatsapp4j.protobuf.contact;
    exports it.auties.whatsapp4j.protobuf.info;
    exports it.auties.whatsapp4j.protobuf.message;
    exports it.auties.whatsapp4j.protobuf.model;

    requires com.fasterxml.jackson.databind;
    requires com.google.common;
    requires it.auties.protoc.api;
    requires org.bouncycastle.provider;
    requires java.prefs;
    requires java.net.http;
    requires java.compiler;
    requires jakarta.validation;
    requires jakarta.xml.bind;
    requires jakarta.websocket.api;

    requires lombok;
}