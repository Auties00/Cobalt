package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMdHistorySyncStatusResult")
@WamEnum
public enum MdHistorySyncStatusResult {
    @WamEnumConstant(1) SUCCESS,
    @WamEnumConstant(2) UNFINISHED,
    @WamEnumConstant(3) FAIL_TO_DOWNLOAD,
    @WamEnumConstant(4) MISSING_CHUNK,
    @WamEnumConstant(5) PROTOBUF_ERROR,
    @WamEnumConstant(6) FAIL_TO_STORE,
    @WamEnumConstant(7) OTHER_ERROR,
    @WamEnumConstant(8) IN_PROGRESS,
    @WamEnumConstant(9) FAIL_TO_RECEIVE,
    @WamEnumConstant(10) FAIL_TO_STORE_CHUNK,
    @WamEnumConstant(11) FAIL_TO_FETCH,
    @WamEnumConstant(12) FAIL_TO_PREPROCESS,
    @WamEnumConstant(13) FAIL_TO_ENCRYPT
}
