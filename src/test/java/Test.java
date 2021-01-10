import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class Test {
    public static void main(String[] args) throws BackingStoreException {
        Preferences.userRoot().clear();
    }
}
