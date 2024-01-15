package it.auties.whatsapp.listener;

public interface OnUserAboutChanged extends Listener {
    /**
     * Called when the companion's status changes
     *
     * @param oldAbout the non-null old about
     * @param newAbout the non-null new about
     */
    void onAboutChange(String oldAbout, String newAbout);
}
