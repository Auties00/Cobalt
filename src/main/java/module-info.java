open module whatsapp4j {
    exports it.auties.whatsapp4j.whatsapp;
    exports it.auties.whatsapp4j.listener;
    exports it.auties.whatsapp4j.utils;
    exports it.auties.whatsapp4j.manager;
    exports it.auties.whatsapp4j.response.impl.json;
    exports it.auties.whatsapp4j.response.impl.binary;
    exports it.auties.whatsapp4j.response.model.common;
    exports it.auties.whatsapp4j.protobuf.chat;
    exports it.auties.whatsapp4j.protobuf.contact;
    exports it.auties.whatsapp4j.protobuf.info;
    exports it.auties.whatsapp4j.protobuf.model;
    exports it.auties.whatsapp4j.protobuf.message.model;
    exports it.auties.whatsapp4j.protobuf.message.standard;
    exports it.auties.whatsapp4j.protobuf.message.device;
    exports it.auties.whatsapp4j.protobuf.message.business;
    exports it.auties.whatsapp4j.protobuf.message.server;
    exports it.auties.whatsapp4j.protobuf.message.security;

    requires jakarta.websocket;
    requires com.fasterxml.jackson.databind;
    requires com.google.zxing;
    requires it.auties.protoc.api;
    requires org.bouncycastle.provider;
    requires java.prefs;
    requires java.net.http;
    requires java.compiler;

    requires transitive java.logging;
    requires static lombok;
}