package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWebcMediaEditorSendWamEvent")
@WamEvent(id = 2890)
public interface WebcMediaEditorSendEvent extends WamEventSpec {
    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong blurImageCount();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong editedImageCount();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong emojiLayerCount();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong imageCount();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong paintedImageCount();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong stickerLayerCount();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong textLayerCount();
}
