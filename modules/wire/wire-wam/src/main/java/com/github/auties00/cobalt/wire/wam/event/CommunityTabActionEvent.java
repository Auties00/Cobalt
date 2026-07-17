package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebCommunityTabActionWamEvent")
@WamEvent(id = 3496)
public interface CommunityTabActionEvent extends WamEventSpec {
    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong communityNoActionTabViews();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong communityTabGroupNavigations();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong communityTabToHomeViews();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong communityTabViews();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong communityTabViewsViaContextMenu();
}
