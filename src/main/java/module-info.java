open module whatsapp4j {
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
    requires org.bouncycastle.provider;

    requires static lombok;
}