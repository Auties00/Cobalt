package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumReactionUserJourneyAction")
@WamEnum
public enum ReactionUserJourneyAction {
    @WamEnumConstant(1) TRAY_OPEN,
    @WamEnumConstant(2) SEARCH_OPEN,
    @WamEnumConstant(3) REACTION_SELECT,
    @WamEnumConstant(4) REACTION_UNSELECT,
    @WamEnumConstant(5) REACTION_SEARCH,
    @WamEnumConstant(6) SEARCH_CLOSE,
    @WamEnumConstant(7) TRAY_CLOSE,
    @WamEnumConstant(8) REACTION_DETAILS,
    @WamEnumConstant(9) SELECT_PROFILE,
    @WamEnumConstant(10) DETAILS_DISMISS,
    @WamEnumConstant(11) SELECT_ALBUM_THUMBNAIL,
    @WamEnumConstant(12) SELECT_TAB_IN_REACTION_DETAILS
}
