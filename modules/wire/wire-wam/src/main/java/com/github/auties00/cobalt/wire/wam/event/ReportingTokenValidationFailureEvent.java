package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.EditType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageType;
import com.github.auties00.cobalt.wire.wam.type.PlatformType;
import com.github.auties00.cobalt.wire.wam.type.ReportingTokenValidationFailureReason;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebReportingTokenValidationFailureWamEvent")
@WamEvent(id = 5466)
public interface ReportingTokenValidationFailureEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<EditType> editType();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> isLid();

    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> isMessageMediaRetry();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> isMessageRetry();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> isPartOfGroupHistory();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> isSecretEncryptedMsg();

    @WamProperty(index = 11, type = WamType.BOOLEAN)
    Optional<Boolean> messageIsForward();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<MediaType> messageMediaType();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<MessageType> messageType();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> offline();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<ReportingTokenValidationFailureReason> reportingTokenValidationFailureReason();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong reportingTokenVersion();

    @WamProperty(index = 14, type = WamType.ENUM)
    Optional<PlatformType> senderPlatform();
}
