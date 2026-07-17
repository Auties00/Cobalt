package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;


@WhatsAppWebModule(moduleName = "WAWebHfmTextSearchCompleteWamEvent")
@WamEvent(id = 2186)
public interface HfmTextSearchCompleteEvent extends WamEventSpec {
}
