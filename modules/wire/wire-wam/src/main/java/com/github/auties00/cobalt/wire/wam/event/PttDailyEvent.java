package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebPttDailyWamEvent")
@WamEvent(id = 2938)
public interface PttDailyEvent extends WamEventSpec {
    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong pttCancelBroadcast();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong pttCancelGroup();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong pttCancelIndividual();

    @WamProperty(index = 42, type = WamType.INTEGER)
    OptionalLong pttCancelInterop();

    @WamProperty(index = 32, type = WamType.INTEGER)
    OptionalLong pttCancelNewsletter();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong pttDraftReviewBroadcast();

    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong pttDraftReviewGroup();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong pttDraftReviewIndividual();

    @WamProperty(index = 43, type = WamType.INTEGER)
    OptionalLong pttDraftReviewInterop();

    @WamProperty(index = 33, type = WamType.INTEGER)
    OptionalLong pttDraftReviewNewsletter();

    @WamProperty(index = 21, type = WamType.INTEGER)
    OptionalLong pttFastplaybackBroadcast();

    @WamProperty(index = 20, type = WamType.INTEGER)
    OptionalLong pttFastplaybackGroup();

    @WamProperty(index = 19, type = WamType.INTEGER)
    OptionalLong pttFastplaybackIndividual();

    @WamProperty(index = 44, type = WamType.INTEGER)
    OptionalLong pttFastplaybackInterop();

    @WamProperty(index = 34, type = WamType.INTEGER)
    OptionalLong pttFastplaybackNewsletter();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong pttLockBroadcast();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong pttLockGroup();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong pttLockIndividual();

    @WamProperty(index = 45, type = WamType.INTEGER)
    OptionalLong pttLockInterop();

    @WamProperty(index = 35, type = WamType.INTEGER)
    OptionalLong pttLockNewsletter();

    @WamProperty(index = 29, type = WamType.INTEGER)
    OptionalLong pttOutOfChatBroadcast();

    @WamProperty(index = 30, type = WamType.INTEGER)
    OptionalLong pttOutOfChatGroup();

    @WamProperty(index = 31, type = WamType.INTEGER)
    OptionalLong pttOutOfChatIndividual();

    @WamProperty(index = 46, type = WamType.INTEGER)
    OptionalLong pttOutOfChatInterop();

    @WamProperty(index = 36, type = WamType.INTEGER)
    OptionalLong pttOutOfChatNewsletter();

    @WamProperty(index = 22, type = WamType.INTEGER)
    OptionalLong pttPausedRecordBroadcast();

    @WamProperty(index = 23, type = WamType.INTEGER)
    OptionalLong pttPausedRecordGroup();

    @WamProperty(index = 24, type = WamType.INTEGER)
    OptionalLong pttPausedRecordIndividual();

    @WamProperty(index = 47, type = WamType.INTEGER)
    OptionalLong pttPausedRecordInterop();

    @WamProperty(index = 37, type = WamType.INTEGER)
    OptionalLong pttPausedRecordNewsletter();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong pttPlaybackBroadcast();

    @WamProperty(index = 17, type = WamType.INTEGER)
    OptionalLong pttPlaybackGroup();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong pttPlaybackIndividual();

    @WamProperty(index = 48, type = WamType.INTEGER)
    OptionalLong pttPlaybackInterop();

    @WamProperty(index = 38, type = WamType.INTEGER)
    OptionalLong pttPlaybackNewsletter();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong pttRecordBroadcast();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong pttRecordGroup();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong pttRecordIndividual();

    @WamProperty(index = 49, type = WamType.INTEGER)
    OptionalLong pttRecordInterop();

    @WamProperty(index = 39, type = WamType.INTEGER)
    OptionalLong pttRecordNewsletter();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong pttSendBroadcast();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong pttSendGroup();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong pttSendIndividual();

    @WamProperty(index = 50, type = WamType.INTEGER)
    OptionalLong pttSendInterop();

    @WamProperty(index = 40, type = WamType.INTEGER)
    OptionalLong pttSendNewsletter();

    @WamProperty(index = 25, type = WamType.INTEGER)
    OptionalLong pttStopTapBroadcast();

    @WamProperty(index = 26, type = WamType.INTEGER)
    OptionalLong pttStopTapGroup();

    @WamProperty(index = 27, type = WamType.INTEGER)
    OptionalLong pttStopTapIndividual();

    @WamProperty(index = 51, type = WamType.INTEGER)
    OptionalLong pttStopTapInterop();

    @WamProperty(index = 41, type = WamType.INTEGER)
    OptionalLong pttStopTapNewsletter();
}
