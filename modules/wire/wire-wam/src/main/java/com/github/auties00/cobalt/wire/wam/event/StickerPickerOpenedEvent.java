package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;


@WhatsAppWebModule(moduleName = "WAWebStickerPickerOpenedWamEvent")
@WamEvent(id = 1854)
public interface StickerPickerOpenedEvent extends WamEventSpec {
}
