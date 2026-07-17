package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.WpbujAction;
import com.github.auties00.cobalt.wire.wam.type.WpbujBenefitStatus;
import com.github.auties00.cobalt.wire.wam.type.WpbujBenefitType;
import com.github.auties00.cobalt.wire.wam.type.WpbujOutcomeName;
import com.github.auties00.cobalt.wire.wam.type.WpbujSource;
import com.github.auties00.cobalt.wire.wam.type.WpbujSurface;
import com.github.auties00.cobalt.wire.wam.type.WsuaProductType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebWaPlusBenefitUserJourneyWamEvent")
@WamEvent(id = 7896)
public interface WaPlusBenefitUserJourneyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<WpbujAction> wpbujAction();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> wpbujActionTarget();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<WpbujBenefitStatus> wpbujBenefitStatus();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<WpbujBenefitType> wpbujBenefitType();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> wpbujCustomFields();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> wpbujErrorMessage();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<WpbujOutcomeName> wpbujOutcomeName();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> wpbujSessionId();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<WpbujSource> wpbujSource();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<WpbujSurface> wpbujSurface();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<WsuaProductType> wsuaProductType();
}
