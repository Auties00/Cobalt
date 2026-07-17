package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumActionCode")
@WamEnum
public enum ActionCode {
    @WamEnumConstant(1) OPEN_MEDIA_HUB,
    @WamEnumConstant(2) SEARCH,
    @WamEnumConstant(3) LIST,
    @WamEnumConstant(4) ICON,
    @WamEnumConstant(5) SCROLL,
    @WamEnumConstant(6) CLICK,
    @WamEnumConstant(7) CLOSE_MEDIA_HUB,
    @WamEnumConstant(8) REPLY,
    @WamEnumConstant(9) FORWARD,
    @WamEnumConstant(10) DOWNLOAD,
    @WamEnumConstant(11) STAR,
    @WamEnumConstant(12) DELETE,
    @WamEnumConstant(13) OPEN,
    @WamEnumConstant(14) GO_TO_MESSAGE,
    @WamEnumConstant(15) MULTISELECT,
    @WamEnumConstant(16) SORT,
    @WamEnumConstant(17) OPEN_MENU,
    @WamEnumConstant(18) FILTER
}
