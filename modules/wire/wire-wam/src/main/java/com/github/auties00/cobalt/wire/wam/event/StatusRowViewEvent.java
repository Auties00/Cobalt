package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.StatusRowEntryMethod;
import com.github.auties00.cobalt.wire.wam.type.StatusRowSection;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebStatusRowViewWamEvent")
@WamEvent(id = 1656)
public interface StatusRowViewEvent extends WamEventSpec {
    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> psaCampaigns();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<StatusRowEntryMethod> statusRowEntryMethod();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong statusRowIndex();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<StatusRowSection> statusRowSection();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong statusRowUnreadItemCount();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong statusRowViewCount();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong statusSessionId();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong statusViewerSessionId();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong updatesTabSessionId();
}
