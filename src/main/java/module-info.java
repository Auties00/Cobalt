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

    // API
    exports com.github.auties00.cobalt.api;

    // Don't expose ARGO models - they are for internal use only

    // Core - Node
    exports com.github.auties00.cobalt.model.core.node;

    // Core - Web app state sync
    exports com.github.auties00.cobalt.model.core.sync;

    // Don't expose JSON models - they are for internal use only

    // Proto models - action
    exports com.github.auties00.cobalt.model.proto.action;

    // Don't expose auth proto models

    // Proto models - business
    exports com.github.auties00.cobalt.model.proto.business;

    // Proto models - button
    exports com.github.auties00.cobalt.model.proto.button.interactive;
    exports com.github.auties00.cobalt.model.proto.button.base;
    exports com.github.auties00.cobalt.model.proto.button.template.hydrated;
    exports com.github.auties00.cobalt.model.proto.button.template.highlyStructured;

    // Proto models - call
    exports com.github.auties00.cobalt.model.proto.call;

    // Proto models - chat
    exports com.github.auties00.cobalt.model.proto.chat;

    // Proto models - contact
    exports com.github.auties00.cobalt.model.proto.contact;

    // Proto models - info
    exports com.github.auties00.cobalt.model.proto.info;

    // Proto models - jid
    exports com.github.auties00.cobalt.model.proto.jid;

    // Proto models - media
    exports com.github.auties00.cobalt.model.proto.media;

    // Proto models - message
    exports com.github.auties00.cobalt.model.proto.message.button;
    exports com.github.auties00.cobalt.model.proto.message.server;
    exports com.github.auties00.cobalt.model.proto.message.model;
    exports com.github.auties00.cobalt.model.proto.message.payment;
    exports com.github.auties00.cobalt.model.proto.message.standard;

    // Proto models - newsletters
    exports com.github.auties00.cobalt.model.proto.newsletter;

    // Proto models - payment
    exports com.github.auties00.cobalt.model.proto.payment;

    // Proto models - poll
    exports com.github.auties00.cobalt.model.proto.poll;

    // Proto models - privacy
    exports com.github.auties00.cobalt.model.proto.privacy;

    // Proto models - product
    exports com.github.auties00.cobalt.model.proto.product;

    // Proto models - setting
    exports com.github.auties00.cobalt.model.proto.setting;

    // Proto models - sync
    exports com.github.auties00.cobalt.model.proto.sync;

    // Store
    exports com.github.auties00.cobalt.store;
}