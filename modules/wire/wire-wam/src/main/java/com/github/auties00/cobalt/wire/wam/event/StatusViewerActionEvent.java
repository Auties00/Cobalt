package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.InlineVideoType;
import com.github.auties00.cobalt.wire.wam.type.StatusCategory;
import com.github.auties00.cobalt.wire.wam.type.StatusViewActionType;
import com.github.auties00.cobalt.wire.wam.type.TopBarAttributionType;
import com.github.auties00.cobalt.wire.wam.type.UrlStatusClicked;
import com.github.auties00.cobalt.wire.wam.type.UrlStatusType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebStatusViewerActionWamEvent")
@WamEvent(id = 6692)
public interface StatusViewerActionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<TopBarAttributionType> attributionType();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> attributionTypes();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<InlineVideoType> externalSourceDomainType();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<StatusCategory> statusCategory();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<UrlStatusClicked> urlStatusClicked();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<UrlStatusType> urlStatusType();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<StatusViewActionType> viewerActionType();
}
