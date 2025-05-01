open module it.auties.cobalt {
    // Cryptography
    requires it.auties.curve;

    // QR related dependencies
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires it.auties.qr;
    requires static java.desktop;

    // Serialization (Protobuf, JSON)
    requires it.auties.protobuf.base;
    requires java.compiler;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.module.paramnames;
    requires com.fasterxml.jackson.datatype.jdk8;
    requires com.fasterxml.jackson.datatype.jsr310;

    // Generate message previews
    requires it.auties.linkpreview;
    requires com.aspose.words;
    requires com.github.kokorin.jaffree;
    requires com.googlecode.ezvcard;

    // Mobile api
    requires net.dongliu.apkparser;
    requires com.google.i18n.phonenumbers.libphonenumber;

    // Web api
    requires java.net.http;

    exports it.auties.whatsapp.api;
    exports it.auties.whatsapp.controller;
    exports it.auties.whatsapp.model.action;
    exports it.auties.whatsapp.model.business;
    exports it.auties.whatsapp.model.chat;
    exports it.auties.whatsapp.model.contact;
    exports it.auties.whatsapp.model.info;
    exports it.auties.whatsapp.model.media;
    exports it.auties.whatsapp.model.message.server;
    exports it.auties.whatsapp.model.message.model;
    exports it.auties.whatsapp.model.message.payment;
    exports it.auties.whatsapp.model.message.standard;
    exports it.auties.whatsapp.model.product;
    exports it.auties.whatsapp.model.setting;
    exports it.auties.whatsapp.model.response;
    exports it.auties.whatsapp.model.payment;
    exports it.auties.whatsapp.model.message.button;
    exports it.auties.whatsapp.model.privacy;
    exports it.auties.whatsapp.model.poll;
    exports it.auties.whatsapp.model.mobile;
    exports it.auties.whatsapp.model.button.interactive;
    exports it.auties.whatsapp.model.button.base;
    exports it.auties.whatsapp.model.button.template.hydrated;
    exports it.auties.whatsapp.model.companion;
    exports it.auties.whatsapp.model.signal.session;
    exports it.auties.whatsapp.model.signal.auth;
    exports it.auties.whatsapp.model.sync;
    exports it.auties.whatsapp.model.signal.sender;
    exports it.auties.whatsapp.model.signal.keypair;
    exports it.auties.whatsapp.model.call;
    exports it.auties.whatsapp.model.node;
    exports it.auties.whatsapp.model.button.template.highlyStructured;
    exports it.auties.whatsapp.model.jid;
    exports it.auties.whatsapp.model.newsletter;
    exports it.auties.whatsapp.exception;
}