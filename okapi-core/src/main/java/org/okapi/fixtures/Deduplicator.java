package org.okapi.fixtures;

public class Deduplicator {
    public static <T> String dedup(String input, Class<T> clazz){
        return clazz.getSimpleName() + "_" + input;
    }
}

