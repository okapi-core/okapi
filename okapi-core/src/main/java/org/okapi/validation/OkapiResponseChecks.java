package org.okapi.validation;

public class OkapiResponseChecks {

    public static boolean is4xx(int code){
        return code >= 400 && code < 500;
    }

    public static boolean is2xx(int code){
        return code >= 200 && code < 300;
    }

    public static boolean is5xx(int code){
        return code >= 500 && code < 600;
    }
}
