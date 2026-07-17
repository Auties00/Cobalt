package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AddContactActionType;
import com.github.auties00.cobalt.wire.wam.type.CompanionAddContactEventType;
import com.github.auties00.cobalt.wire.wam.type.CompanionAddContactSource;
import com.github.auties00.cobalt.wire.wam.type.CompanionContactSaveResult;
import com.github.auties00.cobalt.wire.wam.type.CompanionWhatsappContactStatus;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebCompanionsContactEventWamEvent")
@WamEvent(id = 5718)
public interface CompanionsContactEventEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<AddContactActionType> companionAddContactActionType();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<CompanionAddContactEventType> companionAddContactEventType();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> companionAddContactSessionId();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<CompanionAddContactSource> companionAddContactSource();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<CompanionContactSaveResult> companionContactSaveResult();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> companionFnameEdited();

    @WamProperty(index = 11, type = WamType.BOOLEAN)
    Optional<Boolean> companionHasPhoneNumber();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> companionHasUsername();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> companionIsContactSyncToOs();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> companionLnameEdited();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> companionPhNumberAutofilled();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> companionPhNumberEdited();

    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> companionSyncSettingChanged();

    @WamProperty(index = 14, type = WamType.BOOLEAN)
    Optional<Boolean> companionUsernameAutofilled();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> companionUsernameEdited();

    @WamProperty(index = 16, type = WamType.ENUM)
    Optional<CompanionWhatsappContactStatus> companionWhatsappContactStatus();
}
