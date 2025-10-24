package org.okapi.swim.rest;

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
  String iAm;
  List<Member> members;
}
