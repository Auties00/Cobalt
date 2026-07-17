package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBackupSchedule")
@WamEnum
public enum BackupSchedule {
    @WamEnumConstant(0) OFF,
    @WamEnumConstant(1) DAILY,
    @WamEnumConstant(2) WEEKLY,
    @WamEnumConstant(3) MONTHLY,
    @WamEnumConstant(4) MANUAL
}
