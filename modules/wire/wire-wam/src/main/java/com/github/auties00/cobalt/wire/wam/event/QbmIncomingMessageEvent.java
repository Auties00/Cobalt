package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChatsFolderType;
import com.github.auties00.cobalt.wire.wam.type.ContactType;
import com.github.auties00.cobalt.wire.wam.type.QbmFlag;
import com.github.auties00.cobalt.wire.wam.type.SignupEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.ThreadCreationTime;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebQbmIncomingMessageWamEvent")
@WamEvent(id = 3790)
public interface QbmIncomingMessageEvent extends WamEventSpec {
    @WamProperty(index = 21, type = WamType.INTEGER)
    OptionalLong apiDailyThreadCount7d();

    @WamProperty(index = 22, type = WamType.INTEGER)
    OptionalLong apiMessageCount1d();

    @WamProperty(index = 23, type = WamType.INTEGER)
    OptionalLong apiMessageCount7d();

    @WamProperty(index = 26, type = WamType.INTEGER)
    OptionalLong apiTotalMessageCount();

    @WamProperty(index = 27, type = WamType.INTEGER)
    OptionalLong apiTotalNewThreadCount();

    @WamProperty(index = 24, type = WamType.INTEGER)
    OptionalLong apiUniqueThreadCount1d();

    @WamProperty(index = 25, type = WamType.INTEGER)
    OptionalLong apiUniqueThreadCount7d();

    @WamProperty(index = 36, type = WamType.STRING)
    Optional<String> bizTrustTier();

    @WamProperty(index = 52, type = WamType.INTEGER)
    OptionalLong bodyUrlCountInt();

    @WamProperty(index = 53, type = WamType.INTEGER)
    OptionalLong bodyUrlUniqueCountInt();

    @WamProperty(index = 45, type = WamType.STRING)
    Optional<String> buttonValueJsonArray();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ChatsFolderType> chatsFolderType();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong companionDevices();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<ContactType> contactType();

    @WamProperty(index = 54, type = WamType.INTEGER)
    OptionalLong ctaUrlUniqueCountInt();

    @WamProperty(index = 41, type = WamType.STRING)
    Optional<String> decisionId();

    @WamProperty(index = 40, type = WamType.STRING)
    Optional<String> entSourceSubplatform();

    @WamProperty(index = 43, type = WamType.BOOLEAN)
    Optional<Boolean> fmxCardShown();

    @WamProperty(index = 58, type = WamType.STRING)
    Optional<String> gapRules();

    @WamProperty(index = 59, type = WamType.INTEGER)
    OptionalLong geEvaluationTimestamp();

    @WamProperty(index = 14, type = WamType.STRING)
    Optional<String> hsmTagStr();

    @WamProperty(index = 60, type = WamType.ENUM)
    Optional<SignupEntryPoint> iasEntryPoint();

    @WamProperty(index = 61, type = WamType.STRING)
    Optional<String> iasOptinDs();

    @WamProperty(index = 37, type = WamType.BOOLEAN)
    Optional<Boolean> isBizIntent();

    @WamProperty(index = 38, type = WamType.BOOLEAN)
    Optional<Boolean> isBroadcastMessage();

    @WamProperty(index = 56, type = WamType.BOOLEAN)
    Optional<Boolean> isCoex();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> isFromAdsManagerMm();

    @WamProperty(index = 16, type = WamType.BOOLEAN)
    Optional<Boolean> isFromCapi();

    @WamProperty(index = 62, type = WamType.BOOLEAN)
    Optional<Boolean> isIasSubscriber();

    @WamProperty(index = 39, type = WamType.BOOLEAN)
    Optional<Boolean> isInsubContact();

    @WamProperty(index = 20, type = WamType.BOOLEAN)
    Optional<Boolean> isMuted();

    @WamProperty(index = 44, type = WamType.BOOLEAN)
    Optional<Boolean> isOba();

    @WamProperty(index = 57, type = WamType.BOOLEAN)
    Optional<Boolean> isThroughDecisionService();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> keepChatsArchivedEnabled();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong lastOutgoingMessageDeltaTime();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong lastOutgoingMessageDeltaTimeReceived();

    @WamProperty(index = 46, type = WamType.STRING)
    Optional<String> messageFieldJsonArray();

    @WamProperty(index = 18, type = WamType.BOOLEAN)
    Optional<Boolean> messageHasButton();

    @WamProperty(index = 19, type = WamType.BOOLEAN)
    Optional<Boolean> messageHasUrl();

    @WamProperty(index = 17, type = WamType.STRING)
    Optional<String> messageIdHmac();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> messageTypeStr();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> muted();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> notificationEnabled();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<QbmFlag> qbmFlag();

    @WamProperty(index = 12, type = WamType.STRING)
    Optional<String> qbmFlagStr();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> readReceiptsEnabled();

    @WamProperty(index = 28, type = WamType.INTEGER)
    OptionalLong smbDailyThreadCount7d();

    @WamProperty(index = 29, type = WamType.INTEGER)
    OptionalLong smbMessageCount1d();

    @WamProperty(index = 30, type = WamType.INTEGER)
    OptionalLong smbMessageCount7d();

    @WamProperty(index = 31, type = WamType.INTEGER)
    OptionalLong smbTotalMessageCount();

    @WamProperty(index = 32, type = WamType.INTEGER)
    OptionalLong smbTotalNewThreadCount();

    @WamProperty(index = 33, type = WamType.INTEGER)
    OptionalLong smbUniqueThreadCount1d();

    @WamProperty(index = 34, type = WamType.INTEGER)
    OptionalLong smbUniqueThreadCount7d();

    @WamProperty(index = 47, type = WamType.STRING)
    Optional<String> submessageFieldJsonArray();

    @WamProperty(index = 35, type = WamType.ENUM)
    Optional<ThreadCreationTime> threadCreationTime();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> threadIdHmac();

    @WamProperty(index = 42, type = WamType.STRING)
    Optional<String> threadLidHmac();

    @WamProperty(index = 55, type = WamType.INTEGER)
    OptionalLong urlUniqueCountInt();
}
