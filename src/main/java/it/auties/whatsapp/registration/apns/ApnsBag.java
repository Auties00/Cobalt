package it.auties.whatsapp.registration.apns;

import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListParser;

record ApnsBag(
        int hostCount,
        String hostname
) {
    static ApnsBag ofPlist(byte[] plist) {
        try {
            var parsed = (NSDictionary) PropertyListParser.parse(plist);
            var bagBytes = parsed.get("bag").toJavaObject(byte[].class);
            var bag = (NSDictionary) PropertyListParser.parse(bagBytes);
            var hostCount = bag.get("APNSCourierHostcount")
                    .toJavaObject(Integer.class);
            var hostname = bag.get("APNSCourierHostname")
                    .toJavaObject(String.class);
            return new ApnsBag(hostCount, hostname);
        } catch (Throwable throwable) {
            throw new RuntimeException("Cannot parse apns bag", throwable);
        }
    }
}
