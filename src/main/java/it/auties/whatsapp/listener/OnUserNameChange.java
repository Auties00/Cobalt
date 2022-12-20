package it.auties.whatsapp.listener;

public interface OnUserNameChange
        extends Listener {
    /**
     * Called when the companion's name changes
     *
     * @param oldName the non-null old name
     * @param newName the non-null new name
     */
    @Override
    void onUserNameChange(String oldName, String newName);
}
