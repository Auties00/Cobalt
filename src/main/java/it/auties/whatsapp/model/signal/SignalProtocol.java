package it.auties.whatsapp.model.signal;

public final class SignalProtocol {
    public static final int CURRENT_VERSION = 3;
    public static final String SKMSG = "skmsg";
    public static final String PKMSG = "pkmsg";
    public static final String MSG = "msg";
    public static final String MSMG = "msmsg";

    private SignalProtocol() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
