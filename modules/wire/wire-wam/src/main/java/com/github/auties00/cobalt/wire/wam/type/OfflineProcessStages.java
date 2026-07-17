package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumOfflineProcessStages")
@WamEnum
public enum OfflineProcessStages {
    @WamEnumConstant(1) PAGE_LOAD,
    @WamEnumConstant(2) SOCKET_CONNECT,
    @WamEnumConstant(3) OFFLINE_PREVIEW,
    @WamEnumConstant(4) PROCESSING,
    @WamEnumConstant(5) PROCESS_COMPLETE,
    @WamEnumConstant(6) PROCESS_INTERRUPTED
}
