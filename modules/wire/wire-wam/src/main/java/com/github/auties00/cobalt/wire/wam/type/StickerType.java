package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStickerType")
@WamEnum
public enum StickerType {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) ARROW,
    @WamEnumConstant(2) OVAL,
    @WamEnumConstant(3) RECT,
    @WamEnumConstant(4) THINKING_BUBBLE,
    @WamEnumConstant(5) SPEECH_BUBBLE_OVAL,
    @WamEnumConstant(6) SPEECH_BUBBLE_RECT,
    @WamEnumConstant(7) DIGITAL_CLOCK,
    @WamEnumConstant(8) ANALOG_CLOCK,
    @WamEnumConstant(9) LOCATION,
    @WamEnumConstant(10) ADD_YOURS,
    @WamEnumConstant(11) NORMAL_STICKER,
    @WamEnumConstant(12) EMOJI_STICKER,
    @WamEnumConstant(13) PHOTO,
    @WamEnumConstant(14) MUSIC,
    @WamEnumConstant(15) STATUS_API,
    @WamEnumConstant(16) QUESTION,
    @WamEnumConstant(17) QUESTION_ANSWER,
    @WamEnumConstant(18) REACTION_STICKER,
    @WamEnumConstant(19) NYE_2026
}
