package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ContactSuggestion;
import com.github.auties00.cobalt.wire.wam.type.Setting;
import com.github.auties00.cobalt.wire.wam.type.StatusPrivacySettingsAction;
import com.github.auties00.cobalt.wire.wam.type.StatusPrivacySettingsView;
import com.github.auties00.cobalt.wire.wam.type.StatusPrivacySurface;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebStatusPrivacySettingsWamEvent")
@WamEvent(id = 3200)
public interface StatusPrivacySettingsEvent extends WamEventSpec {
    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> allowForwarding();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> allowResharing();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<ContactSuggestion> contactSuggestion();

    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong contactSuggestionsCount();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong lastSuccessfulRankingUpdate();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<Setting> previousSetting();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong selectedContactsSize();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong selectedGroupsSize();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong selectedListSize();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong selectionPillPos();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<Setting> setting();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong statusPostingSessionId();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> statusPrivacyLists();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<StatusPrivacySettingsAction> statusPrivacySettingsAction();

    @WamProperty(index = 11, type = WamType.ENUM)
    Optional<StatusPrivacySettingsView> statusPrivacySettingsView();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<StatusPrivacySurface> statusPrivacySurface();
}
