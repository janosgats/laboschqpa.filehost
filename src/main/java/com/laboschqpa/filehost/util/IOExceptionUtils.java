package com.laboschqpa.filehost.util;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;

@Log4j2
public class IOExceptionUtils {
    public static void swallowAndLog(Callable callable, String message) {
        try {
            callable.call();
        } catch (IOException e) {
            log.error(message, e);
        }
    }

    public static void wrap(Callable callable, String message) {
        try {
            callable.call();
        } catch (IOException e) {
            throw new RuntimeException(message, e);
        }
    }

    public static <T> T wrap(Supplier<T> supplier, String message) {
        try {
            return supplier.get();
        } catch (IOException e) {
            throw new RuntimeException(message, e);
        }
    }

    public interface Callable {
        void call() throws IOException;
    }

    public interface Supplier<T> {
        T get() throws IOException;
    }
}
