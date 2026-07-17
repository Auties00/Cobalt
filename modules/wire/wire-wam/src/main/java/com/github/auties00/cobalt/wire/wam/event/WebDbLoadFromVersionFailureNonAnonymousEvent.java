package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.WebDbLoaderType;
import com.github.auties00.cobalt.wire.wam.type.WebDbNameType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebWebDbLoadFromVersionFailureNonAnonymousWamEvent")
@WamEvent(id = 4814)
public interface WebDbLoadFromVersionFailureNonAnonymousEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<WebDbLoaderType> webDbLoader();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<WebDbNameType> webDbName();
}
