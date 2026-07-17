package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumParticipantActionSource")
@WamEnum
public enum ParticipantActionSource {
    @WamEnumConstant(1) MINI_CONTACT_SHEET_AUDIO,
    @WamEnumConstant(2) MINI_CONTACT_SHEET_VIDEO,
    @WamEnumConstant(3) HEADER_AUDIO,
    @WamEnumConstant(4) HEADER_VIDEO
}
