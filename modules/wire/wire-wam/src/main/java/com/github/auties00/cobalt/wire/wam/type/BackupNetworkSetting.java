package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBackupNetworkSetting")
@WamEnum
public enum BackupNetworkSetting {
    @WamEnumConstant(0) WIFI_ONLY,
    @WamEnumConstant(1) WIFI_OR_CELLULAR
}
