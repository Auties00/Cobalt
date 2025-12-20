package com.github.auties00.cobalt.model.sync;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

@ProtobufMessage(name = "LimitSharing")
public final class LimitSharing {
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean sharingLimited;

    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    final TriggerType trigger;

    @ProtobufProperty(index = 3, type = ProtobufType.INT64)
    final long limitSharingSettingTimestamp;

    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    final boolean initiatedByMe;

    public LimitSharing(boolean sharingLimited, TriggerType trigger, long limitSharingSettingTimestamp, boolean initiatedByMe) {
        this.sharingLimited = sharingLimited;
        this.trigger = trigger;
        this.limitSharingSettingTimestamp = limitSharingSettingTimestamp;
        this.initiatedByMe = initiatedByMe;
    }

    public boolean sharingLimited() {
        return sharingLimited;
    }

    public Optional<TriggerType> trigger() {
        return Optional.ofNullable(trigger);
    }

    public long limitSharingSettingTimestamp() {
        return limitSharingSettingTimestamp;
    }

    public boolean initiatedByMe() {
        return initiatedByMe;
    }

    @ProtobufEnum(name = "LimitSharing.TriggerType")
    public enum TriggerType {
        UNKNOWN(0),
        CHAT_SETTING(1),
        BIZ_SUPPORTS_FB_HOSTING(2),
        UNKNOWN_GROUP(3);

        final int index;

        TriggerType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }
}
