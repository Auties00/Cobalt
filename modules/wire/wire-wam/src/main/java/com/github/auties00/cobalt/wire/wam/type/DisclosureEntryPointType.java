package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDisclosureEntryPointType")
@WamEnum
public enum DisclosureEntryPointType {
    @WamEnumConstant(0) PRE_THREAD,
    @WamEnumConstant(1) ON_THREAD_ENTRY,
    @WamEnumConstant(2) SEND_FROM_THREAD,
    @WamEnumConstant(3) SEND_FROM_THREAD_KEYBOARD,
    @WamEnumConstant(4) ICEBREAKERS,
    @WamEnumConstant(5) THREAD_ATTACHMENT_BAR,
    @WamEnumConstant(6) THREAD_EMOJI_BAR,
    @WamEnumConstant(7) AUDIO_VIDEO_CALL_FROM_THREAD,
    @WamEnumConstant(8) AUDIO_CALL_FROM_THREAD,
    @WamEnumConstant(9) VIDEO_CALL_FROM_THREAD,
    @WamEnumConstant(10) AUDIO_NOTE_FROM_THREAD,
    @WamEnumConstant(11) VIDEO_NOTE_FROM_THREAD,
    @WamEnumConstant(12) CAMERA_MEDIA_SELECTION,
    @WamEnumConstant(13) CALL_FROM_PROFILE,
    @WamEnumConstant(14) OVERFLOW_SEND_ORDER,
    @WamEnumConstant(15) MESSAGE_FROM_PROFILE,
    @WamEnumConstant(16) VIDEO_CALL_FROM_PROFILE,
    @WamEnumConstant(17) CALL_FROM_BUSINESS_DETAILS_CARD,
    @WamEnumConstant(18) TEXT_ENTRY_FROM_THREAD,
    @WamEnumConstant(19) VIEW_CATALOG_FROM_THREAD,
    @WamEnumConstant(20) VIEW_CATALOG_FROM_PROFILE,
    @WamEnumConstant(21) SEND_CALL_LINK_FROM_THREAD
}
