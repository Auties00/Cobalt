package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPeerDataRequestType")
@WamEnum
public enum PeerDataRequestType {
    @WamEnumConstant(0) UPLOAD_STICKER,
    @WamEnumConstant(1) SEND_RECENT_STICKER_BOOTSTRAP,
    @WamEnumConstant(2) GENERAL_LINK_PREVIEW,
    @WamEnumConstant(3) HISTORY_SYNC_ON_DEMAND,
    @WamEnumConstant(4) PLACEHOLDER_MESSAGE_RESEND,
    @WamEnumConstant(5) WAFFLE_LINKING_NONCE_FETCH,
    @WamEnumConstant(6) SYNCD_SNAPSHOT_RECOVERY,
    @WamEnumConstant(7) HISTORY_SYNC_CHUNK_RETRY,
    @WamEnumConstant(8) GALAXY_FLOW_ACTION
}
