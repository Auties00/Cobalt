package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.UsernameCreationActionName;
import com.github.auties00.cobalt.wire.wam.type.UsernameCreationCurrentScreen;
import com.github.auties00.cobalt.wire.wam.type.UsernameCreationEntrypoint;
import com.github.auties00.cobalt.wire.wam.type.UsernameCreationErrorMessage;
import com.github.auties00.cobalt.wire.wam.type.UsernameCreationFlowType;
import com.github.auties00.cobalt.wire.wam.type.UsernameLinkOriginSurface;
import com.github.auties00.cobalt.wire.wam.type.UsernameLinkType;
import com.github.auties00.cobalt.wire.wam.type.UsernameSource;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebUsernameCreationActionWamEvent")
@WamEvent(id = 5224)
public interface UsernameCreationActionEvent extends WamEventSpec {
    @WamProperty(index = 14, type = WamType.BOOLEAN)
    Optional<Boolean> eligibleToLink();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong linkedAccountsFb();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong linkedAccountsIg();

    @WamProperty(index = 17, type = WamType.INTEGER)
    OptionalLong userJourneyEventMs();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<UsernameCreationActionName> usernameCreationActionName();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<UsernameCreationCurrentScreen> usernameCreationCurrentScreen();

    @WamProperty(index = 15, type = WamType.STRING)
    Optional<String> usernameCreationDeepLinkCampaign();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> usernameCreationDeepLinkChannel();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<UsernameCreationEntrypoint> usernameCreationEntrypoint();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong usernameCreationErrorCd();

    @WamProperty(index = 13, type = WamType.ENUM)
    Optional<UsernameCreationErrorMessage> usernameCreationErrorMessage();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<UsernameCreationFlowType> usernameCreationFlowType();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> usernameCreationUsernameSessionId();

    @WamProperty(index = 18, type = WamType.ENUM)
    Optional<UsernameLinkOriginSurface> usernameLinkOriginSurface();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<UsernameLinkType> usernameLinkType();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<UsernameSource> usernameSource();
}
