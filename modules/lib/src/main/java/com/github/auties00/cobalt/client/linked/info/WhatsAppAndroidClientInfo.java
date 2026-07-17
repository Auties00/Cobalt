package com.github.auties00.cobalt.client.linked.info;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientAppVersion;
import com.github.auties00.cobalt.util.PlayStoreUtils;
import net.dongliu.apk.parser.ByteArrayApkFile;
import net.dongliu.apk.parser.bean.ApkSigner;
import net.dongliu.apk.parser.bean.CertificateMeta;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger.Level;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.zip.ZipInputStream;

/**
 * {@link WhatsAppMobileClientInfo} variant for the consumer ({@code com.whatsapp}) and business ({@code com.whatsapp.w4b})
 * Android WhatsApp APKs.
 *
 * <p>The first call per variant downloads the current APK through the anonymous Play Store pipeline, parses the version and
 * signing certificates, derives the HMAC key, and persists the result to a per variant cache file under
 * {@code $user.home/.cobalt/cache/} so subsequent JVMs avoid the download as long as the Play Store {@code versionCode} has
 * not changed.
 *
 * @implNote This implementation has no WA Web counterpart; the Android registration token scheme lives entirely inside the
 *           Android WhatsApp APK and is reverse engineered here. The cache file is keyed by Play Store {@code versionCode}
 *           so a real WhatsApp release naturally invalidates it; on a cache miss the heavy download runs at most once per
 *           JVM per variant behind a double checked lock.
 * @see WhatsAppMobileClientInfo
 */
final class WhatsAppAndroidClientInfo implements WhatsAppMobileClientInfo {
    /**
     * The logger for {@link WhatsAppAndroidClientInfo}.
     */
    private static final System.Logger LOGGER = Log.get(WhatsAppAndroidClientInfo.class);

    /**
     * Holds the static salt fed into the PBKDF2-HMAC-SHA1 routine that derives the registration token HMAC key.
     *
     * @implNote This implementation embeds the salt directly, reverse engineered from the Android WhatsApp binary and
     *           identical for consumer and business builds, because rotating it would require coordinating with WhatsApp;
     *           it has been stable across many releases.
     */
    private static final byte[] MOBILE_ANDROID_SALT = Base64.getDecoder().decode("PkTwKSZqUfAUyR0rPQ8hYJ0wNsQQ3dW1+3SCnyTXIfEAxxS75FwkDf47wNv/c8pP3p0GXKR6OOQmhyERwx74fw1RYSU10I4r1gyBVDbRJ40pidjM41G1I1oN");

    /**
     * Holds the known APK paths under which the {@code about_logo.png} drawable has shipped across WhatsApp releases.
     *
     * <p>The PBKDF2 password derivation in {@link #getSecretKey(String, byte[])} consumes the bytes of the first matching
     * entry.
     *
     * @implNote This implementation searches the base APK first and falls back to density configuration splits because
     *           newer App Bundle releases moved density qualified drawables out of the base APK and into per density splits;
     *           trying multiple paths absorbs reorganisations of the resource bucketing across releases.
     */
    private static final List<String> ABOUT_LOGO_PATHS = List.of(
            "res/drawable-hdpi/about_logo.png",
            "res/drawable-hdpi-v4/about_logo.png",
            "res/drawable-xxhdpi-v4/about_logo.png"
    );

    /**
     * Holds the Play Store package identifier of the consumer WhatsApp APK.
     */
    private static final String PERSONAL_PACKAGE = "com.whatsapp";

    /**
     * Holds the Play Store package identifier of the WhatsApp Business APK.
     */
    private static final String BUSINESS_PACKAGE = "com.whatsapp.w4b";

    /**
     * Holds the resolved consumer APK identity once it has been downloaded.
     *
     * <p>Populated lazily by the first call to {@link #ofPersonal()} and reused by every subsequent caller in the JVM.
     *
     * @implNote This implementation pairs the field with {@link #personalApkInfoLock} for the double checked locking idiom;
     *           the {@code volatile} modifier publishes a fully constructed instance to readers on the unsynchronised fast
     *           path.
     */
    private static volatile WhatsAppAndroidClientInfo personalApkInfo;

    /**
     * Serialises initialisation of {@link #personalApkInfo}.
     */
    private static final Object personalApkInfoLock = new Object();

    /**
     * Holds the resolved business APK identity once it has been downloaded.
     *
     * <p>Populated lazily by the first call to {@link #ofBusiness()} and reused by every subsequent caller in the JVM.
     *
     * @implNote This implementation pairs the field with {@link #businessApkInfoLock} for the double checked locking idiom;
     *           the {@code volatile} modifier publishes a fully constructed instance to readers on the unsynchronised fast
     *           path.
     */
    private static volatile WhatsAppAndroidClientInfo businessApkInfo;

    /**
     * Serialises initialisation of {@link #businessApkInfo}.
     */
    private static final Object businessApkInfoLock = new Object();

    /**
     * Holds the resolved {@link ClientAppVersion} read from the APK's {@code AndroidManifest.xml}.
     */
    private final ClientAppVersion version;

    /**
     * Holds the MD5 digest of the APK's {@code classes.dex} entry.
     *
     * <p>Folded into the registration token HMAC by {@link #computeRegistrationToken(long)} so the server can verify the
     * caller knows the contents of a real signed DEX file.
     */
    private final byte[] md5Hash;

    /**
     * Holds the HMAC-SHA1 secret key derived from the package name and the {@code about_logo.png} asset.
     *
     * <p>Used as the key of the registration token HMAC in {@link #computeRegistrationToken(long)} and never exposed to
     * callers.
     */
    private final SecretKeySpec secretKey;

    /**
     * Holds the raw X.509 DER bytes of every certificate that signs the APK.
     *
     * <p>Folded into the registration token HMAC in order by {@link #computeRegistrationToken(long)} so the server can
     * verify the signature chain identity.
     */
    private final byte[][] certificates;

    /**
     * Holds whether this instance represents the WhatsApp Business APK rather than the consumer APK.
     */
    private final boolean business;

    /**
     * Holds the Play Store {@code versionCode} the APK was fetched at.
     *
     * <p>Used purely as the invalidation key for the on disk JSON cache and never advertised to the server.
     */
    private final int versionCode;

    /**
     * Constructs an immutable instance from the values extracted out of the APK.
     *
     * @param version      the parsed application version
     * @param versionCode  the Play Store {@code versionCode} used as the cache invalidation key
     * @param md5Hash      the MD5 digest of {@code classes.dex}
     * @param secretKey    the derived HMAC-SHA1 key
     * @param certificates the APK signing certificates in DER form
     * @param business     whether this represents the business variant
     */
    private WhatsAppAndroidClientInfo(ClientAppVersion version, int versionCode, byte[] md5Hash, SecretKeySpec secretKey, byte[][] certificates, boolean business) {
        this.version = version;
        this.versionCode = versionCode;
        this.md5Hash = md5Hash;
        this.secretKey = secretKey;
        this.certificates = certificates;
        this.business = business;
    }

    /**
     * Returns the cached consumer APK identity, downloading and parsing the APK on the first call.
     *
     * <p>Subsequent calls in the same JVM return the same instance. A failed download is not cached, so a later call
     * retries the download.
     *
     * @implNote This implementation uses double checked locking; the {@code volatile} {@link #personalApkInfo} field
     *           publishes the fully constructed instance to readers on the unsynchronised fast path.
     * @return the consumer Android client identity
     * @throws RuntimeException if the APK download or parsing fails
     */
    public static WhatsAppAndroidClientInfo ofPersonal() {
        if (personalApkInfo == null) {
            synchronized (personalApkInfoLock) {
                if(personalApkInfo == null) {
                    personalApkInfo = queryApkInfo(false);
                }
            }
        }
        return personalApkInfo;
    }

    /**
     * Returns the cached business APK identity, downloading and parsing the APK on the first call.
     *
     * <p>Subsequent calls in the same JVM return the same instance. A failed download is not cached, so a later call
     * retries the download.
     *
     * @implNote This implementation uses double checked locking; the {@code volatile} {@link #businessApkInfo} field
     *           publishes the fully constructed instance to readers on the unsynchronised fast path.
     * @return the business Android client identity
     * @throws RuntimeException if the APK download or parsing fails
     */
    public static WhatsAppAndroidClientInfo ofBusiness() {
        if (businessApkInfo == null) {
            synchronized (businessApkInfoLock) {
                if(businessApkInfo == null) {
                    businessApkInfo = queryApkInfo(true);
                }
            }
        }
        return businessApkInfo;
    }

    /**
     * Downloads the consumer or business APK through the anonymous Play Store pipeline and extracts the version,
     * {@code classes.dex} hash, signing certificates, and derived HMAC key.
     *
     * <p>The result is persisted through {@link #saveCached(WhatsAppAndroidClientInfo)} so subsequent JVMs can skip the
     * download. When a cache entry exists for the current Play Store {@code versionCode} it is returned directly without
     * any download.
     *
     * @implNote This implementation fully materialises the base APK into a {@link ByteArrayApkFile} because certificate
     *           extraction needs random access to the APK Signing Block at the tail of the archive. Splits are stream
     *           scanned with {@link ZipInputStream} so they never have to be held in memory; non density splits and any
     *           split left unread once the {@code about_logo.png} is found are closed eagerly so the underlying HTTP
     *           transfer is aborted instead of draining to completion.
     * @param business {@code true} for the business variant, {@code false} for the consumer variant
     * @return a populated {@link WhatsAppAndroidClientInfo} instance
     * @throws RuntimeException if the HTTP download, the APK parsing, or the cryptographic derivation fails
     */
    private static WhatsAppAndroidClientInfo queryApkInfo(boolean business) {
        var packageName = business ? BUSINESS_PACKAGE : PERSONAL_PACKAGE;
        try {
            var latest = PlayStoreUtils.latestVersion(packageName);
            var cached = loadCached(business);
            if (cached != null && cached.versionCode == latest.code()) {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "using cached android apk info for {0}, versionCode {1}", packageName, latest.code());
                }
                return cached;
            }

            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "downloading android apk for {0}, versionCode {1}", packageName, latest.code());
            }
            try (var downloaded = PlayStoreUtils.downloadApk(packageName, latest.code());
                 var baseApk = new ByteArrayApkFile(downloaded.baseApk().readAllBytes())) {
                var aboutLogo = findAboutLogoInBase(baseApk);
                if (aboutLogo == null) {
                    if (Log.DEBUG) {
                        LOGGER.log(Level.DEBUG, "about_logo.png missing from base apk, scanning density splits for {0}", packageName);
                    }
                    for (var split : downloaded.splits().entrySet()) {
                        if (!isDensityConfigSplit(split.getKey())) {
                            try {
                                split.getValue().close();
                            } catch (IOException _) {
                            }
                            continue;
                        }
                        aboutLogo = findAboutLogoInSplit(split.getValue());
                        if (aboutLogo != null) {
                            break;
                        }
                    }
                }
                if (aboutLogo == null) {
                    var notFound = new NoSuchElementException("Missing about_logo.png from apk");
                    if (Log.ERROR) {
                        LOGGER.log(Level.ERROR, "missing about_logo.png resource for " + packageName, notFound);
                    }
                    throw notFound;
                }

                var version = ClientAppVersion.of(baseApk.getApkMeta().getVersionName());

                var digest = MessageDigest.getInstance("MD5");
                digest.update(baseApk.getFileData("classes.dex"));
                var md5Hash = digest.digest();

                var secretKey = getSecretKey(baseApk.getApkMeta().getPackageName(), aboutLogo);

                var certificates = getCertificates(baseApk);

                var info = new WhatsAppAndroidClientInfo(version, latest.code(), md5Hash, secretKey, certificates, business);
                saveCached(info);
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "resolved android apk info for {0}: version {1}, versionCode {2}",
                            packageName, version, latest.code());
                }
                return info;
            }
        } catch (IOException | GeneralSecurityException exception) {
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "failed to extract data from apk for " + packageName, exception);
            }
            throw new RuntimeException("Cannot extract data from APK", exception);
        }
    }

    /**
     * Reads and decodes the on disk JSON cache for the requested variant.
     *
     * @implNote This implementation swallows any read or parse failure and returns {@code null} so a corrupted or
     *           incompatible cache file simply triggers a fresh download rather than propagating an exception.
     * @param business whether this is the business variant
     * @return the decoded client info, or {@code null} if the file is missing, unreadable or malformed
     */
    private static WhatsAppAndroidClientInfo loadCached(boolean business) {
        var path = cacheFile(business);
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try (var in = Files.newInputStream(path)) {
            var json = JSON.parseObject(in);
            var decoder = Base64.getDecoder();
            var version = ClientAppVersion.of(json.getString("version"));
            var versionCode = json.getIntValue("versionCode");
            var md5Hash = decoder.decode(json.getString("md5Hash"));
            var secretKey = new SecretKeySpec(decoder.decode(json.getString("secretKey")), "PBKDF2");
            var certs = json.getJSONArray("certificates");
            var certificates = new byte[certs.size()][];
            for (var i = 0; i < certs.size(); i++) {
                certificates[i] = decoder.decode(certs.getString(i));
            }
            return new WhatsAppAndroidClientInfo(version, versionCode, md5Hash, secretKey, certificates, business);
        } catch (IOException | RuntimeException _) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "no usable cached android apk info at {0}", path);
            }
            return null;
        }
    }

    /**
     * Serialises the given info to JSON and writes it to the per variant cache file.
     *
     * @implNote This implementation emits byte fields as Base64 strings so the payload is plain JSON, creates the parent
     *           directory if missing, and silently swallows any write failure because the cache is best effort: a failed
     *           write just means the next JVM refetches.
     * @param info the client info to persist
     */
    private static void saveCached(WhatsAppAndroidClientInfo info) {
        try {
            var path = cacheFile(info.business);
            var parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            var encoder = Base64.getEncoder();
            var certificates = new JSONArray();
            for (var cert : info.certificates) {
                certificates.add(encoder.encodeToString(cert));
            }
            var json = new JSONObject();
            json.put("versionCode", info.versionCode);
            json.put("version", info.version.toString());
            json.put("md5Hash", encoder.encodeToString(info.md5Hash));
            json.put("secretKey", encoder.encodeToString(info.secretKey.getEncoded()));
            json.put("certificates", certificates);
            Files.writeString(path, json.toJSONString());
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "saved android apk info cache at {0}", path);
            }
        } catch (IOException | RuntimeException exception) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "failed to write android apk info cache", exception);
            }
        }
    }

    /**
     * Resolves the on disk path of the JSON cache file for the requested variant.
     *
     * <p>The cache lives under {@code $user.home/.cobalt/cache/wa-android-{personal,business}.json} so all Cobalt JVMs for
     * the same OS user share it.
     *
     * @param business whether this is the business variant
     * @return the cache file path
     */
    private static Path cacheFile(boolean business) {
        return Path.of(
                System.getProperty("user.home"),
                ".cobalt",
                "cache",
                "wa-android-" + (business ? "business" : "personal") + ".json"
        );
    }

    /**
     * Returns whether the given Play Store split identifier designates a density configuration split.
     *
     * <p>Only density splits can carry density qualified drawables, so ABI splits and locale splits are skipped when
     * scanning for {@code about_logo.png}.
     *
     * @implNote This implementation matches by the trailing {@code dpi} suffix because Play Store density splits are always
     *           named {@code config.hdpi}, {@code config.xxhdpi}, {@code config.xxxhdpi} or similar.
     * @param splitName the split identifier reported by the Play Store
     * @return {@code true} when the split can carry density qualified drawables
     */
    private static boolean isDensityConfigSplit(String splitName) {
        return splitName != null && splitName.endsWith("dpi");
    }

    /**
     * Looks up each {@link #ABOUT_LOGO_PATHS} entry in the base APK and returns the first matching entry's bytes.
     *
     * @param baseApk the materialised base APK
     * @return the raw PNG bytes, or {@code null} when the base APK does not carry the drawable
     * @throws IOException if reading an APK entry fails
     */
    private static byte[] findAboutLogoInBase(ByteArrayApkFile baseApk) throws IOException {
        for (var path : ABOUT_LOGO_PATHS) {
            var data = baseApk.getFileData(path);
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    /**
     * Stream scans a density configuration split for any {@link #ABOUT_LOGO_PATHS} entry and returns the first match.
     *
     * @implNote This implementation uses {@link ZipInputStream} rather than {@link ByteArrayApkFile} so the caller never
     *           has to materialise the full split in memory and can abort the underlying HTTP transfer as soon as the entry
     *           is located, simply by closing the stream.
     * @param stream the split's response body stream
     * @return the raw PNG bytes, or {@code null} when the split does not carry any of the candidate paths
     * @throws IOException if reading the stream or an entry's decompressed bytes fails
     */
    private static byte[] findAboutLogoInSplit(InputStream stream) throws IOException {
        try (var zip = new ZipInputStream(stream)) {
            var entry = zip.getNextEntry();
            while (entry != null) {
                if (ABOUT_LOGO_PATHS.contains(entry.getName())) {
                    return zip.readAllBytes();
                }
                entry = zip.getNextEntry();
            }
            return null;
        }
    }

    /**
     * Extracts the raw DER bytes of every certificate that signs the APK.
     *
     * <p>The result populates {@link #certificates}, which {@link #computeRegistrationToken(long)} folds into the HMAC in
     * order.
     *
     * @param apkFile the parsed APK
     * @return a two dimensional array with one certificate per row in DER form
     * @throws IOException          if reading the signers metadata fails
     * @throws CertificateException if any certificate cannot be parsed
     */
    private static byte[][] getCertificates(ByteArrayApkFile apkFile) throws IOException, CertificateException {
        return apkFile.getApkSingers()
                .stream()
                .map(ApkSigner::getCertificateMetas)
                .flatMap(Collection::stream)
                .map(CertificateMeta::getData)
                .toArray(byte[][]::new);
    }

    /**
     * Derives the HMAC-SHA1 secret key used to sign registration tokens.
     *
     * <p>The resulting key is stored in {@link #secretKey} and reused for every {@link #computeRegistrationToken(long)}
     * call.
     *
     * @implNote This implementation runs PBKDF2-HMAC-SHA1 manually rather than going through
     *           {@code SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")} because the password is binary (raw PNG bytes
     *           appended to the package name) which the JCA factory rejects; iterations are fixed at {@code 128} and the
     *           output size at {@code 64} bytes to match the values the Android binary uses.
     * @param packageName the APK package identifier prepended to the {@code about_logo.png} bytes to form the password
     * @param resource    the raw {@code about_logo.png} bytes appended to the package name to form the password
     * @return a {@link SecretKeySpec} wrapping the derived 64 byte key as {@code PBKDF2}
     * @throws IOException              declared by contract; never thrown in practice
     * @throws GeneralSecurityException if HMAC-SHA1 cannot be instantiated on the running JDK
     */
    private static SecretKeySpec getSecretKey(String packageName, byte[] resource) throws IOException, GeneralSecurityException {
        var packageBytes = packageName.getBytes(StandardCharsets.UTF_8);
        var password = new byte[packageBytes.length + resource.length];
        System.arraycopy(packageBytes, 0, password, 0, packageBytes.length);
        System.arraycopy(resource, 0, password, packageBytes.length, resource.length);

        var mac = Mac.getInstance("HmacSHA1");
        var keySpec = new SecretKeySpec(password, mac.getAlgorithm());
        mac.init(keySpec);

        var keySize = 64;
        var macLen = mac.getMacLength();
        var iterations = 128;
        var blocks = (keySize + macLen - 1) / macLen;

        var out = new byte[keySize];
        var state = new byte[macLen];
        var iBuf = new byte[4];

        var offset = 0;
        for (var block = 1; block <= blocks; ++block) {
            mac.update(MOBILE_ANDROID_SALT);

            iBuf[0] = (byte) (block >>> 24);
            iBuf[1] = (byte) (block >>> 16);
            iBuf[2] = (byte) (block >>> 8);
            iBuf[3] = (byte) (block);
            mac.update(iBuf, 0, iBuf.length);

            mac.doFinal(state, 0);

            var toCopy = Math.min(macLen, keySize - offset);
            System.arraycopy(state, 0, out, offset, toCopy);

            for (var cnt = 1; cnt < iterations; ++cnt) {
                mac.update(state, 0, macLen);

                mac.doFinal(state, 0);

                for (var j = 0; j < toCopy; ++j) {
                    out[offset + j] ^= state[j];
                }
            }

            offset += toCopy;
        }

        return new SecretKeySpec(out, 0, out.length, "PBKDF2");
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation returns the version parsed from the APK's {@code AndroidManifest.xml}
     *           {@code versionName} attribute, which mirrors what the Play Store displays.
     */
    @Override
    public ClientAppVersion version() {
        return version;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation reports the variant determined by which Play Store package the APK was downloaded from
     *           ({@link #PERSONAL_PACKAGE} versus {@link #BUSINESS_PACKAGE}).
     */
    @Override
    public boolean business() {
        return business;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation feeds each APK signing certificate, the {@code classes.dex} MD5 hash, and the decimal
     *           ASCII of the phone number into an HMAC-SHA1 keyed by the derived {@link #secretKey}, in that order, then
     *           Base64 encodes the digest and URL encodes the Base64.
     * @throws InternalError if HMAC-SHA1 is not available on the running JDK
     */
    @Override
    public String computeRegistrationToken(long nationalPhoneNumber) {
        try {
            var mac = Mac.getInstance("HMACSHA1");
            mac.init(secretKey);

            for (var certificate : certificates) {
                mac.update(certificate);
            }

            mac.update(md5Hash);
            mac.update(String.valueOf(nationalPhoneNumber).getBytes(StandardCharsets.UTF_8));
            var token = URLEncoder.encode(Base64.getEncoder().encodeToString(mac.doFinal()), StandardCharsets.UTF_8);
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "computed android registration token for {0}, business {1}", new LogRedactable.Phone(nationalPhoneNumber), business);
            }
            return token;
        }catch (GeneralSecurityException exception) {
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "failed to compute android registration token", exception);
            }
            throw new InternalError("Cannot compute registration token", exception);
        }
    }
}
