package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AudioOutputRoute;
import com.github.auties00.cobalt.wire.wam.type.AudioStreamType;
import com.github.auties00.cobalt.wire.wam.type.PttPlaybackSpeedType;
import com.github.auties00.cobalt.wire.wam.type.PttPlayerType;
import com.github.auties00.cobalt.wire.wam.type.PttStreamType;
import com.github.auties00.cobalt.wire.wam.type.PttTriggerType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebPttPlaybackWamEvent")
@WamEvent(id = 2044)
public interface PttPlaybackEvent extends WamEventSpec {
    @WamProperty(index = 19, type = WamType.ENUM)
    Optional<AudioStreamType> audioStreamType();

    @WamProperty(index = 23, type = WamType.TIMER)
    Optional<Instant> pttAudioRouteBluetoothTime();

    @WamProperty(index = 20, type = WamType.INTEGER)
    OptionalLong pttAudioRouteChangeCount();

    @WamProperty(index = 24, type = WamType.TIMER)
    Optional<Instant> pttAudioRouteEarpieceTime();

    @WamProperty(index = 25, type = WamType.TIMER)
    Optional<Instant> pttAudioRouteHeadsetTime();

    @WamProperty(index = 21, type = WamType.ENUM)
    Optional<AudioOutputRoute> pttAudioRouteInitial();

    @WamProperty(index = 22, type = WamType.ENUM)
    Optional<AudioOutputRoute> pttAudioRouteLast();

    @WamProperty(index = 26, type = WamType.TIMER)
    Optional<Instant> pttAudioRouteSpeakerTime();

    @WamProperty(index = 12, type = WamType.TIMER)
    Optional<Instant> pttDuration();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> pttMainThreadBlock();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong pttMiniPlayerClick();

    @WamProperty(index = 16, type = WamType.BOOLEAN)
    Optional<Boolean> pttMiniPlayerClose();

    @WamProperty(index = 17, type = WamType.INTEGER)
    OptionalLong pttMiniPlayerPauseCnt();

    @WamProperty(index = 1, type = WamType.TIMER)
    Optional<Instant> pttPlayRequestT();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> pttPlaybackFailed();

    @WamProperty(index = 4, type = WamType.TIMER)
    Optional<Instant> pttPlaybackOverallT();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<PttPlaybackSpeedType> pttPlaybackSpeed();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong pttPlaybackSpeedCnt();

    @WamProperty(index = 18, type = WamType.BOOLEAN)
    Optional<Boolean> pttPlayedOutOfChat();

    @WamProperty(index = 14, type = WamType.FLOAT)
    OptionalDouble pttPlayedPct();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<PttPlayerType> pttPlayer();

    @WamProperty(index = 2, type = WamType.TIMER)
    Optional<Instant> pttPlayerInitT();

    @WamProperty(index = 3, type = WamType.TIMER)
    Optional<Instant> pttPlayerPlayT();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong pttSeekCnt();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<PttTriggerType> pttTrigger();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<PttStreamType> pttType();

    @WamProperty(index = 27, type = WamType.INTEGER)
    OptionalLong pttVolumeUpAfterMaxCount();
}
