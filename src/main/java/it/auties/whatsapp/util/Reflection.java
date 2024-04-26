package it.auties.whatsapp.util;

import sun.misc.Unsafe;

import java.lang.reflect.AccessibleObject;

public class Reflection {
    private static final Unsafe unsafe;
    private static final long offset;

    static {
        unsafe = initUnsafe();
        offset = initOffset();
    }

    private static Unsafe initUnsafe() {
        try {
            var unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            return (Unsafe) unsafeField.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Cannot access unsafe");
        }
    }

    @SuppressWarnings("all")
    private static long initOffset() {
        try {
            class AccessibleObjectPlaceholder {
                boolean override;
                Object accessCheckCache;
            }

            var offsetField = AccessibleObjectPlaceholder.class.getDeclaredField("override");
            return unsafe.objectFieldOffset(offsetField);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Cannot access override field", exception);
        }
    }

    public static void open(AccessibleObject object) {
        unsafe.putBoolean(object, offset, true);
    }
}
