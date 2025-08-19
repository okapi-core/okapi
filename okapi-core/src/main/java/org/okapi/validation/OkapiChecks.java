package org.okapi.validation;

import org.okapi.exceptions.BadRequestException;
import java.util.function.Supplier;

public class OkapiChecks {

  public static void checkArgument(boolean condition, String message) throws BadRequestException {
    if (!condition) {
      throw new BadRequestException(message);
    }
  }

  public static <T extends  Exception> void checkArgument(boolean condition, Supplier<T> elseThrow) throws T{
    if(!condition){
      throw elseThrow.get();
    }
  }

  public static <T extends  Exception> void throwIf(boolean condition, Supplier<T> elseThrow) throws T{
    if(condition){
      throw elseThrow.get();
    }
  }
}
