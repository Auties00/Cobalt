package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPttStreamType")
@WamEnum
public enum PttStreamType {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) AAC,
    @WamEnumConstant(2) MP3,
    @WamEnumConstant(3) AMR_NB,
    @WamEnumConstant(4) AMR_WB,
    @WamEnumConstant(5) OPUS,
    @WamEnumConstant(6) MULTIPLE_TRACKS
}
