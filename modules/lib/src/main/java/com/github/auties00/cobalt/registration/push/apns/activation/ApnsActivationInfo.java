package com.github.auties00.cobalt.registration.push.apns.activation;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.registration.push.apns.plist.Plist;
import com.github.auties00.cobalt.registration.push.apns.plist.value.PlistDataValue;
import com.github.auties00.cobalt.registration.push.apns.plist.value.PlistDictionaryValue;

import java.io.IOException;
import java.lang.System.Logger.Level;

/**
 * Models the decoded {@code <Protocol>} plist returned from {@code albert.apple.com}'s
 * {@code deviceActivation} endpoint.
 *
 * <p>An instance is produced by {@link #ofPlist(byte[])} once the activation HTTP call succeeds.
 * Cobalt consumes only the {@code DeviceCertificate} (the X.509 device certificate Apple's iPhone
 * Device CA signs, valid for roughly three years); the rest of the plist ({@code ack-received},
 * {@code show-settings}, capability bits) is parsed but discarded. The caller persists the
 * certificate bytes via {@link com.github.auties00.cobalt.registration.push.apns.ApnsSession}.
 *
 * @implNote This implementation models the result as a plain {@link Record} holding only the single
 * field the courier connection later needs.
 *
 * @param deviceCertificate the DER bytes of the Apple-signed device certificate
 */
public record ApnsActivationInfo(byte[] deviceCertificate) {
    /**
     * The logger for {@link ApnsActivationInfo}.
     */
    private static final System.Logger LOGGER = Log.get(ApnsActivationInfo.class);

    /**
     * Parses the {@code <Protocol>} plist into the structured record.
     *
     * <p>This is invoked once per fresh activation after the inner plist is extracted from the
     * activation response, by {@code ApnsActivation}. The required key path is
     * {@code device-activation -> activation-record -> DeviceCertificate}, read through
     * {@link Plist#parse(byte[])}, {@link PlistDictionaryValue#get(String)}, and
     * {@link PlistDataValue#toByteArray()}.
     *
     * @implNote This implementation funnels every parsing failure (including
     * {@link NullPointerException} from missing keys and {@link ClassCastException} from unexpected
     * types) through a single {@link IOException} so activation-side parse failures surface as
     * ordinary transport failures.
     *
     * @param plist the UTF-8 plist bytes lifted from the activation response
     * @return the decoded activation info
     * @throws IOException if the plist is malformed or missing the expected nested keys
     */
    public static ApnsActivationInfo ofPlist(byte[] plist) throws IOException {
        try {
            var root = (PlistDictionaryValue) Plist.parse(plist);
            var deviceActivation = (PlistDictionaryValue) root.get("device-activation").orElseThrow();
            var activationRecord = (PlistDictionaryValue) deviceActivation.get("activation-record").orElseThrow();
            var deviceCertificate = (PlistDataValue) activationRecord.get("DeviceCertificate").orElseThrow();
            var info = new ApnsActivationInfo(deviceCertificate.toByteArray());
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "parsed device activation info, certificate bytes={0}", info.deviceCertificate().length);
            return info;
        } catch (Exception e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "cannot parse device activation info", e);
            throw new IOException("Cannot parse device activation info", e);
        }
    }
}
