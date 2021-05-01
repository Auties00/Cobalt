module it.auties.whatsapp4j {
    exports it.auties.whatsapp4j.api;
    exports it.auties.whatsapp4j.listener;
    exports it.auties.whatsapp4j.response.impl;
    exports it.auties.whatsapp4j.utils;
    exports it.auties.whatsapp4j.manager;
    exports it.auties.whatsapp4j.binary;
    exports it.auties.whatsapp4j.model;
    exports it.auties.whatsapp4j.builder;
    exports it.auties.whatsapp4j.socket;

    requires com.fasterxml.jackson.databind;
    requires com.google.common;
    requires java.prefs;
    requires java.net.http;
    requires java.compiler;
    requires jakarta.validation;
    requires jakarta.xml.bind;
    requires jakarta.websocket.api;

    requires static lombok;
    requires org.bouncycastle.provider;

    opens it.auties.whatsapp4j.model to com.fasterxml.jackson.databind;
    opens it.auties.whatsapp4j.response.impl to com.fasterxml.jackson.databind;
    opens it.auties.whatsapp4j.response.model to com.fasterxml.jackson.databind;
    opens it.auties.whatsapp4j.request.impl to com.fasterxml.jackson.databind;
    opens it.auties.whatsapp4j.request.model to com.fasterxml.jackson.databind;
    opens it.auties.whatsapp4j.utils to com.fasterxml.jackson.databind;
    opens it.auties.whatsapp4j.binary to com.fasterxml.jackson.databind;
}