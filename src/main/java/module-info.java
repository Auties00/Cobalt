open module it.auties.whatsapp4j {
    requires static lombok;
    requires transitive java.logging;
    requires transitive java.desktop;

    requires jakarta.websocket;
    requires java.net.http;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jdk8;
    requires com.fasterxml.jackson.datatype.jsr310;

    requires it.auties.protoc.api;
    requires it.auties.map;

    requires com.google.zxing;
    requires com.google.zxing.javase;

    requires io.github.classgraph;

    requires org.bouncycastle.provider;
    requires it.auties.bytebuffer;
    requires it.auties.curve;

    exports it.auties.whatsapp.api;
    exports it.auties.whatsapp.manager;
    exports it.auties.whatsapp.protobuf.action;
    exports it.auties.whatsapp.protobuf.business;
    exports it.auties.whatsapp.protobuf.button;
    exports it.auties.whatsapp.protobuf.chat;
    exports it.auties.whatsapp.protobuf.contact;
    exports it.auties.whatsapp.protobuf.info;
    exports it.auties.whatsapp.protobuf.media;
    exports it.auties.whatsapp.protobuf.message.button;
    exports it.auties.whatsapp.protobuf.message.server;
    exports it.auties.whatsapp.protobuf.message.device;
    exports it.auties.whatsapp.protobuf.message.model;
    exports it.auties.whatsapp.protobuf.message.payment;
    exports it.auties.whatsapp.protobuf.message.standard;
    exports it.auties.whatsapp.protobuf.product;
    exports it.auties.whatsapp.protobuf.setting;
    exports it.auties.whatsapp.protobuf.sync;
}