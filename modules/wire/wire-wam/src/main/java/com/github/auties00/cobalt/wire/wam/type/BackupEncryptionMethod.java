package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBackupEncryptionMethod")
@WamEnum
public enum BackupEncryptionMethod {
    @WamEnumConstant(1) NOT_E2EE,
    @WamEnumConstant(2) E2EE_PASSWORD,
    @WamEnumConstant(3) E2EE_64DIGIT,
    @WamEnumConstant(4) E2EE_PASSKEY
}
