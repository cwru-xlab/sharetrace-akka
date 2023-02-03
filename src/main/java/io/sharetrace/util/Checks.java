package io.sharetrace.util;

import com.google.common.collect.Range;

public final class Checks {

    private Checks() {
    }

    public static <T extends Comparable<T>> T checkRange(T value, Range<T> range, String name) {
        checkArgs(range.contains(value), "%s must be in the range %s; got %s", name, range, value);
        return value;
    }

    public static void checkState(boolean condition, String template, Object... args) {
        if (!condition) {
            throw new IllegalStateException(String.format(template, args));
        }
    }

    public static void checkArgs(boolean condition, String template, Object... args) {
        if (!condition) {
            throw new IllegalArgumentException(String.format(template, args));
        }
    }
}
