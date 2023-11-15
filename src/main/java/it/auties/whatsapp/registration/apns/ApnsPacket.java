package it.auties.whatsapp.registration.apns;

import java.util.Map;

public record ApnsPacket(ApnsPayloadTag tag, Map<Integer, byte[]> fields) {

}
