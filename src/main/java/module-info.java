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
    exports com.github.auties00.cobalt.model.action;
    exports com.github.auties00.cobalt.model.business;
    exports com.github.auties00.cobalt.model.chat;
    exports com.github.auties00.cobalt.model.contact;
    exports com.github.auties00.cobalt.model.info;
    exports com.github.auties00.cobalt.model.media;
    exports com.github.auties00.cobalt.model.message.server;
    exports com.github.auties00.cobalt.model.message.model;
    exports com.github.auties00.cobalt.model.message.payment;
    exports com.github.auties00.cobalt.model.message.standard;
    exports com.github.auties00.cobalt.model.product;
    exports com.github.auties00.cobalt.model.setting;
    exports com.github.auties00.cobalt.core.json.response;
    exports com.github.auties00.cobalt.model.payment;
    exports com.github.auties00.cobalt.model.message.button;
    exports com.github.auties00.cobalt.model.privacy;
    exports com.github.auties00.cobalt.model.poll;
    exports com.github.auties00.cobalt.model.button.interactive;
    exports com.github.auties00.cobalt.model.button.base;
    exports com.github.auties00.cobalt.model.button.template.hydrated;
    exports com.github.auties00.cobalt.model.auth;
    exports com.github.auties00.cobalt.model.sync;
    exports com.github.auties00.cobalt.model.call;
    exports com.github.auties00.cobalt.model.button.template.highlyStructured;
    exports com.github.auties00.cobalt.model.jid;
    exports com.github.auties00.cobalt.model.newsletter;
    exports com.github.auties00.cobalt.exception;
    exports com.github.auties00.cobalt.io.node;
    exports com.github.auties00.cobalt.store;
    exports com.github.auties00.cobalt.core.node;
}