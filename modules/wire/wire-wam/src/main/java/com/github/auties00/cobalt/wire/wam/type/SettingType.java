package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSettingType")
@WamEnum
public enum SettingType {
    @WamEnumConstant(0) HYBRID_START_UP,
    @WamEnumConstant(1) HYBRID_SYSTEM_TRAY,
    @WamEnumConstant(2) LANGUAGE,
    @WamEnumConstant(3) REPLACE_TEXT_WITH_EMOJI,
    @WamEnumConstant(4) BANNER_NOTIFICATION_DISPLAY_MODE,
    @WamEnumConstant(5) UNREAD_COUNTER_BADGE_DISPLAY_MODE,
    @WamEnumConstant(6) IS_MESSAGES_NOTIFICATION_ENABLED,
    @WamEnumConstant(7) IS_CALLS_NOTIFICATION_ENABLED,
    @WamEnumConstant(8) IS_REACTIONS_NOTIFICATION_ENABLED,
    @WamEnumConstant(9) IS_STATUS_REACTIONS_NOTIFICATION_ENABLED,
    @WamEnumConstant(10) IS_TEXT_PREVIEW_FOR_NOTIFICATION_ENABLED,
    @WamEnumConstant(11) DEFAULT_NOTIFICATION_TONE_ID,
    @WamEnumConstant(12) GROUP_DEFAULT_NOTIFICATION_TONE_ID,
    @WamEnumConstant(13) APP_THEME,
    @WamEnumConstant(14) WALLPAPER_ID,
    @WamEnumConstant(15) IS_DOODLE_WALLPAPER_ENABLED,
    @WamEnumConstant(16) FONT_SIZE,
    @WamEnumConstant(17) IS_PHOTOS_AUTODOWNLOAD_ENABLED,
    @WamEnumConstant(18) IS_AUDIOS_AUTODOWNLOAD_ENABLED,
    @WamEnumConstant(19) IS_VIDEOS_AUTODOWNLOAD_ENABLED,
    @WamEnumConstant(20) IS_DOCUMENTS_AUTODOWNLOAD_ENABLED,
    @WamEnumConstant(21) DISABLE_LINK_PREVIEWS,
    @WamEnumConstant(22) NOTIFICATION_TONE_ID,
    @WamEnumConstant(23) MEDIA_UPLOAD_QUALITY,
    @WamEnumConstant(24) IS_SPELL_CHECK_ENABLED,
    @WamEnumConstant(25) IS_ENTER_TO_SEND_ENABLED,
    @WamEnumConstant(26) IS_GROUP_MESSAGE_NOTIFICATION_ENABLED,
    @WamEnumConstant(27) IS_GROUP_REACTIONS_NOTIFICATION_ENABLED,
    @WamEnumConstant(28) IS_STATUS_NOTIFICATION_ENABLED,
    @WamEnumConstant(29) STATUS_NOTIFICATION_TONE_ID,
    @WamEnumConstant(30) SHOULD_PLAY_SOUND_FOR_CALL_NOTIFICATION,
    @WamEnumConstant(31) CHAT_THEME_ID,
    @WamEnumConstant(32) COLOR_SCHEME_ID
}
