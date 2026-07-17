package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChannelLinkShareScreen")
@WamEnum
public enum ChannelLinkShareScreen {
    @WamEnumConstant(1) CONTEXT_CARD,
    @WamEnumConstant(2) CHANNEL_INFO,
    @WamEnumConstant(3) CHANNEL_THREAD,
    @WamEnumConstant(4) SHARE_LINK_SCREEN,
    @WamEnumConstant(5) UPDATES_TAB,
    @WamEnumConstant(6) QR_CODE_SCREEN
}
