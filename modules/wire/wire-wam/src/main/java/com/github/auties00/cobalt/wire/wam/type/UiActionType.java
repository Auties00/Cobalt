package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUiActionType")
@WamEnum
public enum UiActionType {
    @WamEnumConstant(1) OTHER,
    @WamEnumConstant(2) APP_OPEN,
    @WamEnumConstant(3) CHAT_OPEN,
    @WamEnumConstant(4) IMAGE_OPEN,
    @WamEnumConstant(5) FIRST_FTS_RESULT,
    @WamEnumConstant(6) CONTACTS_OPEN,
    @WamEnumConstant(7) GROUP_INFO_OPEN,
    @WamEnumConstant(8) MSG_INFO_OPEN,
    @WamEnumConstant(9) COMMUNITY_INFO_OPEN,
    @WamEnumConstant(10) DEFAULT_SUBGROUP_INFO_OPEN,
    @WamEnumConstant(11) COMMUNITY_CREATE,
    @WamEnumConstant(12) COMMUNITY_LINK,
    @WamEnumConstant(13) EMOJI_OBI_DECOMPRESSION,
    @WamEnumConstant(14) EMOJI_PNG_DECOMPRESSION,
    @WamEnumConstant(15) CHAT_LIST_OPEN,
    @WamEnumConstant(16) CALL_LIST_OPEN,
    @WamEnumConstant(17) CHANNEL_INFO_OPEN,
    @WamEnumConstant(18) TTRC,
    @WamEnumConstant(19) PTT_START_LATENCY,
    @WamEnumConstant(20) VIDEO_OPEN,
    @WamEnumConstant(21) GIF_OPEN,
    @WamEnumConstant(22) GALLERY_OPEN,
    @WamEnumConstant(23) REACTION_TRAY_START_LATENCY,
    @WamEnumConstant(24) PTT_STOP_LATENCY,
    @WamEnumConstant(25) PTT_LOCKED_VIEW_OPEN,
    @WamEnumConstant(26) PTT_PLAYBACK_START,
    @WamEnumConstant(27) PTV_PLAYBACK_START,
    @WamEnumConstant(28) PTV_RECORDING_START,
    @WamEnumConstant(29) PTV_RECORDING_STOP,
    @WamEnumConstant(30) PTV_RECORDING_DISCARD,
    @WamEnumConstant(31) PTV_RECORDING_SEND,
    @WamEnumConstant(32) EMOJI_PICKER_START,
    @WamEnumConstant(33) MESSAGE_FORWARD,
    @WamEnumConstant(34) DRAFT_PTT_PLAYBACK_START_LATENCY,
    @WamEnumConstant(35) CONTEXT_MENU_SHOWN_LATENCY,
    @WamEnumConstant(36) KEYBOARD_SHOWN_LATENCY,
    @WamEnumConstant(37) FIRST_CHAR_TYPING_PROCESSING_LATENCY,
    @WamEnumConstant(38) AVG_TYPING_PROCESSING_LATENCY,
    @WamEnumConstant(39) EVOLVE_ABOUT_CHAT_RENDER_LATENCY,
    @WamEnumConstant(40) VOIP_OPEN,
    @WamEnumConstant(41) VOIP_CLOSE,
    @WamEnumConstant(42) VOIP_JOIN_CALL,
    @WamEnumConstant(43) SHOW_QUOTED_ITEM_LATENCY,
    @WamEnumConstant(44) DELETE_QUOTED_ITEM_LATENCY,
    @WamEnumConstant(45) NAVIGATE_TO_QUOTED_ITEM_LATENCY
}
