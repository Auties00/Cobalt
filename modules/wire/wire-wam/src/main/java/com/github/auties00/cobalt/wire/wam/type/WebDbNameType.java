package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebDbNameType")
@WamEnum
public enum WebDbNameType {
    @WamEnumConstant(1) MODEL_STORAGE,
    @WamEnumConstant(2) FTS_STORAGE,
    @WamEnumConstant(3) JOBS_STORAGE,
    @WamEnumConstant(4) LOADGEN_STORAGE,
    @WamEnumConstant(5) LRU_MEDIA_STORAGE_IDB,
    @WamEnumConstant(6) OFFD_STORAGE,
    @WamEnumConstant(7) QPL_STORAGE,
    @WamEnumConstant(8) SIGNAL_STORAGE,
    @WamEnumConstant(9) WORKER_STORAGE,
    @WamEnumConstant(10) SW,
    @WamEnumConstant(11) WAWC,
    @WamEnumConstant(12) WAWC_DB_ENC,
    @WamEnumConstant(13) STATUS_STORAGE
}
