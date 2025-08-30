package org.okapi.promql.eval.visitor;

// parse/DurationUtil.java
public final class DurationUtil {
    private DurationUtil() {}
    /** Prom-like duration: 5s, 1m, 2h, 7d, 2w, 1y */
    public static long parseToMillis(String dur) {
        long n = 0; int i = 0; int len = dur.length();
        while (i < len && Character.isDigit(dur.charAt(i))) { n = n*10 + (dur.charAt(i++) - '0'); }
        if (i >= len) throw new IllegalArgumentException("Bad duration: " + dur);
        char u = dur.charAt(i);
        return switch (u) {
            case 's' -> n * 1000L;
            case 'm' -> n * 60_000L;
            case 'h' -> n * 3_600_000L;
            case 'd' -> n * 86_400_000L;
            case 'w' -> n * 7 * 86_400_000L;
            case 'y' -> n * 365 * 86_400_000L;
            default  -> throw new IllegalArgumentException("Unsupported unit: " + u);
        };
    }
}

