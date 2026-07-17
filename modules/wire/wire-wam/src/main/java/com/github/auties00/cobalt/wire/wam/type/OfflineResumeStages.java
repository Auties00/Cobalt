package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumOfflineResumeStages")
@WamEnum
public enum OfflineResumeStages {
    @WamEnumConstant(1) PAGE_LOAD,
    @WamEnumConstant(2) SOCKET_CONNECT,
    @WamEnumConstant(3) PROCESS_COMPLETE,
    @WamEnumConstant(4) SCREEN_LOAD,
    @WamEnumConstant(5) OFFLINE_PREVIEW,
    @WamEnumConstant(6) OFFLINE_COMPLETE_RECEIVED,
    @WamEnumConstant(7) PREACKS_SENT
}
