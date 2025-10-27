package com.github.auties00.cobalt.client.version;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.model.proto.auth.Version;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class WhatsAppIosClientVersion implements WhatsAppMobileClientVersion {
    private static final URI MOBILE_PERSONAL_IOS_URL = URI.create("https://itunes.apple.com/lookup?bundleId=net.whatsapp.WhatsApp");
    private static final URI MOBILE_BUSINESS_IOS_URL = URI.create("https://itunes.apple.com/lookup?bundleId=net.whatsapp.WhatsAppSMB");
    private static final String MOBILE_IOS_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3.1 Mobile/15E148 Safari/604.1";

    private static volatile WhatsAppIosClientVersion personalIpaInfo;
    private static final Object personalIpaInfoLock = new Object();
    private static volatile WhatsAppIosClientVersion businessIpaInfo;
    private static final Object businessIpaInfoLock = new Object();

    private static final String MOBILE_IOS_STATIC = "0a1mLfGUIBVrMKF1RdvLI5lkRBvof6vn0fD2QRSM";
    private static final String MOBILE_BUSINESS_IOS_STATIC = "USUDuDYDeQhY4RF2fCSp5m3F6kJ1M2J8wS7bbNA2";

    private final Version version;
    private final boolean business;

    private WhatsAppIosClientVersion(Version version, boolean business) {
        this.version = version;
        this.business = business;
    }

    public static WhatsAppIosClientVersion ofPersonal() {
        if (personalIpaInfo == null) {
            synchronized (personalIpaInfoLock) {
                if(personalIpaInfo == null) {
                    personalIpaInfo = queryIpaInfo(false);
                }
            }
        }
        return personalIpaInfo;
    }

    public static WhatsAppIosClientVersion ofBusiness() {
        if (businessIpaInfo == null) {
            synchronized (businessIpaInfoLock) {
                if(businessIpaInfo == null) {
                    businessIpaInfo = queryIpaInfo(true);
                }
            }
        }
        return businessIpaInfo;
    }

    private static WhatsAppIosClientVersion queryIpaInfo(boolean business) {
        try(var httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()) {
            var request = HttpRequest.newBuilder()
                    .uri(business ? MOBILE_BUSINESS_IOS_URL : MOBILE_PERSONAL_IOS_URL)
                    .header("User-Agent", MOBILE_IOS_USER_AGENT)
                    .GET()
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IOException("HTTP request failed with status code: " + response.statusCode());
            }

            var jsonObject = JSON.parseObject(response.body());
            var results = jsonObject.getJSONArray("results");
            if (results == null || results.isEmpty()) {
                return null;
            }

            var result = results.getJSONObject(0);
            var version = result.getString("version");
            if (version == null) {
                return null;
            }

            if (!version.startsWith("2.")) {
                version = "2." + version;
            }

            var parsedVersion = Version.of(version);
            return new WhatsAppIosClientVersion(parsedVersion, business);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Cannot query iOS version", e);
        }
    }

    @Override
    public Version version() {
        return version;
    }

    @Override
    public boolean business() {
        return business;
    }

    @Override
    public String computeRegistrationToken(long nationalPhoneNumber) {
        try {
            var staticToken = business ? MOBILE_BUSINESS_IOS_STATIC : MOBILE_IOS_STATIC;
            var token = staticToken + HexFormat.of().formatHex(version.toHash()) + nationalPhoneNumber;
            var digest = MessageDigest.getInstance("MD5");
            digest.update(token.getBytes());
            var result = digest.digest();
            return HexFormat.of().formatHex(result);
        } catch (NoSuchAlgorithmException exception) {
            throw new UnsupportedOperationException("Missing md5 implementation", exception);
        }
    }
}
