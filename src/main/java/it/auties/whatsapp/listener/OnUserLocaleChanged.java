package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.mobile.CountryLocale;

public interface OnUserLocaleChanged extends Listener {
    /**
     * Called when the companion's locale changes
     *
     * @param oldLocale the non-null old locale
     * @param newLocale the non-null new picture
     */
    @Override
    void onLocaleChanged(CountryLocale oldLocale, CountryLocale newLocale);
}
