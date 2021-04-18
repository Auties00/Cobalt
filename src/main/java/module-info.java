module it.auties.whatsapp4j {
    exports it.auties.whatsapp4j.api;
    exports it.auties.whatsapp4j.response.impl;
    exports it.auties.whatsapp4j.response.model;
    exports it.auties.whatsapp4j.request.impl;
    exports it.auties.whatsapp4j.request.model;

    requires jakarta.activation;
    requires jakarta.validation;

    requires com.fasterxml.jackson.databind;

    //shades
    requires static ez.vcard;
    requires hkdf;

    requires static lombok;

    requires jakarta.xml.bind;

    requires jakarta.websocket.api;
    requires java.net.http;
    requires com.google.common;
    requires io.github.classgraph;
    requires java.prefs;

    opens it.auties.whatsapp4j.response.impl to com.fasterxml.jackson.databind;
    opens it.auties.whatsapp4j.response.model to com.fasterxml.jackson.databind;
    opens it.auties.whatsapp4j.request.impl to com.fasterxml.jackson.databind;
    opens it.auties.whatsapp4j.request.model to com.fasterxml.jackson.databind;
}