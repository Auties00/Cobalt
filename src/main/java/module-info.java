module com.github.auties00.cobalt {
    // Http client
    requires java.net.http;

    // Cryptography
    requires com.github.auties00.libsignal;
    requires com.github.auties00.curve25519;

    // QR related dependencies
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires it.auties.qr;
    requires static java.desktop;

    // Serialization (Protobuf, JSON)
    requires it.auties.protobuf.base;
    requires com.alibaba.fastjson2;

    // Generate message previews
    requires it.auties.linkpreview;
    requires com.googlecode.ezvcard;

    // Message store
    requires com.github.auties00.collections;

    // Mobile api
    requires net.dongliu.apkparser;
    requires com.google.i18n.phonenumbers.libphonenumber;

    exports com.github.auties00.cobalt.api;
    exports com.github.auties00.cobalt.model.proto.action;
    exports com.github.auties00.cobalt.model.proto.business;
    exports com.github.auties00.cobalt.model.proto.chat;
    exports com.github.auties00.cobalt.model.proto.contact;
    exports com.github.auties00.cobalt.model.proto.info;
    exports com.github.auties00.cobalt.model.proto.media;
    exports com.github.auties00.cobalt.model.proto.message.server;
    exports com.github.auties00.cobalt.model.proto.message.model;
    exports com.github.auties00.cobalt.model.proto.message.payment;
    exports com.github.auties00.cobalt.model.proto.message.standard;
    exports com.github.auties00.cobalt.model.proto.product;
    exports com.github.auties00.cobalt.model.proto.setting;
    exports com.github.auties00.cobalt.model.json.response;
    exports com.github.auties00.cobalt.model.proto.payment;
    exports com.github.auties00.cobalt.model.proto.message.button;
    exports com.github.auties00.cobalt.model.proto.privacy;
    exports com.github.auties00.cobalt.model.proto.poll;
    exports com.github.auties00.cobalt.model.proto.button.interactive;
    exports com.github.auties00.cobalt.model.proto.button.base;
    exports com.github.auties00.cobalt.model.proto.button.template.hydrated;
    exports com.github.auties00.cobalt.model.proto.auth;
    exports com.github.auties00.cobalt.model.proto.sync;
    exports com.github.auties00.cobalt.model.proto.call;
    exports com.github.auties00.cobalt.model.proto.button.template.highlyStructured;
    exports com.github.auties00.cobalt.model.proto.jid;
    exports com.github.auties00.cobalt.model.proto.newsletter;
    exports com.github.auties00.cobalt.exception;
    exports com.github.auties00.cobalt.io.node;
    exports com.github.auties00.cobalt.store;
    exports com.github.auties00.cobalt.model.node;
    exports com.github.auties00.cobalt.model.media;
}