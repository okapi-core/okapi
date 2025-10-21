package org.okapi.swim;

import lombok.AllArgsConstructor;
import lombok.experimental.StandardException;

@AllArgsConstructor
@StandardException
public class HttpException extends Exception{
    int code;
    String msg;
}
