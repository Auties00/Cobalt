package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.CoexStatusReplyPrivacyDisclaimerUserAction;
import com.github.auties00.cobalt.wire.wam.type.CoexSysMsgInsertionChannel;
import com.github.auties00.cobalt.wire.wam.type.CoexSysMsgStateTransitionAttempt;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebCoexPrivacySysMsgWamEvent")
@WamEvent(id = 5204, channel = WamChannel.PRIVATE, privateStatsId = 113760892)
public interface CoexPrivacySysMsgEvent extends WamEventSpec {
    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<CoexStatusReplyPrivacyDisclaimerUserAction> coexStatusReplyPrivacyDisclaimerUserAction();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> coexSysMsgBusinessId();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<CoexSysMsgInsertionChannel> coexSysMsgInsertionChannel();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong coexSysMsgInsertionErrorCode();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> coexSysMsgInsertionErrorMsg();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> coexSysMsgInsertionSuccess();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> coexSysMsgIsSelf();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong coexSysMsgMultiDeviceId();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<CoexSysMsgStateTransitionAttempt> coexSysMsgStateTransitionAttempt();
}
