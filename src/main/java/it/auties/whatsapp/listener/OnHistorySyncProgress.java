package it.auties.whatsapp.listener;

public interface OnHistorySyncProgress
        extends Listener {
    /**
     * Called when the socket receives the sync percentage for the full or recent chunk of messages.
     * This method is only called when the QR is first scanned and history is being synced.
     *
     * @param percentage the percentage synced up to now
     * @param recent     whether the sync is about the recent messages or older messages
     */
    void onHistorySyncProgress(int percentage, boolean recent);
}