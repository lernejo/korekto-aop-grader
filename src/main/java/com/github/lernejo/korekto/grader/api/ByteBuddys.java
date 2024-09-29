package com.github.lernejo.korekto.grader.api;

import com.github.lernejo.korekto.toolkit.misc.RandomSupplier;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ByteBuddys {

    static final String GEN_PACKAGE = "fr.lernejo.generated.";
    static final ByteBuddy BYTE_BUDDY = new ByteBuddy();

    public static Class<?> saveToFileAndLoad(DynamicType.Unloaded<?> type, Path directory, ClassLoader cl) {
        try {
            type.saveIn(directory.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return type
            .load(cl, ClassLoadingStrategy.Default.WRAPPER)
            .getLoaded();
    }

    public static Path createTempClassDirectory(RandomSupplier rs) {
        try {
            return Files.createDirectories(Paths.get("target")
                .resolve("generated-classes")
                .resolve("gen-" + System.currentTimeMillis() + "-" + rs.nextInt(100)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
