package it.auties.whatsapp.utils;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import lombok.experimental.UtilityClass;

import java.nio.file.Path;
import java.util.Objects;

@UtilityClass
public class SodiumUtils {
    private final String LIBRARY_PROPERTY = "java.library.path";
    private final String SODIUM_WIN32 = "sodium/windows/libsodium.dll";
    private final String SODIUM_WIN64 = "sodium/windows64/libsodium.dll";
    private final String SODIUM_LINUX = "sodium/linux64/libsodium.so";
    private Sodium sodium;

    public Sodium loadLibrary() {
        if(sodium == null){
            var library = setupJavaProperty();
            sodium = Native.load(library, Sodium.class);
        }

        return sodium;
    }

    private String setupJavaProperty() {
        try {
            var path = getSodiumPath();
            var directory = SodiumUtils.class.getClassLoader().getResource(path);
            var filePath = Path.of(Objects.requireNonNull(directory).toURI());
            System.setProperty(LIBRARY_PROPERTY, filePath.toString());
            return filePath.getFileName().toString();
        }catch (Exception exception){
            throw new RuntimeException("Cannot setup sodium", exception);
        }
    }

    private String getSodiumPath(){
        var is64Bit = Platform.is64Bit();
        if(Platform.isWindows()){
            return is64Bit ? SODIUM_WIN64 : SODIUM_WIN32;
        }

        if(Platform.isLinux() && is64Bit){
            return SODIUM_LINUX;
        }

        throw new IllegalArgumentException("Missing support for this platform");
    }
}
