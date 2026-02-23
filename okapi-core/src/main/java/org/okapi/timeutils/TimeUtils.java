package org.okapi.timeutils;

public class TimeUtils {
    public static long millisToNanos(long millis){
        return millis * 1_000_000L;
    }
}
