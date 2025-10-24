package org.okapi.swim;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.okapi.swim.rest.MetaResponse;

import java.util.Collections;

public class MetaResponseTest {

    @Test
    public void testResponse(){
        var response = new MetaResponse("test", Collections.emptyList());
        var gson = new Gson();
        var serialized = gson.toJson(response);
        System.out.println(serialized);
    }
}
