package it.auties.whatsapp.model.companion;

/**
 * The constants of this enumeration describe the various types of recommendedChannels that can be yielded by a new device's registration through the mobile api
 */
public enum CompanionLinkResult {
    /**
     * The device was successfully linked
     */
    SUCCESS,

    /**
     * The limit of devices, as of now four, has already been reached
     */
    MAX_DEVICES_ERROR,

    /**
     * The device couldn't be linked because of an unknown error
     * This usually means that the qr code is no longer valid
     */
    RETRY_ERROR
}
