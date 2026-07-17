package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AndroidKeystoreStateType;
import com.github.auties00.cobalt.wire.wam.type.ConnectionOriginType;
import com.github.auties00.cobalt.wire.wam.type.ConnectionSequenceStepType;
import com.github.auties00.cobalt.wire.wam.type.DnsResolutionMethodType;
import com.github.auties00.cobalt.wire.wam.type.LoginDnsResolverType;
import com.github.auties00.cobalt.wire.wam.type.LoginHostType;
import com.github.auties00.cobalt.wire.wam.type.LoginPortNumber;
import com.github.auties00.cobalt.wire.wam.type.LoginResultType;
import com.github.auties00.cobalt.wire.wam.type.StreamSocketProviderType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebLoginWamEvent")
@WamEvent(id = 460)
public interface LoginEvent extends WamEventSpec {
    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<AndroidKeystoreStateType> androidKeystoreState();

    @WamProperty(index = 20, type = WamType.STRING)
    Optional<String> appContext();

    @WamProperty(index = 21, type = WamType.INTEGER)
    OptionalLong appContextBitfield();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<ConnectionOriginType> connectionOrigin();

    @WamProperty(index = 11, type = WamType.ENUM)
    Optional<ConnectionSequenceStepType> connectionSequenceStep();

    @WamProperty(index = 5, type = WamType.TIMER)
    Optional<Instant> connectionT();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<DnsResolutionMethodType> dnsResolutionMethod();

    @WamProperty(index = 13, type = WamType.ENUM)
    Optional<LoginDnsResolverType> loginDnsResolver();

    @WamProperty(index = 27, type = WamType.BOOLEAN)
    Optional<Boolean> loginHistoryStepResult();

    @WamProperty(index = 14, type = WamType.ENUM)
    Optional<LoginHostType> loginIpSource();

    @WamProperty(index = 15, type = WamType.ENUM)
    Optional<LoginPortNumber> loginPort();

    @WamProperty(index = 19, type = WamType.STRING)
    Optional<String> loginResolvedPop();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<LoginResultType> loginResult();

    @WamProperty(index = 22, type = WamType.ENUM)
    Optional<StreamSocketProviderType> loginSocketProvider();

    @WamProperty(index = 3, type = WamType.TIMER)
    Optional<Instant> loginT();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong logoutSessionId();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> longConnect();

    @WamProperty(index = 23, type = WamType.INTEGER)
    OptionalLong mnsDnsCacheAge();

    @WamProperty(index = 24, type = WamType.BOOLEAN)
    Optional<Boolean> networkIsVpn();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong noiseProtocolVersion();

    @WamProperty(index = 25, type = WamType.INTEGER)
    OptionalLong numIpv4Addresses();

    @WamProperty(index = 26, type = WamType.INTEGER)
    OptionalLong numIpv6Addresses();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> passive();

    @WamProperty(index = 17, type = WamType.INTEGER)
    OptionalLong pendingAcksCount();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong retryCount();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong sequenceStep();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong serverErrorCode();

    @WamProperty(index = 29, type = WamType.INTEGER)
    OptionalLong traceIdInt();

    @WamProperty(index = 28, type = WamType.INTEGER)
    OptionalLong unprocessedMessageCount();
}
