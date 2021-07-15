package it.auties.whatsapp4j.test.utils;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import it.auties.whatsapp4j.test.sodium.Sodium;
import it.auties.whatsapp4j.utils.internal.Validate;
import lombok.experimental.UtilityClass;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

@UtilityClass
public class SodiumUtils {
    private final String LIBRARY_PROPERTY = "java.library.path";
    private final String SODIUM_WIN32 = "sodium/windows/libsodium.dll";
    private final String SODIUM_WIN64 = "sodium/windows64/libsodium.dll";
    private final String SODIUM_LINUX = "sodium/linux64/libsodium.so";

    public Sodium loadLibrary() {
        try {
            var library = setupJavaProperty();
            return Native.load(library, Sodium.class);
        }catch (Exception exception){
            throw new RuntimeException("Cannot load sodium", exception);
        }
    }

    private String setupJavaProperty() throws URISyntaxException {
        var path = getSodiumPath();
        var directory = SodiumUtils.class.getClassLoader().getResource(path);
        var filePath = Path.of(Objects.requireNonNull(directory).toURI());
        System.setProperty(LIBRARY_PROPERTY, filePath.toString());
        return filePath.getFileName().toString();
    }

    private String getSodiumPath(){
        Validate.isTrue(!Platform.isMac(), "Missing support for mac os");
        Validate.isTrue(!Platform.isARM(), "Missing support for 32bit/64bit ARM");
        var is64Bit = Platform.is64Bit();
        if(Platform.isWindows()){
            return is64Bit ? SODIUM_WIN64 : SODIUM_WIN32;
        }

        if(Platform.isLinux()){
            Validate.isTrue(is64Bit, "Missing support for 32bit Linux");
            return SODIUM_LINUX;
        }

        throw new IllegalArgumentException("Missing support for unknown architecture");
    }
}
