package it.auties.whatsapp.listener;

public interface OnRegistrationCode extends Listener {
    /**
     * Called when an OTP is requested from a new device
     * Only works on the mobile API
     *
     * @param code the registration code
     */
    @Override
    void onRegistrationCode(long code);
}