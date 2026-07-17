package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMessageContextMenuOptionType")
@WamEnum
public enum MessageContextMenuOptionType {
    @WamEnumConstant(1) UNKNOWN,
    @WamEnumConstant(2) OVERFLOW,
    @WamEnumConstant(3) FORWARD,
    @WamEnumConstant(4) DELETE,
    @WamEnumConstant(5) REPLY,
    @WamEnumConstant(6) REPLY_PRIVATELY,
    @WamEnumConstant(7) STAR_OR_UNSTAR,
    @WamEnumConstant(8) COPY,
    @WamEnumConstant(9) REPORT,
    @WamEnumConstant(10) MESSAGE_CONTACT,
    @WamEnumConstant(11) MESSAGE_INFO,
    @WamEnumConstant(12) EDIT,
    @WamEnumConstant(13) FORWARD_SELECT_MESSAGES,
    @WamEnumConstant(14) DELETE_SELECT_MESSAGES,
    @WamEnumConstant(15) SELECT,
    @WamEnumConstant(16) ADD_TO_CALENDAR,
    @WamEnumConstant(17) COPY_LINK_URL,
    @WamEnumConstant(18) REACT,
    @WamEnumConstant(19) DOWNLOAD,
    @WamEnumConstant(20) KEEP_OR_UNKEEP,
    @WamEnumConstant(21) FAVORITE_OR_UNFAVORITE_STICKER,
    @WamEnumConstant(22) COPY_STICKER,
    @WamEnumConstant(23) VIEW_STICKER_PACK,
    @WamEnumConstant(24) DOWNLOAD_ALL,
    @WamEnumConstant(25) SAVE_AS,
    @WamEnumConstant(26) SHARE,
    @WamEnumConstant(27) PIN_OR_UNPIN,
    @WamEnumConstant(28) ASK_META_AI,
    @WamEnumConstant(29) BOT_FEEDBACK,
    @WamEnumConstant(30) REPORT_TO_ADMIN,
    @WamEnumConstant(31) COPY_NEWSLETTER_LINK,
    @WamEnumConstant(32) PAID_PARTNERSHIP,
    @WamEnumConstant(33) VERIFY_SECURITY_CODE,
    @WamEnumConstant(34) ADD_TO_NOTE
}
