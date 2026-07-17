package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumVideoPlayOrigin")
@WamEnum
public enum VideoPlayOrigin {
    @WamEnumConstant(1) CONVERSATION,
    @WamEnumConstant(2) GALLERY_PICKER,
    @WamEnumConstant(3) STARRED_MESSAGES,
    @WamEnumConstant(4) MEDIA_VIEW_PAGER,
    @WamEnumConstant(5) OTHER_ORIGIN,
    @WamEnumConstant(6) CHANNELS,
    @WamEnumConstant(7) STATUS
}
