package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AecMode;
import com.github.auties00.cobalt.wire.wam.type.AgcMode;
import com.github.auties00.cobalt.wire.wam.type.AndroidCamera2SupportLevel;
import com.github.auties00.cobalt.wire.wam.type.AndroidCameraApi;
import com.github.auties00.cobalt.wire.wam.type.AppExitReason;
import com.github.auties00.cobalt.wire.wam.type.AudioEngineType;
import com.github.auties00.cobalt.wire.wam.type.AudioOutputRoute;
import com.github.auties00.cobalt.wire.wam.type.BusyReason;
import com.github.auties00.cobalt.wire.wam.type.Ca2dExtensionConnectionState;
import com.github.auties00.cobalt.wire.wam.type.CallFromUi;
import com.github.auties00.cobalt.wire.wam.type.CallNetworkMedium;
import com.github.auties00.cobalt.wire.wam.type.CallRelayBindStatus;
import com.github.auties00.cobalt.wire.wam.type.CallResultType;
import com.github.auties00.cobalt.wire.wam.type.CallSetupErrorType;
import com.github.auties00.cobalt.wire.wam.type.CallSide;
import com.github.auties00.cobalt.wire.wam.type.CallTermReason;
import com.github.auties00.cobalt.wire.wam.type.CallTestInteger;
import com.github.auties00.cobalt.wire.wam.type.CallTransportType;
import com.github.auties00.cobalt.wire.wam.type.CallTrigger;
import com.github.auties00.cobalt.wire.wam.type.CallVideoState;
import com.github.auties00.cobalt.wire.wam.type.CallWakeupSource;
import com.github.auties00.cobalt.wire.wam.type.CameraPreviewMode;
import com.github.auties00.cobalt.wire.wam.type.CameraStartModeParams;
import com.github.auties00.cobalt.wire.wam.type.ClientIpVersion;
import com.github.auties00.cobalt.wire.wam.type.DataChannelConnectionState;
import com.github.auties00.cobalt.wire.wam.type.DeliveredPriority;
import com.github.auties00.cobalt.wire.wam.type.DeviceArch;
import com.github.auties00.cobalt.wire.wam.type.DndRingPathType;
import com.github.auties00.cobalt.wire.wam.type.EndCallConfirmationType;
import com.github.auties00.cobalt.wire.wam.type.FieldStatsRowType;
import com.github.auties00.cobalt.wire.wam.type.GcInitiationType;
import com.github.auties00.cobalt.wire.wam.type.GcRekeyMasterError;
import com.github.auties00.cobalt.wire.wam.type.GenaiBotType;
import com.github.auties00.cobalt.wire.wam.type.GenaiEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.GenaiExitPoint;
import com.github.auties00.cobalt.wire.wam.type.GooglePlayServicesStatus;
import com.github.auties00.cobalt.wire.wam.type.IncomingCallNotificationStateType;
import com.github.auties00.cobalt.wire.wam.type.IncomingCallUiActionType;
import com.github.auties00.cobalt.wire.wam.type.InitBweSource;
import com.github.auties00.cobalt.wire.wam.type.LobbyEntryPointType;
import com.github.auties00.cobalt.wire.wam.type.MlUndershootPytorchEdgeLibLoadErrorCode;
import com.github.auties00.cobalt.wire.wam.type.MlUndershootPytorchEdgeLibLoadStatus;
import com.github.auties00.cobalt.wire.wam.type.NsMode;
import com.github.auties00.cobalt.wire.wam.type.PeerCallNetworkMedium;
import com.github.auties00.cobalt.wire.wam.type.PeripheralDeviceType;
import com.github.auties00.cobalt.wire.wam.type.PushGhostCallReason;
import com.github.auties00.cobalt.wire.wam.type.PushOfferResult;
import com.github.auties00.cobalt.wire.wam.type.PushProvider;
import com.github.auties00.cobalt.wire.wam.type.PytorchEdgeLibLoadErrorCode;
import com.github.auties00.cobalt.wire.wam.type.PytorchEdgeLibLoadStatus;
import com.github.auties00.cobalt.wire.wam.type.RadioType;
import com.github.auties00.cobalt.wire.wam.type.RingerMode;
import com.github.auties00.cobalt.wire.wam.type.SfuDownlinkMlUndershootPytorchEdgeLibLoadErrorCode;
import com.github.auties00.cobalt.wire.wam.type.SfuDownlinkMlUndershootPytorchEdgeLibLoadStatus;
import com.github.auties00.cobalt.wire.wam.type.SwAecType;
import com.github.auties00.cobalt.wire.wam.type.VoipSettingReleaseType;
import com.github.auties00.cobalt.wire.wam.type.WaCallingHistoryGroupCallRecordSaveConditionCheckStatus;
import com.github.auties00.cobalt.wire.wam.type.WaVoipHistoryCallRedialStatus;
import com.github.auties00.cobalt.wire.wam.type.WaVoipHistorySaveCallRecordConditionCheckStatus;
import com.github.auties00.cobalt.wire.wam.type.XmppStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebCallWamEvent")
@WamEvent(id = 462)
public interface CallEvent extends WamEventSpec {
    @WamProperty(index = 1016, type = WamType.TIMER)
    Optional<Instant> acceptAckLatencyMs();

    @WamProperty(index = 2828, type = WamType.TIMER)
    Optional<Instant> acceptProcessingMs();

    @WamProperty(index = 1434, type = WamType.TIMER)
    Optional<Instant> acceptToFirstFrameDecodedTSs();

    @WamProperty(index = 2829, type = WamType.TIMER)
    Optional<Instant> acceptToRelayDeltaMs();

    @WamProperty(index = 1015, type = WamType.TIMER)
    Optional<Instant> acceptedButNotConnectedTimeSpentMs();

    @WamProperty(index = 1435, type = WamType.TIMER)
    Optional<Instant> ackToFirstFrameEncodedTSs();

    @WamProperty(index = 412, type = WamType.INTEGER)
    OptionalLong activeRelayProtocol();

    @WamProperty(index = 1428, type = WamType.INTEGER)
    OptionalLong adaptiveTcpErrorBitmap();

    @WamProperty(index = 1844, type = WamType.INTEGER)
    OptionalLong aecAlgorithmUsed();

    @WamProperty(index = 1186, type = WamType.TIMER)
    Optional<Instant> aflDisPrefetchFailure1x();

    @WamProperty(index = 1187, type = WamType.TIMER)
    Optional<Instant> aflDisPrefetchFailure2x();

    @WamProperty(index = 1188, type = WamType.TIMER)
    Optional<Instant> aflDisPrefetchFailure4x();

    @WamProperty(index = 1189, type = WamType.TIMER)
    Optional<Instant> aflDisPrefetchFailure8x();

    @WamProperty(index = 1190, type = WamType.TIMER)
    Optional<Instant> aflDisPrefetchFailureTotal();

    @WamProperty(index = 1191, type = WamType.TIMER)
    Optional<Instant> aflDisPrefetchSuccess1x();

    @WamProperty(index = 1192, type = WamType.TIMER)
    Optional<Instant> aflDisPrefetchSuccess2x();

    @WamProperty(index = 1193, type = WamType.TIMER)
    Optional<Instant> aflDisPrefetchSuccess4x();

    @WamProperty(index = 1194, type = WamType.TIMER)
    Optional<Instant> aflDisPrefetchSuccess8x();

    @WamProperty(index = 1195, type = WamType.TIMER)
    Optional<Instant> aflDisPrefetchSuccessTotal();

    @WamProperty(index = 1196, type = WamType.TIMER)
    Optional<Instant> aflNackFailure1x();

    @WamProperty(index = 1197, type = WamType.TIMER)
    Optional<Instant> aflNackFailure2x();

    @WamProperty(index = 1198, type = WamType.TIMER)
    Optional<Instant> aflNackFailure4x();

    @WamProperty(index = 1199, type = WamType.TIMER)
    Optional<Instant> aflNackFailure8x();

    @WamProperty(index = 1200, type = WamType.TIMER)
    Optional<Instant> aflNackFailureTotal();

    @WamProperty(index = 1201, type = WamType.TIMER)
    Optional<Instant> aflNackSuccess1x();

    @WamProperty(index = 1202, type = WamType.TIMER)
    Optional<Instant> aflNackSuccess2x();

    @WamProperty(index = 1203, type = WamType.TIMER)
    Optional<Instant> aflNackSuccess4x();

    @WamProperty(index = 1204, type = WamType.TIMER)
    Optional<Instant> aflNackSuccess8x();

    @WamProperty(index = 1205, type = WamType.TIMER)
    Optional<Instant> aflNackSuccessTotal();

    @WamProperty(index = 1206, type = WamType.TIMER)
    Optional<Instant> aflOther1x();

    @WamProperty(index = 1207, type = WamType.TIMER)
    Optional<Instant> aflOther2x();

    @WamProperty(index = 1208, type = WamType.TIMER)
    Optional<Instant> aflOther4x();

    @WamProperty(index = 1209, type = WamType.TIMER)
    Optional<Instant> aflOther8x();

    @WamProperty(index = 1210, type = WamType.TIMER)
    Optional<Instant> aflOtherTotal();

    @WamProperty(index = 1211, type = WamType.TIMER)
    Optional<Instant> aflPureLoss1x();

    @WamProperty(index = 1212, type = WamType.TIMER)
    Optional<Instant> aflPureLoss2x();

    @WamProperty(index = 1213, type = WamType.TIMER)
    Optional<Instant> aflPureLoss4x();

    @WamProperty(index = 1214, type = WamType.TIMER)
    Optional<Instant> aflPureLoss8x();

    @WamProperty(index = 1215, type = WamType.TIMER)
    Optional<Instant> aflPureLossTotal();

    @WamProperty(index = 1845, type = WamType.INTEGER)
    OptionalLong agcAlgorithmUsed();

    @WamProperty(index = 2382, type = WamType.TIMER)
    Optional<Instant> aiVoiceBackgroundingTime();

    @WamProperty(index = 2818, type = WamType.BOOLEAN)
    Optional<Boolean> aiVoiceHasImagePrompt();

    @WamProperty(index = 2383, type = WamType.BOOLEAN)
    Optional<Boolean> aiVoiceInAppBackgrounded();

    @WamProperty(index = 2384, type = WamType.BOOLEAN)
    Optional<Boolean> aiVoiceOutOfAppBackgrounded();

    @WamProperty(index = 593, type = WamType.INTEGER)
    OptionalLong allocErrorBitmap();

    @WamProperty(index = 1963, type = WamType.INTEGER)
    OptionalLong allocErrorRelayFailoverCnt();

    @WamProperty(index = 1374, type = WamType.TIMER)
    Optional<Instant> altAfFirstPongTimeMs();

    @WamProperty(index = 1375, type = WamType.INTEGER)
    OptionalLong altAfPingsSent();

    @WamProperty(index = 1055, type = WamType.BOOLEAN)
    Optional<Boolean> androidAudioRouteMismatch();

    @WamProperty(index = 444, type = WamType.ENUM)
    Optional<AndroidCamera2SupportLevel> androidCamera2MinHardwareSupportLevel();

    @WamProperty(index = 443, type = WamType.ENUM)
    Optional<AndroidCameraApi> androidCameraApi();

    @WamProperty(index = 477, type = WamType.TIMER)
    Optional<Instant> androidSystemPictureInPictureT();

    @WamProperty(index = 497, type = WamType.TIMER)
    Optional<Instant> androidTelecomTimeSpentBeforeReject();

    @WamProperty(index = 1917, type = WamType.INTEGER)
    OptionalLong answerCallDurationMs();

    @WamProperty(index = 2342, type = WamType.INTEGER)
    OptionalLong appDataRxRtpErrorCount();

    @WamProperty(index = 2343, type = WamType.INTEGER)
    OptionalLong appDataRxRtpPktCount();

    @WamProperty(index = 2344, type = WamType.INTEGER)
    OptionalLong appDataTxRtpErrorCount();

    @WamProperty(index = 2345, type = WamType.INTEGER)
    OptionalLong appDataTxRtpPktCount();

    @WamProperty(index = 1755, type = WamType.ENUM)
    Optional<AppExitReason> appExitReason();

    @WamProperty(index = 1109, type = WamType.BOOLEAN)
    Optional<Boolean> appInBackgroundDuringCall();

    @WamProperty(index = 1938, type = WamType.INTEGER)
    OptionalLong arEffectAttemptedCount();

    @WamProperty(index = 1939, type = WamType.INTEGER)
    OptionalLong arEffectCanceledCount();

    @WamProperty(index = 1940, type = WamType.TIMER)
    Optional<Instant> arEffectDurationT();

    @WamProperty(index = 1941, type = WamType.INTEGER)
    OptionalLong arEffectEnabledCount();

    @WamProperty(index = 1942, type = WamType.INTEGER)
    OptionalLong arEffectFailedCount();

    @WamProperty(index = 1943, type = WamType.TIMER)
    Optional<Instant> arEffectLoadingT();

    @WamProperty(index = 1872, type = WamType.TIMER)
    Optional<Instant> audShareAvgDuckingProcTime();

    @WamProperty(index = 1802, type = WamType.FLOAT)
    OptionalDouble audShareAvgLoudnessMic();

    @WamProperty(index = 1803, type = WamType.FLOAT)
    OptionalDouble audShareAvgLoudnessMixed();

    @WamProperty(index = 1804, type = WamType.FLOAT)
    OptionalDouble audShareAvgLoudnessSystem();

    @WamProperty(index = 1873, type = WamType.INTEGER)
    OptionalLong audShareDuckingProcTime500usTo1ms();

    @WamProperty(index = 1874, type = WamType.INTEGER)
    OptionalLong audShareDuckingProcTimeGt1ms();

    @WamProperty(index = 1875, type = WamType.INTEGER)
    OptionalLong audShareDuckingProcTimeLt500us();

    @WamProperty(index = 1805, type = WamType.INTEGER)
    OptionalLong audShareEchoConfidence();

    @WamProperty(index = 1806, type = WamType.TIMER)
    Optional<Instant> audShareMaxDuckingProcTime();

    @WamProperty(index = 1807, type = WamType.INTEGER)
    OptionalLong audShareNumInputFrames();

    @WamProperty(index = 1808, type = WamType.INTEGER)
    OptionalLong audShareNumMixedFrames();

    @WamProperty(index = 1809, type = WamType.INTEGER)
    OptionalLong audShareStartRequestCount();

    @WamProperty(index = 1810, type = WamType.INTEGER)
    OptionalLong audShareStartSuccessCount();

    @WamProperty(index = 1811, type = WamType.INTEGER)
    OptionalLong audShareStopRequestCount();

    @WamProperty(index = 1812, type = WamType.INTEGER)
    OptionalLong audShareStopSuccessCount();

    @WamProperty(index = 1119, type = WamType.FLOAT)
    OptionalDouble audStreamMixPct();

    @WamProperty(index = 1565, type = WamType.INTEGER)
    OptionalLong audioCalleeAcceptToDecodeT();

    @WamProperty(index = 2727, type = WamType.TIMER)
    Optional<Instant> audioCallerAcceptReceivedToDecodeT();

    @WamProperty(index = 1566, type = WamType.INTEGER)
    OptionalLong audioCallerOfferToDecodeT();

    @WamProperty(index = 2724, type = WamType.INTEGER)
    OptionalLong audioCodecBitrateCap();

    @WamProperty(index = 1847, type = WamType.FLOAT)
    OptionalDouble audioCodecDecodedFecBitrate();

    @WamProperty(index = 2213, type = WamType.FLOAT)
    OptionalDouble audioCodecDecodedFecBitrateDominantSpeaker();

    @WamProperty(index = 1782, type = WamType.FLOAT)
    OptionalDouble audioCodecDecodedFecBytes();

    @WamProperty(index = 755, type = WamType.INTEGER)
    OptionalLong audioCodecDecodedFecFrames();

    @WamProperty(index = 2628, type = WamType.FLOAT)
    OptionalDouble audioCodecDecodedFecSpeechBitrate();

    @WamProperty(index = 1848, type = WamType.FLOAT)
    OptionalDouble audioCodecDecodedNormalBitrate();

    @WamProperty(index = 2214, type = WamType.FLOAT)
    OptionalDouble audioCodecDecodedNormalBitrateDominantSpeaker();

    @WamProperty(index = 1783, type = WamType.FLOAT)
    OptionalDouble audioCodecDecodedNormalBytes();

    @WamProperty(index = 2629, type = WamType.FLOAT)
    OptionalDouble audioCodecDecodedNormalSpeechBitrate();

    @WamProperty(index = 756, type = WamType.INTEGER)
    OptionalLong audioCodecDecodedPlcFrames();

    @WamProperty(index = 751, type = WamType.INTEGER)
    OptionalLong audioCodecEncodedFecFrames();

    @WamProperty(index = 753, type = WamType.INTEGER)
    OptionalLong audioCodecEncodedNonVoiceFrames();

    @WamProperty(index = 1177, type = WamType.INTEGER)
    OptionalLong audioCodecEncodedThrottledVoiceFrames();

    @WamProperty(index = 752, type = WamType.INTEGER)
    OptionalLong audioCodecEncodedVoiceFrames();

    @WamProperty(index = 754, type = WamType.INTEGER)
    OptionalLong audioCodecReceivedFecFrames();

    @WamProperty(index = 1521, type = WamType.INTEGER)
    OptionalLong audioDecodeErrors();

    @WamProperty(index = 2122, type = WamType.BOOLEAN)
    Optional<Boolean> audioDevIsStalled();

    @WamProperty(index = 860, type = WamType.INTEGER)
    OptionalLong audioDeviceIssues();

    @WamProperty(index = 861, type = WamType.INTEGER)
    OptionalLong audioDeviceLastIssue();

    @WamProperty(index = 2123, type = WamType.INTEGER)
    OptionalLong audioDeviceStartupStatus();

    @WamProperty(index = 867, type = WamType.INTEGER)
    OptionalLong audioDeviceSwitchCount();

    @WamProperty(index = 866, type = WamType.TIMER)
    Optional<Instant> audioDeviceSwitchDuration();

    @WamProperty(index = 1813, type = WamType.BOOLEAN)
    Optional<Boolean> audioDuckingIsRun();

    @WamProperty(index = 2615, type = WamType.FLOAT)
    OptionalDouble audioDupEnabledRatio();

    @WamProperty(index = 1522, type = WamType.INTEGER)
    OptionalLong audioEncodeErrors();

    @WamProperty(index = 1736, type = WamType.INTEGER)
    OptionalLong audioFrameFromServerDup();

    @WamProperty(index = 724, type = WamType.TIMER)
    Optional<Instant> audioFrameLoss1xMs();

    @WamProperty(index = 725, type = WamType.TIMER)
    Optional<Instant> audioFrameLoss2xMs();

    @WamProperty(index = 726, type = WamType.TIMER)
    Optional<Instant> audioFrameLoss4xMs();

    @WamProperty(index = 727, type = WamType.TIMER)
    Optional<Instant> audioFrameLoss8xMs();

    @WamProperty(index = 83, type = WamType.INTEGER)
    OptionalLong audioGetFrameUnderflowPs();

    @WamProperty(index = 679, type = WamType.INTEGER)
    OptionalLong audioInbandFecDecoded();

    @WamProperty(index = 678, type = WamType.INTEGER)
    OptionalLong audioInbandFecEncoded();

    @WamProperty(index = 1318, type = WamType.INTEGER)
    OptionalLong audioJbResets();

    @WamProperty(index = 1334, type = WamType.INTEGER)
    OptionalLong audioJbResetsPartial();

    @WamProperty(index = 722, type = WamType.INTEGER)
    OptionalLong audioLossPeriodCount();

    @WamProperty(index = 1184, type = WamType.BOOLEAN)
    Optional<Boolean> audioNackHbhEnabled();

    @WamProperty(index = 1271, type = WamType.INTEGER)
    OptionalLong audioNackReqPktsProcessed();

    @WamProperty(index = 646, type = WamType.INTEGER)
    OptionalLong audioNackReqPktsRecvd();

    @WamProperty(index = 645, type = WamType.INTEGER)
    OptionalLong audioNackReqPktsSent();

    @WamProperty(index = 649, type = WamType.INTEGER)
    OptionalLong audioNackRtpRetransmitDiscardCount();

    @WamProperty(index = 651, type = WamType.INTEGER)
    OptionalLong audioNackRtpRetransmitFailCount();

    @WamProperty(index = 648, type = WamType.INTEGER)
    OptionalLong audioNackRtpRetransmitRecvdCount();

    @WamProperty(index = 647, type = WamType.INTEGER)
    OptionalLong audioNackRtpRetransmitReqCount();

    @WamProperty(index = 650, type = WamType.INTEGER)
    OptionalLong audioNackRtpRetransmitSentCount();

    @WamProperty(index = 1008, type = WamType.INTEGER)
    OptionalLong audioNumPiggybackRxPkt();

    @WamProperty(index = 1007, type = WamType.INTEGER)
    OptionalLong audioNumPiggybackTxPkt();

    @WamProperty(index = 1523, type = WamType.INTEGER)
    OptionalLong audioPacketizeErrors();

    @WamProperty(index = 1524, type = WamType.INTEGER)
    OptionalLong audioParseErrors();

    @WamProperty(index = 1283, type = WamType.INTEGER)
    OptionalLong audioPktsNotTriggerOutOfPaused();

    @WamProperty(index = 1138, type = WamType.INTEGER)
    OptionalLong audioPlayCbIntervalGtDefaultCnt();

    @WamProperty(index = 1139, type = WamType.INTEGER)
    OptionalLong audioPlayCbLatencyGteMaxCnt();

    @WamProperty(index = 2593, type = WamType.INTEGER)
    OptionalLong audioPlayerInitMs();

    @WamProperty(index = 2594, type = WamType.INTEGER)
    OptionalLong audioPlayerStartMs();

    @WamProperty(index = 1878, type = WamType.INTEGER)
    OptionalLong audioPutFrameOverflowCount();

    @WamProperty(index = 2595, type = WamType.INTEGER)
    OptionalLong audioRecorderInitMs();

    @WamProperty(index = 2596, type = WamType.INTEGER)
    OptionalLong audioRecorderStartMs();

    @WamProperty(index = 2330, type = WamType.INTEGER)
    OptionalLong audioRtpTsJumpBackCount();

    @WamProperty(index = 2331, type = WamType.INTEGER)
    OptionalLong audioRtpTsJumpBackMaxMs();

    @WamProperty(index = 2332, type = WamType.INTEGER)
    OptionalLong audioRtpTsJumpBackTotalMs();

    @WamProperty(index = 2333, type = WamType.INTEGER)
    OptionalLong audioRtpTsJumpForwardCount();

    @WamProperty(index = 2334, type = WamType.INTEGER)
    OptionalLong audioRtpTsJumpForwardMaxMs();

    @WamProperty(index = 2335, type = WamType.INTEGER)
    OptionalLong audioRtpTsJumpForwardTotalMs();

    @WamProperty(index = 677, type = WamType.INTEGER)
    OptionalLong audioRtxPktDiscarded();

    @WamProperty(index = 676, type = WamType.INTEGER)
    OptionalLong audioRtxPktProcessed();

    @WamProperty(index = 675, type = WamType.INTEGER)
    OptionalLong audioRtxPktSent();

    @WamProperty(index = 728, type = WamType.FLOAT)
    OptionalDouble audioRxAvgFpp();

    @WamProperty(index = 1561, type = WamType.INTEGER)
    OptionalLong audioStreamRecreations();

    @WamProperty(index = 1322, type = WamType.TIMER)
    Optional<Instant> audioSwbDurationMs();

    @WamProperty(index = 1351, type = WamType.INTEGER)
    OptionalLong audioTarget06Ms();

    @WamProperty(index = 1352, type = WamType.INTEGER)
    OptionalLong audioTarget1015Ms();

    @WamProperty(index = 1353, type = WamType.INTEGER)
    OptionalLong audioTarget1520Ms();

    @WamProperty(index = 1354, type = WamType.INTEGER)
    OptionalLong audioTarget2030Ms();

    @WamProperty(index = 1355, type = WamType.INTEGER)
    OptionalLong audioTarget30PlusMs();

    @WamProperty(index = 1356, type = WamType.INTEGER)
    OptionalLong audioTarget610Ms();

    @WamProperty(index = 1357, type = WamType.INTEGER)
    OptionalLong audioTargetBitrateDrops();

    @WamProperty(index = 450, type = WamType.FLOAT)
    OptionalDouble audioTotalBytesOnNonDefCell();

    @WamProperty(index = 1748, type = WamType.FLOAT)
    OptionalDouble audioTxActiveBitrate();

    @WamProperty(index = 2976, type = WamType.FLOAT)
    OptionalDouble audioTxCrestFactorAvg();

    @WamProperty(index = 2977, type = WamType.FLOAT)
    OptionalDouble audioTxCrestFactorP5();

    @WamProperty(index = 2978, type = WamType.FLOAT)
    OptionalDouble audioTxCrestFactorP50();

    @WamProperty(index = 2979, type = WamType.FLOAT)
    OptionalDouble audioTxCrestFactorP95();

    @WamProperty(index = 1749, type = WamType.FLOAT)
    OptionalDouble audioTxInbandFecBitrate();

    @WamProperty(index = 1750, type = WamType.FLOAT)
    OptionalDouble audioTxNonactiveBitrate();

    @WamProperty(index = 1751, type = WamType.FLOAT)
    OptionalDouble audioTxPktCount();

    @WamProperty(index = 2980, type = WamType.FLOAT)
    OptionalDouble audioTxSiiSnrAvg();

    @WamProperty(index = 2981, type = WamType.FLOAT)
    OptionalDouble audioTxSiiSnrP5();

    @WamProperty(index = 2982, type = WamType.FLOAT)
    OptionalDouble audioTxSiiSnrP50();

    @WamProperty(index = 2983, type = WamType.FLOAT)
    OptionalDouble audioTxSiiSnrP95();

    @WamProperty(index = 2984, type = WamType.FLOAT)
    OptionalDouble audioTxSpectralCentroidAvg();

    @WamProperty(index = 2985, type = WamType.FLOAT)
    OptionalDouble audioTxSpectralCentroidP5();

    @WamProperty(index = 2986, type = WamType.FLOAT)
    OptionalDouble audioTxSpectralCentroidP50();

    @WamProperty(index = 2987, type = WamType.FLOAT)
    OptionalDouble audioTxSpectralCentroidP95();

    @WamProperty(index = 2988, type = WamType.FLOAT)
    OptionalDouble audioTxSpectralFlatnessAvg();

    @WamProperty(index = 2989, type = WamType.FLOAT)
    OptionalDouble audioTxSpectralFlatnessP5();

    @WamProperty(index = 2990, type = WamType.FLOAT)
    OptionalDouble audioTxSpectralFlatnessP50();

    @WamProperty(index = 2991, type = WamType.FLOAT)
    OptionalDouble audioTxSpectralFlatnessP95();

    @WamProperty(index = 2992, type = WamType.FLOAT)
    OptionalDouble audioTxSpectralRolloffAvg();

    @WamProperty(index = 2993, type = WamType.FLOAT)
    OptionalDouble audioTxSpectralRolloffP5();

    @WamProperty(index = 2994, type = WamType.FLOAT)
    OptionalDouble audioTxSpectralRolloffP50();

    @WamProperty(index = 2995, type = WamType.FLOAT)
    OptionalDouble audioTxSpectralRolloffP95();

    @WamProperty(index = 1359, type = WamType.INTEGER)
    OptionalLong audioTxUlpFecPkts();

    @WamProperty(index = 1360, type = WamType.INTEGER)
    OptionalLong audioUlpFecRecovered();

    @WamProperty(index = 2268, type = WamType.TIMER)
    Optional<Instant> audioUnitSetupTime();

    @WamProperty(index = 2269, type = WamType.TIMER)
    Optional<Instant> audioUnitStopTime();

    @WamProperty(index = 2559, type = WamType.INTEGER)
    OptionalLong autoeqAlgorithmUsed();

    @WamProperty(index = 2875, type = WamType.INTEGER)
    OptionalLong automosReceiverModelDownloadFailureCount();

    @WamProperty(index = 2876, type = WamType.INTEGER)
    OptionalLong automosSenderModelDownloadFailureCount();

    @WamProperty(index = 192, type = WamType.FLOAT)
    OptionalDouble avAvgDelta();

    @WamProperty(index = 193, type = WamType.FLOAT)
    OptionalDouble avMaxDelta();

    @WamProperty(index = 1412, type = WamType.BOOLEAN)
    Optional<Boolean> avatarAttempted();

    @WamProperty(index = 1391, type = WamType.BOOLEAN)
    Optional<Boolean> avatarCanceled();

    @WamProperty(index = 1392, type = WamType.INTEGER)
    OptionalLong avatarCanceledCount();

    @WamProperty(index = 1393, type = WamType.TIMER)
    Optional<Instant> avatarDurationT();

    @WamProperty(index = 1394, type = WamType.BOOLEAN)
    Optional<Boolean> avatarEnabled();

    @WamProperty(index = 1395, type = WamType.INTEGER)
    OptionalLong avatarEnabledCount();

    @WamProperty(index = 1396, type = WamType.BOOLEAN)
    Optional<Boolean> avatarFailed();

    @WamProperty(index = 1397, type = WamType.INTEGER)
    OptionalLong avatarFailedCount();

    @WamProperty(index = 1398, type = WamType.TIMER)
    Optional<Instant> avatarLoadingT();

    @WamProperty(index = 1799, type = WamType.TIMER)
    Optional<Instant> aveTimeBwAudRcDynCondTrue();

    @WamProperty(index = 719, type = WamType.TIMER)
    Optional<Instant> aveTimeBwVidRcDynCondTrue();

    @WamProperty(index = 139, type = WamType.TIMER)
    Optional<Instant> avgClockCbT();

    @WamProperty(index = 2099, type = WamType.INTEGER)
    OptionalLong avgConsecutiveUdstPredictionLen();

    @WamProperty(index = 2560, type = WamType.FLOAT)
    OptionalDouble avgCpuTimeMlProcessingMs();

    @WamProperty(index = 1220, type = WamType.FLOAT)
    OptionalDouble avgCpuUtilizationPct();

    @WamProperty(index = 136, type = WamType.TIMER)
    Optional<Instant> avgDecodeT();

    @WamProperty(index = 1700, type = WamType.INTEGER)
    OptionalLong avgEchoConfidence();

    @WamProperty(index = 2734, type = WamType.FLOAT)
    OptionalDouble avgEchoConfidenceAfter30sec();

    @WamProperty(index = 1988, type = WamType.INTEGER)
    OptionalLong avgEchoConfidenceBeforeEc();

    @WamProperty(index = 2735, type = WamType.FLOAT)
    OptionalDouble avgEchoConfidenceFirst10sec();

    @WamProperty(index = 2736, type = WamType.FLOAT)
    OptionalDouble avgEchoConfidenceFirst20sec();

    @WamProperty(index = 2737, type = WamType.FLOAT)
    OptionalDouble avgEchoConfidenceFirst30sec();

    @WamProperty(index = 2738, type = WamType.FLOAT)
    OptionalDouble avgEchoLikelihoodAfter30sec();

    @WamProperty(index = 2739, type = WamType.FLOAT)
    OptionalDouble avgEchoLikelihoodFirst10sec();

    @WamProperty(index = 2740, type = WamType.FLOAT)
    OptionalDouble avgEchoLikelihoodFirst20sec();

    @WamProperty(index = 2741, type = WamType.FLOAT)
    OptionalDouble avgEchoLikelihoodFirst30sec();

    @WamProperty(index = 2710, type = WamType.INTEGER)
    OptionalLong avgEncInputSampleRate();

    @WamProperty(index = 2711, type = WamType.INTEGER)
    OptionalLong avgEncInternalSampleRate();

    @WamProperty(index = 1048, type = WamType.TIMER)
    Optional<Instant> avgEncRestartAndKfGenT();

    @WamProperty(index = 1047, type = WamType.TIMER)
    Optional<Instant> avgEncRestartIntervalT();

    @WamProperty(index = 135, type = WamType.TIMER)
    Optional<Instant> avgEncodeT();

    @WamProperty(index = 1302, type = WamType.FLOAT)
    OptionalDouble avgLoudnessDiffNoiseFrames();

    @WamProperty(index = 1303, type = WamType.FLOAT)
    OptionalDouble avgLoudnessDiffSpeechFrames();

    @WamProperty(index = 1304, type = WamType.FLOAT)
    OptionalDouble avgLoudnessInputNoiseFrames();

    @WamProperty(index = 1305, type = WamType.FLOAT)
    OptionalDouble avgLoudnessInputSpeechFrames();

    @WamProperty(index = 1306, type = WamType.FLOAT)
    OptionalDouble avgLoudnessOutputNoiseFrames();

    @WamProperty(index = 1307, type = WamType.FLOAT)
    OptionalDouble avgLoudnessOutputSpeechFrames();

    @WamProperty(index = 2819, type = WamType.FLOAT)
    OptionalDouble avgLufsMomentaryRx();

    @WamProperty(index = 2820, type = WamType.FLOAT)
    OptionalDouble avgLufsMomentaryTx();

    @WamProperty(index = 2821, type = WamType.FLOAT)
    OptionalDouble avgLufsShortTermRx();

    @WamProperty(index = 2822, type = WamType.FLOAT)
    OptionalDouble avgLufsShortTermTx();

    @WamProperty(index = 2303, type = WamType.TIMER)
    Optional<Instant> avgP2pBindTimeMs();

    @WamProperty(index = 1152, type = WamType.TIMER)
    Optional<Instant> avgPlayCbIntvT();

    @WamProperty(index = 137, type = WamType.TIMER)
    Optional<Instant> avgPlayCbT();

    @WamProperty(index = 495, type = WamType.TIMER)
    Optional<Instant> avgRecordCbIntvT();

    @WamProperty(index = 138, type = WamType.TIMER)
    Optional<Instant> avgRecordCbT();

    @WamProperty(index = 2169, type = WamType.TIMER)
    Optional<Instant> avgRxDelay();

    @WamProperty(index = 2732, type = WamType.FLOAT)
    OptionalDouble avgRxFrameLengthMs();

    @WamProperty(index = 141, type = WamType.FLOAT)
    OptionalDouble avgTargetBitrate();

    @WamProperty(index = 413, type = WamType.INTEGER)
    OptionalLong avgTcpConnCount();

    @WamProperty(index = 414, type = WamType.TIMER)
    Optional<Instant> avgTcpConnLatencyInMsec();

    @WamProperty(index = 2170, type = WamType.TIMER)
    Optional<Instant> avgTxDelay();

    @WamProperty(index = 2733, type = WamType.FLOAT)
    OptionalDouble avgTxFrameLengthMs();

    @WamProperty(index = 442, type = WamType.BOOLEAN)
    Optional<Boolean> batteryDropTriggered();

    @WamProperty(index = 2463, type = WamType.BOOLEAN)
    Optional<Boolean> batteryLow();

    @WamProperty(index = 441, type = WamType.BOOLEAN)
    Optional<Boolean> batteryLowTriggered();

    @WamProperty(index = 1880, type = WamType.STRING)
    Optional<String> betterP2pConnQualityStat();

    @WamProperty(index = 1881, type = WamType.STRING)
    Optional<String> betterRelayConnQualityStat();

    @WamProperty(index = 843, type = WamType.TIMER)
    Optional<Instant> biDirRelayRebindLatencyMs();

    @WamProperty(index = 844, type = WamType.TIMER)
    Optional<Instant> biDirRelayResetLatencyMs();

    @WamProperty(index = 1222, type = WamType.INTEGER)
    OptionalLong boundSocketIpAddressIsInvalid();

    @WamProperty(index = 1814, type = WamType.INTEGER)
    OptionalLong bridgeRecordCircularBufferFrameCount();

    @WamProperty(index = 2608, type = WamType.FLOAT)
    OptionalDouble brightnessEnhancedFramesPct();

    @WamProperty(index = 2609, type = WamType.INTEGER)
    OptionalLong brightnessToggleCount();

    @WamProperty(index = 2649, type = WamType.INTEGER)
    OptionalLong browserAvgUsedJsHeapSizeMb();

    @WamProperty(index = 2676, type = WamType.BOOLEAN)
    Optional<Boolean> browserBatteryChargingAtEnd();

    @WamProperty(index = 2677, type = WamType.BOOLEAN)
    Optional<Boolean> browserBatteryChargingAtStart();

    @WamProperty(index = 2678, type = WamType.INTEGER)
    OptionalLong browserBatteryChargingTimeSec();

    @WamProperty(index = 2679, type = WamType.INTEGER)
    OptionalLong browserBatteryDischargingTimeSec();

    @WamProperty(index = 2680, type = WamType.INTEGER)
    OptionalLong browserBatteryDrainPct();

    @WamProperty(index = 2681, type = WamType.INTEGER)
    OptionalLong browserBatteryLevelEndPct();

    @WamProperty(index = 2682, type = WamType.INTEGER)
    OptionalLong browserBatteryLevelStartPct();

    @WamProperty(index = 2683, type = WamType.BOOLEAN)
    Optional<Boolean> browserBatterySupported();

    @WamProperty(index = 2650, type = WamType.INTEGER)
    OptionalLong browserCpuPressureCriticalPct();

    @WamProperty(index = 2651, type = WamType.INTEGER)
    OptionalLong browserCpuPressureFairPct();

    @WamProperty(index = 2652, type = WamType.INTEGER)
    OptionalLong browserCpuPressureNominalPct();

    @WamProperty(index = 2653, type = WamType.INTEGER)
    OptionalLong browserCpuPressureSeriousPct();

    @WamProperty(index = 2654, type = WamType.BOOLEAN)
    Optional<Boolean> browserCpuPressureSupported();

    @WamProperty(index = 2655, type = WamType.INTEGER)
    OptionalLong browserJsHeapSizeLimitMb();

    @WamProperty(index = 2656, type = WamType.BOOLEAN)
    Optional<Boolean> browserMemorySupported();

    @WamProperty(index = 2657, type = WamType.INTEGER)
    OptionalLong browserPeakUsedJsHeapSizeMb();

    @WamProperty(index = 2658, type = WamType.INTEGER)
    OptionalLong browserTotalJsHeapSizeMb();

    @WamProperty(index = 33, type = WamType.BOOLEAN)
    Optional<Boolean> builtinAecAvailable();

    @WamProperty(index = 38, type = WamType.BOOLEAN)
    Optional<Boolean> builtinAecEnabled();

    @WamProperty(index = 36, type = WamType.STRING)
    Optional<String> builtinAecImplementor();

    @WamProperty(index = 37, type = WamType.STRING)
    Optional<String> builtinAecUuid();

    @WamProperty(index = 34, type = WamType.BOOLEAN)
    Optional<Boolean> builtinAgcAvailable();

    @WamProperty(index = 35, type = WamType.BOOLEAN)
    Optional<Boolean> builtinNsAvailable();

    @WamProperty(index = 2007, type = WamType.ENUM)
    Optional<BusyReason> busyReason();

    @WamProperty(index = 2029, type = WamType.INTEGER)
    OptionalLong bwaCountPrioritizeDomSpkrInSpeakerMode();

    @WamProperty(index = 2537, type = WamType.INTEGER)
    OptionalLong bwaStreamlineValidationFailure();

    @WamProperty(index = 1114, type = WamType.BOOLEAN)
    Optional<Boolean> bwaVidDisablingCandidate();

    @WamProperty(index = 1116, type = WamType.TIMER)
    Optional<Instant> bwaVidDisablingRxCandidateDuration();

    @WamProperty(index = 1115, type = WamType.TIMER)
    Optional<Instant> bwaVidDisablingTxCandidateDuration();

    @WamProperty(index = 2461, type = WamType.INTEGER)
    OptionalLong bweSlrOutputBps();

    @WamProperty(index = 3001, type = WamType.BOOLEAN)
    Optional<Boolean> c50Linked();

    @WamProperty(index = 3034, type = WamType.TIMER)
    Optional<Instant> ca2dExtensionAddT();

    @WamProperty(index = 3035, type = WamType.ENUM)
    Optional<Ca2dExtensionConnectionState> ca2dExtensionConnectionState();

    @WamProperty(index = 3036, type = WamType.TIMER)
    Optional<Instant> ca2dExtensionCreateT();

    @WamProperty(index = 3037, type = WamType.TIMER)
    Optional<Instant> ca2dPreviewT();

    @WamProperty(index = 132, type = WamType.TIMER)
    Optional<Instant> callAcceptFuncT();

    @WamProperty(index = 2854, type = WamType.BOOLEAN)
    Optional<Boolean> callAcceptRcvd();

    @WamProperty(index = 2855, type = WamType.BOOLEAN)
    Optional<Boolean> callAcceptSent();

    @WamProperty(index = 39, type = WamType.ENUM)
    Optional<AecMode> callAecMode();

    @WamProperty(index = 42, type = WamType.INTEGER)
    OptionalLong callAecOffset();

    @WamProperty(index = 43, type = WamType.INTEGER)
    OptionalLong callAecTailLength();

    @WamProperty(index = 52, type = WamType.ENUM)
    Optional<AgcMode> callAgcMode();

    @WamProperty(index = 268, type = WamType.BOOLEAN)
    Optional<Boolean> callAndrGcmFgEnabled();

    @WamProperty(index = 55, type = WamType.INTEGER)
    OptionalLong callAndroidAudioMode();

    @WamProperty(index = 57, type = WamType.INTEGER)
    OptionalLong callAndroidRecordAudioPreset();

    @WamProperty(index = 56, type = WamType.INTEGER)
    OptionalLong callAndroidRecordAudioSource();

    @WamProperty(index = 54, type = WamType.ENUM)
    Optional<AudioEngineType> callAudioEngineType();

    @WamProperty(index = 1336, type = WamType.ENUM)
    Optional<AudioOutputRoute> callAudioOutputRoute();

    @WamProperty(index = 96, type = WamType.FLOAT)
    OptionalDouble callAudioRestartCount();

    @WamProperty(index = 97, type = WamType.FLOAT)
    OptionalDouble callAudioRestartReason();

    @WamProperty(index = 259, type = WamType.TIMER)
    Optional<Instant> callAvgRottRx();

    @WamProperty(index = 258, type = WamType.TIMER)
    Optional<Instant> callAvgRottTx();

    @WamProperty(index = 107, type = WamType.TIMER)
    Optional<Instant> callAvgRtt();

    @WamProperty(index = 2215, type = WamType.TIMER)
    Optional<Instant> callAvgRttDominantSpeaker();

    @WamProperty(index = 1929, type = WamType.TIMER)
    Optional<Instant> callAvgRxStoppedT();

    @WamProperty(index = 195, type = WamType.FLOAT)
    OptionalDouble callBatteryChangePct();

    @WamProperty(index = 50, type = WamType.INTEGER)
    OptionalLong callCalculatedEcOffset();

    @WamProperty(index = 51, type = WamType.INTEGER)
    OptionalLong callCalculatedEcOffsetStddev();

    @WamProperty(index = 1406, type = WamType.TIMER)
    Optional<Instant> callConnectionLatencyMs();

    @WamProperty(index = 505, type = WamType.STRING)
    Optional<String> callCreatorHid();

    @WamProperty(index = 405, type = WamType.ENUM)
    Optional<CallNetworkMedium> callDefNetwork();

    @WamProperty(index = 99, type = WamType.FLOAT)
    OptionalDouble callEcRestartCount();

    @WamProperty(index = 46, type = WamType.FLOAT)
    OptionalDouble callEchoEnergy();

    @WamProperty(index = 44, type = WamType.INTEGER)
    OptionalLong callEchoLikelihood();

    @WamProperty(index = 47, type = WamType.FLOAT)
    OptionalDouble callEchoLikelihoodBeforeEc();

    @WamProperty(index = 2971, type = WamType.STRING)
    Optional<String> callEligibleBucketIdList();

    @WamProperty(index = 3005, type = WamType.STRING)
    Optional<String> callEligibleBucketNameList();

    @WamProperty(index = 2527, type = WamType.FLOAT)
    OptionalDouble callEndBatteryPct();

    @WamProperty(index = 1142, type = WamType.TIMER)
    Optional<Instant> callEndFrameLossMs();

    @WamProperty(index = 130, type = WamType.TIMER)
    Optional<Instant> callEndFuncT();

    @WamProperty(index = 70, type = WamType.BOOLEAN)
    Optional<Boolean> callEndReconnecting();

    @WamProperty(index = 1377, type = WamType.BOOLEAN)
    Optional<Boolean> callEndReconnectingBeforeCallActive();

    @WamProperty(index = 877, type = WamType.BOOLEAN)
    Optional<Boolean> callEndReconnectingBeforeNetworkChange();

    @WamProperty(index = 875, type = WamType.BOOLEAN)
    Optional<Boolean> callEndReconnectingBeforeP2pFailover();

    @WamProperty(index = 869, type = WamType.BOOLEAN)
    Optional<Boolean> callEndReconnectingBeforeRelayFailover();

    @WamProperty(index = 948, type = WamType.BOOLEAN)
    Optional<Boolean> callEndReconnectingBeforeRelayReset();

    @WamProperty(index = 1897, type = WamType.BOOLEAN)
    Optional<Boolean> callEndReconnectingE2ePingable();

    @WamProperty(index = 1898, type = WamType.BOOLEAN)
    Optional<Boolean> callEndReconnectingE2eSignalingAccessible();

    @WamProperty(index = 1595, type = WamType.INTEGER)
    OptionalLong callEndReconnectingExpectedBitmap();

    @WamProperty(index = 1385, type = WamType.BOOLEAN)
    Optional<Boolean> callEndReconnectingRelayPingable();

    @WamProperty(index = 1386, type = WamType.BOOLEAN)
    Optional<Boolean> callEndReconnectingSignalingAccessible();

    @WamProperty(index = 848, type = WamType.BOOLEAN)
    Optional<Boolean> callEndReconnectingSoonAfterCallActive();

    @WamProperty(index = 878, type = WamType.BOOLEAN)
    Optional<Boolean> callEndReconnectingSoonAfterNetworkChange();

    @WamProperty(index = 876, type = WamType.BOOLEAN)
    Optional<Boolean> callEndReconnectingSoonAfterP2pFailover();

    @WamProperty(index = 870, type = WamType.BOOLEAN)
    Optional<Boolean> callEndReconnectingSoonAfterRelayFailover();

    @WamProperty(index = 949, type = WamType.BOOLEAN)
    Optional<Boolean> callEndReconnectingSoonAfterRelayReset();

    @WamProperty(index = 1950, type = WamType.INTEGER)
    OptionalLong callEndReconnectingUnexpectedBitmap();

    @WamProperty(index = 2076, type = WamType.BOOLEAN)
    Optional<Boolean> callEndRelayBindsFailed();

    @WamProperty(index = 2538, type = WamType.INTEGER)
    OptionalLong callEndThermalState();

    @WamProperty(index = 1517, type = WamType.BOOLEAN)
    Optional<Boolean> callEndTxStopped();

    @WamProperty(index = 518, type = WamType.BOOLEAN)
    Optional<Boolean> callEndedDuringAudFreeze();

    @WamProperty(index = 517, type = WamType.BOOLEAN)
    Optional<Boolean> callEndedDuringVidFreeze();

    @WamProperty(index = 23, type = WamType.BOOLEAN)
    Optional<Boolean> callEndedInterrupted();

    @WamProperty(index = 1677, type = WamType.BOOLEAN)
    Optional<Boolean> callEndedPeersInterrupted();

    @WamProperty(index = 626, type = WamType.INTEGER)
    OptionalLong callEnterPipModeCount();

    @WamProperty(index = 2458, type = WamType.BOOLEAN)
    Optional<Boolean> callFromReminder();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<CallFromUi> callFromUi();

    @WamProperty(index = 2124, type = WamType.BOOLEAN)
    Optional<Boolean> callHasNoAudio();

    @WamProperty(index = 2428, type = WamType.BOOLEAN)
    Optional<Boolean> callHeld();

    @WamProperty(index = 45, type = WamType.FLOAT)
    OptionalDouble callHistEchoLikelihood();

    @WamProperty(index = 2308, type = WamType.FLOAT)
    OptionalDouble callInitJbGets();

    @WamProperty(index = 2309, type = WamType.FLOAT)
    OptionalDouble callInitJbMeanWait();

    @WamProperty(index = 2310, type = WamType.FLOAT)
    OptionalDouble callInitJbPlc();

    @WamProperty(index = 2311, type = WamType.FLOAT)
    OptionalDouble callInitJbPlcCng();

    @WamProperty(index = 2312, type = WamType.INTEGER)
    OptionalLong callInitReconnectingStateCount();

    @WamProperty(index = 1157, type = WamType.FLOAT)
    OptionalDouble callInitRxPktLossPct3s();

    @WamProperty(index = 2313, type = WamType.INTEGER)
    OptionalLong callInitVideoRenderAvgFps();

    @WamProperty(index = 2581, type = WamType.FLOAT)
    OptionalDouble callInitialEchoLikelihood();

    @WamProperty(index = 109, type = WamType.TIMER)
    Optional<Instant> callInitialRtt();

    @WamProperty(index = 22, type = WamType.BOOLEAN)
    Optional<Boolean> callInterrupted();

    @WamProperty(index = 108, type = WamType.TIMER)
    Optional<Instant> callLastRtt();

    @WamProperty(index = 1930, type = WamType.TIMER)
    Optional<Instant> callLastRxStoppedT();

    @WamProperty(index = 2580, type = WamType.STRING)
    Optional<String> callLinkRandomId();

    @WamProperty(index = 106, type = WamType.TIMER)
    Optional<Instant> callMaxRtt();

    @WamProperty(index = 1931, type = WamType.TIMER)
    Optional<Instant> callMaxRxStoppedT();

    @WamProperty(index = 422, type = WamType.INTEGER)
    OptionalLong callMessagesBufferedCount();

    @WamProperty(index = 105, type = WamType.TIMER)
    Optional<Instant> callMinRtt();

    @WamProperty(index = 1932, type = WamType.TIMER)
    Optional<Instant> callMinRxStoppedT();

    @WamProperty(index = 1913, type = WamType.TIMER)
    Optional<Instant> callMinimizedDurationT();

    @WamProperty(index = 1568, type = WamType.STRING)
    Optional<String> callNcTestId();

    @WamProperty(index = 1569, type = WamType.STRING)
    Optional<String> callNcTestName();

    @WamProperty(index = 76, type = WamType.ENUM)
    Optional<CallNetworkMedium> callNetwork();

    @WamProperty(index = 77, type = WamType.INTEGER)
    OptionalLong callNetworkSubtype();

    @WamProperty(index = 1632, type = WamType.INTEGER)
    OptionalLong callNotificationState();

    @WamProperty(index = 53, type = WamType.ENUM)
    Optional<NsMode> callNsMode();

    @WamProperty(index = 159, type = WamType.FLOAT)
    OptionalDouble callOfferAckTimout();

    @WamProperty(index = 243, type = WamType.TIMER)
    Optional<Instant> callOfferDelayT();

    @WamProperty(index = 102, type = WamType.TIMER)
    Optional<Instant> callOfferElapsedT();

    @WamProperty(index = 588, type = WamType.INTEGER)
    OptionalLong callOfferFanoutCount();

    @WamProperty(index = 134, type = WamType.TIMER)
    Optional<Instant> callOfferReceiptDelay();

    @WamProperty(index = 1903, type = WamType.TIMER)
    Optional<Instant> callOnNonoptimalRelayMs();

    @WamProperty(index = 18, type = WamType.BOOLEAN)
    Optional<Boolean> callP2pDisabled();

    @WamProperty(index = 2539, type = WamType.INTEGER)
    OptionalLong callPeakThermalState();

    @WamProperty(index = 15, type = WamType.STRING)
    Optional<String> callPeerAppVersion();

    @WamProperty(index = 2464, type = WamType.BOOLEAN)
    Optional<Boolean> callPeerHasBadge();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> callPeerIpStr();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong callPeerIpv4();

    @WamProperty(index = 2462, type = WamType.BOOLEAN)
    Optional<Boolean> callPeerIsMvFrictionEligible();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> callPeerPlatform();

    @WamProperty(index = 1225, type = WamType.STRING)
    Optional<String> callPeerTestBucket();

    @WamProperty(index = 2327, type = WamType.STRING)
    Optional<String> callPeerTestBucketAreaExposureMap();

    @WamProperty(index = 2314, type = WamType.STRING)
    Optional<String> callPeerTestBucketIdList();

    @WamProperty(index = 2328, type = WamType.STRING)
    Optional<String> callPeerTestBucketList();

    @WamProperty(index = 1678, type = WamType.BOOLEAN)
    Optional<Boolean> callPeersInterrupted();

    @WamProperty(index = 501, type = WamType.INTEGER)
    OptionalLong callPendingCallsAcceptedCount();

    @WamProperty(index = 498, type = WamType.INTEGER)
    OptionalLong callPendingCallsCount();

    @WamProperty(index = 499, type = WamType.INTEGER)
    OptionalLong callPendingCallsRejectedCount();

    @WamProperty(index = 500, type = WamType.INTEGER)
    OptionalLong callPendingCallsTerminatedCount();

    @WamProperty(index = 627, type = WamType.TIMER)
    Optional<Instant> callPipModeT();

    @WamProperty(index = 25, type = WamType.BOOLEAN)
    Optional<Boolean> callPlaybackCallbackStopped();

    @WamProperty(index = 93, type = WamType.FLOAT)
    OptionalDouble callPlaybackFramesPs();

    @WamProperty(index = 231, type = WamType.ENUM)
    Optional<RadioType> callRadioType();

    @WamProperty(index = 529, type = WamType.STRING)
    Optional<String> callRandomId();

    @WamProperty(index = 94, type = WamType.FLOAT)
    OptionalDouble callRecentPlaybackFramesPs();

    @WamProperty(index = 29, type = WamType.FLOAT)
    OptionalDouble callRecentRecordFramesPs();

    @WamProperty(index = 1492, type = WamType.INTEGER)
    OptionalLong callReconnectingProbeState();

    @WamProperty(index = 438, type = WamType.INTEGER)
    OptionalLong callReconnectingStateCount();

    @WamProperty(index = 24, type = WamType.BOOLEAN)
    Optional<Boolean> callRecordCallbackStopped();

    @WamProperty(index = 28, type = WamType.INTEGER)
    OptionalLong callRecordFramesPs();

    @WamProperty(index = 98, type = WamType.FLOAT)
    OptionalDouble callRecordMaxEnergyRatio();

    @WamProperty(index = 26, type = WamType.INTEGER)
    OptionalLong callRecordSilenceRatio();

    @WamProperty(index = 131, type = WamType.TIMER)
    Optional<Instant> callRejectFuncT();

    @WamProperty(index = 16, type = WamType.ENUM)
    Optional<CallRelayBindStatus> callRelayBindStatus();

    @WamProperty(index = 104, type = WamType.TIMER)
    Optional<Instant> callRelayCreateT();

    @WamProperty(index = 1300, type = WamType.INTEGER)
    OptionalLong callRelayErrorCode();

    @WamProperty(index = 17, type = WamType.STRING)
    Optional<String> callRelayServer();

    @WamProperty(index = 1909, type = WamType.STRING)
    Optional<String> callRelayServers();

    @WamProperty(index = 1301, type = WamType.INTEGER)
    OptionalLong callRelaysReceived();

    @WamProperty(index = 1155, type = WamType.STRING)
    Optional<String> callReplayerId();

    @WamProperty(index = 63, type = WamType.ENUM)
    Optional<CallResultType> callResult();

    @WamProperty(index = 2077, type = WamType.INTEGER)
    OptionalLong callResultAnc();

    @WamProperty(index = 2021, type = WamType.ENUM)
    Optional<CallResultType> callResultAtAppExit();

    @WamProperty(index = 1407, type = WamType.TIMER)
    Optional<Instant> callRingLatencyMs();

    @WamProperty(index = 103, type = WamType.TIMER)
    Optional<Instant> callRingingT();

    @WamProperty(index = 2577, type = WamType.FLOAT)
    OptionalDouble callRxAutomosNoiseAvg();

    @WamProperty(index = 3012, type = WamType.FLOAT)
    OptionalDouble callRxAutomosNoiseP5();

    @WamProperty(index = 3013, type = WamType.FLOAT)
    OptionalDouble callRxAutomosNoiseP50();

    @WamProperty(index = 3014, type = WamType.FLOAT)
    OptionalDouble callRxAutomosNoiseP95();

    @WamProperty(index = 2578, type = WamType.FLOAT)
    OptionalDouble callRxAutomosOverallAvg();

    @WamProperty(index = 3015, type = WamType.FLOAT)
    OptionalDouble callRxAutomosOverallP5();

    @WamProperty(index = 3016, type = WamType.FLOAT)
    OptionalDouble callRxAutomosOverallP50();

    @WamProperty(index = 3017, type = WamType.FLOAT)
    OptionalDouble callRxAutomosOverallP95();

    @WamProperty(index = 2579, type = WamType.FLOAT)
    OptionalDouble callRxAutomosSpeechAvg();

    @WamProperty(index = 3018, type = WamType.FLOAT)
    OptionalDouble callRxAutomosSpeechP5();

    @WamProperty(index = 3019, type = WamType.FLOAT)
    OptionalDouble callRxAutomosSpeechP50();

    @WamProperty(index = 3020, type = WamType.FLOAT)
    OptionalDouble callRxAutomosSpeechP95();

    @WamProperty(index = 121, type = WamType.FLOAT)
    OptionalDouble callRxAvgBitrate();

    @WamProperty(index = 2216, type = WamType.FLOAT)
    OptionalDouble callRxAvgBitrateDominantSpeaker();

    @WamProperty(index = 122, type = WamType.FLOAT)
    OptionalDouble callRxAvgBwe();

    @WamProperty(index = 125, type = WamType.TIMER)
    Optional<Instant> callRxAvgJitter();

    @WamProperty(index = 128, type = WamType.TIMER)
    Optional<Instant> callRxAvgLossPeriod();

    @WamProperty(index = 1329, type = WamType.INTEGER)
    OptionalLong callRxBweCnt();

    @WamProperty(index = 124, type = WamType.TIMER)
    Optional<Instant> callRxMaxJitter();

    @WamProperty(index = 127, type = WamType.TIMER)
    Optional<Instant> callRxMaxLossPeriod();

    @WamProperty(index = 123, type = WamType.TIMER)
    Optional<Instant> callRxMinJitter();

    @WamProperty(index = 126, type = WamType.TIMER)
    Optional<Instant> callRxMinLossPeriod();

    @WamProperty(index = 120, type = WamType.FLOAT)
    OptionalDouble callRxPktLossPct();

    @WamProperty(index = 892, type = WamType.FLOAT)
    OptionalDouble callRxPktLossRetransmitPct();

    @WamProperty(index = 100, type = WamType.TIMER)
    Optional<Instant> callRxStoppedT();

    @WamProperty(index = 1964, type = WamType.BOOLEAN)
    Optional<Boolean> callSampledForProbing();

    @WamProperty(index = 30, type = WamType.INTEGER)
    OptionalLong callSamplingRate();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> callSelfIpStr();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong callSelfIpv4();

    @WamProperty(index = 2069, type = WamType.STRING)
    Optional<String> callSelfV4Ip();

    @WamProperty(index = 2070, type = WamType.STRING)
    Optional<String> callSelfV6Ip();

    @WamProperty(index = 68, type = WamType.INTEGER)
    OptionalLong callServerNackErrorCode();

    @WamProperty(index = 71, type = WamType.ENUM)
    Optional<CallSetupErrorType> callSetupErrorType();

    @WamProperty(index = 101, type = WamType.TIMER)
    Optional<Instant> callSetupT();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<CallSide> callSide();

    @WamProperty(index = 133, type = WamType.TIMER)
    Optional<Instant> callSoundPortFuncT();

    @WamProperty(index = 2528, type = WamType.FLOAT)
    OptionalDouble callStartBatteryPct();

    @WamProperty(index = 129, type = WamType.TIMER)
    Optional<Instant> callStartFuncT();

    @WamProperty(index = 2540, type = WamType.INTEGER)
    OptionalLong callStartThermalState();

    @WamProperty(index = 41, type = WamType.INTEGER)
    OptionalLong callSwAecMode();

    @WamProperty(index = 40, type = WamType.ENUM)
    Optional<SwAecType> callSwAecType();

    @WamProperty(index = 1363, type = WamType.TIMER)
    Optional<Instant> callSystemPipDurationT();

    @WamProperty(index = 92, type = WamType.TIMER)
    Optional<Instant> callT();

    @WamProperty(index = 2180, type = WamType.INTEGER)
    OptionalLong callTDominantSpeaker();

    @WamProperty(index = 2352, type = WamType.TIMER)
    Optional<Instant> callTSelfDominantSpeaker();

    @WamProperty(index = 69, type = WamType.ENUM)
    Optional<CallTermReason> callTermReason();

    @WamProperty(index = 1324, type = WamType.BOOLEAN)
    Optional<Boolean> callTestBoolean();

    @WamProperty(index = 19, type = WamType.STRING)
    Optional<String> callTestBucket();

    @WamProperty(index = 2329, type = WamType.STRING)
    Optional<String> callTestBucketAreaExposureMap();

    @WamProperty(index = 2045, type = WamType.STRING)
    Optional<String> callTestBucketExposureMap();

    @WamProperty(index = 1890, type = WamType.STRING)
    Optional<String> callTestBucketIdList();

    @WamProperty(index = 1919, type = WamType.STRING)
    Optional<String> callTestBucketNameList();

    @WamProperty(index = 1325, type = WamType.FLOAT)
    OptionalDouble callTestDouble();

    @WamProperty(index = 318, type = WamType.INTEGER)
    OptionalLong callTestEvent();

    @WamProperty(index = 1326, type = WamType.ENUM)
    Optional<CallTestInteger> callTestInteger();

    @WamProperty(index = 1327, type = WamType.INTEGER)
    OptionalLong callTestLong();

    @WamProperty(index = 1328, type = WamType.STRING)
    Optional<String> callTestString();

    @WamProperty(index = 78, type = WamType.INTEGER)
    OptionalLong callTransitionCount();

    @WamProperty(index = 72, type = WamType.ENUM)
    Optional<CallTransportType> callTransport();

    @WamProperty(index = 1268, type = WamType.INTEGER)
    OptionalLong callTransportMaxAllocRetries();

    @WamProperty(index = 80, type = WamType.INTEGER)
    OptionalLong callTransportP2pToRelayFallbackCount();

    @WamProperty(index = 79, type = WamType.INTEGER)
    OptionalLong callTransportRelayToRelayFallbackCount();

    @WamProperty(index = 1429, type = WamType.INTEGER)
    OptionalLong callTransportTcpFallbackToUdpCount();

    @WamProperty(index = 1430, type = WamType.INTEGER)
    OptionalLong callTransportTcpUsedCount();

    @WamProperty(index = 1319, type = WamType.FLOAT)
    OptionalDouble callTransportTotalRxAllocBytes();

    @WamProperty(index = 1320, type = WamType.FLOAT)
    OptionalDouble callTransportTotalTxAllocBytes();

    @WamProperty(index = 1321, type = WamType.INTEGER)
    OptionalLong callTransportTxAllocCnt();

    @WamProperty(index = 1990, type = WamType.ENUM)
    Optional<CallTrigger> callTrigger();

    @WamProperty(index = 2556, type = WamType.FLOAT)
    OptionalDouble callTxAutomosNoiseAvg();

    @WamProperty(index = 3021, type = WamType.FLOAT)
    OptionalDouble callTxAutomosNoiseP5();

    @WamProperty(index = 3022, type = WamType.FLOAT)
    OptionalDouble callTxAutomosNoiseP50();

    @WamProperty(index = 3023, type = WamType.FLOAT)
    OptionalDouble callTxAutomosNoiseP95();

    @WamProperty(index = 2557, type = WamType.FLOAT)
    OptionalDouble callTxAutomosOverallAvg();

    @WamProperty(index = 3024, type = WamType.FLOAT)
    OptionalDouble callTxAutomosOverallP5();

    @WamProperty(index = 3025, type = WamType.FLOAT)
    OptionalDouble callTxAutomosOverallP50();

    @WamProperty(index = 3026, type = WamType.FLOAT)
    OptionalDouble callTxAutomosOverallP95();

    @WamProperty(index = 2558, type = WamType.FLOAT)
    OptionalDouble callTxAutomosSpeechAvg();

    @WamProperty(index = 3027, type = WamType.FLOAT)
    OptionalDouble callTxAutomosSpeechP5();

    @WamProperty(index = 3028, type = WamType.FLOAT)
    OptionalDouble callTxAutomosSpeechP50();

    @WamProperty(index = 3029, type = WamType.FLOAT)
    OptionalDouble callTxAutomosSpeechP95();

    @WamProperty(index = 112, type = WamType.FLOAT)
    OptionalDouble callTxAvgBitrate();

    @WamProperty(index = 113, type = WamType.FLOAT)
    OptionalDouble callTxAvgBwe();

    @WamProperty(index = 116, type = WamType.TIMER)
    Optional<Instant> callTxAvgJitter();

    @WamProperty(index = 119, type = WamType.TIMER)
    Optional<Instant> callTxAvgLossPeriod();

    @WamProperty(index = 1330, type = WamType.INTEGER)
    OptionalLong callTxBweCnt();

    @WamProperty(index = 115, type = WamType.TIMER)
    Optional<Instant> callTxMaxJitter();

    @WamProperty(index = 118, type = WamType.TIMER)
    Optional<Instant> callTxMaxLossPeriod();

    @WamProperty(index = 114, type = WamType.TIMER)
    Optional<Instant> callTxMinJitter();

    @WamProperty(index = 117, type = WamType.TIMER)
    Optional<Instant> callTxMinLossPeriod();

    @WamProperty(index = 111, type = WamType.FLOAT)
    OptionalDouble callTxPktErrorPct();

    @WamProperty(index = 110, type = WamType.FLOAT)
    OptionalDouble callTxPktLossPct();

    @WamProperty(index = 1518, type = WamType.TIMER)
    Optional<Instant> callTxStoppedT();

    @WamProperty(index = 1574, type = WamType.BOOLEAN)
    Optional<Boolean> callUsedVpn();

    @WamProperty(index = 20, type = WamType.INTEGER)
    OptionalLong callUserRate();

    @WamProperty(index = 156, type = WamType.ENUM)
    Optional<CallWakeupSource> callWakeupSource();

    @WamProperty(index = 1383, type = WamType.TIMER)
    Optional<Instant> calleeAcceptToConnectedT();

    @WamProperty(index = 447, type = WamType.TIMER)
    Optional<Instant> calleeAcceptToDecodeT();

    @WamProperty(index = 1384, type = WamType.TIMER)
    Optional<Instant> calleeOfferToRingT();

    @WamProperty(index = 1596, type = WamType.TIMER)
    Optional<Instant> calleePushLatencyMs();

    @WamProperty(index = 476, type = WamType.BOOLEAN)
    Optional<Boolean> callerInContact();

    @WamProperty(index = 445, type = WamType.TIMER)
    Optional<Instant> callerOfferToDecodeT();

    @WamProperty(index = 446, type = WamType.TIMER)
    Optional<Instant> callerVidRtpToDecodeT();

    @WamProperty(index = 2488, type = WamType.INTEGER)
    OptionalLong callingHistoryQuickhdUsedBitrate();

    @WamProperty(index = 2489, type = WamType.INTEGER)
    OptionalLong callingHistoryTpRecordBothMatchCount();

    @WamProperty(index = 2490, type = WamType.INTEGER)
    OptionalLong callingHistoryTpRecordCount();

    @WamProperty(index = 2491, type = WamType.INTEGER)
    OptionalLong callingHistoryTpRecordPeerMatchCount();

    @WamProperty(index = 2492, type = WamType.INTEGER)
    OptionalLong callingHistoryTpRecordSelfMatchCount();

    @WamProperty(index = 2877, type = WamType.INTEGER)
    OptionalLong callingHistoryUaqcRecordAppliedBitrateBps();

    @WamProperty(index = 2878, type = WamType.INTEGER)
    OptionalLong callingHistoryUaqcRecordBitrateBps();

    @WamProperty(index = 2879, type = WamType.INTEGER)
    OptionalLong callingHistoryUaqcRecordBothMatchCount();

    @WamProperty(index = 2880, type = WamType.INTEGER)
    OptionalLong callingHistoryUaqcRecordCount();

    @WamProperty(index = 2881, type = WamType.INTEGER)
    OptionalLong callingHistoryUaqcRecordFilteredCount();

    @WamProperty(index = 2882, type = WamType.INTEGER)
    OptionalLong callingHistoryUaqcRecordPeerMatchCount();

    @WamProperty(index = 2883, type = WamType.INTEGER)
    OptionalLong callingHistoryUaqcRecordPlrPct();

    @WamProperty(index = 2884, type = WamType.INTEGER)
    OptionalLong callingHistoryUaqcRecordRembBps();

    @WamProperty(index = 2885, type = WamType.INTEGER)
    OptionalLong callingHistoryUaqcRecordRttMs();

    @WamProperty(index = 2886, type = WamType.INTEGER)
    OptionalLong callingHistoryUaqcRecordSelfMatchCount();

    @WamProperty(index = 765, type = WamType.INTEGER)
    OptionalLong cameraFormats();

    @WamProperty(index = 850, type = WamType.INTEGER)
    OptionalLong cameraIssues();

    @WamProperty(index = 851, type = WamType.INTEGER)
    OptionalLong cameraLastIssue();

    @WamProperty(index = 2480, type = WamType.INTEGER)
    OptionalLong cameraMaxRetryCount();

    @WamProperty(index = 2369, type = WamType.BOOLEAN)
    Optional<Boolean> cameraOffCallStart();

    @WamProperty(index = 331, type = WamType.INTEGER)
    OptionalLong cameraOffCount();

    @WamProperty(index = 2348, type = WamType.INTEGER)
    OptionalLong cameraOnCount();

    @WamProperty(index = 1131, type = WamType.TIMER)
    Optional<Instant> cameraPauseT();

    @WamProperty(index = 849, type = WamType.BOOLEAN)
    Optional<Boolean> cameraPermission();

    @WamProperty(index = 322, type = WamType.ENUM)
    Optional<CameraPreviewMode> cameraPreviewMode();

    @WamProperty(index = 2481, type = WamType.TIMER)
    Optional<Instant> cameraSetVideoPortDuration();

    @WamProperty(index = 852, type = WamType.TIMER)
    Optional<Instant> cameraStartDuration();

    @WamProperty(index = 856, type = WamType.TIMER)
    Optional<Instant> cameraStartFailureDuration();

    @WamProperty(index = 233, type = WamType.ENUM)
    Optional<CameraStartModeParams> cameraStartMode();

    @WamProperty(index = 916, type = WamType.TIMER)
    Optional<Instant> cameraStartToFirstFrameT();

    @WamProperty(index = 853, type = WamType.TIMER)
    Optional<Instant> cameraStopDuration();

    @WamProperty(index = 858, type = WamType.INTEGER)
    OptionalLong cameraStopFailureCount();

    @WamProperty(index = 855, type = WamType.INTEGER)
    OptionalLong cameraSwitchCount();

    @WamProperty(index = 854, type = WamType.TIMER)
    Optional<Instant> cameraSwitchDuration();

    @WamProperty(index = 857, type = WamType.TIMER)
    Optional<Instant> cameraSwitchFailureDuration();

    @WamProperty(index = 1857, type = WamType.BOOLEAN)
    Optional<Boolean> canTriggerVideoDisabling();

    @WamProperty(index = 1606, type = WamType.BOOLEAN)
    Optional<Boolean> canUseFullScreenIntent();

    @WamProperty(index = 2341, type = WamType.STRING)
    Optional<String> capiCallId();

    @WamProperty(index = 2482, type = WamType.TIMER)
    Optional<Instant> captureDeviceCreateDuration();

    @WamProperty(index = 1437, type = WamType.INTEGER)
    OptionalLong captureDriverNotifyCountSs();

    @WamProperty(index = 2406, type = WamType.INTEGER)
    OptionalLong cellIdAtEnd();

    @WamProperty(index = 2407, type = WamType.INTEGER)
    OptionalLong cellIdAtStart();

    @WamProperty(index = 2408, type = WamType.STRING)
    Optional<String> cellInfoAtEnd();

    @WamProperty(index = 2409, type = WamType.STRING)
    Optional<String> cellInfoAtStart();

    @WamProperty(index = 527, type = WamType.BOOLEAN)
    Optional<Boolean> clampedBwe();

    @WamProperty(index = 1582, type = WamType.TIMER)
    Optional<Instant> closeTcpSocketT();

    @WamProperty(index = 760, type = WamType.FLOAT)
    OptionalDouble combinedE2eAvgRtt();

    @WamProperty(index = 761, type = WamType.FLOAT)
    OptionalDouble combinedE2eMaxRtt();

    @WamProperty(index = 759, type = WamType.FLOAT)
    OptionalDouble combinedE2eMinRtt();

    @WamProperty(index = 623, type = WamType.INTEGER)
    OptionalLong confBridgeSamplingRate();

    @WamProperty(index = 1891, type = WamType.TIMER)
    Optional<Instant> connectToDecodeT();

    @WamProperty(index = 1226, type = WamType.BOOLEAN)
    Optional<Boolean> connectedToCar();

    @WamProperty(index = 2160, type = WamType.BOOLEAN)
    Optional<Boolean> connectedToVpnAtCallStart();

    @WamProperty(index = 974, type = WamType.BOOLEAN)
    Optional<Boolean> conservativeModeStopped();

    @WamProperty(index = 743, type = WamType.TIMER)
    Optional<Instant> conservativeRampUpExploringT();

    @WamProperty(index = 643, type = WamType.INTEGER)
    OptionalLong conservativeRampUpHeldCount();

    @WamProperty(index = 741, type = WamType.TIMER)
    Optional<Instant> conservativeRampUpHoldingT();

    @WamProperty(index = 742, type = WamType.TIMER)
    Optional<Instant> conservativeRampUpRampingUpT();

    @WamProperty(index = 2188, type = WamType.INTEGER)
    OptionalLong countInMcp();

    @WamProperty(index = 2189, type = WamType.INTEGER)
    OptionalLong countInSru();

    @WamProperty(index = 1223, type = WamType.FLOAT)
    OptionalDouble cpuOverUtilizationPct();

    @WamProperty(index = 1820, type = WamType.FLOAT)
    OptionalDouble cpuUtilizationAvg();

    @WamProperty(index = 1821, type = WamType.FLOAT)
    OptionalDouble cpuUtilizationPeak();

    @WamProperty(index = 519, type = WamType.BOOLEAN)
    Optional<Boolean> createdFromGroupCallDowngrade();

    @WamProperty(index = 1556, type = WamType.TIMER)
    Optional<Instant> criticalGroupUpdateProcessT();

    @WamProperty(index = 1438, type = WamType.INTEGER)
    OptionalLong croppedColumnsSs();

    @WamProperty(index = 1439, type = WamType.INTEGER)
    OptionalLong croppedRowsSs();

    @WamProperty(index = 2104, type = WamType.ENUM)
    Optional<DataChannelConnectionState> dataChannelConnectionState();

    @WamProperty(index = 2105, type = WamType.INTEGER)
    OptionalLong dataChannelRxBytes();

    @WamProperty(index = 2106, type = WamType.INTEGER)
    OptionalLong dataChannelRxBytesDropped();

    @WamProperty(index = 2107, type = WamType.INTEGER)
    OptionalLong dataChannelRxMsgs();

    @WamProperty(index = 2108, type = WamType.TIMER)
    Optional<Instant> dataChannelSetupT();

    @WamProperty(index = 2109, type = WamType.INTEGER)
    OptionalLong dataChannelTxBufferedMsgs();

    @WamProperty(index = 2110, type = WamType.INTEGER)
    OptionalLong dataChannelTxBytes();

    @WamProperty(index = 2111, type = WamType.INTEGER)
    OptionalLong dataChannelTxBytesDropped();

    @WamProperty(index = 2112, type = WamType.INTEGER)
    OptionalLong dataChannelTxMsgs();

    @WamProperty(index = 2113, type = WamType.INTEGER)
    OptionalLong dataChannelTxReliableMsgs();

    @WamProperty(index = 537, type = WamType.BOOLEAN)
    Optional<Boolean> dataLimitOnAltNetworkReached();

    @WamProperty(index = 2483, type = WamType.FLOAT)
    OptionalDouble debugMetric1();

    @WamProperty(index = 2484, type = WamType.FLOAT)
    OptionalDouble debugMetric2();

    @WamProperty(index = 2485, type = WamType.FLOAT)
    OptionalDouble debugMetric3();

    @WamProperty(index = 2486, type = WamType.FLOAT)
    OptionalDouble debugMetric4();

    @WamProperty(index = 2487, type = WamType.FLOAT)
    OptionalDouble debugMetric5();

    @WamProperty(index = 1756, type = WamType.TIMER)
    Optional<Instant> dec1280wFreezeT();

    @WamProperty(index = 1757, type = WamType.TIMER)
    Optional<Instant> dec1280wPauseT();

    @WamProperty(index = 1758, type = WamType.TIMER)
    Optional<Instant> dec160wFreezeT();

    @WamProperty(index = 1759, type = WamType.TIMER)
    Optional<Instant> dec160wPauseT();

    @WamProperty(index = 1760, type = WamType.TIMER)
    Optional<Instant> dec240wFreezeT();

    @WamProperty(index = 1761, type = WamType.TIMER)
    Optional<Instant> dec240wPauseT();

    @WamProperty(index = 1762, type = WamType.TIMER)
    Optional<Instant> dec320wFreezeT();

    @WamProperty(index = 1763, type = WamType.TIMER)
    Optional<Instant> dec320wPauseT();

    @WamProperty(index = 1764, type = WamType.TIMER)
    Optional<Instant> dec480wFreezeT();

    @WamProperty(index = 1765, type = WamType.TIMER)
    Optional<Instant> dec480wPauseT();

    @WamProperty(index = 1766, type = WamType.TIMER)
    Optional<Instant> dec640wFreezeT();

    @WamProperty(index = 1767, type = WamType.TIMER)
    Optional<Instant> dec640wPauseT();

    @WamProperty(index = 1768, type = WamType.TIMER)
    Optional<Instant> dec960wFreezeT();

    @WamProperty(index = 1769, type = WamType.TIMER)
    Optional<Instant> dec960wPauseT();

    @WamProperty(index = 2758, type = WamType.FLOAT)
    OptionalDouble decAspectRatioSs();

    @WamProperty(index = 2759, type = WamType.TIMER)
    Optional<Instant> decSs1080pFreezeT();

    @WamProperty(index = 2760, type = WamType.TIMER)
    Optional<Instant> decSs1080pPauseT();

    @WamProperty(index = 2761, type = WamType.TIMER)
    Optional<Instant> decSs1440pFreezeT();

    @WamProperty(index = 2762, type = WamType.TIMER)
    Optional<Instant> decSs1440pPauseT();

    @WamProperty(index = 2763, type = WamType.TIMER)
    Optional<Instant> decSs2160pFreezeT();

    @WamProperty(index = 2764, type = WamType.TIMER)
    Optional<Instant> decSs2160pPauseT();

    @WamProperty(index = 2765, type = WamType.TIMER)
    Optional<Instant> decSs320pFreezeT();

    @WamProperty(index = 2766, type = WamType.TIMER)
    Optional<Instant> decSs320pPauseT();

    @WamProperty(index = 2767, type = WamType.TIMER)
    Optional<Instant> decSs480pFreezeT();

    @WamProperty(index = 2768, type = WamType.TIMER)
    Optional<Instant> decSs480pPauseT();

    @WamProperty(index = 2769, type = WamType.TIMER)
    Optional<Instant> decSs720pFreezeT();

    @WamProperty(index = 2770, type = WamType.TIMER)
    Optional<Instant> decSs720pPauseT();

    @WamProperty(index = 2771, type = WamType.TIMER)
    Optional<Instant> decSs960pFreezeT();

    @WamProperty(index = 2772, type = WamType.TIMER)
    Optional<Instant> decSs960pPauseT();

    @WamProperty(index = 1858, type = WamType.TIMER)
    Optional<Instant> decVidStreamActiveTime();

    @WamProperty(index = 2647, type = WamType.INTEGER)
    OptionalLong defaultMicMode();

    @WamProperty(index = 2171, type = WamType.INTEGER)
    OptionalLong delayResetCount();

    @WamProperty(index = 2031, type = WamType.INTEGER)
    OptionalLong deviceArClass();

    @WamProperty(index = 1675, type = WamType.ENUM)
    Optional<DeviceArch> deviceArch();

    @WamProperty(index = 230, type = WamType.STRING)
    Optional<String> deviceBoard();

    @WamProperty(index = 1269, type = WamType.STRING)
    Optional<String> deviceClass();

    @WamProperty(index = 229, type = WamType.STRING)
    Optional<String> deviceHardware();

    @WamProperty(index = 1879, type = WamType.INTEGER)
    OptionalLong deviceNativeSamplingRate();

    @WamProperty(index = 1364, type = WamType.FLOAT)
    OptionalDouble dlOnlyHighPlrPct();

    @WamProperty(index = 2923, type = WamType.ENUM)
    Optional<DndRingPathType> dndRingPath();

    @WamProperty(index = 1597, type = WamType.BOOLEAN)
    Optional<Boolean> doNotDisturbEnabled();

    @WamProperty(index = 2588, type = WamType.BOOLEAN)
    Optional<Boolean> dontConnectForPausedVidTargetSample();

    @WamProperty(index = 1440, type = WamType.INTEGER)
    OptionalLong downlinkOvershootCountSs();

    @WamProperty(index = 1969, type = WamType.INTEGER)
    OptionalLong downlinkSbweRttSlopeCongestionCount();

    @WamProperty(index = 2270, type = WamType.TIMER)
    Optional<Instant> driverInitTime();

    @WamProperty(index = 2005, type = WamType.INTEGER)
    OptionalLong droppedNetEventCount();

    @WamProperty(index = 2114, type = WamType.TIMER)
    Optional<Instant> dtlsConnectionSetupT();

    @WamProperty(index = 2115, type = WamType.INTEGER)
    OptionalLong dtlsRxBytes();

    @WamProperty(index = 2116, type = WamType.INTEGER)
    OptionalLong dtlsRxPackets();

    @WamProperty(index = 2117, type = WamType.INTEGER)
    OptionalLong dtlsRxPacketsDropped();

    @WamProperty(index = 2118, type = WamType.INTEGER)
    OptionalLong dtlsTxBytes();

    @WamProperty(index = 2119, type = WamType.INTEGER)
    OptionalLong dtlsTxPackets();

    @WamProperty(index = 2120, type = WamType.INTEGER)
    OptionalLong dtlsTxPacketsDropped();

    @WamProperty(index = 2600, type = WamType.INTEGER)
    OptionalLong dtmfBytesSent();

    @WamProperty(index = 2601, type = WamType.INTEGER)
    OptionalLong dtmfBytesSentFailed();

    @WamProperty(index = 2602, type = WamType.INTEGER)
    OptionalLong dtmfEventSent();

    @WamProperty(index = 2603, type = WamType.INTEGER)
    OptionalLong dtmfEventSentFailed();

    @WamProperty(index = 914, type = WamType.INTEGER)
    OptionalLong dtxRxByteFrameCount();

    @WamProperty(index = 912, type = WamType.INTEGER)
    OptionalLong dtxRxCount();

    @WamProperty(index = 911, type = WamType.TIMER)
    Optional<Instant> dtxRxDurationT();

    @WamProperty(index = 913, type = WamType.INTEGER)
    OptionalLong dtxRxTotalCount();

    @WamProperty(index = 1083, type = WamType.INTEGER)
    OptionalLong dtxRxTotalFrameCount();

    @WamProperty(index = 910, type = WamType.INTEGER)
    OptionalLong dtxTxByteFrameCount();

    @WamProperty(index = 619, type = WamType.INTEGER)
    OptionalLong dtxTxCount();

    @WamProperty(index = 618, type = WamType.TIMER)
    Optional<Instant> dtxTxDurationT();

    @WamProperty(index = 909, type = WamType.INTEGER)
    OptionalLong dtxTxTotalCount();

    @WamProperty(index = 1082, type = WamType.INTEGER)
    OptionalLong dtxTxTotalFrameCount();

    @WamProperty(index = 1882, type = WamType.BOOLEAN)
    Optional<Boolean> dualStackTransportEnabled();

    @WamProperty(index = 2190, type = WamType.TIMER)
    Optional<Instant> durationInMcp();

    @WamProperty(index = 2191, type = WamType.TIMER)
    Optional<Instant> durationInSru();

    @WamProperty(index = 1441, type = WamType.TIMER)
    Optional<Instant> durationTSs();

    @WamProperty(index = 1705, type = WamType.TIMER)
    Optional<Instant> durationTSsReceiver();

    @WamProperty(index = 1706, type = WamType.TIMER)
    Optional<Instant> durationTSsSharer();

    @WamProperty(index = 1815, type = WamType.INTEGER)
    OptionalLong dynamicBitrateCapFallbackTimes();

    @WamProperty(index = 2231, type = WamType.INTEGER)
    OptionalLong dynamicBitrateCapFirstFallbackTimeSinceStart();

    @WamProperty(index = 2232, type = WamType.INTEGER)
    OptionalLong dynamicBitrateCapLastFallbackTimeSinceStart();

    @WamProperty(index = 2088, type = WamType.INTEGER)
    OptionalLong dynamicInitBweFallbackCount();

    @WamProperty(index = 1611, type = WamType.INTEGER)
    OptionalLong dynamicTransportEventBitmap();

    @WamProperty(index = 1752, type = WamType.INTEGER)
    OptionalLong dynamicTransportFirstSwitchT();

    @WamProperty(index = 1753, type = WamType.INTEGER)
    OptionalLong dynamicTransportSwitchCnt();

    @WamProperty(index = 1682, type = WamType.INTEGER)
    OptionalLong dynamicTransportTransportSwitchCnt();

    @WamProperty(index = 2158, type = WamType.INTEGER)
    OptionalLong e2eeRetryCount();

    @WamProperty(index = 320, type = WamType.INTEGER)
    OptionalLong echoCancellationMsPerSec();

    @WamProperty(index = 1264, type = WamType.INTEGER)
    OptionalLong echoCancellationNumLoops();

    @WamProperty(index = 940, type = WamType.INTEGER)
    OptionalLong echoCancelledFrameCount();

    @WamProperty(index = 1701, type = WamType.INTEGER)
    OptionalLong echoConf2140();

    @WamProperty(index = 1702, type = WamType.INTEGER)
    OptionalLong echoConf4160();

    @WamProperty(index = 1703, type = WamType.INTEGER)
    OptionalLong echoConfGt60();

    @WamProperty(index = 1704, type = WamType.INTEGER)
    OptionalLong echoConfLt20();

    @WamProperty(index = 1589, type = WamType.INTEGER)
    OptionalLong echoConfidence();

    @WamProperty(index = 1989, type = WamType.INTEGER)
    OptionalLong echoConfidenceBeforeEc();

    @WamProperty(index = 1590, type = WamType.INTEGER)
    OptionalLong echoDelay();

    @WamProperty(index = 941, type = WamType.INTEGER)
    OptionalLong echoEstimatedFrameCount();

    @WamProperty(index = 1724, type = WamType.INTEGER)
    OptionalLong echoLikelihoodDiff();

    @WamProperty(index = 1591, type = WamType.INTEGER)
    OptionalLong echoLtDelay();

    @WamProperty(index = 1265, type = WamType.INTEGER)
    OptionalLong echoMaxConvergeFrameCount();

    @WamProperty(index = 1592, type = WamType.INTEGER)
    OptionalLong echoPercentage();

    @WamProperty(index = 1387, type = WamType.INTEGER)
    OptionalLong echoProbGte40FrmCnt();

    @WamProperty(index = 1388, type = WamType.INTEGER)
    OptionalLong echoProbGte50FrmCnt();

    @WamProperty(index = 1389, type = WamType.INTEGER)
    OptionalLong echoProbGte60FrmCnt();

    @WamProperty(index = 1593, type = WamType.INTEGER)
    OptionalLong echoReturnLoss();

    @WamProperty(index = 987, type = WamType.INTEGER)
    OptionalLong echoSpeakerModeFrameCount();

    @WamProperty(index = 1779, type = WamType.INTEGER)
    OptionalLong electedRelayIdx();

    @WamProperty(index = 2773, type = WamType.FLOAT)
    OptionalDouble encAspectRatioHqSs();

    @WamProperty(index = 2774, type = WamType.FLOAT)
    OptionalDouble encAspectRatioSs();

    @WamProperty(index = 2775, type = WamType.FLOAT)
    OptionalDouble encDownscaleRatioAvgHqSs();

    @WamProperty(index = 2776, type = WamType.FLOAT)
    OptionalDouble encDownscaleRatioAvgSs();

    @WamProperty(index = 1859, type = WamType.TIMER)
    Optional<Instant> encVidStreamActiveTime();

    @WamProperty(index = 1860, type = WamType.TIMER)
    Optional<Instant> encVidStreamActiveTimeHq();

    @WamProperty(index = 81, type = WamType.INTEGER)
    OptionalLong encoderCompStepdowns();

    @WamProperty(index = 90, type = WamType.ENUM)
    Optional<EndCallConfirmationType> endCallAfterConfirmation();

    @WamProperty(index = 2598, type = WamType.FLOAT)
    OptionalDouble enhancedFrameBrightnessAvg();

    @WamProperty(index = 2222, type = WamType.BOOLEAN)
    Optional<Boolean> enterPipBeforeInflectionPoint();

    @WamProperty(index = 2402, type = WamType.INTEGER)
    OptionalLong evQueueOverflowCount();

    @WamProperty(index = 3038, type = WamType.INTEGER)
    OptionalLong extensionType();

    @WamProperty(index = 3039, type = WamType.INTEGER)
    OptionalLong extensionTypeBitmask();

    @WamProperty(index = 3040, type = WamType.STRING)
    Optional<String> extensionUserRid();

    @WamProperty(index = 534, type = WamType.INTEGER)
    OptionalLong failureToCreateAltSocket();

    @WamProperty(index = 532, type = WamType.INTEGER)
    OptionalLong failureToCreateTestAltSocket();

    @WamProperty(index = 2610, type = WamType.INTEGER)
    OptionalLong fgServiceTypesBitmap();

    @WamProperty(index = 328, type = WamType.ENUM)
    Optional<FieldStatsRowType> fieldStatsRowType();

    @WamProperty(index = 503, type = WamType.BOOLEAN)
    Optional<Boolean> finishedDlBwe();

    @WamProperty(index = 528, type = WamType.BOOLEAN)
    Optional<Boolean> finishedOverallBwe();

    @WamProperty(index = 502, type = WamType.BOOLEAN)
    Optional<Boolean> finishedUlBwe();

    @WamProperty(index = 2592, type = WamType.STRING)
    Optional<String> firstAssertFunc();

    @WamProperty(index = 2054, type = WamType.INTEGER)
    OptionalLong firstNetworkMedium();

    @WamProperty(index = 2067, type = WamType.ENUM)
    Optional<DeliveredPriority> firstOfferPushDeliveredPriority();

    @WamProperty(index = 2047, type = WamType.TIMER)
    Optional<Instant> firstOfferPushReceivedSinceCallInitiationMs();

    @WamProperty(index = 2048, type = WamType.TIMER)
    Optional<Instant> firstOfferPushSentByProviderSinceCallInitiationMs();

    @WamProperty(index = 2049, type = WamType.TIMER)
    Optional<Instant> firstOfferPushSentByPushdSinceCallInitiationMs();

    @WamProperty(index = 3009, type = WamType.INTEGER)
    OptionalLong foaSourceSurface();

    @WamProperty(index = 2529, type = WamType.INTEGER)
    OptionalLong fpp1();

    @WamProperty(index = 2530, type = WamType.INTEGER)
    OptionalLong fpp2();

    @WamProperty(index = 2531, type = WamType.INTEGER)
    OptionalLong fpp3();

    @WamProperty(index = 2532, type = WamType.INTEGER)
    OptionalLong fpp4();

    @WamProperty(index = 2533, type = WamType.INTEGER)
    OptionalLong fpp5();

    @WamProperty(index = 2534, type = WamType.INTEGER)
    OptionalLong fpp6();

    @WamProperty(index = 2535, type = WamType.FLOAT)
    OptionalDouble fppAvg();

    @WamProperty(index = 2599, type = WamType.FLOAT)
    OptionalDouble frameBrightnessAvg();

    @WamProperty(index = 2536, type = WamType.INTEGER)
    OptionalLong frameLengthMs();

    @WamProperty(index = 1051, type = WamType.FLOAT)
    OptionalDouble freezeAheadBweCongestionCorrPct();

    @WamProperty(index = 1009, type = WamType.FLOAT)
    OptionalDouble freezeBweCongestionCorrPct();

    @WamProperty(index = 2604, type = WamType.TIMER)
    Optional<Instant> freezeDisableDurationAppBackground();

    @WamProperty(index = 2605, type = WamType.TIMER)
    Optional<Instant> freezeDisableDurationAppUnsubscribe();

    @WamProperty(index = 2606, type = WamType.TIMER)
    Optional<Instant> freezeDisableDurationPeerPaused();

    @WamProperty(index = 2607, type = WamType.TIMER)
    Optional<Instant> freezeDisableTotalDuration();

    @WamProperty(index = 2055, type = WamType.INTEGER)
    OptionalLong gapRecoveredByP2pFallback();

    @WamProperty(index = 2056, type = WamType.INTEGER)
    OptionalLong gapRecoveredByRebind();

    @WamProperty(index = 2057, type = WamType.INTEGER)
    OptionalLong gapRecoveredByRelayFailover();

    @WamProperty(index = 2058, type = WamType.INTEGER)
    OptionalLong gapRecoveredByWeakWifi();

    @WamProperty(index = 1915, type = WamType.FLOAT)
    OptionalDouble gcBadStatusDuringVideoDisabling();

    @WamProperty(index = 2036, type = WamType.ENUM)
    Optional<GcInitiationType> gcInitiationType();

    @WamProperty(index = 2254, type = WamType.ENUM)
    Optional<CallResultType> gcPreviousSegmentCallResult();

    @WamProperty(index = 2074, type = WamType.ENUM)
    Optional<GcRekeyMasterError> gcRekeyMasterError();

    @WamProperty(index = 2032, type = WamType.INTEGER)
    OptionalLong gcUpgradeAttempts();

    @WamProperty(index = 2033, type = WamType.TIMER)
    Optional<Instant> gcUpgradeOfferAckLatencyMs();

    @WamProperty(index = 2034, type = WamType.INTEGER)
    OptionalLong gcUpgradeOfferErrorCode();

    @WamProperty(index = 2035, type = WamType.INTEGER)
    OptionalLong gcUpgradeOfferParticipantCount();

    @WamProperty(index = 2830, type = WamType.TIMER)
    Optional<Instant> genaiActivityCreateToFirstDrawMs();

    @WamProperty(index = 2426, type = WamType.TIMER)
    Optional<Instant> genaiBotEarlyConnectVoipLatencyMs();

    @WamProperty(index = 2909, type = WamType.TIMER)
    Optional<Instant> genaiBotSpeechDurationAvgMs();

    @WamProperty(index = 2910, type = WamType.TIMER)
    Optional<Instant> genaiBotSpeechDurationP90Ms();

    @WamProperty(index = 2911, type = WamType.TIMER)
    Optional<Instant> genaiBotSpeechDurationTotalMs();

    @WamProperty(index = 2385, type = WamType.ENUM)
    Optional<GenaiBotType> genaiBotType();

    @WamProperty(index = 2388, type = WamType.INTEGER)
    OptionalLong genaiBufferedActiveSpeechPct();

    @WamProperty(index = 2370, type = WamType.TIMER)
    Optional<Instant> genaiConnectionReadyLatency();

    @WamProperty(index = 2080, type = WamType.ENUM)
    Optional<GenaiEntryPoint> genaiEntryPoint();

    @WamProperty(index = 2081, type = WamType.ENUM)
    Optional<GenaiExitPoint> genaiExitPoint();

    @WamProperty(index = 2913, type = WamType.INTEGER)
    OptionalLong genaiGroupCallBotInviteFailures();

    @WamProperty(index = 2914, type = WamType.BOOLEAN)
    Optional<Boolean> genaiGroupCallBotInviter();

    @WamProperty(index = 2901, type = WamType.TIMER)
    Optional<Instant> genaiGroupCallBotJoinLatency();

    @WamProperty(index = 2915, type = WamType.INTEGER)
    OptionalLong genaiGroupCallBotRemoveFailures();

    @WamProperty(index = 2916, type = WamType.BOOLEAN)
    Optional<Boolean> genaiGroupCallBotRemover();

    @WamProperty(index = 2389, type = WamType.INTEGER)
    OptionalLong genaiInitialAudioBufferedMs();

    @WamProperty(index = 2037, type = WamType.TIMER)
    Optional<Instant> genaiInitialConnectionLatencyMs();

    @WamProperty(index = 2149, type = WamType.TIMER)
    Optional<Instant> genaiInitialTranscriptionLatencyMs();

    @WamProperty(index = 2424, type = WamType.TIMER)
    Optional<Instant> genaiInterruptDuckingLatencyMs();

    @WamProperty(index = 2902, type = WamType.BOOLEAN)
    Optional<Boolean> genaiIsGroupCall();

    @WamProperty(index = 2831, type = WamType.TIMER)
    Optional<Instant> genaiNativePreOfferLatencyMs();

    @WamProperty(index = 2082, type = WamType.INTEGER)
    OptionalLong genaiNumRequestsSent();

    @WamProperty(index = 2083, type = WamType.INTEGER)
    OptionalLong genaiNumResponseImages();

    @WamProperty(index = 2084, type = WamType.INTEGER)
    OptionalLong genaiNumResponseReels();

    @WamProperty(index = 2085, type = WamType.INTEGER)
    OptionalLong genaiNumResponseSearchResults();

    @WamProperty(index = 2094, type = WamType.INTEGER)
    OptionalLong genaiNumResponseTextResults();

    @WamProperty(index = 2086, type = WamType.INTEGER)
    OptionalLong genaiNumResponsesReceived();

    @WamProperty(index = 2912, type = WamType.INTEGER)
    OptionalLong genaiNumUserInterrupts();

    @WamProperty(index = 2832, type = WamType.TIMER)
    Optional<Instant> genaiPlatformToNativeCrossingMs();

    @WamProperty(index = 2833, type = WamType.TIMER)
    Optional<Instant> genaiPreNativePlatformLatencyMs();

    @WamProperty(index = 2087, type = WamType.BOOLEAN)
    Optional<Boolean> genaiResponseFullSheet();

    @WamProperty(index = 2100, type = WamType.TIMER)
    Optional<Instant> genaiResponseLatencyAvgMs();

    @WamProperty(index = 2101, type = WamType.TIMER)
    Optional<Instant> genaiResponseLatencyP50Ms();

    @WamProperty(index = 2102, type = WamType.TIMER)
    Optional<Instant> genaiResponseLatencyP90Ms();

    @WamProperty(index = 2834, type = WamType.TIMER)
    Optional<Instant> genaiUiAnimationDurationMs();

    @WamProperty(index = 2427, type = WamType.TIMER)
    Optional<Instant> genaiUiPresentedLatencyMs();

    @WamProperty(index = 2425, type = WamType.TIMER)
    Optional<Instant> genaiUserPerceivedInterruptLatencyMs();

    @WamProperty(index = 2139, type = WamType.STRING)
    Optional<String> genaiVoiceSelection();

    @WamProperty(index = 2835, type = WamType.TIMER)
    Optional<Instant> genaiVoipToUiListeningMs();

    @WamProperty(index = 2322, type = WamType.INTEGER)
    OptionalLong goodputPeerDownlink();

    @WamProperty(index = 1967, type = WamType.ENUM)
    Optional<GooglePlayServicesStatus> googlePlayServicesStatus();

    @WamProperty(index = 1822, type = WamType.FLOAT)
    OptionalDouble gpuUtilizationAvg();

    @WamProperty(index = 1823, type = WamType.FLOAT)
    OptionalDouble gpuUtilizationPeak();

    @WamProperty(index = 1529, type = WamType.INTEGER)
    OptionalLong greaterThanLowPlrIsRandomCount();

    @WamProperty(index = 1013, type = WamType.BOOLEAN)
    Optional<Boolean> groupAcceptNoCriticalGroupUpdate();

    @WamProperty(index = 1014, type = WamType.TIMER)
    Optional<Instant> groupAcceptToCriticalGroupUpdateMs();

    @WamProperty(index = 2078, type = WamType.INTEGER)
    OptionalLong groupCallAncFixPeerCount();

    @WamProperty(index = 2079, type = WamType.INTEGER)
    OptionalLong groupCallAncFixSelfCount();

    @WamProperty(index = 439, type = WamType.INTEGER)
    OptionalLong groupCallCallerParticipantCountAtCallStart();

    @WamProperty(index = 1673, type = WamType.INTEGER)
    OptionalLong groupCallInviteCountBeforeConnected();

    @WamProperty(index = 360, type = WamType.INTEGER)
    OptionalLong groupCallInviteCountSinceCallStart();

    @WamProperty(index = 1578, type = WamType.BOOLEAN)
    Optional<Boolean> groupCallIsFirstSegment();

    @WamProperty(index = 357, type = WamType.BOOLEAN)
    Optional<Boolean> groupCallIsGroupCallInvitee();

    @WamProperty(index = 356, type = WamType.BOOLEAN)
    Optional<Boolean> groupCallIsLastSegment();

    @WamProperty(index = 1861, type = WamType.INTEGER)
    OptionalLong groupCallMaximizedPeerCount();

    @WamProperty(index = 361, type = WamType.INTEGER)
    OptionalLong groupCallNackCountSinceCallStart();

    @WamProperty(index = 946, type = WamType.INTEGER)
    OptionalLong groupCallReringCountSinceCallStart();

    @WamProperty(index = 947, type = WamType.INTEGER)
    OptionalLong groupCallReringNackCountSinceCallStart();

    @WamProperty(index = 329, type = WamType.INTEGER)
    OptionalLong groupCallSegmentIdx();

    @WamProperty(index = 358, type = WamType.TIMER)
    Optional<Instant> groupCallTotalCallTSinceCallStart();

    @WamProperty(index = 359, type = WamType.TIMER)
    Optional<Instant> groupCallTotalP3CallTSinceCallStart();

    @WamProperty(index = 592, type = WamType.INTEGER)
    OptionalLong groupCallVideoMaximizedCount();

    @WamProperty(index = 1617, type = WamType.TIMER)
    Optional<Instant> groupCallVideoMaximizedDuration();

    @WamProperty(index = 1862, type = WamType.TIMER)
    Optional<Instant> groupCallVideoSelfMaximizedDuration();

    @WamProperty(index = 2410, type = WamType.STRING)
    Optional<String> hardwareStateAtEnd();

    @WamProperty(index = 2411, type = WamType.STRING)
    Optional<String> hardwareStateAtStart();

    @WamProperty(index = 1978, type = WamType.BOOLEAN)
    Optional<Boolean> hasFbnsPushToken();

    @WamProperty(index = 1979, type = WamType.BOOLEAN)
    Optional<Boolean> hasFcmPushToken();

    @WamProperty(index = 2566, type = WamType.BOOLEAN)
    Optional<Boolean> hasGrapevine();

    @WamProperty(index = 539, type = WamType.BOOLEAN)
    Optional<Boolean> hasRestrictedSettingsForAudioCalls();

    @WamProperty(index = 2071, type = WamType.BOOLEAN)
    Optional<Boolean> hasWorkingDualStackP2p();

    @WamProperty(index = 1427, type = WamType.INTEGER)
    OptionalLong hbhKeyInconsistencyCnt();

    @WamProperty(index = 2459, type = WamType.INTEGER)
    OptionalLong hbhSmlPacketCount();

    @WamProperty(index = 1256, type = WamType.INTEGER)
    OptionalLong hbhSrtcpRxBytes();

    @WamProperty(index = 1257, type = WamType.INTEGER)
    OptionalLong hbhSrtcpRxRejAuthFail();

    @WamProperty(index = 1258, type = WamType.INTEGER)
    OptionalLong hbhSrtcpRxRejEinval();

    @WamProperty(index = 1219, type = WamType.INTEGER)
    OptionalLong hbhSrtcpRxRejectedPktCntFromOldRelay();

    @WamProperty(index = 1248, type = WamType.INTEGER)
    OptionalLong hbhSrtcpRxSuccessNackPktCnt();

    @WamProperty(index = 1646, type = WamType.INTEGER)
    OptionalLong hbhSrtcpRxSuccessPliPktCnt();

    @WamProperty(index = 1249, type = WamType.INTEGER)
    OptionalLong hbhSrtcpRxSuccessRembPktCnt();

    @WamProperty(index = 2646, type = WamType.INTEGER)
    OptionalLong hbhSrtcpRxSuccessRxsbPktCnt();

    @WamProperty(index = 1250, type = WamType.INTEGER)
    OptionalLong hbhSrtcpRxSuccessSbwaPktCnt();

    @WamProperty(index = 1251, type = WamType.INTEGER)
    OptionalLong hbhSrtcpRxSuccessSpPktCnt();

    @WamProperty(index = 1904, type = WamType.INTEGER)
    OptionalLong hbhSrtcpRxSuccessSrtpAfbBatchPktCnt();

    @WamProperty(index = 1685, type = WamType.INTEGER)
    OptionalLong hbhSrtcpRxSuccessSrtpAfbPktCnt();

    @WamProperty(index = 1259, type = WamType.INTEGER)
    OptionalLong hbhSrtcpTxBytes();

    @WamProperty(index = 1254, type = WamType.INTEGER)
    OptionalLong hbhSrtcpTxNackPktCnt();

    @WamProperty(index = 1905, type = WamType.INTEGER)
    OptionalLong hbhSrtcpTxSrtpAfbBatchPktCnt();

    @WamProperty(index = 1686, type = WamType.INTEGER)
    OptionalLong hbhSrtcpTxSrtpAfbPktCnt();

    @WamProperty(index = 2126, type = WamType.INTEGER)
    OptionalLong hbhSrtcpTxWarpTfPktCnt();

    @WamProperty(index = 1849, type = WamType.INTEGER)
    OptionalLong hbhSrtpRxE2eEncCnt();

    @WamProperty(index = 1850, type = WamType.INTEGER)
    OptionalLong hbhSrtpRxE2eEncErrCnt();

    @WamProperty(index = 1585, type = WamType.INTEGER)
    OptionalLong hbhSrtpRxPktCnt();

    @WamProperty(index = 1586, type = WamType.INTEGER)
    OptionalLong hbhSrtpRxRejAuthFail();

    @WamProperty(index = 1587, type = WamType.INTEGER)
    OptionalLong hbhSrtpRxRejEinval();

    @WamProperty(index = 1907, type = WamType.INTEGER)
    OptionalLong hbhSrtpRxWarpRocCnt();

    @WamProperty(index = 1588, type = WamType.INTEGER)
    OptionalLong hbhSrtpTxPktCnt();

    @WamProperty(index = 1851, type = WamType.INTEGER)
    OptionalLong hbhSrtpTxPktErrorCnt();

    @WamProperty(index = 1279, type = WamType.INTEGER)
    OptionalLong hbweHistoryBasedAvgVideoTxBitrate();

    @WamProperty(index = 1280, type = WamType.BOOLEAN)
    Optional<Boolean> hbweHistoryBasedBweInstantRampUpDone();

    @WamProperty(index = 1281, type = WamType.BOOLEAN)
    Optional<Boolean> hbweHistoryBasedBweUpdateCeilingDone();

    @WamProperty(index = 1282, type = WamType.BOOLEAN)
    Optional<Boolean> hbweHistoryBasedBweUpdateCeilingForced();

    @WamProperty(index = 884, type = WamType.TIMER)
    Optional<Instant> highPeerBweT();

    @WamProperty(index = 342, type = WamType.INTEGER)
    OptionalLong hisBasedInitialTxBitrate();

    @WamProperty(index = 339, type = WamType.BOOLEAN)
    Optional<Boolean> hisInfoCouldBeUsedForInitBwe();

    @WamProperty(index = 807, type = WamType.BOOLEAN)
    Optional<Boolean> historyBasedBweActivated();

    @WamProperty(index = 806, type = WamType.BOOLEAN)
    Optional<Boolean> historyBasedBweEnabled();

    @WamProperty(index = 808, type = WamType.BOOLEAN)
    Optional<Boolean> historyBasedBweSuccess();

    @WamProperty(index = 809, type = WamType.INTEGER)
    OptionalLong historyBasedBweVideoTxBitrate();

    @WamProperty(index = 1431, type = WamType.BOOLEAN)
    Optional<Boolean> historyBasedMinRttAvailable();

    @WamProperty(index = 1432, type = WamType.INTEGER)
    OptionalLong historyBasedMinRttCongestionCount();

    @WamProperty(index = 1433, type = WamType.FLOAT)
    OptionalDouble historyBasedMinRttDividedByRuntimeMinRtt();

    @WamProperty(index = 1971, type = WamType.INTEGER)
    OptionalLong historyRansportSelectionSockAddrFamily();

    @WamProperty(index = 1972, type = WamType.INTEGER)
    OptionalLong historyTransportSelectionMatchedRecordCount();

    @WamProperty(index = 2323, type = WamType.BOOLEAN)
    Optional<Boolean> historyVideoRecordBySelfAndPeerIpMatching();

    @WamProperty(index = 2324, type = WamType.BOOLEAN)
    Optional<Boolean> historyVideoRecordBySelfOnlyIpMatching();

    @WamProperty(index = 2290, type = WamType.INTEGER)
    OptionalLong historyVideoRecordEncoderAvgQp();

    @WamProperty(index = 2291, type = WamType.INTEGER)
    OptionalLong historyVideoRecordEncoderLatency();

    @WamProperty(index = 2292, type = WamType.INTEGER)
    OptionalLong historyVideoRecordEncoderOvershoot();

    @WamProperty(index = 2293, type = WamType.INTEGER)
    OptionalLong historyVideoRecordEncoderUndershoot();

    @WamProperty(index = 2294, type = WamType.INTEGER)
    OptionalLong historyVideoRecordFreezePct();

    @WamProperty(index = 2295, type = WamType.TIMER)
    Optional<Instant> historyVideoRecordGood480pDecoding();

    @WamProperty(index = 2296, type = WamType.TIMER)
    Optional<Instant> historyVideoRecordGood480pEncoding();

    @WamProperty(index = 2297, type = WamType.TIMER)
    Optional<Instant> historyVideoRecordGood720pDecoding();

    @WamProperty(index = 2298, type = WamType.TIMER)
    Optional<Instant> historyVideoRecordGood720pEncoding();

    @WamProperty(index = 2299, type = WamType.INTEGER)
    OptionalLong historyVideoRecordInitDownlinkBwe();

    @WamProperty(index = 2300, type = WamType.INTEGER)
    OptionalLong historyVideoRecordInitUplinkBwe();

    @WamProperty(index = 2301, type = WamType.INTEGER)
    OptionalLong historyVideoRecordStableMaxTargetBitrate();

    @WamProperty(index = 2302, type = WamType.INTEGER)
    OptionalLong historyVideoRecordTxPktLossPct();

    @WamProperty(index = 1951, type = WamType.INTEGER)
    OptionalLong hscrollInteractCount();

    @WamProperty(index = 2377, type = WamType.INTEGER)
    OptionalLong hwDecReach1sCount();

    @WamProperty(index = 2378, type = WamType.INTEGER)
    OptionalLong hwDecReach5sCount();

    @WamProperty(index = 2379, type = WamType.INTEGER)
    OptionalLong hwEncReach1sCount();

    @WamProperty(index = 2380, type = WamType.INTEGER)
    OptionalLong hwEncReach5sCount();

    @WamProperty(index = 2684, type = WamType.TIMER)
    Optional<Instant> iceRttAvg();

    @WamProperty(index = 2685, type = WamType.TIMER)
    Optional<Instant> iceRttMax();

    @WamProperty(index = 2686, type = WamType.TIMER)
    Optional<Instant> iceRttMin();

    @WamProperty(index = 1944, type = WamType.INTEGER)
    OptionalLong igluEffectAttemptedCount();

    @WamProperty(index = 1945, type = WamType.INTEGER)
    OptionalLong igluEffectCanceledCount();

    @WamProperty(index = 1946, type = WamType.TIMER)
    Optional<Instant> igluEffectDurationT();

    @WamProperty(index = 1947, type = WamType.INTEGER)
    OptionalLong igluEffectEnabledCount();

    @WamProperty(index = 1948, type = WamType.INTEGER)
    OptionalLong igluEffectFailedCount();

    @WamProperty(index = 1949, type = WamType.TIMER)
    Optional<Instant> igluEffectLoadingT();

    @WamProperty(index = 1350, type = WamType.FLOAT)
    OptionalDouble imbalancedDlPlrTPct();

    @WamProperty(index = 3041, type = WamType.INTEGER)
    OptionalLong imuTxBitrate();

    @WamProperty(index = 3042, type = WamType.INTEGER)
    OptionalLong imuTxDroppedCount();

    @WamProperty(index = 3043, type = WamType.INTEGER)
    OptionalLong imuTxFrameCount();

    @WamProperty(index = 1728, type = WamType.TIMER)
    Optional<Instant> inboundVideoDisablingDuration();

    @WamProperty(index = 1914, type = WamType.ENUM)
    Optional<IncomingCallNotificationStateType> incomingCallNotificationState();

    @WamProperty(index = 387, type = WamType.ENUM)
    Optional<IncomingCallUiActionType> incomingCallUiAction();

    @WamProperty(index = 337, type = WamType.ENUM)
    Optional<InitBweSource> initBweSource();

    @WamProperty(index = 1520, type = WamType.TIMER)
    Optional<Instant> initialAudioRenderDelayT();

    @WamProperty(index = 2502, type = WamType.TIMER)
    Optional<Instant> initialAutoDisabledPeerCameraPauseT();

    @WamProperty(index = 244, type = WamType.FLOAT)
    OptionalDouble initialEstimatedTxBitrate();

    @WamProperty(index = 1683, type = WamType.INTEGER)
    OptionalLong invalidDataPacketCnt();

    @WamProperty(index = 1575, type = WamType.INTEGER)
    OptionalLong invalidRelayMessageCnt();

    @WamProperty(index = 1770, type = WamType.INTEGER)
    OptionalLong iosHwLtrAckMiss();

    @WamProperty(index = 2611, type = WamType.BOOLEAN)
    Optional<Boolean> isAppInBackgroundAtCallEnd();

    @WamProperty(index = 2612, type = WamType.BOOLEAN)
    Optional<Boolean> isAppInBgWhenCallStarts();

    @WamProperty(index = 2059, type = WamType.BOOLEAN)
    Optional<Boolean> isBridgedIpv6();

    @WamProperty(index = 2613, type = WamType.BOOLEAN)
    Optional<Boolean> isCallAnsweredWithScreenLocked();

    @WamProperty(index = 1323, type = WamType.BOOLEAN)
    Optional<Boolean> isCallCreator();

    @WamProperty(index = 1149, type = WamType.BOOLEAN)
    Optional<Boolean> isCallFull();

    @WamProperty(index = 2643, type = WamType.BOOLEAN)
    Optional<Boolean> isDeviceSwitch();

    @WamProperty(index = 1928, type = WamType.BOOLEAN)
    Optional<Boolean> isEventsLink();

    @WamProperty(index = 1316, type = WamType.BOOLEAN)
    Optional<Boolean> isFromCallLink();

    @WamProperty(index = 2075, type = WamType.BOOLEAN)
    Optional<Boolean> isGcRekeyMaster();

    @WamProperty(index = 1921, type = WamType.BOOLEAN)
    Optional<Boolean> isInSymNat();

    @WamProperty(index = 2072, type = WamType.BOOLEAN)
    Optional<Boolean> isIpv6BehindNat();

    @WamProperty(index = 91, type = WamType.BOOLEAN)
    Optional<Boolean> isIpv6Capable();

    @WamProperty(index = 2873, type = WamType.BOOLEAN)
    Optional<Boolean> isLgcAdd();

    @WamProperty(index = 1605, type = WamType.BOOLEAN)
    Optional<Boolean> isLidCall();

    @WamProperty(index = 1372, type = WamType.BOOLEAN)
    Optional<Boolean> isLinkCreator();

    @WamProperty(index = 1335, type = WamType.BOOLEAN)
    Optional<Boolean> isLinkJoin();

    @WamProperty(index = 1090, type = WamType.BOOLEAN)
    Optional<Boolean> isLinkedGroupCall();

    @WamProperty(index = 1579, type = WamType.BOOLEAN)
    Optional<Boolean> isMutedDuringCall();

    @WamProperty(index = 1227, type = WamType.BOOLEAN)
    Optional<Boolean> isOsMicrophoneMute();

    @WamProperty(index = 976, type = WamType.BOOLEAN)
    Optional<Boolean> isPendingCall();

    @WamProperty(index = 1672, type = WamType.BOOLEAN)
    Optional<Boolean> isPhashBased();

    @WamProperty(index = 1774, type = WamType.BOOLEAN)
    Optional<Boolean> isPhashMismatch();

    @WamProperty(index = 927, type = WamType.BOOLEAN)
    Optional<Boolean> isRejoin();

    @WamProperty(index = 945, type = WamType.BOOLEAN)
    Optional<Boolean> isRering();

    @WamProperty(index = 1488, type = WamType.BOOLEAN)
    Optional<Boolean> isScheduledCall();

    @WamProperty(index = 2614, type = WamType.BOOLEAN)
    Optional<Boolean> isTelecomFallbackPath();

    @WamProperty(index = 3006, type = WamType.BOOLEAN)
    Optional<Boolean> isTransferRejoin();

    @WamProperty(index = 2371, type = WamType.BOOLEAN)
    Optional<Boolean> isUgcCall();

    @WamProperty(index = 1674, type = WamType.BOOLEAN)
    Optional<Boolean> isUpgradedGroupCallBeforeConnected();

    @WamProperty(index = 1577, type = WamType.BOOLEAN)
    Optional<Boolean> isVoiceChat();

    @WamProperty(index = 2632, type = WamType.BOOLEAN)
    Optional<Boolean> isWaitingRoomEnabled();

    @WamProperty(index = 146, type = WamType.FLOAT)
    OptionalDouble jbAvgDelay();

    @WamProperty(index = 1413, type = WamType.FLOAT)
    OptionalDouble jbAvgDelayFromDisorderDistanceHist();

    @WamProperty(index = 1414, type = WamType.FLOAT)
    OptionalDouble jbAvgDelayFromPutHist();

    @WamProperty(index = 644, type = WamType.FLOAT)
    OptionalDouble jbAvgDelayUniform();

    @WamProperty(index = 1086, type = WamType.FLOAT)
    OptionalDouble jbAvgDisorderTargetSize();

    @WamProperty(index = 1415, type = WamType.FLOAT)
    OptionalDouble jbAvgPutHistTargetSize();

    @WamProperty(index = 1012, type = WamType.FLOAT)
    OptionalDouble jbAvgTargetSize();

    @WamProperty(index = 1416, type = WamType.FLOAT)
    OptionalDouble jbAvgTargetSizeAddedFromDisorderDistanceHist();

    @WamProperty(index = 1417, type = WamType.FLOAT)
    OptionalDouble jbAvgTargetSizeFromDisorderDistanceHist();

    @WamProperty(index = 1418, type = WamType.FLOAT)
    OptionalDouble jbAvgTargetSizeFromPutHist();

    @WamProperty(index = 1718, type = WamType.FLOAT)
    OptionalDouble jbCng();

    @WamProperty(index = 150, type = WamType.FLOAT)
    OptionalDouble jbDiscards();

    @WamProperty(index = 151, type = WamType.FLOAT)
    OptionalDouble jbEmpties();

    @WamProperty(index = 997, type = WamType.FLOAT)
    OptionalDouble jbEmptyPeriods1x();

    @WamProperty(index = 998, type = WamType.FLOAT)
    OptionalDouble jbEmptyPeriods2x();

    @WamProperty(index = 999, type = WamType.FLOAT)
    OptionalDouble jbEmptyPeriods4x();

    @WamProperty(index = 1000, type = WamType.FLOAT)
    OptionalDouble jbEmptyPeriods8x();

    @WamProperty(index = 1419, type = WamType.FLOAT)
    OptionalDouble jbGetFromDisorderDistanceHist();

    @WamProperty(index = 1420, type = WamType.FLOAT)
    OptionalDouble jbGetFromPutHist();

    @WamProperty(index = 152, type = WamType.FLOAT)
    OptionalDouble jbGets();

    @WamProperty(index = 2217, type = WamType.INTEGER)
    OptionalLong jbGetsDominantSpeaker();

    @WamProperty(index = 149, type = WamType.FLOAT)
    OptionalDouble jbLastDelay();

    @WamProperty(index = 2372, type = WamType.INTEGER)
    OptionalLong jbLastTotalPlcMs();

    @WamProperty(index = 277, type = WamType.FLOAT)
    OptionalDouble jbLost();

    @WamProperty(index = 777, type = WamType.FLOAT)
    OptionalDouble jbLostEmptyHighPeerBwePerSec();

    @WamProperty(index = 775, type = WamType.FLOAT)
    OptionalDouble jbLostEmptyLowPeerBwePerSec();

    @WamProperty(index = 776, type = WamType.FLOAT)
    OptionalDouble jbLostEmptyLowToHighPeerBwePerSec();

    @WamProperty(index = 148, type = WamType.FLOAT)
    OptionalDouble jbMaxDelay();

    @WamProperty(index = 1421, type = WamType.FLOAT)
    OptionalDouble jbMaxDelayFromDisorderDistanceHist();

    @WamProperty(index = 1422, type = WamType.FLOAT)
    OptionalDouble jbMaxDelayFromPutHist();

    @WamProperty(index = 1087, type = WamType.FLOAT)
    OptionalDouble jbMaxDisorderTargetSize();

    @WamProperty(index = 1423, type = WamType.FLOAT)
    OptionalDouble jbMaxPutHistTargetSize();

    @WamProperty(index = 1424, type = WamType.FLOAT)
    OptionalDouble jbMaxTargetSizeAddedFromDisorderDistanceHist();

    @WamProperty(index = 1425, type = WamType.FLOAT)
    OptionalDouble jbMaxTargetSizeFromDisorderDistanceHist();

    @WamProperty(index = 1426, type = WamType.FLOAT)
    OptionalDouble jbMaxTargetSizeFromPutHist();

    @WamProperty(index = 1656, type = WamType.FLOAT)
    OptionalDouble jbMeanWaitTime();

    @WamProperty(index = 2218, type = WamType.FLOAT)
    OptionalDouble jbMeanWaitTimeDominantSpeaker();

    @WamProperty(index = 147, type = WamType.FLOAT)
    OptionalDouble jbMinDelay();

    @WamProperty(index = 846, type = WamType.FLOAT)
    OptionalDouble jbNonSpeechDiscards();

    @WamProperty(index = 1719, type = WamType.FLOAT)
    OptionalDouble jbPlc();

    @WamProperty(index = 1720, type = WamType.FLOAT)
    OptionalDouble jbPlcCng();

    @WamProperty(index = 2478, type = WamType.FLOAT)
    OptionalDouble jbPlcCngBeforeFirstDecode();

    @WamProperty(index = 2479, type = WamType.BOOLEAN)
    Optional<Boolean> jbPlcCngBeforeFirstDecodeInclude();

    @WamProperty(index = 2219, type = WamType.INTEGER)
    OptionalLong jbPlcCngDominantSpeaker();

    @WamProperty(index = 2220, type = WamType.INTEGER)
    OptionalLong jbPlcDominantSpeaker();

    @WamProperty(index = 153, type = WamType.FLOAT)
    OptionalDouble jbPuts();

    @WamProperty(index = 2221, type = WamType.INTEGER)
    OptionalLong jbPutsDominantSpeaker();

    @WamProperty(index = 996, type = WamType.FLOAT)
    OptionalDouble jbTotalEmptyPeriods();

    @WamProperty(index = 2373, type = WamType.INTEGER)
    OptionalLong jbTotalPlc1xMs();

    @WamProperty(index = 2374, type = WamType.INTEGER)
    OptionalLong jbTotalPlc2xMs();

    @WamProperty(index = 2375, type = WamType.INTEGER)
    OptionalLong jbTotalPlc4xMs();

    @WamProperty(index = 2376, type = WamType.INTEGER)
    OptionalLong jbTotalPlc8xMs();

    @WamProperty(index = 1081, type = WamType.INTEGER)
    OptionalLong jbVoiceFrames();

    @WamProperty(index = 894, type = WamType.BOOLEAN)
    Optional<Boolean> joinableDuringCall();

    @WamProperty(index = 893, type = WamType.BOOLEAN)
    Optional<Boolean> joinableNewUi();

    @WamProperty(index = 2718, type = WamType.INTEGER)
    OptionalLong jsHaltCount();

    @WamProperty(index = 2719, type = WamType.TIMER)
    Optional<Instant> jsHaltTotalMsT();

    @WamProperty(index = 2349, type = WamType.INTEGER)
    OptionalLong knownContactVideoUpgradeCount();

    @WamProperty(index = 986, type = WamType.STRING)
    Optional<String> l1Locations();

    @WamProperty(index = 1510, type = WamType.TIMER)
    Optional<Instant> landscapeModeDurationT();

    @WamProperty(index = 1516, type = WamType.INTEGER)
    OptionalLong landscapeModeEnabled();

    @WamProperty(index = 1511, type = WamType.TIMER)
    Optional<Instant> landscapeModeLockedDurationT();

    @WamProperty(index = 1512, type = WamType.INTEGER)
    OptionalLong landscapeModeLockedSwitchCount();

    @WamProperty(index = 1513, type = WamType.TIMER)
    Optional<Instant> landscapeModePipMixedDurationT();

    @WamProperty(index = 1514, type = WamType.INTEGER)
    OptionalLong landscapeModeSwitchCount();

    @WamProperty(index = 415, type = WamType.INTEGER)
    OptionalLong lastConnErrorStatus();

    @WamProperty(index = 2665, type = WamType.INTEGER)
    OptionalLong lastMicMode();

    @WamProperty(index = 1607, type = WamType.FLOAT)
    OptionalDouble lastMinJbAvgDelay();

    @WamProperty(index = 1608, type = WamType.FLOAT)
    OptionalDouble lastMinJbEmpties();

    @WamProperty(index = 1609, type = WamType.FLOAT)
    OptionalDouble lastMinJbGets();

    @WamProperty(index = 1610, type = WamType.FLOAT)
    OptionalDouble lastMinJbLost();

    @WamProperty(index = 1864, type = WamType.FLOAT)
    OptionalDouble lastMinJbMeanWaitTime();

    @WamProperty(index = 1865, type = WamType.INTEGER)
    OptionalLong lastMinJbPlc();

    @WamProperty(index = 1866, type = WamType.INTEGER)
    OptionalLong lastMinJbPlcCng();

    @WamProperty(index = 1619, type = WamType.TIMER)
    Optional<Instant> lastMinVideoRenderEnableDuration();

    @WamProperty(index = 1620, type = WamType.TIMER)
    Optional<Instant> lastMinVideoRenderFreeze2xT();

    @WamProperty(index = 2856, type = WamType.TIMER)
    Optional<Instant> lastMinVideoRenderFreeze2xTV2();

    @WamProperty(index = 1621, type = WamType.TIMER)
    Optional<Instant> lastMinVideoRenderFreeze4xT();

    @WamProperty(index = 2857, type = WamType.TIMER)
    Optional<Instant> lastMinVideoRenderFreeze4xTV2();

    @WamProperty(index = 1622, type = WamType.TIMER)
    Optional<Instant> lastMinVideoRenderFreeze8xT();

    @WamProperty(index = 2858, type = WamType.TIMER)
    Optional<Instant> lastMinVideoRenderFreeze8xTV2();

    @WamProperty(index = 1623, type = WamType.TIMER)
    Optional<Instant> lastMinVideoRenderFreezeT();

    @WamProperty(index = 2859, type = WamType.TIMER)
    Optional<Instant> lastMinVideoRenderFreezeTV2();

    @WamProperty(index = 1624, type = WamType.TIMER)
    Optional<Instant> lastMinuteCallAvgRtt();

    @WamProperty(index = 2223, type = WamType.INTEGER)
    OptionalLong lastPpDuringPip();

    @WamProperty(index = 1684, type = WamType.INTEGER)
    OptionalLong lastRelayCnt();

    @WamProperty(index = 2667, type = WamType.STRING)
    Optional<String> lastVoipActivity();

    @WamProperty(index = 2705, type = WamType.INTEGER)
    OptionalLong lastVoipActivityTimestampSec();

    @WamProperty(index = 2675, type = WamType.STRING)
    Optional<String> lastVoipUiActivity();

    @WamProperty(index = 2706, type = WamType.INTEGER)
    OptionalLong lastVoipUiActivityTimestampSec();

    @WamProperty(index = 2144, type = WamType.ENUM)
    Optional<LobbyEntryPointType> lobbyEntryPoint();

    @WamProperty(index = 1127, type = WamType.TIMER)
    Optional<Instant> lobbyVisibleT();

    @WamProperty(index = 2477, type = WamType.STRING)
    Optional<String> localIpPrefix();

    @WamProperty(index = 1120, type = WamType.INTEGER)
    OptionalLong logSampleRatio();

    @WamProperty(index = 1331, type = WamType.TIMER)
    Optional<Instant> lonelyT();

    @WamProperty(index = 21, type = WamType.BOOLEAN)
    Optional<Boolean> longConnect();

    @WamProperty(index = 535, type = WamType.INTEGER)
    OptionalLong lossOfAltSocket();

    @WamProperty(index = 1933, type = WamType.INTEGER)
    OptionalLong loudnessOutputNoiseFrames2650();

    @WamProperty(index = 1934, type = WamType.INTEGER)
    OptionalLong loudnessOutputNoiseFrames5175();

    @WamProperty(index = 1935, type = WamType.INTEGER)
    OptionalLong loudnessOutputNoiseFrames76100();

    @WamProperty(index = 1936, type = WamType.INTEGER)
    OptionalLong loudnessOutputNoiseFramesGt100();

    @WamProperty(index = 1937, type = WamType.INTEGER)
    OptionalLong loudnessOutputNoiseFramesLeq25();

    @WamProperty(index = 157, type = WamType.FLOAT)
    OptionalDouble lowDataUsageBitrate();

    @WamProperty(index = 885, type = WamType.TIMER)
    Optional<Instant> lowPeerBweT();

    @WamProperty(index = 886, type = WamType.TIMER)
    Optional<Instant> lowToHighPeerBweT();

    @WamProperty(index = 1771, type = WamType.INTEGER)
    OptionalLong ltrAcksAcked();

    @WamProperty(index = 1772, type = WamType.INTEGER)
    OptionalLong ltrAcksReceived();

    @WamProperty(index = 1773, type = WamType.INTEGER)
    OptionalLong ltrFrameCount();

    @WamProperty(index = 2720, type = WamType.TIMER)
    Optional<Instant> mainTabHiddenMsT();

    @WamProperty(index = 2721, type = WamType.TIMER)
    Optional<Instant> mainTabVisibleMsT();

    @WamProperty(index = 452, type = WamType.STRING)
    Optional<String> malformedStanzaXpath();

    @WamProperty(index = 2503, type = WamType.TIMER)
    Optional<Instant> manuallyDisabledPeerCameraPauseT();

    @WamProperty(index = 1530, type = WamType.INTEGER)
    OptionalLong mathPlcRemoveHighPktLossCongCount();

    @WamProperty(index = 1085, type = WamType.INTEGER)
    OptionalLong maxConnectedParticipants();

    @WamProperty(index = 2742, type = WamType.FLOAT)
    OptionalDouble maxEchoConfidenceAfter30sec();

    @WamProperty(index = 1725, type = WamType.INTEGER)
    OptionalLong maxEchoLikelihood();

    @WamProperty(index = 2743, type = WamType.FLOAT)
    OptionalDouble maxEchoLikelihoodAfter30sec();

    @WamProperty(index = 558, type = WamType.INTEGER)
    OptionalLong maxEventQueueDepth();

    @WamProperty(index = 2631, type = WamType.INTEGER)
    OptionalLong maxFieldStatStructEntries();

    @WamProperty(index = 3044, type = WamType.INTEGER)
    OptionalLong maxNumConnectedExtensions();

    @WamProperty(index = 1745, type = WamType.TIMER)
    Optional<Instant> maxPktProcessLatencyMs();

    @WamProperty(index = 1953, type = WamType.TIMER)
    Optional<Instant> maxTargetBitrateVidReaches1000kbpsDuration();

    @WamProperty(index = 2421, type = WamType.TIMER)
    Optional<Instant> maxTargetBitrateVidReaches1200kbpsDuration();

    @WamProperty(index = 2422, type = WamType.TIMER)
    Optional<Instant> maxTargetBitrateVidReaches1300kbpsDuration();

    @WamProperty(index = 1954, type = WamType.TIMER)
    Optional<Instant> maxTargetBitrateVidReaches1500kbpsDuration();

    @WamProperty(index = 1955, type = WamType.TIMER)
    Optional<Instant> maxTargetBitrateVidReaches2000kbpsDuration();

    @WamProperty(index = 1956, type = WamType.TIMER)
    Optional<Instant> maxTargetBitrateVidReaches500kbpsDuration();

    @WamProperty(index = 1746, type = WamType.INTEGER)
    OptionalLong maxUnboundRelayCount();

    @WamProperty(index = 2192, type = WamType.INTEGER)
    OptionalLong mcpDisabledCountClampingPp();

    @WamProperty(index = 2193, type = WamType.INTEGER)
    OptionalLong mcpDisabledCountClampingRbe();

    @WamProperty(index = 2194, type = WamType.INTEGER)
    OptionalLong mcpDisabledCountCongestion();

    @WamProperty(index = 2195, type = WamType.INTEGER)
    OptionalLong mcpDisabledCountReachMcpStop();

    @WamProperty(index = 1747, type = WamType.TIMER)
    Optional<Instant> meanPktProcessLatencyMs();

    @WamProperty(index = 448, type = WamType.TIMER)
    Optional<Instant> mediaStreamSetupT();

    @WamProperty(index = 1824, type = WamType.FLOAT)
    OptionalDouble memUtilizationAvg();

    @WamProperty(index = 1825, type = WamType.FLOAT)
    OptionalDouble memUtilizationPeak();

    @WamProperty(index = 2744, type = WamType.INTEGER)
    OptionalLong memoryAvailableMb();

    @WamProperty(index = 253, type = WamType.INTEGER)
    OptionalLong micAvgPower();

    @WamProperty(index = 252, type = WamType.INTEGER)
    OptionalLong micMaxPower();

    @WamProperty(index = 251, type = WamType.INTEGER)
    OptionalLong micMinPower();

    @WamProperty(index = 859, type = WamType.BOOLEAN)
    Optional<Boolean> micPermission();

    @WamProperty(index = 862, type = WamType.TIMER)
    Optional<Instant> micStartDuration();

    @WamProperty(index = 931, type = WamType.TIMER)
    Optional<Instant> micStartToFirstCallbackT();

    @WamProperty(index = 863, type = WamType.TIMER)
    Optional<Instant> micStopDuration();

    @WamProperty(index = 2304, type = WamType.TIMER)
    Optional<Instant> minP2pSessionMs();

    @WamProperty(index = 2429, type = WamType.BOOLEAN)
    Optional<Boolean> mlBweUsedFallbackModel();

    @WamProperty(index = 2127, type = WamType.TIMER)
    Optional<Instant> mlCongModelAvgInferenceTime();

    @WamProperty(index = 2128, type = WamType.FLOAT)
    OptionalDouble mlCongModelAvgPredLen();

    @WamProperty(index = 2346, type = WamType.INTEGER)
    OptionalLong mlCongModelAvgProbInt();

    @WamProperty(index = 2130, type = WamType.INTEGER)
    OptionalLong mlCongModelDownloadFailureCount();

    @WamProperty(index = 2131, type = WamType.INTEGER)
    OptionalLong mlCongModelInferenceFailureCount();

    @WamProperty(index = 2132, type = WamType.TIMER)
    Optional<Instant> mlCongModelMaxInferenceTime();

    @WamProperty(index = 2133, type = WamType.TIMER)
    Optional<Instant> mlCongModelMinInferenceTime();

    @WamProperty(index = 2134, type = WamType.INTEGER)
    OptionalLong mlCongModelNumCongPredictions();

    @WamProperty(index = 2135, type = WamType.INTEGER)
    OptionalLong mlCongModelNumNonCongPredictions();

    @WamProperty(index = 2136, type = WamType.INTEGER)
    OptionalLong mlCongModelStartBitrate();

    @WamProperty(index = 2137, type = WamType.TIMER)
    Optional<Instant> mlCongShimAvgCreationTime();

    @WamProperty(index = 2138, type = WamType.INTEGER)
    OptionalLong mlCongShimCreationFailureCount();

    @WamProperty(index = 2196, type = WamType.INTEGER)
    OptionalLong mlDisabledCountCloseToCap();

    @WamProperty(index = 2197, type = WamType.INTEGER)
    OptionalLong mlDisabledCountLowBitrate();

    @WamProperty(index = 2198, type = WamType.INTEGER)
    OptionalLong mlDisabledCountMediaUndershoot();

    @WamProperty(index = 2199, type = WamType.INTEGER)
    OptionalLong mlDisabledCountRecentRampUp();

    @WamProperty(index = 2200, type = WamType.INTEGER)
    OptionalLong mlDisabledCountRecentRd();

    @WamProperty(index = 2544, type = WamType.INTEGER)
    OptionalLong mlHdTargeting2ModelDownloadFailureCount();

    @WamProperty(index = 2545, type = WamType.INTEGER)
    OptionalLong mlHdTargeting2ModelHdCapableCount();

    @WamProperty(index = 2546, type = WamType.INTEGER)
    OptionalLong mlHdTargeting2ModelInferenceFailureCount();

    @WamProperty(index = 2547, type = WamType.TIMER)
    Optional<Instant> mlHdTargeting2ModelInferenceTime();

    @WamProperty(index = 2549, type = WamType.INTEGER)
    OptionalLong mlHdTargeting2ModelProbInt();

    @WamProperty(index = 2550, type = WamType.TIMER)
    Optional<Instant> mlHdTargeting2ShimAvgCreationTime();

    @WamProperty(index = 2551, type = WamType.INTEGER)
    OptionalLong mlHdTargeting2ShimCreationFailureCount();

    @WamProperty(index = 2284, type = WamType.INTEGER)
    OptionalLong mlHdTargetingModelDownloadFailureCount();

    @WamProperty(index = 2320, type = WamType.INTEGER)
    OptionalLong mlHdTargetingModelHdCapableCount();

    @WamProperty(index = 2285, type = WamType.INTEGER)
    OptionalLong mlHdTargetingModelInferenceFailureCount();

    @WamProperty(index = 2286, type = WamType.TIMER)
    Optional<Instant> mlHdTargetingModelInferenceTime();

    @WamProperty(index = 2340, type = WamType.INTEGER)
    OptionalLong mlHdTargetingModelProbInt();

    @WamProperty(index = 2627, type = WamType.BOOLEAN)
    Optional<Boolean> mlHdTargetingServerMlEnabled();

    @WamProperty(index = 2288, type = WamType.TIMER)
    Optional<Instant> mlHdTargetingShimAvgCreationTime();

    @WamProperty(index = 2289, type = WamType.INTEGER)
    OptionalLong mlHdTargetingShimCreationFailureCount();

    @WamProperty(index = 2455, type = WamType.TIMER)
    Optional<Instant> mlHdTargetingSmlDelayMs();

    @WamProperty(index = 2456, type = WamType.BOOLEAN)
    Optional<Boolean> mlHdTargetingSmlReceived();

    @WamProperty(index = 2457, type = WamType.INTEGER)
    OptionalLong mlHdTargetingSmlValue();

    @WamProperty(index = 2616, type = WamType.FLOAT)
    OptionalDouble mlNadlAudioDupEnabledRatio();

    @WamProperty(index = 2617, type = WamType.INTEGER)
    OptionalLong mlNadlDifferentResultCount();

    @WamProperty(index = 2618, type = WamType.INTEGER)
    OptionalLong mlNadlModelDownloadFailureCount();

    @WamProperty(index = 2619, type = WamType.INTEGER)
    OptionalLong mlNadlModelInferenceFailureCount();

    @WamProperty(index = 2620, type = WamType.TIMER)
    Optional<Instant> mlNadlModelInferenceTime();

    @WamProperty(index = 2621, type = WamType.TIMER)
    Optional<Instant> mlNadlShimAvgCreationTime();

    @WamProperty(index = 2622, type = WamType.INTEGER)
    OptionalLong mlNadlShimCreationFailureCount();

    @WamProperty(index = 2582, type = WamType.BOOLEAN)
    Optional<Boolean> mlNsAspInitFailed();

    @WamProperty(index = 2712, type = WamType.INTEGER)
    OptionalLong mlNsAspInitFailureReason();

    @WamProperty(index = 2583, type = WamType.BOOLEAN)
    Optional<Boolean> mlNsGetModelPathFailed();

    @WamProperty(index = 2561, type = WamType.BOOLEAN)
    Optional<Boolean> mlNsStoppedByUser();

    @WamProperty(index = 2562, type = WamType.BOOLEAN)
    Optional<Boolean> mlNsStoppedHighCpu();

    @WamProperty(index = 2563, type = WamType.BOOLEAN)
    Optional<Boolean> mlNsStoppedInitFailure();

    @WamProperty(index = 2864, type = WamType.INTEGER)
    OptionalLong mlOfflineRlBweActionFallbackCount();

    @WamProperty(index = 2865, type = WamType.INTEGER)
    OptionalLong mlOfflineRlBweActionPassCount();

    @WamProperty(index = 2866, type = WamType.INTEGER)
    OptionalLong mlOfflineRlBweCheckNotReadyCount();

    @WamProperty(index = 2867, type = WamType.INTEGER)
    OptionalLong mlOfflineRlBweCheckRangeCount();

    @WamProperty(index = 2868, type = WamType.INTEGER)
    OptionalLong mlOfflineRlBweCheckRocCount();

    @WamProperty(index = 2869, type = WamType.INTEGER)
    OptionalLong mlOfflineRlBweCheckTfrcDivCount();

    @WamProperty(index = 2870, type = WamType.INTEGER)
    OptionalLong mlOfflineRlBweCheckVarianceCount();

    @WamProperty(index = 2567, type = WamType.INTEGER)
    OptionalLong mlOfflineRlBweModelAvgSbwe();

    @WamProperty(index = 2568, type = WamType.FLOAT)
    OptionalDouble mlOfflineRlBweModelAvgStd();

    @WamProperty(index = 2569, type = WamType.INTEGER)
    OptionalLong mlOfflineRlBweModelDownloadFailureCount();

    @WamProperty(index = 2570, type = WamType.INTEGER)
    OptionalLong mlOfflineRlBweModelInferenceFailureCount();

    @WamProperty(index = 2571, type = WamType.TIMER)
    Optional<Instant> mlOfflineRlBweModelInferenceTime();

    @WamProperty(index = 2572, type = WamType.TIMER)
    Optional<Instant> mlOfflineRlBweShimAvgCreationTime();

    @WamProperty(index = 2573, type = WamType.INTEGER)
    OptionalLong mlOfflineRlBweShimCreationFailureCount();

    @WamProperty(index = 2871, type = WamType.INTEGER)
    OptionalLong mlOfflineRlBweTotalChecks();

    @WamProperty(index = 2872, type = WamType.FLOAT)
    OptionalDouble mlOfflineRlBweUsagePct();

    @WamProperty(index = 1531, type = WamType.BOOLEAN)
    Optional<Boolean> mlPlcModelAvailableInCall();

    @WamProperty(index = 1532, type = WamType.TIMER)
    Optional<Instant> mlPlcModelAvgDownloadTime();

    @WamProperty(index = 1533, type = WamType.TIMER)
    Optional<Instant> mlPlcModelAvgExtractionTime();

    @WamProperty(index = 1534, type = WamType.INTEGER)
    OptionalLong mlPlcModelAvgInferenceInterval();

    @WamProperty(index = 1535, type = WamType.TIMER)
    Optional<Instant> mlPlcModelAvgInferenceTime();

    @WamProperty(index = 1536, type = WamType.INTEGER)
    OptionalLong mlPlcModelDownloadFailureCount();

    @WamProperty(index = 1537, type = WamType.INTEGER)
    OptionalLong mlPlcModelInferenceFailureCount();

    @WamProperty(index = 1538, type = WamType.TIMER)
    Optional<Instant> mlPlcModelMaxInferenceTime();

    @WamProperty(index = 1539, type = WamType.TIMER)
    Optional<Instant> mlPlcModelMinInferenceTime();

    @WamProperty(index = 1540, type = WamType.INTEGER)
    OptionalLong mlPlcModelShortInferenceIntervalCount();

    @WamProperty(index = 1541, type = WamType.INTEGER)
    OptionalLong mlPlcRemoveHighPktLossCongCount();

    @WamProperty(index = 1542, type = WamType.TIMER)
    Optional<Instant> mlShimAvgCreationTime();

    @WamProperty(index = 1543, type = WamType.INTEGER)
    OptionalLong mlShimCreationFailureCount();

    @WamProperty(index = 2038, type = WamType.TIMER)
    Optional<Instant> mlTrModelAvgInferenceTime();

    @WamProperty(index = 2022, type = WamType.FLOAT)
    OptionalDouble mlTrModelAvgPredLen();

    @WamProperty(index = 2347, type = WamType.INTEGER)
    OptionalLong mlTrModelAvgProbInt();

    @WamProperty(index = 2024, type = WamType.INTEGER)
    OptionalLong mlTrModelBweAction();

    @WamProperty(index = 2039, type = WamType.INTEGER)
    OptionalLong mlTrModelDownloadFailureCount();

    @WamProperty(index = 2040, type = WamType.INTEGER)
    OptionalLong mlTrModelInferenceFailureCount();

    @WamProperty(index = 2041, type = WamType.TIMER)
    Optional<Instant> mlTrModelMaxInferenceTime();

    @WamProperty(index = 2042, type = WamType.TIMER)
    Optional<Instant> mlTrModelMinInferenceTime();

    @WamProperty(index = 2025, type = WamType.INTEGER)
    OptionalLong mlTrModelNumNonTrPredictions();

    @WamProperty(index = 2026, type = WamType.INTEGER)
    OptionalLong mlTrModelNumSkippedTrPredictions();

    @WamProperty(index = 2027, type = WamType.INTEGER)
    OptionalLong mlTrModelNumTrPredictions();

    @WamProperty(index = 2028, type = WamType.INTEGER)
    OptionalLong mlTrModelStartBitrate();

    @WamProperty(index = 2043, type = WamType.TIMER)
    Optional<Instant> mlTrShimAvgCreationTime();

    @WamProperty(index = 2044, type = WamType.INTEGER)
    OptionalLong mlTrShimCreationFailureCount();

    @WamProperty(index = 1633, type = WamType.BOOLEAN)
    Optional<Boolean> mlUndershootModelAvailableInCall();

    @WamProperty(index = 1634, type = WamType.TIMER)
    Optional<Instant> mlUndershootModelAvgDownloadTime();

    @WamProperty(index = 1635, type = WamType.TIMER)
    Optional<Instant> mlUndershootModelAvgExtractionTime();

    @WamProperty(index = 1636, type = WamType.INTEGER)
    OptionalLong mlUndershootModelAvgInferenceInterval();

    @WamProperty(index = 1637, type = WamType.TIMER)
    Optional<Instant> mlUndershootModelAvgInferenceTime();

    @WamProperty(index = 1638, type = WamType.INTEGER)
    OptionalLong mlUndershootModelDownloadFailureCount();

    @WamProperty(index = 1639, type = WamType.INTEGER)
    OptionalLong mlUndershootModelInferenceFailureCount();

    @WamProperty(index = 1640, type = WamType.TIMER)
    Optional<Instant> mlUndershootModelMaxInferenceTime();

    @WamProperty(index = 1641, type = WamType.TIMER)
    Optional<Instant> mlUndershootModelMinInferenceTime();

    @WamProperty(index = 1642, type = WamType.INTEGER)
    OptionalLong mlUndershootModelShortInferenceIntervalCount();

    @WamProperty(index = 1654, type = WamType.ENUM)
    Optional<MlUndershootPytorchEdgeLibLoadErrorCode> mlUndershootPytorchEdgeLibLoadErrorCode();

    @WamProperty(index = 1655, type = WamType.ENUM)
    Optional<MlUndershootPytorchEdgeLibLoadStatus> mlUndershootPytorchEdgeLibLoadStatus();

    @WamProperty(index = 1643, type = WamType.TIMER)
    Optional<Instant> mlUndershootShimAvgCreationTime();

    @WamProperty(index = 1644, type = WamType.INTEGER)
    OptionalLong mlUndershootShimCreationFailureCount();

    @WamProperty(index = 1645, type = WamType.INTEGER)
    OptionalLong mlUndershootTriggerMcpCount();

    @WamProperty(index = 3048, type = WamType.INTEGER)
    OptionalLong mlowDnnComplexityTriggerCount();

    @WamProperty(index = 3049, type = WamType.BOOLEAN)
    Optional<Boolean> mlowDnnLoaded();

    @WamProperty(index = 3050, type = WamType.BOOLEAN)
    Optional<Boolean> mlowDnnPermanentlyDisabled();

    @WamProperty(index = 3051, type = WamType.INTEGER)
    OptionalLong mlowDnnWarmupCount();

    @WamProperty(index = 3052, type = WamType.BOOLEAN)
    Optional<Boolean> mlowDnnWeightsUsed();

    @WamProperty(index = 838, type = WamType.BOOLEAN)
    Optional<Boolean> multipleTxRxRelaysInUse();

    @WamProperty(index = 1169, type = WamType.INTEGER)
    OptionalLong muteNotSupportedCount();

    @WamProperty(index = 1170, type = WamType.INTEGER)
    OptionalLong muteReqAlreadyMutedCount();

    @WamProperty(index = 1171, type = WamType.INTEGER)
    OptionalLong muteReqTimeoutsCount();

    @WamProperty(index = 2713, type = WamType.INTEGER)
    OptionalLong nativeDriverFramesPerBuffer();

    @WamProperty(index = 31, type = WamType.STRING)
    Optional<String> nativeSamplingRate();

    @WamProperty(index = 1498, type = WamType.INTEGER)
    OptionalLong netHealthAverageCount();

    @WamProperty(index = 1499, type = WamType.INTEGER)
    OptionalLong netHealthGoodCount();

    @WamProperty(index = 1500, type = WamType.INTEGER)
    OptionalLong netHealthMeasuringCount();

    @WamProperty(index = 1501, type = WamType.INTEGER)
    OptionalLong netHealthNonetworkCount();

    @WamProperty(index = 1502, type = WamType.FLOAT)
    OptionalDouble netHealthPercentInAverage();

    @WamProperty(index = 1503, type = WamType.FLOAT)
    OptionalDouble netHealthPercentInGood();

    @WamProperty(index = 1504, type = WamType.FLOAT)
    OptionalDouble netHealthPercentInMeasuring();

    @WamProperty(index = 1505, type = WamType.FLOAT)
    OptionalDouble netHealthPercentInNonetwork();

    @WamProperty(index = 1506, type = WamType.FLOAT)
    OptionalDouble netHealthPercentInPoor();

    @WamProperty(index = 1507, type = WamType.INTEGER)
    OptionalLong netHealthPoorCount();

    @WamProperty(index = 1508, type = WamType.INTEGER)
    OptionalLong netHealthSlowPoorByReconnect();

    @WamProperty(index = 1509, type = WamType.INTEGER)
    OptionalLong netHealthSlowPoorByRxStop();

    @WamProperty(index = 653, type = WamType.INTEGER)
    OptionalLong neteqAcceleratedFrames();

    @WamProperty(index = 1721, type = WamType.INTEGER)
    OptionalLong neteqBufferFlushCount();

    @WamProperty(index = 652, type = WamType.INTEGER)
    OptionalLong neteqExpandedFrames();

    @WamProperty(index = 1722, type = WamType.INTEGER)
    OptionalLong neteqPreemptiveExpandedFrames();

    @WamProperty(index = 1723, type = WamType.FLOAT)
    OptionalDouble neteqTargetDelayMs();

    @WamProperty(index = 2282, type = WamType.INTEGER)
    OptionalLong networkEventCriticalEventsRetainedCount();

    @WamProperty(index = 1135, type = WamType.INTEGER)
    OptionalLong networkFailoverTriggeredCount();

    @WamProperty(index = 1911, type = WamType.INTEGER)
    OptionalLong networkMediumTransitionBitmap();

    @WamProperty(index = 2159, type = WamType.STRING)
    Optional<String> networkReachabilityResult();

    @WamProperty(index = 1361, type = WamType.INTEGER)
    OptionalLong newEndCallSurveyVersion();

    @WamProperty(index = 2412, type = WamType.STRING)
    Optional<String> niCallId();

    @WamProperty(index = 2125, type = WamType.INTEGER)
    OptionalLong noAudioDuration();

    @WamProperty(index = 2693, type = WamType.BOOLEAN)
    Optional<Boolean> noiseSuppressionUiStatus();

    @WamProperty(index = 1796, type = WamType.INTEGER)
    OptionalLong nonUdstNumPredictions();

    @WamProperty(index = 1912, type = WamType.TIMER)
    Optional<Instant> noneNetTransitionDurationMs();

    @WamProperty(index = 1846, type = WamType.INTEGER)
    OptionalLong nsAlgorithmUsed();

    @WamProperty(index = 1128, type = WamType.BOOLEAN)
    Optional<Boolean> nseEnabled();

    @WamProperty(index = 1129, type = WamType.TIMER)
    Optional<Instant> nseOfflineQueueMs();

    @WamProperty(index = 2564, type = WamType.INTEGER)
    OptionalLong num10msFrames();

    @WamProperty(index = 2565, type = WamType.INTEGER)
    OptionalLong num10msMlProcessedFrames();

    @WamProperty(index = 2666, type = WamType.INTEGER)
    OptionalLong numAnrs();

    @WamProperty(index = 933, type = WamType.INTEGER)
    OptionalLong numAsserts();

    @WamProperty(index = 1800, type = WamType.INTEGER)
    OptionalLong numAudRcDynCondTrue();

    @WamProperty(index = 3045, type = WamType.INTEGER)
    OptionalLong numConnectedExtensions();

    @WamProperty(index = 330, type = WamType.INTEGER)
    OptionalLong numConnectedParticipants();

    @WamProperty(index = 1052, type = WamType.INTEGER)
    OptionalLong numConnectedPeers();

    @WamProperty(index = 2010, type = WamType.INTEGER)
    OptionalLong numCpuCores();

    @WamProperty(index = 567, type = WamType.INTEGER)
    OptionalLong numCriticalGroupUpdateDropped();

    @WamProperty(index = 1442, type = WamType.INTEGER)
    OptionalLong numCropCaptureContentSs();

    @WamProperty(index = 1729, type = WamType.INTEGER)
    OptionalLong numDecResolutionSwitches();

    @WamProperty(index = 985, type = WamType.INTEGER)
    OptionalLong numDirPjAsserts();

    @WamProperty(index = 2555, type = WamType.INTEGER)
    OptionalLong numFppChanges();

    @WamProperty(index = 1695, type = WamType.INTEGER)
    OptionalLong numHbhFecPktReceived();

    @WamProperty(index = 1696, type = WamType.INTEGER)
    OptionalLong numHbhFecPktSent();

    @WamProperty(index = 1958, type = WamType.INTEGER)
    OptionalLong numHbhFecSrtpPktReceived();

    @WamProperty(index = 1959, type = WamType.INTEGER)
    OptionalLong numHbhFecSrtpPktSent();

    @WamProperty(index = 1054, type = WamType.INTEGER)
    OptionalLong numInvitedParticipants();

    @WamProperty(index = 929, type = WamType.INTEGER)
    OptionalLong numL1Errors();

    @WamProperty(index = 1697, type = WamType.INTEGER)
    OptionalLong numMediaPktRecoveredByHbhFec();

    @WamProperty(index = 1960, type = WamType.INTEGER)
    OptionalLong numMediaPktRecoveredByHbhFecSrtp();

    @WamProperty(index = 2381, type = WamType.INTEGER)
    OptionalLong numMediaPktRecoveredByRsHbhFec();

    @WamProperty(index = 2694, type = WamType.INTEGER)
    OptionalLong numNoiseSuppressionUiStatusTransitions();

    @WamProperty(index = 625, type = WamType.INTEGER)
    OptionalLong numOutOfOrderCriticalGroupUpdate();

    @WamProperty(index = 1053, type = WamType.INTEGER)
    OptionalLong numOutgoingRingingPeers();

    @WamProperty(index = 1583, type = WamType.INTEGER)
    OptionalLong numProcessedNoiseFrames();

    @WamProperty(index = 1584, type = WamType.INTEGER)
    OptionalLong numProcessedSpeechFrames();

    @WamProperty(index = 2008, type = WamType.INTEGER)
    OptionalLong numRelayLatenciesAcked();

    @WamProperty(index = 2009, type = WamType.INTEGER)
    OptionalLong numRelayLatenciesSent();

    @WamProperty(index = 1029, type = WamType.INTEGER)
    OptionalLong numRenderSkipGreenFrame();

    @WamProperty(index = 2366, type = WamType.INTEGER)
    OptionalLong numResRampdowns();

    @WamProperty(index = 993, type = WamType.INTEGER)
    OptionalLong numResSwitch();

    @WamProperty(index = 2245, type = WamType.INTEGER)
    OptionalLong numRsHbhFecSrtpPktReceived();

    @WamProperty(index = 2246, type = WamType.INTEGER)
    OptionalLong numRsHbhFecSrtpPktSent();

    @WamProperty(index = 1647, type = WamType.FLOAT)
    OptionalDouble numRxSubscribers();

    @WamProperty(index = 574, type = WamType.INTEGER)
    OptionalLong numVidDlAutoPause();

    @WamProperty(index = 576, type = WamType.INTEGER)
    OptionalLong numVidDlAutoResume();

    @WamProperty(index = 579, type = WamType.INTEGER)
    OptionalLong numVidDlAutoResumeRejectBadAudio();

    @WamProperty(index = 717, type = WamType.INTEGER)
    OptionalLong numVidRcDynCondTrue();

    @WamProperty(index = 559, type = WamType.INTEGER)
    OptionalLong numVidUlAutoPause();

    @WamProperty(index = 560, type = WamType.INTEGER)
    OptionalLong numVidUlAutoPauseFail();

    @WamProperty(index = 564, type = WamType.INTEGER)
    OptionalLong numVidUlAutoPauseRejectHighSendingRate();

    @WamProperty(index = 565, type = WamType.INTEGER)
    OptionalLong numVidUlAutoPauseRejectTooEarly();

    @WamProperty(index = 566, type = WamType.INTEGER)
    OptionalLong numVidUlAutoPauseUserAction();

    @WamProperty(index = 561, type = WamType.INTEGER)
    OptionalLong numVidUlAutoResume();

    @WamProperty(index = 562, type = WamType.INTEGER)
    OptionalLong numVidUlAutoResumeFail();

    @WamProperty(index = 563, type = WamType.INTEGER)
    OptionalLong numVidUlAutoResumeRejectAudioLqm();

    @WamProperty(index = 1648, type = WamType.FLOAT)
    OptionalDouble numVideoStreamsDisabled();

    @WamProperty(index = 1017, type = WamType.TIMER)
    Optional<Instant> offerAckLatencyMs();

    @WamProperty(index = 1983, type = WamType.ENUM)
    Optional<PushProvider> offerPushProvider();

    @WamProperty(index = 805, type = WamType.TIMER)
    Optional<Instant> oibweDlProbingTime();

    @WamProperty(index = 802, type = WamType.TIMER)
    Optional<Instant> oibweE2eProbingTime();

    @WamProperty(index = 868, type = WamType.BOOLEAN)
    Optional<Boolean> oibweNotFinishedWhenCallActive();

    @WamProperty(index = 803, type = WamType.TIMER)
    Optional<Instant> oibweOibleProbingTime();

    @WamProperty(index = 804, type = WamType.TIMER)
    Optional<Instant> oibweUlProbingTime();

    @WamProperty(index = 525, type = WamType.BOOLEAN)
    Optional<Boolean> onMobileDataSaver();

    @WamProperty(index = 540, type = WamType.BOOLEAN)
    Optional<Boolean> onWifiAtStart();

    @WamProperty(index = 2229, type = WamType.INTEGER)
    OptionalLong onePopToXpopFallbackCount();

    @WamProperty(index = 507, type = WamType.INTEGER)
    OptionalLong oneSideInitRxBitrate();

    @WamProperty(index = 506, type = WamType.INTEGER)
    OptionalLong oneSideInitTxBitrate();

    @WamProperty(index = 509, type = WamType.INTEGER)
    OptionalLong oneSideMinPeerInitRxBitrate();

    @WamProperty(index = 1489, type = WamType.INTEGER)
    OptionalLong oneSideNumRelaysGroupOffer();

    @WamProperty(index = 508, type = WamType.BOOLEAN)
    Optional<Boolean> oneSideRcvdPeerRxBitrate();

    @WamProperty(index = 1490, type = WamType.INTEGER)
    OptionalLong oneSideRelayTransactionIdFirstAllocResp();

    @WamProperty(index = 2103, type = WamType.INTEGER)
    OptionalLong oneToOneTerminateCount();

    @WamProperty(index = 287, type = WamType.INTEGER)
    OptionalLong opusVersion();

    @WamProperty(index = 2305, type = WamType.STRING)
    Optional<String> p2pCandPairStat();

    @WamProperty(index = 1612, type = WamType.STRING)
    Optional<String> p2pConnectionQualityStat();

    @WamProperty(index = 1883, type = WamType.ENUM)
    Optional<ClientIpVersion> p2pLocalCandAf();

    @WamProperty(index = 1884, type = WamType.ENUM)
    Optional<ClientIpVersion> p2pRemoteCandAf();

    @WamProperty(index = 2306, type = WamType.STRING)
    Optional<String> p2pRtpPktCnts();

    @WamProperty(index = 522, type = WamType.INTEGER)
    OptionalLong p2pSuccessCount();

    @WamProperty(index = 1733, type = WamType.INTEGER)
    OptionalLong packetPairAvgBitrate();

    @WamProperty(index = 1734, type = WamType.FLOAT)
    OptionalDouble packetPairReliableRatio();

    @WamProperty(index = 1735, type = WamType.FLOAT)
    OptionalDouble packetPairUnderestimateRatio();

    @WamProperty(index = 1285, type = WamType.INTEGER)
    OptionalLong pausedRtcpCount();

    @WamProperty(index = 599, type = WamType.FLOAT)
    OptionalDouble pcntPoorAudLqmAfterPause();

    @WamProperty(index = 598, type = WamType.FLOAT)
    OptionalDouble pcntPoorAudLqmBeforePause();

    @WamProperty(index = 1314, type = WamType.FLOAT)
    OptionalDouble pctPeersOnCellular();

    @WamProperty(index = 2367, type = WamType.INTEGER)
    OptionalLong peerBusyHours();

    @WamProperty(index = 264, type = WamType.ENUM)
    Optional<PeerCallNetworkMedium> peerCallNetwork();

    @WamProperty(index = 66, type = WamType.ENUM)
    Optional<CallResultType> peerCallResult();

    @WamProperty(index = 2423, type = WamType.BOOLEAN)
    Optional<Boolean> peerCameraOffCallStart();

    @WamProperty(index = 1494, type = WamType.STRING)
    Optional<String> peerDeviceName();

    @WamProperty(index = 2325, type = WamType.INTEGER)
    OptionalLong peerHistoryDownlinkSignal();

    @WamProperty(index = 1922, type = WamType.BOOLEAN)
    Optional<Boolean> peerIsMultiDevice();

    @WamProperty(index = 2597, type = WamType.STRING)
    Optional<String> peerLocalIpPrefix();

    @WamProperty(index = 2853, type = WamType.BOOLEAN)
    Optional<Boolean> peerNoiseSuppressionUiStatus();

    @WamProperty(index = 2006, type = WamType.INTEGER)
    OptionalLong peerReconnectingStateCount();

    @WamProperty(index = 2899, type = WamType.INTEGER)
    OptionalLong peerRenderFailureReason();

    @WamProperty(index = 1340, type = WamType.FLOAT)
    OptionalDouble peerRxForErrorRelayBytes();

    @WamProperty(index = 1341, type = WamType.FLOAT)
    OptionalDouble peerRxForOtherRelayBytes();

    @WamProperty(index = 1342, type = WamType.FLOAT)
    OptionalDouble peerRxForTxRelayBytes();

    @WamProperty(index = 2030, type = WamType.TIMER)
    Optional<Instant> peerSpeakerViewDurationMs();

    @WamProperty(index = 591, type = WamType.ENUM)
    Optional<CallTransportType> peerTransport();

    @WamProperty(index = 191, type = WamType.INTEGER)
    OptionalLong peerVideoHeight();

    @WamProperty(index = 190, type = WamType.INTEGER)
    OptionalLong peerVideoWidth();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<XmppStatus> peerXmppStatus();

    @WamProperty(index = 1957, type = WamType.INTEGER)
    OptionalLong peerYearClass2016();

    @WamProperty(index = 1172, type = WamType.INTEGER)
    OptionalLong peersMuteSuccCount();

    @WamProperty(index = 1173, type = WamType.INTEGER)
    OptionalLong peersRejectedMuteReqCount();

    @WamProperty(index = 1618, type = WamType.ENUM)
    Optional<CallNetworkMedium> perPeerCallNetwork();

    @WamProperty(index = 1649, type = WamType.INTEGER)
    OptionalLong perPeerVideoDisablingEventCount();

    @WamProperty(index = 3002, type = WamType.ENUM)
    Optional<PeripheralDeviceType> peripheralDeviceOrigin();

    @WamProperty(index = 2413, type = WamType.STRING)
    Optional<String> phoneStateAtEnd();

    @WamProperty(index = 2414, type = WamType.STRING)
    Optional<String> phoneStateAtStart();

    @WamProperty(index = 160, type = WamType.FLOAT)
    OptionalDouble pingsSent();

    @WamProperty(index = 2505, type = WamType.INTEGER)
    OptionalLong pinningViewDuration();

    @WamProperty(index = 2506, type = WamType.INTEGER)
    OptionalLong pinningViewPeerDuration();

    @WamProperty(index = 2722, type = WamType.TIMER)
    Optional<Instant> pipWebWindowMsT();

    @WamProperty(index = 1786, type = WamType.INTEGER)
    OptionalLong plcAvgPredProb();

    @WamProperty(index = 1787, type = WamType.INTEGER)
    OptionalLong plcAvgRandomPredictionLength();

    @WamProperty(index = 1788, type = WamType.INTEGER)
    OptionalLong plcNumBurstyPredictions();

    @WamProperty(index = 1789, type = WamType.INTEGER)
    OptionalLong plcNumRandomPredictions();

    @WamProperty(index = 1790, type = WamType.INTEGER)
    OptionalLong plcNumSkippedPredictions();

    @WamProperty(index = 161, type = WamType.FLOAT)
    OptionalDouble pongsReceived();

    @WamProperty(index = 510, type = WamType.INTEGER)
    OptionalLong poolMemUsage();

    @WamProperty(index = 2723, type = WamType.TIMER)
    Optional<Instant> popoutWebWindowMsT();

    @WamProperty(index = 2224, type = WamType.INTEGER)
    OptionalLong postPipBitrate();

    @WamProperty(index = 2225, type = WamType.INTEGER)
    OptionalLong postPipStartBitrate();

    @WamProperty(index = 2226, type = WamType.INTEGER)
    OptionalLong prePipBitrate();

    @WamProperty(index = 89, type = WamType.ENUM)
    Optional<EndCallConfirmationType> presentEndCallConfirmation();

    @WamProperty(index = 1060, type = WamType.STRING)
    Optional<String> prevCallTestBucket();

    @WamProperty(index = 2046, type = WamType.STRING)
    Optional<String> prevCallTestBucketExposureMap();

    @WamProperty(index = 1892, type = WamType.STRING)
    Optional<String> prevCallTestBucketIdList();

    @WamProperty(index = 1920, type = WamType.STRING)
    Optional<String> prevCallTestBucketNameList();

    @WamProperty(index = 2012, type = WamType.BOOLEAN)
    Optional<Boolean> previousCallCallEndReconnectingE2ePingable();

    @WamProperty(index = 2013, type = WamType.BOOLEAN)
    Optional<Boolean> previousCallCallEndReconnectingE2eSignalingAccessible();

    @WamProperty(index = 2014, type = WamType.BOOLEAN)
    Optional<Boolean> previousCallCallEndReconnectingRelayPingable();

    @WamProperty(index = 2015, type = WamType.BOOLEAN)
    Optional<Boolean> previousCallCallEndReconnectingSignalingAccessible();

    @WamProperty(index = 266, type = WamType.TIMER)
    Optional<Instant> previousCallInterval();

    @WamProperty(index = 265, type = WamType.BOOLEAN)
    Optional<Boolean> previousCallVideoEnabled();

    @WamProperty(index = 2016, type = WamType.INTEGER)
    OptionalLong previousCallWeakWifiSwitchDefIntSuccess();

    @WamProperty(index = 2017, type = WamType.INTEGER)
    OptionalLong previousCallWifiSwitchNonDefIntSuccess();

    @WamProperty(index = 267, type = WamType.BOOLEAN)
    Optional<Boolean> previousCallWithSamePeer();

    @WamProperty(index = 1404, type = WamType.BOOLEAN)
    Optional<Boolean> privacySilenceUnknownCaller();

    @WamProperty(index = 1405, type = WamType.BOOLEAN)
    Optional<Boolean> privacyUnknownCaller();

    @WamProperty(index = 327, type = WamType.FLOAT)
    OptionalDouble probeAvgBitrate();

    @WamProperty(index = 2501, type = WamType.INTEGER)
    OptionalLong proxyBitmap();

    @WamProperty(index = 2574, type = WamType.STRING)
    Optional<String> proxyReason();

    @WamProperty(index = 2584, type = WamType.STRING)
    Optional<String> proxyReasonDynamic();

    @WamProperty(index = 2575, type = WamType.INTEGER)
    OptionalLong proxyState();

    @WamProperty(index = 2585, type = WamType.INTEGER)
    OptionalLong proxyStateDynamic();

    @WamProperty(index = 2586, type = WamType.TIMER)
    Optional<Instant> proxyTimeDynamic();

    @WamProperty(index = 1228, type = WamType.BOOLEAN)
    Optional<Boolean> pstnCallExists();

    @WamProperty(index = 1663, type = WamType.TIMER)
    Optional<Instant> pushAcceptToOfferMs();

    @WamProperty(index = 1598, type = WamType.ENUM)
    Optional<PushGhostCallReason> pushGhostCallReason();

    @WamProperty(index = 1664, type = WamType.ENUM)
    Optional<PushOfferResult> pushOfferResult();

    @WamProperty(index = 1599, type = WamType.BOOLEAN)
    Optional<Boolean> pushPriorityDowngraded();

    @WamProperty(index = 1600, type = WamType.BOOLEAN)
    Optional<Boolean> pushRangWithPayload();

    @WamProperty(index = 158, type = WamType.FLOAT)
    OptionalDouble pushToCallOfferDelay();

    @WamProperty(index = 1544, type = WamType.TIMER)
    Optional<Instant> pytorchEdgeLibAvgLoadingTime();

    @WamProperty(index = 1679, type = WamType.TIMER)
    Optional<Instant> pytorchEdgeLibFirstLoadingTime();

    @WamProperty(index = 1563, type = WamType.ENUM)
    Optional<PytorchEdgeLibLoadErrorCode> pytorchEdgeLibLoadErrorCode();

    @WamProperty(index = 1564, type = WamType.ENUM)
    Optional<PytorchEdgeLibLoadStatus> pytorchEdgeLibLoadStatus();

    @WamProperty(index = 2697, type = WamType.BOOLEAN)
    Optional<Boolean> quickhdMlInferenceDone();

    @WamProperty(index = 2698, type = WamType.BOOLEAN)
    Optional<Boolean> quickhdMlIsChecked();

    @WamProperty(index = 2699, type = WamType.INTEGER)
    OptionalLong quickhdMlModelDownloadFailureCount();

    @WamProperty(index = 2700, type = WamType.INTEGER)
    OptionalLong quickhdMlModelInferenceFailureCount();

    @WamProperty(index = 2701, type = WamType.TIMER)
    Optional<Instant> quickhdMlModelInferenceTime();

    @WamProperty(index = 2702, type = WamType.INTEGER)
    OptionalLong quickhdMlPredictedBitrate();

    @WamProperty(index = 2703, type = WamType.TIMER)
    Optional<Instant> quickhdMlShimAvgCreationTime();

    @WamProperty(index = 2704, type = WamType.INTEGER)
    OptionalLong quickhdMlShimCreationFailureCount();

    @WamProperty(index = 2201, type = WamType.INTEGER)
    OptionalLong rampUpCountInAdditive();

    @WamProperty(index = 2202, type = WamType.INTEGER)
    OptionalLong rampUpCountInFr();

    @WamProperty(index = 2203, type = WamType.INTEGER)
    OptionalLong rampUpCountInMcp();

    @WamProperty(index = 2204, type = WamType.INTEGER)
    OptionalLong rampUpCountInNormal();

    @WamProperty(index = 2205, type = WamType.INTEGER)
    OptionalLong rampUpCountInSru();

    @WamProperty(index = 2206, type = WamType.INTEGER)
    OptionalLong rampUpCountInUdstTarget();

    @WamProperty(index = 2207, type = WamType.TIMER)
    Optional<Instant> rampUpDurationInAdditive();

    @WamProperty(index = 2208, type = WamType.TIMER)
    Optional<Instant> rampUpDurationInFr();

    @WamProperty(index = 2209, type = WamType.TIMER)
    Optional<Instant> rampUpDurationInMcp();

    @WamProperty(index = 2210, type = WamType.TIMER)
    Optional<Instant> rampUpDurationInNormal();

    @WamProperty(index = 2211, type = WamType.TIMER)
    Optional<Instant> rampUpDurationInSru();

    @WamProperty(index = 2212, type = WamType.TIMER)
    Optional<Instant> rampUpDurationInUdstTarget();

    @WamProperty(index = 1885, type = WamType.BOOLEAN)
    Optional<Boolean> randomPreferIpv6Enabled();

    @WamProperty(index = 1581, type = WamType.INTEGER)
    OptionalLong randomScheduledId();

    @WamProperty(index = 2928, type = WamType.INTEGER)
    OptionalLong rbeCap();

    @WamProperty(index = 2929, type = WamType.INTEGER)
    OptionalLong rbeCapUpdateCount();

    @WamProperty(index = 2930, type = WamType.INTEGER)
    OptionalLong rbeCapUpdateMax();

    @WamProperty(index = 2931, type = WamType.INTEGER)
    OptionalLong rbeCapUpdateMin();

    @WamProperty(index = 2932, type = WamType.INTEGER)
    OptionalLong rbeGetIndexFromPlatformAndNetworkCount();

    @WamProperty(index = 2933, type = WamType.INTEGER)
    OptionalLong rbeGetIndexFromPlatformAndNetworkSuccess();

    @WamProperty(index = 2934, type = WamType.INTEGER)
    OptionalLong rbeGetMaxTargetBitrateCount();

    @WamProperty(index = 2935, type = WamType.INTEGER)
    OptionalLong rbeGetMaxTargetBitrateUseRbeCount();

    @WamProperty(index = 2936, type = WamType.INTEGER)
    OptionalLong rbeInitCount();

    @WamProperty(index = 2937, type = WamType.INTEGER)
    OptionalLong rbeInitDone();

    @WamProperty(index = 2938, type = WamType.INTEGER)
    OptionalLong rbeInitSuccess();

    @WamProperty(index = 2939, type = WamType.INTEGER)
    OptionalLong rbeInitVectorDone();

    @WamProperty(index = 2940, type = WamType.INTEGER)
    OptionalLong rbeInitVectorSuccess();

    @WamProperty(index = 2941, type = WamType.INTEGER)
    OptionalLong rbeInstantRampUpCount();

    @WamProperty(index = 2942, type = WamType.INTEGER)
    OptionalLong rbeInstantRampUpSuccess();

    @WamProperty(index = 2943, type = WamType.INTEGER)
    OptionalLong rbeInstantRampUpValue();

    @WamProperty(index = 2944, type = WamType.INTEGER)
    OptionalLong rbePeerNetworkMedium();

    @WamProperty(index = 2945, type = WamType.INTEGER)
    OptionalLong rbePeerPlatformId();

    @WamProperty(index = 2946, type = WamType.INTEGER)
    OptionalLong rbeSelfNetworkMedium();

    @WamProperty(index = 2947, type = WamType.INTEGER)
    OptionalLong rbeSelfPlatformId();

    @WamProperty(index = 2948, type = WamType.BOOLEAN)
    Optional<Boolean> rbeShouldFallbackToVidDyn();

    @WamProperty(index = 2949, type = WamType.INTEGER)
    OptionalLong rbeShouldFallbackToVidDynFlipCount();

    @WamProperty(index = 2950, type = WamType.INTEGER)
    OptionalLong rbeTargetingHistoryCount();

    @WamProperty(index = 2951, type = WamType.INTEGER)
    OptionalLong rbeTargetingHistoryDone();

    @WamProperty(index = 2952, type = WamType.INTEGER)
    OptionalLong rbeTargetingHistorySuccess();

    @WamProperty(index = 2953, type = WamType.INTEGER)
    OptionalLong rbeTargetingHistoryValue();

    @WamProperty(index = 2954, type = WamType.INTEGER)
    OptionalLong rbeTargetingMlCount();

    @WamProperty(index = 2955, type = WamType.INTEGER)
    OptionalLong rbeTargetingMlSuccess();

    @WamProperty(index = 2956, type = WamType.INTEGER)
    OptionalLong rbeTargetingMlValue();

    @WamProperty(index = 2957, type = WamType.INTEGER)
    OptionalLong rbeTargetingPpCount();

    @WamProperty(index = 2958, type = WamType.INTEGER)
    OptionalLong rbeTargetingPpHiCount();

    @WamProperty(index = 2959, type = WamType.INTEGER)
    OptionalLong rbeTargetingPpLoCount();

    @WamProperty(index = 2960, type = WamType.INTEGER)
    OptionalLong rbeTargetingPpSuccess();

    @WamProperty(index = 2961, type = WamType.INTEGER)
    OptionalLong rbeTargetingPpValueLast();

    @WamProperty(index = 2962, type = WamType.INTEGER)
    OptionalLong rbeTargetingPpValueMax();

    @WamProperty(index = 2963, type = WamType.INTEGER)
    OptionalLong rbeTargetingPpValueMin();

    @WamProperty(index = 2964, type = WamType.INTEGER)
    OptionalLong rbeUpdateBitmap();

    @WamProperty(index = 2965, type = WamType.INTEGER)
    OptionalLong rbeVidDynCondCount();

    @WamProperty(index = 2966, type = WamType.INTEGER)
    OptionalLong rbeVidDynCount();

    @WamProperty(index = 2967, type = WamType.INTEGER)
    OptionalLong rbeVidDynHdDynMaxTargetBitrateCount();

    @WamProperty(index = 2968, type = WamType.INTEGER)
    OptionalLong rbeVidDynMaxTargetBitrateCount();

    @WamProperty(index = 2969, type = WamType.INTEGER)
    OptionalLong rbeVidDynMaxTargetBitrateInvokeCount();

    @WamProperty(index = 2970, type = WamType.INTEGER)
    OptionalLong rbeVidDynMaxTargetBitrateOverwriteCount();

    @WamProperty(index = 155, type = WamType.FLOAT)
    OptionalDouble rcMaxrtt();

    @WamProperty(index = 1130, type = WamType.BOOLEAN)
    Optional<Boolean> receivedByNse();

    @WamProperty(index = 1443, type = WamType.INTEGER)
    OptionalLong receiverVideoEncodedHeightSs();

    @WamProperty(index = 1444, type = WamType.INTEGER)
    OptionalLong receiverVideoEncodedWidthSs();

    @WamProperty(index = 2233, type = WamType.INTEGER)
    OptionalLong recentPlaybackFpsDiff1215();

    @WamProperty(index = 2234, type = WamType.INTEGER)
    OptionalLong recentPlaybackFpsDiff1619();

    @WamProperty(index = 2235, type = WamType.INTEGER)
    OptionalLong recentPlaybackFpsDiff47();

    @WamProperty(index = 2236, type = WamType.INTEGER)
    OptionalLong recentPlaybackFpsDiff811();

    @WamProperty(index = 2237, type = WamType.INTEGER)
    OptionalLong recentPlaybackFpsDiffGeq20();

    @WamProperty(index = 2238, type = WamType.INTEGER)
    OptionalLong recentPlaybackFpsDiffLt4();

    @WamProperty(index = 2239, type = WamType.INTEGER)
    OptionalLong recentRecordFpsDiff1215();

    @WamProperty(index = 2240, type = WamType.INTEGER)
    OptionalLong recentRecordFpsDiff1619();

    @WamProperty(index = 2241, type = WamType.INTEGER)
    OptionalLong recentRecordFpsDiff47();

    @WamProperty(index = 2242, type = WamType.INTEGER)
    OptionalLong recentRecordFpsDiff811();

    @WamProperty(index = 2243, type = WamType.INTEGER)
    OptionalLong recentRecordFpsDiffGeq20();

    @WamProperty(index = 2244, type = WamType.INTEGER)
    OptionalLong recentRecordFpsDiffLt4();

    @WamProperty(index = 1974, type = WamType.INTEGER)
    OptionalLong reconnectingWithE2eBindRspCount();

    @WamProperty(index = 1975, type = WamType.INTEGER)
    OptionalLong reconnectingWithE2eRspCount();

    @WamProperty(index = 1901, type = WamType.INTEGER)
    OptionalLong reconnectingWithP2pE2eBindRspCount();

    @WamProperty(index = 1899, type = WamType.INTEGER)
    OptionalLong reconnectingWithProbeRspCount();

    @WamProperty(index = 1902, type = WamType.INTEGER)
    OptionalLong reconnectingWithRelayE2eBindRspCount();

    @WamProperty(index = 1976, type = WamType.INTEGER)
    OptionalLong reconnectingWithRelayPingableCount();

    @WamProperty(index = 1977, type = WamType.INTEGER)
    OptionalLong reconnectingWithSignalingAccessibleCount();

    @WamProperty(index = 84, type = WamType.INTEGER)
    OptionalLong recordCircularBufferFrameCount();

    @WamProperty(index = 1580, type = WamType.INTEGER)
    OptionalLong recordNonSilenceFrameCountDuringMute();

    @WamProperty(index = 2393, type = WamType.FLOAT)
    OptionalDouble redAudioBytesDecoded();

    @WamProperty(index = 2394, type = WamType.FLOAT)
    OptionalDouble redAudioBytesSent();

    @WamProperty(index = 2395, type = WamType.INTEGER)
    OptionalLong redPacketsDiscarded();

    @WamProperty(index = 2396, type = WamType.INTEGER)
    OptionalLong redPacketsInserted();

    @WamProperty(index = 2397, type = WamType.INTEGER)
    OptionalLong redPacketsReceived();

    @WamProperty(index = 2398, type = WamType.INTEGER)
    OptionalLong redRtpPacketsReceived();

    @WamProperty(index = 2399, type = WamType.INTEGER)
    OptionalLong redRtpPacketsSent();

    @WamProperty(index = 2400, type = WamType.INTEGER)
    OptionalLong redTotalRedundancyRequested();

    @WamProperty(index = 2401, type = WamType.INTEGER)
    OptionalLong redTotalRedundancySent();

    @WamProperty(index = 1973, type = WamType.BOOLEAN)
    Optional<Boolean> redialAfterCer();

    @WamProperty(index = 2020, type = WamType.TIMER)
    Optional<Instant> redialIntervalSec();

    @WamProperty(index = 1174, type = WamType.INTEGER)
    OptionalLong rejectMuteReqCount();

    @WamProperty(index = 1140, type = WamType.INTEGER)
    OptionalLong rekeyTime();

    @WamProperty(index = 424, type = WamType.TIMER)
    Optional<Instant> relayBindTimeInMsec();

    @WamProperty(index = 1613, type = WamType.STRING)
    Optional<String> relayConnectionQualityStat();

    @WamProperty(index = 423, type = WamType.TIMER)
    Optional<Instant> relayElectionTimeInMsec();

    @WamProperty(index = 481, type = WamType.INTEGER)
    OptionalLong relayFallbackOnRxDataFromRelay();

    @WamProperty(index = 482, type = WamType.INTEGER)
    OptionalLong relayFallbackOnStopRxDataOnP2p();

    @WamProperty(index = 1908, type = WamType.INTEGER)
    OptionalLong relayLatencyStanzasReceivedCount();

    @WamProperty(index = 2359, type = WamType.STRING)
    Optional<String> relayMeasuredC2rRttList();

    @WamProperty(index = 2360, type = WamType.STRING)
    Optional<String> relayMeasuredMaxPeerC2rRttList();

    @WamProperty(index = 2361, type = WamType.STRING)
    Optional<String> relayMeasuredNumPeersList();

    @WamProperty(index = 1525, type = WamType.TIMER)
    Optional<Instant> relayPingAvgRtt();

    @WamProperty(index = 1526, type = WamType.TIMER)
    Optional<Instant> relayPingMaxRtt();

    @WamProperty(index = 1527, type = WamType.TIMER)
    Optional<Instant> relayPingMinRtt();

    @WamProperty(index = 2362, type = WamType.STRING)
    Optional<String> relayRecommendedC2rRttList();

    @WamProperty(index = 2363, type = WamType.STRING)
    Optional<String> relayRecommendedMaxPeerC2rRttList();

    @WamProperty(index = 2307, type = WamType.STRING)
    Optional<String> relayRtpPktCnts();

    @WamProperty(index = 1309, type = WamType.INTEGER)
    OptionalLong relaySwapped();

    @WamProperty(index = 1378, type = WamType.INTEGER)
    OptionalLong removePeerNackCount();

    @WamProperty(index = 1379, type = WamType.INTEGER)
    OptionalLong removePeerNotInCallCount();

    @WamProperty(index = 1380, type = WamType.INTEGER)
    OptionalLong removePeerNotSupportedCount();

    @WamProperty(index = 1381, type = WamType.INTEGER)
    OptionalLong removePeerRequestCount();

    @WamProperty(index = 1382, type = WamType.INTEGER)
    OptionalLong removePeerSuccessCount();

    @WamProperty(index = 780, type = WamType.TIMER)
    Optional<Instant> renderFreezeHighPeerBweT();

    @WamProperty(index = 778, type = WamType.TIMER)
    Optional<Instant> renderFreezeLowPeerBweT();

    @WamProperty(index = 779, type = WamType.TIMER)
    Optional<Instant> renderFreezeLowToHighPeerBweT();

    @WamProperty(index = 2687, type = WamType.STRING)
    Optional<String> rendererType();

    @WamProperty(index = 1968, type = WamType.ENUM)
    Optional<RingerMode> ringerMode();

    @WamProperty(index = 1362, type = WamType.INTEGER)
    OptionalLong rtcpRembInVideoCnt();

    @WamProperty(index = 1168, type = WamType.INTEGER)
    OptionalLong rxAllocRespNoMatchingTid();

    @WamProperty(index = 1528, type = WamType.INTEGER)
    OptionalLong rxBytesForP2p();

    @WamProperty(index = 1408, type = WamType.FLOAT)
    OptionalDouble rxBytesForUnknownP2p();

    @WamProperty(index = 1614, type = WamType.FLOAT)
    OptionalDouble rxBytesForXpop();

    @WamProperty(index = 2172, type = WamType.INTEGER)
    OptionalLong rxDelayHigherThanRttCount();

    @WamProperty(index = 2173, type = WamType.INTEGER)
    OptionalLong rxDelayNegativeCount();

    @WamProperty(index = 1310, type = WamType.FLOAT)
    OptionalDouble rxForErrorRelayBytes();

    @WamProperty(index = 1311, type = WamType.FLOAT)
    OptionalDouble rxForOtherRelayBytes();

    @WamProperty(index = 1312, type = WamType.FLOAT)
    OptionalDouble rxForTxRelayBytes();

    @WamProperty(index = 1698, type = WamType.INTEGER)
    OptionalLong rxHbhFecBitrateKbps();

    @WamProperty(index = 1961, type = WamType.INTEGER)
    OptionalLong rxHbhFecSrtpBitrateKbps();

    @WamProperty(index = 2353, type = WamType.INTEGER)
    OptionalLong rxLowerHandCount();

    @WamProperty(index = 291, type = WamType.INTEGER)
    OptionalLong rxProbeCountSuccess();

    @WamProperty(index = 290, type = WamType.INTEGER)
    OptionalLong rxProbeCountTotal();

    @WamProperty(index = 2354, type = WamType.INTEGER)
    OptionalLong rxRaiseHandCount();

    @WamProperty(index = 2355, type = WamType.INTEGER)
    OptionalLong rxRaiseOrLowerHandErrorCount();

    @WamProperty(index = 2336, type = WamType.INTEGER)
    OptionalLong rxReactionCount();

    @WamProperty(index = 2337, type = WamType.INTEGER)
    OptionalLong rxReactionErrorCount();

    @WamProperty(index = 841, type = WamType.TIMER)
    Optional<Instant> rxRelayRebindLatencyMs();

    @WamProperty(index = 842, type = WamType.TIMER)
    Optional<Instant> rxRelayResetLatencyMs();

    @WamProperty(index = 1295, type = WamType.TIMER)
    Optional<Instant> rxSubOnScreenDur();

    @WamProperty(index = 1370, type = WamType.INTEGER)
    OptionalLong rxSubRequestSentCnt();

    @WamProperty(index = 1296, type = WamType.INTEGER)
    OptionalLong rxSubRequestThrottledCnt();

    @WamProperty(index = 1297, type = WamType.INTEGER)
    OptionalLong rxSubSwitchCnt();

    @WamProperty(index = 1298, type = WamType.TIMER)
    Optional<Instant> rxSubVideoWaitDur();

    @WamProperty(index = 1366, type = WamType.TIMER)
    Optional<Instant> rxSubVideoWaitDurAvg();

    @WamProperty(index = 1367, type = WamType.TIMER)
    Optional<Instant> rxSubVideoWaitDurSum();

    @WamProperty(index = 145, type = WamType.FLOAT)
    OptionalDouble rxTotalBitrate();

    @WamProperty(index = 143, type = WamType.FLOAT)
    OptionalDouble rxTotalBytes();

    @WamProperty(index = 294, type = WamType.FLOAT)
    OptionalDouble rxTpFbBitrate();

    @WamProperty(index = 1495, type = WamType.INTEGER)
    OptionalLong sbweAbsRttOnHoldCount();

    @WamProperty(index = 963, type = WamType.FLOAT)
    OptionalDouble sbweAvgDowntrend();

    @WamProperty(index = 962, type = WamType.FLOAT)
    OptionalDouble sbweAvgUptrend();

    @WamProperty(index = 783, type = WamType.INTEGER)
    OptionalLong sbweCeilingCongestionCount();

    @WamProperty(index = 781, type = WamType.INTEGER)
    OptionalLong sbweCeilingCount();

    @WamProperty(index = 2174, type = WamType.INTEGER)
    OptionalLong sbweCeilingDelayCongestionCount();

    @WamProperty(index = 786, type = WamType.INTEGER)
    OptionalLong sbweCeilingMissingRtcpCongestionCount();

    @WamProperty(index = 787, type = WamType.INTEGER)
    OptionalLong sbweCeilingNoNewDataReceivedCongestionCount();

    @WamProperty(index = 782, type = WamType.INTEGER)
    OptionalLong sbweCeilingPktLossCount();

    @WamProperty(index = 1106, type = WamType.INTEGER)
    OptionalLong sbweCeilingReceiveSideCount();

    @WamProperty(index = 2175, type = WamType.INTEGER)
    OptionalLong sbweCeilingRttAndDelayCongestionCount();

    @WamProperty(index = 784, type = WamType.INTEGER)
    OptionalLong sbweCeilingRttCongestionCount();

    @WamProperty(index = 785, type = WamType.INTEGER)
    OptionalLong sbweCeilingZeroRttCongestionCount();

    @WamProperty(index = 1103, type = WamType.INTEGER)
    OptionalLong sbweGlobalMinRttCongestionCount();

    @WamProperty(index = 1133, type = WamType.INTEGER)
    OptionalLong sbweHighestRttCongestionCount();

    @WamProperty(index = 961, type = WamType.INTEGER)
    OptionalLong sbweHoldCount();

    @WamProperty(index = 1347, type = WamType.TIMER)
    Optional<Instant> sbweHoldDuration();

    @WamProperty(index = 1104, type = WamType.INTEGER)
    OptionalLong sbweMinRttEmaCongestionCount();

    @WamProperty(index = 1308, type = WamType.INTEGER)
    OptionalLong sbweMinRttSlideWindowCount();

    @WamProperty(index = 960, type = WamType.INTEGER)
    OptionalLong sbweRampDownCount();

    @WamProperty(index = 1348, type = WamType.TIMER)
    Optional<Instant> sbweRampDownDuration();

    @WamProperty(index = 959, type = WamType.INTEGER)
    OptionalLong sbweRampUpCount();

    @WamProperty(index = 1349, type = WamType.TIMER)
    Optional<Instant> sbweRampUpDuration();

    @WamProperty(index = 1134, type = WamType.INTEGER)
    OptionalLong sbweRampUpPauseCount();

    @WamProperty(index = 1496, type = WamType.INTEGER)
    OptionalLong sbweRttSlopeCongestionCount();

    @WamProperty(index = 1497, type = WamType.INTEGER)
    OptionalLong sbweRttSlopeOnHoldCount();

    @WamProperty(index = 1594, type = WamType.INTEGER)
    OptionalLong scheduledCallJoinTimeDiffMs();

    @WamProperty(index = 2121, type = WamType.TIMER)
    Optional<Instant> sctpConnectionSetupT();

    @WamProperty(index = 1965, type = WamType.STRING)
    Optional<String> secondBestRelayIp();

    @WamProperty(index = 1906, type = WamType.TIMER)
    Optional<Instant> segmentStartToDecodeT();

    @WamProperty(index = 2648, type = WamType.INTEGER)
    OptionalLong selectedMicMode();

    @WamProperty(index = 2368, type = WamType.INTEGER)
    OptionalLong selfBusyHours();

    @WamProperty(index = 1175, type = WamType.INTEGER)
    OptionalLong selfMuteSuccessCount();

    @WamProperty(index = 1176, type = WamType.INTEGER)
    OptionalLong selfUnmuteAfterMuteReqCount();

    @WamProperty(index = 2386, type = WamType.INTEGER)
    OptionalLong sendSelfStateVideoEnabledVideoCaptureStreamNotRunning();

    @WamProperty(index = 2387, type = WamType.INTEGER)
    OptionalLong sendSelfStateVideoEnabledVideoCaptureStreamNull();

    @WamProperty(index = 975, type = WamType.INTEGER)
    OptionalLong senderBweInitBitrate();

    @WamProperty(index = 1754, type = WamType.BOOLEAN)
    Optional<Boolean> serverPreferRelay();

    @WamProperty(index = 2073, type = WamType.BOOLEAN)
    Optional<Boolean> serverPreferredIpv6();

    @WamProperty(index = 1339, type = WamType.TIMER)
    Optional<Instant> serverRecommendedRelayReceivedMs();

    @WamProperty(index = 1266, type = WamType.TIMER)
    Optional<Instant> serverRecommendedToElectedRelayMs();

    @WamProperty(index = 1376, type = WamType.INTEGER)
    OptionalLong setIpVersionCount();

    @WamProperty(index = 1096, type = WamType.FLOAT)
    OptionalDouble sfuAvgDlPlrAtBalancedCongestion();

    @WamProperty(index = 1094, type = WamType.FLOAT)
    OptionalDouble sfuAvgDlPlrAtHighDlCongestion();

    @WamProperty(index = 1092, type = WamType.FLOAT)
    OptionalDouble sfuAvgDlPlrAtHighUlCongestion();

    @WamProperty(index = 1002, type = WamType.FLOAT)
    OptionalDouble sfuAvgLqHqTargetBitrateDiff();

    @WamProperty(index = 1102, type = WamType.INTEGER)
    OptionalLong sfuAvgPeerRttAtBalancedCongestion();

    @WamProperty(index = 1100, type = WamType.INTEGER)
    OptionalLong sfuAvgPeerRttAtHighPeerCongestion();

    @WamProperty(index = 1098, type = WamType.INTEGER)
    OptionalLong sfuAvgPeerRttAtHighSelfCongestion();

    @WamProperty(index = 1101, type = WamType.INTEGER)
    OptionalLong sfuAvgSelfRttAtBalancedCongestion();

    @WamProperty(index = 1099, type = WamType.INTEGER)
    OptionalLong sfuAvgSelfRttAtHighPeerCongestion();

    @WamProperty(index = 1097, type = WamType.INTEGER)
    OptionalLong sfuAvgSelfRttAtHighSelfCongestion();

    @WamProperty(index = 673, type = WamType.FLOAT)
    OptionalDouble sfuAvgTargetBitrate();

    @WamProperty(index = 943, type = WamType.FLOAT)
    OptionalDouble sfuAvgTargetBitrateHq();

    @WamProperty(index = 1095, type = WamType.FLOAT)
    OptionalDouble sfuAvgUlPlrAtBalancedCongestion();

    @WamProperty(index = 1093, type = WamType.FLOAT)
    OptionalDouble sfuAvgUlPlrAtHighDlCongestion();

    @WamProperty(index = 1091, type = WamType.FLOAT)
    OptionalDouble sfuAvgUlPlrAtHighUlCongestion();

    @WamProperty(index = 1075, type = WamType.INTEGER)
    OptionalLong sfuBalancedPktLossAtCongestion();

    @WamProperty(index = 1079, type = WamType.INTEGER)
    OptionalLong sfuBalancedRttAtCongestion();

    @WamProperty(index = 919, type = WamType.FLOAT)
    OptionalDouble sfuBwaAllParticipantDlBwUsedPct();

    @WamProperty(index = 918, type = WamType.FLOAT)
    OptionalDouble sfuBwaAllParticipantUlBwUsedPct();

    @WamProperty(index = 928, type = WamType.INTEGER)
    OptionalLong sfuBwaChangeNumStreamCount();

    @WamProperty(index = 1003, type = WamType.FLOAT)
    OptionalDouble sfuBwaSelfDlBwUsedPct();

    @WamProperty(index = 917, type = WamType.FLOAT)
    OptionalDouble sfuBwaSelfUlBwUsedPct();

    @WamProperty(index = 2541, type = WamType.TIMER)
    Optional<Instant> sfuBwaSimulcastCapabilityWaitTimeMs();

    @WamProperty(index = 920, type = WamType.INTEGER)
    OptionalLong sfuBwaSimulcastDisabledCntReasonBattery();

    @WamProperty(index = 921, type = WamType.INTEGER)
    OptionalLong sfuBwaSimulcastDisabledCntReasonNetMedium();

    @WamProperty(index = 926, type = WamType.TIMER)
    Optional<Instant> sfuBwaVidEncHqStreamScheduledT();

    @WamProperty(index = 925, type = WamType.TIMER)
    Optional<Instant> sfuBwaVidEncLqStreamScheduledT();

    @WamProperty(index = 662, type = WamType.FLOAT)
    OptionalDouble sfuDownlinkAvgCombinedBwe();

    @WamProperty(index = 2440, type = WamType.INTEGER)
    OptionalLong sfuDownlinkAvgConsecutiveUdstPredictionLen();

    @WamProperty(index = 667, type = WamType.FLOAT)
    OptionalDouble sfuDownlinkAvgPktLossPct();

    @WamProperty(index = 661, type = WamType.FLOAT)
    OptionalDouble sfuDownlinkAvgRemoteBwe();

    @WamProperty(index = 660, type = WamType.FLOAT)
    OptionalDouble sfuDownlinkAvgSenderBwe();

    @WamProperty(index = 1876, type = WamType.INTEGER)
    OptionalLong sfuDownlinkDynamicInitBweFallbackCount();

    @WamProperty(index = 1158, type = WamType.FLOAT)
    OptionalDouble sfuDownlinkInitCombinedBwe3s();

    @WamProperty(index = 1159, type = WamType.FLOAT)
    OptionalDouble sfuDownlinkInitPktLossPct3s();

    @WamProperty(index = 1784, type = WamType.FLOAT)
    OptionalDouble sfuDownlinkInitSenderBwe();

    @WamProperty(index = 1775, type = WamType.FLOAT)
    OptionalDouble sfuDownlinkMaxCombinedBwe();

    @WamProperty(index = 2441, type = WamType.BOOLEAN)
    Optional<Boolean> sfuDownlinkMlBweUsedFallbackModel();

    @WamProperty(index = 2442, type = WamType.TIMER)
    Optional<Instant> sfuDownlinkMlUndershootModelAvgInferenceTime();

    @WamProperty(index = 2443, type = WamType.INTEGER)
    OptionalLong sfuDownlinkMlUndershootModelDownloadFailureCount();

    @WamProperty(index = 2454, type = WamType.INTEGER)
    OptionalLong sfuDownlinkMlUndershootModelInferenceFailureCount();

    @WamProperty(index = 2444, type = WamType.TIMER)
    Optional<Instant> sfuDownlinkMlUndershootModelMaxInferenceTime();

    @WamProperty(index = 2445, type = WamType.TIMER)
    Optional<Instant> sfuDownlinkMlUndershootModelMinInferenceTime();

    @WamProperty(index = 2446, type = WamType.ENUM)
    Optional<SfuDownlinkMlUndershootPytorchEdgeLibLoadErrorCode> sfuDownlinkMlUndershootPytorchEdgeLibLoadErrorCode();

    @WamProperty(index = 2447, type = WamType.ENUM)
    Optional<SfuDownlinkMlUndershootPytorchEdgeLibLoadStatus> sfuDownlinkMlUndershootPytorchEdgeLibLoadStatus();

    @WamProperty(index = 2448, type = WamType.TIMER)
    Optional<Instant> sfuDownlinkMlUndershootShimAvgCreationTime();

    @WamProperty(index = 2449, type = WamType.INTEGER)
    OptionalLong sfuDownlinkMlUndershootShimCreationFailureCount();

    @WamProperty(index = 2450, type = WamType.INTEGER)
    OptionalLong sfuDownlinkNonUdstNumPredictions();

    @WamProperty(index = 1999, type = WamType.INTEGER)
    OptionalLong sfuDownlinkPacketPairAvgBitrate();

    @WamProperty(index = 2000, type = WamType.FLOAT)
    OptionalDouble sfuDownlinkPacketPairReliableRatio();

    @WamProperty(index = 2001, type = WamType.FLOAT)
    OptionalDouble sfuDownlinkPacketPairUnderestimateRatio();

    @WamProperty(index = 1981, type = WamType.INTEGER)
    OptionalLong sfuDownlinkRbweLowNoCongCnt();

    @WamProperty(index = 973, type = WamType.FLOAT)
    OptionalDouble sfuDownlinkSbweAvgDowntrend();

    @WamProperty(index = 972, type = WamType.FLOAT)
    OptionalDouble sfuDownlinkSbweAvgUptrend();

    @WamProperty(index = 797, type = WamType.INTEGER)
    OptionalLong sfuDownlinkSbweCeilingCongestionCount();

    @WamProperty(index = 795, type = WamType.INTEGER)
    OptionalLong sfuDownlinkSbweCeilingCount();

    @WamProperty(index = 2176, type = WamType.INTEGER)
    OptionalLong sfuDownlinkSbweCeilingDelayCongestionCount();

    @WamProperty(index = 800, type = WamType.INTEGER)
    OptionalLong sfuDownlinkSbweCeilingMissingRtcpCongestionCount();

    @WamProperty(index = 801, type = WamType.INTEGER)
    OptionalLong sfuDownlinkSbweCeilingNoNewDataReceivedCongestionCount();

    @WamProperty(index = 796, type = WamType.INTEGER)
    OptionalLong sfuDownlinkSbweCeilingPktLossCount();

    @WamProperty(index = 2177, type = WamType.INTEGER)
    OptionalLong sfuDownlinkSbweCeilingRttAndDelayCongestionCount();

    @WamProperty(index = 798, type = WamType.INTEGER)
    OptionalLong sfuDownlinkSbweCeilingRttCongestionCount();

    @WamProperty(index = 799, type = WamType.INTEGER)
    OptionalLong sfuDownlinkSbweCeilingZeroRttCongestionCount();

    @WamProperty(index = 971, type = WamType.INTEGER)
    OptionalLong sfuDownlinkSbweHoldCount();

    @WamProperty(index = 970, type = WamType.INTEGER)
    OptionalLong sfuDownlinkSbweRampDownCount();

    @WamProperty(index = 969, type = WamType.INTEGER)
    OptionalLong sfuDownlinkSbweRampUpCount();

    @WamProperty(index = 958, type = WamType.FLOAT)
    OptionalDouble sfuDownlinkSenderBweDiffStddev();

    @WamProperty(index = 957, type = WamType.FLOAT)
    OptionalDouble sfuDownlinkSenderBweStddev();

    @WamProperty(index = 2451, type = WamType.INTEGER)
    OptionalLong sfuDownlinkUdstAvgPredProb();

    @WamProperty(index = 2163, type = WamType.INTEGER)
    OptionalLong sfuDownlinkUdstMcpAvgStartBitrate();

    @WamProperty(index = 2164, type = WamType.INTEGER)
    OptionalLong sfuDownlinkUdstMcpAvgStopBitrate();

    @WamProperty(index = 2452, type = WamType.INTEGER)
    OptionalLong sfuDownlinkUdstNumPredictions();

    @WamProperty(index = 2453, type = WamType.INTEGER)
    OptionalLong sfuDownlinkUdstSkippedPredictions();

    @WamProperty(index = 2165, type = WamType.INTEGER)
    OptionalLong sfuDownlinkUndershootTriggerMcpCount();

    @WamProperty(index = 1111, type = WamType.TIMER)
    Optional<Instant> sfuFirstRxBandwidthReportTime();

    @WamProperty(index = 883, type = WamType.TIMER)
    Optional<Instant> sfuFirstRxParticipantReportTime();

    @WamProperty(index = 881, type = WamType.TIMER)
    Optional<Instant> sfuFirstRxUplinkReportTime();

    @WamProperty(index = 1074, type = WamType.INTEGER)
    OptionalLong sfuHighDlPktLossAtCongestion();

    @WamProperty(index = 1078, type = WamType.INTEGER)
    OptionalLong sfuHighDlRttAtCongestion();

    @WamProperty(index = 1073, type = WamType.INTEGER)
    OptionalLong sfuHighUlPktLossAtCongestion();

    @WamProperty(index = 1077, type = WamType.INTEGER)
    OptionalLong sfuHighUlRttAtCongestion();

    @WamProperty(index = 674, type = WamType.FLOAT)
    OptionalDouble sfuMaxTargetBitrate();

    @WamProperty(index = 944, type = WamType.FLOAT)
    OptionalDouble sfuMaxTargetBitrateHq();

    @WamProperty(index = 672, type = WamType.FLOAT)
    OptionalDouble sfuMinTargetBitrate();

    @WamProperty(index = 942, type = WamType.FLOAT)
    OptionalDouble sfuMinTargetBitrateHq();

    @WamProperty(index = 813, type = WamType.FLOAT)
    OptionalDouble sfuPeerDownlinkStddevAllCombinedBwe();

    @WamProperty(index = 1110, type = WamType.INTEGER)
    OptionalLong sfuRxBandwidthReportCount();

    @WamProperty(index = 882, type = WamType.INTEGER)
    OptionalLong sfuRxParticipantReportCount();

    @WamProperty(index = 880, type = WamType.INTEGER)
    OptionalLong sfuRxUplinkReportCount();

    @WamProperty(index = 1260, type = WamType.INTEGER)
    OptionalLong sfuServerBwaBrAdjustedForParticipantChange();

    @WamProperty(index = 1261, type = WamType.INTEGER)
    OptionalLong sfuServerBwaBrCappedByUplink();

    @WamProperty(index = 1262, type = WamType.INTEGER)
    OptionalLong sfuServerBwaInvalidSimulcastResult();

    @WamProperty(index = 1263, type = WamType.INTEGER)
    OptionalLong sfuServerBwaLocalBwaRun();

    @WamProperty(index = 1337, type = WamType.INTEGER)
    OptionalLong sfuServerBwaLocalBwaTransition();

    @WamProperty(index = 1338, type = WamType.TIMER)
    Optional<Instant> sfuServerBwaLongestSbwaMissingMs();

    @WamProperty(index = 833, type = WamType.TIMER)
    Optional<Instant> sfuSimulcastAvgDecSessFlipTime();

    @WamProperty(index = 837, type = WamType.TIMER)
    Optional<Instant> sfuSimulcastAvgEncSchedEventUpdateTime();

    @WamProperty(index = 923, type = WamType.INTEGER)
    OptionalLong sfuSimulcastBwaCandidateCnt();

    @WamProperty(index = 874, type = WamType.INTEGER)
    OptionalLong sfuSimulcastBwaDownlinkBottleneckCount();

    @WamProperty(index = 873, type = WamType.INTEGER)
    OptionalLong sfuSimulcastBwaUplinkBottleneckCount();

    @WamProperty(index = 952, type = WamType.TIMER)
    Optional<Instant> sfuSimulcastDecAvgKfRecvTimeSinceFlip();

    @WamProperty(index = 951, type = WamType.FLOAT)
    OptionalDouble sfuSimulcastDecAvgNumReplayedCachedPkt();

    @WamProperty(index = 950, type = WamType.FLOAT)
    OptionalDouble sfuSimulcastDecAvgNumSkippedCachedPkt();

    @WamProperty(index = 953, type = WamType.INTEGER)
    OptionalLong sfuSimulcastDecNumNoKf();

    @WamProperty(index = 744, type = WamType.INTEGER)
    OptionalLong sfuSimulcastDecSessFlipCount();

    @WamProperty(index = 768, type = WamType.INTEGER)
    OptionalLong sfuSimulcastDecSessFlipErrorBitmap();

    @WamProperty(index = 767, type = WamType.INTEGER)
    OptionalLong sfuSimulcastDecSessFlipErrorCount();

    @WamProperty(index = 766, type = WamType.INTEGER)
    OptionalLong sfuSimulcastEncErrorBitmap();

    @WamProperty(index = 735, type = WamType.INTEGER)
    OptionalLong sfuSimulcastEncSchedEventErrorCount();

    @WamProperty(index = 733, type = WamType.INTEGER)
    OptionalLong sfuSimulcastEncSchedEventSuccessUpdateCount();

    @WamProperty(index = 2257, type = WamType.TIMER)
    Optional<Instant> sfuTemporalScalabilityBaseLayerDuration();

    @WamProperty(index = 2258, type = WamType.INTEGER)
    OptionalLong sfuTemporalScalabilityBaseLayerTriggered();

    @WamProperty(index = 2259, type = WamType.BOOLEAN)
    Optional<Boolean> sfuTemporalScalabilityRecvBaseLayerOnly();

    @WamProperty(index = 659, type = WamType.FLOAT)
    OptionalDouble sfuUplinkAvgCombinedBwe();

    @WamProperty(index = 664, type = WamType.FLOAT)
    OptionalDouble sfuUplinkAvgPktLossPct();

    @WamProperty(index = 658, type = WamType.FLOAT)
    OptionalDouble sfuUplinkAvgRemoteBwe();

    @WamProperty(index = 670, type = WamType.FLOAT)
    OptionalDouble sfuUplinkAvgRtt();

    @WamProperty(index = 657, type = WamType.FLOAT)
    OptionalDouble sfuUplinkAvgSenderBwe();

    @WamProperty(index = 2252, type = WamType.INTEGER)
    OptionalLong sfuUplinkFirstPpBitrate();

    @WamProperty(index = 2255, type = WamType.INTEGER)
    OptionalLong sfuUplinkFirstRawPpBitrate();

    @WamProperty(index = 2256, type = WamType.INTEGER)
    OptionalLong sfuUplinkFirstRawPpTime();

    @WamProperty(index = 2253, type = WamType.INTEGER)
    OptionalLong sfuUplinkFirstReliablePpTime();

    @WamProperty(index = 1160, type = WamType.FLOAT)
    OptionalDouble sfuUplinkInitCombinedBwe3s();

    @WamProperty(index = 1161, type = WamType.FLOAT)
    OptionalDouble sfuUplinkInitPktLossPct3s();

    @WamProperty(index = 1785, type = WamType.FLOAT)
    OptionalDouble sfuUplinkInitSenderBwe();

    @WamProperty(index = 1776, type = WamType.FLOAT)
    OptionalDouble sfuUplinkMaxCombinedBwe();

    @WamProperty(index = 671, type = WamType.FLOAT)
    OptionalDouble sfuUplinkMaxRtt();

    @WamProperty(index = 669, type = WamType.FLOAT)
    OptionalDouble sfuUplinkMinRtt();

    @WamProperty(index = 2002, type = WamType.INTEGER)
    OptionalLong sfuUplinkPacketPairAvgBitrate();

    @WamProperty(index = 2003, type = WamType.FLOAT)
    OptionalDouble sfuUplinkPacketPairReliableRatio();

    @WamProperty(index = 2004, type = WamType.FLOAT)
    OptionalDouble sfuUplinkPacketPairUnderestimateRatio();

    @WamProperty(index = 1982, type = WamType.INTEGER)
    OptionalLong sfuUplinkRbweLowNoCongCnt();

    @WamProperty(index = 968, type = WamType.FLOAT)
    OptionalDouble sfuUplinkSbweAvgDowntrend();

    @WamProperty(index = 967, type = WamType.FLOAT)
    OptionalDouble sfuUplinkSbweAvgUptrend();

    @WamProperty(index = 790, type = WamType.INTEGER)
    OptionalLong sfuUplinkSbweCeilingCongestionCount();

    @WamProperty(index = 788, type = WamType.INTEGER)
    OptionalLong sfuUplinkSbweCeilingCount();

    @WamProperty(index = 2178, type = WamType.INTEGER)
    OptionalLong sfuUplinkSbweCeilingDelayCongestionCount();

    @WamProperty(index = 793, type = WamType.INTEGER)
    OptionalLong sfuUplinkSbweCeilingMissingRtcpCongestionCount();

    @WamProperty(index = 794, type = WamType.INTEGER)
    OptionalLong sfuUplinkSbweCeilingNoNewDataReceivedCongestionCount();

    @WamProperty(index = 789, type = WamType.INTEGER)
    OptionalLong sfuUplinkSbweCeilingPktLossCount();

    @WamProperty(index = 2179, type = WamType.INTEGER)
    OptionalLong sfuUplinkSbweCeilingRttAndDelayCongestionCount();

    @WamProperty(index = 791, type = WamType.INTEGER)
    OptionalLong sfuUplinkSbweCeilingRttCongestionCount();

    @WamProperty(index = 792, type = WamType.INTEGER)
    OptionalLong sfuUplinkSbweCeilingZeroRttCongestionCount();

    @WamProperty(index = 966, type = WamType.INTEGER)
    OptionalLong sfuUplinkSbweHoldCount();

    @WamProperty(index = 965, type = WamType.INTEGER)
    OptionalLong sfuUplinkSbweRampDownCount();

    @WamProperty(index = 964, type = WamType.INTEGER)
    OptionalLong sfuUplinkSbweRampUpCount();

    @WamProperty(index = 956, type = WamType.FLOAT)
    OptionalDouble sfuUplinkSenderBweDiffStddev();

    @WamProperty(index = 955, type = WamType.FLOAT)
    OptionalDouble sfuUplinkSenderBweStddev();

    @WamProperty(index = 2166, type = WamType.INTEGER)
    OptionalLong sfuUplinkUdstMcpAvgStartBitrate();

    @WamProperty(index = 2167, type = WamType.INTEGER)
    OptionalLong sfuUplinkUdstMcpAvgStopBitrate();

    @WamProperty(index = 2168, type = WamType.INTEGER)
    OptionalLong sfuUplinkUndershootTriggerMcpCount();

    @WamProperty(index = 2493, type = WamType.TIMER)
    Optional<Instant> shortDec1280wDuration();

    @WamProperty(index = 2494, type = WamType.INTEGER)
    OptionalLong shortDec1280wNum();

    @WamProperty(index = 2495, type = WamType.TIMER)
    Optional<Instant> shortDec640wDuration();

    @WamProperty(index = 2496, type = WamType.INTEGER)
    OptionalLong shortDec640wNum();

    @WamProperty(index = 2497, type = WamType.TIMER)
    Optional<Instant> shortEnc1280wDuration();

    @WamProperty(index = 2498, type = WamType.INTEGER)
    OptionalLong shortEnc1280wNum();

    @WamProperty(index = 2499, type = WamType.TIMER)
    Optional<Instant> shortEnc640wDuration();

    @WamProperty(index = 2500, type = WamType.INTEGER)
    OptionalLong shortEnc640wNum();

    @WamProperty(index = 1780, type = WamType.STRING)
    Optional<String> signalingReflexiveIpPeer();

    @WamProperty(index = 1781, type = WamType.STRING)
    Optional<String> signalingReflexiveIpSelf();

    @WamProperty(index = 982, type = WamType.TIMER)
    Optional<Instant> simulcastReplayVideoRenderFreeze2xT();

    @WamProperty(index = 983, type = WamType.TIMER)
    Optional<Instant> simulcastReplayVideoRenderFreeze4xT();

    @WamProperty(index = 984, type = WamType.TIMER)
    Optional<Instant> simulcastReplayVideoRenderFreeze8xT();

    @WamProperty(index = 981, type = WamType.TIMER)
    Optional<Instant> simulcastReplayVideoRenderFreezeT();

    @WamProperty(index = 1985, type = WamType.INTEGER)
    OptionalLong skipSetVidStreamActiveFromNoneCnt();

    @WamProperty(index = 1986, type = WamType.INTEGER)
    OptionalLong skipSetVidStreamActiveFromPauseCnt();

    @WamProperty(index = 1987, type = WamType.INTEGER)
    OptionalLong skipVidConnOnCreateCnt();

    @WamProperty(index = 748, type = WamType.INTEGER)
    OptionalLong skippedBwaCycles();

    @WamProperty(index = 747, type = WamType.INTEGER)
    OptionalLong skippedBweCycles();

    @WamProperty(index = 2623, type = WamType.FLOAT)
    OptionalDouble smlNadlAudioDupEnabledRatio();

    @WamProperty(index = 2624, type = WamType.INTEGER)
    OptionalLong smlNadlDifferentResultRcvdCount();

    @WamProperty(index = 2625, type = WamType.TIMER)
    Optional<Instant> smlNadlFirstResultDelayMs();

    @WamProperty(index = 2626, type = WamType.INTEGER)
    OptionalLong smlNadlResultRcvdCount();

    @WamProperty(index = 2576, type = WamType.FLOAT)
    OptionalDouble snr();

    @WamProperty(index = 250, type = WamType.INTEGER)
    OptionalLong speakerAvgPower();

    @WamProperty(index = 249, type = WamType.INTEGER)
    OptionalLong speakerMaxPower();

    @WamProperty(index = 248, type = WamType.INTEGER)
    OptionalLong speakerMinPower();

    @WamProperty(index = 864, type = WamType.TIMER)
    Optional<Instant> speakerStartDuration();

    @WamProperty(index = 932, type = WamType.TIMER)
    Optional<Instant> speakerStartToFirstCallbackT();

    @WamProperty(index = 865, type = WamType.TIMER)
    Optional<Instant> speakerStopDuration();

    @WamProperty(index = 1991, type = WamType.INTEGER)
    OptionalLong speakerViewDuration();

    @WamProperty(index = 1313, type = WamType.INTEGER)
    OptionalLong sreRecommendedDiff();

    @WamProperty(index = 1743, type = WamType.INTEGER)
    OptionalLong srtpEncType();

    @WamProperty(index = 2247, type = WamType.INTEGER)
    OptionalLong ssReceiverBweBeforeSs();

    @WamProperty(index = 2248, type = WamType.FLOAT)
    OptionalDouble ssReceiverPlrBeforeSs();

    @WamProperty(index = 1445, type = WamType.INTEGER)
    OptionalLong ssReceiverStartFailCount();

    @WamProperty(index = 1446, type = WamType.INTEGER)
    OptionalLong ssReceiverStartRequestCount();

    @WamProperty(index = 1447, type = WamType.INTEGER)
    OptionalLong ssReceiverStartSuccessCount();

    @WamProperty(index = 1448, type = WamType.INTEGER)
    OptionalLong ssReceiverStopFailCount();

    @WamProperty(index = 1449, type = WamType.INTEGER)
    OptionalLong ssReceiverStopRequestCount();

    @WamProperty(index = 1450, type = WamType.INTEGER)
    OptionalLong ssReceiverStopSuccessCount();

    @WamProperty(index = 1451, type = WamType.INTEGER)
    OptionalLong ssReceiverVersion();

    @WamProperty(index = 2249, type = WamType.INTEGER)
    OptionalLong ssSharerBweBeforeSs();

    @WamProperty(index = 1707, type = WamType.INTEGER)
    OptionalLong ssSharerContentTypeChange();

    @WamProperty(index = 2250, type = WamType.FLOAT)
    OptionalDouble ssSharerPlrBeforeSs();

    @WamProperty(index = 1452, type = WamType.INTEGER)
    OptionalLong ssSharerStartFailCount();

    @WamProperty(index = 1453, type = WamType.INTEGER)
    OptionalLong ssSharerStartRequestCount();

    @WamProperty(index = 1454, type = WamType.INTEGER)
    OptionalLong ssSharerStartSuccessCount();

    @WamProperty(index = 1455, type = WamType.INTEGER)
    OptionalLong ssSharerStopFailCount();

    @WamProperty(index = 1456, type = WamType.INTEGER)
    OptionalLong ssSharerStopRequestCount();

    @WamProperty(index = 1457, type = WamType.INTEGER)
    OptionalLong ssSharerStopSuccessCount();

    @WamProperty(index = 1708, type = WamType.INTEGER)
    OptionalLong ssSharerTextContentBytesEncoded();

    @WamProperty(index = 1709, type = WamType.TIMER)
    Optional<Instant> ssSharerTextContentDuration();

    @WamProperty(index = 1710, type = WamType.INTEGER)
    OptionalLong ssSharerTextContentFrames();

    @WamProperty(index = 1711, type = WamType.INTEGER)
    OptionalLong ssSharerTextContentPixelsEncoded();

    @WamProperty(index = 1712, type = WamType.INTEGER)
    OptionalLong ssSharerTextContentQp();

    @WamProperty(index = 1458, type = WamType.INTEGER)
    OptionalLong ssSharerVersion();

    @WamProperty(index = 1713, type = WamType.INTEGER)
    OptionalLong ssSharerVideoContentBytesEncoded();

    @WamProperty(index = 1714, type = WamType.TIMER)
    Optional<Instant> ssSharerVideoContentDuration();

    @WamProperty(index = 1715, type = WamType.INTEGER)
    OptionalLong ssSharerVideoContentFrames();

    @WamProperty(index = 1716, type = WamType.INTEGER)
    OptionalLong ssSharerVideoContentPixelsEncoded();

    @WamProperty(index = 1717, type = WamType.INTEGER)
    OptionalLong ssSharerVideoContentQp();

    @WamProperty(index = 1459, type = WamType.TIMER)
    Optional<Instant> ssTimeInStaticContentType();

    @WamProperty(index = 1460, type = WamType.TIMER)
    Optional<Instant> ssTimeInVideoContentType();

    @WamProperty(index = 1918, type = WamType.INTEGER)
    OptionalLong startCallDurationMs();

    @WamProperty(index = 900, type = WamType.BOOLEAN)
    Optional<Boolean> startedInitBweProbing();

    @WamProperty(index = 1287, type = WamType.INTEGER)
    OptionalLong streamDroppedPkts();

    @WamProperty(index = 2777, type = WamType.TIMER)
    Optional<Instant> streamDurationDecSs();

    @WamProperty(index = 2778, type = WamType.TIMER)
    Optional<Instant> streamDurationEncHqSs();

    @WamProperty(index = 2779, type = WamType.TIMER)
    Optional<Instant> streamDurationEncSs();

    @WamProperty(index = 1288, type = WamType.TIMER)
    Optional<Instant> streamPausedTimeMs();

    @WamProperty(index = 1289, type = WamType.INTEGER)
    OptionalLong streamTransitionsToPaused();

    @WamProperty(index = 1923, type = WamType.INTEGER)
    OptionalLong stsAfSwitchCnt();

    @WamProperty(index = 1399, type = WamType.INTEGER)
    OptionalLong switchToAvatarDisplayedCount();

    @WamProperty(index = 538, type = WamType.INTEGER)
    OptionalLong switchToDefTriggeredByGoodDefNet();

    @WamProperty(index = 750, type = WamType.INTEGER)
    OptionalLong switchToNonSfu();

    @WamProperty(index = 1057, type = WamType.INTEGER)
    OptionalLong switchToNonSimulcast();

    @WamProperty(index = 749, type = WamType.INTEGER)
    OptionalLong switchToSfu();

    @WamProperty(index = 1056, type = WamType.INTEGER)
    OptionalLong switchToSimulcast();

    @WamProperty(index = 257, type = WamType.INTEGER)
    OptionalLong symmetricNatPortGap();

    @WamProperty(index = 541, type = WamType.INTEGER)
    OptionalLong systemNotificationOfNetChange();

    @WamProperty(index = 2271, type = WamType.FLOAT)
    OptionalDouble systemVolumeDuringIncomingCall();

    @WamProperty(index = 1557, type = WamType.INTEGER)
    OptionalLong tcpAvailableCount();

    @WamProperty(index = 1558, type = WamType.INTEGER)
    OptionalLong tcpAvailableOnUdpCount();

    @WamProperty(index = 1900, type = WamType.INTEGER)
    OptionalLong tcpConnectedCount();

    @WamProperty(index = 1910, type = WamType.STRING)
    Optional<String> tcpFailureStatus();

    @WamProperty(index = 440, type = WamType.TIMER)
    Optional<Instant> telecomFrameworkCallStartDelayT();

    @WamProperty(index = 1801, type = WamType.TIMER)
    Optional<Instant> timeAudRcDynCondTrue();

    @WamProperty(index = 2504, type = WamType.TIMER)
    Optional<Instant> timeBeforeFirstDisabledPeerCameraPauseT();

    @WamProperty(index = 1224, type = WamType.TIMER)
    Optional<Instant> timeCpuUtilizationSamplingInMs();

    @WamProperty(index = 1738, type = WamType.TIMER)
    Optional<Instant> timeDec1280w();

    @WamProperty(index = 2507, type = WamType.TIMER)
    Optional<Instant> timeDec1280wInPinningView();

    @WamProperty(index = 2508, type = WamType.TIMER)
    Optional<Instant> timeDec1280wInSpeakerView();

    @WamProperty(index = 2509, type = WamType.TIMER)
    Optional<Instant> timeDec1280wPinnedUser();

    @WamProperty(index = 2510, type = WamType.TIMER)
    Optional<Instant> timeDec1280wSpeakerInSpeakerView();

    @WamProperty(index = 1739, type = WamType.TIMER)
    Optional<Instant> timeDec160w();

    @WamProperty(index = 1730, type = WamType.TIMER)
    Optional<Instant> timeDec240w();

    @WamProperty(index = 1731, type = WamType.TIMER)
    Optional<Instant> timeDec320w();

    @WamProperty(index = 2511, type = WamType.TIMER)
    Optional<Instant> timeDec320wInPinningView();

    @WamProperty(index = 2512, type = WamType.TIMER)
    Optional<Instant> timeDec320wInSpeakerView();

    @WamProperty(index = 2513, type = WamType.TIMER)
    Optional<Instant> timeDec320wPinnedUser();

    @WamProperty(index = 2514, type = WamType.TIMER)
    Optional<Instant> timeDec320wSpeakerInSpeakerView();

    @WamProperty(index = 1732, type = WamType.TIMER)
    Optional<Instant> timeDec480w();

    @WamProperty(index = 2181, type = WamType.INTEGER)
    OptionalLong timeDec480wDominantSpeaker();

    @WamProperty(index = 2515, type = WamType.TIMER)
    Optional<Instant> timeDec480wInPinningView();

    @WamProperty(index = 2516, type = WamType.TIMER)
    Optional<Instant> timeDec480wInSpeakerView();

    @WamProperty(index = 2517, type = WamType.TIMER)
    Optional<Instant> timeDec480wPinnedUser();

    @WamProperty(index = 2518, type = WamType.TIMER)
    Optional<Instant> timeDec480wSpeakerInSpeakerView();

    @WamProperty(index = 1740, type = WamType.TIMER)
    Optional<Instant> timeDec640w();

    @WamProperty(index = 2519, type = WamType.TIMER)
    Optional<Instant> timeDec640wInPinningView();

    @WamProperty(index = 2520, type = WamType.TIMER)
    Optional<Instant> timeDec640wInSpeakerView();

    @WamProperty(index = 2521, type = WamType.TIMER)
    Optional<Instant> timeDec640wPinnedUser();

    @WamProperty(index = 2522, type = WamType.TIMER)
    Optional<Instant> timeDec640wSpeakerInSpeakerView();

    @WamProperty(index = 1741, type = WamType.TIMER)
    Optional<Instant> timeDec960w();

    @WamProperty(index = 2523, type = WamType.TIMER)
    Optional<Instant> timeDec960wInPinningView();

    @WamProperty(index = 2524, type = WamType.TIMER)
    Optional<Instant> timeDec960wInSpeakerView();

    @WamProperty(index = 2525, type = WamType.TIMER)
    Optional<Instant> timeDec960wPinnedUser();

    @WamProperty(index = 2526, type = WamType.TIMER)
    Optional<Instant> timeDec960wSpeakerInSpeakerView();

    @WamProperty(index = 2780, type = WamType.TIMER)
    Optional<Instant> timeDecReach1080pSs();

    @WamProperty(index = 2154, type = WamType.TIMER)
    Optional<Instant> timeDecReach1280w();

    @WamProperty(index = 2781, type = WamType.TIMER)
    Optional<Instant> timeDecReach1440pSs();

    @WamProperty(index = 2782, type = WamType.TIMER)
    Optional<Instant> timeDecReach2160pSs();

    @WamProperty(index = 2783, type = WamType.TIMER)
    Optional<Instant> timeDecReach320pSs();

    @WamProperty(index = 2784, type = WamType.TIMER)
    Optional<Instant> timeDecReach480pSs();

    @WamProperty(index = 2161, type = WamType.TIMER)
    Optional<Instant> timeDecReach480w();

    @WamProperty(index = 2155, type = WamType.TIMER)
    Optional<Instant> timeDecReach640w();

    @WamProperty(index = 2785, type = WamType.TIMER)
    Optional<Instant> timeDecReach720pSs();

    @WamProperty(index = 2786, type = WamType.TIMER)
    Optional<Instant> timeDecReach960pSs();

    @WamProperty(index = 2364, type = WamType.TIMER)
    Optional<Instant> timeDecReach960w();

    @WamProperty(index = 2787, type = WamType.TIMER)
    Optional<Instant> timeDecSs1080p();

    @WamProperty(index = 2788, type = WamType.TIMER)
    Optional<Instant> timeDecSs1440p();

    @WamProperty(index = 2789, type = WamType.TIMER)
    Optional<Instant> timeDecSs2160p();

    @WamProperty(index = 2790, type = WamType.TIMER)
    Optional<Instant> timeDecSs320p();

    @WamProperty(index = 2791, type = WamType.TIMER)
    Optional<Instant> timeDecSs480p();

    @WamProperty(index = 2792, type = WamType.TIMER)
    Optional<Instant> timeDecSs720p();

    @WamProperty(index = 2793, type = WamType.TIMER)
    Optional<Instant> timeDecSs960p();

    @WamProperty(index = 992, type = WamType.TIMER)
    Optional<Instant> timeEnc1280w();

    @WamProperty(index = 988, type = WamType.TIMER)
    Optional<Instant> timeEnc160w();

    @WamProperty(index = 1676, type = WamType.TIMER)
    Optional<Instant> timeEnc240w();

    @WamProperty(index = 989, type = WamType.TIMER)
    Optional<Instant> timeEnc320w();

    @WamProperty(index = 990, type = WamType.TIMER)
    Optional<Instant> timeEnc480w();

    @WamProperty(index = 991, type = WamType.TIMER)
    Optional<Instant> timeEnc640w();

    @WamProperty(index = 1631, type = WamType.TIMER)
    Optional<Instant> timeEnc960w();

    @WamProperty(index = 2794, type = WamType.TIMER)
    Optional<Instant> timeEncReach1080pSs();

    @WamProperty(index = 2156, type = WamType.TIMER)
    Optional<Instant> timeEncReach1280w();

    @WamProperty(index = 2795, type = WamType.TIMER)
    Optional<Instant> timeEncReach1440pSs();

    @WamProperty(index = 2796, type = WamType.TIMER)
    Optional<Instant> timeEncReach2160pSs();

    @WamProperty(index = 2797, type = WamType.TIMER)
    Optional<Instant> timeEncReach320pSs();

    @WamProperty(index = 2798, type = WamType.TIMER)
    Optional<Instant> timeEncReach480pSs();

    @WamProperty(index = 2162, type = WamType.TIMER)
    Optional<Instant> timeEncReach480w();

    @WamProperty(index = 2157, type = WamType.TIMER)
    Optional<Instant> timeEncReach640w();

    @WamProperty(index = 2799, type = WamType.TIMER)
    Optional<Instant> timeEncReach720pSs();

    @WamProperty(index = 2800, type = WamType.TIMER)
    Optional<Instant> timeEncReach960pSs();

    @WamProperty(index = 2365, type = WamType.TIMER)
    Optional<Instant> timeEncReach960w();

    @WamProperty(index = 2801, type = WamType.TIMER)
    Optional<Instant> timeEncSs1080p();

    @WamProperty(index = 2802, type = WamType.TIMER)
    Optional<Instant> timeEncSs1080pHq();

    @WamProperty(index = 2803, type = WamType.TIMER)
    Optional<Instant> timeEncSs1440p();

    @WamProperty(index = 2804, type = WamType.TIMER)
    Optional<Instant> timeEncSs1440pHq();

    @WamProperty(index = 2805, type = WamType.TIMER)
    Optional<Instant> timeEncSs2160p();

    @WamProperty(index = 2806, type = WamType.TIMER)
    Optional<Instant> timeEncSs2160pHq();

    @WamProperty(index = 2807, type = WamType.TIMER)
    Optional<Instant> timeEncSs320p();

    @WamProperty(index = 2808, type = WamType.TIMER)
    Optional<Instant> timeEncSs320pHq();

    @WamProperty(index = 2809, type = WamType.TIMER)
    Optional<Instant> timeEncSs480p();

    @WamProperty(index = 2810, type = WamType.TIMER)
    Optional<Instant> timeEncSs480pHq();

    @WamProperty(index = 2811, type = WamType.TIMER)
    Optional<Instant> timeEncSs720p();

    @WamProperty(index = 2812, type = WamType.TIMER)
    Optional<Instant> timeEncSs720pHq();

    @WamProperty(index = 2813, type = WamType.TIMER)
    Optional<Instant> timeEncSs960p();

    @WamProperty(index = 2814, type = WamType.TIMER)
    Optional<Instant> timeEncSs960pHq();

    @WamProperty(index = 2668, type = WamType.INTEGER)
    OptionalLong timeFirstAnrSinceCallStartSec();

    @WamProperty(index = 2468, type = WamType.TIMER)
    Optional<Instant> timeIn2xDownscaleSs();

    @WamProperty(index = 2469, type = WamType.TIMER)
    Optional<Instant> timeIn4xDownscaleSs();

    @WamProperty(index = 2470, type = WamType.TIMER)
    Optional<Instant> timeInNoDownscaleSs();

    @WamProperty(index = 530, type = WamType.TIMER)
    Optional<Instant> timeOnNonDefNetwork();

    @WamProperty(index = 531, type = WamType.TIMER)
    Optional<Instant> timeOnNonDefNetworkPerSegment();

    @WamProperty(index = 2227, type = WamType.TIMER)
    Optional<Instant> timePipRecoverTo480p();

    @WamProperty(index = 2228, type = WamType.TIMER)
    Optional<Instant> timePipRecoverTo720p();

    @WamProperty(index = 2066, type = WamType.TIMER)
    Optional<Instant> timeSinceLastPushReceivedMs();

    @WamProperty(index = 715, type = WamType.TIMER)
    Optional<Instant> timeSinceLastRtpToCallEndInMsec();

    @WamProperty(index = 1267, type = WamType.TIMER)
    Optional<Instant> timeToFirstElectedRelayMs();

    @WamProperty(index = 718, type = WamType.TIMER)
    Optional<Instant> timeVidRcDynCondTrue();

    @WamProperty(index = 1126, type = WamType.INTEGER)
    OptionalLong totalAqsMsgSent();

    @WamProperty(index = 723, type = WamType.TIMER)
    Optional<Instant> totalAudioFrameLossMs();

    @WamProperty(index = 449, type = WamType.FLOAT)
    OptionalDouble totalBytesOnNonDefCell();

    @WamProperty(index = 1461, type = WamType.INTEGER)
    OptionalLong totalFramesCapturedInLast10secSs();

    @WamProperty(index = 1462, type = WamType.INTEGER)
    OptionalLong totalFramesCapturedSs();

    @WamProperty(index = 1463, type = WamType.INTEGER)
    OptionalLong totalFramesRenderedInLast10secSs();

    @WamProperty(index = 1464, type = WamType.INTEGER)
    OptionalLong totalFramesRenderedSs();

    @WamProperty(index = 2011, type = WamType.INTEGER)
    OptionalLong totalMemoryGb();

    @WamProperty(index = 573, type = WamType.TIMER)
    Optional<Instant> totalTimeVidUlAutoPause();

    @WamProperty(index = 898, type = WamType.TIMER)
    Optional<Instant> trafficShaperAvgAudioQueueMs();

    @WamProperty(index = 242, type = WamType.TIMER)
    Optional<Instant> trafficShaperAvgQueueMs();

    @WamProperty(index = 899, type = WamType.TIMER)
    Optional<Instant> trafficShaperAvgVideoQueueMs();

    @WamProperty(index = 240, type = WamType.INTEGER)
    OptionalLong trafficShaperMaxDelayViolations();

    @WamProperty(index = 241, type = WamType.INTEGER)
    OptionalLong trafficShaperMinDelayViolations();

    @WamProperty(index = 237, type = WamType.INTEGER)
    OptionalLong trafficShaperOverflowCount();

    @WamProperty(index = 238, type = WamType.INTEGER)
    OptionalLong trafficShaperQueueEmptyCount();

    @WamProperty(index = 239, type = WamType.INTEGER)
    OptionalLong trafficShaperQueuedPacketCount();

    @WamProperty(index = 552, type = WamType.TIMER)
    Optional<Instant> transportCurTimeInMsecAsyncWriteWaitingInQueue();

    @WamProperty(index = 1867, type = WamType.INTEGER)
    OptionalLong transportDebugBitmap();

    @WamProperty(index = 555, type = WamType.INTEGER)
    OptionalLong transportLastSendOsError();

    @WamProperty(index = 1924, type = WamType.TIMER)
    Optional<Instant> transportMaxDnsResolveDelayMs();

    @WamProperty(index = 1925, type = WamType.TIMER)
    Optional<Instant> transportMaxNegRttMs();

    @WamProperty(index = 580, type = WamType.INTEGER)
    OptionalLong transportNumAsyncWriteDispatched();

    @WamProperty(index = 551, type = WamType.INTEGER)
    OptionalLong transportNumAsyncWriteQueued();

    @WamProperty(index = 699, type = WamType.INTEGER)
    OptionalLong transportOvershoot10PercCount();

    @WamProperty(index = 700, type = WamType.INTEGER)
    OptionalLong transportOvershoot20PercCount();

    @WamProperty(index = 701, type = WamType.INTEGER)
    OptionalLong transportOvershoot40PercCount();

    @WamProperty(index = 708, type = WamType.INTEGER)
    OptionalLong transportOvershootLongestStreakS();

    @WamProperty(index = 704, type = WamType.INTEGER)
    OptionalLong transportOvershootSinceLast10sCount();

    @WamProperty(index = 705, type = WamType.INTEGER)
    OptionalLong transportOvershootSinceLast15sCount();

    @WamProperty(index = 702, type = WamType.INTEGER)
    OptionalLong transportOvershootSinceLast1sCount();

    @WamProperty(index = 706, type = WamType.INTEGER)
    OptionalLong transportOvershootSinceLast30sCount();

    @WamProperty(index = 703, type = WamType.INTEGER)
    OptionalLong transportOvershootSinceLast5sCount();

    @WamProperty(index = 709, type = WamType.FLOAT)
    OptionalDouble transportOvershootStreakAvgS();

    @WamProperty(index = 707, type = WamType.FLOAT)
    OptionalDouble transportOvershootTimeBetweenAvgS();

    @WamProperty(index = 2050, type = WamType.INTEGER)
    OptionalLong transportP2pPeerMsgCnt();

    @WamProperty(index = 1886, type = WamType.INTEGER)
    OptionalLong transportRestartCnt();

    @WamProperty(index = 1887, type = WamType.INTEGER)
    OptionalLong transportRestartReasonBitmap();

    @WamProperty(index = 2326, type = WamType.INTEGER)
    OptionalLong transportRtpCbNotAttachedPktSkipCnt();

    @WamProperty(index = 2051, type = WamType.INTEGER)
    OptionalLong transportRtpZeroPayloadCnt();

    @WamProperty(index = 2052, type = WamType.INTEGER)
    OptionalLong transportRxAllocTotalCnt();

    @WamProperty(index = 2093, type = WamType.INTEGER)
    OptionalLong transportRxHistoricalRelayPktCnt();

    @WamProperty(index = 2053, type = WamType.BOOLEAN)
    Optional<Boolean> transportRxRelaySetImplictlyToTx();

    @WamProperty(index = 2418, type = WamType.INTEGER)
    OptionalLong transportRxWarpPktOnInvalidRelayAddrCnt();

    @WamProperty(index = 556, type = WamType.INTEGER)
    OptionalLong transportSendErrorCount();

    @WamProperty(index = 2283, type = WamType.INTEGER)
    OptionalLong transportSenderSubscriptionBaseLayerTriggered();

    @WamProperty(index = 1059, type = WamType.INTEGER)
    OptionalLong transportSplitterRxErrCnt();

    @WamProperty(index = 1058, type = WamType.INTEGER)
    OptionalLong transportSplitterTxErrCnt();

    @WamProperty(index = 1141, type = WamType.INTEGER)
    OptionalLong transportSrtcpRxRejectedPktCnt();

    @WamProperty(index = 1996, type = WamType.INTEGER)
    OptionalLong transportSrtpRtpCbNotAttachedOnCgu();

    @WamProperty(index = 2419, type = WamType.INTEGER)
    OptionalLong transportSrtpRxAuthFail();

    @WamProperty(index = 1997, type = WamType.INTEGER)
    OptionalLong transportSrtpRxAuthFailOnCgu();

    @WamProperty(index = 1570, type = WamType.INTEGER)
    OptionalLong transportSrtpRxInitRejNoDupPktCnt();

    @WamProperty(index = 1038, type = WamType.INTEGER)
    OptionalLong transportSrtpRxMaxPktSize();

    @WamProperty(index = 763, type = WamType.FLOAT)
    OptionalDouble transportSrtpRxRejectedBitrate();

    @WamProperty(index = 772, type = WamType.INTEGER)
    OptionalLong transportSrtpRxRejectedDupPktCnt();

    @WamProperty(index = 762, type = WamType.INTEGER)
    OptionalLong transportSrtpRxRejectedPktCnt();

    @WamProperty(index = 2019, type = WamType.INTEGER)
    OptionalLong transportSrtpTxCtxNotFound();

    @WamProperty(index = 774, type = WamType.INTEGER)
    OptionalLong transportSrtpTxFailedPktCnt();

    @WamProperty(index = 773, type = WamType.INTEGER)
    OptionalLong transportSrtpTxMaxPktSize();

    @WamProperty(index = 1998, type = WamType.INTEGER)
    OptionalLong transportSrtpUnknownSsrcOnCgu();

    @WamProperty(index = 554, type = WamType.INTEGER)
    OptionalLong transportTotalNumSendOsError();

    @WamProperty(index = 553, type = WamType.TIMER)
    Optional<Instant> transportTotalTimeInMsecAsyncWriteQueueToDispatch();

    @WamProperty(index = 710, type = WamType.INTEGER)
    OptionalLong transportUndershoot10PercCount();

    @WamProperty(index = 711, type = WamType.INTEGER)
    OptionalLong transportUndershoot20PercCount();

    @WamProperty(index = 712, type = WamType.INTEGER)
    OptionalLong transportUndershoot40PercCount();

    @WamProperty(index = 536, type = WamType.INTEGER)
    OptionalLong triggeredButDataLimitReached();

    @WamProperty(index = 1545, type = WamType.FLOAT)
    OptionalDouble txFailedEncCheckBytes();

    @WamProperty(index = 1546, type = WamType.INTEGER)
    OptionalLong txFailedEncCheckPackets();

    @WamProperty(index = 1699, type = WamType.INTEGER)
    OptionalLong txHbhFecBitrateKbps();

    @WamProperty(index = 1962, type = WamType.INTEGER)
    OptionalLong txHbhFecSrtpBitrateKbps();

    @WamProperty(index = 2356, type = WamType.INTEGER)
    OptionalLong txLowerHandCount();

    @WamProperty(index = 289, type = WamType.INTEGER)
    OptionalLong txProbeCountSuccess();

    @WamProperty(index = 288, type = WamType.INTEGER)
    OptionalLong txProbeCountTotal();

    @WamProperty(index = 2357, type = WamType.INTEGER)
    OptionalLong txRaiseHandCount();

    @WamProperty(index = 2358, type = WamType.INTEGER)
    OptionalLong txRaiseOrLowerHandErrorCount();

    @WamProperty(index = 2338, type = WamType.INTEGER)
    OptionalLong txReactionCount();

    @WamProperty(index = 2339, type = WamType.INTEGER)
    OptionalLong txReactionErrorCount();

    @WamProperty(index = 839, type = WamType.TIMER)
    Optional<Instant> txRelayRebindLatencyMs();

    @WamProperty(index = 840, type = WamType.TIMER)
    Optional<Instant> txRelayResetLatencyMs();

    @WamProperty(index = 1519, type = WamType.INTEGER)
    OptionalLong txStoppedCount();

    @WamProperty(index = 1650, type = WamType.INTEGER)
    OptionalLong txSubscriptionChangeCount();

    @WamProperty(index = 144, type = WamType.FLOAT)
    OptionalDouble txTotalBitrate();

    @WamProperty(index = 142, type = WamType.FLOAT)
    OptionalDouble txTotalBytes();

    @WamProperty(index = 293, type = WamType.FLOAT)
    OptionalDouble txTpFbBitrate();

    @WamProperty(index = 2471, type = WamType.INTEGER)
    OptionalLong uaqcNumStateTransitions();

    @WamProperty(index = 2472, type = WamType.TIMER)
    Optional<Instant> uaqcTimeInBwManagedStateMs();

    @WamProperty(index = 2473, type = WamType.TIMER)
    Optional<Instant> uaqcTimeInDrainStateMs();

    @WamProperty(index = 2474, type = WamType.TIMER)
    Optional<Instant> uaqcTimeInHighQualityStateMs();

    @WamProperty(index = 2475, type = WamType.TIMER)
    Optional<Instant> uaqcTimeInLowStateMs();

    @WamProperty(index = 2476, type = WamType.TIMER)
    Optional<Instant> uaqcTimeInProbingStateMs();

    @WamProperty(index = 1559, type = WamType.INTEGER)
    OptionalLong udpAvailableCount();

    @WamProperty(index = 1560, type = WamType.INTEGER)
    OptionalLong udpAvailableOnTcpCount();

    @WamProperty(index = 1791, type = WamType.INTEGER)
    OptionalLong udstAvgPredProb();

    @WamProperty(index = 1792, type = WamType.INTEGER)
    OptionalLong udstMcpAvgEndBitrate();

    @WamProperty(index = 1793, type = WamType.INTEGER)
    OptionalLong udstMcpAvgStartBitrate();

    @WamProperty(index = 1794, type = WamType.INTEGER)
    OptionalLong udstNumPredictions();

    @WamProperty(index = 1795, type = WamType.INTEGER)
    OptionalLong udstSkippedPredictions();

    @WamProperty(index = 2060, type = WamType.INTEGER)
    OptionalLong uiReconnecting();

    @WamProperty(index = 1365, type = WamType.FLOAT)
    OptionalDouble ulOnlyHighPlrPct();

    @WamProperty(index = 2068, type = WamType.STRING)
    Optional<String> unboundRelayList();

    @WamProperty(index = 1952, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 2350, type = WamType.INTEGER)
    OptionalLong unknownContactVideoUpgradeCount();

    @WamProperty(index = 1576, type = WamType.INTEGER)
    OptionalLong unknownRelayMessageCnt();

    @WamProperty(index = 1465, type = WamType.INTEGER)
    OptionalLong uplinkOvershootCountSs();

    @WamProperty(index = 1970, type = WamType.INTEGER)
    OptionalLong uplinkSbweRttSlopeCongestionCount();

    @WamProperty(index = 1466, type = WamType.INTEGER)
    OptionalLong uplinkUndershootCountSs();

    @WamProperty(index = 341, type = WamType.INTEGER)
    OptionalLong usedInitTxBitrate();

    @WamProperty(index = 1150, type = WamType.INTEGER)
    OptionalLong usedIpv4Count();

    @WamProperty(index = 1151, type = WamType.INTEGER)
    OptionalLong usedIpv6Count();

    @WamProperty(index = 87, type = WamType.STRING)
    Optional<String> userDescription();

    @WamProperty(index = 88, type = WamType.INTEGER)
    OptionalLong userProblems();

    @WamProperty(index = 86, type = WamType.INTEGER)
    OptionalLong userRating();

    @WamProperty(index = 1877, type = WamType.INTEGER)
    OptionalLong userRedialCount();

    @WamProperty(index = 2415, type = WamType.STRING)
    Optional<String> uvmCellId();

    @WamProperty(index = 2745, type = WamType.FLOAT)
    OptionalDouble uvqAvgInferenceLatencyMs();

    @WamProperty(index = 2815, type = WamType.FLOAT)
    OptionalDouble uvqAvgNormalizationLatencyMs();

    @WamProperty(index = 2816, type = WamType.FLOAT)
    OptionalDouble uvqAvgPatchExtractionLatencyMs();

    @WamProperty(index = 2746, type = WamType.FLOAT)
    OptionalDouble uvqAvgScore();

    @WamProperty(index = 2747, type = WamType.INTEGER)
    OptionalLong uvqDownloadFailureCount();

    @WamProperty(index = 2748, type = WamType.INTEGER)
    OptionalLong uvqDownloadSuccessCount();

    @WamProperty(index = 2749, type = WamType.INTEGER)
    OptionalLong uvqInferenceFailureCount();

    @WamProperty(index = 2750, type = WamType.INTEGER)
    OptionalLong uvqInferenceSuccessCount();

    @WamProperty(index = 2751, type = WamType.INTEGER)
    OptionalLong uvqLoadFailureCount();

    @WamProperty(index = 2752, type = WamType.INTEGER)
    OptionalLong uvqLoadSuccessCount();

    @WamProperty(index = 2753, type = WamType.FLOAT)
    OptionalDouble uvqMaxScore();

    @WamProperty(index = 2754, type = WamType.FLOAT)
    OptionalDouble uvqMinScore();

    @WamProperty(index = 2755, type = WamType.FLOAT)
    OptionalDouble uvqP50Score();

    @WamProperty(index = 2756, type = WamType.FLOAT)
    OptionalDouble uvqP5Score();

    @WamProperty(index = 2757, type = WamType.FLOAT)
    OptionalDouble uvqP95Score();

    @WamProperty(index = 1777, type = WamType.INTEGER)
    OptionalLong uwpCameraLastDeviceHresultError();

    @WamProperty(index = 1778, type = WamType.TIMER)
    Optional<Instant> uwpCameraMediacaptureTime();

    @WamProperty(index = 1826, type = WamType.FLOAT)
    OptionalDouble uwpSystemVolumeDuringIncomingCall();

    @WamProperty(index = 1827, type = WamType.STRING)
    Optional<String> uwpVoipCameraLastErrorDeviceName();

    @WamProperty(index = 1828, type = WamType.STRING)
    Optional<String> uwpVoipCameraLastErrorManufacturerName();

    @WamProperty(index = 1829, type = WamType.INTEGER)
    OptionalLong uwpVoipCameraTotalErrors();

    @WamProperty(index = 1830, type = WamType.TIMER)
    Optional<Instant> uwpVoipInitTime();

    @WamProperty(index = 1831, type = WamType.STRING)
    Optional<String> uwpVoipLastAppCrashReason();

    @WamProperty(index = 1832, type = WamType.STRING)
    Optional<String> uwpVoipLastNativeCrashReason();

    @WamProperty(index = 1833, type = WamType.STRING)
    Optional<String> uwpVoipMicLastErrorDeviceName();

    @WamProperty(index = 1834, type = WamType.STRING)
    Optional<String> uwpVoipMicLastErrorManufacturerName();

    @WamProperty(index = 1835, type = WamType.INTEGER)
    OptionalLong uwpVoipMicTotalErrors();

    @WamProperty(index = 1836, type = WamType.INTEGER)
    OptionalLong uwpVoipNumAnrEvents();

    @WamProperty(index = 1837, type = WamType.INTEGER)
    OptionalLong uwpVoipNumCriticalEvents();

    @WamProperty(index = 1838, type = WamType.INTEGER)
    OptionalLong uwpVoipNumUnhandledExceptionEvents();

    @WamProperty(index = 1839, type = WamType.INTEGER)
    OptionalLong uwpVoipTotalCameraDevices();

    @WamProperty(index = 1840, type = WamType.INTEGER)
    OptionalLong uwpVoipTotalMicDevices();

    @WamProperty(index = 1841, type = WamType.TIMER)
    Optional<Instant> uwpVoipWindowIncomingAcceptToCallLayoutTime();

    @WamProperty(index = 1842, type = WamType.TIMER)
    Optional<Instant> uwpVoipWindowIncomingOfferToLayoutTime();

    @WamProperty(index = 1843, type = WamType.TIMER)
    Optional<Instant> uwpVoipWindowOutgoingLaunchTime();

    @WamProperty(index = 1144, type = WamType.TIMER)
    Optional<Instant> v2vAudioFrameLoss2xMs();

    @WamProperty(index = 1146, type = WamType.TIMER)
    Optional<Instant> v2vAudioFrameLoss8xMs();

    @WamProperty(index = 1147, type = WamType.INTEGER)
    OptionalLong v2vAudioLossPeriodCount();

    @WamProperty(index = 1148, type = WamType.TIMER)
    Optional<Instant> v2vTotalAudioFrameLossMs();

    @WamProperty(index = 1888, type = WamType.STRING)
    Optional<String> v4RelayConnQualityStat();

    @WamProperty(index = 1889, type = WamType.STRING)
    Optional<String> v6RelayConnQualityStat();

    @WamProperty(index = 2465, type = WamType.TIMER)
    Optional<Instant> vcLatency();

    @WamProperty(index = 2466, type = WamType.TIMER)
    Optional<Instant> vcLatencyConnection();

    @WamProperty(index = 2467, type = WamType.TIMER)
    Optional<Instant> vcLatencyUi();

    @WamProperty(index = 1121, type = WamType.TIMER)
    Optional<Instant> vidAvgBurstyPktLossLength();

    @WamProperty(index = 1122, type = WamType.TIMER)
    Optional<Instant> vidAvgRandomPktLossLength();

    @WamProperty(index = 1123, type = WamType.TIMER)
    Optional<Instant> vidBurstyPktLossTime();

    @WamProperty(index = 688, type = WamType.INTEGER)
    OptionalLong vidCorrectRetxDetectPcnt();

    @WamProperty(index = 2589, type = WamType.INTEGER)
    OptionalLong vidIsPausedOnCreateAndConnectFuncCnt();

    @WamProperty(index = 2590, type = WamType.INTEGER)
    OptionalLong vidIsPausedOnCreateFuncCnt();

    @WamProperty(index = 1063, type = WamType.FLOAT)
    OptionalDouble vidJbDiscards();

    @WamProperty(index = 1064, type = WamType.FLOAT)
    OptionalDouble vidJbEmpties();

    @WamProperty(index = 1065, type = WamType.FLOAT)
    OptionalDouble vidJbGets();

    @WamProperty(index = 1061, type = WamType.FLOAT)
    OptionalDouble vidJbLost();

    @WamProperty(index = 1066, type = WamType.FLOAT)
    OptionalDouble vidJbPuts();

    @WamProperty(index = 1067, type = WamType.FLOAT)
    OptionalDouble vidJbResets();

    @WamProperty(index = 1124, type = WamType.INTEGER)
    OptionalLong vidNumRandToBursty();

    @WamProperty(index = 698, type = WamType.INTEGER)
    OptionalLong vidNumRetxDropped();

    @WamProperty(index = 757, type = WamType.INTEGER)
    OptionalLong vidNumRxRetx();

    @WamProperty(index = 693, type = WamType.STRING)
    Optional<String> vidPktRxState0();

    @WamProperty(index = 2591, type = WamType.INTEGER)
    OptionalLong vidPortResumeSubCnt();

    @WamProperty(index = 1125, type = WamType.TIMER)
    Optional<Instant> vidRandomPktLossTime();

    @WamProperty(index = 589, type = WamType.BOOLEAN)
    Optional<Boolean> vidUlAutoPausedAtCallEnd();

    @WamProperty(index = 590, type = WamType.TIMER)
    Optional<Instant> vidUlTimeSinceAutoPauseAtCallEnd();

    @WamProperty(index = 716, type = WamType.INTEGER)
    OptionalLong vidWrongRetxDetectPcnt();

    @WamProperty(index = 276, type = WamType.TIMER)
    Optional<Instant> videoActiveTime();

    @WamProperty(index = 1039, type = WamType.INTEGER)
    OptionalLong videoAheadNumAvSyncDiscardFrames();

    @WamProperty(index = 1687, type = WamType.INTEGER)
    OptionalLong videoAv1Time();

    @WamProperty(index = 484, type = WamType.TIMER)
    Optional<Instant> videoAveDelayLtrp();

    @WamProperty(index = 1816, type = WamType.INTEGER)
    OptionalLong videoAverageBitrateDiffSbwaToClientBwa();

    @WamProperty(index = 1817, type = WamType.INTEGER)
    OptionalLong videoAverageLqBitrateFromSbwa();

    @WamProperty(index = 390, type = WamType.FLOAT)
    OptionalDouble videoAvgCombPsnr();

    @WamProperty(index = 2925, type = WamType.FLOAT)
    OptionalDouble videoAvgCombPsnrSs();

    @WamProperty(index = 1467, type = WamType.INTEGER)
    OptionalLong videoAvgEncKfQpSs();

    @WamProperty(index = 1468, type = WamType.INTEGER)
    OptionalLong videoAvgEncPFrameQpSs();

    @WamProperty(index = 410, type = WamType.FLOAT)
    OptionalDouble videoAvgEncodingPsnr();

    @WamProperty(index = 2926, type = WamType.FLOAT)
    OptionalDouble videoAvgEncodingPsnrSs();

    @WamProperty(index = 408, type = WamType.FLOAT)
    OptionalDouble videoAvgScalingPsnr();

    @WamProperty(index = 2927, type = WamType.FLOAT)
    OptionalDouble videoAvgScalingPsnrSs();

    @WamProperty(index = 186, type = WamType.FLOAT)
    OptionalDouble videoAvgSenderBwe();

    @WamProperty(index = 184, type = WamType.FLOAT)
    OptionalDouble videoAvgTargetBitrate();

    @WamProperty(index = 828, type = WamType.FLOAT)
    OptionalDouble videoAvgTargetBitrateHq();

    @WamProperty(index = 1469, type = WamType.FLOAT)
    OptionalDouble videoAvgTargetBitrateHqSs();

    @WamProperty(index = 1491, type = WamType.FLOAT)
    OptionalDouble videoAvgTargetBitrateSs();

    @WamProperty(index = 1818, type = WamType.FLOAT)
    OptionalDouble videoAvgTotalTargetBitrate();

    @WamProperty(index = 1470, type = WamType.FLOAT)
    OptionalDouble videoAvgTotalTargetBitrateSs();

    @WamProperty(index = 1040, type = WamType.INTEGER)
    OptionalLong videoBehindNumAvSyncDiscardFrames();

    @WamProperty(index = 222, type = WamType.INTEGER)
    OptionalLong videoCaptureAvgFps();

    @WamProperty(index = 226, type = WamType.TIMER)
    Optional<Instant> videoCaptureConverterTs();

    @WamProperty(index = 496, type = WamType.INTEGER)
    OptionalLong videoCaptureFrameOverwriteCount();

    @WamProperty(index = 228, type = WamType.INTEGER)
    OptionalLong videoCaptureHeight();

    @WamProperty(index = 1471, type = WamType.INTEGER)
    OptionalLong videoCaptureHeightSs();

    @WamProperty(index = 1863, type = WamType.INTEGER)
    OptionalLong videoCapturePortRecreateCount();

    @WamProperty(index = 227, type = WamType.INTEGER)
    OptionalLong videoCaptureWidth();

    @WamProperty(index = 1472, type = WamType.INTEGER)
    OptionalLong videoCaptureWidthSs();

    @WamProperty(index = 401, type = WamType.INTEGER)
    OptionalLong videoCodecScheme();

    @WamProperty(index = 303, type = WamType.INTEGER)
    OptionalLong videoCodecSubType();

    @WamProperty(index = 236, type = WamType.INTEGER)
    OptionalLong videoCodecType();

    @WamProperty(index = 2430, type = WamType.FLOAT)
    OptionalDouble videoCombPsnrP5();

    @WamProperty(index = 2431, type = WamType.FLOAT)
    OptionalDouble videoCombPsnrP50();

    @WamProperty(index = 2432, type = WamType.FLOAT)
    OptionalDouble videoCombPsnrP95();

    @WamProperty(index = 3030, type = WamType.FLOAT)
    OptionalDouble videoCompositeBrightnessAvg();

    @WamProperty(index = 207, type = WamType.FLOAT)
    OptionalDouble videoDecAvgFps();

    @WamProperty(index = 1473, type = WamType.FLOAT)
    OptionalDouble videoDecAvgFpsSs();

    @WamProperty(index = 205, type = WamType.INTEGER)
    OptionalLong videoDecColorId();

    @WamProperty(index = 419, type = WamType.INTEGER)
    OptionalLong videoDecCrcMismatchFrames();

    @WamProperty(index = 174, type = WamType.INTEGER)
    OptionalLong videoDecErrorFrames();

    @WamProperty(index = 1688, type = WamType.INTEGER)
    OptionalLong videoDecErrorFramesAv1();

    @WamProperty(index = 714, type = WamType.INTEGER)
    OptionalLong videoDecErrorFramesCodecSwitch();

    @WamProperty(index = 713, type = WamType.INTEGER)
    OptionalLong videoDecErrorFramesDuplicate();

    @WamProperty(index = 680, type = WamType.INTEGER)
    OptionalLong videoDecErrorFramesH264();

    @WamProperty(index = 2630, type = WamType.INTEGER)
    OptionalLong videoDecErrorFramesH265();

    @WamProperty(index = 478, type = WamType.INTEGER)
    OptionalLong videoDecErrorFramesIgnoreConsecutive();

    @WamProperty(index = 682, type = WamType.INTEGER)
    OptionalLong videoDecErrorFramesOutoforder();

    @WamProperty(index = 810, type = WamType.INTEGER)
    OptionalLong videoDecErrorFramesSpsPpsMissingAfterResolutionSwitch();

    @WamProperty(index = 2260, type = WamType.INTEGER)
    OptionalLong videoDecErrorFramesVav1();

    @WamProperty(index = 681, type = WamType.INTEGER)
    OptionalLong videoDecErrorFramesVp8();

    @WamProperty(index = 462, type = WamType.INTEGER)
    OptionalLong videoDecErrorLtrpFramesVp8();

    @WamProperty(index = 480, type = WamType.INTEGER)
    OptionalLong videoDecErrorLtrpFramesVp8NoLtr();

    @WamProperty(index = 1084, type = WamType.INTEGER)
    OptionalLong videoDecFatalErrorNum();

    @WamProperty(index = 172, type = WamType.INTEGER)
    OptionalLong videoDecInputFrames();

    @WamProperty(index = 175, type = WamType.INTEGER)
    OptionalLong videoDecKeyframes();

    @WamProperty(index = 223, type = WamType.TIMER)
    Optional<Instant> videoDecLatency();

    @WamProperty(index = 210, type = WamType.INTEGER)
    OptionalLong videoDecLostPackets();

    @WamProperty(index = 461, type = WamType.INTEGER)
    OptionalLong videoDecLtrpFramesVp8();

    @WamProperty(index = 204, type = WamType.INTEGER)
    OptionalLong videoDecName();

    @WamProperty(index = 2729, type = WamType.INTEGER)
    OptionalLong videoDecNoRtcpSessionNum();

    @WamProperty(index = 616, type = WamType.INTEGER)
    OptionalLong videoDecNumSkippedFramesVp8();

    @WamProperty(index = 173, type = WamType.INTEGER)
    OptionalLong videoDecOutputFrames();

    @WamProperty(index = 1474, type = WamType.INTEGER)
    OptionalLong videoDecOutputFramesInLast10secSs();

    @WamProperty(index = 1475, type = WamType.INTEGER)
    OptionalLong videoDecOutputFramesSs();

    @WamProperty(index = 206, type = WamType.INTEGER)
    OptionalLong videoDecRestart();

    @WamProperty(index = 209, type = WamType.INTEGER)
    OptionalLong videoDecSkipPackets();

    @WamProperty(index = 232, type = WamType.INTEGER)
    OptionalLong videoDecodePausedCount();

    @WamProperty(index = 1726, type = WamType.INTEGER)
    OptionalLong videoDisablingActionReversalCount();

    @WamProperty(index = 1652, type = WamType.INTEGER)
    OptionalLong videoDisablingEventCount();

    @WamProperty(index = 1819, type = WamType.TIMER)
    Optional<Instant> videoDisablingPausedDurationNoSbwa();

    @WamProperty(index = 1653, type = WamType.TIMER)
    Optional<Instant> videoDisablingToCallEndDelay();

    @WamProperty(index = 273, type = WamType.INTEGER)
    OptionalLong videoDowngradeCount();

    @WamProperty(index = 2972, type = WamType.INTEGER)
    OptionalLong videoEdgeAvg();

    @WamProperty(index = 163, type = WamType.BOOLEAN)
    Optional<Boolean> videoEnabled();

    @WamProperty(index = 270, type = WamType.BOOLEAN)
    Optional<Boolean> videoEnabledAtCallStart();

    @WamProperty(index = 609, type = WamType.TIMER)
    Optional<Instant> videoEncAllLtrpTimeInMsec();

    @WamProperty(index = 216, type = WamType.FLOAT)
    OptionalDouble videoEncAvgFps();

    @WamProperty(index = 825, type = WamType.FLOAT)
    OptionalDouble videoEncAvgFpsHq();

    @WamProperty(index = 1216, type = WamType.FLOAT)
    OptionalDouble videoEncAvgQpKeyFrameOpenh264();

    @WamProperty(index = 466, type = WamType.FLOAT)
    OptionalDouble videoEncAvgQpKeyFrameVp8();

    @WamProperty(index = 470, type = WamType.FLOAT)
    OptionalDouble videoEncAvgQpLtrpFrameVp8();

    @WamProperty(index = 1218, type = WamType.FLOAT)
    OptionalDouble videoEncAvgQpPFramePrevRefOpenh264();

    @WamProperty(index = 475, type = WamType.FLOAT)
    OptionalDouble videoEncAvgQpPFramePrevRefVp8();

    @WamProperty(index = 685, type = WamType.FLOAT)
    OptionalDouble videoEncAvgSizeAllLtrpFrameVp8();

    @WamProperty(index = 464, type = WamType.FLOAT)
    OptionalDouble videoEncAvgSizeKeyFrameVp8();

    @WamProperty(index = 468, type = WamType.FLOAT)
    OptionalDouble videoEncAvgSizeLtrpFrameVp8();

    @WamProperty(index = 473, type = WamType.FLOAT)
    OptionalDouble videoEncAvgSizePFramePrevRefVp8();

    @WamProperty(index = 215, type = WamType.FLOAT)
    OptionalDouble videoEncAvgTargetFps();

    @WamProperty(index = 827, type = WamType.FLOAT)
    OptionalDouble videoEncAvgTargetFpsHq();

    @WamProperty(index = 1476, type = WamType.FLOAT)
    OptionalDouble videoEncBitrateHqSs();

    @WamProperty(index = 213, type = WamType.INTEGER)
    OptionalLong videoEncColorId();

    @WamProperty(index = 686, type = WamType.FLOAT)
    OptionalDouble videoEncDeviationAllLtrpFrameVp8();

    @WamProperty(index = 687, type = WamType.FLOAT)
    OptionalDouble videoEncDeviationPFramePrevRefVp8();

    @WamProperty(index = 217, type = WamType.INTEGER)
    OptionalLong videoEncDiscardFrame();

    @WamProperty(index = 938, type = WamType.INTEGER)
    OptionalLong videoEncDiscardFrameHq();

    @WamProperty(index = 179, type = WamType.INTEGER)
    OptionalLong videoEncDropFrames();

    @WamProperty(index = 937, type = WamType.INTEGER)
    OptionalLong videoEncDropFramesHq();

    @WamProperty(index = 178, type = WamType.INTEGER)
    OptionalLong videoEncErrorFrames();

    @WamProperty(index = 936, type = WamType.INTEGER)
    OptionalLong videoEncErrorFramesHq();

    @WamProperty(index = 1049, type = WamType.INTEGER)
    OptionalLong videoEncFatalErrorNum();

    @WamProperty(index = 176, type = WamType.INTEGER)
    OptionalLong videoEncInputFrames();

    @WamProperty(index = 934, type = WamType.INTEGER)
    OptionalLong videoEncInputFramesHq();

    @WamProperty(index = 1477, type = WamType.INTEGER)
    OptionalLong videoEncInputFramesInLast10secSs();

    @WamProperty(index = 1478, type = WamType.INTEGER)
    OptionalLong videoEncInputFramesSs();

    @WamProperty(index = 180, type = WamType.INTEGER)
    OptionalLong videoEncKeyframes();

    @WamProperty(index = 939, type = WamType.INTEGER)
    OptionalLong videoEncKeyframesHq();

    @WamProperty(index = 1479, type = WamType.INTEGER)
    OptionalLong videoEncKeyframesSs();

    @WamProperty(index = 463, type = WamType.INTEGER)
    OptionalLong videoEncKeyframesVp8();

    @WamProperty(index = 731, type = WamType.TIMER)
    Optional<Instant> videoEncKfErrCodecSwitchT();

    @WamProperty(index = 224, type = WamType.TIMER)
    Optional<Instant> videoEncLatency();

    @WamProperty(index = 826, type = WamType.TIMER)
    Optional<Instant> videoEncLatencyHq();

    @WamProperty(index = 471, type = WamType.INTEGER)
    OptionalLong videoEncLtrpFrameGenFailedVp8();

    @WamProperty(index = 467, type = WamType.INTEGER)
    OptionalLong videoEncLtrpFramesVp8();

    @WamProperty(index = 494, type = WamType.INTEGER)
    OptionalLong videoEncLtrpToKfFallbackVp8();

    @WamProperty(index = 1050, type = WamType.INTEGER)
    OptionalLong videoEncModifyNum();

    @WamProperty(index = 1400, type = WamType.TIMER)
    Optional<Instant> videoEncMsInOpenh264HighComp();

    @WamProperty(index = 1401, type = WamType.TIMER)
    Optional<Instant> videoEncMsInOpenh264LowComp();

    @WamProperty(index = 1402, type = WamType.TIMER)
    Optional<Instant> videoEncMsInOpenh264MediumComp();

    @WamProperty(index = 1403, type = WamType.TIMER)
    Optional<Instant> videoEncMsInOpenh264UltrahighComp();

    @WamProperty(index = 212, type = WamType.INTEGER)
    OptionalLong videoEncName();

    @WamProperty(index = 2730, type = WamType.INTEGER)
    OptionalLong videoEncNoRtcpSessionNum();

    @WamProperty(index = 2731, type = WamType.INTEGER)
    OptionalLong videoEncNoRtcpSessionNumHq();

    @WamProperty(index = 600, type = WamType.INTEGER)
    OptionalLong videoEncNumErrorLtrHoldFailedVp8();

    @WamProperty(index = 1480, type = WamType.INTEGER)
    OptionalLong videoEncOutputFrameSs();

    @WamProperty(index = 177, type = WamType.INTEGER)
    OptionalLong videoEncOutputFrames();

    @WamProperty(index = 935, type = WamType.INTEGER)
    OptionalLong videoEncOutputFramesHq();

    @WamProperty(index = 472, type = WamType.INTEGER)
    OptionalLong videoEncPFramePrevRefVp8();

    @WamProperty(index = 2433, type = WamType.FLOAT)
    OptionalDouble videoEncPsnrP5();

    @WamProperty(index = 2434, type = WamType.FLOAT)
    OptionalDouble videoEncPsnrP50();

    @WamProperty(index = 2435, type = WamType.FLOAT)
    OptionalDouble videoEncPsnrP95();

    @WamProperty(index = 608, type = WamType.TIMER)
    Optional<Instant> videoEncRegularLtrpTimeInMsec();

    @WamProperty(index = 214, type = WamType.INTEGER)
    OptionalLong videoEncRestart();

    @WamProperty(index = 1046, type = WamType.INTEGER)
    OptionalLong videoEncRestartPresetChange();

    @WamProperty(index = 1045, type = WamType.INTEGER)
    OptionalLong videoEncRestartResChange();

    @WamProperty(index = 1689, type = WamType.TIMER)
    Optional<Instant> videoEncTimeOvershoot10PercAv1();

    @WamProperty(index = 363, type = WamType.TIMER)
    Optional<Instant> videoEncTimeOvershoot10PercH264();

    @WamProperty(index = 366, type = WamType.TIMER)
    Optional<Instant> videoEncTimeOvershoot10PercH265();

    @WamProperty(index = 2261, type = WamType.TIMER)
    Optional<Instant> videoEncTimeOvershoot10PercVav1();

    @WamProperty(index = 369, type = WamType.TIMER)
    Optional<Instant> videoEncTimeOvershoot10PercVp8();

    @WamProperty(index = 1690, type = WamType.TIMER)
    Optional<Instant> videoEncTimeOvershoot20PercAv1();

    @WamProperty(index = 364, type = WamType.TIMER)
    Optional<Instant> videoEncTimeOvershoot20PercH264();

    @WamProperty(index = 367, type = WamType.TIMER)
    Optional<Instant> videoEncTimeOvershoot20PercH265();

    @WamProperty(index = 2262, type = WamType.TIMER)
    Optional<Instant> videoEncTimeOvershoot20PercVav1();

    @WamProperty(index = 370, type = WamType.TIMER)
    Optional<Instant> videoEncTimeOvershoot20PercVp8();

    @WamProperty(index = 1691, type = WamType.TIMER)
    Optional<Instant> videoEncTimeOvershoot40PercAv1();

    @WamProperty(index = 365, type = WamType.TIMER)
    Optional<Instant> videoEncTimeOvershoot40PercH264();

    @WamProperty(index = 368, type = WamType.TIMER)
    Optional<Instant> videoEncTimeOvershoot40PercH265();

    @WamProperty(index = 2263, type = WamType.TIMER)
    Optional<Instant> videoEncTimeOvershoot40PercVav1();

    @WamProperty(index = 371, type = WamType.TIMER)
    Optional<Instant> videoEncTimeOvershoot40PercVp8();

    @WamProperty(index = 1019, type = WamType.TIMER)
    Optional<Instant> videoEncTimeSpentInNegative10Vp8Ms();

    @WamProperty(index = 1018, type = WamType.TIMER)
    Optional<Instant> videoEncTimeSpentInNegative12Vp8Ms();

    @WamProperty(index = 1022, type = WamType.TIMER)
    Optional<Instant> videoEncTimeSpentInNegative4Vp8Ms();

    @WamProperty(index = 1021, type = WamType.TIMER)
    Optional<Instant> videoEncTimeSpentInNegative6Vp8Ms();

    @WamProperty(index = 1020, type = WamType.TIMER)
    Optional<Instant> videoEncTimeSpentInNegative8Vp8Ms();

    @WamProperty(index = 1692, type = WamType.TIMER)
    Optional<Instant> videoEncTimeUndershoot10PercAv1();

    @WamProperty(index = 375, type = WamType.TIMER)
    Optional<Instant> videoEncTimeUndershoot10PercH264();

    @WamProperty(index = 378, type = WamType.TIMER)
    Optional<Instant> videoEncTimeUndershoot10PercH265();

    @WamProperty(index = 2264, type = WamType.TIMER)
    Optional<Instant> videoEncTimeUndershoot10PercVav1();

    @WamProperty(index = 381, type = WamType.TIMER)
    Optional<Instant> videoEncTimeUndershoot10PercVp8();

    @WamProperty(index = 1693, type = WamType.TIMER)
    Optional<Instant> videoEncTimeUndershoot20PercAv1();

    @WamProperty(index = 376, type = WamType.TIMER)
    Optional<Instant> videoEncTimeUndershoot20PercH264();

    @WamProperty(index = 379, type = WamType.TIMER)
    Optional<Instant> videoEncTimeUndershoot20PercH265();

    @WamProperty(index = 2265, type = WamType.TIMER)
    Optional<Instant> videoEncTimeUndershoot20PercVav1();

    @WamProperty(index = 382, type = WamType.TIMER)
    Optional<Instant> videoEncTimeUndershoot20PercVp8();

    @WamProperty(index = 1694, type = WamType.TIMER)
    Optional<Instant> videoEncTimeUndershoot40PercAv1();

    @WamProperty(index = 377, type = WamType.TIMER)
    Optional<Instant> videoEncTimeUndershoot40PercH264();

    @WamProperty(index = 380, type = WamType.TIMER)
    Optional<Instant> videoEncTimeUndershoot40PercH265();

    @WamProperty(index = 2266, type = WamType.TIMER)
    Optional<Instant> videoEncTimeUndershoot40PercVav1();

    @WamProperty(index = 383, type = WamType.TIMER)
    Optional<Instant> videoEncTimeUndershoot40PercVp8();

    @WamProperty(index = 1481, type = WamType.INTEGER)
    OptionalLong videoEncoderHeightSs();

    @WamProperty(index = 1482, type = WamType.INTEGER)
    OptionalLong videoEncoderWidthSs();

    @WamProperty(index = 183, type = WamType.INTEGER)
    OptionalLong videoFecRecovered();

    @WamProperty(index = 334, type = WamType.INTEGER)
    OptionalLong videoH264Time();

    @WamProperty(index = 335, type = WamType.INTEGER)
    OptionalLong videoH265Time();

    @WamProperty(index = 189, type = WamType.INTEGER)
    OptionalLong videoHeight();

    @WamProperty(index = 904, type = WamType.FLOAT)
    OptionalDouble videoInitRxBitrate16s();

    @WamProperty(index = 901, type = WamType.FLOAT)
    OptionalDouble videoInitRxBitrate2s();

    @WamProperty(index = 902, type = WamType.FLOAT)
    OptionalDouble videoInitRxBitrate4s();

    @WamProperty(index = 903, type = WamType.FLOAT)
    OptionalDouble videoInitRxBitrate8s();

    @WamProperty(index = 402, type = WamType.INTEGER)
    OptionalLong videoInitialCodecScheme();

    @WamProperty(index = 321, type = WamType.INTEGER)
    OptionalLong videoInitialCodecType();

    @WamProperty(index = 185, type = WamType.FLOAT)
    OptionalDouble videoLastSenderBwe();

    @WamProperty(index = 426, type = WamType.FLOAT)
    OptionalDouble videoMaxRxBitrate();

    @WamProperty(index = 420, type = WamType.FLOAT)
    OptionalDouble videoMaxTargetBitrate();

    @WamProperty(index = 829, type = WamType.FLOAT)
    OptionalDouble videoMaxTargetBitrateHq();

    @WamProperty(index = 425, type = WamType.FLOAT)
    OptionalDouble videoMaxTxBitrate();

    @WamProperty(index = 824, type = WamType.FLOAT)
    OptionalDouble videoMaxTxBitrateHq();

    @WamProperty(index = 421, type = WamType.FLOAT)
    OptionalDouble videoMinTargetBitrate();

    @WamProperty(index = 830, type = WamType.FLOAT)
    OptionalDouble videoMinTargetBitrateHq();

    @WamProperty(index = 2973, type = WamType.INTEGER)
    OptionalLong videoMotionAvg();

    @WamProperty(index = 2974, type = WamType.INTEGER)
    OptionalLong videoMotionP5();

    @WamProperty(index = 2975, type = WamType.INTEGER)
    OptionalLong videoMotionP95();

    @WamProperty(index = 1185, type = WamType.BOOLEAN)
    Optional<Boolean> videoNackHbhEnabled();

    @WamProperty(index = 1272, type = WamType.INTEGER)
    OptionalLong videoNackRtpRetransmitRecvdCount();

    @WamProperty(index = 1373, type = WamType.INTEGER)
    OptionalLong videoNackRtpRetransmitReqCount();

    @WamProperty(index = 2917, type = WamType.INTEGER)
    OptionalLong videoNackRtpRetransmitRetryCount();

    @WamProperty(index = 594, type = WamType.INTEGER)
    OptionalLong videoNpsiGenFailed();

    @WamProperty(index = 595, type = WamType.INTEGER)
    OptionalLong videoNpsiNoNack();

    @WamProperty(index = 1010, type = WamType.INTEGER)
    OptionalLong videoNumAvSyncDiscardFrames();

    @WamProperty(index = 3031, type = WamType.FLOAT)
    OptionalDouble videoOverexposureAvg();

    @WamProperty(index = 275, type = WamType.ENUM)
    Optional<CallVideoState> videoPeerState();

    @WamProperty(index = 654, type = WamType.INTEGER)
    OptionalLong videoPeerTriggeredPauseCount();

    @WamProperty(index = 1270, type = WamType.INTEGER)
    OptionalLong videoQualityScore();

    @WamProperty(index = 2315, type = WamType.FLOAT)
    OptionalDouble videoRecvPsnrAvg();

    @WamProperty(index = 2996, type = WamType.FLOAT)
    OptionalDouble videoRecvPsnrAvgSs();

    @WamProperty(index = 2316, type = WamType.FLOAT)
    OptionalDouble videoRecvPsnrP5();

    @WamProperty(index = 2317, type = WamType.FLOAT)
    OptionalDouble videoRecvPsnrP50();

    @WamProperty(index = 2997, type = WamType.FLOAT)
    OptionalDouble videoRecvPsnrP50Ss();

    @WamProperty(index = 2998, type = WamType.FLOAT)
    OptionalDouble videoRecvPsnrP5Ss();

    @WamProperty(index = 2318, type = WamType.FLOAT)
    OptionalDouble videoRecvPsnrP95();

    @WamProperty(index = 2999, type = WamType.FLOAT)
    OptionalDouble videoRecvPsnrP95Ss();

    @WamProperty(index = 3033, type = WamType.TIMER)
    Optional<Instant> videoRecvToRenderLatency();

    @WamProperty(index = 208, type = WamType.INTEGER)
    OptionalLong videoRenderAvgFps();

    @WamProperty(index = 2182, type = WamType.INTEGER)
    OptionalLong videoRenderAvgFpsDominantSpeaker();

    @WamProperty(index = 225, type = WamType.TIMER)
    Optional<Instant> videoRenderConverterTs();

    @WamProperty(index = 196, type = WamType.TIMER)
    Optional<Instant> videoRenderDelayT();

    @WamProperty(index = 304, type = WamType.TIMER)
    Optional<Instant> videoRenderFreeze2xT();

    @WamProperty(index = 2183, type = WamType.INTEGER)
    OptionalLong videoRenderFreeze2xTDominantSpeaker();

    @WamProperty(index = 2860, type = WamType.TIMER)
    Optional<Instant> videoRenderFreeze2xTDominantSpeakerV2();

    @WamProperty(index = 2714, type = WamType.TIMER)
    Optional<Instant> videoRenderFreeze2xTV2();

    @WamProperty(index = 305, type = WamType.TIMER)
    Optional<Instant> videoRenderFreeze4xT();

    @WamProperty(index = 2184, type = WamType.INTEGER)
    OptionalLong videoRenderFreeze4xTDominantSpeaker();

    @WamProperty(index = 2861, type = WamType.TIMER)
    Optional<Instant> videoRenderFreeze4xTDominantSpeakerV2();

    @WamProperty(index = 2715, type = WamType.TIMER)
    Optional<Instant> videoRenderFreeze4xTV2();

    @WamProperty(index = 306, type = WamType.TIMER)
    Optional<Instant> videoRenderFreeze8xT();

    @WamProperty(index = 2185, type = WamType.INTEGER)
    OptionalLong videoRenderFreeze8xTDominantSpeaker();

    @WamProperty(index = 2862, type = WamType.TIMER)
    Optional<Instant> videoRenderFreeze8xTDominantSpeakerV2();

    @WamProperty(index = 2716, type = WamType.TIMER)
    Optional<Instant> videoRenderFreeze8xTV2();

    @WamProperty(index = 235, type = WamType.TIMER)
    Optional<Instant> videoRenderFreezeT();

    @WamProperty(index = 2186, type = WamType.INTEGER)
    OptionalLong videoRenderFreezeTDominantSpeaker();

    @WamProperty(index = 2863, type = WamType.TIMER)
    Optional<Instant> videoRenderFreezeTDominantSpeakerV2();

    @WamProperty(index = 2717, type = WamType.TIMER)
    Optional<Instant> videoRenderFreezeTV2();

    @WamProperty(index = 2089, type = WamType.FLOAT)
    OptionalDouble videoRenderHarmonicFpsAvg();

    @WamProperty(index = 2090, type = WamType.FLOAT)
    OptionalDouble videoRenderHarmonicFpsP5();

    @WamProperty(index = 2091, type = WamType.FLOAT)
    OptionalDouble videoRenderHarmonicFpsP50();

    @WamProperty(index = 2092, type = WamType.FLOAT)
    OptionalDouble videoRenderHarmonicFpsP95();

    @WamProperty(index = 908, type = WamType.TIMER)
    Optional<Instant> videoRenderInitFreeze16sT();

    @WamProperty(index = 2823, type = WamType.TIMER)
    Optional<Instant> videoRenderInitFreeze16sTV2();

    @WamProperty(index = 905, type = WamType.TIMER)
    Optional<Instant> videoRenderInitFreeze2sT();

    @WamProperty(index = 2824, type = WamType.TIMER)
    Optional<Instant> videoRenderInitFreeze2sTV2();

    @WamProperty(index = 906, type = WamType.TIMER)
    Optional<Instant> videoRenderInitFreeze4sT();

    @WamProperty(index = 2825, type = WamType.TIMER)
    Optional<Instant> videoRenderInitFreeze4sTV2();

    @WamProperty(index = 907, type = WamType.TIMER)
    Optional<Instant> videoRenderInitFreeze8sT();

    @WamProperty(index = 2826, type = WamType.TIMER)
    Optional<Instant> videoRenderInitFreeze8sTV2();

    @WamProperty(index = 526, type = WamType.TIMER)
    Optional<Instant> videoRenderInitFreezeT();

    @WamProperty(index = 2827, type = WamType.TIMER)
    Optional<Instant> videoRenderInitFreezeTV2();

    @WamProperty(index = 569, type = WamType.INTEGER)
    OptionalLong videoRenderNumFreezes();

    @WamProperty(index = 571, type = WamType.INTEGER)
    OptionalLong videoRenderNumSinceLastFreeze10s();

    @WamProperty(index = 572, type = WamType.INTEGER)
    OptionalLong videoRenderNumSinceLastFreeze30s();

    @WamProperty(index = 570, type = WamType.INTEGER)
    OptionalLong videoRenderNumSinceLastFreeze5s();

    @WamProperty(index = 1132, type = WamType.TIMER)
    Optional<Instant> videoRenderPauseT();

    @WamProperty(index = 568, type = WamType.TIMER)
    Optional<Instant> videoRenderSumTimeSinceLastFreeze();

    @WamProperty(index = 2587, type = WamType.FLOAT)
    OptionalDouble videoRenderedRxBitrate();

    @WamProperty(index = 1178, type = WamType.INTEGER)
    OptionalLong videoRetxRtcpNack();

    @WamProperty(index = 1179, type = WamType.INTEGER)
    OptionalLong videoRetxRtcpPli();

    @WamProperty(index = 1273, type = WamType.INTEGER)
    OptionalLong videoRtcpNackProcessed();

    @WamProperty(index = 1274, type = WamType.INTEGER)
    OptionalLong videoRtcpNackProcessedHq();

    @WamProperty(index = 169, type = WamType.FLOAT)
    OptionalDouble videoRxBitrate();

    @WamProperty(index = 2187, type = WamType.INTEGER)
    OptionalLong videoRxBitrateDominantSpeaker();

    @WamProperty(index = 1992, type = WamType.INTEGER)
    OptionalLong videoRxBitrateDominantSpeakerInSpeakerMode();

    @WamProperty(index = 1993, type = WamType.INTEGER)
    OptionalLong videoRxBitrateDominantSpeakerWithPeerInSpeakerMode();

    @WamProperty(index = 1994, type = WamType.INTEGER)
    OptionalLong videoRxBitrateNonSpeakerInSpeakerMode();

    @WamProperty(index = 1995, type = WamType.INTEGER)
    OptionalLong videoRxBitrateNonSpeakerWithPeerInSpeakerMode();

    @WamProperty(index = 1483, type = WamType.INTEGER)
    OptionalLong videoRxBitrateSs();

    @WamProperty(index = 187, type = WamType.BOOLEAN)
    Optional<Boolean> videoRxBweHitTxBwe();

    @WamProperty(index = 489, type = WamType.FLOAT)
    OptionalDouble videoRxBytesRtcpApp();

    @WamProperty(index = 219, type = WamType.FLOAT)
    OptionalDouble videoRxFecBitrate();

    @WamProperty(index = 182, type = WamType.INTEGER)
    OptionalLong videoRxFecFrames();

    @WamProperty(index = 485, type = WamType.INTEGER)
    OptionalLong videoRxKfBeforeLtrpAfterRpsi();

    @WamProperty(index = 721, type = WamType.INTEGER)
    OptionalLong videoRxNumCodecSwitch();

    @WamProperty(index = 201, type = WamType.INTEGER)
    OptionalLong videoRxPackets();

    @WamProperty(index = 170, type = WamType.FLOAT)
    OptionalDouble videoRxPktLossPct();

    @WamProperty(index = 487, type = WamType.INTEGER)
    OptionalLong videoRxPktRtcpApp();

    @WamProperty(index = 2095, type = WamType.FLOAT)
    OptionalDouble videoRxRsFecBitrate();

    @WamProperty(index = 2096, type = WamType.INTEGER)
    OptionalLong videoRxRsFecPkts();

    @WamProperty(index = 621, type = WamType.INTEGER)
    OptionalLong videoRxRtcpFir();

    @WamProperty(index = 203, type = WamType.INTEGER)
    OptionalLong videoRxRtcpNack();

    @WamProperty(index = 1181, type = WamType.INTEGER)
    OptionalLong videoRxRtcpNackDropped();

    @WamProperty(index = 521, type = WamType.INTEGER)
    OptionalLong videoRxRtcpNpsi();

    @WamProperty(index = 202, type = WamType.INTEGER)
    OptionalLong videoRxRtcpPli();

    @WamProperty(index = 1182, type = WamType.INTEGER)
    OptionalLong videoRxRtcpPliDropped();

    @WamProperty(index = 459, type = WamType.INTEGER)
    OptionalLong videoRxRtcpRpsi();

    @WamProperty(index = 168, type = WamType.FLOAT)
    OptionalDouble videoRxTotalBytes();

    @WamProperty(index = 2436, type = WamType.FLOAT)
    OptionalDouble videoScalPsnrP5();

    @WamProperty(index = 2437, type = WamType.FLOAT)
    OptionalDouble videoScalPsnrP50();

    @WamProperty(index = 2438, type = WamType.FLOAT)
    OptionalDouble videoScalPsnrP95();

    @WamProperty(index = 274, type = WamType.ENUM)
    Optional<CallVideoState> videoSelfState();

    @WamProperty(index = 954, type = WamType.FLOAT)
    OptionalDouble videoSenderBweDiffStddev();

    @WamProperty(index = 348, type = WamType.FLOAT)
    OptionalDouble videoSenderBweStddev();

    @WamProperty(index = 3000, type = WamType.INTEGER)
    OptionalLong videoStateReorderDropCount();

    @WamProperty(index = 1562, type = WamType.INTEGER)
    OptionalLong videoStreamRecreations();

    @WamProperty(index = 351, type = WamType.TIMER)
    Optional<Instant> videoTargetBitrateReaches1000kbpsT();

    @WamProperty(index = 1797, type = WamType.TIMER)
    Optional<Instant> videoTargetBitrateReaches100kbpsT();

    @WamProperty(index = 435, type = WamType.TIMER)
    Optional<Instant> videoTargetBitrateReaches1500kbpsT();

    @WamProperty(index = 436, type = WamType.TIMER)
    Optional<Instant> videoTargetBitrateReaches2000kbpsT();

    @WamProperty(index = 433, type = WamType.TIMER)
    Optional<Instant> videoTargetBitrateReaches250kbpsT();

    @WamProperty(index = 1798, type = WamType.TIMER)
    Optional<Instant> videoTargetBitrateReaches300kbpsT();

    @WamProperty(index = 350, type = WamType.TIMER)
    Optional<Instant> videoTargetBitrateReaches500kbpsT();

    @WamProperty(index = 434, type = WamType.TIMER)
    Optional<Instant> videoTargetBitrateReaches750kbpsT();

    @WamProperty(index = 451, type = WamType.FLOAT)
    OptionalDouble videoTotalBytesOnNonDefCell();

    @WamProperty(index = 165, type = WamType.FLOAT)
    OptionalDouble videoTxBitrate();

    @WamProperty(index = 823, type = WamType.FLOAT)
    OptionalDouble videoTxBitrateHq();

    @WamProperty(index = 1484, type = WamType.FLOAT)
    OptionalDouble videoTxBitrateSs();

    @WamProperty(index = 488, type = WamType.FLOAT)
    OptionalDouble videoTxBytesRtcpApp();

    @WamProperty(index = 218, type = WamType.FLOAT)
    OptionalDouble videoTxFecBitrate();

    @WamProperty(index = 181, type = WamType.INTEGER)
    OptionalLong videoTxFecFrames();

    @WamProperty(index = 720, type = WamType.INTEGER)
    OptionalLong videoTxNumCodecSwitch();

    @WamProperty(index = 197, type = WamType.INTEGER)
    OptionalLong videoTxPackets();

    @WamProperty(index = 818, type = WamType.INTEGER)
    OptionalLong videoTxPacketsHq();

    @WamProperty(index = 167, type = WamType.FLOAT)
    OptionalDouble videoTxPktErrorPct();

    @WamProperty(index = 821, type = WamType.FLOAT)
    OptionalDouble videoTxPktErrorPctHq();

    @WamProperty(index = 166, type = WamType.FLOAT)
    OptionalDouble videoTxPktLossPct();

    @WamProperty(index = 486, type = WamType.INTEGER)
    OptionalLong videoTxPktRtcpApp();

    @WamProperty(index = 1275, type = WamType.INTEGER)
    OptionalLong videoTxResendCauseKf();

    @WamProperty(index = 1276, type = WamType.INTEGER)
    OptionalLong videoTxResendCauseKfHq();

    @WamProperty(index = 1277, type = WamType.INTEGER)
    OptionalLong videoTxResendFailures();

    @WamProperty(index = 1278, type = WamType.INTEGER)
    OptionalLong videoTxResendFailuresHq();

    @WamProperty(index = 2644, type = WamType.INTEGER)
    OptionalLong videoTxResendLimitedPackets();

    @WamProperty(index = 2645, type = WamType.INTEGER)
    OptionalLong videoTxResendLimitedPacketsHq();

    @WamProperty(index = 198, type = WamType.INTEGER)
    OptionalLong videoTxResendPackets();

    @WamProperty(index = 819, type = WamType.INTEGER)
    OptionalLong videoTxResendPacketsHq();

    @WamProperty(index = 2097, type = WamType.FLOAT)
    OptionalDouble videoTxRsFecBitrate();

    @WamProperty(index = 2098, type = WamType.INTEGER)
    OptionalLong videoTxRsFecPkts();

    @WamProperty(index = 620, type = WamType.INTEGER)
    OptionalLong videoTxRtcpFirEmptyJb();

    @WamProperty(index = 200, type = WamType.INTEGER)
    OptionalLong videoTxRtcpNack();

    @WamProperty(index = 520, type = WamType.INTEGER)
    OptionalLong videoTxRtcpNpsi();

    @WamProperty(index = 199, type = WamType.INTEGER)
    OptionalLong videoTxRtcpPli();

    @WamProperty(index = 820, type = WamType.INTEGER)
    OptionalLong videoTxRtcpPliHq();

    @WamProperty(index = 458, type = WamType.INTEGER)
    OptionalLong videoTxRtcpRpsi();

    @WamProperty(index = 164, type = WamType.FLOAT)
    OptionalDouble videoTxTotalBytes();

    @WamProperty(index = 817, type = WamType.FLOAT)
    OptionalDouble videoTxTotalBytesHq();

    @WamProperty(index = 453, type = WamType.INTEGER)
    OptionalLong videoUpdateEncoderFailureCount();

    @WamProperty(index = 325, type = WamType.INTEGER)
    OptionalLong videoUpgradeCancelByTimeoutCount();

    @WamProperty(index = 323, type = WamType.INTEGER)
    OptionalLong videoUpgradeCancelCount();

    @WamProperty(index = 272, type = WamType.INTEGER)
    OptionalLong videoUpgradeCount();

    @WamProperty(index = 326, type = WamType.INTEGER)
    OptionalLong videoUpgradeRejectByTimeoutCount();

    @WamProperty(index = 324, type = WamType.INTEGER)
    OptionalLong videoUpgradeRejectCount();

    @WamProperty(index = 271, type = WamType.INTEGER)
    OptionalLong videoUpgradeRequestCount();

    @WamProperty(index = 2874, type = WamType.INTEGER)
    OptionalLong videoWebcodecsDecFatalErrorNum();

    @WamProperty(index = 188, type = WamType.INTEGER)
    OptionalLong videoWidth();

    @WamProperty(index = 2351, type = WamType.INTEGER)
    OptionalLong viewUnknownPeerVideoCount();

    @WamProperty(index = 2145, type = WamType.FLOAT)
    OptionalDouble vmosAvg();

    @WamProperty(index = 2633, type = WamType.FLOAT)
    OptionalDouble vmosAvgInferenceLatencyMs();

    @WamProperty(index = 2634, type = WamType.FLOAT)
    OptionalDouble vmosAvgLoadTimeInMs();

    @WamProperty(index = 2635, type = WamType.INTEGER)
    OptionalLong vmosDownloadFailureCount();

    @WamProperty(index = 2636, type = WamType.INTEGER)
    OptionalLong vmosLoadFailureCount();

    @WamProperty(index = 2146, type = WamType.FLOAT)
    OptionalDouble vmosP5();

    @WamProperty(index = 2147, type = WamType.FLOAT)
    OptionalDouble vmosP50();

    @WamProperty(index = 2148, type = WamType.FLOAT)
    OptionalDouble vmosP95();

    @WamProperty(index = 2272, type = WamType.STRING)
    Optional<String> voipCameraLastErrorDeviceName();

    @WamProperty(index = 2273, type = WamType.INTEGER)
    OptionalLong voipCameraTotalErrors();

    @WamProperty(index = 2274, type = WamType.TIMER)
    Optional<Instant> voipInitTime();

    @WamProperty(index = 2275, type = WamType.STRING)
    Optional<String> voipMicLastErrorDeviceName();

    @WamProperty(index = 2276, type = WamType.INTEGER)
    OptionalLong voipMicTotalErrors();

    @WamProperty(index = 1136, type = WamType.INTEGER)
    OptionalLong voipParamsCompressedSize();

    @WamProperty(index = 1137, type = WamType.INTEGER)
    OptionalLong voipParamsUncompressedSize();

    @WamProperty(index = 1615, type = WamType.ENUM)
    Optional<VoipSettingReleaseType> voipSettingReleaseType();

    @WamProperty(index = 1616, type = WamType.INTEGER)
    OptionalLong voipSettingVersion();

    @WamProperty(index = 1571, type = WamType.INTEGER)
    OptionalLong voipSettingsDictLookupFailure();

    @WamProperty(index = 1572, type = WamType.INTEGER)
    OptionalLong voipSettingsDictLookupSuccess();

    @WamProperty(index = 1573, type = WamType.INTEGER)
    OptionalLong voipSettingsDictNoLookup();

    @WamProperty(index = 2277, type = WamType.INTEGER)
    OptionalLong voipTotalCameraDevices();

    @WamProperty(index = 2278, type = WamType.INTEGER)
    OptionalLong voipTotalMicDevices();

    @WamProperty(index = 2279, type = WamType.TIMER)
    Optional<Instant> voipWindowIncomingAcceptToCallLayoutTime();

    @WamProperty(index = 2280, type = WamType.TIMER)
    Optional<Instant> voipWindowIncomingOfferToLayoutTime();

    @WamProperty(index = 2281, type = WamType.TIMER)
    Optional<Instant> voipWindowOutgoingLaunchTime();

    @WamProperty(index = 2637, type = WamType.FLOAT)
    OptionalDouble vsrAvgInferenceLatencyMs();

    @WamProperty(index = 2403, type = WamType.FLOAT)
    OptionalDouble vsrAvgLatencyInMs();

    @WamProperty(index = 2638, type = WamType.FLOAT)
    OptionalDouble vsrAvgLoadTimeInMs();

    @WamProperty(index = 2639, type = WamType.FLOAT)
    OptionalDouble vsrAvgPostProcessLatencyMs();

    @WamProperty(index = 2640, type = WamType.FLOAT)
    OptionalDouble vsrAvgPreProcessLatencyMs();

    @WamProperty(index = 2728, type = WamType.INTEGER)
    OptionalLong vsrDisableReason();

    @WamProperty(index = 2641, type = WamType.INTEGER)
    OptionalLong vsrDownloadFailureCount();

    @WamProperty(index = 2836, type = WamType.BOOLEAN)
    Optional<Boolean> vsrDownloadSuccess();

    @WamProperty(index = 2391, type = WamType.INTEGER)
    OptionalLong vsrInputFrames();

    @WamProperty(index = 2642, type = WamType.INTEGER)
    OptionalLong vsrLoadFailureCount();

    @WamProperty(index = 2837, type = WamType.BOOLEAN)
    Optional<Boolean> vsrLoadSuccess();

    @WamProperty(index = 2392, type = WamType.INTEGER)
    OptionalLong vsrOutputFrames();

    @WamProperty(index = 1665, type = WamType.BOOLEAN)
    Optional<Boolean> waBadCallDetectorFreqRttCycle();

    @WamProperty(index = 1666, type = WamType.BOOLEAN)
    Optional<Boolean> waBadCallDetectorHighInitRtt();

    @WamProperty(index = 1667, type = WamType.BOOLEAN)
    Optional<Boolean> waBadCallDetectorHistRtt();

    @WamProperty(index = 1742, type = WamType.FLOAT)
    OptionalDouble waBadCallDetectorInitRttStddev();

    @WamProperty(index = 1668, type = WamType.INTEGER)
    OptionalLong waBadCallDetectorMteBadCombine();

    @WamProperty(index = 1657, type = WamType.INTEGER)
    OptionalLong waCallingHistoryDlSbweBySelfIp();

    @WamProperty(index = 1658, type = WamType.ENUM)
    Optional<WaCallingHistoryGroupCallRecordSaveConditionCheckStatus> waCallingHistoryGroupCallRecordSaveConditionCheckStatus();

    @WamProperty(index = 1659, type = WamType.BOOLEAN)
    Optional<Boolean> waCallingHistoryGroupCallSelfIpAddressAvailable();

    @WamProperty(index = 1680, type = WamType.BOOLEAN)
    Optional<Boolean> waCallingHistoryInitDlSbweSuccess();

    @WamProperty(index = 1681, type = WamType.BOOLEAN)
    Optional<Boolean> waCallingHistoryInitUlSbweSuccess();

    @WamProperty(index = 1660, type = WamType.BOOLEAN)
    Optional<Boolean> waCallingHistoryIsGroupCallRecordSaved();

    @WamProperty(index = 1669, type = WamType.INTEGER)
    OptionalLong waCallingHistoryLastAvgRttBySelfAndPeerIp();

    @WamProperty(index = 1670, type = WamType.INTEGER)
    OptionalLong waCallingHistoryLastMaxRttBySelfAndPeerIp();

    @WamProperty(index = 1671, type = WamType.INTEGER)
    OptionalLong waCallingHistoryLastMinRttBySelfAndPeerIp();

    @WamProperty(index = 1661, type = WamType.INTEGER)
    OptionalLong waCallingHistoryNumOfGroupCallRecordLoaded();

    @WamProperty(index = 1662, type = WamType.INTEGER)
    OptionalLong waCallingHistoryUlSbweBySelfIp();

    @WamProperty(index = 1893, type = WamType.INTEGER)
    OptionalLong waCallingInitDlBweReuse2p();

    @WamProperty(index = 1894, type = WamType.INTEGER)
    OptionalLong waCallingInitUlBweReuse2p();

    @WamProperty(index = 1895, type = WamType.INTEGER)
    OptionalLong waCallingSfuLast2pSegmentSbwe();

    @WamProperty(index = 1896, type = WamType.INTEGER)
    OptionalLong waCallingSfuLast2pSegmentTotalRxBitrate();

    @WamProperty(index = 891, type = WamType.INTEGER)
    OptionalLong waLongFreezeCount();

    @WamProperty(index = 890, type = WamType.INTEGER)
    OptionalLong waReconnectFreezeCount();

    @WamProperty(index = 1547, type = WamType.INTEGER)
    OptionalLong waSframeAudioRxDupPktsCnt();

    @WamProperty(index = 1548, type = WamType.INTEGER)
    OptionalLong waSframeAudioRxErrorMissingKey();

    @WamProperty(index = 1549, type = WamType.INTEGER)
    OptionalLong waSframeAudioRxRejectPktsCnt();

    @WamProperty(index = 1550, type = WamType.INTEGER)
    OptionalLong waSframeAudioTxErrorPktCnt();

    @WamProperty(index = 1551, type = WamType.INTEGER)
    OptionalLong waSframeVideoHqTxErrorPktCnt();

    @WamProperty(index = 1552, type = WamType.INTEGER)
    OptionalLong waSframeVideoLqTxErrorPktCnt();

    @WamProperty(index = 1553, type = WamType.INTEGER)
    OptionalLong waSframeVideoRxDupPktsCnt();

    @WamProperty(index = 1554, type = WamType.INTEGER)
    OptionalLong waSframeVideoRxErrorMissingKey();

    @WamProperty(index = 1555, type = WamType.INTEGER)
    OptionalLong waSframeVideoRxRejectPktsCnt();

    @WamProperty(index = 889, type = WamType.INTEGER)
    OptionalLong waShortFreezeCount();

    @WamProperty(index = 1346, type = WamType.ENUM)
    Optional<WaVoipHistoryCallRedialStatus> waVoipHistoryCallRedialStatus();

    @WamProperty(index = 1162, type = WamType.INTEGER)
    OptionalLong waVoipHistoryGetVideoTxBitrateBySelfAndPeerIpStrResult();

    @WamProperty(index = 1163, type = WamType.BOOLEAN)
    Optional<Boolean> waVoipHistoryGetVideoTxBitrateBySelfAndPeerIpStrSuccess();

    @WamProperty(index = 1164, type = WamType.INTEGER)
    OptionalLong waVoipHistoryGetVideoTxBitrateBySelfIpStrResult();

    @WamProperty(index = 1165, type = WamType.BOOLEAN)
    Optional<Boolean> waVoipHistoryGetVideoTxBitrateBySelfIpStrSuccess();

    @WamProperty(index = 834, type = WamType.BOOLEAN)
    Optional<Boolean> waVoipHistoryIpAddressNotAvailable();

    @WamProperty(index = 1343, type = WamType.BOOLEAN)
    Optional<Boolean> waVoipHistoryIsCallParticipantRecordSaved();

    @WamProperty(index = 737, type = WamType.BOOLEAN)
    Optional<Boolean> waVoipHistoryIsCallRecordLoaded();

    @WamProperty(index = 738, type = WamType.BOOLEAN)
    Optional<Boolean> waVoipHistoryIsCallRecordSaved();

    @WamProperty(index = 769, type = WamType.BOOLEAN)
    Optional<Boolean> waVoipHistoryIsInitialized();

    @WamProperty(index = 1344, type = WamType.INTEGER)
    OptionalLong waVoipHistoryNumOfCallParticipantRecordFound();

    @WamProperty(index = 1166, type = WamType.INTEGER)
    OptionalLong waVoipHistoryNumOfCallRecordFoundByMatchingSelfAndPeerIpStr();

    @WamProperty(index = 1167, type = WamType.INTEGER)
    OptionalLong waVoipHistoryNumOfCallRecordFoundByMatchingSelfIpStr();

    @WamProperty(index = 739, type = WamType.INTEGER)
    OptionalLong waVoipHistoryNumOfCallRecordLoaded();

    @WamProperty(index = 770, type = WamType.ENUM)
    Optional<WaVoipHistorySaveCallRecordConditionCheckStatus> waVoipHistorySaveCallRecordConditionCheckStatus();

    @WamProperty(index = 1601, type = WamType.INTEGER)
    OptionalLong warpClientDupRtx();

    @WamProperty(index = 1602, type = WamType.INTEGER)
    OptionalLong warpClientNackRtx();

    @WamProperty(index = 2140, type = WamType.INTEGER)
    OptionalLong warpClientNackRtxAudio();

    @WamProperty(index = 2141, type = WamType.INTEGER)
    OptionalLong warpClientNackRtxVideo();

    @WamProperty(index = 656, type = WamType.FLOAT)
    OptionalDouble warpHeaderRxTotalBytes();

    @WamProperty(index = 655, type = WamType.FLOAT)
    OptionalDouble warpHeaderTxTotalBytes();

    @WamProperty(index = 1118, type = WamType.INTEGER)
    OptionalLong warpMiRxPktErrorCount();

    @WamProperty(index = 1117, type = WamType.INTEGER)
    OptionalLong warpMiTxPktErrorCount();

    @WamProperty(index = 1154, type = WamType.INTEGER)
    OptionalLong warpRelayChangeDetectCount();

    @WamProperty(index = 1852, type = WamType.INTEGER)
    OptionalLong warpRxE2eSrtp();

    @WamProperty(index = 1853, type = WamType.INTEGER)
    OptionalLong warpRxHbhSrtp();

    @WamProperty(index = 1854, type = WamType.INTEGER)
    OptionalLong warpRxNoPdAttr();

    @WamProperty(index = 746, type = WamType.INTEGER)
    OptionalLong warpRxPktErrorCount();

    @WamProperty(index = 1737, type = WamType.INTEGER)
    OptionalLong warpServerDupAudioRtxUsed();

    @WamProperty(index = 1603, type = WamType.INTEGER)
    OptionalLong warpServerDupRtx();

    @WamProperty(index = 1604, type = WamType.INTEGER)
    OptionalLong warpServerNackRtx();

    @WamProperty(index = 2142, type = WamType.INTEGER)
    OptionalLong warpServerNackRtxAudio();

    @WamProperty(index = 2143, type = WamType.INTEGER)
    OptionalLong warpServerNackRtxVideo();

    @WamProperty(index = 2150, type = WamType.INTEGER)
    OptionalLong warpSnErrorBadCnt();

    @WamProperty(index = 2151, type = WamType.INTEGER)
    OptionalLong warpSnErrorOooCnt();

    @WamProperty(index = 2152, type = WamType.INTEGER)
    OptionalLong warpSnErrorResetCnt();

    @WamProperty(index = 2153, type = WamType.INTEGER)
    OptionalLong warpSnRxCnt();

    @WamProperty(index = 1855, type = WamType.INTEGER)
    OptionalLong warpTxE2eSrtp();

    @WamProperty(index = 1856, type = WamType.INTEGER)
    OptionalLong warpTxHbhSrtp();

    @WamProperty(index = 745, type = WamType.INTEGER)
    OptionalLong warpTxPktErrorCount();

    @WamProperty(index = 1089, type = WamType.TIMER)
    Optional<Instant> wavFileWriteMaxLatency();

    @WamProperty(index = 429, type = WamType.INTEGER)
    OptionalLong weakCellularNetConditionDetected();

    @WamProperty(index = 2321, type = WamType.INTEGER)
    OptionalLong weakNetConditionByBadCallMl();

    @WamProperty(index = 2061, type = WamType.INTEGER)
    OptionalLong weakNetConditionByJitter();

    @WamProperty(index = 2062, type = WamType.INTEGER)
    OptionalLong weakNetConditionByLossPeriod();

    @WamProperty(index = 2063, type = WamType.INTEGER)
    OptionalLong weakNetConditionByPlr();

    @WamProperty(index = 2064, type = WamType.INTEGER)
    OptionalLong weakNoneNetConditionDetected();

    @WamProperty(index = 430, type = WamType.INTEGER)
    OptionalLong weakWifiNetConditionDetected();

    @WamProperty(index = 397, type = WamType.INTEGER)
    OptionalLong weakWifiSwitchToDefNetSuccess();

    @WamProperty(index = 395, type = WamType.INTEGER)
    OptionalLong weakWifiSwitchToDefNetSuccessByPeriodicalCheck();

    @WamProperty(index = 396, type = WamType.INTEGER)
    OptionalLong weakWifiSwitchToDefNetTriggered();

    @WamProperty(index = 394, type = WamType.INTEGER)
    OptionalLong weakWifiSwitchToDefNetTriggeredByPeriodicalCheck();

    @WamProperty(index = 399, type = WamType.INTEGER)
    OptionalLong weakWifiSwitchToNonDefNetFalsePositive();

    @WamProperty(index = 400, type = WamType.INTEGER)
    OptionalLong weakWifiSwitchToNonDefNetSuccess();

    @WamProperty(index = 398, type = WamType.INTEGER)
    OptionalLong weakWifiSwitchToNonDefNetTriggered();

    @WamProperty(index = 3008, type = WamType.BOOLEAN)
    Optional<Boolean> wearableCallHaveUsedPov();

    @WamProperty(index = 3004, type = WamType.STRING)
    Optional<String> wearableDeviceTypes();

    @WamProperty(index = 2903, type = WamType.INTEGER)
    OptionalLong webAudioCaptureOverrunCount();

    @WamProperty(index = 2904, type = WamType.TIMER)
    Optional<Instant> webAudioCaptureSabFillAvgMs();

    @WamProperty(index = 2905, type = WamType.TIMER)
    Optional<Instant> webAudioCaptureSabFillMaxMs();

    @WamProperty(index = 2906, type = WamType.TIMER)
    Optional<Instant> webAudioCaptureStartupSeedMs();

    @WamProperty(index = 2887, type = WamType.TIMER)
    Optional<Instant> webAudioCtxBaseLatencyMs();

    @WamProperty(index = 2888, type = WamType.TIMER)
    Optional<Instant> webAudioCtxOutputLatencyAvgMs();

    @WamProperty(index = 2889, type = WamType.TIMER)
    Optional<Instant> webAudioCtxOutputLatencyMaxMs();

    @WamProperty(index = 2890, type = WamType.TIMER)
    Optional<Instant> webAudioPlatformDelayMs();

    @WamProperty(index = 2891, type = WamType.TIMER)
    Optional<Instant> webAudioRbDelayAvgMs();

    @WamProperty(index = 2892, type = WamType.TIMER)
    Optional<Instant> webAudioRbDelayMaxMs();

    @WamProperty(index = 2893, type = WamType.INTEGER)
    OptionalLong webAudioRbFillMaxPct();

    @WamProperty(index = 2894, type = WamType.INTEGER)
    OptionalLong webAudioUnderrunTotal();

    @WamProperty(index = 2838, type = WamType.FLOAT)
    OptionalDouble webAvSyncAvgDeltaMs();

    @WamProperty(index = 2839, type = WamType.INTEGER)
    OptionalLong webAvSyncCalibrationCount();

    @WamProperty(index = 2840, type = WamType.INTEGER)
    OptionalLong webAvSyncDeviceChangeResetCount();

    @WamProperty(index = 2841, type = WamType.BOOLEAN)
    Optional<Boolean> webAvSyncEnabled();

    @WamProperty(index = 2842, type = WamType.INTEGER)
    OptionalLong webAvSyncForceRecalibrationCount();

    @WamProperty(index = 2843, type = WamType.INTEGER)
    OptionalLong webAvSyncFramesEvictedTotal();

    @WamProperty(index = 2844, type = WamType.INTEGER)
    OptionalLong webAvSyncFramesHeldTotal();

    @WamProperty(index = 2845, type = WamType.INTEGER)
    OptionalLong webAvSyncFramesRenderedInSync();

    @WamProperty(index = 2846, type = WamType.INTEGER)
    OptionalLong webAvSyncFramesRenderedLate();

    @WamProperty(index = 2847, type = WamType.FLOAT)
    OptionalDouble webAvSyncMaxAbsDeltaMs();

    @WamProperty(index = 2848, type = WamType.INTEGER)
    OptionalLong webAvSyncMaxQueueDepth();

    @WamProperty(index = 2849, type = WamType.FLOAT)
    OptionalDouble webAvSyncP50DeltaMs();

    @WamProperty(index = 2850, type = WamType.FLOAT)
    OptionalDouble webAvSyncP95DeltaMs();

    @WamProperty(index = 2851, type = WamType.INTEGER)
    OptionalLong webAvSyncRecalibrationCount();

    @WamProperty(index = 2852, type = WamType.INTEGER)
    OptionalLong webAvSyncTimeOutOfSyncMs();

    @WamProperty(index = 2895, type = WamType.FLOAT)
    OptionalDouble webSctpBaPeakAvgBytes();

    @WamProperty(index = 2896, type = WamType.FLOAT)
    OptionalDouble webSctpBaPeakMaxBytes();

    @WamProperty(index = 2897, type = WamType.FLOAT)
    OptionalDouble webSctpBaTailAvgBytes();

    @WamProperty(index = 2898, type = WamType.FLOAT)
    OptionalDouble webSctpBaTailMaxBytes();

    @WamProperty(index = 2900, type = WamType.BOOLEAN)
    Optional<Boolean> webTransportUsed();

    @WamProperty(index = 2907, type = WamType.TIMER)
    Optional<Instant> webVideoCaptureCaptureToEncodedAvgMs();

    @WamProperty(index = 2908, type = WamType.TIMER)
    Optional<Instant> webVideoCapturePresentToConstructAvgMs();

    @WamProperty(index = 3032, type = WamType.STRING)
    Optional<String> webVideoEncoderType();

    @WamProperty(index = 1984, type = WamType.BOOLEAN)
    Optional<Boolean> webrtcCompatible();

    @WamProperty(index = 2416, type = WamType.STRING)
    Optional<String> wifiInfoAtEnd();

    @WamProperty(index = 2417, type = WamType.STRING)
    Optional<String> wifiInfoAtStart();

    @WamProperty(index = 263, type = WamType.INTEGER)
    OptionalLong wifiRssiAtCallStart();

    @WamProperty(index = 2688, type = WamType.BOOLEAN)
    Optional<Boolean> windowDragged();

    @WamProperty(index = 2689, type = WamType.BOOLEAN)
    Optional<Boolean> windowResized();

    @WamProperty(index = 2696, type = WamType.STRING)
    Optional<String> wzav1Version();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<XmppStatus> xmppStatus();

    @WamProperty(index = 1493, type = WamType.STRING)
    Optional<String> xpopCallPeerRelayIp();

    @WamProperty(index = 2420, type = WamType.TIMER)
    Optional<Instant> xpopPop2popRttMs();

    @WamProperty(index = 1409, type = WamType.INTEGER)
    OptionalLong xpopRelayCount();

    @WamProperty(index = 1410, type = WamType.INTEGER)
    OptionalLong xpopRelayErrorBitmap();

    @WamProperty(index = 1515, type = WamType.INTEGER)
    OptionalLong xpopTo1popFallbackCnt();

    @WamProperty(index = 2230, type = WamType.INTEGER)
    OptionalLong xpopTo1popFallbackCount();

    @WamProperty(index = 1088, type = WamType.TIMER)
    Optional<Instant> zedFileWriteMaxLatency();
}
