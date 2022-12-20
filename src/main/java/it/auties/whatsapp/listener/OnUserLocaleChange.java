package it.auties.whatsapp.listener;

public interface OnUserLocaleChange
        extends Listener {
    /**
     * Called when the companion's locale changes
     *
     * @param oldLocale the non-null old locale
     * @param newLocale the non-null new picture
     */
    @Override
    void onUserLocaleChange(String oldLocale, String newLocale);
}
