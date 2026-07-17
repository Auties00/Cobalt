package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumTopBarAttributionType")
@WamEnum
public enum TopBarAttributionType {
    @WamEnumConstant(0) MUSIC,
    @WamEnumConstant(1) CROSSPOSTING,
    @WamEnumConstant(2) MENTION,
    @WamEnumConstant(3) RESHARED_STATUS,
    @WamEnumConstant(4) RESHARED_FROM_MENTION,
    @WamEnumConstant(5) CHANNEL,
    @WamEnumConstant(6) FB,
    @WamEnumConstant(7) IG,
    @WamEnumConstant(8) SPOTIFY,
    @WamEnumConstant(9) E2EE,
    @WamEnumConstant(10) RL,
    @WamEnumConstant(11) GROUP_STATUS,
    @WamEnumConstant(12) SHARING_API,
    @WamEnumConstant(13) FORWARD_STATUS,
    @WamEnumConstant(14) AI_CREATED,
    @WamEnumConstant(15) LAYOUTS,
    @WamEnumConstant(16) CLOSE_SHARING,
    @WamEnumConstant(17) CHANNEL_STATUS
}
