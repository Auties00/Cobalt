package it.auties.whatsapp.util;

import it.auties.whatsapp.api.WhatsappOptions.MobileOptions;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.crypto.MD5;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import it.auties.whatsapp.model.mobile.VerificationCodeResponse;
import it.auties.whatsapp.model.request.Attributes;
import it.auties.whatsapp.util.Specification.Whatsapp;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HexFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static java.util.Base64.getUrlEncoder;
import static java.util.Map.entry;

@UtilityClass
public class RegistrationHelper implements JacksonProvider {
    public void register(@NonNull Keys keys, @NonNull MobileOptions options) {
        var phoneNumber = PhoneNumber.of(options.phoneNumber());
        var userAgent = createUserAgent(options);
        var response = askForVerificationCode(keys, phoneNumber, userAgent, options.verificationCodeMethod());
        var code = options.verificationCodeHandler().apply(response);
        verify(keys, options, code);
    }

    public void verify(@NonNull Keys keys, @NonNull MobileOptions options, @NonNull String code) {
        var phoneNumber = PhoneNumber.of(options.phoneNumber());
        var userAgent = createUserAgent(options);
        sendVerificationCode(keys, phoneNumber, userAgent, code);
        keys.registered(true);
    }

    private String createUserAgent(MobileOptions options) {
        return "WhatsApp/%s %s/%s Device/%s-%s".formatted(options.version(), options.osName(), options.osVersion(), options.deviceManufacturer(), options.deviceName());
    }

    private void sendVerificationCode(Keys keys, PhoneNumber phoneNumber, String userAgent, String code) {
        try {
            var registerOptions = getRegistrationOptions(keys, phoneNumber, entry("code", code.replaceAll("-", "")));
            var codeResponse = sendRegistrationRequest(userAgent,"/register", registerOptions);
            var phoneNumberResponse = JSON.readValue(codeResponse.body(), VerificationCodeResponse.class);
            Validate.isTrue(phoneNumberResponse.status()
                    .isSuccessful(), "Unexpected response: %s", phoneNumberResponse);
        } catch (IOException exception) {
            throw new RuntimeException("Cannot send verification code", exception);
        }
    }

    private VerificationCodeResponse askForVerificationCode(Keys keys, PhoneNumber phoneNumber, String userAgent, VerificationCodeMethod method) {
        try {
            var codeOptions = getRegistrationOptions(keys, phoneNumber, entry("mcc", phoneNumber.countryCode()
                    .mcc()), entry("mnc", phoneNumber.countryCode()
                    .mnc()), entry("sim_mcc", "000"), entry("sim_mnc", "000"), entry("method", method.type()), entry("reason", ""), entry("hasav", "1"));
            var codeResponse = sendRegistrationRequest(userAgent, "/code", codeOptions);
            var phoneNumberResponse = JSON.readValue(codeResponse.body(), VerificationCodeResponse.class);
            Validate.isTrue(phoneNumberResponse.status()
                    .isSuccessful(), "Unexpected response: %s", phoneNumberResponse);
            return phoneNumberResponse;
        } catch (IOException exception) {
            throw new RuntimeException("Cannot get verification code", exception);
        }
    }

    private HttpResponse<String> sendRegistrationRequest(String userAgent, String path, Map<String, Object> params) {
        try {
            var client = HttpClient.newHttpClient();
            var request = HttpRequest.newBuilder()
                    .uri(URI.create("%s%s?%s".formatted(Whatsapp.MOBILE_REGISTRATION_ENDPOINT, path, toFormParams(params))))
                    .GET()
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", userAgent)
                    .build();
            System.out.println("Sending request to: " + request.uri());
            var result = client.send(request, BodyHandlers.ofString());
            System.out.println("Received: " + result.body());
            return result;
        } catch (IOException | InterruptedException exception) {
            throw new RuntimeException("Cannot get verification code", exception);
        }
    }

    @SafeVarargs
    private Map<String, Object> getRegistrationOptions(Keys keys, PhoneNumber phoneNumber, Entry<String, Object>... attributes) {
        return Attributes.of(attributes)
                .put("cc", phoneNumber.countryCode().prefix())
                .put("in", phoneNumber.number())
                .put("lg", "en")
                .put("lc", "GB")
                .put("mistyped", "6")
                .put("authkey", getUrlEncoder().encodeToString(keys.noiseKeyPair().publicKey()))
                .put("e_regid", getUrlEncoder().encodeToString(keys.encodedId()))
                .put("e_keytype", "BQ")
                .put("e_ident", getUrlEncoder().encodeToString(keys.identityKeyPair().publicKey()))
                .put("e_skey_id", getUrlEncoder().encodeToString(keys.signedKeyPair().encodedId()))
                .put("e_skey_val", getUrlEncoder().encodeToString(keys.signedKeyPair().publicKey()))
                .put("e_skey_sig", getUrlEncoder().encodeToString(keys.signedKeyPair().signature()))
                .put("fdid", keys.phoneId())
                .put("expid", keys.deviceId())
                .put("network_radio_type", "1")
                .put("simnum", "1")
                .put("hasinrc", "1")
                .put("pid", ThreadLocalRandom.current().nextInt(1000))
                .put("rc", "0")
                .put("id", keys.identityId())
                .put("token", HexFormat.of().formatHex(MD5.calculate(Whatsapp.MOBILE_TOKEN + phoneNumber.number())))
                .toMap();
    }

    private String toFormParams(Map<String, Object> values) {
        return values.entrySet()
                .stream()
                .map(entry -> "%s=%s".formatted(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("&"));
    }
}
