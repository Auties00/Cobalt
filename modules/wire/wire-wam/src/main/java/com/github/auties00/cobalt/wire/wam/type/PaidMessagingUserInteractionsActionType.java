package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPaidMessagingUserInteractionsActionType")
@WamEnum
public enum PaidMessagingUserInteractionsActionType {
    @WamEnumConstant(0) VIEW,
    @WamEnumConstant(1) CLICK,
    @WamEnumConstant(2) READ,
    @WamEnumConstant(3) FORWARD,
    @WamEnumConstant(4) MEDIA_PLAYBACK,
    @WamEnumConstant(5) SHOW_MORE_CLICKED,
    @WamEnumConstant(6) CAROUSEL_SCROLLED
}
