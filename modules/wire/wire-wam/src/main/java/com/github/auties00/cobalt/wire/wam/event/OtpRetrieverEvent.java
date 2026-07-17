package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChatsFolderType;
import com.github.auties00.cobalt.wire.wam.type.CtaFallbackReason;
import com.github.auties00.cobalt.wire.wam.type.CtaType;
import com.github.auties00.cobalt.wire.wam.type.OtpEventSource;
import com.github.auties00.cobalt.wire.wam.type.OtpEventType;
import com.github.auties00.cobalt.wire.wam.type.OtpProductType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebOtpRetrieverWamEvent")
@WamEvent(id = 3468, channel = WamChannel.PRIVATE, privateStatsId = 113760892)
public interface OtpRetrieverEvent extends WamEventSpec {
    @WamProperty(index = 26, type = WamType.INTEGER)
    OptionalLong businessLid();

    @WamProperty(index = 28, type = WamType.STRING)
    Optional<String> businessLidOrJid();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong businessPhoneNumber();

    @WamProperty(index = 19, type = WamType.STRING)
    Optional<String> chatId();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<ChatsFolderType> chatsFolderType();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<CtaFallbackReason> ctaFallbackReason();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<CtaType> ctaType();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> isKeepChatsArchivedEnabled();

    @WamProperty(index = 14, type = WamType.BOOLEAN)
    Optional<Boolean> isMessageNotificationEnabled();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> isNotificationEnabled();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong messageReceivedElapsedTimeSeconds();

    @WamProperty(index = 20, type = WamType.STRING)
    Optional<String> otpCorrelationId();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<OtpEventSource> otpEventSource();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<OtpEventType> otpEventType();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> otpFailureReason();

    @WamProperty(index = 21, type = WamType.INTEGER)
    OptionalLong otpHandshakeElapsedTimeMs();

    @WamProperty(index = 23, type = WamType.STRING)
    Optional<String> otpHandshakeId();

    @WamProperty(index = 29, type = WamType.BOOLEAN)
    Optional<Boolean> otpIosAutofillDisabled();

    @WamProperty(index = 24, type = WamType.BOOLEAN)
    Optional<Boolean> otpMaskLinkedDevices();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<OtpProductType> otpProductType();

    @WamProperty(index = 22, type = WamType.STRING)
    Optional<String> otpSdkVersion();

    @WamProperty(index = 27, type = WamType.STRING)
    Optional<String> otpSenderAttributes();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> otpSessionId();

    @WamProperty(index = 18, type = WamType.STRING)
    Optional<String> receiverCountryCode();

    @WamProperty(index = 17, type = WamType.STRING)
    Optional<String> templateId();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> thirdPartyPackageNameFromIntent();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> thirdPartyPackageSignatureHash();

    @WamProperty(index = 25, type = WamType.INTEGER)
    OptionalLong waDeviceId();
}
