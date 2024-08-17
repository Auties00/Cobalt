package it.auties.whatsapp.registration.cloudVerification.apns;

import java.util.Map;

public record ApnsPacket(ApnsPayloadTag tag, Map<Integer, byte[]> fields) {

}
