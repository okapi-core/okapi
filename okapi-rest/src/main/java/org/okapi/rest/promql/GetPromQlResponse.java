package org.okapi.rest.promql;

import lombok.Data;
import com.google.gson.annotations.SerializedName;

/**
 * Top-level Prometheus HTTP API response envelope.
 * Works for both success and error responses.
 */
@Data
public class GetPromQlResponse<T> {
    @SerializedName("status")
    private String status; // "success" or "error"

    @SerializedName("data")
    private T data;

    @SerializedName("errorType")
    private String errorType;

    @SerializedName("error")
    private String error;

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }
}
