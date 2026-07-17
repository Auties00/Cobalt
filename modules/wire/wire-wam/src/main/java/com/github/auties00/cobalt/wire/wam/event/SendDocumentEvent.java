package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.DocumentType;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebSendDocumentWamEvent")
@WamEvent(id = 2172)
public interface SendDocumentEvent extends WamEventSpec {
    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> documentExt();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong documentPageSize();

    @WamProperty(index = 1, type = WamType.FLOAT)
    OptionalDouble documentSize();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<DocumentType> documentType();
}
