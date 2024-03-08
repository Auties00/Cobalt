package it.auties.whatsapp.registration.apns;

import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListParser;

record DeviceActivationInfo(boolean ackReceived, boolean showSettings, ActivationRecord activationRecord) {
    static DeviceActivationInfo ofPlist(byte[] plist) {
        try {
            var parsed = (NSDictionary) PropertyListParser.parse(plist);
            var deviceActivation = (NSDictionary) parsed.get("device-activation");
            var ackReceived = deviceActivation.get("ack-received")
                    .toJavaObject(boolean.class);
            var showSettings = deviceActivation.get("show-settings")
                    .toJavaObject(boolean.class);
            var activationDictionary = (NSDictionary) deviceActivation.get("activation-record");
            var deviceCertificate = activationDictionary.get("DeviceCertificate")
                    .toJavaObject(byte[].class);
            var activationRecord = new ActivationRecord(deviceCertificate);
            return new DeviceActivationInfo(ackReceived, showSettings, activationRecord);
        } catch (Throwable throwable) {
            throw new RuntimeException("Cannot parse device activation info", throwable);
        }
    }
}
