package org.okapi.swim.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.okapi.swim.ping.Member;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MetaResponse {
  @SerializedName("iam")
  @JsonProperty("iam")
  String iAm;

  List<Member> members;
}
