package it.auties.whatsapp.listener;

public interface OnUserStatusChange extends Listener {
    /**
     * Called when the companion's status changes
     *
     * @param oldStatus the non-null old status
     * @param newStatus the non-null new status
     */
    void onUserStatusChange(String oldStatus, String newStatus);
}
