package it.auties.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.protobuf.ByteString;
import com.google.protobuf.UnknownFieldSetLite;
import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.protobuf.beta.*;
import it.auties.whatsapp.socket.Proto;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

@Accessors(fluent = true)
public class ProtocTest {
    private static final ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
    @Getter
    private static final WhatsappKeys keys = new WhatsappKeys();
    
    public static void main(String[] args) throws Exception {

        var aPayload = createPayload();
        var bPayload = createPayloadBeta();

        var aDecoded = Proto.ClientPayload.parseFrom(aPayload);
        var bDecoded = Proto.ClientPayload.parseFrom(bPayload);

        System.out.println("Custom: " + aDecoded);
        System.out.println();
        System.out.println("Google: " + bDecoded);

    }

    @SneakyThrows
    private static byte[] createPayload() {
        var payload = createClientPayload(keys().mayRestore());
        if (!keys().mayRestore()) {
            payload.regData(createRegisterData());
        }else {
            payload.username(Long.parseLong(keys().me().user()))
                    .device(keys().me().device());
        }

        return ProtobufEncoder.encode(payload.build());
    }

    private static ClientPayload.ClientPayloadBuilder createClientPayload(boolean passive) {
        return ClientPayload.builder()
                .connectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                .connectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                .userAgent(createUserAgent())
                .passive(passive)
                .webInfo(WebInfo.builder().webSubPlatform(WebInfo.WebInfoWebSubPlatform.WEB_BROWSER).build());
    }

    private static UserAgent createUserAgent() {
        return UserAgent.builder()
                .appVersion(new AppVersion(2, 2126, 14, 0, 0))
                .platform(UserAgent.UserAgentPlatform.WEB)
                .releaseChannel(UserAgent.UserAgentReleaseChannel.RELEASE)
                .mcc("000")
                .mnc("000")
                .osVersion("0.1")
                .manufacturer("")
                .device("Desktop")
                .osBuildNumber("0.1")
                .localeLanguageIso6391("en")
                .localeCountryIso31661Alpha2("en")
                .build();
    }

    @SneakyThrows
    private static CompanionRegData createRegisterData() {
        return CompanionRegData.builder()
                .buildHash(Base64.getDecoder().decode("S9Kdc4pc4EJryo21snc5cg=="))
                .companionProps(ProtobufEncoder.encode(createCompanionProps()))
                .eRegid(BinaryArray.of(keys().id(), 4).data())
                .eKeytype(BinaryArray.of(5, 1).data())
                .eIdent(keys().signedIdentityKey().publicKey())
                .eSkeyId(BinaryArray.of(keys().signedPreKey().id(), 3).data())
                .eSkeyVal(keys().signedPreKey().keyPair().publicKey())
                .eSkeySig(keys().signedPreKey().signature())
                .build();
    }

    private static CompanionProps createCompanionProps() {
        return CompanionProps.builder()
                .os("Windows")
                .version(new AppVersion(10, 0, 0, 0, 0))
                .platformType(CompanionProps.CompanionPropsPlatformType.CHROME)
                .requireFullSync(false)
                .build();
    }


    private static byte[] createPayloadBeta() {
        var payload = createClientPayloadBeta(keys().mayRestore());
        if (!keys().mayRestore()) {
            return payload.setRegData(createRegisterDataBeta())
                    .build()
                    .toByteArray();
        }

        return payload.setUsername(Long.parseLong(keys().me().user()))
                .setDevice(keys().me().device())
                .build()
                .toByteArray();
    }

    private static Proto.ClientPayload.Builder createClientPayloadBeta(boolean passive) {
        return Proto.ClientPayload.newBuilder()
                .setConnectReason(Proto.ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                .setConnectType(Proto.ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                .setUserAgent(createUserAgentBeta())
                .setPassive(passive)
                .setWebInfo(Proto.WebInfo.newBuilder().setWebSubPlatform(Proto.WebInfo.WebInfoWebSubPlatform.WEB_BROWSER));
    }

    private static Proto.UserAgent createUserAgentBeta() {
        return Proto.UserAgent.newBuilder()
                .setAppVersion(Proto.AppVersion.newBuilder().setPrimary(2).setSecondary(2126).setTertiary(14).build())
                .setPlatform(Proto.UserAgent.UserAgentPlatform.WEB)
                .setReleaseChannel(Proto.UserAgent.UserAgentReleaseChannel.RELEASE)
                .setMcc("000")
                .setMnc("000")
                .setOsVersion("0.1")
                .setManufacturer("")
                .setDevice("Desktop")
                .setOsBuildNumber("0.1")
                .setLocaleLanguageIso6391("en")
                .setLocaleCountryIso31661Alpha2("en")
                .build();
    }

    private static Proto.CompanionRegData createRegisterDataBeta() {
        return Proto.CompanionRegData.newBuilder()
                .setBuildHash(ByteString.copyFrom(Base64.getDecoder().decode("S9Kdc4pc4EJryo21snc5cg==")))
                .setCompanionProps(ByteString.copyFrom(createCompanionPropsBeta().toByteArray()))
                .setERegid(ByteString.copyFrom(BinaryArray.of(keys().id(), 4).data()))
                .setEKeytype(ByteString.copyFrom(BinaryArray.of(5, 1).data()))
                .setEIdent(ByteString.copyFrom(keys().signedIdentityKey().publicKey()))
                .setESkeyId(ByteString.copyFrom(BinaryArray.of(keys().signedPreKey().id(), 3).data()))
                .setESkeyVal(ByteString.copyFrom(keys().signedPreKey().keyPair().publicKey()))
                .setESkeySig(ByteString.copyFrom(keys().signedPreKey().signature()))
                .build();
    }

    private static Proto.CompanionProps createCompanionPropsBeta() {
        return Proto.CompanionProps.newBuilder()
                .setOs("Windows")
                .setVersion(Proto.AppVersion.newBuilder().setPrimary(10).build())
                .setPlatformType(Proto.CompanionProps.CompanionPropsPlatformType.CHROME)
                .setRequireFullSync(false)
                .build();
    }
}
