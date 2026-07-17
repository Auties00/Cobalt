package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AddMembersEntrypointType;
import com.github.auties00.cobalt.wire.wam.type.BundleSendSource;
import com.github.auties00.cobalt.wire.wam.type.GroupCreateEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.GroupMemberAddingActionType;
import com.github.auties00.cobalt.wire.wam.type.GroupMemberAddingMemberType;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebGroupMemberAddingUserJourneyWamEvent")
@WamEvent(id = 5336)
public interface GroupMemberAddingUserJourneyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong addSelectedContactsCount();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 20, type = WamType.ENUM)
    Optional<BundleSendSource> bundleSendSource();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong frequentlyContactedIndex();

    @WamProperty(index = 28, type = WamType.ENUM)
    Optional<AddMembersEntrypointType> groupAddMemberEntryPoint();

    @WamProperty(index = 16, type = WamType.ENUM)
    Optional<GroupCreateEntryPoint> groupCreateEntryPoint();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> groupCreationGroupId();

    @WamProperty(index = 21, type = WamType.INTEGER)
    OptionalLong groupHistoryMessagesCount();

    @WamProperty(index = 25, type = WamType.INTEGER)
    OptionalLong groupHistoryOutWindowPinsCount();

    @WamProperty(index = 26, type = WamType.INTEGER)
    OptionalLong groupHistoryPinsCount();

    @WamProperty(index = 27, type = WamType.INTEGER)
    OptionalLong groupHistoryUncountedMessagesCount();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<GroupMemberAddingActionType> groupMemberAddingActionType();

    @WamProperty(index = 24, type = WamType.ENUM)
    Optional<GroupMemberAddingMemberType> groupMemberAddingMemberType();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong groupServerErrorCode();

    @WamProperty(index = 19, type = WamType.STRING)
    Optional<String> groupServerErrorCodeMsg();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> hasGroupName();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> hasProfilePicture();

    @WamProperty(index = 29, type = WamType.BOOLEAN)
    Optional<Boolean> isAdmin();

    @WamProperty(index = 22, type = WamType.BOOLEAN)
    Optional<Boolean> isGroupHistoryToggledOn();

    @WamProperty(index = 30, type = WamType.BOOLEAN)
    Optional<Boolean> isTeeBotNoticeOnly();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong potentialTotalSuggestionCount();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong recentlyContactedIndex();

    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong selectedMemberCnt();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong suggestedContactsCount();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong suggestedContactsIndex();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<TsSurface> uiSurface();

    @WamProperty(index = 23, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong userJourneyEventMs();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> userJourneyFunnelId();
}
