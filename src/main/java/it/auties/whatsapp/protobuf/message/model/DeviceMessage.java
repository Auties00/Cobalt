package it.auties.whatsapp.protobuf.message.model;

import it.auties.whatsapp.protobuf.message.device.DeviceSentMessage;
import it.auties.whatsapp.protobuf.message.device.DeviceSyncMessage;

/**
 * A model interface that represents a WhatsappMessage sent by the device linked to this whatsapp web session
 */
public sealed interface DeviceMessage extends Message permits DeviceSyncMessage, DeviceSentMessage {
}
