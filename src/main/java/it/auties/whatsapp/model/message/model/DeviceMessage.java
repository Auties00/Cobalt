package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.message.device.DeviceSentMessage;
import it.auties.whatsapp.model.message.device.DeviceSyncMessage;

/**
 * A model interface that represents a WhatsappMessage sent by the device linked to this whatsapp web session
 */
public sealed interface DeviceMessage extends Message permits DeviceSyncMessage, DeviceSentMessage {
}
