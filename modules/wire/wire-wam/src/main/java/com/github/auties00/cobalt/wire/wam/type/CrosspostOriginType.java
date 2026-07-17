package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCrosspostOriginType")
@WamEnum
public enum CrosspostOriginType {
    @WamEnumConstant(1) AUTO_XPOST_STATUS_CREATION_SHARE_VIEW,
    @WamEnumConstant(2) AUTO_XPOST_STATUS_CREATION_AUDIENCE_SELECTOR_VIEW,
    @WamEnumConstant(3) CONTEXTUAL_SHARE_ICON,
    @WamEnumConstant(4) STATUS_LIST_SINGLE_STATUS_SHARE_MENU,
    @WamEnumConstant(5) STATUS_LIST_SINGLE_STATUS_LONG_PRESS_SHARE_MENU,
    @WamEnumConstant(6) STATUS_LIST_MULTIPLE_STATUSES_SHARE_MENU,
    @WamEnumConstant(7) STATUS_DETAIL_TOP_SHARE_MENU,
    @WamEnumConstant(8) STATUS_DETAIL_OVERLAY_APP_ICON,
    @WamEnumConstant(9) XPOST_RETRY,
    @WamEnumConstant(10) AUTO_XPOST_RETRY,
    @WamEnumConstant(11) AUTO_XPOST_SHARE_EXTENSION
}
