package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStatusViewActionType")
@WamEnum
public enum StatusViewActionType {
    @WamEnumConstant(0) ATTRIBUTION_TAPPED,
    @WamEnumConstant(1) ATTRIBUTION_LIST_OPENED,
    @WamEnumConstant(2) ATTRIBUTION_LIST_ITEM_TAPPED,
    @WamEnumConstant(3) QUESTION_STICKER_CLICK,
    @WamEnumConstant(4) QUESTION_ANSWER_SEND,
    @WamEnumConstant(5) REACTION_STICKER_CLICK,
    @WamEnumConstant(6) RESHARE_BUTTON_TAP,
    @WamEnumConstant(7) CLOSE_SHARING_EMOJI_CLICK,
    @WamEnumConstant(8) URL_CLICK
}
