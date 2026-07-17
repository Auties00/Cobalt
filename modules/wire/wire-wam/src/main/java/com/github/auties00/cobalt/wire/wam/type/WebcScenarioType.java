package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcScenarioType")
@WamEnum
public enum WebcScenarioType {
    @WamEnumConstant(0) INITIAL_PAIRING,
    @WamEnumConstant(1) OFFLINE_RESUME,
    @WamEnumConstant(2) IDLE,
    @WamEnumConstant(3) RECENT_HISTORY_SYNC,
    @WamEnumConstant(4) CHAT_NAVIGATION
}
