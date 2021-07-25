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
    exports it.auties.whatsapp4j.protobuf.message.model;
    exports it.auties.whatsapp4j.protobuf.message.standard;
    exports it.auties.whatsapp4j.protobuf.message.device;
    exports it.auties.whatsapp4j.protobuf.message.business;
    exports it.auties.whatsapp4j.protobuf.message.server;
    exports it.auties.whatsapp4j.protobuf.message.security;
    exports it.auties.whatsapp4j.protobuf.info;
    exports it.auties.whatsapp4j.protobuf.model.biz;
    exports it.auties.whatsapp4j.protobuf.model.button;
    exports it.auties.whatsapp4j.protobuf.model.call;
    exports it.auties.whatsapp4j.protobuf.model.history;
    exports it.auties.whatsapp4j.protobuf.model.hsm;
    exports it.auties.whatsapp4j.protobuf.model.media;
    exports it.auties.whatsapp4j.protobuf.model.companion;
    exports it.auties.whatsapp4j.protobuf.model.setting;
    exports it.auties.whatsapp4j.protobuf.model.syncd;
    exports it.auties.whatsapp4j.protobuf.model.product;
    exports it.auties.whatsapp4j.protobuf.model.key;
    exports it.auties.whatsapp4j.protobuf.model.server;
    exports it.auties.whatsapp4j.protobuf.model.misc;
    exports it.auties.whatsapp4j.protobuf.model.client;
    exports it.auties.whatsapp4j.protobuf.model.recent;

    requires jakarta.websocket;
    requires com.fasterxml.jackson.databind;
    requires com.google.zxing;
    requires java.desktop;
    requires it.auties.protoc.api;
    requires org.bouncycastle.provider;
    requires java.prefs;
    requires java.net.http;
    requires java.compiler;
    requires noise.java;

    requires transitive java.logging;
    requires static lombok;
    requires static jdk.unsupported;
}