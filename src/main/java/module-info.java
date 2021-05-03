module whatsapp4j {
    exports it.auties.whatsapp4j.api;
    exports it.auties.whatsapp4j.listener;
    exports it.auties.whatsapp4j.response.impl.json;
    exports it.auties.whatsapp4j.response.impl.binary;
    exports it.auties.whatsapp4j.utils;
    exports it.auties.whatsapp4j.manager;
    exports it.auties.whatsapp4j.model;
    exports it.auties.whatsapp4j.binary;
    exports it.auties.whatsapp4j.request.model;
    exports it.auties.whatsapp4j.response.model.common;

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

    opens it.auties.whatsapp4j.socket;
    opens it.auties.whatsapp4j.model;
    opens it.auties.whatsapp4j.request.impl;
    opens it.auties.whatsapp4j.request.model;
    opens it.auties.whatsapp4j.binary;
    opens it.auties.whatsapp4j.response.model.json;
    opens it.auties.whatsapp4j.response.model.binary;
    opens it.auties.whatsapp4j.response.model.common;
    opens it.auties.whatsapp4j.response.impl.json;
    opens it.auties.whatsapp4j.response.impl.binary;
}