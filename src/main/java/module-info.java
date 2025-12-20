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
    requires com.github.auties00.cobalt;

    // Client
    exports com.github.auties00.cobalt.client;

    // Exception
    exports com.github.auties00.cobalt.exception;

    // Node
    exports com.github.auties00.cobalt.node;

    // Proto models - action
    exports com.github.auties00.cobalt.model.action;

    // Proto models - auth
    exports com.github.auties00.cobalt.model.auth;

    // Proto models - business
    exports com.github.auties00.cobalt.model.business;

    // Proto models - button
    exports com.github.auties00.cobalt.model.button.interactive;
    exports com.github.auties00.cobalt.model.button.base;
    exports com.github.auties00.cobalt.model.button.template.hydrated;
    exports com.github.auties00.cobalt.model.button.template.highlyStructured;

    // Proto models - call
    exports com.github.auties00.cobalt.model.call;

    // Proto models - chat
    exports com.github.auties00.cobalt.model.chat;

    // Proto models - contact
    exports com.github.auties00.cobalt.model.contact;

    // Proto models - info
    exports com.github.auties00.cobalt.model.info;

    // Proto models - jid
    exports com.github.auties00.cobalt.model.jid;

    // Proto models - media
    exports com.github.auties00.cobalt.model.media;

    // Proto models - message
    exports com.github.auties00.cobalt.model.message.button;
    exports com.github.auties00.cobalt.model.message.server;
    exports com.github.auties00.cobalt.model.message.model;
    exports com.github.auties00.cobalt.model.message.payment;
    exports com.github.auties00.cobalt.model.message.standard;

    // Proto models - newsletters
    exports com.github.auties00.cobalt.model.newsletter;

    // Proto models - payment
    exports com.github.auties00.cobalt.model.payment;

    // Proto models - poll
    exports com.github.auties00.cobalt.model.poll;

    // Proto models - preferences
    exports com.github.auties00.cobalt.model.preferences;

    // Proto models - privacy
    exports com.github.auties00.cobalt.model.privacy;

    // Proto models - product
    exports com.github.auties00.cobalt.model.product;

    // Proto models - setting
    exports com.github.auties00.cobalt.model.setting;

    // Proto models - sync
    exports com.github.auties00.cobalt.model.sync;

    // Store
    exports com.github.auties00.cobalt.store;

    // Media
    exports com.github.auties00.cobalt.media;
}