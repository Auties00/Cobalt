package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ClientGroupSizeBucket;
import com.github.auties00.cobalt.wire.wam.type.GroupTypeClient;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebThreadInteractionDataNotificationWamEvent")
@WamEvent(id = 6412)
public interface ThreadInteractionDataNotificationEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ClientGroupSizeBucket> groupSizeBucket();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<GroupTypeClient> groupTypeClient();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> isAGroup();

    @WamProperty(index = 26, type = WamType.STRING)
    Optional<String> threadCreationDate();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> threadDs();

    @WamProperty(index = 24, type = WamType.STRING)
    Optional<String> threadId();

    @WamProperty(index = 25, type = WamType.STRING)
    Optional<String> threadIdByLid();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong totalLinkReshareMessageNotifShown();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong totalLinkReshareMessageNotifShownFb();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong totalLinkReshareMessageNotifShownIg();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong totalLinkReshareMessageNotifTapToOpen();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong totalLinkReshareMessageNotifTapToOpenFb();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong totalLinkReshareMessageNotifTapToOpenIg();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong totalMessageReminderNotifShown();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong totalMessageReminderNotifTapToOpen();

    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong totalNotifMarkAsRead();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong totalNotifMissedCallVoipCallback();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong totalNotifMissedCallVoipMessage();

    @WamProperty(index = 17, type = WamType.INTEGER)
    OptionalLong totalNotifOthers();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong totalNotifReply();

    @WamProperty(index = 19, type = WamType.INTEGER)
    OptionalLong totalNotifRtcVoipAccept();

    @WamProperty(index = 20, type = WamType.INTEGER)
    OptionalLong totalNotifRtcVoipDecline();

    @WamProperty(index = 21, type = WamType.INTEGER)
    OptionalLong totalNotifShowPreview();

    @WamProperty(index = 22, type = WamType.INTEGER)
    OptionalLong totalNotifShown();

    @WamProperty(index = 23, type = WamType.INTEGER)
    OptionalLong totalNotifTapToOpen();
}
