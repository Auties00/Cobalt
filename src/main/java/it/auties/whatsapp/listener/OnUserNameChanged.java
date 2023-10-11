package it.auties.whatsapp.listener;

public interface OnUserNameChanged extends Listener {
    /**
     * Called when the companion's name changes
     *
     * @param oldName the non-null old name
     * @param newName the non-null new name
     */
    @Override
    void onNameChanged(String oldName, String newName);
}
