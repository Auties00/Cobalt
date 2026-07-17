package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.BusinessProfileEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.BusinessProfileField;
import com.github.auties00.cobalt.wire.wam.type.EditProfileAction;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebEditBusinessProfileWamEvent")
@WamEvent(id = 1466)
public interface EditBusinessProfileEvent extends WamEventSpec {
    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<BusinessProfileEntryPoint> businessProfileEntryPoint();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> editBusinessProfileSessionId();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<EditProfileAction> editProfileAction();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<BusinessProfileField> editProfileActionField();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> hasAddress();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> hasCategory();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> hasDescription();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> hasEmail();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> hasHours();

    @WamProperty(index = 11, type = WamType.BOOLEAN)
    Optional<Boolean> hasPaymentInfo();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> hasWebsite();
}
