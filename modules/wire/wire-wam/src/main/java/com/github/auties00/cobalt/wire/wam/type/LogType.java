package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumLogType")
@WamEnum
public enum LogType {
    @WamEnumConstant(0) MINOR_EVENT,
    @WamEnumConstant(1) COUNTING_STAT,
    @WamEnumConstant(3) UNCAUGHT_EXCEPTION,
    @WamEnumConstant(4) UNHANDLED_REJECTED_PROMISE,
    @WamEnumConstant(5) INVESTIGATION,
    @WamEnumConstant(6) UNCATEGORIZED
}
