package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStatusInteractionType")
@WamEnum
public enum StatusInteractionType {
    @WamEnumConstant(1) QUESTION_ANSWER_SENT,
    @WamEnumConstant(2) QUESTION_ANSWER_RECEIVED,
    @WamEnumConstant(3) REPLY,
    @WamEnumConstant(4) MENTION_RESHARE_SENT,
    @WamEnumConstant(5) MENTION_RESHARE_RECEIVED,
    @WamEnumConstant(6) STATUS_RESHARE_SENT,
    @WamEnumConstant(7) STATUS_RESHARE_RECEIVED,
    @WamEnumConstant(8) REACTION_STICKER_SENT,
    @WamEnumConstant(9) REACTION_STICKER_RECEIVED,
    @WamEnumConstant(10) REACTION,
    @WamEnumConstant(11) ADD_YOURS_RESPONSE
}
