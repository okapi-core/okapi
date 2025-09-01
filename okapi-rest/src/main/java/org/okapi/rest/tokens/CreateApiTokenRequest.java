package org.okapi.rest.tokens;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CreateApiTokenRequest {
    @SerializedName("canRead")
    boolean canRead;
    @SerializedName("canWrite")
    boolean canWrite;

}
