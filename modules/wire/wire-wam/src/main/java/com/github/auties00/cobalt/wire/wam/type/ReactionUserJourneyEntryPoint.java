package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumReactionUserJourneyEntryPoint")
@WamEnum
public enum ReactionUserJourneyEntryPoint {
    @WamEnumConstant(1) MESSAGE_HOLD,
    @WamEnumConstant(2) MESSAGE_DOUBLE_TAP,
    @WamEnumConstant(3) MEDIA_VIEWER_REACTION_CTA,
    @WamEnumConstant(4) EXISTING_REACTION_CTA,
    @WamEnumConstant(5) MACOS_MESSAGE_REACTION_BUTTON,
    @WamEnumConstant(6) MACOS_MESSAGE_MENU_ITEM_REACTION,
    @WamEnumConstant(7) MACOS_LAST_MESSAGE_REACT_SHORTCUT,
    @WamEnumConstant(8) VOICE_CHAT_REACTION_BUTTON,
    @WamEnumConstant(9) VOICE_CHAT_MINI_PLAYER_HOLD,
    @WamEnumConstant(10) MESSAGE_REACTION_BUTTON,
    @WamEnumConstant(11) MESSAGE_MENU_ITEM_REACTION,
    @WamEnumConstant(12) MULTI_MESSAGE_HOLD
}
