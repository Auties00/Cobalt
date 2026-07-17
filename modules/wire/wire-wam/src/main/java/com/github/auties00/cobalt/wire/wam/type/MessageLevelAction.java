package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMessageLevelAction")
@WamEnum
public enum MessageLevelAction {
    @WamEnumConstant(0) STAR,
    @WamEnumConstant(1) FORWARD,
    @WamEnumConstant(2) COPY,
    @WamEnumConstant(3) REPORT,
    @WamEnumConstant(4) DELETE,
    @WamEnumConstant(5) BUTTON_CLICK,
    @WamEnumConstant(6) LINK_CLICK,
    @WamEnumConstant(7) MESSAGE_VIEW,
    @WamEnumConstant(8) UNSTAR,
    @WamEnumConstant(9) SHARE,
    @WamEnumConstant(10) ADD_CONTACT,
    @WamEnumConstant(11) RATE,
    @WamEnumConstant(12) PIN_MESSAGE,
    @WamEnumConstant(13) UNPIN_MESSAGE,
    @WamEnumConstant(14) DOWNLOAD_HD_MEDIA,
    @WamEnumConstant(15) URL_FRICTION_BANNER_VIEW
}
