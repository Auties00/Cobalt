package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBotBizActionType")
@WamEnum
public enum BotBizActionType {
    @WamEnumConstant(1) BOT_BIZ_CARD_CLICK,
    @WamEnumConstant(2) BOT_BIZ_CARD_MESSAGE_CLICK,
    @WamEnumConstant(3) BOT_BIZ_CARD_VIEW_AI_CLICK,
    @WamEnumConstant(4) BOT_BIZ_DEEPLINK_CLICK,
    @WamEnumConstant(5) BOT_BIZ_NUX_APPEAR,
    @WamEnumConstant(6) BOT_BIZ_NUX_DISMISS,
    @WamEnumConstant(7) BOT_BIZ_NUX_SELECT,
    @WamEnumConstant(8) BOT_BIZ_INFO_CHAT_CLICK,
    @WamEnumConstant(9) BOT_BIZ_NUX_CONTINUE_CLICKED,
    @WamEnumConstant(10) BOT_BIZ_NUX_DISMISS_AUTO_ACCEPT,
    @WamEnumConstant(11) BOT_BIZ_NUX_APPEAR_MERGED,
    @WamEnumConstant(12) BOT_BIZ_NUX_APPEAR_NEW,
    @WamEnumConstant(13) BOT_BIZ_MESSAGE_SEND_CLICK,
    @WamEnumConstant(14) BOT_BIZ_MESSAGE_RENDERED
}
